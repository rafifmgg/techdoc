package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.jobs.LtaReportJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for LTA report generation jobs.
 * Schedules automatic execution of LTA report generation workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LtaReportScheduler {

    private final LtaReportJob ltaReportJob;

    @Value("${cron.lta.report.enabled:false}")
    private boolean enabled;

    /**
     * Schedule LTA report generation job to run at 03:00 AM every day.
     * This runs after the upload jobs to ensure latest data is included.
     */
    @Scheduled(cron = "${cron.lta.report.schedule:0 0 2 * * ?}") // Default: 2:00 AM, every day
    @SchedulerLock(name = "${cron.lta.report.shedlock.name:generate_lta_daily_summary_rpt}")
    public void scheduledReportGeneration() {
        if (!enabled) {
            log.debug("LTA report generation job is disabled");
            return;
        }

        log.info("Scheduled execution of LTA report generation job using framework");
        try {
            ltaReportJob.execute()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled LTA report generation job completed successfully: {}", result.getMessage());
                    } else {
                        log.error("Scheduled LTA report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled LTA report generation job execution", ex);
                    return null;
                });
        } catch (Exception e) {
            log.error("Error in scheduled LTA report generation job: {}", e.getMessage(), e);
        }
    }

}