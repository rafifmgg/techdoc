package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.services;

import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.dto.ReportResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Unclaimed Batch Data report generation
 */
public interface UnclaimedReportService {

    /**
     * Execute the Unclaimed Batch Data report generation job
     *
     * @return CompletableFuture containing the report result
     */
    CompletableFuture<ReportResult> executeJob();

    /**
     * Generate Unclaimed Batch Data report for a specific date
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Report result
     */
    ReportResult generateReport(String reportDate);

    /**
     * Check for new MHA/DataHive UNC results and generate Batch Data Report if available
     *
     * @return Report result, or null if no new results available
     */
    ReportResult checkAndGenerateBatchDataReport();
}
