package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.services.ToppanReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Toppan Daily report generation.
 * This scheduler runs daily at midnight to generate Excel reports for Daily filtered data.
 *
 * The Daily report scheduler is responsible for automatically generating daily reports
 * containing information about notices that have been processed with the Daily status.
 * These reports are useful for monitoring and auditing purposes.
 *
 * Features:
 * - Runs automatically at midnight every day (configurable via cron.toppan.report.schedule property)
 * - Can be enabled/disabled via cron.toppan.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel reports with multiple sheets (Summary, Success, Error)
 * - Stores reports in Azure Blob Storage for easy access
 *
 * Configuration properties:
 * - cron.toppan.report.enabled: Enable/disable the scheduler (default: false)
 * - cron.toppan.report.schedule: Cron expression for scheduling (default: 0 0 0 * * ?)
 * - cron.toppan.report.shedlock.name: Name for ShedLock (default: generate_Toppan_tsrov_report)
 * - blob.folder.toppan.report: Azure Blob Storage folder for reports (default: /offence/reports/tsrov/)
 */
@Slf4j
@Component
public class ToppanReportScheduler {

    private final ToppanReportService toppanReportService;
    
    @Value("${cron.toppan.report.enabled:false}")
    private boolean enabled;
    
    public ToppanReportScheduler(ToppanReportService toppanReportService) {
        this.toppanReportService = toppanReportService;
    }
    
    /**
     * Scheduled execution of the Toppan Daily report generation job.
     * The schedule is configured to run at midnight every day.
     */
    @Scheduled(cron = "${cron.toppan.report.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.toppan.report.shedlock.name:generate_Toppan_daily_summary_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Toppan Daily report generation job is disabled. Skipping scheduled execution.");
            return;
        }
        
        log.info("Starting scheduled execution of Toppan Daily report generation job");
        
        toppanReportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Toppan Daily report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Toppan Daily report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Toppan Daily report generation job execution", ex);
                    return null;
                });
    }
}
