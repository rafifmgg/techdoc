package com.ocmsintranet.cronservice.testing.suspension_revival.notification.controllers;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import com.ocmsintranet.cronservice.testing.suspension_revival.notification.service.NotificationTestService;
import com.ocmsintranet.cronservice.testing.suspension_revival.notification.service.NotificationTestSetupService;
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
public class NotificationTestController {

    @Autowired
    private NotificationTestService testService;

    @Autowired
    private NotificationTestSetupService setupService;

    @PostMapping("/notification-test")
    public ResponseEntity<SuspensionRevivalTestResponse> handleNotificationTest(
            @RequestBody(required = false) SuspensionRevivalTestRequest request) {

        if (request == null) {
            request = new SuspensionRevivalTestRequest();
        }

        log.info("Received POST /v1/notification-test with details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = testService.executeNotificationTest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notification-test-setup")
    public ResponseEntity<Map<String, Object>> handleSetup() {
        log.info("Received POST /v1/notification-test-setup");

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

    @DeleteMapping("/notification-test-cleanup")
    public ResponseEntity<Map<String, Object>> handleCleanup() {
        log.info("Received DELETE /v1/notification-test-cleanup");

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
