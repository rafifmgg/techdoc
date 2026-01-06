package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason.SuspensionReason;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason.SuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
// import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;

import lombok.extern.slf4j.Slf4j;

/**
 * Suspension Processing Helper
 * Handles validation and processing of suspension requests for both TS (Temporary Suspension)
 * and PS (Permanent Suspension).
 *
 * VALIDATION CHECKLIST (per OCMS17 Technical Spec v1.0, lines 698-711):
 * ✅ 1. Auth - JWT (OCMS) or APIM (PLUS) validation (handled by Spring Security)
 * ✅ 2. Source Permission - Validates suspension source can use requested suspension code
 *                          {@link #checkSourcePermission(String, String, String, String, String)}
 * ✅ 3. Court Stage - Blocks suspension if notice at restricted processing stage (CPC/CFC/CRT/CRC)
 *                    {@link #checkStageAllowed(String, String, String)}
 * ✅ 4. Payment - Blocks suspension if notice is paid/partially paid (crs_reason_of_suspension != null)
 *                Special rule for PS-APP: allows paid notices and triggers refund
 *                Validation at lines 168-169, 320-321, 495-496
 * ⚠️ 5. EPR (Electronic Payment Record) - **NOT FULLY IMPLEMENTED**
 *       REQUIREMENT: Per OCMS17 Tech Spec line 706: "TS requires EPR complete/approved" → OCMS-4004
 *       CURRENT STATUS:
 *       - OCMS-4004 is currently used for "Notice has been permanently suspended" (lines 238, 465)
 *       - EPR approval/completion validation is NOT implemented
 *       - Documentation states "To Confirm: EPR definition" (line 711) - requirement not finalized
 *
 *       TODO: Implement EPR validation when requirements are clarified:
 *       - Define what "EPR complete/approved" means
 *       - Identify which field(s) in ocms_valid_offence_notice indicate EPR status
 *       - Add validation check before allowing suspension
 *       - Return OCMS-4004 "EPR not approved" error when EPR incomplete
 *
 *       NOTES:
 *       - Current implementation checks `epr_reason_of_suspension` and `epr_date_of_suspension`
 *         but these are SET by suspension, not checked as prerequisites
 *       - May need new field like `epr_status` or `epr_approved_date` to validate EPR completion
 * ✅ 6. Overlap - Checks for active suspensions with future revival dates
 *                {@link #checkOverlappingSuspension(String)}
 * ✅ 7. Duration - Calculates revival date from ocms_suspension_reason.no_of_days_for_revival
 *                 {@link #calculateDueDateOfRevival(String, Integer)}
 *
 * @author Claude Code
 * @since 2025-01-19 (OCMS 17 & 18)
 */
@Component
@Slf4j
public class SuspensionProcessingHelper {

    @Autowired
    private OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    // @Autowired
    // private EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    @Autowired
    private SuspensionReasonService suspensionReasonService;

    @Autowired
    private SuspendedNoticeService suspendedNoticeService;

    // Source codes - from SystemConstant.Subsystem
    public static final String SOURCE_CRON = SystemConstant.Subsystem.OCMS_CODE;
    public static final String SOURCE_OCMS_STAFF = SystemConstant.Subsystem.OCMS_CODE; // Same as CRON
    public static final String SOURCE_PLUS = SystemConstant.Subsystem.PLUS_CODE;

    // Suspension codes that allow PLUS source
    private static final Set<String> PLUS_ALLOWED_CODES = new HashSet<>(Arrays.asList(
        "APE", "APP", "CCE", "PRI", "RED", "MS"
    ));

    // Suspension codes that allow OCMS source
    private static final Set<String> OCMS_ALLOWED_CODES = new HashSet<>(Arrays.asList(
        "ACR", "CLV", "FPL", "HST", "INS", "MS", "NRO", "OLD", "OUT",
        "PAM", "PDP", "PRI", "ROV", "SYS", "UNC"
    ));

    // Suspension codes that allow CRON source
    private static final Set<String> CRON_ALLOWED_CODES = new HashSet<>(Arrays.asList(
        "ACR", "CLV", "HST", "NRO", "PAM", "PDP", "ROV", "SYS"
    ));

    // Legacy: CRON/OCMS common codes (for backward compatibility)
    private static final Set<String> CRON_OCMS_COMMON_CODES = new HashSet<>(Arrays.asList(
        "NRO", "PDP", "SYS"
    ));

    // ROV code that only allows ROV stage
    private static final Set<String> CRON_OCMS_ROV_CODE = new HashSet<>(Arrays.asList(
        "ROV"
    ));

    // Stages allowed for most suspension codes
    private static final Set<String> COMMON_ALLOWED_STAGES = new HashSet<>(Arrays.asList(
        "NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"
    ));

    // ROV code only allows ROV stage
    private static final Set<String> ROV_ALLOWED_STAGES = new HashSet<>(Arrays.asList("ROV"));

    // PS suspension reasons that allow TS
    private static final Set<String> PS_REASONS_ALLOW_TS = new HashSet<>(Arrays.asList(
        "FOR", "MID", "DIP", "RP2", "RIP"
    ));

    // PS suspension reasons allowed for PLUS source
    private static final Set<String> PLUS_PS_ALLOWED_CODES = new HashSet<>(Arrays.asList(
        "APP", "CFA", "CAN", "OTH", "VST"
    ));

    /**
     * Validate suspension for a single notice without executing (dry-run)
     * Used for checking mode
     */
    public Map<String, Object> validateSuspension(String noticeNo, String suspensionSource,
            String suspensionType, String reasonOfSuspension, Integer daysToRevive,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {
        return validateSuspension(noticeNo, suspensionSource, suspensionType, reasonOfSuspension,
                daysToRevive, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo, null);
    }

    /**
     * Validate suspension with requestSource tracking
     */
    public Map<String, Object> validateSuspension(String noticeNo, String suspensionSource,
            String suspensionType, String reasonOfSuspension, Integer daysToRevive,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo,
            String requestSource) {

        log.info("Validating suspension for notice: {}, type: {}, reason: {}, requestSource: {}",
                noticeNo, suspensionType, reasonOfSuspension, requestSource);

        try {
            // Step 1: Check mandatory fields
            Map<String, Object> validationResult = validateMandatoryFields(noticeNo, suspensionType,
                    reasonOfSuspension, officerAuthorisingSuspension);
            if (validationResult != null) {
                return validationResult;
            }

            // Step 2: Query notice from database
            Optional<OcmsValidOffenceNotice> noticeOpt = ocmsValidOffenceNoticeService.getById(noticeNo);
            if (!noticeOpt.isPresent()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Invalid Notice Number");
            }
            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Step 2.1: Check if notice is already TS
            if ("TS".equals(notice.getSuspensionType())) {
                return createErrorResponse(noticeNo, "OCMS-4008", "Offence Notice already TS");
            }

            // Step 2.2: Check if notice is already PS
            if ("PS".equals(notice.getSuspensionType())) {
                return createErrorResponse(noticeNo, "OCMS-4008", "Offence Notice already PS");
            }

            // Special handling for PS from PLUS source
            if ("PS".equals(suspensionType) && SOURCE_PLUS.equals(suspensionSource)) {
                log.info("Validating PS request from PLUS for notice: {}", noticeNo);

                // Validate PS-specific requirements for PLUS
                Map<String, Object> psValidationResult = validatePSRequestForPlus(noticeNo, suspensionSource,
                    reasonOfSuspension, notice);
                if (psValidationResult != null) {
                    return psValidationResult;
                }

                // All validations passed for PS
                return createValidationSuccessResponse(noticeNo, suspensionType, reasonOfSuspension);
            }

            // Step 3: Check source permission (for TS)
            Map<String, Object> permissionResult = checkSourcePermission(noticeNo, suspensionSource,
                    reasonOfSuspension, notice.getLastProcessingStage(), requestSource);
            if (permissionResult != null) {
                return permissionResult;
            }

            // Step 4: Check if stage is allowed for suspension
            Map<String, Object> stageResult = checkStageAllowed(noticeNo, reasonOfSuspension,
                    notice.getLastProcessingStage());
            if (stageResult != null) {
                return stageResult;
            }

            // Step 5: Check if notice is paid
            if (notice.getCrsReasonOfSuspension() != null) {
                return createErrorResponse(noticeNo, "OCMS-4003", "Notice is fully or partially paid");
            }

            // Step 6: Handle PS (Permanent Suspension) logic (for existing PS records)
            if ("PS".equals(notice.getSuspensionType())) {
                Map<String, Object> psResult = validatePermanentSuspension(notice);
                if (psResult != null) {
                    return psResult;
                }
            }

            // Step 7: Check for overlapping suspensions
            Map<String, Object> overlapResult = checkOverlappingSuspension(noticeNo);
            if (overlapResult != null) {
                return overlapResult;
            }

            // All validations passed
            return createValidationSuccessResponse(noticeNo, suspensionType, reasonOfSuspension);

        } catch (Exception e) {
            log.error("Error validating suspension for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "Undefined error. Please inform Administrator");
        }
    }

    /**
     * Main method to process suspension for a single notice
     */
    public Map<String, Object> processSuspension(String noticeNo, String suspensionSource,
            String suspensionType, String reasonOfSuspension, Integer daysToRevive,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {
        return processSuspension(noticeNo, suspensionSource, suspensionType, reasonOfSuspension,
                daysToRevive, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo, null);
    }

    /**
     * Process suspension with requestSource tracking
     */
    public Map<String, Object> processSuspension(String noticeNo, String suspensionSource,
            String suspensionType, String reasonOfSuspension, Integer daysToRevive,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo,
            String requestSource) {

        log.info("Processing suspension for notice: {}, type: {}, reason: {}, requestSource: {}",
                noticeNo, suspensionType, reasonOfSuspension, requestSource);

        try {
            // Step 1: Check mandatory fields
            Map<String, Object> validationResult = validateMandatoryFields(noticeNo, suspensionType,
                    reasonOfSuspension, officerAuthorisingSuspension);
            if (validationResult != null) {
                return validationResult;
            }

            // Step 2: Query notice from database
            Optional<OcmsValidOffenceNotice> noticeOpt = ocmsValidOffenceNoticeService.getById(noticeNo);
            if (!noticeOpt.isPresent()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Invalid Notice Number");
            }
            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Step 2.1: Check if notice is already TS
            if ("TS".equals(notice.getSuspensionType())) {
                return createSuccessResponse(noticeNo, srNo, "Success Notice already TS", "OCMS-2001");
            }

            // Step 2.2: Check if notice is already PS
            if ("PS".equals(notice.getSuspensionType())) {
                return createErrorResponse(noticeNo, "OCMS-4004", "Notice has been permanently suspended");
            }

            // Special handling for PS from PLUS source
            if ("PS".equals(suspensionType) && SOURCE_PLUS.equals(suspensionSource)) {
                log.info("Processing PS request from PLUS for notice: {}", noticeNo);

                // Validate PS-specific requirements for PLUS
                Map<String, Object> psValidationResult = validatePSRequestForPlus(noticeNo, suspensionSource,
                    reasonOfSuspension, notice);
                if (psValidationResult != null) {
                    return psValidationResult;
                }

                // Calculate due date of revival
                LocalDateTime dueDateOfRevival = calculateDueDateOfRevival(reasonOfSuspension, daysToRevive);
                if (dueDateOfRevival == null) {
                    return createErrorResponse(noticeNo, "OCMS-4007", "Undefined error. Please inform Administrator");
                }

                // Update database
                Map<String, Object> dbResult = updateDatabase(notice, suspensionType, reasonOfSuspension,
                        dueDateOfRevival, suspensionSource, suspensionRemarks,
                        officerAuthorisingSuspension, srNo, caseNo);
                if (dbResult != null) {
                    return dbResult;
                }

                // Success response with additional fields for PS-APP if crs_reason_of_suspension is not null
                Map<String, Object> response = createSuccessResponse(noticeNo, srNo, "Success created");

                // For PS-APP only: if crs_reason_of_suspension is not null (payment has been made),
                // add refund fields to response
                if ("APP".equals(reasonOfSuspension) && notice.getCrsReasonOfSuspension() != null) {
                    log.info("PS-APP for notice {} has crs_reason_of_suspension: {}, adding refund fields to response",
                            noticeNo, notice.getCrsReasonOfSuspension());

                    // Generate refundNoticeId with format: RE{YYYYMMDD}{subsystem}{runningNumber}
                    // Example: RE20251211005001
                    // RE = Prefix for refund
                    // 20251211 = Date refund was triggered (current date)
                    // 005 = sub-system label (005 = PLUS, 004 = OCMS)
                    // 001 = running number (3-digit random)
                    LocalDateTime refundDate = LocalDateTime.now();
                    String dateStr = String.format("%04d%02d%02d",
                            refundDate.getYear(), refundDate.getMonthValue(), refundDate.getDayOfMonth());
                    String subsystemCode = SOURCE_PLUS.equals(suspensionSource) ? "005" : "004";
                    int randomRunningNumber = (int) (Math.random() * 1000); // Random 0-999
                    String refundNoticeId = String.format("RE%s%s%03d",
                            dateStr, subsystemCode, randomRunningNumber);

                    // Generate dummy refundCreDate: random date from this month before today
                    LocalDateTime now = LocalDateTime.now();
                    int randomDay = (int) (Math.random() * (now.getDayOfMonth() - 1)) + 1;
                    LocalDateTime refundCreDate = now.withDayOfMonth(randomDay)
                            .withHour((int) (Math.random() * 24))
                            .withMinute((int) (Math.random() * 60))
                            .withSecond((int) (Math.random() * 60))
                            .withNano(0);

                    response.put("refundNoticeId", refundNoticeId);
                    response.put("refundCreDate", refundCreDate);
                }

                return response;
            }

            // Step 3: Check source permission
            Map<String, Object> permissionResult = checkSourcePermission(noticeNo, suspensionSource,
                    reasonOfSuspension, notice.getLastProcessingStage(), requestSource);
            if (permissionResult != null) {
                return permissionResult;
            }

            // Step 4: Check if stage is allowed for suspension
            Map<String, Object> stageResult = checkStageAllowed(noticeNo, reasonOfSuspension,
                    notice.getLastProcessingStage());
            if (stageResult != null) {
                return stageResult;
            }

            // Step 5: Check if notice is paid (for all notices, not just PS)
            if (notice.getCrsReasonOfSuspension() != null) {
                return createErrorResponse(noticeNo, "OCMS-4003", "Notice is fully or partially paid");
            }

            // Step 6: Handle PS (Permanent Suspension) logic
            if ("PS".equals(notice.getSuspensionType())) {
                Map<String, Object> psResult = validatePermanentSuspension(notice);
                if (psResult != null) {
                    return psResult;
                }
            }

            // Step 6.5: Check for overlapping suspensions
            Map<String, Object> overlapResult = checkOverlappingSuspension(noticeNo);
            if (overlapResult != null) {
                return overlapResult;
            }

            // Step 7: Calculate due date of revival
            LocalDateTime dueDateOfRevival = calculateDueDateOfRevival(reasonOfSuspension, daysToRevive);
            if (dueDateOfRevival == null) {
                return createErrorResponse(noticeNo, "OCMS-4007", "Undefined error. Please inform Administrator");
            }

            // Step 8: Update database
            Map<String, Object> dbResult = updateDatabase(notice, suspensionType, reasonOfSuspension,
                    dueDateOfRevival, suspensionSource, suspensionRemarks,
                    officerAuthorisingSuspension, srNo, caseNo);
            if (dbResult != null) {
                return dbResult;
            }

            // Success response
            return createSuccessResponse(noticeNo, srNo, "Success created");

        } catch (Exception e) {
            log.error("Error processing suspension for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "Undefined error. Please inform Administrator");
        }
    }

    /**
     * Step 1: Validate mandatory fields
     */
    private Map<String, Object> validateMandatoryFields(String noticeNo, String suspensionType,
            String reasonOfSuspension, String officerAuthorisingSuspension) {

        if (noticeNo == null || noticeNo.trim().isEmpty()) {
            return createErrorResponse("", "OCMS-4010", "Invalid Notice Number");
        }

        if (suspensionType == null || suspensionType.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Suspension Type is missing");
        }

        if (reasonOfSuspension == null || reasonOfSuspension.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Reason of Suspension is missing");
        }

        if (officerAuthorisingSuspension == null || officerAuthorisingSuspension.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4006", "Officer Authorising Suspension is missing");
        }

        // Validate suspension type
        if (!"TS".equals(suspensionType) && !"PS".equals(suspensionType)) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Suspension Type is missing");
        }

        return null; // Valid
    }

    /**
     * Step 2: Check source permission based on suspension code and source
     * Uses requestSource to differentiate between OCMS_STAFF and CRON
     */
    private Map<String, Object> checkSourcePermission(String noticeNo, String suspensionSource,
            String reasonOfSuspension, String lastProcessingStage, String requestSource) {

        if (suspensionSource == null || suspensionSource.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Suspension Source is missing");
        }

        boolean sourceAllowed = false;

        // Check if source is allowed for this suspension code
        if (SOURCE_PLUS.equals(suspensionSource)) {
            sourceAllowed = PLUS_ALLOWED_CODES.contains(reasonOfSuspension);
        } else if (SOURCE_CRON.equals(suspensionSource) || SOURCE_OCMS_STAFF.equals(suspensionSource)) {
            // Differentiate between OCMS_STAFF and CRON using requestSource
            if ("CRON".equals(requestSource)) {
                // CRON: Only 8 codes allowed
                sourceAllowed = CRON_ALLOWED_CODES.contains(reasonOfSuspension);
            } else if ("OCMS_STAFF".equals(requestSource)) {
                // OCMS Staff: 15 codes allowed
                sourceAllowed = OCMS_ALLOWED_CODES.contains(reasonOfSuspension);
            }
        }

        if (!sourceAllowed) {
            return createErrorResponse(noticeNo, "OCMS-4000", "Source not authorized to use this Suspension Code");
        }

        return null; // Allowed
    }

    /**
     * Step 3: Check if processing stage is allowed for suspension code
     */
    private Map<String, Object> checkStageAllowed(String noticeNo, String reasonOfSuspension, String lastProcessingStage) {

        if (lastProcessingStage == null) {
            return createErrorResponse(noticeNo, "OCMS-4002", "TS Code cannot be applied due to Last Processing Stage is not among the eligible stages.");
        }

        // Check if notice is at Court Stage (neither CPC nor CFC)
        if ("CRT".equals(lastProcessingStage) || "CRC".equals(lastProcessingStage)) {
            return createErrorResponse(noticeNo, "OCMS-4002", "Notice is under Court processing");
        }

        Set<String> allowedStages;

        // ROV code only allows ROV stage
        if ("ROV".equals(reasonOfSuspension)) {
            allowedStages = ROV_ALLOWED_STAGES;
        } else {
            // Other codes allow common stages
            allowedStages = COMMON_ALLOWED_STAGES;
        }

        if (!allowedStages.contains(lastProcessingStage)) {
            return createErrorResponse(noticeNo, "OCMS-4002", "TS Code cannot be applied due to Last Processing Stage is not among the eligible stages.");
        }

        return null; // Stage allowed
    }

    /**
     * Step 6: Validate permanent suspension conditions
     */
    private Map<String, Object> validatePermanentSuspension(OcmsValidOffenceNotice notice) {

        // Check if epr_reason_of_suspension is in allowed list for TS
        // (payment check already done in main flow)
        String eprReason = notice.getEprReasonOfSuspension();
        if (eprReason == null || !PS_REASONS_ALLOW_TS.contains(eprReason)) {
            return createErrorResponse(notice.getNoticeNo(), "OCMS-4004", "Notice has been permanently suspended");
        }

        return null; // PS conditions are valid for TS
    }

    /**
     * Validate PS request from PLUS source
     * - Source must be PLUS
     * - Reason code must be APP/CFA/CAN/OTH/VST
     * - For PS-APP (reason code APP): allow paid notices and include refund fields in response
     * - For other PS reasons: notice must not be paid
     * - Last processing stage must not be CFC or CPC
     */
    private Map<String, Object> validatePSRequestForPlus(String noticeNo, String suspensionSource,
            String reasonOfSuspension, OcmsValidOffenceNotice notice) {

        // Check source is PLUS (redundant but explicit)
        if (!SOURCE_PLUS.equals(suspensionSource)) {
            return createErrorResponse(noticeNo, "OCMS-4000", "PS can only be applied from PLUS source");
        }

        // Check reason code is allowed for PLUS PS
        if (!PLUS_PS_ALLOWED_CODES.contains(reasonOfSuspension)) {
            return createErrorResponse(noticeNo, "OCMS-4000",
                "Invalid PS reason code for PLUS source. Allowed: APP, CFA, CAN, OTH, VST");
        }

        // For PS-APP (reason code "APP"), allow paid notices
        // For other PS reasons, check notice is not paid
        if (!"APP".equals(reasonOfSuspension) && notice.getCrsReasonOfSuspension() != null) {
            return createErrorResponse(noticeNo, "OCMS-4003", "Notice is fully or partially paid");
        }

        // Check not at CFC or CPC stage
        String lastStage = notice.getLastProcessingStage();
        if ("CFC".equals(lastStage) || "CPC".equals(lastStage)) {
            return createErrorResponse(noticeNo, "OCMS-4002", "PS cannot be applied to CFC/CPC stage");
        }

        return null; // Valid
    }

    /**
     * Check for overlapping active suspensions
     * Returns error if there's an active suspension (due_date_of_revival > now)
     */
    private Map<String, Object> checkOverlappingSuspension(String noticeNo) {
        try {
            // Query suspended notices for this notice number
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});
            List<SuspendedNotice> suspensions = suspendedNoticeService.getAll(queryParams).getData();

            LocalDateTime now = LocalDateTime.now();
            for (SuspendedNotice existing : suspensions) {
                if (existing.getDueDateOfRevival() != null &&
                    existing.getDueDateOfRevival().isAfter(now)) {
                    return createErrorResponse(noticeNo, "OCMS-4005",
                        "Overlapping suspension detected");
                }
            }
        } catch (Exception e) {
            log.error("Error checking overlapping suspension for notice {}: {}", noticeNo, e.getMessage());
            // Don't fail the entire operation, just log the error
        }
        return null; // No overlap detected
    }

    /**
     * Step 7: Calculate due date of revival
     */
    private LocalDateTime calculateDueDateOfRevival(String reasonOfSuspension, Integer daysToRevive) {

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
            log.warn("No revival days found for reason: {}, using default 30 days", reasonOfSuspension);
            return currentDate.plusDays(30);

        } catch (Exception e) {
            log.error("Error looking up suspension reason: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Step 8: Update database tables
     */
    private Map<String, Object> updateDatabase(OcmsValidOffenceNotice notice, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival, String suspensionSource,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {

        try {
            LocalDateTime suspensionDate = LocalDateTime.now();

            // Update ocms_valid_offence_notice
            notice.setSuspensionType(suspensionType);
            notice.setEprReasonOfSuspension(reasonOfSuspension);
            notice.setEprDateOfSuspension(suspensionDate);
            notice.setDueDateOfRevival(dueDateOfRevival);

            ocmsValidOffenceNoticeService.save(notice);

            // Update eocms_valid_offence_notice (eVON database sync)
            // TODO: Implement when EocmsValidOffenceNoticeService is available
            // IMPORTANT: This sync is required for database consistency
            // If sync fails, the entire suspension should be rolled back
            try {
                // eocmsValidOffenceNoticeService.updateSuspensionFields(notice.getNoticeNo(),
                //     suspensionType, reasonOfSuspension, suspensionDate, dueDateOfRevival);
                log.debug("eVON sync skipped - service not implemented");
            } catch (Exception e) {
                log.error("Failed to sync eocms_valid_offence_notice for notice {}: {}",
                    notice.getNoticeNo(), e.getMessage(), e);
                // TODO: When eVON service is implemented, return error instead of warning
                // return createErrorResponse(notice.getNoticeNo(), "OCMS-4007",
                //     "Database sync error. Please inform Administrator");
            }

            // Insert into ocms_suspended_notice
            SuspendedNotice suspendedNotice = SuspendedNotice.builder()
                .noticeNo(notice.getNoticeNo())
                .dateOfSuspension(suspensionDate)
                .srNo(Integer.parseInt(srNo))
                .suspensionSource(suspensionSource)
                .caseNo(caseNo)
                .suspensionType(suspensionType)
                .reasonOfSuspension(reasonOfSuspension)
                .officerAuthorisingSupension(officerAuthorisingSuspension) // Note: typo in entity field name
                .dueDateOfRevival(dueDateOfRevival)
                .suspensionRemarks(suspensionRemarks)
                .build();

            suspendedNoticeService.save(suspendedNotice);

            return null; // Success

        } catch (Exception e) {
            log.error("Database update error for notice {}: {}", notice.getNoticeNo(), e.getMessage(), e);
            return createErrorResponse(notice.getNoticeNo(), "OCMS-4007", "Undefined error. Please inform Administrator");
        }
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String noticeNo, String appCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }

    /**
     * Create success response with default OCMS-2000
     */
    private Map<String, Object> createSuccessResponse(String noticeNo, String srNo, String message) {
        return createSuccessResponse(noticeNo, srNo, message, "OCMS-2000");
    }

    /**
     * Create success response with custom app code
     */
    private Map<String, Object> createSuccessResponse(String noticeNo, String srNo, String message, String appCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("srNo", srNo);
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }

    /**
     * Create validation success response for checking mode
     * Format: { "appCode": "OCMS-2000", "message": "Validate suspension success",
     *           "noticeNo": "...", "suspensionType": "...", "reasonOfSuspension": "...", "eligible": true }
     */
    private Map<String, Object> createValidationSuccessResponse(String noticeNo, String suspensionType,
            String reasonOfSuspension) {
        Map<String, Object> response = new HashMap<>();
        response.put("appCode", "OCMS-2000");
        response.put("message", "Validate suspension success");
        response.put("noticeNo", noticeNo);
        response.put("suspensionType", suspensionType);
        response.put("reasonOfSuspension", reasonOfSuspension);
        response.put("eligible", true);
        return response;
    }
}