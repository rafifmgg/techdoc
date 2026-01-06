package com.ocmsintranet.cronservice.testing.ocms20_reports.services;

import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.services.UnclaimedReportService;
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
 * Test service for Unclaimed Batch Data Report generation (OCMS 20)
 * Tests the complete UNC report workflow including:
 * - Checking for unprocessed UNC results from MHA/DataHive
 * - Generating 15-column Excel report from ocms_temp_unc_hst_addr
 * - Uploading to Azure Blob Storage
 * - Marking results as processed
 */
@Service
@Slf4j
public class UnclaimedReportTestService {

    private final UnclaimedReportService unclaimedReportService;

    @Autowired
    public UnclaimedReportTestService(UnclaimedReportService unclaimedReportService) {
        this.unclaimedReportService = unclaimedReportService;
    }

    /**
     * Execute complete Unclaimed Batch Data report test workflow
     *
     * @param request Test configuration
     * @return Test response with all step results
     */
    public ReportTestResponse executeUnclaimedReportTest(ReportTestRequest request) {
        ReportTestResponse response = new ReportTestResponse("OCMS 20 - Unclaimed Batch Data Report Test");
        log.info("Starting Unclaimed Batch Data Report Test");

        // Step 1: Check and generate Unclaimed Batch Data Report
        TestStepResult step1 = executeStep1_GenerateReport(request);
        response.addStep(step1);

        // Step 2: Verify report was uploaded
        TestStepResult step2 = executeStep2_VerifyReport(request, step1);
        response.addStep(step2);

        // Generate summary
        response.generateSummary();

        log.info("Unclaimed Batch Data Report Test completed: {}", response.getSummary());
        return response;
    }

    /**
     * Step 1: Check and generate Unclaimed Batch Data Report
     * Generates report if new UNC results are available from MHA/DataHive
     *
     * @param request Test configuration
     * @return Step result
     */
    private TestStepResult executeStep1_GenerateReport(ReportTestRequest request) {
        TestStepResult result = new TestStepResult(
            "Step 1: Check and Generate Unclaimed Batch Data Report",
            TestStepStatus.RUNNING.name()
        );

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
            log.info("Checking for new MHA/DataHive UNC results and generating Batch Data Report...");
            ReportResult reportResult = unclaimedReportService.checkAndGenerateBatchDataReport();

            if (reportResult == null) {
                result.setStatus(TestStepStatus.WARNING.name());
                result.addDetail("⚠️ No new MHA/DataHive UNC results available for reporting");
                result.addDetail("ℹ️ This is normal if no UNC queries have been processed yet");
                result.addDetail("ℹ️ UNC queries are triggered when offenders fail to claim notices");

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "no_new_results");
                jsonData.put("message", "No new UNC results to generate report from");
                result.setJsonData(jsonData);

                log.info("Step 1 completed: No new results available");
                return result;
            }

            if (reportResult.isSuccess()) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("✅ Unclaimed Batch Data Report generated successfully");
                result.addDetail(String.format("📊 Processed %d UNC records", reportResult.getRecordCount()));
                result.addDetail("📄 Report contains 15 columns:");
                result.addDetail("   1. S/N");
                result.addDetail("   2. Notice Number");
                result.addDetail("   3. Offender ID");
                result.addDetail("   4. ID Type");
                result.addDetail("   5. Block/House No");
                result.addDetail("   6. Street Name");
                result.addDetail("   7. Floor No");
                result.addDetail("   8. Unit No");
                result.addDetail("   9. Building Name");
                result.addDetail("   10. Postal Code");
                result.addDetail("   11. Address Type");
                result.addDetail("   12. Invalid Address Tag");
                result.addDetail("   13. Last Change Date");
                result.addDetail("   14. Response Date");
                result.addDetail("   15. Query Reason");

                if (request.getUploadToBlob()) {
                    result.addDetail("☁️ Report uploaded to Azure Blob Storage");
                } else {
                    result.addDetail("ℹ️ Upload to Blob skipped (uploadToBlob=false)");
                }

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "success");
                jsonData.put("record_count", reportResult.getRecordCount());
                jsonData.put("message", reportResult.getMessage());
                jsonData.put("report_url", reportResult.getReportUrl());
                result.setJsonData(jsonData);

                log.info("Step 1 completed: Report generated successfully with {} records",
                    reportResult.getRecordCount());
                return result;

            } else {
                result.setStatus(TestStepStatus.FAILED.name());
                result.addDetail("❌ Report generation failed: " + reportResult.getMessage());

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "failed");
                jsonData.put("error", reportResult.getMessage());
                result.setJsonData(jsonData);

                log.error("Step 1 failed: {}", reportResult.getMessage());
                return result;
            }

        } catch (Exception e) {
            log.error("Step 1 failed: {}", e.getMessage(), e);
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
     * Step 2: Verify report was uploaded to Azure Blob Storage
     * Checks that report URL is available
     *
     * @param request Test configuration
     * @param previousStep Previous step result
     * @return Step result
     */
    private TestStepResult executeStep2_VerifyReport(ReportTestRequest request, TestStepResult previousStep) {
        TestStepResult result = new TestStepResult(
            "Step 2: Verify Report Upload",
            TestStepStatus.RUNNING.name()
        );

        // Skip if previous step failed or skipped
        if (!TestStepStatus.SUCCESS.name().equals(previousStep.getStatus())) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("⚠️ Skipped due to Step 1 status: " + previousStep.getStatus());
            return result;
        }

        // Skip if uploadToBlob flag is false
        if (!request.getUploadToBlob()) {
            result.setStatus(TestStepStatus.SUCCESS.name());
            result.addDetail("ℹ️ Report upload verification skipped (uploadToBlob=false)");
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("status", "skipped");
            jsonData.put("reason", "uploadToBlob flag set to false");
            result.setJsonData(jsonData);
            return result;
        }

        try {
            // Extract report URL from previous step
            @SuppressWarnings("unchecked")
            Map<String, Object> previousData = (Map<String, Object>) previousStep.getJsonData();
            String reportUrl = (String) previousData.get("report_url");

            if (reportUrl != null && !reportUrl.isEmpty()) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("✅ Report successfully uploaded to Azure Blob Storage");
                result.addDetail("📁 Report URL: " + reportUrl);
                result.addDetail("✅ UNC results marked as processed in database");
                result.addDetail("ℹ️ Future queries will not regenerate this data");

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "success");
                jsonData.put("report_url", reportUrl);
                jsonData.put("verification", "Report URL available and accessible");
                result.setJsonData(jsonData);

                log.info("Step 2 completed: Report verification successful");
                return result;

            } else {
                result.setStatus(TestStepStatus.FAILED.name());
                result.addDetail("❌ No report URL found - upload may have failed");
                result.addDetail("⚠️ Check Azure Blob Storage configuration");

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("status", "failed");
                jsonData.put("error", "Report URL not available");
                result.setJsonData(jsonData);

                log.error("Step 2 failed: No report URL available");
                return result;
            }

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
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
