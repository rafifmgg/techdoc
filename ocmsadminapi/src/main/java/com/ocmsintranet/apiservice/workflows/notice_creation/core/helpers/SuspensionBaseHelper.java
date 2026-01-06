package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason.SuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for suspension processing
 * Contains common validation and database operation logic shared between TS and PS
 *
 * @author Claude Code
 * @since 2025-01-19 (OCMS 18)
 */
@Slf4j
public abstract class SuspensionBaseHelper {

    @Autowired
    protected OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    @Autowired
    protected EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    @Autowired
    protected SuspensionReasonService suspensionReasonService;

    @Autowired
    protected SuspendedNoticeService suspendedNoticeService;

    // Source codes - from SystemConstant.Subsystem
    protected static final String SOURCE_CRON = SystemConstant.Subsystem.OCMS_CODE;
    protected static final String SOURCE_OCMS_STAFF = SystemConstant.Subsystem.OCMS_CODE; // Same as CRON
    protected static final String SOURCE_PLUS = SystemConstant.Subsystem.PLUS_CODE;

    // Stages allowed for most suspension codes
    protected static final Set<String> COMMON_ALLOWED_STAGES = new HashSet<>(Arrays.asList(
        "NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"
    ));

    // ROV code only allows ROV stage
    protected static final Set<String> ROV_ALLOWED_STAGES = new HashSet<>(Arrays.asList("ROV"));

    // CLV code only allows RR3/DR3 stages (VIP vehicles at end of reminder stage)
    protected static final Set<String> CLV_ALLOWED_STAGES = new HashSet<>(Arrays.asList("RR3", "DR3"));

    // NRO code excludes RR3/DR3 stages (MHA exceptions not at reminder end stages)
    protected static final Set<String> NRO_EXCLUDED_STAGES = new HashSet<>(Arrays.asList("RR3", "DR3"));

    /**
     * Get suspension type (TS or PS) - implemented by subclasses
     */
    protected abstract String getSuspensionType();

    /**
     * Calculate revival date - implemented by subclasses
     * TS: Calculate from reference table
     * PS: Always return NULL
     */
    protected abstract LocalDateTime calculateRevivalDate(String reasonOfSuspension, Integer daysToRevive);

    /**
     * Validate overlap conditions - implemented by subclasses
     * TS: Simple check if PS allows TS
     * PS: Complex revive old PS logic
     */
    protected abstract Map<String, Object> validateOverlap(OcmsValidOffenceNotice notice, String reasonOfSuspension);

    /**
     * Common validation: Check mandatory fields
     */
    protected Map<String, Object> validateMandatoryFields(String noticeNo, String suspensionType,
            String reasonOfSuspension, String officerAuthorisingSuspension) {

        if (noticeNo == null || noticeNo.trim().isEmpty()) {
            return createErrorResponse("", "OCMS-4001", "Invalid Notice Number");
        }

        if (suspensionType == null || suspensionType.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Suspension Type is missing");
        }

        if (reasonOfSuspension == null || reasonOfSuspension.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Reason of Suspension is missing");
        }

        if (officerAuthorisingSuspension == null || officerAuthorisingSuspension.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Officer Authorising Suspension is missing");
        }

        // Validate suspension type
        if (!"TS".equals(suspensionType) && !"PS".equals(suspensionType)) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Invalid Suspension Type");
        }

        return null; // Valid
    }

    /**
     * Common validation: Query notice from database
     */
    protected Optional<OcmsValidOffenceNotice> queryNotice(String noticeNo) {
        return ocmsValidOffenceNoticeService.getById(noticeNo);
    }

    /**
     * Common validation: Check processing stage
     *
     * Code-specific stage restrictions (per OCMS 17 v1.3):
     * - ROV: Only ROV stage allowed
     * - CLV: Only RR3/DR3 stages (VIP vehicles at end of reminder stage)
     * - NRO: All common stages EXCEPT RR3/DR3
     * - All others: Common stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC)
     */
    protected Map<String, Object> checkStageAllowed(String noticeNo, String reasonOfSuspension, String lastProcessingStage) {

        if (lastProcessingStage == null) {
            return createErrorResponse(noticeNo, "OCMS-4002",
                getSuspensionType() + " Code cannot be applied due to Last Processing Stage is not among the eligible stages.");
        }

        // Check if notice is at Court Stage (neither CPC nor CFC)
        if ("CRT".equals(lastProcessingStage) || "CRC".equals(lastProcessingStage)) {
            return createErrorResponse(noticeNo, "OCMS-4002", "Notice is under Court processing");
        }

        Set<String> allowedStages;
        String errorMessage = getSuspensionType() + " Code cannot be applied due to Last Processing Stage is not among the eligible stages.";

        // Code-specific stage restrictions
        if ("ROV".equals(reasonOfSuspension)) {
            // ROV code only allows ROV stage
            allowedStages = ROV_ALLOWED_STAGES;
            errorMessage = "ROV code can only be applied at ROV stage. Current stage: " + lastProcessingStage;

        } else if ("CLV".equals(reasonOfSuspension)) {
            // CLV code only allows RR3/DR3 stages (VIP vehicles at end of reminder)
            allowedStages = CLV_ALLOWED_STAGES;
            errorMessage = "CLV code can only be applied at RR3 or DR3 stage. Current stage: " + lastProcessingStage;

        } else if ("NRO".equals(reasonOfSuspension)) {
            // NRO code excludes RR3/DR3 stages
            if (NRO_EXCLUDED_STAGES.contains(lastProcessingStage)) {
                return createErrorResponse(noticeNo, "OCMS-4002",
                    "NRO code cannot be applied at RR3/DR3 stages. Current stage: " + lastProcessingStage);
            }
            allowedStages = COMMON_ALLOWED_STAGES;

        } else {
            // Other codes allow common stages
            allowedStages = COMMON_ALLOWED_STAGES;
        }

        if (!allowedStages.contains(lastProcessingStage)) {
            return createErrorResponse(noticeNo, "OCMS-4002", errorMessage);
        }

        return null; // Stage allowed
    }

    /**
     * Common database operation: Update VON table
     */
    protected void updateVON(OcmsValidOffenceNotice notice, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival) {

        LocalDateTime suspensionDate = LocalDateTime.now();

        notice.setSuspensionType(suspensionType);
        notice.setEprReasonOfSuspension(reasonOfSuspension);
        notice.setEprDateOfSuspension(suspensionDate);
        notice.setDueDateOfRevival(dueDateOfRevival);

        ocmsValidOffenceNoticeService.save(notice);
    }

    /**
     * Common database operation: Update eVON table (Internet/eService DB)
     * Syncs suspension data to public eService portal database
     */
    protected void updateEVON(String noticeNo, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival) {

        try {
            // Query eVON record
            Optional<EocmsValidOffenceNotice> eocmsNoticeOpt = eocmsValidOffenceNoticeService.getById(noticeNo);

            if (!eocmsNoticeOpt.isPresent()) {
                log.warn("eVON record not found for notice {}, skipping eVON sync", noticeNo);
                return;
            }

            // Update suspension fields
            EocmsValidOffenceNotice eocmsNotice = eocmsNoticeOpt.get();
            LocalDateTime suspensionDate = LocalDateTime.now();

            eocmsNotice.setSuspensionType(suspensionType);
            eocmsNotice.setEprReasonOfSuspension(reasonOfSuspension);
            eocmsNotice.setEprDateOfSuspension(suspensionDate);
            // Note: eVON does not have due_date_of_revival field

            // Save to eService database
            eocmsValidOffenceNoticeService.save(eocmsNotice);

            log.info("eVON synced for notice {}: type={}, reason={}", noticeNo, suspensionType, reasonOfSuspension);

        } catch (Exception e) {
            // Log but don't fail - eVON sync failure shouldn't block suspension
            log.error("Failed to update eVON for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Common database operation: Insert into suspended_notice table
     */
    protected void insertSuspendedNotice(OcmsValidOffenceNotice notice, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival, String suspensionSource,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {

        LocalDateTime suspensionDate = LocalDateTime.now();

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
    }

    /**
     * Common operation: Update all database tables in transaction
     */
    protected Map<String, Object> updateDatabase(OcmsValidOffenceNotice notice, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival, String suspensionSource,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {

        try {
            // Update VON
            updateVON(notice, suspensionType, reasonOfSuspension, dueDateOfRevival);

            // Update eVON (if available)
            updateEVON(notice.getNoticeNo(), suspensionType, reasonOfSuspension, dueDateOfRevival);

            // Insert suspended_notice
            insertSuspendedNotice(notice, suspensionType, reasonOfSuspension, dueDateOfRevival,
                suspensionSource, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo);

            return null; // Success

        } catch (Exception e) {
            log.error("Database update error for notice {}: {}", notice.getNoticeNo(), e.getMessage(), e);
            return createErrorResponse(notice.getNoticeNo(), "OCMS-4007", "System error. Please inform Administrator");
        }
    }

    /**
     * Option 3 Support: Create suspended_notice record ONLY (don't update VON/eVON)
     * Used when new TS has earlier revival date than existing TS
     */
    protected void createSuspendedNoticeOnly(OcmsValidOffenceNotice notice, String suspensionType,
            String reasonOfSuspension, LocalDateTime dueDateOfRevival, String suspensionSource,
            String suspensionRemarks, String officerAuthorisingSuspension, String srNo, String caseNo) {

        try {
            // Only insert suspended_notice (for audit trail)
            insertSuspendedNotice(notice, suspensionType, reasonOfSuspension, dueDateOfRevival,
                suspensionSource, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo);

            log.info("Created suspended_notice record only for notice {} (VON not updated)", notice.getNoticeNo());

        } catch (Exception e) {
            log.error("Failed to create suspended_notice for notice {}: {}", notice.getNoticeNo(), e.getMessage(), e);
            // Non-blocking: don't throw exception
        }
    }

    /**
     * Create error response
     */
    protected Map<String, Object> createErrorResponse(String noticeNo, String appCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }

    /**
     * Create success response with default OCMS-2000
     */
    protected Map<String, Object> createSuccessResponse(String noticeNo, String srNo, String message) {
        return createSuccessResponse(noticeNo, srNo, message, "OCMS-2000");
    }

    /**
     * Create success response with custom app code
     */
    protected Map<String, Object> createSuccessResponse(String noticeNo, String srNo, String message, String appCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("srNo", srNo);
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }
}
