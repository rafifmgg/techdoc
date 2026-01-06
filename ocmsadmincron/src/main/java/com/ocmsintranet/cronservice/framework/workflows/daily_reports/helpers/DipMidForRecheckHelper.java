package com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCMS 19 - FB-1: DIP/MID/FOR Day-End Re-check Helper
 *
 * Purpose: Prevent diplomatic/military/foreign vehicles from progressing to next stage without proper suspension
 *
 * Business Logic:
 * - Query notices at RD2/DN2 stage with no active PS
 * - Check if vehicle_registration_type is D (Diplomatic), I (Military/MID), or F (Foreign)
 * - Re-apply PS-DIP / PS-MID / PS-FOR if accidentally revived during the day
 * - Runs daily at 11:59 PM (end of day) before stage transition cron
 *
 * Specification Reference:
 * - v2.0_OCMS_19_Revive_Suspensions_Feedback.md Row 2 "FB-1: Modify Prepare for Next Stage cron"
 *
 * @author Claude Code
 * @since 2025-12-21 (OCMS 19)
 */
@Slf4j
@Component
public class DipMidForRecheckHelper {

    private final TableQueryService tableQueryService;
    private final SuspensionApiClient suspensionApiClient;

    // Vehicle registration types requiring PS
    private static final String VEH_REG_TYPE_DIPLOMATIC = "D";  // Diplomatic vehicles → PS-DIP
    private static final String VEH_REG_TYPE_MILITARY = "I";    // Military/MID vehicles → PS-MID
    private static final String VEH_REG_TYPE_FOREIGN = "F";     // Foreign vehicles → PS-FOR

    // PS suspension codes
    private static final String PS_CODE_DIP = "DIP";  // Diplomatic Vehicle
    private static final String PS_CODE_MID = "MID";  // Military ID
    private static final String PS_CODE_FOR = "FOR";  // Foreign Vehicle

    // Processing stages where re-check is required (before transition to RR3/DR3)
    private static final String STAGE_RD2 = "RD2";  // Registration Department Stage 2
    private static final String STAGE_DN2 = "DN2";  // Delivery Note Stage 2

    public DipMidForRecheckHelper(
            TableQueryService tableQueryService,
            SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.suspensionApiClient = suspensionApiClient;
    }

    /**
     * Query notices at RD2/DN2 stage requiring DIP/MID/FOR re-check
     *
     * Query Logic:
     * - current_processing_stage IN ('RD2', 'DN2')
     * - vehicle_registration_type IN ('D', 'I', 'F')
     * - No active PS suspension (PS accidentally revived)
     *
     * @return List of notice records needing re-suspension
     */
    public List<Map<String, Object>> queryNoticesForRecheck() {
        try {
            log.info("[DIP/MID/FOR Recheck] Querying notices at RD2/DN2 stage for day-end re-check");

            // Build filters for table query
            Map<String, Object> filters = new HashMap<>();

            // Filter 1: Processing stage is RD2 or DN2
            filters.put("currentProcessingStage[$in]", new String[]{STAGE_RD2, STAGE_DN2});

            // Filter 2: Vehicle type is Diplomatic, Military, or Foreign
            filters.put("vehicleRegistrationType[$in]", new String[]{
                VEH_REG_TYPE_DIPLOMATIC,
                VEH_REG_TYPE_MILITARY,
                VEH_REG_TYPE_FOREIGN
            });

            log.debug("[DIP/MID/FOR Recheck] Query filters: {}", filters);

            List<Map<String, Object>> potentialNotices = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (potentialNotices.isEmpty()) {
                log.info("[DIP/MID/FOR Recheck] No notices found at RD2/DN2 stage with DIP/MID/FOR vehicle types");
                return new ArrayList<>();
            }

            log.info("[DIP/MID/FOR Recheck] Found {} potential notices at RD2/DN2 stage with DIP/MID/FOR vehicle types",
                    potentialNotices.size());

            // Filter out notices that already have active PS
            List<Map<String, Object>> noticesNeedingRecheck = new ArrayList<>();
            for (Map<String, Object> notice : potentialNotices) {
                String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));

                if (!hasActivePS(noticeNo)) {
                    noticesNeedingRecheck.add(notice);
                    log.debug("[DIP/MID/FOR Recheck] Notice {} needs PS re-application (no active PS found)", noticeNo);
                } else {
                    log.debug("[DIP/MID/FOR Recheck] Notice {} already has active PS, skipping", noticeNo);
                }
            }

            log.info("[DIP/MID/FOR Recheck] {} notices need PS re-application (out of {} potential)",
                    noticesNeedingRecheck.size(), potentialNotices.size());

            return noticesNeedingRecheck;

        } catch (Exception e) {
            log.error("[DIP/MID/FOR Recheck] Error querying notices for recheck: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query notices for DIP/MID/FOR recheck", e);
        }
    }

    /**
     * Check if notice has active PS suspension
     *
     * @param noticeNo The notice number
     * @return true if active PS exists, false otherwise
     */
    private boolean hasActivePS(String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            filters.put("suspensionType", SystemConstant.SuspensionType.PERMANENT);
            filters.put("dateOfRevival[$null]", true); // Not yet revived

            List<Map<String, Object>> activePSList = tableQueryService.query("ocms_suspended_notice", filters);

            return !activePSList.isEmpty();

        } catch (Exception e) {
            log.error("[DIP/MID/FOR Recheck] Error checking active PS for notice {}: {}", noticeNo, e.getMessage(), e);
            // Conservative approach: assume PS exists to avoid unnecessary re-application
            return true;
        }
    }

    /**
     * Re-apply PS suspension for a single notice
     *
     * Determines PS code based on vehicle_registration_type:
     * - D (Diplomatic) → PS-DIP
     * - I (Military/MID) → PS-MID
     * - F (Foreign) → PS-FOR
     *
     * @param notice The notice record from query
     * @return true if PS was re-applied successfully, false otherwise
     */
    public boolean reapplyPS(Map<String, Object> notice) {
        try {
            String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));
            String vehRegType = (String) notice.getOrDefault("vehicleRegistrationType", notice.get("vehicle_registration_type"));

            if (noticeNo == null || vehRegType == null) {
                log.error("[DIP/MID/FOR Recheck] Missing notice_no or vehicle_registration_type in record: {}", notice);
                return false;
            }

            // Determine PS code based on vehicle registration type
            String psCode = determinePSCode(vehRegType);

            if (psCode == null) {
                log.error("[DIP/MID/FOR Recheck] Unknown vehicle registration type '{}' for notice {}", vehRegType, noticeNo);
                return false;
            }

            log.info("[DIP/MID/FOR Recheck] Re-applying PS-{} to notice {} (vehicle type: {})",
                    psCode, noticeNo, vehRegType);

            // Re-apply PS via SuspensionApiClient
            Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                    noticeNo,
                    SystemConstant.SuspensionType.PERMANENT,
                    psCode,
                    "Day-end re-check: Re-applied PS-" + psCode + " for " + getVehicleTypeName(vehRegType) + " vehicle",
                    SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                    SystemConstant.Subsystem.OCMS_CODE,
                    null,  // caseNo - not applicable
                    null   // daysToRevive - not applicable for PS
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("[DIP/MID/FOR Recheck] Successfully re-applied PS-{} to notice {}", psCode, noticeNo);
                return true;
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                log.error("[DIP/MID/FOR Recheck] API returned error for notice {}: {}", noticeNo, errorMsg);
                return false;
            }

        } catch (Exception e) {
            String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));
            log.error("[DIP/MID/FOR Recheck] Error re-applying PS for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determine PS suspension code based on vehicle registration type
     *
     * @param vehRegType Vehicle registration type (D, I, or F)
     * @return PS code (DIP, MID, or FOR), or null if unknown
     */
    private String determinePSCode(String vehRegType) {
        if (VEH_REG_TYPE_DIPLOMATIC.equals(vehRegType)) {
            return PS_CODE_DIP;
        } else if (VEH_REG_TYPE_MILITARY.equals(vehRegType)) {
            return PS_CODE_MID;
        } else if (VEH_REG_TYPE_FOREIGN.equals(vehRegType)) {
            return PS_CODE_FOR;
        }
        return null;
    }

    /**
     * Get human-readable vehicle type name
     *
     * @param vehRegType Vehicle registration type code
     * @return Human-readable name
     */
    private String getVehicleTypeName(String vehRegType) {
        if (VEH_REG_TYPE_DIPLOMATIC.equals(vehRegType)) {
            return "Diplomatic";
        } else if (VEH_REG_TYPE_MILITARY.equals(vehRegType)) {
            return "Military";
        } else if (VEH_REG_TYPE_FOREIGN.equals(vehRegType)) {
            return "Foreign";
        }
        return "Unknown";
    }

    /**
     * Process batch re-application of PS suspensions
     *
     * Continues processing even if individual re-applications fail
     * Returns summary of successes and failures
     *
     * @param notices List of notice records to process
     * @return Map with "successCount" and "failureCount"
     */
    public Map<String, Integer> processBatchReapplication(List<Map<String, Object>> notices) {
        Map<String, Integer> summary = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        log.info("[DIP/MID/FOR Recheck] Processing batch re-application for {} notices", notices.size());

        for (Map<String, Object> notice : notices) {
            try {
                boolean success = reapplyPS(notice);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));
                log.error("[DIP/MID/FOR Recheck] Exception processing notice {}: {}", noticeNo, e.getMessage(), e);
                failureCount++;
            }
        }

        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);

        log.info("[DIP/MID/FOR Recheck] Batch re-application completed: {} successful, {} failed",
                successCount, failureCount);

        return summary;
    }
}
