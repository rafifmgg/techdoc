package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.services;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.jobs.MhaReportJob;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.helpers.MhaReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the MhaReportService interface.
 */
@Slf4j
@Service
public class MhaReportServiceImpl implements MhaReportService {

    private final MhaReportJob mhaReportJob;
    private final MhaReportHelper mhaReportHelper;

    public MhaReportServiceImpl(MhaReportJob mhaReportJob, MhaReportHelper mhaReportHelper) {
        this.mhaReportJob = mhaReportJob;
        this.mhaReportHelper = mhaReportHelper;
    }

    /**
     * Execute the MHA Daily report generation job.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Executing MHA Daily report generation job");

        // Get yesterday's date as the default report date
        String reportDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            ReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing MHA Daily report generation job", e);
            ReportResult errorResult = new ReportResult(false, "Error executing MHA Daily report generation job: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a Daily report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ReportResult with execution status and processing statistics
     */
    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Generating Daily report for date: {}", reportDate);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = mhaReportHelper.fetchSuspendedNoticesData(reportDate);

            int totalCount = 0;
            int successCount = 0;
            int errorCount = 0;

            if (reportData != null && !reportData.isEmpty()) {
                totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
                successCount = (Integer) reportData.getOrDefault("successCount", 0);
                errorCount = (Integer) reportData.getOrDefault("errorCount", 0);
            }

            // Set the specific report date before execution
            mhaReportJob.setReportDate(reportDate);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                mhaReportJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with statistics and the detailed message from the job
            String resultMessage = jobResult.getMessage() != null ? 
                jobResult.getMessage() : 
                (success ? "MHA Daily report generation completed successfully" : "MHA Daily report generation failed");
                
            ReportResult result = new ReportResult(success, resultMessage,
                totalCount, successCount, errorCount, reportDate);

            return result;

        } catch (Exception e) {
            log.error("Error generating Daily report for date: {}", reportDate, e);
            return new ReportResult(false, "Error generating MHA Daily report: " + e.getMessage());
        }
    }
}
