package com.ocmsintranet.cronservice.framework.workflows.paymentReport.services;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.MonthlyPaidReportResult;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Monthly Paid Report operations.
 *
 * This service provides methods for generating monthly paid reports, both asynchronously
 * and synchronously. It serves as the main entry point for report generation operations
 * and is used by both the scheduler and the REST controller.
 *
 * The service is responsible for:
 * 1. Executing the report generation job asynchronously (used by the scheduler)
 * 2. Generating reports for specific months on demand (used by the REST controller)
 *
 * Implementation notes:
 * - The executeJob method returns a CompletableFuture to support asynchronous execution
 * - The generateReport method is synchronous and returns immediately with the result
 * - Both methods ultimately delegate to the MonthlyPaidReportJob for the actual report generation
 */
public interface MonthlyPaidReportService {

    /**
     * Execute the Monthly Paid Report generation job.
     * Uses previous month as the report month.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    CompletableFuture<MonthlyPaidReportResult> executeJob();

    /**
     * Generate a Monthly Paid Report for a specific month.
     *
     * @param yearMonth The month for which to generate the report (format: yyyy-MM)
     * @return MonthlyPaidReportResult with execution status and processing statistics
     */
    MonthlyPaidReportResult generateReport(String yearMonth);
}
