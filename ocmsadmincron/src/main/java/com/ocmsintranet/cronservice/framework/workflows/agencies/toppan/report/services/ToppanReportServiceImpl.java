package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.services;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.jobs.ToppanReportJob;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.helpers.ToppanReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the ToppanReportService interface.
 * Handles Toppan report file retrieval and download URL generation.
 */
@Slf4j
@Service
public class ToppanReportServiceImpl implements ToppanReportService {

    private final ToppanReportJob toppanReportJob;
    private final ToppanReportHelper toppanReportHelper;

    public ToppanReportServiceImpl(ToppanReportJob toppanReportJob, ToppanReportHelper toppanReportHelper) {
        this.toppanReportJob = toppanReportJob;
        this.toppanReportHelper = toppanReportHelper;
    }

    /**
     * Execute the Toppan report file retrieval job.
     *
     * @return CompletableFuture with the job result including download URL
     */
    @Override
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Executing Toppan report file retrieval job");

        // Get yesterday's date as the default report date
        String reportDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            ReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing Toppan report file retrieval job", e);
            ReportResult errorResult = new ReportResult(false, "Error executing Toppan report file retrieval job: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a report file retrieval for a specific date.
     * Retrieves all 3 Toppan report files and generates download URLs.
     *
     * @param reportDate The date for which to retrieve the report files (format: yyyy-MM-dd)
     * @return ReportResult with execution status and download URLs
     */
    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Retrieving Toppan report files for date: {}", reportDate);

        try {
            // Check if files exist
            Map<String, Object> fileCheckResult = toppanReportHelper.checkReportFileExists(reportDate);
            boolean allFilesFound = (Boolean) fileCheckResult.getOrDefault("allFilesFound", false);
            int filesFound = (Integer) fileCheckResult.getOrDefault("foundCount", 0);

            @SuppressWarnings("unchecked")
            Map<String, String> foundFiles = (Map<String, String>) fileCheckResult.get("foundFiles");

            Map<String, String> downloadUrls = new HashMap<>();
            if (filesFound > 0) {
                // Generate download URLs for found files
                downloadUrls = toppanReportHelper.generateDownloadUrls(reportDate);
            }

            // Set the specific report date before execution
            toppanReportJob.setReportDate(reportDate);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                toppanReportJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with download information
            String message;
            if (success) {
                if (allFilesFound) {
                    message = "Toppan report: all 5 files retrieved successfully";
                } else if (filesFound > 0) {
                    message = String.format("Toppan report: only %d out of 5 files found", filesFound);
                } else {
                    message = "Toppan report files not found for the specified date";
                }
            } else {
                message = "Toppan report file retrieval failed";
            }

            ReportResult result = new ReportResult(success, message, reportDate, downloadUrls, foundFiles);
            return result;

        } catch (Exception e) {
            log.error("Error retrieving Toppan report files for date: {}", reportDate, e);
            return new ReportResult(false, "Error retrieving Toppan report files: " + e.getMessage());
        }
    }
}
