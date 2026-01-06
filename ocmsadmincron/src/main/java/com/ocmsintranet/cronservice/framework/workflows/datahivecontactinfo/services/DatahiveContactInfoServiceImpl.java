package com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.services;

import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.jobs.DatahiveContactInfoJob;
import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.helpers.DatahiveContactInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the NotificationReportService interface.
 * This service handles Datahive Contact Information report generation operations.
 */
@Slf4j
@Service
public class DatahiveContactInfoServiceImpl implements DatahiveContactInfoService {

    private final DatahiveContactInfoJob datahiveContactInfoJob;
    private final DatahiveContactInfoHelper datahiveContactInfoHelper;

    public DatahiveContactInfoServiceImpl(DatahiveContactInfoJob datahiveContactInfoJob, DatahiveContactInfoHelper datahiveContactInfoHelper) {
        this.datahiveContactInfoJob = datahiveContactInfoJob;
        this.datahiveContactInfoHelper = datahiveContactInfoHelper;
    }

    /**
     * Execute the Datahive Contact Information report generation job.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Executing Datahive Contact Information report generation job");

        // Get yesterday's date as the default report date
        String reportDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            ReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing Datahive Contact Information report generation job", e);
            ReportResult errorResult = new ReportResult(false, "Error executing Datahive Contact Information report generation job: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate an Datahive Contact Information report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ReportResult with execution status and processing statistics
     */
    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Generating Datahive Contact Information report for date: {}", reportDate);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = datahiveContactInfoHelper.fetchNotificationReportsData(reportDate);

            int totalCount = 0;
            int successCount = 0;
            int errorCount = 0;

            if (reportData != null && !reportData.isEmpty()) {
                totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
                successCount = (Integer) reportData.getOrDefault("successCount", 0);
                errorCount = (Integer) reportData.getOrDefault("errorCount", 0);
            }

            // Set the specific report date before execution
            datahiveContactInfoJob.setReportDate(reportDate);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                datahiveContactInfoJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with statistics
            ReportResult result = new ReportResult(success,
                success ? "Datahive Contact Information report generation completed successfully" : "Datahive Contact Information report generation failed",
                totalCount, successCount, errorCount, reportDate);

            return result;

        } catch (Exception e) {
            log.error("Error generating Datahive Contact Information report for date: {}", reportDate, e);
            return new ReportResult(false, "Error generating Datahive Contact Information report: " + e.getMessage());
        }
    }
}
