package com.ocmsintranet.cronservice.framework.workflows.daily_reports.services;

import java.util.concurrent.CompletableFuture;

/**
 * OCMS 14 & OCMS 10: Service interface for Daily Report generation jobs
 * Provides async execution methods for:
 * - RIP Hirer/Driver Report (OCMS 14)
 * - Classified Vehicle Report (OCMS 14)
 * - ANS Letter Reconciliation Report (OCMS 10)
 */
public interface DailyReportService {

    /**
     * Execute RIP Hirer/Driver Report generation job asynchronously
     *
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeRipHirerDriverReport();

    /**
     * Execute Classified Vehicle Report generation job asynchronously
     *
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeClassifiedVehicleReport();

    /**
     * Execute RIP Hirer/Driver Report for a specific date (for testing/manual execution)
     *
     * @param reportDate Report date in yyyy-MM-dd format
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeRipHirerDriverReport(String reportDate);

    /**
     * Execute Classified Vehicle Report for a specific date (for testing/manual execution)
     *
     * @param reportDate Report date in yyyy-MM-dd format
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeClassifiedVehicleReport(String reportDate);

    /**
     * Execute ANS Letter Reconciliation Report
     * Triggered when Toppan acknowledgement file is received
     *
     * @param processDate Date of the CSR/ACK files being reconciled (yyyy-MM-dd format)
     * @param ackFileContent Content of the acknowledgement file from Toppan
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeAnsLetterReconciliation(String processDate, String ackFileContent);

    /**
     * Execute DIP/MID/FOR Day-End Re-check (OCMS 19)
     * Runs at 11:59 PM daily to re-apply PS suspensions before stage transition
     *
     * @return CompletableFuture with job result
     */
    CompletableFuture<com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult> executeDipMidForRecheck();
}
