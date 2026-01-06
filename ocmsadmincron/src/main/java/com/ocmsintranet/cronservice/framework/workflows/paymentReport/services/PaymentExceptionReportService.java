package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.ExceptionReportResult;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Payment Exception Report operations.
 *
 * This service provides methods for generating payment exception reports, both asynchronously
 * and synchronously. It serves as the main entry point for report generation operations
 * and is used by both the scheduler and the REST controller.
 *
 * The service is responsible for:
 * 1. Executing the report generation job asynchronously (used by the scheduler)
 * 2. Generating reports for specific dates on demand (used by the REST controller)
 *
 * Implementation notes:
 * - The executeJob method returns a CompletableFuture to support asynchronous execution
 * - The generateReport method is synchronous and returns immediately with the result
 * - Both methods ultimately delegate to the PaymentExceptionReportJob for the actual report generation
 */
public interface PaymentExceptionReportService {

    /**
     * Execute the Payment Exception Report generation job.
     * Uses yesterday's date as the report date.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    CompletableFuture<ExceptionReportResult> executeJob();

    /**
     * Generate a Payment Exception Report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ExceptionReportResult with execution status and processing statistics
     */
    ExceptionReportResult generateReport(String reportDate);
}
