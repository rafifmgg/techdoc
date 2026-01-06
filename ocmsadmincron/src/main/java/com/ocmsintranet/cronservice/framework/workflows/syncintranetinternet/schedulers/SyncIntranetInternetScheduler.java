package com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.jobs.SyncIntranetInternetJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Process 7: Batch Cron Sync (Intranet to Internet)
 *
 * This scheduler runs periodically to sync records where is_sync = false from:
 * - Intranet VON → Internet eVON
 * - Intranet ONOD → PII eONOD
 *
 * The scheduler handles:
 * 1. Failed immediate syncs from other processes (Process 1, 6)
 * 2. Deferred syncs from processes that don't sync immediately (Process 2-5)
 *
 * Default schedule: Every 5 minutes
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
public class SyncIntranetInternetScheduler {

    private final SyncIntranetInternetJob syncJob;

    @Value("${payment.sync.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Scheduled batch sync job - runs every 5 minutes by default
     * Can be configured via application properties:
     * - payment.sync.enabled: Enable/disable the scheduler (default: true)
     * - cron.sync.intranet.internet.schedule: Cron expression for schedule
     * - cron.sync.intranet.internet.shedlock.name: ShedLock name for distributed lock
     */
    // @Scheduled(cron = "${cron.sync.intranet.internet.schedule:0 */5 * * * ?}")
    // @SchedulerLock(
    //     name = "${cron.sync.intranet.internet.shedlock.name:sync_intranet_to_internet}",
    //     lockAtLeastFor = "PT2M",      // Lock for at least 2 minutes
    //     lockAtMostFor = "PT10M"       // Maximum lock time 10 minutes
    // )
    public void syncIntranetToInternet() {
        if (!schedulerEnabled) {
            log.debug("Batch Cron Sync scheduler is disabled");
            return;
        }

        log.info("==========================================");
        log.info("Starting scheduled Batch Cron Sync (Intranet to Internet)");
        log.info("Cron job: sync_intranet_to_internet");
        log.info("==========================================");

        try {
            // Execute the sync job
            syncJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled batch sync completed successfully: {}", result.getMessage());
                    } else {
                        log.error("Scheduled batch sync failed: {}", result.getMessage());
                        // Framework will handle email notification for failures
                    }
                })
                .exceptionally(e -> {
                    log.error("Unexpected error in scheduled batch sync: {}", e.getMessage(), e);
                    // Framework will handle email notification for exceptions
                    return null;
                });

        } catch (Exception e) {
            log.error("Error starting scheduled batch sync: {}", e.getMessage(), e);
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
        log.info("Manual trigger for Batch Cron Sync");
        return syncJob.execute();
    }
}
