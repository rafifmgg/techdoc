package com.ocmsintranet.cronservice.framework.workflows.adminfee.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.adminfee.services.AdminFeeService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Admin Fee workflow.
 *
 * Triggers the admin fee job daily to apply administration fees to foreign vehicle notices
 * that remain unpaid beyond the FOD (Furnish Offender Details) parameter period.
 *
 * Default Schedule: Daily at 3:00 AM (0 0 3 * * ?)
 * ShedLock: Prevents concurrent execution across multiple instances
 *
 * Reference: OCMS 14 Functional Document v1.7, Section 3.7 - Foreign Vehicle Processing
 */
@Slf4j
@Component
public class AdminFeeScheduler {

    private final AdminFeeService adminFeeService;

    @Value("${cron.adminfee.enabled:false}")
    private boolean enabled;

    public AdminFeeScheduler(AdminFeeService adminFeeService) {
        this.adminFeeService = adminFeeService;
    }

    /**
     * Scheduled execution of admin fee job.
     *
     * Runs daily at 3:00 AM by default.
     * ShedLock ensures only one instance runs at a time:
     * - lockAtLeastFor: 5 minutes (prevents rapid re-execution)
     * - lockAtMostFor: 30 minutes (releases lock if instance crashes)
     */
    @Scheduled(cron = "${cron.adminfee.schedule:0 0 3 * * ?}")
    @SchedulerLock(
        name = "${cron.adminfee.shedlock.name:apply_admin_fee}",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT30M"
    )
    public void runAdminFeeJob() {
        if (!enabled) {
            log.debug("Admin fee cron is disabled. Skipping execution.");
            return;
        }

        log.info("Starting scheduled admin fee job execution");

        try {
            adminFeeService.executeJob()
                .thenAccept(result -> {
                    log.info("Admin fee job completed: {}", result.getMessage());
                    log.info("Processed: {}, Updated: {}",
                            result.getNoticesProcessed(), result.getNoticesUpdated());
                })
                .exceptionally(e -> {
                    log.error("Error in scheduled admin fee job: {}", e.getMessage(), e);
                    return null;
                });

        } catch (Exception e) {
            log.error("Unexpected error triggering admin fee job: {}", e.getMessage(), e);
        }
    }
}
