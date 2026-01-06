package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;
import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services.HstAutoSuspendService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduler for Auto-Suspending New Notices with HST IDs
 * OCMS 20: Automatically apply TS-HST suspension to new notices when HST address is invalid
 *
 * This scheduler is responsible for:
 * 1. Checking ocms_hst for HST IDs
 * 2. Verifying address validity in ocms_temp_unc_hst_addr
 * 3. Finding associated notices from ocms_offence_notice_owner_driver
 * 4. Applying TS-HST (Temporary Suspension - House Tenant) to notices with invalid addresses
 *
 * This runs AFTER MHA/DataHive results are stored in the temp table.
 *
 * Features:
 * - Runs daily at 03:00 AM (after MHA/DataHive processing and report generation)
 * - Can be enabled/disabled via cron.hst.auto.suspend.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Only suspends notices that don't already have a suspension
 *
 * Configuration properties:
 * - cron.hst.auto.suspend.enabled: Enable/disable the scheduler (default: false)
 * - cron.hst.auto.suspend.schedule: Cron expression for scheduling (default: 0 0 3 * * ?)
 * - cron.hst.auto.suspend.shedlock.name: Name for ShedLock (default: hst_auto_suspend)
 */
@Slf4j
@Component
public class HstAutoSuspendScheduler {

    private final HstAutoSuspendService hstAutoSuspendService;

    @Value("${cron.hst.auto.suspend.enabled:false}")
    private boolean enabled;

    public HstAutoSuspendScheduler(HstAutoSuspendService hstAutoSuspendService) {
        this.hstAutoSuspendService = hstAutoSuspendService;
    }

    /**
     * Scheduled execution of the HST auto-suspend job.
     * The schedule is configured to run daily at 03:00 AM (0 0 3 * * ?).
     *
     * This job checks all HST IDs and applies TS-HST suspension to notices with invalid addresses.
     */
    @Scheduled(cron = "${cron.hst.auto.suspend.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.hst.auto.suspend.shedlock.name:hst_auto_suspend}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT2H")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("[HST-AUTO-SUSPEND] Auto-suspend job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("[HST-AUTO-SUSPEND] Starting scheduled execution of HST auto-suspend job");

        try {
            CompletableFuture<SuspensionResult> future = hstAutoSuspendService.executeAutoSuspendJob();
            SuspensionResult result = future.get(); // Wait for completion

            if (result == null) {
                log.warn("[HST-AUTO-SUSPEND] Job returned null result");
            } else if (result.isSuccess()) {
                log.info("[HST-AUTO-SUSPEND] Job completed successfully: {}", result.getDetailedMessage());
            } else {
                log.error("[HST-AUTO-SUSPEND] Job failed: {}", result.getMessage());
            }
        } catch (Exception ex) {
            log.error("[HST-AUTO-SUSPEND] Exception occurred during job execution", ex);
        }
    }
}
