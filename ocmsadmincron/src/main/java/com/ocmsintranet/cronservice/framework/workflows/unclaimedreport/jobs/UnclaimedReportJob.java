package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.helpers.UnclaimedReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * CRON Job for Unclaimed Batch Data Report Generation
 * Based on OCMS 20 Specification
 *
 * This job:
 * 1. Queries all notices with active TS-UNC suspensions
 * 2. Generates Excel report with 11 columns
 * 3. Uploads report to Azure Blob Storage
 *
 * Extends CronJobTemplate to follow the standard CRON job pattern.
 */
@Slf4j
@Component
public class UnclaimedReportJob extends TrackedCronJobTemplate {

    private final UnclaimedReportHelper reportHelper;

    private Map<String, String> jobMetadata;
    private String reportDate;
    private int recordCount;

    @Autowired
    public UnclaimedReportJob(UnclaimedReportHelper reportHelper) {
        this.reportHelper = reportHelper;
    }

    @Override
    protected String getJobName() {
        return "UnclaimedBatchDataReportJob";
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("[{}] Validating pre-conditions", getJobName());

        try {
            // Check if there are any unclaimed notices to report
            int count = reportHelper.getUnclaimedNoticesCount();
            log.info("[{}] Found {} unclaimed notices for reporting", getJobName(), count);

            // Allow job to run even if count is 0 (will generate empty report)
            return true;

        } catch (Exception e) {
            log.error("[{}] Pre-condition validation failed: {}", getJobName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        log.info("[{}] Initializing job", getJobName());

        // Initialize metadata
        jobMetadata = new HashMap<>();
        reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        recordCount = 0;

        jobMetadata.put("reportDate", reportDate);
        jobMetadata.put("jobStartTime", LocalDate.now().toString());

        log.info("[{}] Job initialized for report date: {}", getJobName(), reportDate);
    }

    @Override
    protected JobResult doExecute() {
        log.info("[{}] Executing job for report date: {}", getJobName(), reportDate);

        try {
            // Fetch unclaimed batch data
            log.info("[{}] Step 1: Fetching unclaimed batch data", getJobName());
            Map<String, Object> reportData = reportHelper.fetchUnclaimedBatchData();

            if (reportData.isEmpty()) {
                log.warn("[{}] No unclaimed data found for report", getJobName());
                jobMetadata.put("recordCount", "0");
                jobMetadata.put("status", "NO_DATA");
                return new JobResult(true, "No unclaimed data found for report");
            }

            recordCount = (int) reportData.getOrDefault("totalCount", 0);
            jobMetadata.put("recordCount", String.valueOf(recordCount));
            log.info("[{}] Found {} unclaimed records", getJobName(), recordCount);

            // Generate Excel report
            log.info("[{}] Step 2: Generating Excel report", getJobName());
            byte[] excelBytes = reportHelper.generateExcelReport(reportData, reportDate);
            log.info("[{}] Excel report generated: {} bytes", getJobName(), excelBytes.length);

            jobMetadata.put("reportSize", String.valueOf(excelBytes.length));
            jobMetadata.put("status", "SUCCESS");

            // Note: Upload to blob storage is handled by the service layer
            // This job focuses on data retrieval and Excel generation
            log.info("[{}] Job execution completed successfully", getJobName());

            return new JobResult(
                true,
                String.format("Unclaimed Batch Data report generated successfully with %d records", recordCount)
            );

        } catch (Exception e) {
            log.error("[{}] Job execution failed: {}", getJobName(), e.getMessage(), e);
            jobMetadata.put("status", "FAILED");
            jobMetadata.put("errorMessage", e.getMessage());

            return new JobResult(
                false,
                "Failed to generate Unclaimed Batch Data report: " + e.getMessage()
            );
        }
    }

    @Override
    protected void cleanup() {
        log.info("[{}] Cleaning up job resources", getJobName());

        // Clear temporary data
        jobMetadata = null;
        reportDate = null;
        recordCount = 0;

        log.info("[{}] Cleanup completed", getJobName());
    }

    /**
     * Public method to execute the job (for manual triggering via controller)
     *
     * @return Job execution result
     */
    public JobResult executeManually() {
        if (!validatePreConditions()) {
            return new JobResult(false, "Pre-conditions validation failed");
        }
        initialize();
        JobResult result = doExecute();
        cleanup();
        return result;
    }

    /**
     * Get job metadata for tracking
     *
     * @return Map containing job metadata
     */
    public Map<String, String> getMetadata() {
        return jobMetadata != null ? new HashMap<>(jobMetadata) : new HashMap<>();
    }
}
