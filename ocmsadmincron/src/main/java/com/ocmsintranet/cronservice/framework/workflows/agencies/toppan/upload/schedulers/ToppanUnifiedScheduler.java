package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.jobs.ToppanLettersGeneratorJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Unified Toppan Scheduler
 * 
 * This scheduler runs the unified Toppan letters generation job at 0030hr daily
 * as per the high-level flow diagram: generate_toppan_letters
 * 
 * The job processes all 6 stages sequentially:
 * 1. RD1 - First Request for Driver's/Hirer's Details
 * 2. RD2 - Second Request for Driver's/Hirer's Details  
 * 3. RR3 - Final Reminder to Vehicle Owner
 * 4. DN1 - First Reminder to Driver
 * 5. DN2 - Second Reminder to Driver
 * 6. DR3 - Final Reminder to Driver
 * 
 * Each stage includes:
 * - Query DataHive for FIN & UEN enrichment
 * - Generate Toppan enquiry file
 * - Update processing stage
 * 
 * This replaces the individual stage schedulers in ToppanUploadScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToppanUnifiedScheduler {
    
    private final ToppanLettersGeneratorJob toppanLettersGeneratorJob;
    
    @Value("${cron.toppan.upload.enabled:false}")
    private boolean unifiedSchedulerEnabled;
        
    /**
     * Unified schedule for all Toppan stages
     * Runs at 00:30 (0030hr) daily as per the flow diagram
     * 
     * The cron expression: "0 30 0 * * ?" means:
     * - 0 seconds
     * - 30 minutes
     * - 0 hour (midnight)
     * - Every day of month
     * - Every month
     * - Any day of week
     */
    @Scheduled(cron = "${cron.toppan.upload.schedule:0 30 0 * * ?}")
    @SchedulerLock(
        name = "${cron.toppan.upload.shedlock.name:generate_toppan_letters}", 
        lockAtLeastFor = "PT5M",      // Lock for at least 5 minutes
        lockAtMostFor = "PT3H"        // Maximum lock time 3 hours (all stages)
    )
    public void generateToppanLetters() {
        if (!unifiedSchedulerEnabled) {
            log.debug("Unified Toppan scheduler is disabled");
            return;
        }
        
        log.info("==========================================");
        log.info("Starting scheduled Toppan letters generation at 0030hr");
        log.info("Cron job: generate_toppan_letters");
        log.info("==========================================");
        
        try {
            // Execute the unified job that processes all stages
            toppanLettersGeneratorJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Toppan letters generation completed successfully: {}", 
                            result.getMessage());
                    } else {
                        log.error("Scheduled Toppan letters generation failed: {}", 
                            result.getMessage());
                        // Framework will handle email notification for failures
                    }
                })
                .exceptionally(e -> {
                    log.error("Unexpected error in scheduled Toppan letters generation: {}", 
                        e.getMessage(), e);
                    // Framework will handle email notification for exceptions
                    return null;
                });
                
        } catch (Exception e) {
            log.error("Error starting scheduled Toppan letters generation: {}", e.getMessage(), e);
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
        log.info("Manual trigger for unified Toppan letters generation");
        return toppanLettersGeneratorJob.execute();
    }
}