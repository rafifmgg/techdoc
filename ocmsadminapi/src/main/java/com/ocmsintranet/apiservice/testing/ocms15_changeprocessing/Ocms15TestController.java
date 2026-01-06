package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing;

import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestRequest;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OCMS 15 Change Processing Stage Integration Test Controller
 *
 * REST API endpoints for testing OCMS 15 functionality:
 *
 * Complete Test Workflow (Staff Portal or Postman):
 *
 * Step 1: Setup test data
 *   POST /v1/ocms15-test-setup
 *   - Creates test notices via /staff-create-notice API
 *   - Resets test data to known initial state
 *
 * Step 2: Run the test
 *   POST /v1/ocms15-test
 *   - Executes 4-step integration test
 *   - Returns step results
 *   - Can be triggered by URA staff from Staff Portal
 *
 * Step 3: Cleanup test data
 *   DELETE /v1/ocms15-test-cleanup
 *   - Deletes test notices from VON, ONOD, and change processing tables
 *   - Removes all test-related data
 *
 * Usage Examples:
 *
 * Test OCMS manual flow only:
 * POST /v1/ocms15-test
 * {
 *   "details": true,
 *   "triggerApi": true,
 *   "testOcmsFlow": true,
 *   "testPlusFlow": false
 * }
 *
 * Test PLUS integration flow only:
 * POST /v1/ocms15-test
 * {
 *   "details": true,
 *   "triggerApi": true,
 *   "testOcmsFlow": false,
 *   "testPlusFlow": true
 * }
 *
 * Test both flows:
 * POST /v1/ocms15-test
 * {
 *   "details": true,
 *   "triggerApi": true,
 *   "testOcmsFlow": true,
 *   "testPlusFlow": true
 * }
 *
 * Cleanup specific notices:
 * POST /v1/ocms15-test-cleanup
 * {
 *   "noticeNumbers": ["T41000001X", "T41000002X"]
 * }
 *
 * Cleanup all test notices (DANGEROUS!):
 * POST /v1/ocms15-test-cleanup
 * {
 *   "deleteAll": true
 * }
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class Ocms15TestController {

    @Autowired
    private Ocms15TestService ocms15TestService;

    @Autowired
    private Ocms15TestSetupService setupService;

    /**
     * Setup endpoint - Prepares test data before test execution
     * Creates test notices via /staff-create-notice API
     *
     * IMPORTANT: Call this BEFORE running the test
     *
     * @return Setup result with created notice count
     */
    @PostMapping("/ocms15-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        try {
            log.info("Received POST /v1/ocms15-test-setup");

            Map<String, Object> result = setupService.setupTestData();

            log.info("Setup completed successfully: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Setup failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Execute OCMS 15 integration test (4-step pattern)
     *
     * IMPORTANT: Call /ocms15-test-setup BEFORE running this test
     *
     * @param request Test request with configuration options
     * @return Test response with step results
     */
    @PostMapping("/ocms15-test")
    public ResponseEntity<Ocms15TestResponse> executeTest(@RequestBody(required = false) Ocms15TestRequest request) {
        try {
            // Use defaults if request is null
            if (request == null) {
                request = new Ocms15TestRequest();
            }

            log.info("Received POST /v1/ocms15-test: OCMS={}, PLUS={}, TriggerAPI={}, Details={}",
                request.getTestOcmsFlow(), request.getTestPlusFlow(), request.getTriggerApi(), request.getDetails());

            Ocms15TestResponse response = ocms15TestService.executeTest(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing OCMS 15 test: {}", e.getMessage(), e);

            Ocms15TestResponse errorResponse = new Ocms15TestResponse();
            com.ocmsintranet.apiservice.testing.main.StepResult errorStep = new com.ocmsintranet.apiservice.testing.main.StepResult();
            errorStep.setStepName("Error");
            errorStep.setSuccess(false);
            errorStep.setMessage("Test execution failed: " + e.getMessage());
            errorStep.setDetails(java.util.Map.of("error", e.getMessage()));
            errorResponse.addStep(errorStep);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cleanup endpoint - Removes test data after test execution
     * Deletes test notices from VON, ONOD, and change processing tables
     *
     * IMPORTANT: Call this AFTER running the test to clean up
     *
     * @return Cleanup result with deleted notice count
     */
    @DeleteMapping("/ocms15-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        try {
            log.info("Received DELETE /v1/ocms15-test-cleanup");

            Map<String, Object> result = setupService.cleanupTestData();

            log.info("Cleanup completed successfully: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
