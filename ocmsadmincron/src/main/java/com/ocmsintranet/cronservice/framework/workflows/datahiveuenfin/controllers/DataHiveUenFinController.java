package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.jobs.DataHiveUenFinJob;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.helpers.DataHiveUenFinHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for DataHive UEN & FIN cron operations
 */
@RestController
@RequestMapping("/${api.version}/datahive-uen-fin")
@Slf4j
@RequiredArgsConstructor
public class DataHiveUenFinController {

    private final DataHiveUenFinJob dataHiveUenFinJob;
    private final DataHiveUenFinHelper dataHiveUenFinHelper;

    /**
     * Execute the DataHive UEN & FIN synchronization process
     *
     * @return ResponseEntity containing the process execution status and results
     */
    @PostMapping("/execute-sync")
    public ResponseEntity<Map<String, Object>> executeDataHiveSync() {
        log.info("REST request to execute DataHive UEN & FIN synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            // Execute the DataHive synchronization job
            var jobResultFuture = dataHiveUenFinJob.execute();
            var jobResult = jobResultFuture.get(); // Wait for completion

            // Prepare response
            response.put("success", jobResult.isSuccess());
            response.put("message", jobResult.getMessage());

            return jobResult.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            log.error("Error executing DataHive UEN & FIN synchronization", e);
            response.put("error", "Error executing sync: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
