package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.MonthlyPaidReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.jobs.MonthlyPaidReportJob;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers.MonthlyPaidReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the MonthlyPaidReportService interface.
 */
@Slf4j
@Service
public class MonthlyPaidReportServiceImpl implements MonthlyPaidReportService {

    private final MonthlyPaidReportJob reportJob;
    private final MonthlyPaidReportHelper reportHelper;

    public MonthlyPaidReportServiceImpl(
            MonthlyPaidReportJob reportJob,
            MonthlyPaidReportHelper reportHelper) {
        this.reportJob = reportJob;
        this.reportHelper = reportHelper;
    }

    /**
     * Execute the Monthly Paid Report generation job.
     * Uses previous month as the report month.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<MonthlyPaidReportResult> executeJob() {
        log.info("Executing Monthly Paid Report generation job");

        // Get previous month as the default report month
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        String reportMonth = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try {
            MonthlyPaidReportResult result = generateReport(reportMonth);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing Monthly Paid Report generation job", e);
            MonthlyPaidReportResult errorResult = new MonthlyPaidReportResult(
                false,
                "Error executing Monthly Paid Report generation job: " + e.getMessage()
            );
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a Monthly Paid Report for a specific month.
     *
     * @param yearMonth The month for which to generate the report (format: yyyy-MM)
     * @return MonthlyPaidReportResult with execution status and processing statistics
     */
    @Override
    public MonthlyPaidReportResult generateReport(String yearMonth) {
        log.info("Generating Monthly Paid Report for month: {}", yearMonth);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = reportHelper.fetchPaidRecords(yearMonth);

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

            // Set the specific report month before execution
            reportJob.setReportMonth(yearMonth);

            // Execute the actual job using framework's template method
            CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> jobFuture =
                reportJob.execute();
            com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult jobResult = jobFuture.get();
            boolean success = jobResult.isSuccess();

            // Create result with statistics and the detailed message from the job
            String resultMessage = jobResult.getMessage() != null ?
                jobResult.getMessage() :
                (success ? "Monthly Paid Report generation completed successfully" : "Monthly Paid Report generation failed");

            MonthlyPaidReportResult result = new MonthlyPaidReportResult(
                success,
                resultMessage,
                totalCount,
                eServiceCount,
                axsCount,
                offlineCount,
                jtcCollectionCount,
                refundCount,
                yearMonth
            );

            return result;

        } catch (Exception e) {
            log.error("Error generating Monthly Paid Report for month: {}", yearMonth, e);
            return new MonthlyPaidReportResult(
                false,
                "Error generating Monthly Paid Report: " + e.getMessage()
            );
        }
    }
}
