package com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.jobs.Ocms41SyncFurnishedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for OCMS 41: Sync Furnished Submissions (Internet → Intranet)
 *
 * This scheduler runs periodically to sync furnish hirer/driver submissions from Internet DB to Intranet DB
 * where is_sync = 'N'
 *
 * Default schedule: Every 5 minutes (per OCMS 41 specs)
 * Cron expression: 0 star-slash-5 star star star question-mark means:
 * - 0 seconds
 * - Every 5 minutes
 * - Every hour
 * - Every day of month
 * - Every month
 * - Any day of week
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ocms41SyncFurnishedScheduler {

    private final Ocms41SyncFurnishedJob syncJob;

    @Value("${ocms41.sync.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Scheduled sync job - runs every 5 minutes by default
     * Can be configured via application properties:
     * - ocms41.sync.enabled: Enable/disable the scheduler (default: true)
     * - cron.ocms41.sync.furnished.schedule: Cron expression for schedule
     * - cron.ocms41.sync.furnished.shedlock.name: ShedLock name for distributed lock
     */
    @Scheduled(cron = "${cron.ocms41.sync.furnished.schedule:0 */5 * * * ?}")
    @SchedulerLock(
        name = "${cron.ocms41.sync.furnished.shedlock.name:ocms41_sync_furnished}",
        lockAtLeastFor = "PT2M",      // Lock for at least 2 minutes
        lockAtMostFor = "PT10M"       // Maximum lock time 10 minutes
    )
    public void syncFurnishedSubmissions() {
        if (!schedulerEnabled) {
            log.debug("OCMS 41 Furnished Sync scheduler is disabled");
            return;
        }

        log.info("==========================================");
        log.info("Starting scheduled OCMS 41: Sync Furnished Submissions");
        log.info("Cron job: ocms41_sync_furnished");
        log.info("==========================================");

        try {
            // Execute the sync job
            syncJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled OCMS 41 furnished sync completed successfully: {}", result.getMessage());
                    } else {
                        log.error("Scheduled OCMS 41 furnished sync failed: {}", result.getMessage());
                        // Framework will handle email notification for failures
                    }
                })
                .exceptionally(e -> {
                    log.error("Unexpected error in scheduled OCMS 41 furnished sync: {}", e.getMessage(), e);
                    // Framework will handle email notification for exceptions
                    return null;
                });

        } catch (Exception e) {
            log.error("Error starting scheduled OCMS 41 furnished sync: {}", e.getMessage(), e);
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
        log.info("Manual trigger for OCMS 41 Furnished Sync");
        return syncJob.execute();
    }
}
