package com.ocmsintranet.cronservice.testing.suspension_revival.ps.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.services.MhaNricDownloadService;
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
 * Test Controller for MHA NRIC Suspension Testing
 *
 * Manual test endpoints for validating MHA NRIC-based suspension creation
 * Test Cases: 2.1 - 2.6 in TEST_PLAN.md
 *
 * MHA suspension reasons:
 * - RIP: Rest in Peace (deceased)
 * - RP2: Repatriated
 * - NRO: Non-Renewable (permit not renewable)
 * - OLD: Old/Invalid NRIC
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/suspension/ps/mha-nric")
public class PsMhaNricTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private MhaNricDownloadService mhaNricDownloadService;

    private String currentTestNoticeNo;
    private String currentTestNric;
    private String currentReasonCode;

    /**
     * Get available test scenarios for MHA NRIC suspensions
     *
     * GET /api/test/suspension/mha-nric/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving MHA NRIC test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "MHA NRIC Suspensions");
        response.put("suspensionType", "PS");
        response.put("reasonsOfSuspension", new String[]{"RIP", "RP2", "NRO", "OLD"});
        response.put("file", "MhaProcessingManager.java");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "2.1",
                        "name", "Create PS-RIP suspension for deceased person",
                        "type", "Integration",
                        "priority", "HIGH",
                        "reasonCode", "RIP"
                ),
                Map.of(
                        "id", "2.2",
                        "name", "Create PS-RP2 suspension for repatriated person",
                        "type", "Integration",
                        "priority", "HIGH",
                        "reasonCode", "RP2"
                ),
                Map.of(
                        "id", "2.3",
                        "name", "Create PS-NRO suspension for non-renewable permit",
                        "type", "Integration",
                        "priority", "MEDIUM",
                        "reasonCode", "NRO"
                ),
                Map.of(
                        "id", "2.4",
                        "name", "Create PS-OLD suspension for invalid NRIC",
                        "type", "Integration",
                        "priority", "MEDIUM",
                        "reasonCode", "OLD"
                ),
                Map.of(
                        "id", "2.5",
                        "name", "Verify SuspensionApiClient calls localhost:8085",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "2.6",
                        "name", "Handle MHA API failure gracefully",
                        "type", "Error Handling",
                        "priority", "MEDIUM"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/suspension/mha-nric/setup",
                "execute", "POST /api/test/suspension/mha-nric/execute",
                "verify", "GET /api/test/suspension/mha-nric/verify",
                "cleanup", "POST /api/test/suspension/mha-nric/cleanup",
                "runTest", "POST /api/test/suspension/mha-nric/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for MHA NRIC test
     *
     * POST /api/test/suspension/mha-nric/setup
     *
     * Creates:
     * - Test VON record with NRIC
     * - Test MHA NRIC file entry
     *
     * @param params Setup parameters (nric, reasonCode, vehicleNo optional)
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, String> params) {
        log.info("Setting up MHA NRIC test data");

        try {
            // Generate unique test notice number
            currentTestNoticeNo = "900" + String.format("%06d", System.currentTimeMillis() % 1000000) + "A";

            String vehicleNo = params != null && params.containsKey("vehicleNo")
                    ? params.get("vehicleNo")
                    : "TESTMHA" + (System.currentTimeMillis() % 1000);

            currentTestNric = params != null && params.containsKey("nric")
                    ? params.get("nric")
                    : "S9" + String.format("%06d", (System.currentTimeMillis() % 1000000)) + "A";

            currentReasonCode = params != null && params.containsKey("reasonCode")
                    ? params.get("reasonCode")
                    : "RIP"; // Default to RIP (deceased)

            // Validate reason code
            if (!isValidReasonCode(currentReasonCode)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid reason code. Must be one of: RIP, RP2, NRO, OLD");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Create test VON with NRIC
            testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, currentTestNric, null);

            // Create test MHA NRIC file
            Long fileId = testDataHelper.createTestMhaFile(currentTestNric, currentReasonCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("testData", Map.of(
                    "noticeNo", currentTestNoticeNo,
                    "vehicleNo", vehicleNo,
                    "nric", currentTestNric,
                    "reasonCode", currentReasonCode,
                    "mhaFileId", fileId,
                    "expectedSuspensionType", "PS",
                    "expectedReasonOfSuspension", currentReasonCode
            ));

            log.info("MHA NRIC test setup completed: noticeNo={}, nric={}, reasonCode={}",
                    currentTestNoticeNo, currentTestNric, currentReasonCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up MHA NRIC test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute MHA download process to trigger MHA NRIC suspension
     *
     * POST /api/test/suspension/mha-nric/execute
     *
     * Triggers the MHA download cron which should:
     * 1. Process MHA NRIC file
     * 2. Call SuspensionApiClient
     * 3. Create PS suspension with reason code (RIP/RP2/NRO/OLD)
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing MHA download process for MHA NRIC test");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Trigger MHA download service
            Boolean result = mhaNricDownloadService.executeJob().get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MHA download process executed");
            response.put("result", result);
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("testNric", currentTestNric);
            response.put("reasonCode", currentReasonCode);

            log.info("MHA NRIC test execution completed: {}", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing MHA NRIC test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify MHA NRIC suspension was created correctly
     *
     * GET /api/test/suspension/mha-nric/verify
     *
     * Checks:
     * 1. Suspension exists in suspended_notice table
     * 2. Suspension type = PS (Permanent Suspension)
     * 3. Reason of suspension matches (RIP/RP2/NRO/OLD)
     * 4. Officer, remarks, source are correct
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying MHA NRIC suspension creation");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("testNric", currentTestNric);
            response.put("expectedReasonCode", currentReasonCode);

            // Check if suspension exists
            boolean exists = testDataHelper.suspensionExists(currentTestNoticeNo);
            response.put("suspensionExists", exists);

            if (exists) {
                // Get suspension details
                Map<String, Object> suspension = testDataHelper.getSuspensionDetails(currentTestNoticeNo);
                response.put("suspensionDetails", suspension);

                // Validate suspension type and reason
                Map<String, Object> validations = new HashMap<>();
                validations.put("suspensionType", "PS".equals(suspension.get("suspension_type")));
                validations.put("reasonOfSuspension", currentReasonCode.equals(suspension.get("reason_of_suspension")));
                validations.put("suspensionSource", "004".equals(suspension.get("suspension_source")));
                validations.put("officerPresent", suspension.get("officer_authorising_suspension") != null);
                validations.put("remarksPresent", suspension.get("suspension_remarks") != null);

                response.put("validations", validations);

                // Overall result
                boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);
                response.put("success", allValid);
                response.put("message", allValid
                        ? "MHA NRIC suspension created successfully and all validations passed"
                        : "MHA NRIC suspension created but some validations failed");

                log.info("MHA NRIC verification completed: success={}, reasonCode={}", allValid, currentReasonCode);
            } else {
                response.put("success", false);
                response.put("message", "MHA NRIC suspension was not created");
                log.warn("MHA NRIC suspension not found for noticeNo: {}, nric: {}",
                        currentTestNoticeNo, currentTestNric);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying MHA NRIC suspension: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/suspension/mha-nric/cleanup
     *
     * Deletes:
     * - Test suspension records
     * - Test VON records
     * - Test MHA file records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up MHA NRIC test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNo = null;
            currentTestNric = null;
            currentReasonCode = null;

            log.info("MHA NRIC cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up MHA NRIC test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete MHA NRIC test (setup → execute → verify → cleanup)
     *
     * POST /api/test/suspension/mha-nric/run-test
     *
     * Executes all test steps in sequence
     *
     * @param params Test parameters (nric, reasonCode optional)
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, String> params) {
        log.info("Running complete MHA NRIC test");

        String testReasonCode = params != null && params.containsKey("reasonCode")
                ? params.get("reasonCode")
                : "RIP";

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "MHA NRIC Suspension Creation Test");
        testResult.put("testId", "2.1-2.4");
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

            log.info("MHA NRIC complete test finished: success={}, reasonCode={}", verifySuccess, testReasonCode);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running MHA NRIC test: {}", e.getMessage(), e);
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
     * GET /api/test/suspension/mha-nric/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNo", currentTestNoticeNo);
        state.put("currentTestNric", currentTestNric);
        state.put("currentReasonCode", currentReasonCode);
        state.put("hasActiveTest", currentTestNoticeNo != null);
        state.put("trackedNoticeNos", testDataHelper.getTrackedNoticeNos());

        return ResponseEntity.ok(state);
    }

    /**
     * Validate MHA reason code
     *
     * @param reasonCode Reason code to validate
     * @return true if valid
     */
    private boolean isValidReasonCode(String reasonCode) {
        return reasonCode != null &&
               (reasonCode.equals("RIP") ||
                reasonCode.equals("RP2") ||
                reasonCode.equals("NRO") ||
                reasonCode.equals("OLD"));
    }
}
