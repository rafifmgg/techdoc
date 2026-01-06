package com.ocmsintranet.cronservice.testing.ocms20_reports.controllers;

import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestRequest;
import com.ocmsintranet.cronservice.testing.ocms20_reports.dto.ReportTestResponse;
import com.ocmsintranet.cronservice.testing.ocms20_reports.services.HstReportTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller for HST Report generation (OCMS 20)
 *
 * Provides REST endpoints to test the complete HST report workflow:
 * 1. Queue HST IDs for monthly MHA/DataHive check
 * 2. Generate reports when results available
 * 3. Verify reports were uploaded to Azure Blob Storage
 *
 * Usage:
 * POST /test/ocms20/hst-report
 * Body: {
 *   "generateReport": true,
 *   "uploadToBlob": true,
 *   "sendEmail": true,
 *   "details": true
 * }
 *
 * Response: Complete test results with all steps
 */
@RestController
@RequestMapping("/test/ocms20")
@Slf4j
public class HstReportTestController {

    private final HstReportTestService hstReportTestService;

    @Autowired
    public HstReportTestController(HstReportTestService hstReportTestService) {
        this.hstReportTestService = hstReportTestService;
    }

    /**
     * Test HST Report generation workflow
     *
     * This endpoint tests:
     * - Queuing HST IDs for monthly MHA/DataHive check
     * - Generating Monthly HST Data Report (address comparison)
     * - Generating Monthly HST Work Items Report (11 statistics)
     * - Uploading reports to Azure Blob Storage
     * - Sending email notification to OIC
     *
     * @param request Test configuration (optional - defaults to all true)
     * @return Test response with all step results
     */
    @PostMapping("/hst-report")
    public ResponseEntity<ReportTestResponse> testHstReport(@RequestBody(required = false) ReportTestRequest request) {
        log.info("Received POST /test/ocms20/hst-report");

        // Default to full test if no request body provided
        if (request == null) {
            request = new ReportTestRequest(true, true, true, true);
        }

        log.info("Test configuration: generateReport={}, uploadToBlob={}, sendEmail={}, details={}",
            request.getGenerateReport(), request.getUploadToBlob(),
            request.getSendEmail(), request.getDetails());

        ReportTestResponse response = hstReportTestService.executeHstReportTest(request);

        log.info("HST Report test completed: {}", response.getSummary());
        return ResponseEntity.ok(response);
    }

    /**
     * Get test endpoint information
     *
     * @return Endpoint documentation
     */
    @GetMapping("/hst-report/info")
    public ResponseEntity<String> getTestInfo() {
        String info = """
            OCMS 20 - HST Report Test Endpoint
            ===================================

            Endpoint: POST /test/ocms20/hst-report

            Purpose: Test the complete HST report generation workflow

            Request Body (all optional, default true):
            {
              "generateReport": true,   // Generate the reports
              "uploadToBlob": true,     // Upload to Azure Blob Storage
              "sendEmail": true,        // Send email notification to OIC
              "details": true           // Show detailed results
            }

            Test Steps:
            1. Queue HST IDs for monthly MHA/DataHive check
            2. Check and generate HST reports if results available
            3. Verify reports were uploaded to Azure Blob Storage

            Reports Generated:
            - Monthly HST Data Report (address comparison)
            - Monthly HST Work Items Report (11 statistics)

            Expected Behavior:
            - If no new MHA/DataHive results: Returns WARNING status
            - If results available: Generates reports and sends email to OIC
            - Reports uploaded to: /offence/reports/hst/ in Azure Blob

            Note: This test uses REAL data and generates ACTUAL reports.
            """;

        return ResponseEntity.ok(info);
    }
}
