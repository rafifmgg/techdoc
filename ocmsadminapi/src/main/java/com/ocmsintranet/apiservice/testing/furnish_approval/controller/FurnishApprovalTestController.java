package com.ocmsintranet.apiservice.testing.furnish_approval.controller;

import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_approval.service.FurnishApprovalTestService;
import com.ocmsintranet.apiservice.testing.furnish_approval.service.FurnishApprovalTestSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Furnish Approval Integration Testing
 * Supports both Staff Portal and Postman/automated testing
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class FurnishApprovalTestController {

    private final FurnishApprovalTestService testService;
    private final FurnishApprovalTestSetupService setupService;

    @Value("${api.version}")
    private String apiVersion;

    @PostMapping("/${api.version}/furnish-approval-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /furnish-approval-test-setup");
        try {
            Map<String, Object> result = setupService.setupTestData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Setup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/${api.version}/furnish-approval-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /furnish-approval-test-cleanup");
        try {
            Map<String, Object> result = setupService.cleanupTestData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/${api.version}/furnish-approval-test")
    public ResponseEntity<Map<String, Object>> handleStaffPortalTest(@RequestBody(required = false) Map<String, Object> request) {
        log.info("Received POST /furnish-approval-test (Staff Portal)");
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test execution completed");
            result.put("note", "Use POST /v1/test/furnish-approval with scenarioName to run individual scenarios");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Test execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/${api.version}/test/furnish-approval")
    public ResponseEntity<FurnishApprovalTestResponse> executeTest(@RequestBody FurnishApprovalTestRequest request) {
        log.info("Received furnish approval test request - Scenario: {}", request.getScenarioName());

        try {
            FurnishApprovalTestResponse response = testService.executeTest(request);

            HttpStatus status = switch (response.getOverallResult()) {
                case "PASS" -> HttpStatus.OK;
                case "FAIL" -> HttpStatus.OK;
                case "ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
                default -> HttpStatus.OK;
            };

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error executing test - Scenario: {}", request.getScenarioName(), e);

            FurnishApprovalTestResponse errorResponse = FurnishApprovalTestResponse.builder()
                    .scenarioName(request.getScenarioName())
                    .overallResult("ERROR")
                    .errorMessage("Test execution failed: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Furnish Approval Test Controller is running");
    }
}
