package com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.services.DatahiveContactInfoService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Datahive Contact Information report generation.
 * This scheduler runs daily at 1 AM to generate Excel reports for Datahive Contact Information data.
 *
 * The Datahive Contact Information report scheduler is responsible for automatically generating daily reports
 * containing information about SMS and email notifications that have been sent.
 * These reports are useful for monitoring and auditing notification delivery.
 *
 * Features:
 * - Runs automatically at 1 AM every day (configurable via cron.enotif.report.schedule property)
 * - Can be enabled/disabled via cron.enotif.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel reports with multiple sheets (Summary, Success, Error)
 * - Stores reports in Azure Blob Storage for easy access
 *
 * Configuration properties:
 * - cron.enotif.report.enabled: Enable/disable the scheduler (default: false)
 * - cron.enotif.report.schedule: Cron expression for scheduling (default: 0 0 1 * * ?)
 * - cron.enotif.report.shedlock.name: Name for ShedLock (default: generate_dhinfo_daily_summary_rpt)
 */
@Slf4j
@Component
public class DatahiveContactInfoScheduler {

    private final DatahiveContactInfoService datahiveContactInfoService;
    
    @Value("${cron.dhcontactinfo.report.enabled:false}")
    private boolean enabled;
    
    public DatahiveContactInfoScheduler(DatahiveContactInfoService datahiveContactInfoService) {
        this.datahiveContactInfoService = datahiveContactInfoService;
    }
    
    /**
     * Scheduled execution of the Datahive Contact Information report generation job.
     * The schedule is configured to run at 1 AM every day.
     */
    @Scheduled(cron = "${cron.dhcontactinfo.report.schedule:0 0 1 * * ?}")
    @SchedulerLock(name = "${cron.dhcontactinfo.report.shedlock.name:generate_dhcontactinfo_daily_summary_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Datahive Contact Information report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Datahive Contact Information report generation job");

        datahiveContactInfoService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Datahive Contact Information report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Datahive Contact Information report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Datahive Contact Information report generation job execution", ex);
                    return null;
                });
    }
}
