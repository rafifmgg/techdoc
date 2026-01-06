package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.services.MhaReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for MHA Daily report generation.
 * This scheduler runs daily at midnight to generate Excel reports for Daily filtered data.
 *
 * The Daily report scheduler is responsible for automatically generating daily reports
 * containing information about notices that have been processed with the Daily status.
 * These reports are useful for monitoring and auditing purposes.
 *
 * Features:
 * - Runs automatically at midnight every day (configurable via cron.mha.report.schedule property)
 * - Can be enabled/disabled via cron.mha.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel reports with multiple sheets (Summary, Success, Error)
 * - Stores reports in Azure Blob Storage for easy access
 *
 * Configuration properties:
 * - cron.mha.report.enabled: Enable/disable the scheduler (default: false)
 * - cron.mha.report.schedule: Cron expression for scheduling (default: 0 0 0 * * ?)
 * - cron.mha.report.shedlock.name: Name for ShedLock (default: generate_mha_tsrov_report)
 * - blob.folder.mha.report: Azure Blob Storage folder for reports (default: /offence/reports/tsrov/)
 */
@Slf4j
@Component
public class MhaReportScheduler {

    private final MhaReportService mhaReportService;
    
    @Value("${cron.mha.report.enabled:false}")
    private boolean enabled;
    
    public MhaReportScheduler(MhaReportService mhaReportService) {
        this.mhaReportService = mhaReportService;
    }
    
    /**
     * Scheduled execution of the MHA Daily report generation job.
     * The schedule is configured to run at midnight every day.
     */
    @Scheduled(cron = "${cron.mha.report.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.mha.report.shedlock.name:generate_mha_daily_summary_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("MHA Daily report generation job is disabled. Skipping scheduled execution.");
            return;
        }
        
        log.info("Starting scheduled execution of MHA Daily report generation job");
        
        mhaReportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled MHA Daily report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled MHA Daily report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled MHA Daily report generation job execution", ex);
                    return null;
                });
    }
}
