package com.ocmsintranet.apiservice.testing.reduction.controller;

import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestRequest;
import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestResponse;
import com.ocmsintranet.apiservice.testing.reduction.service.ReductionTestService;
import com.ocmsintranet.apiservice.testing.reduction.service.ReductionTestSetupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Reduction testing endpoints
 *
 * Provides 3 endpoints:
 * 1. POST /v1/reduction-test-setup - Prepares test data
 * 2. POST /v1/reduction-test - Runs the 4-step test workflow
 * 3. DELETE /v1/reduction-test-cleanup - Cleans up test data
 */
@RestController
@RequestMapping("/v1")
@Slf4j
public class ReductionTestController {

    private final ReductionTestService reductionTestService;
    private final ReductionTestSetupService setupService;

    public ReductionTestController(ReductionTestService reductionTestService,
                                   ReductionTestSetupService setupService) {
        this.reductionTestService = reductionTestService;
        this.setupService = setupService;
    }

    /**
     * Setup endpoint - Prepares test data before test execution
     * Creates/resets test notices to known state
     *
     * IMPORTANT: Call this BEFORE running the reduction test
     *
     * Workflow:
     * 1. Create or reset 14 test notices in VON/eVON
     * 2. Set known initial values (amount_payable, computer_rule_code, last_processing_stage)
     * 3. Clear any existing suspension/log records
     *
     * Can be triggered from Staff Portal
     *
     * @return Setup result with created/reset record counts
     */
    @PostMapping("/reduction-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /reduction-test-setup");

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
     * POST endpoint for Reduction testing
     * Executes 4 steps and returns results for all steps:
     * - Step 1: Load test scenarios from scenario.json
     * - Step 2: Call reduction API for each scenario
     * - Step 3: Fetch verification data from both databases
     * - Step 4: Verify reduction logic and database consistency
     *
     * Can be triggered from Staff Portal
     *
     * @param request ReductionTestRequest containing details and triggerApi flags (default: true)
     * @return ResponseEntity with ReductionTestResponse containing step execution results
     */
    @PostMapping("/reduction-test")
    public ResponseEntity<ReductionTestResponse> handleReductionTest(@RequestBody ReductionTestRequest request) {
        log.info("Received POST /reduction-test with details={}, triggerApi={}",
            request.getDetails(), request.getTriggerApi());

        ReductionTestResponse response = reductionTestService.executeReductionTest(request);

        log.info("Reduction test completed with {} steps", response.getSteps().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Cleanup endpoint - Removes test data after test execution
     * Deletes suspension records, reduction logs, and resets notices
     *
     * IMPORTANT: Call this AFTER running the reduction test to clean up
     *
     * Workflow:
     * 1. Delete suspension records created by tests
     * 2. Delete reduction log records created by tests
     * 3. Reset test notices to initial state (amount_payable, clear suspension fields)
     *
     * Can be triggered from Staff Portal
     *
     * @return Cleanup result with deleted/reset record counts
     */
    @DeleteMapping("/reduction-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /reduction-test-cleanup");

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
