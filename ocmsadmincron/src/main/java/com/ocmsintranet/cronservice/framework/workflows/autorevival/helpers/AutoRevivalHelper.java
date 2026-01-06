package com.ocmsintranet.cronservice.framework.workflows.autorevival.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Auto-Revival workflow.
 *
 * This helper contains business logic for:
 * - Querying suspended notices with due revivals
 * - Processing revivals via SuspensionApiClient (API)
 * - No direct DB access - all operations through utilities
 *
 * Revival Logic:
 * - Process ALL notices where due_date_of_revival <= TODAY
 * - Only process TS (Temporary Suspension) types
 * - Only process notices where date_of_revival IS NULL (not yet revived)
 * - Use SuspensionApiClient.applyRevival() to call API
 */
@Slf4j
@Component
public class AutoRevivalHelper {

    private final TableQueryService tableQueryService;
    private final SuspensionApiClient suspensionApiClient;

    public AutoRevivalHelper(
            TableQueryService tableQueryService,
            SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.suspensionApiClient = suspensionApiClient;
    }

    /**
     * Query suspended notices that need revival
     *
     * Query Logic:
     * - due_date_of_revival <= TODAY (includes overdue)
     * - date_of_revival IS NULL (not yet revived)
     * - suspension_type = 'TS' (temporary suspensions only)
     *
     * Uses TableQueryService (NOT direct SQL) following framework pattern
     *
     * @return List of suspended notice records needing revival
     */
    public List<Map<String, Object>> queryNoticesForRevival() {
        try {
            log.info("Querying suspended notices for auto-revival");

            // Build filters using TableQueryService pattern
            Map<String, Object> filters = new HashMap<>();

            // Filter 1: Only temporary suspensions
            filters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);

            // Filter 2: Not yet revived (date_of_revival is NULL)
            filters.put("dateOfRevival[$null]", true);

            // Filter 3: Due date is today or earlier (overdue)
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            filters.put("dueDateOfRevival[$lte]", todayStr);

            log.debug("Query filters: {}", filters);

            List<Map<String, Object>> results = tableQueryService.query("ocms_suspended_notice", filters);

            log.info("Found {} suspended notices needing revival", results.size());

            return results;

        } catch (Exception e) {
            log.error("Error querying notices for revival: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query notices for revival", e);
        }
    }

    /**
     * Process a single revival via API
     *
     * Uses SuspensionApiClient.applyRevivalSingle() to call APIM → API → Helper → DB
     * Does NOT access database directly - follows architecture pattern
     *
     * @param record The suspended notice record from query
     * @return true if revival was successful, false otherwise
     */
    public boolean processRevival(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            if (noticeNo == null) {
                noticeNo = (String) record.get("notice_no");
            }

            if (noticeNo == null) {
                log.error("No notice_no found in record: {}", record);
                return false;
            }

            log.info("Processing auto-revival for notice: {}", noticeNo);

            // Call revival API (through APIM)
            Map<String, Object> apiResponse = suspensionApiClient.applyRevivalSingle(
                noticeNo,
                "Auto-revived on due date", // revivalRemarks
                SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingRevival
                SystemConstant.Subsystem.OCMS_CODE // revivalSource (004)
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("Successfully applied auto-revival for notice {}", noticeNo);
                return true;
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                log.error("API returned error for notice {}: {}", noticeNo, errorMsg);
                return false;
            }

        } catch (Exception e) {
            String noticeNo = (String) record.getOrDefault("noticeNo", record.get("notice_no"));
            log.error("Error processing revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process multiple revivals in batch
     *
     * Continues processing even if individual revivals fail
     * Returns summary of successes and failures
     *
     * @param records List of suspended notice records to revive
     * @return Map with "successCount" and "failureCount"
     */
    public Map<String, Integer> processBatchRevivals(List<Map<String, Object>> records) {
        Map<String, Integer> summary = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        log.info("Processing batch revivals for {} notices", records.size());

        for (Map<String, Object> record : records) {
            try {
                boolean success = processRevival(record);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                String noticeNo = (String) record.getOrDefault("noticeNo", record.get("notice_no"));
                log.error("Exception processing revival for notice {}: {}", noticeNo, e.getMessage(), e);
                failureCount++;
            }
        }

        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);

        log.info("Batch revival completed: {} successful, {} failed", successCount, failureCount);

        return summary;
    }

    /**
     * Process TS-OLD auto-revival for VIP vehicles
     *
     * TS-OLD is applied to VIP notices immediately after creation (21-day investigation period)
     * After 21 days, this method:
     * 1. Auto-revives the TS-OLD suspension
     * 2. Triggers LTA vehicle ownership check (handled by LtaUploadJob in separate task)
     * 3. Notice proceeds to RD1 via normal stage transition
     *
     * @return Map with "successCount" and "failureCount"
     */
    public Map<String, Integer> processTsOldRevival() {
        Map<String, Integer> summary = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        try {
            log.info("Processing TS-OLD auto-revival for VIP vehicles");

            // Query TS-OLD suspensions due for revival
            Map<String, Object> filters = new HashMap<>();
            filters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            filters.put("reasonOfSuspension", SystemConstant.SuspensionReason.OLD);
            filters.put("dateOfRevival[$null]", true);

            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            filters.put("dueDateOfRevival[$lte]", todayStr);

            List<Map<String, Object>> tsOldRecords = tableQueryService.query("ocms_suspended_notice", filters);

            if (tsOldRecords.isEmpty()) {
                log.info("No TS-OLD suspensions found needing revival");
                summary.put("successCount", 0);
                summary.put("failureCount", 0);
                return summary;
            }

            log.info("Found {} TS-OLD suspensions needing revival", tsOldRecords.size());

            // Process each TS-OLD revival
            for (Map<String, Object> record : tsOldRecords) {
                try {
                    String noticeNo = (String) record.getOrDefault("noticeNo", record.get("notice_no"));

                    // Check if this is a VIP vehicle
                    if (!isVipVehicle(noticeNo)) {
                        log.warn("TS-OLD found for non-VIP notice {}, skipping", noticeNo);
                        continue;
                    }

                    log.info("Auto-reviving TS-OLD for VIP notice {} after 21 days", noticeNo);

                    // Revive TS-OLD via API
                    Map<String, Object> apiResponse = suspensionApiClient.applyRevivalSingle(
                        noticeNo,
                        "Auto-revived after TS-OLD period ended", // revivalRemarks
                        SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingRevival
                        SystemConstant.Subsystem.OCMS_CODE // revivalSource
                    );

                    if (suspensionApiClient.isSuccess(apiResponse)) {
                        log.info("Successfully auto-revived TS-OLD for VIP notice {}", noticeNo);
                        successCount++;

                        // Note: LTA vehicle ownership check will be triggered by LtaUploadJob
                        // (implemented in separate task #16)
                    } else {
                        String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                        log.error("Failed to revive TS-OLD for notice {}: {}", noticeNo, errorMsg);
                        failureCount++;
                    }

                } catch (Exception e) {
                    String noticeNo = (String) record.getOrDefault("noticeNo", record.get("notice_no"));
                    log.error("Error processing TS-OLD revival for notice {}: {}", noticeNo, e.getMessage(), e);
                    failureCount++;
                }
            }

        } catch (Exception e) {
            log.error("Error in TS-OLD revival processing: {}", e.getMessage(), e);
        }

        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);

        log.info("TS-OLD revival completed: {} successful, {} failed", successCount, failureCount);

        return summary;
    }

    /**
     * Process TS-CLV looping for VIP vehicles at RR3/DR3
     *
     * TS-CLV (VIP Clearance) looping logic:
     * - VIP notices at RR3/DR3 stages that are unpaid
     * - If no TS-CLV → Apply TS-CLV
     * - If TS-CLV was revived and another TS applied → Re-apply TS-CLV (looping)
     * - If payment received → Stop looping (handled by payment workflow)
     *
     * @return Map with "successCount" and "failureCount"
     */
    public Map<String, Integer> processTsClvLooping() {
        Map<String, Integer> summary = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        try {
            log.info("Processing TS-CLV looping for VIP vehicles at RR3/DR3");

            // Query VIP notices at RR3 or DR3 stages
            Map<String, Object> filters = new HashMap<>();
            filters.put("nextProcessingStage[$in]", new String[]{"RR3", "DR3"});

            List<Map<String, Object>> vipNotices = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (vipNotices.isEmpty()) {
                log.info("No notices found at RR3/DR3 stages");
                summary.put("successCount", 0);
                summary.put("failureCount", 0);
                return summary;
            }

            log.info("Found {} notices at RR3/DR3 stages", vipNotices.size());

            // Process each notice
            for (Map<String, Object> notice : vipNotices) {
                try {
                    String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));
                    String paymentStatus = (String) notice.get("paymentStatus");

                    // Check if VIP vehicle
                    if (!isVipVehicle(noticeNo)) {
                        continue;
                    }

                    // Check if notice is paid (skip if paid)
                    if ("FP".equals(paymentStatus)) {
                        log.debug("VIP notice {} is paid, skipping TS-CLV looping", noticeNo);
                        continue;
                    }

                    // Check current suspension status
                    Map<String, Object> currentSuspension = getCurrentSuspension(noticeNo);

                    if (currentSuspension == null || !"CLV".equals(currentSuspension.get("reasonOfSuspension"))) {
                        // No TS-CLV currently → Apply TS-CLV
                        log.info("Applying TS-CLV looping suspension to VIP notice {}", noticeNo);
                        applyTsClvSuspension(noticeNo);
                        successCount++;
                    } else {
                        // TS-CLV already applied → Check if it was revived and needs reapplication
                        boolean needsReapplication = checkIfTsClvNeedsReapplication(noticeNo);

                        if (needsReapplication) {
                            log.info("Re-applying TS-CLV looping suspension to VIP notice {} (was revived)", noticeNo);
                            applyTsClvSuspension(noticeNo);
                            successCount++;
                        }
                    }

                } catch (Exception e) {
                    String noticeNo = (String) notice.getOrDefault("noticeNo", notice.get("notice_no"));
                    log.error("Error processing TS-CLV looping for notice {}: {}", noticeNo, e.getMessage(), e);
                    failureCount++;
                }
            }

        } catch (Exception e) {
            log.error("Error in TS-CLV looping processing: {}", e.getMessage(), e);
        }

        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);

        log.info("TS-CLV looping completed: {} successful, {} failed", successCount, failureCount);

        return summary;
    }

    /**
     * Check if notice is for a VIP vehicle
     *
     * @param noticeNo The notice number
     * @return true if vehicle_registration_type = 'V', false otherwise
     */
    private boolean isVipVehicle(String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);

            List<Map<String, Object>> notices = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (notices.isEmpty()) {
                log.warn("Notice {} not found", noticeNo);
                return false;
            }

            String vehicleRegType = (String) notices.get(0).get("vehicleRegistrationType");
            return "V".equals(vehicleRegType);

        } catch (Exception e) {
            log.error("Error checking VIP status for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current active suspension for a notice
     *
     * @param noticeNo The notice number
     * @return Current suspension record, or null if none
     */
    private Map<String, Object> getCurrentSuspension(String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            filters.put("dateOfRevival[$null]", true); // Not yet revived

            List<Map<String, Object>> suspensions = tableQueryService.query("ocms_suspended_notice", filters);

            if (suspensions.isEmpty()) {
                return null;
            }

            // Return the most recent suspension (highest sr_no)
            Map<String, Object> mostRecent = suspensions.get(0);
            int maxSrNo = getIntValue(mostRecent, "srNo");

            for (Map<String, Object> suspension : suspensions) {
                int srNo = getIntValue(suspension, "srNo");
                if (srNo > maxSrNo) {
                    maxSrNo = srNo;
                    mostRecent = suspension;
                }
            }

            return mostRecent;

        } catch (Exception e) {
            log.error("Error getting current suspension for notice {}: {}", noticeNo, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if TS-CLV needs reapplication (looping logic)
     *
     * Logic: TS-CLV was revived AND another TS is now active
     *
     * @param noticeNo The notice number
     * @return true if TS-CLV should be re-applied
     */
    private boolean checkIfTsClvNeedsReapplication(String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);

            List<Map<String, Object>> allSuspensions = tableQueryService.query("ocms_suspended_notice", filters);

            // Check if TS-CLV was previously revived
            boolean tsClvWasRevived = allSuspensions.stream()
                    .anyMatch(s -> "CLV".equals(s.get("reasonOfSuspension")) && s.get("dateOfRevival") != null);

            if (!tsClvWasRevived) {
                return false;
            }

            // Check if there's currently an active non-CLV TS
            boolean hasActiveNonClvTs = allSuspensions.stream()
                    .anyMatch(s -> SystemConstant.SuspensionType.TEMPORARY.equals(s.get("suspensionType")) &&
                                  !"CLV".equals(s.get("reasonOfSuspension")) &&
                                  s.get("dateOfRevival") == null);

            return hasActiveNonClvTs;

        } catch (Exception e) {
            log.error("Error checking TS-CLV reapplication for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Apply TS-CLV suspension via API
     *
     * @param noticeNo The notice number
     */
    private void applyTsClvSuspension(String noticeNo) {
        try {
            // TODO: Call SuspensionApiClient to apply TS-CLV
            // For now, just log - actual implementation depends on API structure
            log.info("Would apply TS-CLV suspension to notice {} (API integration pending)", noticeNo);

        } catch (Exception e) {
            log.error("Error applying TS-CLV to notice {}: {}", noticeNo, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Helper to get integer value from map
     */
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
