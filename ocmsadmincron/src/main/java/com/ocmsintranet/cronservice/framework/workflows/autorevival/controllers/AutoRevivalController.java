package com.ocmsintranet.cronservice.framework.workflows.autorevival.controllers;

import com.ocmsintranet.cronservice.framework.workflows.autorevival.services.AutoRevivalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Auto-Revival workflow
 *
 * Provides manual trigger endpoint for the auto-revival job
 * Primarily used for testing and ad-hoc execution
 */
@Slf4j
@RestController
@RequestMapping("/api/cron/auto-revival")
public class AutoRevivalController {

    private final AutoRevivalService autoRevivalService;

    public AutoRevivalController(AutoRevivalService autoRevivalService) {
        this.autoRevivalService = autoRevivalService;
    }

    /**
     * Manually trigger the auto-revival job
     *
     * POST /api/cron/auto-revival/trigger
     *
     * Use this endpoint to manually execute the auto-revival job
     * without waiting for the scheduled execution
     *
     * @return ResponseEntity with job result
     */
    @PostMapping("/trigger")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerAutoRevival() {
        log.info("Manual trigger requested for auto-revival job");

        return autoRevivalService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", result);

                log.info("Manual auto-revival job completed: {}", result);
                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());

                log.error("Error in manual auto-revival job: {}", e.getMessage(), e);
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
            });
    }

    /**
     * Health check endpoint
     *
     * GET /api/cron/auto-revival/health
     *
     * @return Simple health check response
     */
    @PostMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "auto-revival");
        return ResponseEntity.ok(response);
    }
}
