package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.schedulers;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services.LtaDownloadService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for the LTA download job
 */
@Slf4j
@Component
public class LtaDownloadScheduler {

    private final LtaDownloadService ltaDownloadService;
    
    @Value("${cron.lta.download.enabled:false}")
    private boolean enabled;
    
    public LtaDownloadScheduler(LtaDownloadService ltaDownloadService) {
        this.ltaDownloadService = ltaDownloadService;
    }
    
    /**
     * Scheduled execution of the LTA download job
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "${cron.lta.download.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.lta.download.shedlock.name:process_lta_rov_files}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void executeScheduledJob() {
        if (!enabled) {
            log.info("LTA download scheduler is disabled. Skipping execution.");
            return;
        }
        
        log.info("Executing scheduled LTA download job");
        try {
            // Execute returns a CompletableFuture, so we need to get the result
            JobResult result = ltaDownloadService.executeLtaDownloadJob().get();

            if (result.isSuccess()) {
                log.info("Scheduled LTA download job executed successfully: {}", result.getMessage());
            } else {
                log.error("Scheduled LTA download job execution failed: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("Error executing scheduled LTA download job", e);
        }
    }
}
