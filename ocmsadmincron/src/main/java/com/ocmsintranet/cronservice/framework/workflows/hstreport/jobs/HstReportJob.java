package com.ocmsintranet.cronservice.framework.workflows.hstreport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.helpers.HstReportHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * CRON Job for Monthly HST Report Generation
 * Based on OCMS 20 Specification
 *
 * This job:
 * 1. Queries all HST records from ocms_hst
 * 2. Joins with ocms_suspended_notice to get TS-HST suspension details
 * 3. Generates Excel report with 10 columns
 * 4. Uploads report to Azure Blob Storage
 * 5. Runs on 1st of every month at midnight
 *
 * Extends CronJobTemplate to follow the standard CRON job pattern.
 */
@Slf4j
@Component
public class HstReportJob extends TrackedCronJobTemplate {

    private final HstReportHelper reportHelper;

    private Map<String, String> jobMetadata;
    private String reportDate;
    private int recordCount;

    @Autowired
    public HstReportJob(HstReportHelper reportHelper) {
        this.reportHelper = reportHelper;
    }

    @Override
    protected String getJobName() {
        return "MonthlyHSTReportJob";
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("[{}] Validating pre-conditions", getJobName());

        try {
            // Check if there are any HST records to report
            int count = reportHelper.getHstRecordsCount();
            log.info("[{}] Found {} HST records for reporting", getJobName(), count);

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
        jobMetadata.put("reportType", "Monthly HST Report");

        log.info("[{}] Job initialized for report date: {}", getJobName(), reportDate);
    }

    @Override
    protected JobResult doExecute() {
        log.info("[{}] Executing job for report date: {}", getJobName(), reportDate);

        try {
            // Fetch monthly HST data
            log.info("[{}] Step 1: Fetching monthly HST data", getJobName());
            Map<String, Object> reportData = reportHelper.fetchMonthlyHstData();

            if (reportData.isEmpty()) {
                log.warn("[{}] No HST data found for report", getJobName());
                jobMetadata.put("recordCount", "0");
                jobMetadata.put("status", "NO_DATA");
                return new JobResult(true, "No HST data found for report");
            }

            recordCount = (int) reportData.getOrDefault("totalCount", 0);
            jobMetadata.put("recordCount", String.valueOf(recordCount));
            log.info("[{}] Found {} HST records", getJobName(), recordCount);

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
                String.format("Monthly HST report generated successfully with %d records", recordCount)
            );

        } catch (Exception e) {
            log.error("[{}] Job execution failed: {}", getJobName(), e.getMessage(), e);
            jobMetadata.put("status", "FAILED");
            jobMetadata.put("errorMessage", e.getMessage());

            return new JobResult(
                false,
                "Failed to generate Monthly HST report: " + e.getMessage()
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
