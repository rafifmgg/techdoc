package com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.jobs.Ocms41AutoApprovalJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for OCMS 41: Auto-Approval for Furnished Submissions
 *
 * This scheduler runs periodically to auto-review furnished submissions
 * that have been synced from Internet DB (status = 'S')
 *
 * Default schedule: Every 10 minutes (after sync job runs at 5-min intervals)
 * Cron expression: 0 star-slash-10 star star star question-mark means:
 * - 0 seconds
 * - Every 10 minutes
 * - Every hour
 * - Every day of month
 * - Every month
 * - Any day of week
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ocms41AutoApprovalScheduler {

    private final Ocms41AutoApprovalJob autoApprovalJob;

    @Value("${ocms41.auto-approval.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Scheduled auto-approval job - runs every 10 minutes by default
     * Can be configured via application properties:
     * - ocms41.auto-approval.enabled: Enable/disable the scheduler (default: true)
     * - cron.ocms41.auto.approval.schedule: Cron expression for schedule
     * - cron.ocms41.auto.approval.shedlock.name: ShedLock name for distributed lock
     */
    @Scheduled(cron = "${cron.ocms41.auto.approval.schedule:0 */10 * * * ?}")
    @SchedulerLock(
        name = "${cron.ocms41.auto.approval.shedlock.name:ocms41_auto_approval}",
        lockAtLeastFor = "PT5M",      // Lock for at least 5 minutes
        lockAtMostFor = "PT15M"       // Maximum lock time 15 minutes
    )
    public void processAutoApproval() {
        if (!schedulerEnabled) {
            log.debug("OCMS 41 Auto-Approval scheduler is disabled");
            return;
        }

        log.info("==========================================");
        log.info("Starting scheduled OCMS 41: Auto-Approval Process");
        log.info("Cron job: ocms41_auto_approval");
        log.info("==========================================");

        try {
            // Execute the auto-approval job
            autoApprovalJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled OCMS 41 auto-approval completed successfully: {}", result.getMessage());
                    } else {
                        log.error("Scheduled OCMS 41 auto-approval failed: {}", result.getMessage());
                        // Framework will handle email notification for failures
                    }
                })
                .exceptionally(e -> {
                    log.error("Unexpected error in scheduled OCMS 41 auto-approval: {}", e.getMessage(), e);
                    // Framework will handle email notification for exceptions
                    return null;
                });

        } catch (Exception e) {
            log.error("Error starting scheduled OCMS 41 auto-approval: {}", e.getMessage(), e);
            // Framework will handle email notification
        }
    }

    /**
     * Manual trigger endpoint support
     * This method can be called directly for manual execution
     *
     * @return CompletableFuture with job result
     */
    public java.util.concurrent.CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult>
            triggerManualExecution() {
        log.info("Manual trigger for OCMS 41 Auto-Approval");
        return autoApprovalJob.execute();
    }
}
