package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.controllers;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services.LtaDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for manually triggering the LTA download job
 */
@Slf4j
@RestController
@RequestMapping("${api.version}/lta/download")
public class LtaDownloadController {

    private final LtaDownloadService ltaDownloadService;

    public LtaDownloadController(LtaDownloadService ltaDownloadService) {
        this.ltaDownloadService = ltaDownloadService;
    }

    /**
     * Manually trigger the LTA download job
     * 
     * @return Job execution result
     */
    @PostMapping("/manual")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> manualTrigger() {
        log.info("Manual trigger of LTA download job");
        
        return ltaDownloadService.executeLtaDownloadJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "LtaDownloadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return result.isSuccess() ? 
                    ResponseEntity.ok(response) : 
                    ResponseEntity.badRequest().body(response);
            })
            .exceptionally(e -> {
                log.error("Error executing LTA download job: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                response.put("jobName", "LtaDownloadJob");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.internalServerError().body(response);
            });
    }
}
