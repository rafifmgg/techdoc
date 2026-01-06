package com.ocmsintranet.cronservice.testing.suspension_revival.ts.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services.LtaDownloadService;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for TS-ROV Suspension Testing (LTA ROV Files)
 *
 * Manual test endpoints for validating LTA ROV suspension creation
 * Test Cases: 1.1 - 1.4 in TEST_PLAN.md
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/suspension/ts-rov")
public class TsRovTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private LtaDownloadService ltaDownloadService;

    private String currentTestNoticeNo;

    /**
     * Get available test scenarios for TS-ROV
     *
     * GET /api/test/suspension/ts-rov/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving TS-ROV test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "TS-ROV (LTA ROV)");
        response.put("suspensionType", "TS");
        response.put("reasonOfSuspension", "ROV");
        response.put("file", "LtaProcessingStageManager.java");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "1.1",
                        "name", "Create TS-ROV suspension for valid LTA file",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "1.2",
                        "name", "Verify SuspensionApiClient calls localhost:8085",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "1.3",
                        "name", "Handle API failure gracefully",
                        "type", "Error Handling",
                        "priority", "MEDIUM"
                ),
                Map.of(
                        "id", "1.4",
                        "name", "Verify suspension details (officer, remarks, source)",
                        "type", "Data Validation",
                        "priority", "HIGH"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/suspension/ts-rov/setup",
                "execute", "POST /api/test/suspension/ts-rov/execute",
                "verify", "GET /api/test/suspension/ts-rov/verify",
                "cleanup", "POST /api/test/suspension/ts-rov/cleanup",
                "runTest", "POST /api/test/suspension/ts-rov/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for TS-ROV test
     *
     * POST /api/test/suspension/ts-rov/setup
     *
     * Creates:
     * - Test VON record
     * - Test LTA ROV file entry
     *
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, String> params) {
        log.info("Setting up TS-ROV test data");

        try {
            // Generate unique test notice number
            currentTestNoticeNo = "TEST-ROV-" + System.currentTimeMillis();
            String vehicleNo = params != null && params.containsKey("vehicleNo")
                    ? params.get("vehicleNo")
                    : "TEST" + System.currentTimeMillis();
            String nric = params != null && params.containsKey("nric")
                    ? params.get("nric")
                    : "S1234567A";

            // Create test VON
            testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, nric, null);

            // Create test LTA ROV file
            Long fileId = testDataHelper.createTestLtaFile(vehicleNo, "ROV");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("testData", Map.of(
                    "noticeNo", currentTestNoticeNo,
                    "vehicleNo", vehicleNo,
                    "nric", nric,
                    "ltaFileId", fileId,
                    "fileType", "ROV"
            ));

            log.info("TS-ROV test setup completed: noticeNo={}", currentTestNoticeNo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up TS-ROV test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute LTA download process to trigger TS-ROV suspension
     *
     * POST /api/test/suspension/ts-rov/execute
     *
     * Triggers the LTA download cron which should:
     * 1. Process ROV file
     * 2. Call SuspensionApiClient
     * 3. Create TS-ROV suspension
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing LTA download process for TS-ROV test");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Trigger LTA download service
            var jobResult = ltaDownloadService.executeLtaDownloadJob().get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", jobResult.isSuccess());
            response.put("message", "LTA download process executed");
            response.put("result", jobResult.getMessage());
            response.put("testNoticeNo", currentTestNoticeNo);

            log.info("TS-ROV test execution completed: {}", jobResult.getMessage());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing TS-ROV test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify TS-ROV suspension was created correctly
     *
     * GET /api/test/suspension/ts-rov/verify
     *
     * Checks:
     * 1. Suspension exists in suspended_notice table
     * 2. Suspension type = TS
     * 3. Reason of suspension = ROV
     * 4. Officer, remarks, source are correct
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying TS-ROV suspension creation");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("testNoticeNo", currentTestNoticeNo);

            // Check if suspension exists
            boolean exists = testDataHelper.suspensionExists(currentTestNoticeNo);
            response.put("suspensionExists", exists);

            if (exists) {
                // Get suspension details
                Map<String, Object> suspension = testDataHelper.getSuspensionDetails(currentTestNoticeNo);
                response.put("suspensionDetails", suspension);

                // Validate suspension type and reason
                Map<String, Object> validations = new HashMap<>();
                validations.put("suspensionType", "TS".equals(suspension.get("suspension_type")));
                validations.put("reasonOfSuspension", "ROV".equals(suspension.get("reason_of_suspension")));
                validations.put("suspensionSource", "004".equals(suspension.get("suspension_source")));
                validations.put("officerPresent", suspension.get("officer_authorising_suspension") != null);
                validations.put("remarksPresent", suspension.get("suspension_remarks") != null);

                response.put("validations", validations);

                // Overall result
                boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);
                response.put("success", allValid);
                response.put("message", allValid
                        ? "TS-ROV suspension created successfully and all validations passed"
                        : "TS-ROV suspension created but some validations failed");

                log.info("TS-ROV verification completed: success={}", allValid);
            } else {
                response.put("success", false);
                response.put("message", "TS-ROV suspension was not created");
                log.warn("TS-ROV suspension not found for noticeNo: {}", currentTestNoticeNo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying TS-ROV suspension: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/suspension/ts-rov/cleanup
     *
     * Deletes:
     * - Test suspension records
     * - Test VON records
     * - Test LTA file records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up TS-ROV test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNo = null;

            log.info("TS-ROV cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up TS-ROV test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete TS-ROV test (setup → execute → verify → cleanup)
     *
     * POST /api/test/suspension/ts-rov/run-test
     *
     * Executes all test steps in sequence
     *
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, String> params) {
        log.info("Running complete TS-ROV test");

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "TS-ROV Suspension Creation Test");
        testResult.put("testId", "1.1");

        try {
            // Step 1: Setup
            log.info("Step 1: Setup");
            ResponseEntity<Map<String, Object>> setupResponse = setup(params);
            testResult.put("setup", setupResponse.getBody());

            if (!setupResponse.getStatusCode().is2xxSuccessful()) {
                testResult.put("success", false);
                testResult.put("failedAt", "setup");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(testResult);
            }

            // Step 2: Execute
            log.info("Step 2: Execute");
            ResponseEntity<Map<String, Object>> executeResponse = execute();
            testResult.put("execute", executeResponse.getBody());

            if (!executeResponse.getStatusCode().is2xxSuccessful()) {
                testResult.put("success", false);
                testResult.put("failedAt", "execute");
                cleanup(); // Cleanup on failure
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(testResult);
            }

            // Step 3: Verify
            log.info("Step 3: Verify");
            ResponseEntity<Map<String, Object>> verifyResponse = verify();
            testResult.put("verify", verifyResponse.getBody());

            boolean verifySuccess = verifyResponse.getBody() != null
                    && Boolean.TRUE.equals(verifyResponse.getBody().get("success"));

            testResult.put("success", verifySuccess);

            if (!verifySuccess) {
                testResult.put("failedAt", "verify");
            }

            // Step 4: Cleanup
            log.info("Step 4: Cleanup");
            ResponseEntity<Map<String, Object>> cleanupResponse = cleanup();
            testResult.put("cleanup", cleanupResponse.getBody());

            log.info("TS-ROV complete test finished: success={}", verifySuccess);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running TS-ROV test: {}", e.getMessage(), e);
            testResult.put("success", false);
            testResult.put("error", e.getMessage());

            // Attempt cleanup
            try {
                cleanup();
            } catch (Exception cleanupError) {
                log.error("Error during cleanup: {}", cleanupError.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(testResult);
        }
    }

    /**
     * Get current test state
     *
     * GET /api/test/suspension/ts-rov/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNo", currentTestNoticeNo);
        state.put("hasActiveTest", currentTestNoticeNo != null);
        state.put("trackedNoticeNos", testDataHelper.getTrackedNoticeNos());

        return ResponseEntity.ok(state);
    }
}
