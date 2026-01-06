package com.ocmsintranet.cronservice.framework.workflows.hstreport.services;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.dto.ReportResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Monthly HST report generation
 */
public interface HstReportService {

    /**
     * Execute the Monthly HST report generation job
     *
     * @return CompletableFuture containing the report result
     */
    CompletableFuture<ReportResult> executeJob();

    /**
     * Generate Monthly HST report for a specific date
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Report result
     */
    ReportResult generateReport(String reportDate);

    /**
     * Queue all HST IDs for MHA/DataHive monthly check
     * Called on 1st of every month
     *
     * @return Number of HST IDs queued
     */
    int queueHstIdsForMonthlyCheck();

    /**
     * Check if MHA/DataHive results are ready and generate reports if available
     *
     * @return Report result if generated, null if no new results
     */
    ReportResult checkAndGenerateReports();
}
