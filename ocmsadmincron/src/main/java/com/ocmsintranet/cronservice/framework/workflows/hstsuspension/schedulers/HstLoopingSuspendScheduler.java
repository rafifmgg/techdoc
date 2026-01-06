package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;
import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services.HstLoopingSuspendService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduler for TS-HST Looping Suspension
 * OCMS 20: Re-apply TS-HST suspension when revival date approaches (indefinite looping)
 *
 * This scheduler is responsible for:
 * 1. Checking for TS-HST suspensions approaching revival date
 * 2. Verifying if address is still invalid in ocms_temp_unc_hst_addr
 * 3. Re-applying TS-HST suspension if address remains invalid
 * 4. Allowing revival if address becomes valid
 *
 * This creates an indefinite loop where TS-HST is continuously re-applied until:
 * - The address becomes valid (from MHA/DataHive updates)
 * - Manual intervention removes the suspension
 * - The notice is otherwise resolved
 *
 * Features:
 * - Runs daily at 04:00 AM (after auto-suspend job)
 * - Can be enabled/disabled via cron.hst.looping.suspend.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Configurable number of days before revival to check (default: 7 days)
 *
 * Configuration properties:
 * - cron.hst.looping.suspend.enabled: Enable/disable the scheduler (default: false)
 * - cron.hst.looping.suspend.schedule: Cron expression for scheduling (default: 0 0 4 * * ?)
 * - cron.hst.looping.suspend.shedlock.name: Name for ShedLock (default: hst_looping_suspend)
 * - cron.hst.looping.days.before.revival: Days before revival to check (default: 7)
 */
@Slf4j
@Component
public class HstLoopingSuspendScheduler {

    private final HstLoopingSuspendService hstLoopingSuspendService;

    @Value("${cron.hst.looping.suspend.enabled:false}")
    private boolean enabled;

    public HstLoopingSuspendScheduler(HstLoopingSuspendService hstLoopingSuspendService) {
        this.hstLoopingSuspendService = hstLoopingSuspendService;
    }

    /**
     * Scheduled execution of the TS-HST looping suspension job.
     * The schedule is configured to run daily at 04:00 AM (0 0 4 * * ?).
     *
     * This job checks TS-HST suspensions approaching revival and re-applies if needed.
     */
    @Scheduled(cron = "${cron.hst.looping.suspend.schedule:0 0 4 * * ?}")
    @SchedulerLock(name = "${cron.hst.looping.suspend.shedlock.name:hst_looping_suspend}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT2H")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("[TS-HST-LOOP] Looping suspension job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("[TS-HST-LOOP] Starting scheduled execution of TS-HST looping suspension job");

        try {
            CompletableFuture<SuspensionResult> future = hstLoopingSuspendService.executeLoopingJob();
            SuspensionResult result = future.get(); // Wait for completion

            if (result == null) {
                log.warn("[TS-HST-LOOP] Job returned null result");
            } else if (result.isSuccess()) {
                log.info("[TS-HST-LOOP] Job completed successfully: {}", result.getDetailedMessage());
            } else {
                log.error("[TS-HST-LOOP] Job failed: {}", result.getMessage());
            }
        } catch (Exception ex) {
            log.error("[TS-HST-LOOP] Exception occurred during job execution", ex);
        }
    }
}
