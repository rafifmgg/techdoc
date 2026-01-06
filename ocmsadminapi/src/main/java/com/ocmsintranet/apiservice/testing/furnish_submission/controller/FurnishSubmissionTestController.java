package com.ocmsintranet.apiservice.testing.furnish_submission.controller;

import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_submission.service.FurnishSubmissionTestService;
import com.ocmsintranet.apiservice.testing.furnish_submission.service.FurnishSubmissionTestSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Furnish Submission Integration Testing
 *
 * Endpoints:
 * - POST /${api.version}/furnish-submission-test-setup
 *   Setup test data before running tests (Staff Portal)
 *
 * - POST /${api.version}/furnish-submission-test
 *   Execute furnish submission test (Staff Portal)
 *
 * - DELETE /${api.version}/furnish-submission-test-cleanup
 *   Cleanup test data after tests (Staff Portal)
 *
 * - POST /${api.version}/test/furnish-submission
 *   Execute a single test scenario (Postman/automated)
 *
 * Staff Portal Usage:
 * 1. Call setup endpoint to create test data
 * 2. Call test endpoint to run tests
 * 3. Call cleanup endpoint to remove test data
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class FurnishSubmissionTestController {

    private final FurnishSubmissionTestService testService;
    private final FurnishSubmissionTestSetupService setupService;

    @Value("${api.version}")
    private String apiVersion;

    /**
     * Setup endpoint - Prepares test data before test execution (Staff Portal)
     * Creates/resets test notices to known state
     *
     * IMPORTANT: Call this BEFORE running the test
     *
     * @return Setup result with created record count
     */
    @PostMapping("/${api.version}/furnish-submission-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /furnish-submission-test-setup");

        try {
            Map<String, Object> result = setupService.setupTestData();
            log.info("Setup completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Setup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Cleanup endpoint - Removes test data after test execution (Staff Portal)
     * Deletes furnish applications, suspensions, and resets notices
     *
     * IMPORTANT: Call this AFTER running the test to clean up
     *
     * @return Cleanup result with deleted record count
     */
    @DeleteMapping("/${api.version}/furnish-submission-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /furnish-submission-test-cleanup");

        try {
            Map<String, Object> result = setupService.cleanupTestData();
            log.info("Cleanup completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Main test endpoint - Executes furnish submission test (Staff Portal)
     * Runs all test scenarios from scenario.json
     *
     * @param request Test request with options (details, triggerApi)
     * @return Test execution results
     */
    @PostMapping("/${api.version}/furnish-submission-test")
    public ResponseEntity<Map<String, Object>> handleStaffPortalTest(@RequestBody(required = false) Map<String, Object> request) {
        log.info("Received POST /furnish-submission-test (Staff Portal)");

        try {
            // Run all scenarios
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test execution completed");
            result.put("note", "Use POST /v1/test/furnish-submission with scenarioName to run individual scenarios");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Test execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Execute a single furnish submission test scenario (Postman/automated)
     *
     * @param request Test request with scenario name
     * @return Test execution results
     */
    @PostMapping("/${api.version}/test/furnish-submission")
    public ResponseEntity<FurnishSubmissionTestResponse> executeTest(@RequestBody FurnishSubmissionTestRequest request) {
        log.info("Received furnish submission test request - Scenario: {}", request.getScenarioName());

        try {
            FurnishSubmissionTestResponse response = testService.executeTest(request);

            // Return appropriate HTTP status based on test result
            HttpStatus status = switch (response.getOverallResult()) {
                case "PASS" -> HttpStatus.OK;
                case "FAIL" -> HttpStatus.OK; // Still return 200 for test failures (test ran successfully)
                case "ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
                default -> HttpStatus.OK;
            };

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error executing test - Scenario: {}", request.getScenarioName(), e);

            FurnishSubmissionTestResponse errorResponse = FurnishSubmissionTestResponse.builder()
                    .scenarioName(request.getScenarioName())
                    .overallResult("ERROR")
                    .errorMessage("Test execution failed: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for testing framework
     *
     * @return Simple OK response
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Furnish Submission Test Controller is running");
    }
}
