package com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.services.Ocms41AutoApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OCMS 41: Auto-Approval Job for Furnished Submissions
 *
 * This job automatically reviews newly synced furnished submissions (status = 'S')
 * and determines whether they qualify for auto-approval or require manual review.
 *
 * Schedule: After sync job completes (every 5 minutes)
 * Purpose: Process furnished submissions through 6 validation checks
 *
 * The job processes:
 * - ocms_furnish_application WHERE status = 'S' (Submitted)
 *
 * For each submitted record:
 * - Run 6 validation checks (PS, HST, FIN/Passport, Furnishable Stage, Prior Submission, Existing Offender)
 * - If ALL checks PASS → Auto-approve (add offender, change stage, status = 'A')
 * - If ANY check FAILS → Manual review (create TS-PDP suspension, status = 'P')
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ocms41AutoApprovalJob extends TrackedCronJobTemplate {

    private final Ocms41AutoApprovalService autoApprovalService;

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for OCMS 41 Auto-Approval");

        if (autoApprovalService == null) {
            log.error("Ocms41AutoApprovalService is not initialized");
            return false;
        }

        log.info("Pre-conditions validated successfully");
        return true;
    }

    @Override
    protected JobResult doExecute() {
        try {
            log.info("=== Starting OCMS 41: Auto-Approval Process ===");

            // Execute the auto-approval service
            autoApprovalService.processAutoApproval();

            log.info("=== OCMS 41 Auto-Approval completed successfully ===");
            return new JobResult(true, "OCMS 41 auto-approval completed successfully");

        } catch (Exception e) {
            log.error("OCMS 41 auto-approval failed with error", e);
            return new JobResult(false, "OCMS 41 auto-approval failed: " + e.getMessage());
        }
    }

    @Override
    protected String getJobName() {
        return "OCMS 41: Auto-Approval for Furnished Submissions";
    }
}
