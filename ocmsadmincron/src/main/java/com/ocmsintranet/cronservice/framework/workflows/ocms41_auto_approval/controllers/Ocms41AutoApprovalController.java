package com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.controllers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.jobs.Ocms41AutoApprovalJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for OCMS 41: Auto-Approval for Furnished Submissions
 *
 * Provides REST endpoints to manually trigger the auto-approval process
 * for furnished submissions that have been synced from Internet DB.
 */
@RestController
@RequestMapping("/${api.version}/ocms41/auto-approval")
@Slf4j
@RequiredArgsConstructor
public class Ocms41AutoApprovalController {

    private final Ocms41AutoApprovalJob autoApprovalJob;

    @Value("${ocms41.auto-approval.enabled:true}")
    private boolean autoApprovalEnabled;

    /**
     * Execute the OCMS 41 Auto-Approval process
     *
     * This endpoint triggers the auto-approval workflow for furnished submissions:
     * - Processes ocms_furnish_application WHERE status = 'S'
     * - Runs 6 validation checks for each submission
     * - Auto-approves or routes to manual review based on results
     *
     * @return ResponseEntity containing the auto-approval execution status and results
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeAutoApproval() {
        log.info("REST request to execute OCMS 41: Auto-Approval Process");

        Map<String, Object> response = new HashMap<>();

        // Check if auto-approval feature is enabled
        if (!autoApprovalEnabled) {
            log.warn("OCMS 41 auto-approval feature is disabled. Skipping auto-approval.");
            response.put("success", false);
            response.put("enabled", false);
            response.put("message", "OCMS 41 auto-approval is disabled. Enable it by setting ocms41.auto-approval.enabled=true");
            return ResponseEntity.ok(response);
        }

        try {
            // Execute the auto-approval job
            var jobResultFuture = autoApprovalJob.execute();
            var jobResult = jobResultFuture.get(); // Wait for completion

            // Prepare response
            response.put("success", jobResult.isSuccess());
            response.put("message", jobResult.getMessage());
            response.put("description", "OCMS 41: Auto-approval process for furnished submissions");

            log.info("OCMS 41 auto-approval completed: {}", jobResult.isSuccess() ? "SUCCESS" : "FAILED");

            return jobResult.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            log.error("Error executing OCMS 41 auto-approval", e);
            response.put("error", "Error executing OCMS 41 auto-approval: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
