package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.ExceptionReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.jobs.PaymentExceptionReportJob;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers.PaymentExceptionReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the PaymentExceptionReportService interface.
 */
@Slf4j
@Service
public class PaymentExceptionReportServiceImpl implements PaymentExceptionReportService {

    private final PaymentExceptionReportJob reportJob;
    private final PaymentExceptionReportHelper reportHelper;

    public PaymentExceptionReportServiceImpl(
            PaymentExceptionReportJob reportJob,
            PaymentExceptionReportHelper reportHelper) {
        this.reportJob = reportJob;
        this.reportHelper = reportHelper;
    }

    /**
     * Execute the Payment Exception Report generation job.
     * Uses yesterday's date as the report date.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    @Override
    public CompletableFuture<ExceptionReportResult> executeJob() {
        log.info("Executing Payment Exception Report generation job");

        // Get yesterday's date as the default report date
        String reportDate = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            ExceptionReportResult result = generateReport(reportDate);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error executing Payment Exception Report generation job", e);
            ExceptionReportResult errorResult = new ExceptionReportResult(
                false,
                "Error executing Payment Exception Report generation job: " + e.getMessage()
            );
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a Payment Exception Report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ExceptionReportResult with execution status and processing statistics
     */
    @Override
    public ExceptionReportResult generateReport(String reportDate) {
        log.info("Generating Payment Exception Report for date: {}", reportDate);

        try {
            // First, fetch data to get processing statistics
            Map<String, Object> reportData = reportHelper.fetchExceptionRecords(reportDate);

            int totalCount = 0;

            if (reportData != null && !reportData.isEmpty()) {
                totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
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
                (success ? "Payment Exception Report generation completed successfully" : "Payment Exception Report generation failed");

            ExceptionReportResult result = new ExceptionReportResult(
                success,
                resultMessage,
                totalCount,
                reportDate
            );

            return result;

        } catch (Exception e) {
            log.error("Error generating Payment Exception Report for date: {}", reportDate, e);
            return new ExceptionReportResult(
                false,
                "Error generating Payment Exception Report: " + e.getMessage()
            );
        }
    }
}
