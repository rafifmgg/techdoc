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
 * Temporary Suspension (TS) Revival Helper
 * Handles all TS-specific revival validation and business logic
 *
 * TS Revival Logic:
 * 1. Find all active TS for the notice (date_of_revival IS NULL)
 * 2. Revive the specified TS (update suspended_notice with revival fields)
 * 3. Check for other active TS
 * 4. If other TS exist → Apply TS with LATEST revival date to VON/eVON (Option 3)
 * 5. If no other TS → Clear VON/eVON suspension fields
 *
 * @author Claude Code
 * @since 2025-11-25 (OCMS 17)
 */
@Component
@Slf4j
public class TemporaryRevivalHelper extends RevivalBaseHelper {

    @Override
    protected String getSuspensionType() {
        return "TS";
    }

    /**
     * Main method to revive TS for a single notice
     */
    @Transactional
    public Map<String, Object> reviveTS(String noticeNo, String revivalReason,
            String revivalRemarks, String officerAuthorisingRevival) {

        log.info("Processing TS revival for notice: {}, reason: {}", noticeNo, revivalReason);

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

            // Step 3: Check if notice is currently TS suspended
            Map<String, Object> suspensionCheck = checkSuspensionExists(noticeNo, notice, "TS");
            if (suspensionCheck != null) {
                return suspensionCheck;
            }

            // Step 4: Find the current active TS suspension
            // We need to find the TS that's currently applied to VON
            // This is the TS with matching (suspension_type, reason, date) in suspended_notice
            List<SuspendedNotice> activeTSList = findActiveSuspensions(noticeNo, "TS");

            if (activeTSList.isEmpty()) {
                return createErrorResponse(noticeNo, "OCMS-4005",
                    "No active TS suspension found in suspended_notice table");
            }

            // Find the TS that matches VON's current suspension
            SuspendedNotice currentTS = findCurrentTSFromVON(activeTSList, notice);

            if (currentTS == null) {
                // Fallback: If can't match VON, use the latest active TS by date_of_suspension
                currentTS = activeTSList.stream()
                    .max((a, b) -> a.getDateOfSuspension().compareTo(b.getDateOfSuspension()))
                    .orElse(null);

                if (currentTS == null) {
                    return createErrorResponse(noticeNo, "OCMS-4005",
                        "Could not identify current TS suspension");
                }

                log.warn("Could not match VON suspension to suspended_notice, using latest TS for notice {}",
                    noticeNo);
            }

            // Step 5: Update database (revival fields + check for other TS + update VON/eVON)
            Map<String, Object> dbResult = updateDatabaseAfterRevival(noticeNo, currentTS,
                    revivalReason, revivalRemarks, officerAuthorisingRevival);
            if (dbResult != null) {
                return dbResult;
            }

            // Success response
            return createSuccessResponse(noticeNo, "TS Revival Success");

        } catch (Exception e) {
            log.error("Error processing TS revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
        }
    }

    /**
     * TS-specific: Find remaining active TS with latest revival date (Option 3)
     */
    @Override
    protected Optional<SuspendedNotice> findRemainingActiveSuspension(String noticeNo) {
        // Find all active TS for this notice (excluding already revived ones)
        List<SuspendedNotice> activeTSList = findActiveSuspensions(noticeNo, "TS");

        if (activeTSList.isEmpty()) {
            return Optional.empty();
        }

        // Option 3: Return TS with LATEST revival date (longest duration)
        SuspendedNotice latestRevivalTS = activeTSList.stream()
            .filter(ts -> ts.getDueDateOfRevival() != null) // Only TS with revival dates
            .max((a, b) -> {
                LocalDateTime dateA = a.getDueDateOfRevival();
                LocalDateTime dateB = b.getDueDateOfRevival();
                return dateA.compareTo(dateB);
            })
            .orElse(null);

        if (latestRevivalTS != null) {
            log.info("Found remaining TS with latest revival date {} for notice {}",
                latestRevivalTS.getDueDateOfRevival(), noticeNo);
            return Optional.of(latestRevivalTS);
        }

        // Fallback: If no TS has revival date, use latest by date_of_suspension
        SuspendedNotice latestTS = activeTSList.stream()
            .max((a, b) -> a.getDateOfSuspension().compareTo(b.getDateOfSuspension()))
            .orElse(null);

        return Optional.ofNullable(latestTS);
    }

    /**
     * Helper: Find the TS that matches VON's current suspension
     * Match by: reason_of_suspension and date_of_suspension
     */
    private SuspendedNotice findCurrentTSFromVON(List<SuspendedNotice> activeTSList, OcmsValidOffenceNotice notice) {

        String vonReason = notice.getEprReasonOfSuspension();
        LocalDateTime vonDate = notice.getEprDateOfSuspension();

        if (vonReason == null || vonDate == null) {
            return null;
        }

        // Find suspended_notice that matches VON's reason and date
        return activeTSList.stream()
            .filter(ts -> vonReason.equals(ts.getReasonOfSuspension()) &&
                         vonDate.equals(ts.getDateOfSuspension()))
            .findFirst()
            .orElse(null);
    }
}
