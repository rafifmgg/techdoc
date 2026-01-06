package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.jobs.MhaNricDownloadJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for MHA NRIC download operations.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/${api.version}/mha/download")
public class MhaNricDownloadController {

    private final MhaNricDownloadJob mhaNricDownloadJob;

    /**
     * Endpoint to trigger the MHA NRIC download job.
     * 
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Manual trigger for MHA NRIC download job");
        
        return mhaNricDownloadJob.execute()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "MhaNricDownloadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error triggering MHA NRIC download job: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                response.put("jobName", "MhaNricDownloadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to handle SLIFT token callbacks.
     * 
     * @param requestId The request ID for which the token was received
     * @param token The received token
     * @return Response entity with callback handling status
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestParam("requestId") String requestId,
            @RequestParam("token") String token) {
        
        log.info("Received SLIFT token callback for request ID: {}", requestId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Delegate to the job's handleComcryptCallback method
            mhaNricDownloadJob.handleComcryptCallback(requestId, token);
            
            response.put("success", true);
            response.put("message", "Token callback received and being processed");
            response.put("requestId", requestId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing SLIFT token callback for request ID: {}", requestId, e);
            
            response.put("success", false);
            response.put("message", "Error processing token callback: " + e.getMessage());
            response.put("requestId", requestId);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
