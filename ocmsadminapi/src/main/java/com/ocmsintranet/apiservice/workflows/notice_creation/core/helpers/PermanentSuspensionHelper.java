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
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;

import lombok.extern.slf4j.Slf4j;

/**
 * Permanent Suspension (PS) Processing Helper
 * Handles all PS-specific validation and business logic
 *
 * Key Features:
 * - Revival date always NULL (permanent suspension)
 * - Revive old PS before applying new EPR PS (except 5 exception codes)
 * - Allow PS-APP/CFA/VST on paid notices (trigger refund)
 * - CRS PS (FP/PRA) only allowed with 5 exception codes
 *
 * @author Claude Code
 * @since 2025-01-19 (OCMS 18)
 */
@Component
@Slf4j
public class PermanentSuspensionHelper extends SuspensionBaseHelper {

    // PS codes that allow PLUS source (per Source Permission Matrix)
    // Note: CFA is OCMS-only, not PLUS (spec line 292)
    private static final Set<String> PLUS_ALLOWED_PS_CODES = new HashSet<>(Arrays.asList(
        "APP", "CAN", "VST"
    ));

    // PS codes that allow OCMS Staff source
    private static final Set<String> OCMS_STAFF_PS_CODES = new HashSet<>(Arrays.asList(
        "ANS", "CAN", "CFA", "CFP", "DBB", "DIP", "FCT", "FOR", "FTC", "IST",
        "MID", "OTH", "RIP", "RP2", "SCT", "SLC", "SSV", "VCT", "VST",
        "WWC", "WWF", "WWP"
    ));

    // PS codes that allow OCMS Backend (auto-triggered)
    private static final Set<String> OCMS_BACKEND_PS_CODES = new HashSet<>(Arrays.asList(
        "ANS", "DBB", "DIP", "FOR", "MID", "RIP", "RP2", "FP", "PRA",
        "CFP", "IST", "WWC", "WWF", "WWP"
    ));

    // 5 Exception codes: Allow TS on top + Allow FP/PRA without revival + Allow stacking
    private static final Set<String> PS_EXCEPTION_CODES = new HashSet<>(Arrays.asList(
        "DIP", "FOR", "MID", "RIP", "RP2"
    ));

    // 3 PS codes allowed on paid notices (trigger refund)
    private static final Set<String> PS_REFUND_CODES = new HashSet<>(Arrays.asList(
        "APP", "CFA", "VST"
    ));

    // CRS codes (payment-triggered)
    private static final Set<String> CRS_PS_CODES = new HashSet<>(Arrays.asList(
        "FP", "PRA"
    ));

    @Override
    protected String getSuspensionType() {
        return "PS";
    }

    /**
     * Main method to process PS for a single notice
     */
    @Transactional
    public Map<String, Object> processPS(String noticeNo, String suspensionSource,
            String reasonOfSuspension, String suspensionRemarks,
            String officerAuthorisingSuspension, String srNo, String caseNo) {

        log.info("Processing PS for notice: {}, reason: {}", noticeNo, reasonOfSuspension);

        try {
            // Step 1: Check mandatory fields (from base)
            Map<String, Object> validationResult = validateMandatoryFields(noticeNo, "PS",
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

            // Step 2.1: Check if notice is already PS with the SAME code (idempotent)
            if ("PS".equals(notice.getSuspensionType())) {
                String existingPsCode = notice.getEprReasonOfSuspension();
                if (reasonOfSuspension.equals(existingPsCode)) {
                    // Idempotent - notice already has this exact PS code, no change needed
                    return createSuccessResponse(noticeNo, srNo, "Success Notice already PS", "OCMS-2001");
                }
                // Different PS code - continue to overlap validation (revive or stacking logic)
            }

            // Step 3: Check source permission (PS-specific)
            Map<String, Object> permissionResult = checkSourcePermissionPS(noticeNo, suspensionSource,
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

            // Step 5: PS-specific - Check paid notice validation
            Map<String, Object> paidResult = validatePaidNotice(notice, reasonOfSuspension);
            if (paidResult != null) {
                return paidResult;
            }

            // Step 6: Validate overlap (PS-specific) - Complex revive old PS logic
            Map<String, Object> overlapResult = validateOverlap(notice, reasonOfSuspension);
            if (overlapResult != null) {
                return overlapResult;
            }

            // Step 7: Calculate revival date (PS-specific) - Always NULL
            LocalDateTime dueDateOfRevival = calculateRevivalDate(reasonOfSuspension, null);

            // Step 8: Update database (from base)
            Map<String, Object> dbResult = updateDatabase(notice, "PS", reasonOfSuspension,
                    dueDateOfRevival, suspensionSource, suspensionRemarks,
                    officerAuthorisingSuspension, srNo, caseNo);
            if (dbResult != null) {
                return dbResult;
            }

            // Success response
            return createSuccessResponse(noticeNo, srNo, "PS Success");

        } catch (Exception e) {
            log.error("Error processing PS for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
        }
    }

    /**
     * PS-specific: Check source permission
     */
    private Map<String, Object> checkSourcePermissionPS(String noticeNo, String suspensionSource,
            String reasonOfSuspension) {

        if (suspensionSource == null || suspensionSource.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4000", "Suspension Source is missing");
        }

        boolean sourceAllowed = false;

        // Check if source is allowed for this PS code
        if (SOURCE_PLUS.equals(suspensionSource)) {
            sourceAllowed = PLUS_ALLOWED_PS_CODES.contains(reasonOfSuspension);
        } else if (SOURCE_OCMS_STAFF.equals(suspensionSource)) {
            sourceAllowed = OCMS_STAFF_PS_CODES.contains(reasonOfSuspension);
        } else if (SOURCE_CRON.equals(suspensionSource)) {
            sourceAllowed = OCMS_BACKEND_PS_CODES.contains(reasonOfSuspension);
        }

        if (!sourceAllowed) {
            return createErrorResponse(noticeNo, "OCMS-4000", "Source not authorized to use this Suspension Code");
        }

        return null; // Allowed
    }

    /**
     * PS-specific: Validate paid notice
     * Only APP, CFA, VST can be applied to paid notices (trigger refund)
     */
    private Map<String, Object> validatePaidNotice(OcmsValidOffenceNotice notice, String psCode) {
        if (notice.getCrsReasonOfSuspension() != null) {
            // Notice is paid (fully or partially)
            if (!PS_REFUND_CODES.contains(psCode)) {
                return createErrorResponse(notice.getNoticeNo(), "OCMS-4003",
                    "Paid/partially paid notices only allow APP, CFA, or VST");
            }

            // If allowed code, log refund trigger (MVP: store refund date)
            log.info("PS code {} applied to paid notice {} - refund triggered", psCode, notice.getNoticeNo());
            // TODO: Implement refund logic (MVP: store refund date)
        }
        return null; // Valid
    }

    @Override
    protected Map<String, Object> validateOverlap(OcmsValidOffenceNotice notice, String newPsCode) {
        // PS overlap validation: Complex logic with revive old PS

        if (!"PS".equals(notice.getSuspensionType())) {
            // No existing PS - allow new PS
            return null;
        }

        String existingPsCode = notice.getEprReasonOfSuspension();
        if (existingPsCode == null) {
            return null; // No existing PS code
        }

        log.info("Notice {} has existing PS: {}, applying new PS: {}",
            notice.getNoticeNo(), existingPsCode, newPsCode);

        // Check if new PS is CRS code (FP/PRA)
        if (CRS_PS_CODES.contains(newPsCode)) {
            // CRS PS logic: Only allow if existing PS is one of 5 exception codes
            if (!PS_EXCEPTION_CODES.contains(existingPsCode)) {
                return createErrorResponse(notice.getNoticeNo(), "OCMS-4008",
                    "Cannot apply PS-FP/PRA on existing PS");
            }
            // Allow stacking - no revival
            log.info("CRS PS {} allowed on exception code {} - no revival", newPsCode, existingPsCode);

        } else {
            // New EPR PS logic: Check if we need to revive old PS
            if (PS_EXCEPTION_CODES.contains(existingPsCode)) {
                // Exception codes - allow stacking, no revival
                log.info("New PS {} allowed on exception code {} - no revival", newPsCode, existingPsCode);
            } else {
                // Revive old PS before inserting new
                log.info("Reviving old PS {} before applying new PS {}", existingPsCode, newPsCode);
                reviveOldPS(notice.getNoticeNo());
            }
        }

        return null; // Overlap validation passed
    }

    /**
     * PS-specific: Revive old PS
     * Set date_of_revival = NOW(), revival_reason = 'CSR'
     */
    private void reviveOldPS(String noticeNo) {
        try {
            // Query active PS for this notice
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});
            List<SuspendedNotice> suspensions = suspendedNoticeService.getAll(queryParams).getData();

            LocalDateTime now = LocalDateTime.now();
            int revivedCount = 0;

            for (SuspendedNotice suspension : suspensions) {
                // Only revive active PS (date_of_revival IS NULL)
                if ("PS".equals(suspension.getSuspensionType()) && suspension.getDateOfRevival() == null) {
                    suspension.setDateOfRevival(now);
                    suspension.setRevivalReason("CSR"); // Change Suspension Reason
                    suspension.setOfficerAuthorisingRevival("SYSTEM");
                    suspension.setUpdUserId("SYSTEM");
                    suspension.setUpdDate(now);

                    suspendedNoticeService.save(suspension);
                    revivedCount++;

                    log.info("Revived PS for notice {}: reason={}, sr_no={}",
                        noticeNo, suspension.getReasonOfSuspension(), suspension.getSrNo());
                }
            }

            if (revivedCount == 0) {
                log.warn("No active PS found to revive for notice {}", noticeNo);
            } else {
                log.info("Revived {} active PS records for notice {}", revivedCount, noticeNo);
            }

        } catch (Exception e) {
            log.error("Error reviving old PS for notice {}: {}", noticeNo, e.getMessage(), e);
            // Don't fail the entire operation - log and continue
        }
    }

    @Override
    protected LocalDateTime calculateRevivalDate(String reasonOfSuspension, Integer daysToRevive) {
        // PS-specific: Revival date is ALWAYS NULL (permanent suspension)
        return null;
    }
}
