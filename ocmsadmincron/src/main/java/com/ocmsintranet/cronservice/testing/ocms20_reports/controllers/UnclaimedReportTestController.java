package com.ocmsintranet.cronservice.testing.ocms20_reports.controllers;

import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestRequest;
import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestResponse;
import com.ocmsintranet.cronservice.testing.ocms20_reports.services.UnclaimedReportTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller for Unclaimed Batch Data Report generation (OCMS 20)
 *
 * Provides REST endpoints to test the complete UNC report workflow:
 * 1. Check for unprocessed UNC results from MHA/DataHive
 * 2. Generate 15-column Excel report
 * 3. Upload to Azure Blob Storage
 * 4. Mark results as processed
 *
 * Usage:
 * POST /test/ocms20/unclaimed-report
 * Body: {
 *   "generateReport": true,
 *   "uploadToBlob": true,
 *   "details": true
 * }
 *
 * Response: Complete test results with all steps
 */
@RestController
@RequestMapping("/test/ocms20")
@Slf4j
public class UnclaimedReportTestController {

    private final UnclaimedReportTestService unclaimedReportTestService;

    @Autowired
    public UnclaimedReportTestController(UnclaimedReportTestService unclaimedReportTestService) {
        this.unclaimedReportTestService = unclaimedReportTestService;
    }

    /**
     * Test Unclaimed Batch Data Report generation workflow
     *
     * This endpoint tests:
     * - Checking for unprocessed UNC results from MHA/DataHive
     * - Generating 15-column Excel report from ocms_temp_unc_hst_addr
     * - Uploading report to Azure Blob Storage
     * - Marking UNC results as processed
     *
     * @param request Test configuration (optional - defaults to all true)
     * @return Test response with all step results
     */
    @PostMapping("/unclaimed-report")
    public ResponseEntity<ReportTestResponse> testUnclaimedReport(
            @RequestBody(required = false) ReportTestRequest request) {

        log.info("Received POST /test/ocms20/unclaimed-report");

        // Default to full test if no request body provided
        if (request == null) {
            request = new ReportTestRequest(true, true, false, true);  // sendEmail N/A for UNC
        }

        log.info("Test configuration: generateReport={}, uploadToBlob={}, details={}",
            request.getGenerateReport(), request.getUploadToBlob(), request.getDetails());

        ReportTestResponse response = unclaimedReportTestService.executeUnclaimedReportTest(request);

        log.info("Unclaimed Batch Data Report test completed: {}", response.getSummary());
        return ResponseEntity.ok(response);
    }

    /**
     * Get test endpoint information
     *
     * @return Endpoint documentation
     */
    @GetMapping("/unclaimed-report/info")
    public ResponseEntity<String> getTestInfo() {
        String info = """
            OCMS 20 - Unclaimed Batch Data Report Test Endpoint
            ====================================================

            Endpoint: POST /test/ocms20/unclaimed-report

            Purpose: Test the Unclaimed Batch Data report generation workflow

            Request Body (all optional, default true):
            {
              "generateReport": true,   // Generate the report
              "uploadToBlob": true,     // Upload to Azure Blob Storage
              "details": true           // Show detailed results
            }

            Test Steps:
            1. Check for unprocessed UNC results from MHA/DataHive
            2. Generate 15-column Excel report if results available
            3. Verify report was uploaded to Azure Blob Storage

            Report Columns (15 total):
            1. S/N
            2. Notice Number
            3. Offender ID
            4. ID Type
            5. Block/House No
            6. Street Name
            7. Floor No
            8. Unit No
            9. Building Name
            10. Postal Code
            11. Address Type
            12. Invalid Address Tag
            13. Last Change Date
            14. Response Date
            15. Query Reason

            Expected Behavior:
            - If no new UNC results: Returns WARNING status
            - If results available: Generates report and uploads to Azure
            - Results marked as processed (report_generated = 'Y')
            - Report uploaded to: /offence/reports/unclaimed/ in Azure Blob

            Note: This test uses REAL data and generates ACTUAL reports.
            After running, the UNC results will be marked as processed.
            """;

        return ResponseEntity.ok(info);
    }
}
