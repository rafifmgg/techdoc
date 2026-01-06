package com.ocmsintranet.apiservice.workflows.notice_management.suspension.revivenotice.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Helper class for processing revival of suspended notices (TS only for now)
 * Handles PLUS TS revival flow
 */
@Component
@Slf4j
public class ReviveProcessingHelper {

    private final SuspendedNoticeService suspendedNoticeService;
    private final OcmsValidOffenceNoticeService validOffenceNoticeService;

    // TS code that is NOT allowed to be revived
    private static final String BLOCKED_TS_CODE = "HST";

    // Source code for PLUS
    private static final String SOURCE_PLUS = "005";

    public ReviveProcessingHelper(
            SuspendedNoticeService suspendedNoticeService,
            OcmsValidOffenceNoticeService validOffenceNoticeService) {
        this.suspendedNoticeService = suspendedNoticeService;
        this.validOffenceNoticeService = validOffenceNoticeService;
    }

    /**
     * Validate revival request for PLUS
     *
     * @param noticeNo Notice number
     * @param revivalReason Revival reason code
     * @param revivedBy Officer authorising revival
     * @param revivalRemarks Revival remarks (optional)
     * @return Map with validation result (null if valid, error map if invalid)
     */
    public Map<String, Object> validateRevival(
            String noticeNo,
            String revivalReason,
            String revivedBy,
            String revivalRemarks) {

        log.info("Validating revival for notice: {}, reason: {}, revivedBy: {}",
                noticeNo, revivalReason, revivedBy);

        // Step 1: Validate mandatory fields
        if (noticeNo == null || noticeNo.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4000", "noticeNo is required");
        }

        if (revivalReason == null || revivalReason.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4000", "revivalReason is required");
        }

        if (revivedBy == null || revivedBy.trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4000", "revivedBy is required");
        }

        // Step 2: Validate revivalRemarks length (max 50 chars)
        if (revivalRemarks != null && revivalRemarks.length() > 50) {
            return createErrorResponse(noticeNo, "OCMS-4000",
                "revivalRemarks cannot exceed 50 characters");
        }

        // Step 3: Check if notice exists
        Optional<OcmsValidOffenceNotice> noticeOpt = validOffenceNoticeService.getById(noticeNo);
        if (noticeOpt.isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4001", "Notice not found");
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();

        // Step 4: Check if notice has active suspension
        if (notice.getSuspensionType() == null || notice.getSuspensionType().trim().isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4002", "Notice is not currently suspended");
        }

        // Step 5: Check if suspension is PS - reject PS revival (not ready yet)
        if ("PS".equals(notice.getSuspensionType())) {
            return createErrorResponse(noticeNo, "OCMS-4003",
                "PS revival is not supported yet. Please contact system administrator.");
        }

        // Step 6: Check if suspension is TS
        if (!"TS".equals(notice.getSuspensionType())) {
            return createErrorResponse(noticeNo, "OCMS-4004",
                "Unknown suspension type: " + notice.getSuspensionType());
        }

        // Step 7: Get suspended notice records
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put("noticeNo", new String[]{noticeNo});
        List<SuspendedNotice> suspendedNotices = suspendedNoticeService.getAll(queryParams).getData();

        if (suspendedNotices.isEmpty()) {
            return createErrorResponse(noticeNo, "OCMS-4005",
                "No suspended notice records found");
        }

        // Step 8: Find active TS (date_of_revival is null)
        SuspendedNotice activeTS = null;
        for (SuspendedNotice sn : suspendedNotices) {
            if ("TS".equals(sn.getSuspensionType()) && sn.getDateOfRevival() == null) {
                activeTS = sn;
                break;
            }
        }

        if (activeTS == null) {
            return createErrorResponse(noticeNo, "OCMS-4006",
                "No active TS suspension found for this notice");
        }

        // Step 9: Check if TS code is HST (not allowed to revive)
        String reasonOfSuspension = activeTS.getReasonOfSuspension();
        if (BLOCKED_TS_CODE.equals(reasonOfSuspension)) {
            return createErrorResponse(noticeNo, "OCMS-4007",
                "Revival is not allowed for TS-HST notices");
        }

        // Step 10: All validations passed
        log.info("Validation passed for notice: {}", noticeNo);
        return null;
    }

    /**
     * Process revival for PLUS TS suspension
     *
     * @param noticeNo Notice number
     * @param revivalReason Revival reason code
     * @param revivedBy Officer authorising revival
     * @param revivalRemarks Revival remarks (optional)
     * @param caseNo Case number from PLUS (optional)
     * @return Map with processing result
     */
    public Map<String, Object> processRevival(
            String noticeNo,
            String revivalReason,
            String revivedBy,
            String revivalRemarks,
            String caseNo) {

        log.info("Processing revival for notice: {}, caseNo: {}", noticeNo, caseNo);

        try {
            // Step 1: Get the notice
            Optional<OcmsValidOffenceNotice> noticeOpt = validOffenceNoticeService.getById(noticeNo);
            if (noticeOpt.isEmpty()) {
                return createErrorResponse(noticeNo, "OCMS-4001", "Notice not found");
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Step 2: Get active TS suspension
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});
            List<SuspendedNotice> suspendedNotices = suspendedNoticeService.getAll(queryParams).getData();

            SuspendedNotice activeTS = null;
            for (SuspendedNotice sn : suspendedNotices) {
                if ("TS".equals(sn.getSuspensionType()) && sn.getDateOfRevival() == null) {
                    activeTS = sn;
                    break;
                }
            }

            if (activeTS == null) {
                return createErrorResponse(noticeNo, "OCMS-4006",
                    "No active TS suspension found");
            }

            // Step 3: Update suspended notice with revival information
            activeTS.setDateOfRevival(LocalDateTime.now());
            activeTS.setRevivalReason(revivalReason);
            activeTS.setOfficerAuthorisingRevival(revivedBy);
            activeTS.setRevivalRemarks(revivalRemarks);

            // Update case number if provided
            if (caseNo != null && !caseNo.trim().isEmpty()) {
                activeTS.setCaseNo(caseNo);
            }

            suspendedNoticeService.save(activeTS);
            log.info("Updated suspended notice for revival: noticeNo={}, srNo={}", noticeNo, activeTS.getSrNo());

            // Step 4: Update valid offence notice
            notice.setSuspensionType(null);
            notice.setEprReasonOfSuspension(null);
            notice.setDueDateOfRevival(null);
            notice.setEprDateOfSuspension(null);

            // Set next processing date = current date + 2 days
            notice.setNextProcessingDate(LocalDateTime.now().plusDays(2));

            // Set updater
            notice.setUpdUserId(revivedBy);
            notice.setUpdDate(LocalDateTime.now());

            validOffenceNoticeService.save(notice);
            log.info("Updated valid offence notice for revival: noticeNo={}", noticeNo);

            // Step 5: Return success response
            return createSuccessResponse(noticeNo, "Revive Success");

        } catch (Exception e) {
            log.error("Error processing revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse(noticeNo, "OCMS-5000",
                "Something went wrong on our end. Please try again later.");
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
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String noticeNo, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("noticeNo", noticeNo);
        response.put("message", message);
        response.put("appCode", "OCMS-2000");
        return response;
    }
}
