package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.services.LtaUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for LTA upload jobs.
 * Schedules automatic execution of LTA upload workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LtaUploadScheduler {

    private final LtaUploadService ltaUploadService;
    
    @Value("${cron.lta.upload.enabled:false}")
    private boolean enabled;
    
    /**
     * Schedule LTA upload job to run at 02:00 AM every day.
     * This aligns with the LTA data exchange schedule.
     */
    @Scheduled(cron = "${cron.lta.upload.schedule:0 0 2 * * *}") // Default: 2:00 AM, every day
    @SchedulerLock(name = "${cron.lta.upload.shedlock.name:generate_lta_rov_files}")
    public void scheduledUpload() {
        if (!enabled) {
            log.debug("LTA upload job is disabled");
            return;
        }
        
        log.info("Scheduled execution of LTA upload job");
        try {
            ltaUploadService.executeLtaUpload();
        } catch (Exception e) {
            log.error("Error in scheduled LTA upload job: {}", e.getMessage(), e);
        }
    }
}
