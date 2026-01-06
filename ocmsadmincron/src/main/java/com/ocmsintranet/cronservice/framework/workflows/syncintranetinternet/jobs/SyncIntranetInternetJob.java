package com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet.SyncIntranetInternetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Process 7: Batch Cron Sync Job (Push Intranet to Internet)
 *
 * This job synchronizes records from Intranet database to Internet database where is_sync = false.
 * It runs periodically as a scheduled cron job to handle:
 * 1. Failed immediate syncs from other processes (Process 1, 6)
 * 2. Deferred syncs from processes that don't sync immediately (Process 2-5)
 *
 * The job processes two types of records:
 * - VON (Valid Offence Notice) → eVON (Internet DB)
 * - ONOD (Offence Notice Owner Driver) → eONOD (PII DB)
 *
 * For each record with is_sync = false:
 * 1. Check if corresponding record exists in target DB
 * 2. INSERT if not exists, UPDATE if exists
 * 3. Set is_sync = true in source record
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncIntranetInternetJob extends TrackedCronJobTemplate {

    private final SyncIntranetInternetService syncService;

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for Batch Cron Sync");

        if (syncService == null) {
            log.error("SyncIntranetInternetService is not initialized");
            return false;
        }

        log.info("Pre-conditions validated successfully");
        return true;
    }

    @Override
    protected JobResult doExecute() {
        try {
            log.info("=== Starting Batch Cron Sync (Intranet to Internet) ===");

            // Execute the sync service
            syncService.syncIntranetToInternet();

            log.info("=== Batch Cron Sync completed successfully ===");
            return new JobResult(true, "Batch cron sync completed successfully");

        } catch (Exception e) {
            log.error("Batch cron sync failed with error", e);
            return new JobResult(false, "Batch cron sync failed: " + e.getMessage());
        }
    }

    @Override
    protected String getJobName() {
        return "Batch Cron Sync (Intranet to Internet)";
    }
}
