package com.ocmsintranet.apiservice.workflows.furnish.manualreview.services;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OCMS 41: Manual Review Service
 *
 * Handles OIC manual review and approval/rejection of furnished submissions
 * that did not qualify for auto-approval.
 *
 * This service provides the backend APIs for:
 * 1. Furnishability check (ensure notice is still furnishable)
 * 2. Approve Hirer (add hirer, change stage, update status)
 * 3. Approve Driver (add driver, change stage, update status)
 * 4. Reject submission (update status to Rejected)
 * 5. Send Email notification
 * 6. Send SMS notification
 *
 * Based on Functional Spec v1.1 Section 3.4.4.5.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ocms41ManualReviewService {

    private final OcmsFurnishApplicationService furnishService;
    private final OcmsValidOffenceNoticeService noticeService;
    private final SuspendedNoticeService suspensionService;
    private final OcmsOffenceNoticeOwnerDriverService offenderService;
    private final FurnishNotificationService notificationService;

    // Furnishable stages: RD1, RD2, DN1, DN2
    private static final List<String> FURNISHABLE_STAGES = List.of("RD1", "RD2", "DN1", "DN2");

    // Suspension types
    private static final String SUSPENSION_TYPE_PS = "PS"; // Permanent Suspension

    // Offender roles
    private static final String OFFENDER_ROLE_HIRER = "H";
    private static final String OFFENDER_ROLE_DRIVER = "D";

    // Furnished submission statuses
    private static final String STATUS_PENDING = "P";   // Pending manual review
    private static final String STATUS_APPROVED = "A";  // Approved
    private static final String STATUS_REJECTED = "R";  // Rejected

    /**
     * API 13: Furnishability Check
     *
     * Checks whether a notice is still furnishable before approval.
     * This check is performed before allowing OIC to approve a submission.
     *
     * The check validates:
     * 1. Notice does not have active Permanent Suspension
     * 2. Notice is in a furnishable stage (RD1, RD2, DN1, DN2)
     *
     * @param noticeNo Notice number to check
     * @return FurnishabilityResult indicating whether notice is furnishable
     */
    public FurnishabilityResult checkFurnishability(String noticeNo) {
        log.info("Checking furnishability for notice={}", noticeNo);

        // Get notice details
        Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(noticeNo);
        if (noticeOpt.isEmpty()) {
            log.error("Notice not found: {}", noticeNo);
            return FurnishabilityResult.notFurnishable("Notice not found");
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();

        // CHECK 1: Active Permanent Suspension
        List<SuspendedNotice> suspensions = suspensionService.findByNoticeNoAndSuspensionType(
                noticeNo, SUSPENSION_TYPE_PS);

        boolean hasActivePS = suspensions.stream()
                .anyMatch(s -> s.getDateOfRevival() == null || s.getDateOfRevival().isAfter(LocalDateTime.now()));

        if (hasActivePS) {
            log.warn("Notice {} has active Permanent Suspension", noticeNo);
            return FurnishabilityResult.notFurnishable("Notice has active Permanent Suspension");
        }

        // CHECK 2: Furnishable Stage
        String currentStage = notice.getLastProcessingStage();
        if (!FURNISHABLE_STAGES.contains(currentStage)) {
            log.warn("Notice {} is not in furnishable stage. Current stage: {}", noticeNo, currentStage);
            return FurnishabilityResult.notFurnishable(
                    "Notice is not in furnishable stage. Current stage: " + currentStage);
        }

        log.info("Notice {} is furnishable", noticeNo);
        return FurnishabilityResult.furnishable();
    }

    /**
     * API 14: Approve Hirer
     *
     * Approves a furnished submission for a Hirer:
     * 1. Update submission status to 'Approved'
     * 2. Add furnished particulars as Hirer (owner_driver_indicator = 'H')
     * 3. Change processing stage (LPS=RD1, NPS=RD2)
     *
     * @param txnNo Transaction number of the furnished submission
     * @param reviewedBy User ID of OIC who reviewed
     * @return ApprovalResult indicating success or failure
     */
    @Transactional
    public ApprovalResult approveHirer(String txnNo, String reviewedBy) {
        log.info("Approving Hirer for txn_no={}, reviewed_by={}", txnNo, reviewedBy);

        try {
            // Get furnished submission
            Optional<OcmsFurnishApplication> submissionOpt = furnishService.getById(txnNo);
            if (submissionOpt.isEmpty()) {
                return ApprovalResult.failure("Furnished submission not found: " + txnNo);
            }

            OcmsFurnishApplication submission = submissionOpt.get();

            // Verify this is a Hirer submission
            if (!OFFENDER_ROLE_HIRER.equals(submission.getOwnerDriverIndicator())) {
                return ApprovalResult.failure("Submission is not for Hirer role. Role: " +
                        submission.getOwnerDriverIndicator());
            }

            // Check furnishability
            FurnishabilityResult furnishCheck = checkFurnishability(submission.getNoticeNo());
            if (!furnishCheck.isFurnishable()) {
                return ApprovalResult.failure(furnishCheck.getReason());
            }

            // Get notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(submission.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                return ApprovalResult.failure("Notice not found: " + submission.getNoticeNo());
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // STEP 1: Update submission status to Approved
            submission.setStatus(STATUS_APPROVED);
            submission.setRemarks("Approved by " + reviewedBy + " on " + LocalDateTime.now());
            furnishService.update(submission.getTxnNo(), submission);

            // STEP 2: Add furnished particulars as Hirer
            addFurnishedOffender(submission, notice, OFFENDER_ROLE_HIRER, reviewedBy);

            // STEP 3: Change processing stage (RD1 -> RD2)
            changeProcessingStage(notice, OFFENDER_ROLE_HIRER, reviewedBy);

            log.info("Successfully approved Hirer for txn_no={}", txnNo);
            return ApprovalResult.success();

        } catch (Exception e) {
            log.error("Failed to approve Hirer for txn_no={}: {}", txnNo, e.getMessage(), e);
            return ApprovalResult.failure("Error approving Hirer: " + e.getMessage());
        }
    }

    /**
     * API 15: Approve Driver
     *
     * Approves a furnished submission for a Driver:
     * 1. Update submission status to 'Approved'
     * 2. Add furnished particulars as Driver (owner_driver_indicator = 'D')
     * 3. Change processing stage (LPS=DN1, NPS=DN2)
     *
     * @param txnNo Transaction number of the furnished submission
     * @param reviewedBy User ID of OIC who reviewed
     * @return ApprovalResult indicating success or failure
     */
    @Transactional
    public ApprovalResult approveDriver(String txnNo, String reviewedBy) {
        log.info("Approving Driver for txn_no={}, reviewed_by={}", txnNo, reviewedBy);

        try {
            // Get furnished submission
            Optional<OcmsFurnishApplication> submissionOpt = furnishService.getById(txnNo);
            if (submissionOpt.isEmpty()) {
                return ApprovalResult.failure("Furnished submission not found: " + txnNo);
            }

            OcmsFurnishApplication submission = submissionOpt.get();

            // Verify this is a Driver submission
            if (!OFFENDER_ROLE_DRIVER.equals(submission.getOwnerDriverIndicator())) {
                return ApprovalResult.failure("Submission is not for Driver role. Role: " +
                        submission.getOwnerDriverIndicator());
            }

            // Check furnishability
            FurnishabilityResult furnishCheck = checkFurnishability(submission.getNoticeNo());
            if (!furnishCheck.isFurnishable()) {
                return ApprovalResult.failure(furnishCheck.getReason());
            }

            // Get notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(submission.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                return ApprovalResult.failure("Notice not found: " + submission.getNoticeNo());
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // STEP 1: Update submission status to Approved
            submission.setStatus(STATUS_APPROVED);
            submission.setRemarks("Approved by " + reviewedBy + " on " + LocalDateTime.now());
            furnishService.update(submission.getTxnNo(), submission);

            // STEP 2: Add furnished particulars as Driver
            addFurnishedOffender(submission, notice, OFFENDER_ROLE_DRIVER, reviewedBy);

            // STEP 3: Change processing stage (DN1 -> DN2)
            changeProcessingStage(notice, OFFENDER_ROLE_DRIVER, reviewedBy);

            log.info("Successfully approved Driver for txn_no={}", txnNo);
            return ApprovalResult.success();

        } catch (Exception e) {
            log.error("Failed to approve Driver for txn_no={}: {}", txnNo, e.getMessage(), e);
            return ApprovalResult.failure("Error approving Driver: " + e.getMessage());
        }
    }

    /**
     * API 18: Reject Submission
     *
     * Rejects a furnished submission:
     * 1. Update submission status to 'Rejected'
     * 2. Store rejection reason
     *
     * @param txnNo Transaction number of the furnished submission
     * @param reviewedBy User ID of OIC who reviewed
     * @param rejectionReason Reason for rejection
     * @return ApprovalResult indicating success or failure
     */
    @Transactional
    public ApprovalResult rejectSubmission(String txnNo, String reviewedBy, String rejectionReason) {
        log.info("Rejecting submission for txn_no={}, reviewed_by={}, reason={}", txnNo, reviewedBy, rejectionReason);

        try {
            // Get furnished submission
            Optional<OcmsFurnishApplication> submissionOpt = furnishService.getById(txnNo);
            if (submissionOpt.isEmpty()) {
                return ApprovalResult.failure("Furnished submission not found: " + txnNo);
            }

            OcmsFurnishApplication submission = submissionOpt.get();

            // Update submission status to Rejected
            submission.setStatus(STATUS_REJECTED);
            submission.setRemarks("Rejected by " + reviewedBy + " on " + LocalDateTime.now() + ". Reason: " + rejectionReason);
            furnishService.update(submission.getTxnNo(), submission);

            log.info("Successfully rejected submission for txn_no={}", txnNo);
            return ApprovalResult.success();

        } catch (Exception e) {
            log.error("Failed to reject submission for txn_no={}: {}", txnNo, e.getMessage(), e);
            return ApprovalResult.failure("Error rejecting submission: " + e.getMessage());
        }
    }

    /**
     * API 16: Send Email
     *
     * Sends email notification to submitter or furnished person.
     *
     * @param txnNo Transaction number
     * @param noticeNo Notice number
     * @param recipient Email address
     * @param subject Email subject
     * @param htmlBody Email HTML body
     * @return NotificationResult indicating success or failure
     */
    public NotificationResult sendEmail(String txnNo, String noticeNo, String recipient, String subject, String htmlBody) {
        log.info("Sending email for txn_no={}, notice={}, recipient={}", txnNo, noticeNo, recipient);

        try {
            // Get current processing stage
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(noticeNo);
            String processingStage = noticeOpt.map(OcmsValidOffenceNotice::getLastProcessingStage).orElse("MANUAL_REVIEW");

            // Send and record email
            boolean sent = notificationService.sendAndRecordEmail(
                    noticeNo,
                    processingStage,
                    recipient,
                    subject,
                    htmlBody
            );

            if (sent) {
                log.info("Email sent successfully for txn_no={}", txnNo);
                return NotificationResult.success("Email sent successfully");
            } else {
                log.warn("Email sending failed for txn_no={}", txnNo);
                return NotificationResult.failure("Email sending failed");
            }

        } catch (Exception e) {
            log.error("Failed to send email for txn_no={}: {}", txnNo, e.getMessage(), e);
            return NotificationResult.failure("Error sending email: " + e.getMessage());
        }
    }

    /**
     * API 17: Send SMS
     *
     * Sends SMS notification to submitter or furnished person.
     *
     * @param txnNo Transaction number
     * @param noticeNo Notice number
     * @param mobileCode Mobile country code (e.g., "65")
     * @param mobileNo Mobile number
     * @param message SMS message
     * @return NotificationResult indicating success or failure
     */
    public NotificationResult sendSms(String txnNo, String noticeNo, String mobileCode, String mobileNo, String message) {
        log.info("Sending SMS for txn_no={}, notice={}, mobile=+{}{}", txnNo, noticeNo, mobileCode, mobileNo);

        try {
            // Get current processing stage
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(noticeNo);
            String processingStage = noticeOpt.map(OcmsValidOffenceNotice::getLastProcessingStage).orElse("MANUAL_REVIEW");

            // Send and record SMS
            boolean sent = notificationService.sendAndRecordSms(
                    noticeNo,
                    processingStage,
                    mobileCode != null ? mobileCode : "65",
                    mobileNo,
                    message
            );

            if (sent) {
                log.info("SMS sent successfully for txn_no={}", txnNo);
                return NotificationResult.success("SMS sent successfully");
            } else {
                log.warn("SMS sending failed for txn_no={}", txnNo);
                return NotificationResult.failure("SMS sending failed");
            }

        } catch (Exception e) {
            log.error("Failed to send SMS for txn_no={}: {}", txnNo, e.getMessage(), e);
            return NotificationResult.failure("Error sending SMS: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Add furnished offender to database
     * Creates new ONOD record with furnished person as Hirer or Driver
     */
    private void addFurnishedOffender(OcmsFurnishApplication submission,
                                      OcmsValidOffenceNotice notice,
                                      String role,
                                      String modifiedBy) {
        log.debug("Adding furnished offender: notice={}, role={}, id={}",
                submission.getNoticeNo(), role, submission.getFurnishIdNo());

        // Create new offender record
        OcmsOffenceNoticeOwnerDriver newOffender = new OcmsOffenceNoticeOwnerDriver();

        // Set composite key fields
        newOffender.setNoticeNo(submission.getNoticeNo());
        newOffender.setOwnerDriverIndicator(role); // H or D

        // Set offender details
        newOffender.setIdType(submission.getFurnishIdType());
        newOffender.setIdNo(submission.getFurnishIdNo());
        newOffender.setName(submission.getFurnishName());
        newOffender.setOffenderIndicator("Y"); // New offender is now the current offender

        // Set contact details
        newOffender.setEmailAddr(submission.getFurnishEmailAddr());
        newOffender.setOffenderTelCode(submission.getFurnishTelCode());
        newOffender.setOffenderTelNo(submission.getFurnishTelNo());

        // Save to database (create new record)
        offenderService.save(newOffender);

        log.info("Added furnished offender: notice={}, role={}, id={}",
                submission.getNoticeNo(), role, submission.getFurnishIdNo());
    }

    /**
     * Change processing stage based on role
     * Hirer: RD1 → RD2 (LPS=RD1, NPS=RD2)
     * Driver: DN1 → DN2 (LPS=DN1, NPS=DN2)
     */
    private void changeProcessingStage(OcmsValidOffenceNotice notice, String role, String modifiedBy) {
        String currentStage = notice.getLastProcessingStage();
        String newStage;

        // Determine new stage based on role
        if (OFFENDER_ROLE_HIRER.equals(role)) {
            // Hirer furnish: RD1 → RD2
            newStage = "RD2";
            log.debug("Changing stage for Hirer furnish: {} → {}", currentStage, newStage);
        } else if (OFFENDER_ROLE_DRIVER.equals(role)) {
            // Driver furnish: DN1 → DN2
            newStage = "DN2";
            log.debug("Changing stage for Driver furnish: {} → {}", currentStage, newStage);
        } else {
            log.warn("Unknown role: {}", role);
            return;
        }

        // Update notice processing stage
        notice.setLastProcessingStage(currentStage); // Keep current as LPS
        notice.setNextProcessingStage(newStage);     // Set new as NPS

        noticeService.update(notice.getNoticeNo(), notice);

        log.info("Changed processing stage for notice={}: LPS={}, NPS={}",
                notice.getNoticeNo(), currentStage, newStage);
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Result class for furnishability check
     */
    public static class FurnishabilityResult {
        private final boolean furnishable;
        private final String reason;

        private FurnishabilityResult(boolean furnishable, String reason) {
            this.furnishable = furnishable;
            this.reason = reason;
        }

        public static FurnishabilityResult furnishable() {
            return new FurnishabilityResult(true, null);
        }

        public static FurnishabilityResult notFurnishable(String reason) {
            return new FurnishabilityResult(false, reason);
        }

        public boolean isFurnishable() {
            return furnishable;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Result class for approval/rejection operations
     */
    public static class ApprovalResult {
        private final boolean success;
        private final String message;

        private ApprovalResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ApprovalResult success() {
            return new ApprovalResult(true, "Success");
        }

        public static ApprovalResult failure(String message) {
            return new ApprovalResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Result class for email/SMS notification operations
     */
    public static class NotificationResult {
        private final boolean success;
        private final String message;

        private NotificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static NotificationResult success(String message) {
            return new NotificationResult(true, message);
        }

        public static NotificationResult failure(String message) {
            return new NotificationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
