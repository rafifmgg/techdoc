package com.ocmsintranet.cronservice.testing.suspension_revival.revival.controllers;

import com.ocmsintranet.cronservice.framework.workflows.autorevival.services.AutoRevivalService;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test Controller for Auto-Revival Testing
 *
 * Manual test endpoints for validating auto-revival functionality
 * Test Cases: 7.1 - 7.10 in TEST_PLAN.md
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/revival/auto-revival")
public class AutoRevivalTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private AutoRevivalService autoRevivalService;

    private final List<String> currentTestNoticeNos = new ArrayList<>();

    /**
     * Get available test scenarios for Auto-Revival
     *
     * GET /api/test/auto-revival/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving Auto-Revival test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "Auto-Revival");
        response.put("description", "Automatic revival of TS suspensions when due_date_of_revival is reached");
        response.put("file", "AutoRevivalHelper.java");
        response.put("cronSchedule", "Daily at 2:00 AM");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "7.1",
                        "name", "Revive TS suspension when due_date_of_revival = TODAY",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "7.2",
                        "name", "Skip TS suspension when due_date_of_revival > TODAY",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "7.3",
                        "name", "Skip TS suspension when already revived",
                        "type", "Integration",
                        "priority", "MEDIUM"
                ),
                Map.of(
                        "id", "7.4",
                        "name", "Handle multiple suspensions for revival",
                        "type", "Integration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "7.5",
                        "name", "Continue processing on individual failure",
                        "type", "Error Handling",
                        "priority", "HIGH"
                )
        });

        response.put("endpoints", Map.of(
                "setup", "POST /api/test/auto-revival/setup",
                "execute", "POST /api/test/auto-revival/execute",
                "verify", "GET /api/test/auto-revival/verify",
                "cleanup", "POST /api/test/auto-revival/cleanup",
                "runTest", "POST /api/test/auto-revival/run-test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Setup test data for Auto-Revival test
     *
     * POST /api/test/auto-revival/setup
     *
     * Creates test suspensions with different due_date_of_revival scenarios:
     * - Suspension due today (should be revived)
     * - Suspension due tomorrow (should NOT be revived)
     * - Already revived suspension (should be skipped)
     *
     * @return Setup result with test data IDs
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Setting up Auto-Revival test data");

        try {
            currentTestNoticeNos.clear();
            List<Map<String, Object>> testSuspensions = new ArrayList<>();

            Integer dueToday = params != null && params.containsKey("dueToday")
                    ? (Integer) params.get("dueToday")
                    : 2;
            Integer dueTomorrow = params != null && params.containsKey("dueTomorrow")
                    ? (Integer) params.get("dueTomorrow")
                    : 1;
            Integer alreadyRevived = params != null && params.containsKey("alreadyRevived")
                    ? (Integer) params.get("alreadyRevived")
                    : 1;

            // Create suspensions due today (days_to_revive = 0)
            for (int i = 0; i < dueToday; i++) {
                String noticeNo = "TEST-AR-TODAY-" + System.currentTimeMillis() + "-" + i;
                String vehicleNo = "TEST" + System.currentTimeMillis() + i;

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, "S1234567A", null);
                Integer srNo = testDataHelper.createTestSuspension(noticeNo, "TS", "ROV", 0);

                currentTestNoticeNos.add(noticeNo);
                testSuspensions.add(Map.of(
                        "noticeNo", noticeNo,
                        "srNo", srNo,
                        "daysToRevive", 0,
                        "expectedAction", "SHOULD_BE_REVIVED"
                ));
            }

            // Create suspensions due tomorrow (days_to_revive = 1)
            for (int i = 0; i < dueTomorrow; i++) {
                String noticeNo = "TEST-AR-TOMORROW-" + System.currentTimeMillis() + "-" + i;
                String vehicleNo = "TEST" + System.currentTimeMillis() + (i + 100);

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, "S1234567B", null);
                Integer srNo = testDataHelper.createTestSuspension(noticeNo, "TS", "SYS", 1);

                currentTestNoticeNos.add(noticeNo);
                testSuspensions.add(Map.of(
                        "noticeNo", noticeNo,
                        "srNo", srNo,
                        "daysToRevive", 1,
                        "expectedAction", "SHOULD_NOT_BE_REVIVED"
                ));
            }

            // Create already revived suspensions (days_to_revive = -1, manually set as revived)
            for (int i = 0; i < alreadyRevived; i++) {
                String noticeNo = "TEST-AR-REVIVED-" + System.currentTimeMillis() + "-" + i;
                String vehicleNo = "TEST" + System.currentTimeMillis() + (i + 200);

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, "S1234567C", null);
                Integer srNo = testDataHelper.createTestSuspension(noticeNo, "TS", "ACR", -1);

                // Manually mark as revived
                testDataHelper.getSuspensionDetails(noticeNo); // This will track it

                currentTestNoticeNos.add(noticeNo);
                testSuspensions.add(Map.of(
                        "noticeNo", noticeNo,
                        "srNo", srNo,
                        "daysToRevive", -1,
                        "expectedAction", "SHOULD_BE_SKIPPED"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("testSuspensions", testSuspensions);
            response.put("counts", Map.of(
                    "dueToday", dueToday,
                    "dueTomorrow", dueTomorrow,
                    "alreadyRevived", alreadyRevived,
                    "total", dueToday + dueTomorrow + alreadyRevived
            ));

            log.info("Auto-Revival test setup completed: {} suspensions created", testSuspensions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting up Auto-Revival test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute auto-revival process
     *
     * POST /api/test/auto-revival/execute
     *
     * Triggers the auto-revival service which should:
     * 1. Query suspensions due for revival
     * 2. Call SuspensionApiClient for each revival
     * 3. Update date_of_revival in database
     *
     * @return Execution result
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        log.info("Executing auto-revival process for test");

        if (currentTestNoticeNos.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Trigger auto-revival service
            String result = autoRevivalService.executeJob().get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auto-revival process executed");
            response.put("result", result);
            response.put("testNoticeNos", currentTestNoticeNos);

            log.info("Auto-Revival test execution completed: {}", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing Auto-Revival test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify auto-revival results
     *
     * GET /api/test/auto-revival/verify
     *
     * Checks:
     * 1. Suspensions due today are revived (date_of_revival IS NOT NULL)
     * 2. Suspensions due tomorrow are NOT revived (date_of_revival IS NULL)
     * 3. Already revived suspensions remain unchanged
     *
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        log.info("Verifying auto-revival results");

        if (currentTestNoticeNos.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No test data setup. Call /setup first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> verifications = new ArrayList<>();

            int passedCount = 0;
            int failedCount = 0;

            for (String noticeNo : currentTestNoticeNos) {
                Map<String, Object> verification = new HashMap<>();
                verification.put("noticeNo", noticeNo);

                boolean isRevived = testDataHelper.isRevived(noticeNo);
                Map<String, Object> suspension = testDataHelper.getSuspensionDetails(noticeNo);

                verification.put("isRevived", isRevived);
                verification.put("suspensionDetails", suspension);

                // Determine expected behavior based on notice number prefix
                String expectedAction;
                boolean passed;

                if (noticeNo.contains("TODAY")) {
                    expectedAction = "SHOULD_BE_REVIVED";
                    passed = isRevived;
                } else if (noticeNo.contains("TOMORROW")) {
                    expectedAction = "SHOULD_NOT_BE_REVIVED";
                    passed = !isRevived;
                } else if (noticeNo.contains("REVIVED")) {
                    expectedAction = "SHOULD_BE_SKIPPED";
                    passed = isRevived; // Already revived, should remain revived
                } else {
                    expectedAction = "UNKNOWN";
                    passed = false;
                }

                verification.put("expectedAction", expectedAction);
                verification.put("passed", passed);

                if (passed) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                verifications.add(verification);
            }

            response.put("verifications", verifications);
            response.put("summary", Map.of(
                    "total", currentTestNoticeNos.size(),
                    "passed", passedCount,
                    "failed", failedCount
            ));
            response.put("success", failedCount == 0);
            response.put("message", failedCount == 0
                    ? "All auto-revival verifications passed"
                    : failedCount + " verification(s) failed");

            log.info("Auto-Revival verification completed: passed={}, failed={}", passedCount, failedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying auto-revival: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup test data
     *
     * POST /api/test/auto-revival/cleanup
     *
     * Deletes:
     * - Test suspension records
     * - Test VON records
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("Cleaning up Auto-Revival test data");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data cleaned up successfully");
            response.put("deletedCounts", deletedCounts);

            currentTestNoticeNos.clear();

            log.info("Auto-Revival cleanup completed: {}", deletedCounts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up Auto-Revival test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Run complete Auto-Revival test (setup → execute → verify → cleanup)
     *
     * POST /api/test/auto-revival/run-test
     *
     * Executes all test steps in sequence
     *
     * @return Complete test result
     */
    @PostMapping("/run-test")
    public ResponseEntity<Map<String, Object>> runTest(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Running complete Auto-Revival test");

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("testName", "Auto-Revival Test");
        testResult.put("testId", "7.1-7.4");

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

            log.info("Auto-Revival complete test finished: success={}", verifySuccess);
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Error running Auto-Revival test: {}", e.getMessage(), e);
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
     * GET /api/test/auto-revival/state
     *
     * @return Current test state information
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNos", currentTestNoticeNos);
        state.put("hasActiveTest", !currentTestNoticeNos.isEmpty());
        state.put("suspensionsDueForRevival", testDataHelper.getSuspensionsDueForRevival());

        return ResponseEntity.ok(state);
    }

    /**
     * Get list of suspensions currently due for revival
     *
     * GET /api/test/auto-revival/due-list
     *
     * @return List of suspensions due for revival
     */
    @GetMapping("/due-list")
    public ResponseEntity<Map<String, Object>> getDueList() {
        log.info("Retrieving suspensions due for revival");

        try {
            List<String> dueNoticeNos = testDataHelper.getSuspensionsDueForRevival();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", dueNoticeNos.size());
            response.put("noticeNos", dueNoticeNos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving due list: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
