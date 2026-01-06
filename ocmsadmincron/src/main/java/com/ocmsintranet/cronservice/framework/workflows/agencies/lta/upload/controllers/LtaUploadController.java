package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.services.LtaUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for LTA upload operations.
 * Provides endpoints to manually trigger LTA upload job and handle SLIFT callbacks.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/lta")
@RequiredArgsConstructor
public class LtaUploadController {

    private final LtaUploadService ltaUploadService;

    /**
     * Trigger LTA upload job manually.
     * @return Job execution result
     */
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerUpload() {
        log.info("Manual trigger for LTA upload job");
        
        return ltaUploadService.executeLtaUpload()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "LtaUploadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error triggering LTA upload job: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                response.put("jobName", "LtaUploadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.internalServerError().body(response);
            });
    }
}
