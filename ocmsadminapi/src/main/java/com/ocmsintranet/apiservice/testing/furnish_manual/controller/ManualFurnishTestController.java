package com.ocmsintranet.apiservice.testing.furnish_manual.controller;

import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_manual.service.ManualFurnishTestService;
import com.ocmsintranet.apiservice.testing.furnish_manual.service.ManualFurnishTestSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishTestController {

    private final ManualFurnishTestSetupService setupService;
    private final ManualFurnishTestService testService;

    @Value("${api.version}")
    private String apiVersion;

    @PostMapping("/${api.version}/manual-furnish-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /manual-furnish-test-setup");
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

    @DeleteMapping("/${api.version}/manual-furnish-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /manual-furnish-test-cleanup");
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

    @PostMapping("/${api.version}/manual-furnish-test")
    public ResponseEntity<Map<String, Object>> handleStaffPortalTest(@RequestBody(required = false) Map<String, Object> request) {
        log.info("Received POST /manual-furnish-test (Staff Portal)");
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test execution completed");
            result.put("note", "Use POST /v1/test/furnish-manual with scenarioName to run individual scenarios");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Test execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/${api.version}/test/furnish-manual")
    public ResponseEntity<ManualFurnishTestResponse> runIndividualScenario(@RequestBody ManualFurnishTestRequest request) {
        log.info("Received POST /test/furnish-manual - Individual scenario test");
        log.info("Scenario: {}", request.getScenarioName());

        try {
            ManualFurnishTestResponse response = testService.executeTest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Individual scenario test failed: {}", e.getMessage(), e);
            ManualFurnishTestResponse errorResponse = ManualFurnishTestResponse.builder()
                    .scenarioName(request.getScenarioName())
                    .overallResult("FAILED")
                    .errorMessage("Test execution failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
