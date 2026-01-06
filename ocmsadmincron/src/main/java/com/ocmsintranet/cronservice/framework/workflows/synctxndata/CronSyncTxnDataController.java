package com.ocmsintranet.cronservice.framework.workflows.synctxndata;

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
 * Controller for manually triggering transaction data synchronization
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/sync-txn-data")
@RequiredArgsConstructor
public class CronSyncTxnDataController {

    private final CronSyncTxnData cronSyncTxnData;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    /**
     * Manually trigger transaction data synchronization
     * Syncs transactions from Internet DB to Intranet DB and applies suspension rules
     *
     * @return Job execution result
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerSync() {
        log.info("Manual trigger for transaction sync job via REST API");

        // Check if payment sync feature is enabled
        if (!paymentSyncEnabled) {
            log.warn("Payment sync feature is disabled. Skipping transaction sync.");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("enabled", false);
            response.put("message", "Payment sync feature is disabled. Enable it by setting payment.sync.enabled=true");
            response.put("jobName", "transaction_sync");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }

        try {
            cronSyncTxnData.syncTransactionData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transaction sync completed successfully. Check logs for details.");
            response.put("jobName", "transaction_sync");
            response.put("description", "Sync transaction data from Internet to Intranet DB");
            response.put("timestamp", System.currentTimeMillis());

            log.info("Transaction sync job completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Exception during transaction sync job", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobName", "transaction_sync");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
