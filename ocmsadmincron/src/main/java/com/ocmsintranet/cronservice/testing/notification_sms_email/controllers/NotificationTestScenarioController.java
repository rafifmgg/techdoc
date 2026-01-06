package com.ocmsintranet.cronservice.testing.notification_sms_email.controllers;

import com.ocmsintranet.cronservice.testing.notification_sms_email.services.NotificationSmsEmailTestService;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for testing notification SMS/Email workflow with HST checking.
 * This controller provides endpoints to run tests for the notification process.
 */
@RestController
@RequestMapping("/test/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestScenarioController {

    private final NotificationSmsEmailTestService notificationSmsEmailTestService;

    @Value("${test.endpoints.enabled:false}")
    private boolean testEndpointsEnabled;

    /**
     * Endpoint to run the notification SMS/Email test flow.
     * This test simulates the end-to-end process of notification with HST checking.
     *
     * @return List of test step results
     */
    @PostMapping("/run-test")
    public ResponseEntity<List<TestStepResult>> runNotificationTest() {
        if (!testEndpointsEnabled) {
            log.warn("Test endpoints are disabled. Enable with test.endpoints.enabled=true");
            return ResponseEntity.badRequest().build();
        }

        log.info("🚀 Starting Notification SMS/Email Test Flow");
        List<TestStepResult> results = notificationSmsEmailTestService.runTest();

        // Count successes and failures
        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failureCount = results.size() - successCount;

        log.info("📊 Test Flow Summary: {} steps total, {} succeeded, {} failed",
                results.size(), successCount, failureCount);

        if (failureCount > 0) {
            log.warn("⚠️ Some test steps failed. Check results for details.");
        } else {
            log.info("✅ All test steps completed successfully!");
        }

        return ResponseEntity.ok(results);
    }
}
