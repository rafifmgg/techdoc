package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.services.MhaNricDownloadService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for MHA NRIC download operations.
 */
@Slf4j
@Component
public class MhaNricDownloadScheduler {

    private final MhaNricDownloadService mhaNricDownloadService;
    
    @Value("${cron.mha.download.enabled:false}")
    private boolean enabled;
    
    public MhaNricDownloadScheduler(MhaNricDownloadService mhaNricDownloadService) {
        this.mhaNricDownloadService = mhaNricDownloadService;
    }
    
    /**
     * Scheduled execution of the MHA NRIC download job.
     * The schedule is configured in cron-schedules.properties.
     */
    @Scheduled(cron = "${cron.mha.download.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.mha.download.shedlock.name:process_mha_files}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("MHA NRIC download job is disabled. Skipping scheduled execution.");
            return;
        }
        
        log.info("Starting scheduled execution of MHA NRIC download job");

        mhaNricDownloadService.executeJob()
                .thenAccept(result -> {
                    if (result) {
                        log.info("Scheduled MHA NRIC download job completed successfully");
                    } else {
                        log.error("Scheduled MHA NRIC download job failed");
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled MHA NRIC download job execution", ex);
                    return null;
                });
    }
}
