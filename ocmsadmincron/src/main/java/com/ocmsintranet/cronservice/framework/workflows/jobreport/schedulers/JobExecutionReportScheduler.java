package com.ocmsintranet.cronservice.framework.workflows.jobreport.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ocmsintranet.cronservice.framework.workflows.jobreport.jobs.JobExecutionReportJob;

/**
 * Scheduler for the daily job execution report
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "cron.job.execution.report.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class JobExecutionReportScheduler {
    
    private final JobExecutionReportJob jobExecutionReportJob;
    
    /**
     * Scheduled task to run the job execution report
     * Default schedule: Daily at 5:00 PM
     */
    @Scheduled(cron = "${cron.job.execution.report.schedule:0 0 17 * * ?}")
    @SchedulerLock(name = "${cron.job.execution.report.shedlock.name:generate_batch_summary_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void executeScheduledReport() {
        log.info("========== Starting scheduled job execution report ==========");
        
        try {
            jobExecutionReportJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled job execution report completed successfully");
                    } else {
                        log.error("Scheduled job execution report failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error during scheduled job execution report", ex);
                    return null;
                });
        } catch (Exception e) {
            log.error("Error initiating scheduled job execution report", e);
        }
        
        log.info("========== Scheduled job execution report initiated ==========");
    }
}
