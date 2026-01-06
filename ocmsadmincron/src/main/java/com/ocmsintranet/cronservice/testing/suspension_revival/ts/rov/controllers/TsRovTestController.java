package com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.controllers;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.service.TsRovTestService;
import com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.service.TsRovTestSetupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TS-ROV Test Controller (Framework-Aligned)
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Tests TS-ROV suspension creation via LTA download cron
 * Test Cases: 1.1 - 1.4 in TEST_PLAN.md
 *
 * IMPORTANT: Only available in non-production environments
 *
 * NEW 4-STEP PATTERN:
 * - Step 1: Load Test Scenarios (from ts_rov_scenarios.json)
 * - Step 2: Trigger LTA Download Process
 * - Step 3: Fetch Verification Data (suspensions, VON, LTA files)
 * - Step 4: Verify Business Logic
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@Profile("!prod")
public class TsRovTestController {

    @Autowired
    private TsRovTestService testService;

    @Autowired
    private TsRovTestSetupService setupService;

    /**
     * Main test endpoint - Executes 4-step test workflow
     *
     * Can be triggered from Staff Portal or Postman
     *
     * POST /v1/ts-rov-test
     *
     * Request body:
     * {
     *   "details": true,     // Show detailed verification results
     *   "triggerApi": true   // Actually trigger the LTA download cron
     * }
     *
     * @param request Test request with configuration flags
     * @return Test response with all 4 steps
     */
    @PostMapping("/ts-rov-test")
    public ResponseEntity<SuspensionRevivalTestResponse> handleTsRovTest(
            @RequestBody(required = false) SuspensionRevivalTestRequest request) {

        if (request == null) {
            request = new SuspensionRevivalTestRequest(); // Use defaults
        }

        log.info("Received POST /v1/ts-rov-test with details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = testService.executeTsRovTest(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Setup endpoint - Prepares test data before test execution
     *
     * Creates/resets test notices to known state:
     * - Creates test VON records from scenario.json
     * - Creates test LTA ROV file records
     * - Clears any existing suspension records
     *
     * IMPORTANT: Call this BEFORE running the test
     *
     * POST /v1/ts-rov-test-setup
     *
     * @return Setup result with created record count
     */
    @PostMapping("/ts-rov-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /v1/ts-rov-test-setup");

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
     * Cleanup endpoint - Removes test data after test execution
     *
     * Deletes:
     * - Suspension records created by tests
     * - Test VON records
     * - Test LTA file records
     *
     * IMPORTANT: Call this AFTER running the test to clean up
     *
     * DELETE /v1/ts-rov-test-cleanup
     *
     * @return Cleanup result with deleted record count
     */
    @DeleteMapping("/ts-rov-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /v1/ts-rov-test-cleanup");

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
}
