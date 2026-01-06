package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.schedulers;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.jobs.ToppanDownloadJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduler for Toppan download job
 * Runs periodically to check for and process response files from Toppan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToppanDownloadScheduler {
    
    private final ToppanDownloadJob toppanDownloadJob;
    
    @Value("${cron.toppan.download.enabled:false}")
    private boolean schedulerEnabled;
        
    /**
     * Schedule the Toppan download job
     * Default: Run daily at 1:30 AM
     * This can be adjusted based on when Toppan typically sends response files
     */
    @Scheduled(cron = "${cron.toppan.download.schedule:0 30 1 * * ?}")
    @SchedulerLock(name = "${cron.toppan.download.shedlock.name:download_toppan_ack_files}", lockAtLeastFor = "PT1M", lockAtMostFor = "PT55M")
    public void downloadToppanResponses() {
        if (!schedulerEnabled) {
            log.debug("Toppan download scheduler is disabled");
            return;
        }

        log.info("Starting scheduled Toppan download job");
        
        try {
            CompletableFuture<JobResult> futureResult = toppanDownloadJob.execute();
            
            // Wait for completion (with timeout)
            JobResult result = futureResult.get();
            
            if (result.isSuccess()) {
                log.info("Toppan download job completed successfully: {}", result.getMessage());
            } else {
                log.error("Toppan download job failed: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error executing scheduled Toppan download job", e);
        }
    }
    
    /**
     * Trigger manual execution of the download job
     * Used by the controller for on-demand processing
     */
    public CompletableFuture<JobResult> triggerManualExecution() {
        log.info("Manual trigger for Toppan download job");
        return toppanDownloadJob.execute();
    }
}
