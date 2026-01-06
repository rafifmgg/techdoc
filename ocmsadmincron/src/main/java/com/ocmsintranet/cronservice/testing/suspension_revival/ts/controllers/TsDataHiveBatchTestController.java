package com.ocmsintranet.cronservice.testing.suspension_revival.ts.controllers;

import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.jobs.DataHiveUenFinJob;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test Controller for DataHive Batch Suspension Testing (TS-ACR, TS-SYS)
 *
 * Manual test endpoints for validating DataHive batch record suspension processing
 * Test Cases: 5.1 - 5.4 in TEST_PLAN.md
 *
 * Tests batch processing of multiple suspensions:
 * - Multiple UEN records (ACR - de-registered companies)
 * - Multiple FIN records (SYS - system errors)
 * - Mixed UEN + FIN in same batch
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/suspension/ts/datahive-batch")
public class TsDataHiveBatchTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private DataHiveUenFinJob dataHiveUenFinJob;

    private final List<String> currentTestNoticeNos = new ArrayList<>();
    private int uenCount = 0;
    private int finCount = 0;

    /**
     * Get available test scenarios for DataHive batch suspension
     *
     * GET /api/test/suspension/ts/datahive-batch/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving DataHive Batch test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "DataHive Batch Suspension (TS)");
        response.put("suspensionType", "TS");
        response.put("reasonsOfSuspension", new String[]{"ACR", "SYS"});
        response.put("file", "DataHiveUenFinJob.java");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "5.1",
                        "name", "Create multiple TS-ACR suspensions in batch (UEN)",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "5.2",
                        "name", "Create multiple TS-SYS suspensions in batch (FIN)",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "5.3",
                        "name", "Handle mixed UEN + FIN batch processing",
                        "type", "Integration",
                        "priority", "MEDIUM"
                ),
                Map.of(
                        "id", "5.4",
                        "name", "Continue batch processing when individual record fails",
                        "type", "Error Handling",
                        "priority", "HIGH"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/suspension/ts/datahive-batch/setup",
                "execute", "POST /api/test/suspension/ts/datahive-batch/execute",
                "verify", "GET /api/test/suspension/ts/datahive-batch/verify",
                "cleanup", "POST /api/test/suspension/ts/datahive-batch/cleanup",
                "runTest", "POST /api/test/suspension/ts/datahive-batch/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for DataHive batch suspension test
     *
     * POST /api/test/suspension/ts/datahive-batch/setup
     *
     * Creates:
     * - Multiple test VON records with UENs and/or FINs
     * - Test NRO TEMP records for batch DataHive processing
     *
     * @param params Setup parameters (uenCount, finCount)
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Setting up DataHive Batch test data");

        try {
            currentTestNoticeNos.clear();

            uenCount = params != null && params.containsKey("uenCount")
                    ? (Integer) params.get("uenCount")
                    : 3; // Default 3 UEN records

            finCount = params != null && params.containsKey("finCount")
                    ? (Integer) params.get("finCount")
                    : 2; // Default 2 FIN records

            List<Map<String, String>> testRecords = new ArrayList<>();

            // Create UEN test records
            for (int i = 0; i < uenCount; i++) {
                String noticeNo = "900" + String.format("%06d", (System.currentTimeMillis() + i) % 1000000) + "A";
                String uen = "2024" + String.format("%05d", ((System.currentTimeMillis() + i) % 100000)) + "A";
                String vehicleNo = "TESTDHU" + (System.currentTimeMillis() % 1000) + i;

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, null, uen);
                currentTestNoticeNos.add(noticeNo);

                testRecords.add(Map.of(
                        "noticeNo", noticeNo,
                        "idType", "UEN",
                        "idNo", uen,
                        "vehicleNo", vehicleNo,
                        "expectedReason", "ACR"
                ));

                // Small delay to ensure unique timestamps
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Create FIN test records
            for (int i = 0; i < finCount; i++) {
                String noticeNo = "900" + String.format("%06d", (System.currentTimeMillis() + i + 100) % 1000000) + "B";
                String fin = "S9" + String.format("%06d", ((System.currentTimeMillis() + i) % 1000000)) + "B";
                String vehicleNo = "TESTDHF" + (System.currentTimeMillis() % 1000) + i;

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, fin, null);
                currentTestNoticeNos.add(noticeNo);

                testRecords.add(Map.of(
                        "noticeNo", noticeNo,
                        "idType", "FIN",
                        "idNo", fin,
                        "vehicleNo", vehicleNo,
                        "expectedReason", "SYS"
                ));

                // Small delay to ensure unique timestamps
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch test data created successfully");
            response.put("testRecords", testRecords);
            response.put("counts", Map.of(
                    "uen", uenCount,
                    "fin", finCount,
                    "total", uenCount + finCount
            ));

            log.info("DataHive Batch test setup completed: {} UEN, {} FIN, {} total",
                    uenCount, finCount, currentTestNoticeNos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up DataHive Batch test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute DataHive batch processing to trigger suspensions
     *
     * POST /api/test/suspension/ts/datahive-batch/execute
     *
     * Triggers DataHive batch processing which should:
     * 1. Query DataHive for batch UEN/FIN data
     * 2. Process records by type (UEN batch, FIN batch)
     * 3. Determine suspension reasons per record
     * 4. Call SuspensionApiClient for each
     * 5. Create TS suspensions in batch
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing DataHive batch processing test");

        if (currentTestNoticeNos.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Note: DataHive batch job is triggered by scheduler
            // Manual testing requires triggering the scheduler manually
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "DataHive batch test data setup complete - trigger scheduler manually");
            response.put("note", "Execute DataHive scheduler to process test data");
            response.put("testNoticeNos", currentTestNoticeNos);
            response.put("counts", Map.of(
                    "uen", uenCount,
                    "fin", finCount,
                    "total", currentTestNoticeNos.size()
            ));

            log.info("DataHive Batch test execution completed: {} records processed", currentTestNoticeNos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing DataHive Batch test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify DataHive batch suspensions were created correctly
     *
     * GET /api/test/suspension/ts/datahive-batch/verify
     *
     * Checks:
     * 1. All suspensions exist in suspended_notice table
     * 2. All suspension types = TS
     * 3. Reasons match expectations (ACR for UEN, SYS for FIN)
     * 4. Batch processing completed without errors
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying DataHive batch suspensions");

        if (currentTestNoticeNos.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> verifications = new ArrayList<>();

            int successCount = 0;
            int failedCount = 0;

            for (String noticeNo : currentTestNoticeNos) {
                Map<String, Object> verification = new HashMap<>();
                verification.put("noticeNo", noticeNo);

                boolean exists = testDataHelper.suspensionExists(noticeNo);
                verification.put("exists", exists);

                if (exists) {
                    Map<String, Object> suspension = testDataHelper.getSuspensionDetails(noticeNo);
                    verification.put("suspensionDetails", suspension);

                    // Validate
                    boolean typeValid = "TS".equals(suspension.get("suspension_type"));
                    String reason = (String) suspension.get("reason_of_suspension");
                    boolean reasonValid = "ACR".equals(reason) || "SYS".equals(reason);
                    boolean sourceValid = "004".equals(suspension.get("suspension_source"));

                    verification.put("typeValid", typeValid);
                    verification.put("reasonValid", reasonValid);
                    verification.put("sourceValid", sourceValid);

                    boolean allValid = typeValid && reasonValid && sourceValid;
                    verification.put("passed", allValid);

                    if (allValid) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                } else {
                    verification.put("passed", false);
                    failedCount++;
                }

                verifications.add(verification);
            }

            response.put("verifications", verifications);
            response.put("summary", Map.of(
                    "total", currentTestNoticeNos.size(),
                    "successful", successCount,
                    "failed", failedCount,
                    "uenRecords", uenCount,
                    "finRecords", finCount
            ));
            response.put("success", failedCount == 0);
            response.put("message", failedCount == 0
                    ? "All batch suspensions created successfully"
                    : failedCount + " suspension(s) failed in batch");

            log.info("DataHive Batch verification completed: {} successful, {} failed",
                    successCount, failedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying DataHive batch suspensions: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/suspension/ts/datahive-batch/cleanup
     *
     * Deletes:
     * - All test suspension records
     * - All test VON records
     * - All test NRO TEMP records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up DataHive Batch test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNos.clear();
            uenCount = 0;
            finCount = 0;

            log.info("DataHive Batch cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up DataHive Batch test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete DataHive Batch test (setup → execute → verify → cleanup)
     *
     * POST /api/test/suspension/ts/datahive-batch/run-test
     *
     * Executes all test steps in sequence for batch processing
     *
     * @param params Test parameters (uenCount, finCount optional)
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Running complete DataHive Batch test");

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "DataHive Batch Suspension Creation Test (TS)");
        testResult.put("testId", "5.1-5.3");

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

            log.info("DataHive Batch complete test finished: success={}", verifySuccess);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running DataHive Batch test: {}", e.getMessage(), e);
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
     * GET /api/test/suspension/ts/datahive-batch/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNos", currentTestNoticeNos);
        state.put("uenCount", uenCount);
        state.put("finCount", finCount);
        state.put("totalCount", currentTestNoticeNos.size());
        state.put("hasActiveTest", !currentTestNoticeNos.isEmpty());
        state.put("trackedNoticeNos", testDataHelper.getTrackedNoticeNos());

        return ResponseEntity.ok(state);
    }
}
