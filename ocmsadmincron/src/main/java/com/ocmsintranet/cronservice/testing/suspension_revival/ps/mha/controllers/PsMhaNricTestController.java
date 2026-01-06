package com.ocmsintranet.cronservice.testing.suspension_revival.ps.mha.controllers;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import com.ocmsintranet.cronservice.testing.suspension_revival.ps.mha.service.PsMhaNricTestService;
import com.ocmsintranet.cronservice.testing.suspension_revival.ps.mha.service.PsMhaNricTestSetupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1")
@Profile("!prod")
public class PsMhaNricTestController {

    @Autowired
    private PsMhaNricTestService testService;

    @Autowired
    private PsMhaNricTestSetupService setupService;

    @PostMapping("/ps-mha-nric-test")
    public ResponseEntity<SuspensionRevivalTestResponse> handlePsMhaNricTest(
            @RequestBody(required = false) SuspensionRevivalTestRequest request) {

        if (request == null) {
            request = new SuspensionRevivalTestRequest();
        }

        log.info("Received POST /v1/ps-mha-nric-test with details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = testService.executePsMhaNricTest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ps-mha-nric-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /v1/ps-mha-nric-test-setup");

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

    @DeleteMapping("/ps-mha-nric-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /v1/ps-mha-nric-test-cleanup");

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
