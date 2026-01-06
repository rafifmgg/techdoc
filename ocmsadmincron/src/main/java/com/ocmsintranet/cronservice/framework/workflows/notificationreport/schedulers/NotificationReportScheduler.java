package com.ocmsintranet.cronservice.framework.workflows.notificationreport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.notificationreport.services.NotificationReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for eNotification report generation.
 * This scheduler runs daily at 3 AM to generate Excel reports for eNotification data.
 *
 * The eNotification report scheduler is responsible for automatically generating daily reports
 * containing information about SMS and email notifications that have been sent.
 * These reports are useful for monitoring and auditing notification delivery.
 *
 * Features:
 * - Runs automatically at 3 AM every day (configurable via cron.enotif.report.schedule property)
 * - Can be enabled/disabled via cron.enotif.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel reports with multiple sheets (Summary, Success, Error)
 * - Stores reports in Azure Blob Storage for easy access
 *
 * Configuration properties:
 * - cron.enotif.report.enabled: Enable/disable the scheduler (default: false)
 * - cron.enotif.report.schedule: Cron expression for scheduling (default: 0 0 3 * * ?)
 * - cron.enotif.report.shedlock.name: Name for ShedLock (default: generate_enotif_daily_summary_rpt)
 * - blob.folder.enotif.report: Azure Blob Storage folder for reports (default: /offence/reports/enotification/)
 */
@Slf4j
@Component
public class NotificationReportScheduler {

    private final NotificationReportService notificationReportService;
    
    @Value("${cron.enareminder.report.enabled:false}")
    private boolean enabled;
    
    public NotificationReportScheduler(NotificationReportService notificationReportService) {
        this.notificationReportService = notificationReportService;
    }
    
    /**
     * Scheduled execution of the eNotification report generation job.
     * The schedule is configured to run at 3 AM every day.
     */
    @Scheduled(cron = "${cron.enareminder.report.schedule:0 0 1 * * ?}")
    @SchedulerLock(name = "${cron.enareminder.report.shedlock.name:generate_ena_daily_summary_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("eNotification report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of eNotification report generation job");

        notificationReportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled eNotification report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled eNotification report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled eNotification report generation job execution", ex);
                    return null;
                });
    }
}
