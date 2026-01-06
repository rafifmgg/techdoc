package com.ocmsintranet.cronservice.framework.workflows.autorevival.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.autorevival.helpers.AutoRevivalHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Auto-Revival Job
 *
 * This job performs automatic revival of suspended notices when their due_date_of_revival is reached.
 *
 * Flow:
 * 1. Query all suspended notices with due_date_of_revival <= TODAY and date_of_revival IS NULL
 * 2. Filter for TS (Temporary Suspension) types only
 * 3. For each notice:
 *    a. Call SuspensionApiClient.applyRevival() via helper
 *    b. API handles all DB updates (suspended_notice, VON, isSync, etc.)
 *    c. Log success or failure
 *    d. Continue processing other notices even if one fails
 * 4. Return summary with success/failure counts
 *
 * Architecture:
 * - Extends TrackedCronJobTemplate (automatic job history tracking)
 * - Uses AutoRevivalHelper for business logic
 * - NO direct DB access - all through SuspensionApiClient (API)
 * - Individual failures don't stop batch processing
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("suspension_auto_revival")
public class AutoRevivalJob extends TrackedCronJobTemplate {

    private final AutoRevivalHelper autoRevivalHelper;

    @Value("${cron.autorevival.shedlock.name:suspension_auto_revival}")
    private String jobName;

    public AutoRevivalJob(AutoRevivalHelper autoRevivalHelper) {
        this.autoRevivalHelper = autoRevivalHelper;
    }

    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for auto-revival job");

        try {
            // Check if required dependencies are initialized
            if (autoRevivalHelper == null) {
                log.error("AutoRevivalHelper is not initialized");
                return false;
            }

            log.info("Pre-conditions validated successfully");
            return true;

        } catch (Exception e) {
            log.error("Error validating pre-conditions: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        // Call parent initialize which records job start in history
        super.initialize();

        log.info("Initializing auto-revival job");
        // No additional initialization needed
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up auto-revival job resources");
        // No cleanup needed
        super.cleanup();
    }

    @Override
    protected JobResult doExecute() {
        log.info("Starting auto-revival job execution");

        try {
            int totalSuccess = 0;
            int totalFailures = 0;

            // Step 1: Process standard revivals (existing logic)
            List<Map<String, Object>> noticesForRevival = autoRevivalHelper.queryNoticesForRevival();

            if (!noticesForRevival.isEmpty()) {
                log.info("Found {} notices needing standard revival", noticesForRevival.size());
                Map<String, Integer> standardSummary = autoRevivalHelper.processBatchRevivals(noticesForRevival);
                totalSuccess += standardSummary.get("successCount");
                totalFailures += standardSummary.get("failureCount");
            } else {
                log.info("No suspended notices found needing standard revival");
            }

            // Step 2: Process TS-OLD auto-revival for VIP vehicles (OCMS 14)
            Map<String, Integer> tsOldSummary = autoRevivalHelper.processTsOldRevival();
            totalSuccess += tsOldSummary.get("successCount");
            totalFailures += tsOldSummary.get("failureCount");

            // Step 3: Process TS-CLV looping for VIP vehicles at RR3/DR3 (OCMS 14)
            Map<String, Integer> tsClvSummary = autoRevivalHelper.processTsClvLooping();
            totalSuccess += tsClvSummary.get("successCount");
            totalFailures += tsClvSummary.get("failureCount");

            // Step 4: Determine overall job success
            // Job is successful if at least one operation succeeded OR if there were no notices to process
            boolean overallSuccess = totalSuccess > 0 || (totalSuccess == 0 && totalFailures == 0);

            String message = String.format(
                "Auto-revival job completed. Total: %d successful, %d failed",
                totalSuccess, totalFailures
            );

            log.info(message);

            return new JobResult(overallSuccess, message);

        } catch (Exception e) {
            log.error("Error executing auto-revival job: {}", e.getMessage(), e);
            return new JobResult(false, "Error: " + e.getMessage());
        }
    }
}
