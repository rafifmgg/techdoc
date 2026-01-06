package com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.controllers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.jobs.Ocms41SyncFurnishedJob;
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
 * Controller for OCMS 41: Sync Furnished Submissions (Internet → Intranet)
 *
 * Provides REST endpoints to manually trigger the synchronization
 * of furnish hirer/driver submissions from Internet DB to Intranet DB.
 */
@RestController
@RequestMapping("/${api.version}/ocms41/sync-furnished")
@Slf4j
@RequiredArgsConstructor
public class Ocms41SyncFurnishedController {

    private final Ocms41SyncFurnishedJob syncJob;

    @Value("${ocms41.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * Execute the OCMS 41 Furnished Submissions Sync process
     *
     * This endpoint triggers the synchronization of:
     * - eocms_furnish_application (Internet DB) → ocms_furnish_application (Intranet DB)
     *   where is_sync = 'N'
     *
     * @return ResponseEntity containing the sync execution status and results
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeFurnishedSync() {
        log.info("REST request to execute OCMS 41: Sync Furnished Submissions");

        Map<String, Object> response = new HashMap<>();

        // Check if sync feature is enabled
        if (!syncEnabled) {
            log.warn("OCMS 41 furnished sync feature is disabled. Skipping sync.");
            response.put("success", false);
            response.put("enabled", false);
            response.put("message", "OCMS 41 furnished sync is disabled. Enable it by setting ocms41.sync.enabled=true");
            return ResponseEntity.ok(response);
        }

        try {
            // Execute the sync job
            var jobResultFuture = syncJob.execute();
            var jobResult = jobResultFuture.get(); // Wait for completion

            // Prepare response
            response.put("success", jobResult.isSuccess());
            response.put("message", jobResult.getMessage());
            response.put("description", "OCMS 41: Sync furnished submissions from Internet DB to Intranet DB");

            log.info("OCMS 41 furnished sync completed: {}", jobResult.isSuccess() ? "SUCCESS" : "FAILED");

            return jobResult.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            log.error("Error executing OCMS 41 furnished sync", e);
            response.put("error", "Error executing OCMS 41 furnished sync: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
