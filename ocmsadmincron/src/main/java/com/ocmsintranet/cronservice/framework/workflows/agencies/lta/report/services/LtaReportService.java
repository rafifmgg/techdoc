package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for LTA report workflow.
 * Provides methods to execute report generation and retrieve report information.
 */
public interface LtaReportService {

    /**
     * Execute the LTA report generation workflow.
     * This will query LTA data, generate various reports,
     * and save them to the configured output location.
     *
     * @return CompletableFuture with the job result
     */
    CompletableFuture<JobResult> executeLtaReportGeneration();

    /**
     * Get report status information.
     *
     * @param reportType Optional report type filter
     * @param reportPeriod Optional report period filter
     * @return Map containing report status information
     */
    Map<String, Object> getReportStatus(String reportType, String reportPeriod);

    /**
     * Get report content for download.
     *
     * @param reportType Report type to retrieve
     * @param reportPeriod Report period to retrieve
     * @return Report content as string, or null if not found
     */
    String getReportContent(String reportType, String reportPeriod);

    /**
     * Get available report options (types and periods).
     *
     * @return Map containing available report types and periods
     */
    Map<String, Object> getAvailableReportOptions();
}