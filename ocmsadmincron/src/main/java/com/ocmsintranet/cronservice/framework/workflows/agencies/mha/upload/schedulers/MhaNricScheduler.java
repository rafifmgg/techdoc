package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.jobs.MhaNricUploadJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for MHA NRIC verification jobs.
 * Schedules automatic execution of MHA NRIC upload and download jobs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MhaNricScheduler {

    private final MhaNricUploadJob mhaNricUploadJob;
    
    @Value("${cron.mha.nric.upload.enabled:false}")
    private boolean enabled;
    
    /**
     * Schedule MHA NRIC upload job to run at 05:00 AM every day except Monday.
     * This aligns with the MHA data exchange schedule in the guide.
     */
    @Scheduled(cron = "${cron.mha.nric.upload.schedule:0 0 5 * * 2-7}") // Default: 5:00 AM, Tuesday through Sunday
    @SchedulerLock(name = "${cron.mha.nric.upload.shedlock.name:generate_mha_files}")
    public void scheduledUpload() {
        if (!enabled) {
            log.debug("MHA NRIC upload job is disabled");
            return;
        }
        
        log.info("Scheduled execution of MHA NRIC upload job");
        try {
            mhaNricUploadJob.execute();
        } catch (Exception e) {
            log.error("Error in scheduled MHA NRIC upload job: {}", e.getMessage(), e);
        }
    }
}
