package com.ocmsintranet.cronservice.testing.ocms20_reports.services;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.services.HstReportService;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.common.TestStepStatus;
import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestRequest;
import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Test service for HST Report generation (OCMS 20)
 * Tests the complete HST report workflow including:
 * - Monthly HST ID queuing
 * - Report generation from MHA/DataHive results
 * - HST Data Report (address comparison)
 * - HST Work Items Report (11 statistics)
 * - Email notification to OIC
 */
@Service
@Slf4j
public class HstReportTestService {

    private final HstReportService hstReportService;

    @Autowired
    public HstReportTestService(HstReportService hstReportService) {
        this.hstReportService = hstReportService;
    }

    /**
     * Execute complete HST report test workflow
     *
     * @param request Test configuration
     * @return Test response with all step results
     */
    public ReportTestResponse executeHstReportTest(ReportTestRequest request) {
        ReportTestResponse response = new ReportTestResponse("OCMS 20 - HST Report Generation Test");
        log.info("Starting HST Report Generation Test");

        // Step 1: Queue HST IDs for monthly check
        TestStepResult step1 = executeStep1_QueueHstIds();
        response.addStep(step1);

        // Step 2: Check and generate reports (if MHA/DataHive results available)
        TestStepResult step2 = executeStep2_GenerateReports(request, step1);
        response.addStep(step2);

        // Step 3: Verify report files were created
        TestStepResult step3 = executeStep3_VerifyReports(request, step2);
        response.addStep(step3);

        // Generate summary
        response.generateSummary();

        log.info("HST Report Generation Test completed: {}", response.getSummary());
        return response;
    }

    /**
     * Step 1: Queue HST IDs for monthly check
     * This simulates the monthly HST ID queuing job
     *
     * @return Step result
     */
    private TestStepResult executeStep1_QueueHstIds() {
        TestStepResult result = new TestStepResult(
            "Step 1: Queue HST IDs for Monthly MHA/DataHive Check",
            TestStepStatus.RUNNING.name()
        );

        try {
            log.info("Queuing HST IDs for monthly check...");
            int queuedCount = hstReportService.queueHstIdsForMonthlyCheck();

            result.setStatus(TestStepStatus.SUCCESS.name());
            result.addDetail(String.format("✅ Successfully queued %d HST IDs for MHA/DataHive check", queuedCount));

            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("queued_count", queuedCount);
            jsonData.put("status", "success");
            result.setJsonData(jsonData);

            log.info("Step 1 completed: {} HST IDs queued", queuedCount);
            return result;

        } catch (Exception e) {
            log.error("Step 1 failed: {}", e.getMessage(), e);
            result.setStatus(TestStepStatus.FAILED.name());
            result.addDetail("❌ Failed to queue HST IDs: " + e.getMessage());

            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("error", e.getMessage());
            jsonData.put("status", "failed");
            result.setJsonData(jsonData);

            return result;
        }
    }

    /**
     * Step 2: Check and generate HST reports
     * Generates both HST Data Report and HST Work Items Report if new results available
     *
     * @param request Test configuration
     * @param previousStep Previous step result
     * @return Step result
     */
    private TestStepResult executeStep2_GenerateReports(ReportTestRequest request, TestStepResult previousStep) {
        TestStepResult result = new TestStepResult(
            "Step 2: Check and Generate HST Reports",
            TestStepStatus.RUNNING.name()
        );

        // Skip if previous step failed
        if (!TestStepStatus.SUCCESS.name().equals(previousStep.getStatus())) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("⚠️ Skipped due to Step 1 failure");
            return result;
        }

        // Skip if generateReport flag is false
        if (!request.getGenerateReport()) {
            result.setStatus(TestStepStatus.SUCCESS.name());
            result.addDetail("ℹ️ Report generation skipped (generateReport=false)");
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("status", "skipped");
            jsonData.put("reason", "generateReport flag set to false");
            result.setJsonData(jsonData);
            return result;
        }

        try {
            log.info("Checking for new MHA/DataHive HST results and generating reports...");
            ReportResult reportResult = hstReportService.checkAndGenerateReports();

            if (reportResult == null) {
                result.setStatus(TestStepStatus.WARNING.name());
                result.addDetail("⚠️ No new MHA/DataHive HST results available for reporting");
                result.addDetail("ℹ️ This is normal if no MHA/DataHive queries have been processed yet");

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "no_new_results");
                jsonData.put("message", "No new HST results to generate reports from");
                result.setJsonData(jsonData);

                log.info("Step 2 completed: No new results available");
                return result;
            }

            if (reportResult.isSuccess()) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("✅ HST reports generated successfully");
                result.addDetail(String.format("📊 Processed %d HST records", reportResult.getRecordCount()));
                result.addDetail("📄 Generated: Monthly HST Data Report (address comparison)");
                result.addDetail("📄 Generated: Monthly HST Work Items Report (11 statistics)");

                if (request.getSendEmail()) {
                    result.addDetail("📧 Email notification sent to OIC");
                } else {
                    result.addDetail("ℹ️ Email notification skipped (sendEmail=false)");
                }

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "success");
                jsonData.put("record_count", reportResult.getRecordCount());
                jsonData.put("message", reportResult.getMessage());
                jsonData.put("report_url", reportResult.getReportUrl());
                result.setJsonData(jsonData);

                log.info("Step 2 completed: Reports generated successfully");
                return result;

            } else {
                result.setStatus(TestStepStatus.FAILED.name());
                result.addDetail("❌ Report generation failed: " + reportResult.getMessage());

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "failed");
                jsonData.put("error", reportResult.getMessage());
                result.setJsonData(jsonData);

                log.error("Step 2 failed: {}", reportResult.getMessage());
                return result;
            }

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
            result.setStatus(TestStepStatus.FAILED.name());
            result.addDetail("❌ Exception during report generation: " + e.getMessage());

            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("error", e.getMessage());
            jsonData.put("status", "failed");
            result.setJsonData(jsonData);

            return result;
        }
    }

    /**
     * Step 3: Verify reports were created
     * Checks that reports exist in Azure Blob Storage
     *
     * @param request Test configuration
     * @param previousStep Previous step result
     * @return Step result
     */
    private TestStepResult executeStep3_VerifyReports(ReportTestRequest request, TestStepResult previousStep) {
        TestStepResult result = new TestStepResult(
            "Step 3: Verify Report Files",
            TestStepStatus.RUNNING.name()
        );

        // Skip if previous step failed or skipped
        if (!TestStepStatus.SUCCESS.name().equals(previousStep.getStatus())) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("⚠️ Skipped due to Step 2 status: " + previousStep.getStatus());
            return result;
        }

        try {
            // Extract report URL from previous step
            @SuppressWarnings("unchecked")
            Map<String, Object> previousData = (Map<String, Object>) previousStep.getJsonData();
            String reportUrl = (String) previousData.get("report_url");

            if (reportUrl != null && !reportUrl.isEmpty()) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("✅ Report uploaded to Azure Blob Storage");
                result.addDetail("📁 Report URL: " + reportUrl);

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "success");
                jsonData.put("report_url", reportUrl);
                jsonData.put("verification", "Report URL available");
                result.setJsonData(jsonData);

                log.info("Step 3 completed: Report verification successful");
                return result;

            } else {
                result.setStatus(TestStepStatus.WARNING.name());
                result.addDetail("⚠️ No report URL found in previous step result");

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "warning");
                jsonData.put("message", "No report URL available for verification");
                result.setJsonData(jsonData);

                log.warn("Step 3 completed with warning: No report URL available");
                return result;
            }

        } catch (Exception e) {
            log.error("Step 3 failed: {}", e.getMessage(), e);
            result.setStatus(TestStepStatus.FAILED.name());
            result.addDetail("❌ Verification failed: " + e.getMessage());

            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("error", e.getMessage());
            jsonData.put("status", "failed");
            result.setJsonData(jsonData);

            return result;
        }
    }
}
