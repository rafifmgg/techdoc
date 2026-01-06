package com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.services.Ocms41SyncFurnishedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OCMS 41: Sync Furnished Submissions Job (Internet → Intranet)
 *
 * This job synchronizes furnish hirer/driver submissions from Internet database to Intranet database
 * where is_sync = false.
 *
 * Schedule: Every 5 minutes (per OCMS 41 specs)
 * Purpose: Sync submissions from eService (Internet DB) to Intranet DB for auto-approval processing
 *
 * The job processes:
 * - eocms_furnish_application (Internet DB) → ocms_furnish_application (Intranet DB)
 *
 * For each record with is_sync = 'N':
 * 1. Decrypt PII fields (auto-handled by JPA)
 * 2. Check if corresponding record exists in Intranet DB
 * 3. INSERT if not exists, UPDATE if exists
 * 4. Set is_sync = 'Y' in Internet DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ocms41SyncFurnishedJob extends TrackedCronJobTemplate {

    private final Ocms41SyncFurnishedService syncService;

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for OCMS 41 Furnished Sync");

        if (syncService == null) {
            log.error("Ocms41SyncFurnishedService is not initialized");
            return false;
        }

        log.info("Pre-conditions validated successfully");
        return true;
    }

    @Override
    protected JobResult doExecute() {
        try {
            log.info("=== Starting OCMS 41: Sync Furnished Submissions (Internet → Intranet) ===");

            // Execute the sync service
            syncService.syncFurnishedSubmissions();

            log.info("=== OCMS 41 Furnished Sync completed successfully ===");
            return new JobResult(true, "OCMS 41 furnished sync completed successfully");

        } catch (Exception e) {
            log.error("OCMS 41 furnished sync failed with error", e);
            return new JobResult(false, "OCMS 41 furnished sync failed: " + e.getMessage());
        }
    }

    @Override
    protected String getJobName() {
        return "OCMS 41: Sync Furnished Submissions (Internet → Intranet)";
    }
}
