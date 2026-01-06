package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.DailyPaidReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.jobs.DailyPaidReportJob;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers.DailyPaidReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the DailyPaidReportService interface.
 */
@Slf4j
@Service
public class DailyPaidReportServiceImpl implements DailyPaidReportService {

    private final DailyPaidReportJob reportJob;
    private final DailyPaidReportHelper reportHelper;

    public DailyPaidReportServiceImpl(
            DailyPaidReportJob reportJob,
            DailyPaidReportHelper reportHelper) {
        this.reportJob = reportJob;
        this.reportHelper = reportHelper;
    }

    /**
     * Execute the Daily Paid Report generation job.
     * Uses yesterday as the report date.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<DailyPaidReportResult> executeJob() {
        log.info("Executing Daily Paid Report generation job");

        // Get yesterday as the default report date
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String reportDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try {
            DailyPaidReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing Daily Paid Report generation job", e);
            DailyPaidReportResult errorResult = new DailyPaidReportResult(
                false,
                "Error executing Daily Paid Report generation job: " + e.getMessage()
            );
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a Daily Paid Report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return DailyPaidReportResult with execution status and processing statistics
     */
    @Override
    public DailyPaidReportResult generateReport(String reportDate) {
        log.info("Generating Daily Paid Report for date: {}", reportDate);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = reportHelper.fetchPaidRecords(reportDate);

            int totalCount = 0;
            int eServiceCount = 0;
            int axsCount = 0;
            int offlineCount = 0;
            int jtcCollectionCount = 0;
            int refundCount = 0;

            if (reportData != null && !reportData.isEmpty()) {
                totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
                eServiceCount = (Integer) reportData.getOrDefault("eServiceCount", 0);
                axsCount = (Integer) reportData.getOrDefault("axsCount", 0);
                offlineCount = (Integer) reportData.getOrDefault("offlineCount", 0);
                jtcCollectionCount = (Integer) reportData.getOrDefault("jtcCollectionCount", 0);
                refundCount = (Integer) reportData.getOrDefault("refundCount", 0);
            }

            // Set the specific report date before execution
            reportJob.setReportDate(reportDate);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                reportJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with statistics and the detailed message from the job
            String resultMessage = jobResult.getMessage() != null ?
                jobResult.getMessage() :
                (success ? "Daily Paid Report generation completed successfully" : "Daily Paid Report generation failed");

            DailyPaidReportResult result = new DailyPaidReportResult(
                success,
                resultMessage,
                totalCount,
                eServiceCount,
                axsCount,
                offlineCount,
                jtcCollectionCount,
                refundCount,
                reportDate
            );

            return result;

        } catch (Exception e) {
            log.error("Error generating Daily Paid Report for date: {}", reportDate, e);
            return new DailyPaidReportResult(
                false,
                "Error generating Daily Paid Report: " + e.getMessage()
            );
        }
    }
}
