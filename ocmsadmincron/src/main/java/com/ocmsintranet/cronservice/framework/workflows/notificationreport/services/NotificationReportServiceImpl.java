package com.ocmsintranet.cronservice.framework.workflows.notificationreport.services;

import com.ocmsintranet.cronservice.framework.workflows.notificationreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.notificationreport.jobs.NotificationReportJob;
import com.ocmsintranet.cronservice.framework.workflows.notificationreport.helpers.NotificationReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the NotificationReportService interface.
 * This service handles eNotification report generation operations.
 */
@Slf4j
@Service
public class NotificationReportServiceImpl implements NotificationReportService {

    private final NotificationReportJob notificationReportJob;
    private final NotificationReportHelper notificationReportHelper;

    public NotificationReportServiceImpl(NotificationReportJob notificationReportJob, NotificationReportHelper notificationReportHelper) {
        this.notificationReportJob = notificationReportJob;
        this.notificationReportHelper = notificationReportHelper;
    }

    /**
     * Execute the eNotification report generation job.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Executing eNotification report generation job");

        // Get yesterday's date as the default report date
        String reportDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            ReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing eNotification report generation job", e);
            ReportResult errorResult = new ReportResult(false, "Error executing eNotification report generation job: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate an eNotification report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ReportResult with execution status and processing statistics
     */
    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Generating eNotification report for date: {}", reportDate);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = notificationReportHelper.fetchNotificationReportsData(reportDate);

            int totalCount = 0;
            int successCount = 0;
            int errorCount = 0;

            if (reportData != null && !reportData.isEmpty()) {
                totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
                successCount = (Integer) reportData.getOrDefault("successCount", 0);
                errorCount = (Integer) reportData.getOrDefault("errorCount", 0);
            }

            // Set the specific report date before execution
            notificationReportJob.setReportDate(reportDate);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                notificationReportJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with statistics
            ReportResult result = new ReportResult(success,
                success ? "eNotification report generation completed successfully" : "eNotification report generation failed",
                totalCount, successCount, errorCount, reportDate);

            return result;

        } catch (Exception e) {
            log.error("Error generating eNotification report for date: {}", reportDate, e);
            return new ReportResult(false, "Error generating eNotification report: " + e.getMessage());
        }
    }
}
