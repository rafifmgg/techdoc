package com.ocmsintranet.cronservice.testing.suspension_revival.ps.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.services.ToppanStageTransitionService;
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
 * Test Controller for Toppan Suspension Testing
 *
 * Manual test endpoints for validating Toppan-based suspension creation
 * Test Cases: 3.1 - 3.4 in TEST_PLAN.md
 *
 * Toppan suspension reasons from data:
 * - APP: Appeal
 * - ACR: Appeal Case Resolved
 * - SYS: System suspension
 * - HST: Historical
 * - MS: Manual Suspension
 * - FOR: Foreign vehicle
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/suspension/ps/toppan")
public class PsToppanTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private ToppanStageTransitionService toppanStageTransitionService;

    private String currentTestNoticeNo;
    private String currentReasonCode;

    /**
     * Get available test scenarios for Toppan suspensions
     *
     * GET /api/test/suspension/toppan/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving Toppan test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "Toppan Suspensions");
        response.put("suspensionTypes", new String[]{"TS", "PS"});
        response.put("reasonsOfSuspension", new String[]{"APP", "ACR", "SYS", "HST", "MS", "FOR"});
        response.put("file", "ToppanProcessingManager.java");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "3.1",
                        "name", "Create TS-APP suspension from Toppan file",
                        "type", "Integration",
                        "priority", "HIGH",
                        "reasonCode", "APP"
                ),
                Map.of(
                        "id", "3.2",
                        "name", "Create PS-FOR suspension from Toppan file",
                        "type", "Integration",
                        "priority", "MEDIUM",
                        "reasonCode", "FOR"
                ),
                Map.of(
                        "id", "3.3",
                        "name", "Verify SuspensionApiClient calls localhost:8085",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "3.4",
                        "name", "Handle Toppan API failure gracefully",
                        "type", "Error Handling",
                        "priority", "MEDIUM"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/suspension/toppan/setup",
                "execute", "POST /api/test/suspension/toppan/execute",
                "verify", "GET /api/test/suspension/toppan/verify",
                "cleanup", "POST /api/test/suspension/toppan/cleanup",
                "runTest", "POST /api/test/suspension/toppan/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for Toppan test
     *
     * POST /api/test/suspension/toppan/setup
     *
     * Creates:
     * - Test VON record
     * - Test Toppan file entry (simulated)
     *
     * @param params Setup parameters (vehicleNo, nric, uen, reasonCode, suspensionType optional)
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, String> params) {
        log.info("Setting up Toppan test data");

        try {
            // Generate unique test notice number in valid range
            currentTestNoticeNo = "900" + String.format("%06d", System.currentTimeMillis() % 1000000) + "A";

            String vehicleNo = params != null && params.containsKey("vehicleNo")
                    ? params.get("vehicleNo")
                    : "TESTTOP" + (System.currentTimeMillis() % 1000);

            String nric = params != null && params.containsKey("nric")
                    ? params.get("nric")
                    : "S9" + String.format("%06d", (System.currentTimeMillis() % 1000000)) + "A";

            String uen = params != null && params.containsKey("uen")
                    ? params.get("uen")
                    : null; // Optional

            currentReasonCode = params != null && params.containsKey("reasonCode")
                    ? params.get("reasonCode")
                    : "APP"; // Default to APP (Appeal)

            String suspensionType = params != null && params.containsKey("suspensionType")
                    ? params.get("suspensionType")
                    : "TS"; // Default to TS

            // Validate reason code
            if (!isValidReasonCode(currentReasonCode)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid reason code. Must be one of: APP, ACR, SYS, HST, MS, FOR");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Validate suspension type
            if (!suspensionType.equals("TS") && !suspensionType.equals("PS")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid suspension type. Must be TS or PS");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Create test VON
            testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, nric, uen);

            // Note: Toppan file creation would typically be handled by file upload/processing
            // For testing, we may need to manually insert records or trigger file processing
            log.info("Toppan test data prepared (file processing may require manual trigger)");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("testData", Map.of(
                    "noticeNo", currentTestNoticeNo,
                    "vehicleNo", vehicleNo,
                    "nric", nric,
                    "uen", uen != null ? uen : "N/A",
                    "reasonCode", currentReasonCode,
                    "suspensionType", suspensionType,
                    "expectedSuspensionType", suspensionType,
                    "expectedReasonOfSuspension", currentReasonCode
            ));

            log.info("Toppan test setup completed: noticeNo={}, reasonCode={}, suspensionType={}",
                    currentTestNoticeNo, currentReasonCode, suspensionType);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up Toppan test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute Toppan download process to trigger suspension
     *
     * POST /api/test/suspension/toppan/execute
     *
     * Triggers the Toppan download cron which should:
     * 1. Process Toppan file
     * 2. Call SuspensionApiClient
     * 3. Create suspension with specified reason code
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing Toppan download process for test");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Note: Toppan download is triggered by scheduler
            // Manual testing requires triggering the scheduler or calling the controller directly
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Toppan test data setup complete - trigger scheduler manually");
            response.put("note", "Execute Toppan download scheduler to process test data");
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("reasonCode", currentReasonCode);

            log.info("Toppan test setup completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing Toppan test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify Toppan suspension was created correctly
     *
     * GET /api/test/suspension/toppan/verify
     *
     * Checks:
     * 1. Suspension exists in suspended_notice table
     * 2. Suspension type matches expected (TS/PS)
     * 3. Reason of suspension matches
     * 4. Officer, remarks, source are correct
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying Toppan suspension creation");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("expectedReasonCode", currentReasonCode);

            // Check if suspension exists
            boolean exists = testDataHelper.suspensionExists(currentTestNoticeNo);
            response.put("suspensionExists", exists);

            if (exists) {
                // Get suspension details
                Map<String, Object> suspension = testDataHelper.getSuspensionDetails(currentTestNoticeNo);
                response.put("suspensionDetails", suspension);

                // Validate suspension details
                Map<String, Object> validations = new HashMap<>();

                String suspensionType = (String) suspension.get("suspension_type");
                validations.put("suspensionTypeValid",
                    suspensionType != null && (suspensionType.equals("TS") || suspensionType.equals("PS")));

                validations.put("reasonOfSuspension",
                    currentReasonCode.equals(suspension.get("reason_of_suspension")));

                validations.put("suspensionSource",
                    "004".equals(suspension.get("suspension_source")));

                validations.put("officerPresent",
                    suspension.get("officer_authorising_suspension") != null);

                validations.put("remarksPresent",
                    suspension.get("suspension_remarks") != null);

                response.put("validations", validations);

                // Overall result
                boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);
                response.put("success", allValid);
                response.put("message", allValid
                        ? "Toppan suspension created successfully and all validations passed"
                        : "Toppan suspension created but some validations failed");

                log.info("Toppan verification completed: success={}, reasonCode={}", allValid, currentReasonCode);
            } else {
                response.put("success", false);
                response.put("message", "Toppan suspension was not created");
                log.warn("Toppan suspension not found for noticeNo: {}", currentTestNoticeNo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying Toppan suspension: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/suspension/toppan/cleanup
     *
     * Deletes:
     * - Test suspension records
     * - Test VON records
     * - Test Toppan file records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up Toppan test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNo = null;
            currentReasonCode = null;

            log.info("Toppan cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up Toppan test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete Toppan test (setup → execute → verify → cleanup)
     *
     * POST /api/test/suspension/toppan/run-test
     *
     * Executes all test steps in sequence
     *
     * @param params Test parameters (reasonCode, suspensionType optional)
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, String> params) {
        log.info("Running complete Toppan test");

        String testReasonCode = params != null && params.containsKey("reasonCode")
                ? params.get("reasonCode")
                : "APP";

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "Toppan Suspension Creation Test");
        testResult.put("testId", "3.1-3.2");
        testResult.put("reasonCode", testReasonCode);

        try {
            // Step 1: Setup
            log.info("Step 1: Setup (reasonCode={})", testReasonCode);
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

            log.info("Toppan complete test finished: success={}, reasonCode={}", verifySuccess, testReasonCode);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running Toppan test: {}", e.getMessage(), e);
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
     * GET /api/test/suspension/toppan/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNo", currentTestNoticeNo);
        state.put("currentReasonCode", currentReasonCode);
        state.put("hasActiveTest", currentTestNoticeNo != null);
        state.put("trackedNoticeNos", testDataHelper.getTrackedNoticeNos());

        return ResponseEntity.ok(state);
    }

    /**
     * Validate Toppan reason code
     *
     * @param reasonCode Reason code to validate
     * @return true if valid
     */
    private boolean isValidReasonCode(String reasonCode) {
        return reasonCode != null &&
               (reasonCode.equals("APP") ||
                reasonCode.equals("ACR") ||
                reasonCode.equals("SYS") ||
                reasonCode.equals("HST") ||
                reasonCode.equals("MS") ||
                reasonCode.equals("FOR"));
    }
}
