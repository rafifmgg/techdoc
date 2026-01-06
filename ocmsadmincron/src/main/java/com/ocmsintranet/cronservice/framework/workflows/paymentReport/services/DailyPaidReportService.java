package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.DailyPaidReportResult;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Daily Paid Report operations.
 *
 * This service provides methods for generating daily paid reports, both asynchronously
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
 * - Both methods ultimately delegate to the DailyPaidReportJob for the actual report generation
 */
public interface DailyPaidReportService {

    /**
     * Execute the Daily Paid Report generation job.
     * Uses yesterday as the report date.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    CompletableFuture<DailyPaidReportResult> executeJob();

    /**
     * Generate a Daily Paid Report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return DailyPaidReportResult with execution status and processing statistics
     */
    DailyPaidReportResult generateReport(String reportDate);
}
