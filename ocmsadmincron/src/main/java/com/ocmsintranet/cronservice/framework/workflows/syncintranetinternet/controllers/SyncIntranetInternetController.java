package com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.controllers;

import com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.jobs.SyncIntranetInternetJob;
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
 * Controller for Batch Cron Sync operations (Intranet to Internet)
 *
 * Provides REST endpoints to manually trigger the batch synchronization
 * of VON and ONOD records from Intranet DB to Internet/PII DB.
 */
@RestController
@RequestMapping("/${api.version}/batch-sync")
@Slf4j
@RequiredArgsConstructor
public class SyncIntranetInternetController {

    private final SyncIntranetInternetJob syncJob;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    /**
     * Execute the Batch Cron Sync process
     *
     * This endpoint triggers the synchronization of:
     * - VON records (Intranet → Internet DB) where is_sync = false
     * - ONOD records (Intranet → PII DB) where is_sync = false
     *
     * @return ResponseEntity containing the sync execution status and results
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeBatchSync() {
        log.info("REST request to execute Batch Cron Sync (Intranet to Internet)");

        Map<String, Object> response = new HashMap<>();

        // Check if payment sync feature is enabled
        if (!paymentSyncEnabled) {
            log.warn("Payment sync feature is disabled. Skipping batch sync.");
            response.put("success", false);
            response.put("enabled", false);
            response.put("message", "Payment sync feature is disabled. Enable it by setting payment.sync.enabled=true");
            return ResponseEntity.ok(response);
        }

        try {
            // Execute the batch sync job
            var jobResultFuture = syncJob.execute();
            var jobResult = jobResultFuture.get(); // Wait for completion

            // Prepare response
            response.put("success", jobResult.isSuccess());
            response.put("message", jobResult.getMessage());
            response.put("description", "Batch sync from Intranet to Internet/PII databases");

            log.info("Batch sync completed: {}", jobResult.isSuccess() ? "SUCCESS" : "FAILED");

            return jobResult.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            log.error("Error executing batch sync", e);
            response.put("error", "Error executing batch sync: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
