package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.services.UnclaimedReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Unclaimed Batch Data report generation.
 * This scheduler runs daily at 2 AM to generate Excel reports for unclaimed reminders.
 *
 * The Unclaimed Batch Data report scheduler is responsible for automatically generating daily reports
 * containing information about notices with active TS-UNC suspensions.
 * These reports are useful for monitoring unclaimed reminder letters.
 *
 * Features:
 * - Runs automatically at 2 AM every day (configurable via cron.unclaimed.report.schedule property)
 * - Can be enabled/disabled via cron.unclaimed.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel reports with 11 columns showing unclaimed reminder details
 * - Stores reports in Azure Blob Storage for easy access
 *
 * Configuration properties:
 * - cron.unclaimed.report.enabled: Enable/disable the scheduler (default: false)
 * - cron.unclaimed.report.schedule: Cron expression for scheduling (default: 0 0 2 * * ?)
 * - cron.unclaimed.report.shedlock.name: Name for ShedLock (default: generate_unclaimed_batch_data_rpt)
 * - blob.folder.unclaimed.report: Azure Blob Storage folder for reports (default: /offence/reports/unclaimed/)
 */
@Slf4j
@Component
public class UnclaimedReportScheduler {

    private final UnclaimedReportService unclaimedReportService;

    @Value("${cron.unclaimed.report.enabled:false}")
    private boolean enabled;

    public UnclaimedReportScheduler(UnclaimedReportService unclaimedReportService) {
        this.unclaimedReportService = unclaimedReportService;
    }

    /**
     * Scheduled execution of the Unclaimed Batch Data report generation job.
     * The schedule is configured to run at 2 AM every day.
     */
    @Scheduled(cron = "${cron.unclaimed.report.schedule:0 0 2 * * ?}")
    @SchedulerLock(name = "${cron.unclaimed.report.shedlock.name:generate_unclaimed_batch_data_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Unclaimed Batch Data report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Unclaimed Batch Data report generation job");

        unclaimedReportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Unclaimed Batch Data report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Unclaimed Batch Data report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Unclaimed Batch Data report generation job execution", ex);
                    return null;
                });
    }
}
