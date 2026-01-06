package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.schedulers.ToppanDownloadScheduler;
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
 * Controller for manually triggering Toppan download job
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/toppan")
@RequiredArgsConstructor
public class ToppanDownloadController {
    
    private final ToppanDownloadScheduler toppanDownloadScheduler;
    
    /**
     * Manually trigger the Toppan download job
     * Downloads and processes response files from Toppan
     * 
     * @return Job execution result
     */
    @PostMapping("/download-responses")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> downloadToppanResponses() {
        log.info("Manual trigger for Toppan download job via REST API");
        
        return toppanDownloadScheduler.triggerManualExecution()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "toppan_download");
                response.put("description", "Download and process Toppan response files");
                response.put("timestamp", System.currentTimeMillis());
                
                if (result.isSuccess()) {
                    log.info("Toppan download job completed: {}", result.getMessage());
                    return ResponseEntity.ok(response);
                } else {
                    log.error("Toppan download job failed: {}", result.getMessage());
                    response.put("error", result.getMessage());
                    return ResponseEntity.internalServerError().body(response);
                }
            })
            .exceptionally(throwable -> {
                log.error("Exception during Toppan download job", throwable);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", throwable.getMessage());
                errorResponse.put("jobName", "toppan_download");
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }
}