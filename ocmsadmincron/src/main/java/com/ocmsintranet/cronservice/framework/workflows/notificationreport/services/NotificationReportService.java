package com.ocmsintranet.cronservice.framework.workflows.notificationreport.services;

import com.ocmsintranet.cronservice.framework.workflows.notificationreport.dto.ReportResult;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for MHA Daily report operations.
 * 
 * This service provides methods for generating Daily reports, both asynchronously
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
 * - Both methods ultimately delegate to the MhaReportJob for the actual report generation
 */
public interface NotificationReportService {
    
    /**
     * Execute the MHA Daily report generation job.
     *
     * @return CompletableFuture with the job result including processing statistics
     */
    CompletableFuture<ReportResult> executeJob();

    /**
     * Generate a Daily report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return ReportResult with execution status and processing statistics
     */
    ReportResult generateReport(String reportDate);
}
