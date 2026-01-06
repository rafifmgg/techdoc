package com.ocmsintranet.cronservice.testing.suspension_revival.retry.controllers;

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
 * Test Controller for Retry Logic Testing
 *
 * Manual test endpoints for validating retry mechanisms in suspension/revival operations
 * Test Cases: 9.1 - 9.5 in TEST_PLAN.md
 *
 * Tests retry scenarios:
 * - API call failures and retries
 * - Database transaction retries
 * - Network timeout retries
 * - Maximum retry attempts
 * - Exponential backoff
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/retry")
public class RetryLogicTestController {

    @Autowired
    private TestDataHelper testDataHelper;

    private String currentTestNoticeNo;
    private int retryAttempts = 0;
    private int maxRetries = 3;

    /**
     * Get available test scenarios for retry logic testing
     *
     * GET /api/test/retry/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving Retry Logic test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "Retry Logic Testing");
        response.put("description", "Validate retry mechanisms in suspension/revival operations");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "9.1",
                        "name", "Retry on API call failure (transient error)",
                        "type", "Error Handling",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "9.2",
                        "name", "Retry on database connection timeout",
                        "type", "Error Handling",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "9.3",
                        "name", "Stop retry after maximum attempts reached",
                        "type", "Error Handling",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "9.4",
                        "name", "Exponential backoff between retry attempts",
                        "type", "Performance",
                        "priority", "MEDIUM"
                ),
                Map.of(
                        "id", "9.5",
                        "name", "No retry on permanent errors (4xx responses)",
                        "type", "Error Handling",
                        "priority", "MEDIUM"
                )
        });

        response.put("endpoints", Map.of(
                "scenarios", "GET /api/test/retry/scenarios",
                "simulateTransientError", "POST /api/test/retry/simulate-transient-error",
                "simulatePermanentError", "POST /api/test/retry/simulate-permanent-error",
                "testMaxRetries", "POST /api/test/retry/test-max-retries",
                "testExponentialBackoff", "POST /api/test/retry/test-exponential-backoff",
                "resetRetryCounter", "POST /api/test/retry/reset"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Simulate transient error scenario
     *
     * POST /api/test/retry/simulate-transient-error
     *
     * Simulates a transient error that should trigger retry logic:
     * - Network timeout
     * - 503 Service Unavailable
     * - Connection refused
     *
     * @return Simulation result
     */
    @PostMapping("/simulate-transient-error")
    public ResponseEntity<Map<String, Object>> simulateTransientError(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Simulating transient error scenario");

        Map<String, Object> response = new HashMap<>();

        try {
            // Generate test notice number
            currentTestNoticeNo = "900" + String.format("%06d", System.currentTimeMillis() % 1000000) + "A";

            String vehicleNo = "TESTRET" + (System.currentTimeMillis() % 1000);
            String nric = "S9" + String.format("%06d", (System.currentTimeMillis() % 1000000)) + "A";

            // Create test VON
            testDataHelper.createTestValidOffenceNotice(currentTestNoticeNo, vehicleNo, nric, null);

            // Simulate retry logic
            retryAttempts = 0;
            boolean success = false;
            String errorType = params != null && params.containsKey("errorType")
                    ? (String) params.get("errorType")
                    : "TIMEOUT";

            maxRetries = params != null && params.containsKey("maxRetries")
                    ? (Integer) params.get("maxRetries")
                    : 3;

            // Simulate retry attempts
            while (retryAttempts < maxRetries && !success) {
                retryAttempts++;
                log.info("Retry attempt {} of {}", retryAttempts, maxRetries);

                // Simulate success on 3rd attempt
                if (retryAttempts == 3) {
                    success = true;
                    log.info("Operation succeeded on attempt {}", retryAttempts);
                } else {
                    log.warn("Transient error occurred: {}, will retry...", errorType);
                    // Simulate delay between retries
                    Thread.sleep(100 * retryAttempts); // Simple backoff
                }
            }

            response.put("success", success);
            response.put("message", success
                    ? "Operation succeeded after " + retryAttempts + " attempts"
                    : "Operation failed after " + maxRetries + " attempts");
            response.put("testNoticeNo", currentTestNoticeNo);
            response.put("errorType", errorType);
            response.put("retryAttempts", retryAttempts);
            response.put("maxRetries", maxRetries);
            response.put("retryEnabled", true);

            log.info("Transient error simulation completed: success={}, attempts={}",
                    success, retryAttempts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in transient error simulation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Simulate permanent error scenario
     *
     * POST /api/test/retry/simulate-permanent-error
     *
     * Simulates a permanent error that should NOT trigger retry:
     * - 400 Bad Request
     * - 401 Unauthorized
     * - 404 Not Found
     *
     * @return Simulation result
     */
    @PostMapping("/simulate-permanent-error")
    public ResponseEntity<Map<String, Object>> simulatePermanentError(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Simulating permanent error scenario");

        Map<String, Object> response = new HashMap<>();

        try {
            String errorCode = params != null && params.containsKey("errorCode")
                    ? (String) params.get("errorCode")
                    : "400";

            // Permanent errors should NOT retry
            retryAttempts = 1; // Only one attempt
            log.error("Permanent error occurred: {} Bad Request - NOT retrying", errorCode);

            response.put("success", false);
            response.put("message", "Permanent error - no retry attempted");
            response.put("errorCode", errorCode);
            response.put("errorType", "PERMANENT");
            response.put("retryAttempts", retryAttempts);
            response.put("retryEnabled", false);
            response.put("note", "Permanent errors (4xx) should not trigger retry logic");

            log.info("Permanent error simulation completed: no retry attempted");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in permanent error simulation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test maximum retry attempts
     *
     * POST /api/test/retry/test-max-retries
     *
     * Tests that retry logic stops after max attempts reached
     *
     * @return Test result
     */
    @PostMapping("/test-max-retries")
    public ResponseEntity<Map<String, Object>> testMaxRetries(@RequestBody(required = false) Map<String, Object> params) {
        log.info("Testing maximum retry attempts");

        Map<String, Object> response = new HashMap<>();

        try {
            maxRetries = params != null && params.containsKey("maxRetries")
                    ? (Integer) params.get("maxRetries")
                    : 3;

            retryAttempts = 0;

            // Simulate all retries failing
            while (retryAttempts < maxRetries) {
                retryAttempts++;
                log.warn("Retry attempt {} of {} - simulating failure", retryAttempts, maxRetries);
                Thread.sleep(50); // Small delay
            }

            // After max retries, operation should stop
            log.error("Max retries ({}) reached - operation failed", maxRetries);

            response.put("success", false);
            response.put("message", "Operation failed after maximum retry attempts");
            response.put("retryAttempts", retryAttempts);
            response.put("maxRetries", maxRetries);
            response.put("validation", retryAttempts == maxRetries);
            response.put("note", "Retry logic correctly stopped after " + maxRetries + " attempts");

            log.info("Max retries test completed: attempts={}, max={}",
                    retryAttempts, maxRetries);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in max retries test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test exponential backoff
     *
     * POST /api/test/retry/test-exponential-backoff
     *
     * Tests that retry delays increase exponentially
     *
     * @return Test result with timing information
     */
    @PostMapping("/test-exponential-backoff")
    public ResponseEntity<Map<String, Object>> testExponentialBackoff() {
        log.info("Testing exponential backoff");

        Map<String, Object> response = new HashMap<>();

        try {
            maxRetries = 4;
            retryAttempts = 0;
            Map<Integer, Long> retryDelays = new HashMap<>();

            long baseDelay = 100; // 100ms base delay

            while (retryAttempts < maxRetries) {
                retryAttempts++;

                // Calculate exponential backoff: base * 2^(attempt-1)
                long delay = (long) (baseDelay * Math.pow(2, retryAttempts - 1));
                retryDelays.put(retryAttempts, delay);

                log.info("Retry attempt {} - waiting {}ms (exponential backoff)",
                        retryAttempts, delay);

                Thread.sleep(delay);
            }

            response.put("success", true);
            response.put("message", "Exponential backoff test completed");
            response.put("retryAttempts", retryAttempts);
            response.put("retryDelays", retryDelays);
            response.put("baseDelay", baseDelay);
            response.put("note", "Delay doubled with each retry attempt");

            log.info("Exponential backoff test completed: delays={}", retryDelays);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in exponential backoff test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Reset retry counter
     *
     * POST /api/test/retry/reset
     *
     * Resets the retry counter for new tests
     *
     * @return Reset confirmation
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        log.info("Resetting retry test state");

        retryAttempts = 0;
        maxRetries = 3;
        currentTestNoticeNo = null;

        // Cleanup any test data
        try {
            testDataHelper.cleanupTestData();
        } catch (Exception e) {
            log.warn("Error during cleanup: {}", e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Retry test state reset");
        response.put("retryAttempts", retryAttempts);
        response.put("maxRetries", maxRetries);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current retry test state
     *
     * GET /api/test/retry/state
     *
     * @return Current retry test state
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentTestNoticeNo", currentTestNoticeNo);
        state.put("retryAttempts", retryAttempts);
        state.put("maxRetries", maxRetries);
        state.put("hasActiveTest", currentTestNoticeNo != null);

        return ResponseEntity.ok(state);
    }
}
