package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.controllers;

import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services.NotificationSmsEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for manually triggering the Notification SMS and Email workflow.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/notification-sms-email")
public class NotificationSmsEmailController {

    private final NotificationSmsEmailService notificationService;
    
    @Value("${api.version}")
    private String apiVersion;

    public NotificationSmsEmailController(NotificationSmsEmailService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Endpoint to manually trigger the main notification flow
     * 
     * @return CompletableFuture with structured response containing success status and message
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeNotification() {
        log.info("Manual trigger for notification SMS and email workflow");
        
        return notificationService.processMainFlow()
                .thenApply(result -> {
                    log.info("Notification workflow completed with result: {}", result);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", result);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error executing notification workflow: {}", ex.getMessage(), ex);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Error executing notification workflow: " + ex.getMessage());
                    return ResponseEntity.status(500).body(response);
                });
    }

    /**
     * Endpoint to manually trigger the retry notification flow
     * 
     * @return CompletableFuture with structured response containing success status and message
     */
    @PostMapping("/retry")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeRetryNotification() {
        log.info("Manual trigger for retry notification SMS and email workflow");
        
        return notificationService.processRetryFlow()
                .thenApply(result -> {
                    log.info("Retry notification workflow completed with result: {}", result);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", result);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error executing retry notification workflow: {}", ex.getMessage(), ex);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Error executing retry notification workflow: " + ex.getMessage());
                    return ResponseEntity.status(500).body(response);
                });
    }
}
