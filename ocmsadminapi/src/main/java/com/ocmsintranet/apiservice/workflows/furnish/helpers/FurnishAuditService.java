package com.ocmsintranet.apiservice.workflows.furnish.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Audit service for furnish submission workflow.
 * Handles logging and audit trail creation.
 *
 * Based on OCMS 41 requirements and reduction workflow pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishAuditService {

    /**
     * Log submission received
     */
    public void logSubmissionReceived(FurnishSubmissionRequest request) {
        log.info("Furnish submission received - TxnNo: {}, NoticeNo: {}, VehicleNo: {}, OwnerDriverIndicator: {}",
                request.getTxnNo(),
                request.getNoticeNo(),
                request.getVehicleNo(),
                request.getOwnerDriverIndicator());
    }

    /**
     * Log validation started
     */
    public void logValidationStarted(String noticeNo) {
        log.debug("Starting validation for notice: {}", noticeNo);
    }

    /**
     * Log validation completed
     */
    public void logValidationCompleted(String noticeNo, boolean passed) {
        if (passed) {
            log.info("Validation completed successfully for notice: {}", noticeNo);
        } else {
            log.warn("Validation failed for notice: {}", noticeNo);
        }
    }

    /**
     * Log auto-approval check started
     */
    public void logAutoApprovalCheckStarted(String noticeNo) {
        log.debug("Starting auto-approval checks for notice: {}", noticeNo);
    }

    /**
     * Log auto-approval check completed
     */
    public void logAutoApprovalCheckCompleted(FurnishContext context) {
        String noticeNo = context.getRequest().getNoticeNo();

        if (context.isAutoApprovalPassed()) {
            log.info("Auto-approval checks PASSED for notice: {}", noticeNo);
        } else {
            log.warn("Auto-approval checks FAILED for notice: {}. Reasons: {}",
                    noticeNo, context.getFailureReasonsSummary());
        }
    }

    /**
     * Log furnish application created
     */
    public void logFurnishApplicationCreated(String txnNo, String status, boolean isResubmission) {
        log.info("Furnish application created - TxnNo: {}, Status: {}, Resubmission: {}",
                txnNo, status, isResubmission);
    }

    /**
     * Log hirer/driver record created
     */
    public void logHirerDriverRecordCreated(String noticeNo, String furnishIdNo, String ownerDriverIndicator) {
        log.info("Hirer/Driver record created - NoticeNo: {}, FurnishID: {}, Type: {}",
                noticeNo, furnishIdNo, ownerDriverIndicator);
    }

    /**
     * Log suspension applied
     */
    public void logSuspensionApplied(String noticeNo, String suspensionType, int days) {
        log.info("Suspension applied - NoticeNo: {}, Type: {}, Duration: {} days",
                noticeNo, suspensionType, days);
    }

    /**
     * Log manual review required
     */
    public void logManualReviewRequired(String noticeNo, String reason) {
        log.info("Manual review required - NoticeNo: {}, Reason: {}", noticeNo, reason);
    }

    /**
     * Log submission completed successfully
     */
    public void logSubmissionCompleted(FurnishContext context) {
        FurnishSubmissionRequest request = context.getRequest();
        log.info("Furnish submission completed - TxnNo: {}, NoticeNo: {}, AutoApproved: {}, " +
                        "HirerDriverCreated: {}, SuspensionApplied: {}",
                request.getTxnNo(),
                request.getNoticeNo(),
                context.isAutoApprovalPassed(),
                context.isOwnerDriverRecordCreated(),
                context.isSuspensionApplied());
    }

    /**
     * Log submission failed
     */
    public void logSubmissionFailed(String txnNo, String noticeNo, String reason) {
        log.error("Furnish submission failed - TxnNo: {}, NoticeNo: {}, Reason: {}",
                txnNo, noticeNo, reason);
    }

    /**
     * Log technical error
     */
    public void logTechnicalError(String operation, String txnNo, Exception e) {
        log.error("Technical error during {} - TxnNo: {}, Error: {}",
                operation, txnNo, e.getMessage(), e);
    }
}
