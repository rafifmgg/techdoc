package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.controllers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.EmailNotificationResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PSCheckResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PendingApplicationDTO;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.jobs.DailyOfficerNotificationJob;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.jobs.PSCheckJob;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services.Ocms41NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for OCMS 41 notification workflows
 * Provides manual trigger endpoints for testing and ad-hoc execution
 */
@RestController
@RequestMapping("/api/cron/ocms41-notifications")
@RequiredArgsConstructor
@Slf4j
public class Ocms41NotificationController {

    private final Ocms41NotificationService notificationService;
    private final DailyOfficerNotificationJob dailyOfficerNotificationJob;
    private final PSCheckJob psCheckJob;

    /**
     * Get list of pending applications
     * GET /api/cron/ocms41-notifications/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingApplicationDTO>> getPendingApplications() {
        log.info("REST: Getting pending applications");
        List<PendingApplicationDTO> pendingApplications = notificationService.getPendingApplications();
        return ResponseEntity.ok(pendingApplications);
    }

    /**
     * Manually trigger daily officer notification
     * POST /api/cron/ocms41-notifications/send-notification
     */
    @PostMapping("/send-notification")
    public ResponseEntity<String> sendDailyNotification() {
        log.info("REST: Manually triggering daily officer notification");

        CompletableFuture<Object> future = dailyOfficerNotificationJob.execute()
            .thenApply(result -> {
                if (result.isSuccess()) {
                    log.info("Daily officer notification completed successfully: {}", result.getMessage());
                    return ResponseEntity.ok("Notification sent: " + result.getMessage());
                } else {
                    log.error("Daily officer notification failed: {}", result.getMessage());
                    return ResponseEntity.status(500).body("Notification failed: " + result.getMessage());
                }
            });

        return ResponseEntity.accepted().body("Daily officer notification job started");
    }

    /**
     * Manually trigger PS check
     * POST /api/cron/ocms41-notifications/ps-check
     */
    @PostMapping("/ps-check")
    public ResponseEntity<String> runPSCheck() {
        log.info("REST: Manually triggering PS check");

        CompletableFuture<Object> future = psCheckJob.execute()
            .thenApply(result -> {
                if (result.isSuccess()) {
                    log.info("PS check completed successfully: {}", result.getMessage());
                    return ResponseEntity.ok("PS check completed: " + result.getMessage());
                } else {
                    log.error("PS check failed: {}", result.getMessage());
                    return ResponseEntity.status(500).body("PS check failed: " + result.getMessage());
                }
            });

        return ResponseEntity.accepted().body("PS check job started");
    }

    /**
     * Get status of daily officer notification job
     * GET /api/cron/ocms41-notifications/status/notification
     */
    @GetMapping("/status/notification")
    public ResponseEntity<?> getNotificationJobStatus() {
        log.info("REST: Getting daily officer notification job status");
        var statusInfo = dailyOfficerNotificationJob.getStatusInfo();
        return ResponseEntity.ok(statusInfo);
    }

    /**
     * Get status of PS check job
     * GET /api/cron/ocms41-notifications/status/ps-check
     */
    @GetMapping("/status/ps-check")
    public ResponseEntity<?> getPSCheckJobStatus() {
        log.info("REST: Getting PS check job status");
        var statusInfo = psCheckJob.getStatusInfo();
        return ResponseEntity.ok(statusInfo);
    }
}
