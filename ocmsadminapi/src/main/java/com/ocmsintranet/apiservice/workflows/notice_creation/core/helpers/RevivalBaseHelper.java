package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for revival processing
 * Contains common validation and database operation logic shared between TS and PS revival
 *
 * Revival Process:
 * 1. Validate mandatory fields
 * 2. Query notice and find active suspensions
 * 3. Update suspended_notice table (set revival fields)
 * 4. Check for other active suspensions
 * 5. Update VON/eVON based on remaining active suspensions
 *
 * @author Claude Code
 * @since 2025-11-25 (OCMS 17 & 18)
 */
@Slf4j
public abstract class RevivalBaseHelper {

    @Autowired
    protected OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    @Autowired
    protected EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    @Autowired
    protected SuspendedNoticeService suspendedNoticeService;

    @Autowired
    protected NpdPatchingHelper npdPatchingHelper;

    // Source codes - from SystemConstant.Subsystem
    protected static final String SOURCE_CRON = SystemConstant.Subsystem.OCMS_CODE;
    protected static final String SOURCE_OCMS_STAFF = SystemConstant.Subsystem.OCMS_CODE; // Same as CRON
    protected static final String SOURCE_PLUS = SystemConstant.Subsystem.PLUS_CODE;

    /**
     * Get suspension type (TS or PS) - implemented by subclasses
     */
    protected abstract String getSuspensionType();

    /**
     * Find active suspensions after revival - implemented by subclasses
     * TS: Find TS with latest revival date
     * PS: Find PS with latest date_of_suspension
     */
    protected abstract Optional<SuspendedNotice> findRemainingActiveSuspension(String noticeNo);

    /**
     * Common validation: Check mandatory fields for revival
     */
    protected Map<String, Object> validateMandatoryFields(String noticeNo, String revivalReason,
            String officerAuthorisingRevival) {

        if (noticeNo == null || noticeNo.trim().isEmpty()) {
            return createErrorResponse("", "OCMS-4001", "Invalid Notice Number");
        }

        if (revivalReason == null || revivalReason.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Revival Reason is missing");
        }

        if (officerAuthorisingRevival == null || officerAuthorisingRevival.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4007", "Officer Authorising Revival is missing");
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
     * Common validation: Check if notice is currently suspended
     */
    protected Map<String, Object> checkSuspensionExists(String noticeNo, OcmsValidOffenceNotice notice,
            String suspensionType) {

        if (!suspensionType.equals(notice.getSuspensionType())) {
            return createErrorResponse(noticeNo, "OCMS-4005",
                "Notice is not currently " + suspensionType + " suspended");
        }

        return null; // Valid
    }

    /**
     * Common operation: Find all active suspensions for a notice
     * Active = date_of_revival IS NULL
     */
    protected List<SuspendedNotice> findActiveSuspensions(String noticeNo, String suspensionType) {
        try {
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});
            queryParams.put("suspensionType", new String[]{suspensionType});

            List<SuspendedNotice> allSuspensions = suspendedNoticeService.getAll(queryParams).getData();

            // Filter for active suspensions (date_of_revival IS NULL)
            return allSuspensions.stream()
                .filter(s -> s.getDateOfRevival() == null)
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding active suspensions for notice {}: {}", noticeNo, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Common database operation: Update suspended_notice table with revival fields
     */
    protected void updateSuspendedNoticeRevival(SuspendedNotice suspendedNotice, String revivalReason,
            String revivalRemarks, String officerAuthorisingRevival) {

        LocalDateTime revivalDate = LocalDateTime.now();

        suspendedNotice.setDateOfRevival(revivalDate);
        suspendedNotice.setRevivalReason(revivalReason);
        suspendedNotice.setRevivalRemarks(revivalRemarks);
        suspendedNotice.setOfficerAuthorisingRevival(officerAuthorisingRevival);

        suspendedNoticeService.save(suspendedNotice);
    }

    /**
     * Common database operation: Clear VON suspension fields
     */
    protected void clearVONSuspension(OcmsValidOffenceNotice notice) {
        notice.setSuspensionType(null);
        notice.setEprReasonOfSuspension(null);
        notice.setEprDateOfSuspension(null);
        notice.setDueDateOfRevival(null);

        ocmsValidOffenceNoticeService.save(notice);
    }

    /**
     * Common database operation: Update VON with new active suspension
     */
    protected void updateVONWithActiveSuspension(OcmsValidOffenceNotice notice, SuspendedNotice activeSuspension) {
        notice.setSuspensionType(activeSuspension.getSuspensionType());
        notice.setEprReasonOfSuspension(activeSuspension.getReasonOfSuspension());
        notice.setEprDateOfSuspension(activeSuspension.getDateOfSuspension());
        notice.setDueDateOfRevival(activeSuspension.getDueDateOfRevival());

        ocmsValidOffenceNoticeService.save(notice);
    }

    /**
     * Common database operation: Update eVON table (Internet/eService DB)
     * Syncs suspension data to public eService portal database
     */
    protected void updateEVON(String noticeNo, String suspensionType,
            String reasonOfSuspension, LocalDateTime dateOfSuspension, LocalDateTime dueDateOfRevival) {

        try {
            // Query eVON record
            Optional<EocmsValidOffenceNotice> eocmsNoticeOpt = eocmsValidOffenceNoticeService.getById(noticeNo);

            if (!eocmsNoticeOpt.isPresent()) {
                log.warn("eVON record not found for notice {}, skipping eVON sync", noticeNo);
                return;
            }

            // Update suspension fields
            EocmsValidOffenceNotice eocmsNotice = eocmsNoticeOpt.get();

            eocmsNotice.setSuspensionType(suspensionType);
            eocmsNotice.setEprReasonOfSuspension(reasonOfSuspension);
            eocmsNotice.setEprDateOfSuspension(dateOfSuspension);
            // Note: eVON does not have due_date_of_revival field

            // Save to eService database
            eocmsValidOffenceNoticeService.save(eocmsNotice);

            log.info("eVON synced for notice {}: type={}, reason={}", noticeNo, suspensionType, reasonOfSuspension);

        } catch (Exception e) {
            // Log but don't fail - eVON sync failure shouldn't block revival
            log.error("Failed to update eVON for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Common database operation: Clear eVON suspension fields
     */
    protected void clearEVONSuspension(String noticeNo) {
        updateEVON(noticeNo, null, null, null, null);
    }

    /**
     * Common operation: Update all database tables after revival
     *
     * Logic:
     * 1. Update suspended_notice (set revival fields)
     * 2. Check for other active suspensions
     * 3a. If other active suspensions exist → Update VON/eVON with latest one
     * 3b. If no other suspensions → Clear VON/eVON suspension fields
     * 4. Patch NPD if required (OCMS 19 - NPD +2 days for DataHive)
     */
    protected Map<String, Object> updateDatabaseAfterRevival(String noticeNo, SuspendedNotice currentSuspension,
            String revivalReason, String revivalRemarks, String officerAuthorisingRevival) {

        try {
            // Step 1: Update suspended_notice with revival fields
            updateSuspendedNoticeRevival(currentSuspension, revivalReason, revivalRemarks, officerAuthorisingRevival);

            // Step 2: Find other active suspensions (after this one is revived)
            Optional<SuspendedNotice> remainingSuspensionOpt = findRemainingActiveSuspension(noticeNo);

            // Step 3: Update VON/eVON based on remaining suspensions
            Optional<OcmsValidOffenceNotice> noticeOpt = queryNotice(noticeNo);
            if (!noticeOpt.isPresent()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Notice not found after revival");
            }
            OcmsValidOffenceNotice notice = noticeOpt.get();

            if (remainingSuspensionOpt.isPresent()) {
                // Other active suspension exists - update VON/eVON with it
                SuspendedNotice remainingSuspension = remainingSuspensionOpt.get();

                updateVONWithActiveSuspension(notice, remainingSuspension);
                updateEVON(noticeNo,
                    remainingSuspension.getSuspensionType(),
                    remainingSuspension.getReasonOfSuspension(),
                    remainingSuspension.getDateOfSuspension(),
                    remainingSuspension.getDueDateOfRevival());

                log.info("Notice {} revived: remaining {} suspension applied (reason: {})",
                    noticeNo, remainingSuspension.getSuspensionType(), remainingSuspension.getReasonOfSuspension());

            } else {
                // No other active suspensions - clear VON/eVON
                clearVONSuspension(notice);
                clearEVONSuspension(noticeNo);

                log.info("Notice {} fully revived: all suspensions cleared", noticeNo);
            }

            // Step 4: Patch NPD +2 days if required (OCMS 19 specification)
            // Apply NPD patching based on the suspension code that was just revived
            String suspensionType = currentSuspension.getSuspensionType();
            String suspensionCode = currentSuspension.getReasonOfSuspension();

            boolean npdPatched = npdPatchingHelper.patchNpdIfRequired(notice, suspensionType, suspensionCode);
            if (npdPatched) {
                // Save notice with patched NPD
                ocmsValidOffenceNoticeService.save(notice);
                log.info("Notice {} NPD patched after revival of {}-{}", noticeNo, suspensionType, suspensionCode);
            }

            return null; // Success

        } catch (Exception e) {
            log.error("Database update error during revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-4007", "System error. Please inform Administrator");
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
     * Create success response
     */
    protected Map<String, Object> createSuccessResponse(String noticeNo, String message) {
        return createSuccessResponse(noticeNo, message, "OCMS-2000");
    }

    /**
     * Create success response with custom app code
     */
    protected Map<String, Object> createSuccessResponse(String noticeNo, String message, String appCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }
}
