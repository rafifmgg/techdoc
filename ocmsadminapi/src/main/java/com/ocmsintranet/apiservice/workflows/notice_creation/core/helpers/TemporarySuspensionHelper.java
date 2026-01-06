package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason.SuspensionReason;

import lombok.extern.slf4j.Slf4j;

/**
 * Temporary Suspension (TS) Processing Helper
 * Handles all TS-specific validation and business logic
 *
 * @author Claude Code (Refactored from SuspensionProcessingHelper)
 * @since 2025-01-19 (OCMS 18)
 */
@Component
@Slf4j
public class TemporarySuspensionHelper extends SuspensionBaseHelper {

    // Suspension codes that allow PLUS source (TS-specific)
    private static final Set<String> PLUS_ALLOWED_TS_CODES = new HashSet<>(Arrays.asList(
        "APE", "APP", "CCE", "PRI", "RED"
    ));

    // Suspension codes that allow CRON/OCMS source (TS-specific)
    private static final Set<String> CRON_OCMS_COMMON_TS_CODES = new HashSet<>(Arrays.asList(
        "NRO", "PDP", "SYS"
    ));

    // ROV code that only allows ROV stage (TS-specific)
    private static final Set<String> CRON_OCMS_ROV_TS_CODE = new HashSet<>(Arrays.asList(
        "ROV"
    ));

    // PS suspension reasons that allow TS on top
    private static final Set<String> PS_REASONS_ALLOW_TS = new HashSet<>(Arrays.asList(
        "FOR", "MID", "DIP", "RP2", "RIP"
    ));

    @Override
    protected String getSuspensionType() {
        return "TS";
    }

    /**
     * Main method to process TS for a single notice
     */
    @Transactional
    public Map<String, Object> processTS(String noticeNo, String suspensionSource,
            String reasonOfSuspension, Integer daysToRevive, String suspensionRemarks,
            String officerAuthorisingSuspension, String srNo, String caseNo) {

        log.info("Processing TS for notice: {}, reason: {}", noticeNo, reasonOfSuspension);

        try {
            // Step 1: Check mandatory fields (from base)
            Map<String, Object> validationResult = validateMandatoryFields(noticeNo, "TS",
                    reasonOfSuspension, officerAuthorisingSuspension);
            if (validationResult != null) {
                return validationResult;
            }

            // Step 2: Query notice from database (from base)
            Optional<OcmsValidOffenceNotice> noticeOpt = queryNotice(noticeNo);
            if (!noticeOpt.isPresent()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Invalid Notice Number");
            }
            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Step 2.1: Check if notice is already TS
            if ("TS".equals(notice.getSuspensionType())) {
                return createSuccessResponse(noticeNo, srNo, "Success Notice already TS", "OCMS-2001");
            }

            // Step 3: Check source permission (TS-specific)
            Map<String, Object> permissionResult = checkSourcePermissionTS(noticeNo, suspensionSource,
                    reasonOfSuspension);
            if (permissionResult != null) {
                return permissionResult;
            }

            // Step 4: Check if stage is allowed for suspension (from base)
            Map<String, Object> stageResult = checkStageAllowed(noticeNo, reasonOfSuspension,
                    notice.getLastProcessingStage());
            if (stageResult != null) {
                return stageResult;
            }

            // Step 5: Check if notice is paid (TS does not allow paid notices)
            if (notice.getCrsReasonOfSuspension() != null) {
                return createErrorResponse(noticeNo, "OCMS-4003", "Notice is fully or partially paid");
            }

            // Step 6: Validate overlap (TS-specific) - Check if PS allows TS on top
            Map<String, Object> overlapResult = validateOverlap(notice, reasonOfSuspension);
            if (overlapResult != null) {
                return overlapResult;
            }

            // Step 7: Calculate due date of revival (TS-specific)
            LocalDateTime dueDateOfRevival = calculateRevivalDate(reasonOfSuspension, daysToRevive);
            if (dueDateOfRevival == null) {
                return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
            }

            // Step 7.1: Option 3 - Latest Revival Date Precedence (OCMS17_TT017)
            // If multiple TS exist, the latest revival date (longest duration) governs
            LocalDateTime existingRevivalDate = notice.getDueDateOfRevival();
            if (existingRevivalDate != null && dueDateOfRevival.isBefore(existingRevivalDate)) {
                // Don't update VON - existing TS has later revival date
                log.info("TS applied for notice {} but VON not updated: existing revival date {} is later than new {}",
                         noticeNo, existingRevivalDate, dueDateOfRevival);

                // Still create suspended_notice record for audit trail
                createSuspendedNoticeOnly(notice, "TS", reasonOfSuspension, dueDateOfRevival,
                        suspensionSource, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo);

                return createSuccessResponse(noticeNo, srNo,
                    "TS applied (existing TS with later revival date retained)", "OCMS-2001");
            }

            // Step 8: Update database (from base)
            Map<String, Object> dbResult = updateDatabase(notice, "TS", reasonOfSuspension,
                    dueDateOfRevival, suspensionSource, suspensionRemarks,
                    officerAuthorisingSuspension, srNo, caseNo);
            if (dbResult != null) {
                return dbResult;
            }

            // Success response
            return createSuccessResponse(noticeNo, srNo, "TS Success");

        } catch (Exception e) {
            log.error("Error processing TS for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
        }
    }

    /**
     * TS-specific: Check source permission based on suspension code and source
     */
    private Map<String, Object> checkSourcePermissionTS(String noticeNo, String suspensionSource,
            String reasonOfSuspension) {

        if (suspensionSource == null || suspensionSource.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4000", "Suspension Source is missing");
        }

        boolean sourceAllowed = false;

        // Check if source is allowed for this TS code
        if (SOURCE_PLUS.equals(suspensionSource)) {
            sourceAllowed = PLUS_ALLOWED_TS_CODES.contains(reasonOfSuspension);
        } else if (SOURCE_CRON.equals(suspensionSource) || SOURCE_OCMS_STAFF.equals(suspensionSource)) {
            sourceAllowed = CRON_OCMS_COMMON_TS_CODES.contains(reasonOfSuspension) ||
                           CRON_OCMS_ROV_TS_CODE.contains(reasonOfSuspension);
        }

        if (!sourceAllowed) {
            return createErrorResponse(noticeNo, "OCMS-4000", "Source not authorized to use this Suspension Code");
        }

        return null; // Allowed
    }

    @Override
    protected Map<String, Object> validateOverlap(OcmsValidOffenceNotice notice, String reasonOfSuspension) {
        // TS overlap validation: Simple check if existing PS allows TS on top
        if ("PS".equals(notice.getSuspensionType())) {
            String eprReason = notice.getEprReasonOfSuspension();
            if (eprReason == null || !PS_REASONS_ALLOW_TS.contains(eprReason)) {
                return createErrorResponse(notice.getNoticeNo(), "OCMS-4004",
                    "Cannot apply TS - Notice has been permanently suspended");
            }
        }
        return null; // Overlap validation passed
    }

    @Override
    protected LocalDateTime calculateRevivalDate(String reasonOfSuspension, Integer daysToRevive) {
        // TS-specific: Calculate revival date from reference table or provided days

        LocalDateTime currentDate = LocalDateTime.now();

        // If daysToRevive is provided, use it
        if (daysToRevive != null && daysToRevive > 0) {
            return currentDate.plusDays(daysToRevive);
        }

        // Otherwise, lookup from ocms_suspension_reason table
        try {
            // Query suspension reasons using BaseService getAll method
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("reasonOfSuspension", new String[]{reasonOfSuspension});
            List<SuspensionReason> suspensionReasons = suspensionReasonService.getAll(queryParams).getData();

            for (SuspensionReason reason : suspensionReasons) {
                if ("TS".equals(reason.getSuspensionType()) && reason.getNoOfDaysForRevival() != null) {
                    return currentDate.plusDays(reason.getNoOfDaysForRevival());
                }
            }

            // Default to 30 days if not found
            log.warn("No revival days found for TS reason: {}, using default 30 days", reasonOfSuspension);
            return currentDate.plusDays(30);

        } catch (Exception e) {
            log.error("Error looking up TS suspension reason: {}", e.getMessage());
            return null;
        }
    }
}
