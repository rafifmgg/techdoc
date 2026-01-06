package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;

import lombok.extern.slf4j.Slf4j;

/**
 * Permanent Suspension (PS) Revival Helper
 * Handles all PS-specific revival validation and business logic
 *
 * PS Revival Logic:
 * 1. Find all active PS for the notice (date_of_revival IS NULL)
 * 2. Revive the specified PS (update suspended_notice with revival fields)
 * 3. Check for other active PS
 * 4. If other PS exist → Apply PS with LATEST date_of_suspension to VON/eVON
 * 5. If no other PS → Clear VON/eVON suspension fields
 *
 * @author Claude Code
 * @since 2025-11-25 (OCMS 18)
 */
@Component
@Slf4j
public class PermanentRevivalHelper extends RevivalBaseHelper {

    @Override
    protected String getSuspensionType() {
        return "PS";
    }

    // PS codes that require refund when revived (OCMS 19 MVP requirement)
    private static final String PS_CODE_FP = "FP";  // Fully Paid
    private static final String PS_CODE_PRA = "PRA"; // Partial Reduction Applied

    /**
     * Main method to revive PS for a single notice
     */
    @Transactional
    public Map<String, Object> revivePS(String noticeNo, String revivalReason,
            String revivalRemarks, String officerAuthorisingRevival) {

        log.info("Processing PS revival for notice: {}, reason: {}", noticeNo, revivalReason);

        try {
            // Step 1: Check mandatory fields (from base)
            Map<String, Object> validationResult = validateMandatoryFields(noticeNo, revivalReason,
                    officerAuthorisingRevival);
            if (validationResult != null) {
                return validationResult;
            }

            // Step 2: Query notice from database (from base)
            Optional<OcmsValidOffenceNotice> noticeOpt = queryNotice(noticeNo);
            if (!noticeOpt.isPresent()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Invalid Notice Number");
            }
            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Step 3: Check if notice is currently PS suspended
            Map<String, Object> suspensionCheck = checkSuspensionExists(noticeNo, notice, "PS");
            if (suspensionCheck != null) {
                return suspensionCheck;
            }

            // Step 4: Find the current active PS suspension
            // We need to find the PS that's currently applied to VON
            // This is the PS with matching (suspension_type, reason, date) in suspended_notice
            List<SuspendedNotice> activePSList = findActiveSuspensions(noticeNo, "PS");

            if (activePSList.isEmpty()) {
                return createErrorResponse(noticeNo, "OCMS-4005",
                    "No active PS suspension found in suspended_notice table");
            }

            // Find the PS that matches VON's current suspension
            SuspendedNotice currentPS = findCurrentPSFromVON(activePSList, notice);

            if (currentPS == null) {
                // Fallback: If can't match VON, use the latest active PS by date_of_suspension
                currentPS = activePSList.stream()
                    .max((a, b) -> a.getDateOfSuspension().compareTo(b.getDateOfSuspension()))
                    .orElse(null);

                if (currentPS == null) {
                    return createErrorResponse(noticeNo, "OCMS-4005",
                        "Could not identify current PS suspension");
                }

                log.warn("Could not match VON suspension to suspended_notice, using latest PS for notice {}",
                    noticeNo);
            }

            // Step 5: Check if refund is required (OCMS 19 MVP - PS-FP only)
            // For MVP: Log refund identification. Full integration in OCMS 43.
            checkAndLogRefundIdentification(notice, currentPS);

            // Step 6: Update database (revival fields + check for other PS + update VON/eVON)
            Map<String, Object> dbResult = updateDatabaseAfterRevival(noticeNo, currentPS,
                    revivalReason, revivalRemarks, officerAuthorisingRevival);
            if (dbResult != null) {
                return dbResult;
            }

            // Success response
            return createSuccessResponse(noticeNo, "PS Revival Success");

        } catch (Exception e) {
            log.error("Error processing PS revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
        }
    }

    /**
     * OCMS 19 MVP: Check if refund is required and log identification
     *
     * Spec Requirement (v2.0_OCMS_19_Revive_Suspensions_Feedback.md:247):
     * "For OCMS MVP, OCMS will only store the date that the refund has been identified."
     *
     * PS-FP (Fully Paid): Requires refund when revived
     * PS-PRA (Partial Reduction Applied): Post-MVP (pending URA QUESTION-6)
     *
     * Full refund processing will be implemented in OCMS 43 using ocms_refund_notice table.
     * For MVP, we log the refund identification with timestamp for audit trail.
     *
     * @param notice The notice being revived
     * @param currentPS The PS suspension being revived
     */
    private void checkAndLogRefundIdentification(OcmsValidOffenceNotice notice, SuspendedNotice currentPS) {
        String psCode = currentPS.getReasonOfSuspension();
        String noticeNo = notice.getNoticeNo();

        // Check if this PS code requires refund
        if (PS_CODE_FP.equals(psCode)) {
            // PS-FP: Fully Paid - refund required
            log.info("[REFUND IDENTIFIED] Notice {}: PS-FP revival detected. " +
                    "Refund required for fully paid notice. " +
                    "Amount paid: {}, Amount payable: {}, Identified date: {}. " +
                    "Action: Will be processed by OCMS 43 refund module.",
                    noticeNo,
                    notice.getAmountPaid(),
                    notice.getAmountPayable(),
                    LocalDateTime.now());

        } else if (PS_CODE_PRA.equals(psCode)) {
            // PS-PRA: Partial Reduction Applied - post-MVP (QUESTION-6)
            log.info("[REFUND PENDING] Notice {}: PS-PRA revival detected. " +
                    "Refund handling pending URA clarification (QUESTION-6). " +
                    "Amount paid: {}, Amount payable: {}, Identified date: {}. " +
                    "Status: Post-MVP feature.",
                    noticeNo,
                    notice.getAmountPaid(),
                    notice.getAmountPayable(),
                    LocalDateTime.now());
        }

        // Other PS codes do not require refund
    }

    /**
     * PS-specific: Find remaining active PS with latest date_of_suspension
     */
    @Override
    protected Optional<SuspendedNotice> findRemainingActiveSuspension(String noticeNo) {
        // Find all active PS for this notice (excluding already revived ones)
        List<SuspendedNotice> activePSList = findActiveSuspensions(noticeNo, "PS");

        if (activePSList.isEmpty()) {
            return Optional.empty();
        }

        // Return PS with LATEST date_of_suspension (most recent PS)
        SuspendedNotice latestPS = activePSList.stream()
            .max((a, b) -> a.getDateOfSuspension().compareTo(b.getDateOfSuspension()))
            .orElse(null);

        if (latestPS != null) {
            log.info("Found remaining PS with latest date_of_suspension {} for notice {}",
                latestPS.getDateOfSuspension(), noticeNo);
        }

        return Optional.ofNullable(latestPS);
    }

    /**
     * Helper: Find the PS that matches VON's current suspension
     * Match by: reason_of_suspension and date_of_suspension
     */
    private SuspendedNotice findCurrentPSFromVON(List<SuspendedNotice> activePSList, OcmsValidOffenceNotice notice) {

        String vonReason = notice.getEprReasonOfSuspension();
        LocalDateTime vonDate = notice.getEprDateOfSuspension();

        if (vonReason == null || vonDate == null) {
            return null;
        }

        // Find suspended_notice that matches VON's reason and date
        return activePSList.stream()
            .filter(ps -> vonReason.equals(ps.getReasonOfSuspension()) &&
                         vonDate.equals(ps.getDateOfSuspension()))
            .findFirst()
            .orElse(null);
    }
}
