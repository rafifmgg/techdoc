package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.helpers.LtaReportHelper;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.dto.LtaReportData;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LTA VRLS Vehicle Ownership Check Report Generation Job
 *
 * High-level workflow:
 * 1. Query yesterday's LTA VRLS processing records
 * 2. Check if TS-ROV records exist (conditional generation)
 * 3. Generate multi-sheet Excel report if TS-ROV records found
 * 4. Send email notification with Excel attachment
 *
 * Business Rule: Only generate report if TS-ROV records exist for yesterday
 */
@Slf4j
@Component
public class LtaReportJob extends TrackedCronJobTemplate {

    private final LtaReportHelper ltaReportHelper;
    private Map<String, String> jobMetadata = new HashMap<>();

    @Value("${cron.lta.report.shedlock.name:generate_lta_daily_summary_rpt}")
    private String jobName;

    // Report date for the current execution
    private String reportDate;

    /**
     * Set the report date for this job execution
     * This allows overriding the default yesterday's date
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
        log.info("Report date manually set to: {}", reportDate);
    }

    public LtaReportJob(LtaReportHelper ltaReportHelper) {
        this.ltaReportHelper = ltaReportHelper;
    }

    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for LTA VRLS report job");
        return ltaReportHelper != null;
    }

    @Override
    protected void initialize() {
        log.info("Initializing LTA VRLS Report Job");
        jobMetadata.clear();
        jobMetadata.put("jobStartTime", LocalDateTime.now().toString());

        // Set report date to yesterday by default if not already set
        if (reportDate == null || reportDate.isEmpty()) {
            reportDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("Report date set to default (yesterday): {}", reportDate);
        } else {
            log.info("Using manually set report date: {}", reportDate);
        }

        // Call parent initialize to trigger batchjob tracking
        super.initialize();
    }

    @Override
    protected void cleanup() {
        log.info("LTA VRLS report job cleanup completed");
    }

    @Override
    protected JobResult doExecute() {
        log.info("Starting LTA VRLS Report Generation - checking records for: {}", reportDate);

        try {
            // Step 1: Query LTA VRLS processing records for the specified date
            LocalDate targetDate = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            List<LtaReportData> yesterdayRecords = ltaReportHelper.queryYesterdayRecords(targetDate);

            if (yesterdayRecords.isEmpty()) {
                log.info("No LTA VRLS records found for {}, job completed with no action", targetDate);
                return new JobResult(true, String.format("No LTA VRLS records found for %s. Metrics: totalRecords: 0, tsRovRecords: 0", targetDate));
            }

            log.info("Found {} LTA VRLS records for {}", yesterdayRecords.size(), targetDate);
            recordJobMetadata("totalRecords", String.valueOf(yesterdayRecords.size()));
            recordJobMetadata("reportDate", targetDate.toString());

            // Step 2: Check if TS-ROV records exist (Critical Business Rule)
            List<LtaReportData> tsRovRecords = ltaReportHelper.filterTsRovRecords(yesterdayRecords);

            if (tsRovRecords.isEmpty()) {
                log.info("No TS-ROV records found for {}, skipping report generation", targetDate);
                return new JobResult(true, String.format("No TS-ROV records found for %s - report generation skipped. Metrics: totalRecords: %d, tsRovRecords: 0",
                    targetDate, yesterdayRecords.size()));
            }

            log.info("Found {} TS-ROV records for {} - proceeding with report generation", tsRovRecords.size(), targetDate);
            recordJobMetadata("tsRovRecords", String.valueOf(tsRovRecords.size()));

            // Step 3: Generate Excel report in memory (delegate to helper)
            byte[] excelReport = ltaReportHelper.generateExcelReport(yesterdayRecords, targetDate);
            recordJobMetadata("excelGenerated", "true");
            recordJobMetadata("excelSize", String.valueOf(excelReport.length));

            // Step 4: Send email notification directly with in-memory Excel (no file upload)
            log.info("Sending email with in-memory Excel attachment (no blob upload)");
            boolean emailSent = ltaReportHelper.sendEmailWithExcelAttachment(excelReport, targetDate, yesterdayRecords.size());
            recordJobMetadata("emailSent", String.valueOf(emailSent));
            recordJobMetadata("blobUpload", "skipped");

            if (!emailSent) {
                log.warn("Excel report generated but email notification failed");
                return new JobResult(true, String.format("Excel report generated successfully for %d records but email notification failed. Metrics: totalRecords: %d, tsRovRecords: %d, emailSent: 0",
                    yesterdayRecords.size(), yesterdayRecords.size(), tsRovRecords.size()));
            }

            return new JobResult(true, String.format(
                "Successfully generated and emailed Excel report for %d LTA VRLS records (%d TS-ROV records found). Metrics: totalRecords: %d, tsRovRecords: %d, emailSent: 1",
                yesterdayRecords.size(), tsRovRecords.size(), yesterdayRecords.size(), tsRovRecords.size()));

        } catch (Exception e) {
            log.error("Error executing LTA VRLS report job: {}", e.getMessage(), e);
            return new JobResult(false, "Error executing LTA VRLS report job: " + e.getMessage());
        }
    }

    private void recordJobMetadata(String key, String value) {
        jobMetadata.put(key, value);
        log.info("Job Metadata: {} = {}", key, value);
    }

    public Map<String, String> getJobMetadata() {
        return new HashMap<>(jobMetadata);
    }
}