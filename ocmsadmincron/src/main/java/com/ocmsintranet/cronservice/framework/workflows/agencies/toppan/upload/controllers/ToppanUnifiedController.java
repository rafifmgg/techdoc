package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.schedulers.ToppanUnifiedScheduler;
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
 * Controller for manually triggering the unified Toppan letters generation job
 * This processes all 6 stages (RD1, RD2, RR3, DN1, DN2, DR3) in sequence
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/toppan")
@RequiredArgsConstructor
public class ToppanUnifiedController {
    
    private final ToppanUnifiedScheduler toppanUnifiedScheduler;
    
    /**
     * Trigger the unified Toppan letters generation job
     * This will process all stages sequentially: RD1 -> RD2 -> RR3 -> DN1 -> DN2 -> DR3
     * 
     * @return Job execution result with details of all stages processed
     */
    @PostMapping("/generate-letters")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateToppanLetters() {
        log.info("Manual trigger for unified Toppan letters generation (all stages)");
        
        return toppanUnifiedScheduler.triggerManualExecution()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "generate_toppan_letters");
                response.put("description", "Unified Toppan letters generation for all stages");
                response.put("stages", "RD1, RD2, RR3, DN1, DN2, DR3");
                response.put("timestamp", System.currentTimeMillis());
                
                if (result.isSuccess()) {
                    log.info("Unified Toppan letters generation completed successfully: {}", result.getMessage());
                    return ResponseEntity.ok(response);
                } else {
                    log.error("Unified Toppan letters generation failed: {}", result.getMessage());
                    // Framework will handle email notification for failures
                    return ResponseEntity.internalServerError().body(response);
                }
            })
            .exceptionally(e -> {
                log.error("Error executing unified Toppan letters generation: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error: " + e.getMessage());
                response.put("jobName", "generate_toppan_letters");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.internalServerError().body(response);
            });
    }
}