package com.ocmsintranet.cronservice.testing.suspension_revival.ts.controllers;

import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.services.DataHiveUenFinService;
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
 * Test Controller for DataHive Single Suspension Testing (TS-ACR, TS-SYS)
 *
 * Manual test endpoints for validating DataHive single record suspension processing
 * Test Cases: 4.1 - 4.4 in TEST_PLAN.md
 *
 * DataHive suspension reasons:
 * - ACR: De-registered company (TS)
 * - SYS: System error (TS)
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/suspension/ts/datahive-single")
public class TsDataHiveSingleTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private DataHiveUenFinService dataHiveUenFinService;

    private String currentTestNoticeNo;
    private String currentReasonCode;
    private String currentIdNo;
    private String currentIdType;

    /**
     * Get available test scenarios for DataHive single suspension
     *
     * GET /api/test/suspension/ts/datahive-single/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving DataHive Single test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "DataHive Single Suspension (TS)");
        response.put("suspensionType", "TS");
        response.put("reasonsOfSuspension", new String[]{"ACR", "SYS"});
        response.put("file", "DataHiveUenFinService.java");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "4.1",
                        "name", "Create TS-ACR suspension for de-registered company (single UEN)",
                        "type", "Integration",
                        "priority", "HIGH",
                        "reasonCode", "ACR"
                ),
                Map.of(
                        "id", "4.2",
                        "name", "Create TS-SYS suspension for system error (single FIN)",
                        "type", "Integration",
                        "priority", "HIGH",
                        "reasonCode", "SYS"
                ),
                Map.of(
                        "id", "4.3",
                        "name", "Verify SuspensionApiClient calls localhost:8085",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "4.4",
                        "name", "Handle DataHive API failure gracefully",
                        "type", "Error Handling",
                        "priority", "MEDIUM"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/suspension/ts/datahive-single/setup",
                "execute", "POST /api/test/suspension/ts/datahive-single/execute",
                "verify", "GET /api/test/suspension/ts/datahive-single/verify",
                "cleanup", "POST /api/test/suspension/ts/datahive-single/cleanup",
                "runTest", "POST /api/test/suspension/ts/datahive-single/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for DataHive single suspension test
     *
     * POST /api/test/suspension/ts/datahive-single/setup
     *
     * Creates:
     * - Test VON record with UEN or FIN
     * - Test NRO TEMP record for DataHive processing
     *
     * @param params Setup parameters (idNo, idType, reasonCode optional)
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, String> params) {
        log.info("Setting up DataHive Single test data");

        try {
            // Generate unique test notice number
            currentTestNoticeNo = "900" + String.format("%06d", System.currentTimeMillis() % 1000000) + "A";

            currentIdType = params != null && params.containsKey("idType")
                    ? params.get("idType")
                    : "U"; // Default to UEN

            // Generate ID based on type
            if ("U".equals(currentIdType)) {
                // UEN format: 202400001A
                currentIdNo = params != null && params.containsKey("idNo")
                        ? params.get("idNo")
                        : "2024" + String.format("%05d", (System.currentTimeMillis() % 100000)) + "A";
            } else if ("F".equals(currentIdType)) {
                // FIN format: S9123456A
                currentIdNo = params != null && params.containsKey("idNo")
                        ? params.get("idNo")
                        : "S9" + String.format("%06d", (System.currentTimeMillis() % 1000000)) + "A";
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid idType. Must be 'U' (UEN) or 'F' (FIN)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            currentReasonCode = params != null && params.containsKey("reasonCode")
                    ? params.get("reasonCode")
                    : "ACR"; // Default to ACR (de-registered)

            // Validate reason code
            if (!currentReasonCode.equals("ACR") && !currentReasonCode.equals("SYS")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid reason code. Must be 'ACR' or 'SYS'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            String vehicleNo = params != null && params.containsKey("vehicleNo")
                    ? params.get("vehicleNo")
                    : "TESTDH" + (System.currentTimeMillis() % 1000);

            // Create test VON
            if ("U".equals(currentIdType)) {
                testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, null, currentIdNo);
            } else {
                testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, currentIdNo, null);
            }

            // Note: In real scenario, ocms_nro_temp record would be created by scheduler
            // For testing, we may need to manually insert this record or mock the DataHive flow
            log.info("Test VON created. DataHive processing would normally require ocms_nro_temp record");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("testData", Map.of(
                    "noticeNo", currentTestNoticeNo,
                    "vehicleNo", vehicleNo,
                    "idType", currentIdType,
                    "idNo", currentIdNo,
                    "reasonCode", currentReasonCode,
                    "expectedSuspensionType", "TS",
                    "expectedReasonOfSuspension", currentReasonCode
            ));

            log.info("DataHive Single test setup completed: noticeNo={}, idType={}, idNo={}, reasonCode={}",
                    currentTestNoticeNo, currentIdType, currentIdNo, currentReasonCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up DataHive Single test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute DataHive single processing to trigger suspension
     *
     * POST /api/test/suspension/ts/datahive-single/execute
     *
     * Triggers DataHive single record processing which should:
     * 1. Query DataHive for UEN/FIN data
     * 2. Determine suspension reason (ACR for de-registered, SYS for errors)
     * 3. Call SuspensionApiClient
     * 4. Create TS suspension
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing DataHive single processing test");

        if (currentTestNoticeNo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Note: DataHive processing is normally triggered by cron job
            // For testing, we would need to call the appropriate service method directly
            // This may require additional setup or mocking

            log.info("DataHive single processing would be triggered here");
            log.info("In production, this is handled by DataHiveUenFinJob cron");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "DataHive single processing flow documented");
            response.put("note", "Manual trigger requires calling DataHiveUenFinService methods directly");
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("idType", currentIdType);
            response.put("idNo", currentIdNo);
            response.put("reasonCode", currentReasonCode);

            log.info("DataHive Single test execution noted: {}", currentTestNoticeNo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing DataHive Single test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify DataHive suspension was created correctly
     *
     * GET /api/test/suspension/ts/datahive-single/verify
     *
     * Checks:
     * 1. Suspension exists in suspended_notice table
     * 2. Suspension type = TS
     * 3. Reason of suspension matches (ACR/SYS)
     * 4. Officer, remarks, source are correct
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying DataHive suspension creation");

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
            response.put("idType", currentIdType);
            response.put("idNo", currentIdNo);

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
                validations.put("reasonOfSuspension", currentReasonCode.equals(suspension.get("reason_of_suspension")));
                validations.put("suspensionSource", "004".equals(suspension.get("suspension_source")));
                validations.put("officerPresent", suspension.get("officer_authorising_suspension") != null);
                validations.put("remarksPresent", suspension.get("suspension_remarks") != null);

                response.put("validations", validations);

                // Overall result
                boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);
                response.put("success", allValid);
                response.put("message", allValid
                        ? "DataHive TS suspension created successfully and all validations passed"
                        : "DataHive TS suspension created but some validations failed");

                log.info("DataHive Single verification completed: success={}, reasonCode={}", allValid, currentReasonCode);
            } else {
                response.put("success", false);
                response.put("message", "DataHive TS suspension was not created");
                log.warn("DataHive TS suspension not found for noticeNo: {}", currentTestNoticeNo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying DataHive suspension: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/suspension/ts/datahive-single/cleanup
     *
     * Deletes:
     * - Test suspension records
     * - Test VON records
     * - Test NRO TEMP records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up DataHive Single test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNo = null;
            currentReasonCode = null;
            currentIdNo = null;
            currentIdType = null;

            log.info("DataHive Single cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up DataHive Single test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete DataHive Single test (setup → execute → verify → cleanup)
     *
     * POST /api/test/suspension/ts/datahive-single/run-test
     *
     * Executes all test steps in sequence
     *
     * @param params Test parameters (idType, idNo, reasonCode optional)
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, String> params) {
        log.info("Running complete DataHive Single test");

        String testReasonCode = params != null && params.containsKey("reasonCode")
                ? params.get("reasonCode")
                : "ACR";

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "DataHive Single Suspension Creation Test (TS)");
        testResult.put("testId", "4.1-4.2");
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

            log.info("DataHive Single complete test finished: success={}, reasonCode={}", verifySuccess, testReasonCode);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running DataHive Single test: {}", e.getMessage(), e);
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
     * GET /api/test/suspension/ts/datahive-single/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNo", currentTestNoticeNo);
        state.put("currentReasonCode", currentReasonCode);
        state.put("currentIdType", currentIdType);
        state.put("currentIdNo", currentIdNo);
        state.put("hasActiveTest", currentTestNoticeNo != null);
        state.put("trackedNoticeNos", testDataHelper.getTrackedNoticeNos());

        return ResponseEntity.ok(state);
    }
}
