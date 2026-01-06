package com.ocmsintranet.cronservice.framework.workflows.ocms41_auto_approval.services;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst.OcmsHstService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OCMS 41: Auto-Approval Service
 *
 * Automatically reviews furnished submissions and determines whether they:
 * (a) Can be auto-approved, OR
 * (b) Require manual review by an OIC
 *
 * Auto-Approval Process (6 validation checks):
 * 1. Check for active Permanent Suspension (PS)
 * 2. Check if Furnished ID is an HST ID
 * 3. Check if Furnished ID type is FIN or Passport
 * 4. Check if Notice is still in Furnishable Stage
 * 5. Check if there was a prior Furnish submission
 * 6. Check existing Hirer/Driver and Furnished ID
 *
 * If ALL checks PASS → Auto-Approve (add offender, change stage)
 * If ANY check FAILS → Manual Review (create TS-PDP suspension, status = Pending)
 *
 * Based on Functional Spec v1.1 Section 2.4.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ocms41AutoApprovalService {

    private final OcmsFurnishApplicationService furnishService;
    private final OcmsValidOffenceNoticeService noticeService;
    private final OcmsSuspendedNoticeService suspensionService;
    private final OcmsHstService hstService;
    private final OcmsOffenceNoticeOwnerDriverService offenderService;

    @Value("${ocms41.auto-approval.enabled:true}")
    private boolean autoApprovalEnabled;

    // Furnishable stages: RD1, RD2, DN1, DN2
    private static final List<String> FURNISHABLE_STAGES = List.of("RD1", "RD2", "DN1", "DN2");

    // ID types that disqualify auto-approval
    private static final String ID_TYPE_FIN = "F";
    private static final String ID_TYPE_PASSPORT = "P";

    // Suspension types
    private static final String SUSPENSION_TYPE_PS = "PS"; // Permanent Suspension
    private static final String SUSPENSION_TYPE_TS_PDP = "TS"; // Temporary Suspension - Pending Driver Particulars

    // Offender roles
    private static final String OFFENDER_ROLE_OWNER = "O";
    private static final String OFFENDER_ROLE_HIRER = "H";
    private static final String OFFENDER_ROLE_DRIVER = "D";

    // Furnish application statuses
    private static final String STATUS_SUBMITTED = "S"; // Submitted (newly synced from Internet)
    private static final String STATUS_PENDING = "P";   // Pending manual review
    private static final String STATUS_APPROVED = "A";  // Auto-approved

    /**
     * Main auto-approval method
     * Processes all newly synced furnished submissions (status = 'S')
     */
    public void processAutoApproval() {
        log.info("=== Starting OCMS 41: Auto-Approval Process at {} ===", LocalDateTime.now());

        if (!autoApprovalEnabled) {
            log.info("Auto-approval is disabled. Skipping process.");
            return;
        }

        try {
            // Get all submitted applications (status = 'S')
            List<OcmsFurnishApplication> submissions = furnishService.findByStatus(STATUS_SUBMITTED);

            if (submissions.isEmpty()) {
                log.info("No submitted applications found for auto-approval");
                return;
            }

            log.info("Found {} submitted applications for auto-approval", submissions.size());

            int approved = 0;
            int pending = 0;
            int failed = 0;

            for (OcmsFurnishApplication submission : submissions) {
                try {
                    AutoApprovalResult result = processSubmission(submission);

                    if (result.isApproved()) {
                        // Execute approval actions
                        executeApproval(submission);
                        approved++;
                        log.info("Auto-approved: txn_no={}, notice={}", submission.getTxnNo(), submission.getNoticeNo());
                    } else {
                        // Execute manual review actions
                        executeManualReview(submission, result.getReason());
                        pending++;
                        log.info("Pending manual review: txn_no={}, notice={}, reason={}",
                                submission.getTxnNo(), submission.getNoticeNo(), result.getReason());
                    }

                } catch (Exception e) {
                    failed++;
                    log.error("Failed to process submission txn_no={}: {}",
                            submission.getTxnNo(), e.getMessage(), e);
                }
            }

            log.info("=== Auto-Approval completed. Approved: {}, Pending: {}, Failed: {} ===",
                    approved, pending, failed);

        } catch (Exception e) {
            log.error("Error during auto-approval process", e);
        }
    }

    /**
     * Process a single furnished submission through auto-approval checks
     *
     * @param submission The furnished submission to process
     * @return AutoApprovalResult indicating approved or pending with reason
     */
    @Transactional
    public AutoApprovalResult processSubmission(OcmsFurnishApplication submission) {
        log.debug("Processing submission txn_no={}, notice={}", submission.getTxnNo(), submission.getNoticeNo());

        // Get the notice details
        Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(submission.getNoticeNo());
        if (noticeOpt.isEmpty()) {
            log.error("Notice not found: {}", submission.getNoticeNo());
            return AutoApprovalResult.pending("Notice not found");
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();

        // Run all validation checks
        AutoApprovalResult result;

        // CHECK 1: Active Permanent Suspension
        result = checkActivePermanentSuspension(submission, notice);
        if (!result.isApproved()) return result;

        // CHECK 2: HST ID
        result = checkHstId(submission);
        if (!result.isApproved()) return result;

        // CHECK 3: FIN/Passport ID type
        result = checkFinPassportIdType(submission);
        if (!result.isApproved()) return result;

        // CHECK 4: Furnishable stage
        result = checkFurnishableStage(submission, notice);
        if (!result.isApproved()) return result;

        // CHECK 5: Prior submission
        result = checkPriorSubmission(submission);
        if (!result.isApproved()) return result;

        // CHECK 6: Existing hirer/driver
        result = checkExistingOffender(submission, notice);
        if (!result.isApproved()) return result;

        // ALL CHECKS PASSED → Auto-approve
        log.info("All checks passed for txn_no={}, proceeding with auto-approval", submission.getTxnNo());
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 1: Check for active Permanent Suspension (PS)
     * If Notice has active PS → FAIL (manual review)
     */
    private AutoApprovalResult checkActivePermanentSuspension(OcmsFurnishApplication submission,
                                                                OcmsValidOffenceNotice notice) {
        log.debug("[CHECK 1] Checking for active Permanent Suspension for notice={}", notice.getNoticeNo());

        // Query for active PS suspension
        List<OcmsSuspendedNotice> suspensions = suspensionService.findByNoticeNoAndSuspensionType(
                notice.getNoticeNo(), SUSPENSION_TYPE_PS);

        // Check if any active PS exists
        boolean hasActivePS = suspensions.stream()
                .anyMatch(s -> s.getDateOfRevival() == null || s.getDateOfRevival().isAfter(LocalDateTime.now()));

        if (hasActivePS) {
            log.debug("[CHECK 1] FAIL: Notice has active Permanent Suspension");
            return AutoApprovalResult.pending("Notice has active Permanent Suspension");
        }

        log.debug("[CHECK 1] PASS: No active Permanent Suspension found");
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 2: Check if Furnished ID is an HST ID
     * If Furnished ID is in HST database → FAIL (manual review)
     */
    private AutoApprovalResult checkHstId(OcmsFurnishApplication submission) {
        log.debug("[CHECK 2] Checking if Furnished ID is an HST ID: {}", submission.getFurnishIdNo());

        boolean isHst = hstService.existsByIdNo(submission.getFurnishIdNo());

        if (isHst) {
            log.debug("[CHECK 2] FAIL: Furnished ID is listed in HST database");
            return AutoApprovalResult.pending("Furnished ID is High Security Threat (HST)");
        }

        log.debug("[CHECK 2] PASS: Furnished ID is not in HST database");
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 3: Check if Furnished ID type is FIN or Passport
     * If ID type is FIN (F) or Passport (P) → FAIL (manual review)
     * If ID type is NRIC (N) or UEN (U) → PASS
     */
    private AutoApprovalResult checkFinPassportIdType(OcmsFurnishApplication submission) {
        log.debug("[CHECK 3] Checking Furnished ID type: {}", submission.getFurnishIdType());

        String idType = submission.getFurnishIdType();

        if (ID_TYPE_FIN.equals(idType)) {
            log.debug("[CHECK 3] FAIL: Furnished ID type is FIN");
            return AutoApprovalResult.pending("Furnished ID type is FIN (Foreign Identification Number)");
        }

        if (ID_TYPE_PASSPORT.equals(idType)) {
            log.debug("[CHECK 3] FAIL: Furnished ID type is Passport");
            return AutoApprovalResult.pending("Furnished ID type is Passport");
        }

        log.debug("[CHECK 3] PASS: Furnished ID type is NRIC or UEN");
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 4: Check if Notice is still in Furnishable Stage
     * Furnishable stages: RD1, RD2, DN1, DN2
     * If current LPS is not furnishable → FAIL (manual review)
     */
    private AutoApprovalResult checkFurnishableStage(OcmsFurnishApplication submission,
                                                       OcmsValidOffenceNotice notice) {
        log.debug("[CHECK 4] Checking if Notice is in Furnishable Stage. Current LPS: {}",
                notice.getLastProcessingStage());

        String currentStage = notice.getLastProcessingStage();

        if (!FURNISHABLE_STAGES.contains(currentStage)) {
            log.debug("[CHECK 4] FAIL: Notice is no longer in Furnishable Stage (current: {})", currentStage);
            return AutoApprovalResult.pending("Notice is no longer in Furnishable Stage: " + currentStage);
        }

        log.debug("[CHECK 4] PASS: Notice is in Furnishable Stage");
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 5: Check if there was a prior Furnish submission
     * If prior submissions exist for same notice → FAIL (manual review)
     */
    private AutoApprovalResult checkPriorSubmission(OcmsFurnishApplication submission) {
        log.debug("[CHECK 5] Checking for prior Furnish submissions for notice={}", submission.getNoticeNo());

        // Find all submissions for this notice
        List<OcmsFurnishApplication> allSubmissions = furnishService.findByNoticeNo(submission.getNoticeNo());

        // Check if there are other submissions (excluding current one)
        long priorCount = allSubmissions.stream()
                .filter(s -> !s.getTxnNo().equals(submission.getTxnNo()))
                .count();

        if (priorCount > 0) {
            log.debug("[CHECK 5] FAIL: Found {} prior submissions for this notice", priorCount);
            return AutoApprovalResult.pending("Prior furnish submissions exist for this notice");
        }

        log.debug("[CHECK 5] PASS: No prior submissions found");
        return AutoApprovalResult.approved();
    }

    /**
     * CHECK 6: Check existing Hirer/Driver and whether Furnished ID was submitted earlier
     * This check has 3 sub-checks (9a, 9b, 9c in spec):
     * a) Check if there's an existing offender with same owner_driver_indicator (H or D)
     * b) Check if Furnished ID is listed as another offender role (O, H, or D)
     * c) Check if Submitter is still the Current Offender
     */
    private AutoApprovalResult checkExistingOffender(OcmsFurnishApplication submission,
                                                       OcmsValidOffenceNotice notice) {
        log.debug("[CHECK 6] Checking existing offenders for notice={}", notice.getNoticeNo());

        String furnishIdNo = submission.getFurnishIdNo();
        String ownerDriverIndicator = submission.getOwnerDriverIndicator(); // H or D
        String submitterIdNo = submission.getOwnerIdNo(); // The person who submitted

        // Get all existing offenders for this notice
        List<OcmsOffenceNoticeOwnerDriver> existingOffenders =
                offenderService.findByNoticeNo(notice.getNoticeNo());

        // CHECK 6a: Is there an existing Hirer/Driver with same role?
        boolean hasExistingRole = existingOffenders.stream()
                .anyMatch(o -> o.getOwnerDriverIndicator().equals(ownerDriverIndicator));

        if (hasExistingRole) {
            log.debug("[CHECK 6a] FAIL: Existing {} already exists for this notice",
                    OFFENDER_ROLE_HIRER.equals(ownerDriverIndicator) ? "Hirer" : "Driver");
            return AutoApprovalResult.pending("Existing " +
                    (OFFENDER_ROLE_HIRER.equals(ownerDriverIndicator) ? "Hirer" : "Driver") +
                    " already exists for this notice");
        }

        // CHECK 6b: Is Furnished ID listed as another offender role?
        boolean furnishIdInAnotherRole = existingOffenders.stream()
                .anyMatch(o -> o.getIdNo().equals(furnishIdNo));

        if (furnishIdInAnotherRole) {
            log.debug("[CHECK 6b] FAIL: Furnished ID is listed as another offender role for this notice");
            return AutoApprovalResult.pending("Furnished ID is already listed as another offender for this notice");
        }

        // CHECK 6c: Is Submitter still the Current Offender?
        Optional<OcmsOffenceNoticeOwnerDriver> currentOffender = existingOffenders.stream()
                .filter(o -> o.getOffenderIndicator() != null && o.getOffenderIndicator().equals("Y"))
                .findFirst();

        if (currentOffender.isPresent()) {
            String currentOffenderId = currentOffender.get().getIdNo();
            if (!currentOffenderId.equals(submitterIdNo)) {
                log.debug("[CHECK 6c] FAIL: Submitter is not the Current Offender");
                return AutoApprovalResult.pending("Submitter is not the Current Offender");
            }
        }

        log.debug("[CHECK 6] PASS: All existing offender checks passed");
        return AutoApprovalResult.approved();
    }

    // ==================== PROCESSING ACTIONS ====================

    /**
     * Execute auto-approval actions (Step 10a-10d in spec)
     * 1. Add furnished offender to database
     * 2. Change processing stage (RD1→RD2 or DN1→DN2)
     * 3. Update status to 'Approved'
     *
     * @param submission The furnished submission to approve
     */
    @Transactional
    private void executeApproval(OcmsFurnishApplication submission) {
        log.info("Executing auto-approval for txn_no={}, notice={}", submission.getTxnNo(), submission.getNoticeNo());

        try {
            // Get the notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(submission.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                throw new RuntimeException("Notice not found: " + submission.getNoticeNo());
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // ACTION 1: Add furnished offender to database
            addFurnishedOffender(submission, notice);

            // ACTION 2: Change processing stage
            changeProcessingStage(submission, notice);

            // ACTION 3: Update submission status to 'Approved'
            submission.setStatus(STATUS_APPROVED);
            submission.setUpdUserId("OCMS41_AUTO_APPROVAL");
            submission.setUpdDate(LocalDateTime.now());
            furnishService.update(submission.getTxnNo(), submission);

            log.info("Auto-approval completed successfully for txn_no={}", submission.getTxnNo());

        } catch (Exception e) {
            log.error("Failed to execute auto-approval for txn_no={}: {}", submission.getTxnNo(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Execute manual review actions (Step 11a-11c in spec)
     * 1. Create TS-PDP suspension (21 days)
     * 2. Update status to 'Pending'
     *
     * @param submission The furnished submission requiring manual review
     * @param reason Reason for requiring manual review
     */
    @Transactional
    private void executeManualReview(OcmsFurnishApplication submission, String reason) {
        log.info("Executing manual review setup for txn_no={}, notice={}, reason={}",
                submission.getTxnNo(), submission.getNoticeNo(), reason);

        try {
            // Get the notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(submission.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                throw new RuntimeException("Notice not found: " + submission.getNoticeNo());
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // ACTION 1: Create TS-PDP suspension (21 days)
            createTsPdpSuspension(submission, notice, reason);

            // ACTION 2: Update submission status to 'Pending'
            submission.setStatus(STATUS_PENDING);
            submission.setRemarks(reason); // Store reason for manual review
            submission.setUpdUserId("OCMS41_AUTO_APPROVAL");
            submission.setUpdDate(LocalDateTime.now());
            furnishService.update(submission.getTxnNo(), submission);

            log.info("Manual review setup completed for txn_no={}", submission.getTxnNo());

        } catch (Exception e) {
            log.error("Failed to execute manual review setup for txn_no={}: {}", submission.getTxnNo(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ACTION 1 (Approval): Add furnished offender to database
     * Creates new ONOD record with furnished person as Hirer or Driver
     *
     * @param submission Furnish application
     * @param notice Notice details
     */
    private void addFurnishedOffender(OcmsFurnishApplication submission, OcmsValidOffenceNotice notice) {
        log.debug("Adding furnished offender: notice={}, role={}, id={}",
                submission.getNoticeNo(), submission.getOwnerDriverIndicator(), submission.getFurnishIdNo());

        // Create new offender record
        OcmsOffenceNoticeOwnerDriver newOffender = new OcmsOffenceNoticeOwnerDriver();

        // Set composite key fields
        newOffender.setNoticeNo(submission.getNoticeNo());
        newOffender.setOwnerDriverIndicator(submission.getOwnerDriverIndicator()); // H or D

        // Set offender details
        newOffender.setIdType(submission.getFurnishIdType());
        newOffender.setIdNo(submission.getFurnishIdNo());
        newOffender.setName(submission.getFurnishName());
        newOffender.setOffenderIndicator("Y"); // New offender is now the current offender

        // Set audit fields
        newOffender.setUpdUserId("OCMS41_AUTO_APPROVAL");
        newOffender.setUpdDate(LocalDateTime.now());

        // Save to database
        offenderService.save(newOffender);

        log.info("Added furnished offender: notice={}, role={}, id={}",
                submission.getNoticeNo(), submission.getOwnerDriverIndicator(), submission.getFurnishIdNo());
    }

    /**
     * ACTION 2 (Approval): Change processing stage
     * Hirer furnish: RD1 → RD2 (LPS=RD1, NPS=RD2)
     * Driver furnish: DN1 → DN2 (LPS=DN1, NPS=DN2)
     *
     * @param submission Furnish application
     * @param notice Notice to update
     */
    private void changeProcessingStage(OcmsFurnishApplication submission, OcmsValidOffenceNotice notice) {
        String currentStage = notice.getLastProcessingStage();
        String newStage;

        // Determine new stage based on furnished role
        if (OFFENDER_ROLE_HIRER.equals(submission.getOwnerDriverIndicator())) {
            // Hirer furnish: RD1 → RD2
            newStage = "RD2";
            log.debug("Changing stage for Hirer furnish: {} → {}", currentStage, newStage);
        } else if (OFFENDER_ROLE_DRIVER.equals(submission.getOwnerDriverIndicator())) {
            // Driver furnish: DN1 → DN2
            newStage = "DN2";
            log.debug("Changing stage for Driver furnish: {} → {}", currentStage, newStage);
        } else {
            log.warn("Unknown owner_driver_indicator: {}", submission.getOwnerDriverIndicator());
            return;
        }

        // Update notice processing stage
        notice.setLastProcessingStage(currentStage); // Keep current as LPS
        notice.setNextProcessingStage(newStage);     // Set new as NPS
        notice.setUpdUserId("OCMS41_AUTO_APPROVAL");
        notice.setUpdDate(LocalDateTime.now());

        noticeService.update(notice.getNoticeNo(), notice);

        log.info("Changed processing stage for notice={}: LPS={}, NPS={}",
                submission.getNoticeNo(), currentStage, newStage);
    }

    /**
     * ACTION 1 (Manual Review): Create TS-PDP suspension
     * Temporary Suspension - Pending Driver Particulars (21 days)
     *
     * @param submission Furnish application
     * @param notice Notice to suspend
     * @param reason Reason for suspension
     */
    private void createTsPdpSuspension(OcmsFurnishApplication submission, OcmsValidOffenceNotice notice, String reason) {
        log.debug("Creating TS-PDP suspension for notice={}, reason={}", submission.getNoticeNo(), reason);

        // Get next sr_no for this notice
        Integer maxSrNo = suspensionService.findMaxSrNoByNoticeNo(submission.getNoticeNo());
        int nextSrNo = (maxSrNo == null) ? 1 : maxSrNo + 1;

        // Create suspension record
        OcmsSuspendedNotice suspension = new OcmsSuspendedNotice();

        // Set composite key
        suspension.setNoticeNo(submission.getNoticeNo());
        suspension.setSrNo(nextSrNo);

        // Set suspension details
        suspension.setSuspensionType(SUSPENSION_TYPE_TS_PDP); // TS
        suspension.setReasonOfSuspension("PDP"); // Pending Driver Particulars
        suspension.setDateOfSuspension(LocalDateTime.now());

        // Calculate due date (21 days from now)
        LocalDateTime dueDate = LocalDateTime.now().plusDays(21);
        suspension.setDueDateOfRevival(dueDate);

        // Set suspension remarks with reason
        suspension.setSuspensionRemarks("Auto-approval failed: " + reason);

        // Set audit fields
        suspension.setUpdUserId("OCMS41_AUTO_APPROVAL");
        suspension.setUpdDate(LocalDateTime.now());

        // Save to database
        suspensionService.save(suspension);

        log.info("Created TS-PDP suspension: notice={}, sr_no={}, due_date={}",
                submission.getNoticeNo(), nextSrNo, dueDate);
    }

    /**
     * Result class for auto-approval checks
     */
    public static class AutoApprovalResult {
        private final boolean approved;
        private final String reason;

        private AutoApprovalResult(boolean approved, String reason) {
            this.approved = approved;
            this.reason = reason;
        }

        public static AutoApprovalResult approved() {
            return new AutoApprovalResult(true, null);
        }

        public static AutoApprovalResult pending(String reason) {
            return new AutoApprovalResult(false, reason);
        }

        public boolean isApproved() {
            return approved;
        }

        public String getReason() {
            return reason;
        }
    }
}
