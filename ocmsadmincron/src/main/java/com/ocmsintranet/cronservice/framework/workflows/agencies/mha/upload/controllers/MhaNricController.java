package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.jobs.MhaNricUploadJob;

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
 * REST controller for MHA NRIC verification operations.
 * Provides endpoints to manually trigger MHA NRIC upload and download jobs.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/mha")
@RequiredArgsConstructor
public class MhaNricController {

    private final MhaNricUploadJob mhaNricUploadJob;

    /**
     * Trigger MHA NRIC upload job manually.
     * @return Job execution result
     */
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerUpload() {
        log.info("Manual trigger for MHA NRIC upload job");
        
        return mhaNricUploadJob.execute()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "MhaNricUploadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error triggering MHA NRIC upload job: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                response.put("jobName", "MhaNricUploadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.internalServerError().body(response);
            });
    }

    // Download job implementation has been removed as per previous discussion
}
