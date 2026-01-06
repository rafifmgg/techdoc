package com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers.DipMidForRecheckHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OCMS 19 - FB-1: DIP/MID/FOR Day-End Re-check Job
 *
 * Purpose: Prevent diplomatic/military/foreign vehicles from progressing to next stage without proper suspension
 *
 * Job Logic:
 * 1. Query all notices at RD2/DN2 stage with vehicle_registration_type IN (D, I, F)
 * 2. Check if they have active PS suspension
 * 3. If PS was accidentally revived → Re-apply PS-DIP/PS-MID/PS-FOR
 * 4. Log all re-applications for audit trail
 *
 * Scheduling:
 * - Runs daily at 11:59 PM (end of day)
 * - Before "Prepare for Next Stage" cron (which runs at midnight)
 * - Uses ShedLock to prevent concurrent execution
 *
 * Specification Reference:
 * - v2.0_OCMS_19_Revive_Suspensions_Feedback.md Row 2 "FB-1: Modify Prepare for Next Stage cron"
 *
 * Architecture:
 * - Extends TrackedCronJobTemplate (automatic job history tracking)
 * - Uses DipMidForRecheckHelper for business logic
 * - Uses SuspensionApiClient for PS re-application (via helper)
 * - Individual failures don't stop batch processing
 *
 * @author Claude Code
 * @since 2025-12-21 (OCMS 19)
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("ocms19_dip_mid_for_recheck")
public class DipMidForRecheckJob extends TrackedCronJobTemplate {

    private final DipMidForRecheckHelper dipMidForRecheckHelper;

    @Value("${cron.dipmidfor.recheck.shedlock.name:ocms19_dip_mid_for_recheck}")
    private String jobName;

    public DipMidForRecheckJob(DipMidForRecheckHelper dipMidForRecheckHelper) {
        this.dipMidForRecheckHelper = dipMidForRecheckHelper;
    }

    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("[DIP/MID/FOR Recheck Job] Validating pre-conditions");

        try {
            // Check if required dependencies are initialized
            if (dipMidForRecheckHelper == null) {
                log.error("[DIP/MID/FOR Recheck Job] DipMidForRecheckHelper is not initialized");
                return false;
            }

            log.info("[DIP/MID/FOR Recheck Job] Pre-conditions validated successfully");
            return true;

        } catch (Exception e) {
            log.error("[DIP/MID/FOR Recheck Job] Error validating pre-conditions: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        // Call parent initialize which records job start in history
        super.initialize();

        log.info("[DIP/MID/FOR Recheck Job] Initializing day-end re-check job");
        // No additional initialization needed
    }

    @Override
    protected void cleanup() {
        log.info("[DIP/MID/FOR Recheck Job] Cleaning up job resources");
        // No cleanup needed
        super.cleanup();
    }

    @Override
    protected JobResult doExecute() {
        log.info("[DIP/MID/FOR Recheck Job] Starting day-end re-check execution");

        try {
            // Step 1: Query notices at RD2/DN2 stage needing PS re-check
            List<Map<String, Object>> noticesForRecheck = dipMidForRecheckHelper.queryNoticesForRecheck();

            if (noticesForRecheck.isEmpty()) {
                log.info("[DIP/MID/FOR Recheck Job] No notices found needing PS re-application");
                return new JobResult(true, "No notices need PS re-application");
            }

            log.info("[DIP/MID/FOR Recheck Job] Found {} notices needing PS re-application", noticesForRecheck.size());

            // Step 2: Re-apply PS for each notice
            Map<String, Integer> summary = dipMidForRecheckHelper.processBatchReapplication(noticesForRecheck);

            int successCount = summary.get("successCount");
            int failureCount = summary.get("failureCount");

            // Step 3: Determine overall job success
            // Job is successful if at least one PS was re-applied successfully
            // OR if there were no failures
            boolean overallSuccess = successCount > 0 || (successCount == 0 && failureCount == 0);

            String message = String.format(
                    "DIP/MID/FOR day-end re-check completed. Total: %d PS re-applied, %d failed",
                    successCount, failureCount
            );

            log.info("[DIP/MID/FOR Recheck Job] {}", message);

            return new JobResult(overallSuccess, message);

        } catch (Exception e) {
            log.error("[DIP/MID/FOR Recheck Job] Error executing day-end re-check: {}", e.getMessage(), e);
            return new JobResult(false, "Error: " + e.getMessage());
        }
    }
}
