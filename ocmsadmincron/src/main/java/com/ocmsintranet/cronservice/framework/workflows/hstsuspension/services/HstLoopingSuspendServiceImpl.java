package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of HstLoopingSuspendService
 * OCMS 20: Re-apply TS-HST suspension when revival date approaches (indefinite looping)
 *
 * Process Flow:
 * 1. Query ocms_valid_offence_notice for TS-HST suspensions approaching revival
 * 2. Check if address is still invalid in ocms_temp_unc_hst_addr
 * 3. Re-apply TS-HST suspension if address remains invalid
 * 4. Loop continues until address becomes valid
 */
@Slf4j
@Service
public class HstLoopingSuspendServiceImpl implements HstLoopingSuspendService {

    private final TableQueryService tableQueryService;
    private final SuspensionApiClient suspensionApiClient;

    @Value("${cron.hst.looping.days.before.revival:7}")
    private int daysBeforeRevival;

    @Autowired
    public HstLoopingSuspendServiceImpl(
            TableQueryService tableQueryService,
            SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.suspensionApiClient = suspensionApiClient;
    }

    @Override
    @Async
    public CompletableFuture<SuspensionResult> executeLoopingJob() {
        log.info("[TS-HST-LOOP] Starting looping suspension job for TS-HST notices approaching revival");

        try {
            // Calculate target date range (e.g., 7 days from now)
            LocalDate checkDate = LocalDate.now().plusDays(daysBeforeRevival);
            String formattedCheckDate = checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

            log.info("[TS-HST-LOOP] Checking for TS-HST suspensions with revival date on or before: {}",
                    formattedCheckDate);

            // Query for TS-HST suspensions approaching revival
            Map<String, Object> vonFilter = new HashMap<>();
            vonFilter.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            vonFilter.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.HST);
            vonFilter.put("dueDateOfRevival.lte", formattedCheckDate);
            vonFilter.put("$limit", 10000);

            List<Map<String, Object>> vonRecords = tableQueryService.query(
                    "ocms_valid_offence_notice", vonFilter);

            log.info("[TS-HST-LOOP] Found {} TS-HST suspensions approaching revival", vonRecords.size());

            int totalProcessed = 0;
            int reapplied = 0;
            int skipped = 0;
            int errors = 0;

            // Process each notice
            for (Map<String, Object> vonRecord : vonRecords) {
                try {
                    String noticeNo = (String) vonRecord.get("noticeNo");

                    if (noticeNo == null || noticeNo.isEmpty()) {
                        log.warn("[TS-HST-LOOP] Skipping VON record with empty noticeNo");
                        continue;
                    }

                    totalProcessed++;

                    // Find HST ID for this notice
                    String hstId = findHstIdForNotice(noticeNo);

                    if (hstId == null) {
                        log.warn("[TS-HST-LOOP] No HST ID found for notice {}, skipping", noticeNo);
                        skipped++;
                        continue;
                    }

                    // Check if address is still invalid
                    boolean isStillInvalid = checkInvalidAddress(hstId);

                    if (isStillInvalid) {
                        // Re-apply TS-HST suspension
                        boolean success = reapplyTsHstSuspension(noticeNo, hstId);

                        if (success) {
                            reapplied++;
                            log.info("[TS-HST-LOOP] Re-applied TS-HST to notice {} (HST ID: {})",
                                    noticeNo, hstId);
                        } else {
                            errors++;
                            log.error("[TS-HST-LOOP] Failed to re-apply TS-HST to notice {}", noticeNo);
                        }
                    } else {
                        log.info("[TS-HST-LOOP] Address is now valid for HST ID {}, allowing notice {} to revive",
                                hstId, noticeNo);
                        skipped++;
                    }

                } catch (Exception e) {
                    log.error("[TS-HST-LOOP] Error processing notice", e);
                    errors++;
                }
            }

            log.info("[TS-HST-LOOP] Looping suspension job completed: processed={}, reapplied={}, skipped={}, errors={}",
                    totalProcessed, reapplied, skipped, errors);

            SuspensionResult result = SuspensionResult.success(totalProcessed, reapplied, skipped);
            result.setErrors(errors);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("[TS-HST-LOOP] Error executing looping suspension job", e);
            return CompletableFuture.completedFuture(
                    SuspensionResult.error("Job execution failed: " + e.getMessage())
            );
        }
    }

    @Override
    public SuspensionResult processNotice(String noticeNo) {
        log.info("[TS-HST-LOOP] Processing single notice: {}", noticeNo);

        try {
            // Find HST ID for this notice
            String hstId = findHstIdForNotice(noticeNo);

            if (hstId == null) {
                return SuspensionResult.error("No HST ID found for notice");
            }

            // Check if address is still invalid
            boolean isStillInvalid = checkInvalidAddress(hstId);

            if (isStillInvalid) {
                // Re-apply TS-HST suspension
                boolean success = reapplyTsHstSuspension(noticeNo, hstId);

                if (success) {
                    return SuspensionResult.success(1, 1, 0);
                } else {
                    return SuspensionResult.error("Failed to re-apply TS-HST suspension");
                }
            } else {
                return SuspensionResult.success(1, 0, 1);
            }

        } catch (Exception e) {
            log.error("[TS-HST-LOOP] Error processing notice {}", noticeNo, e);
            return SuspensionResult.error("Failed to process notice: " + e.getMessage());
        }
    }

    /**
     * Find HST ID associated with a notice
     *
     * @param noticeNo Notice number
     * @return HST ID or null if not found
     */
    private String findHstIdForNotice(String noticeNo) {
        try {
            // Query ocms_offence_notice_owner_driver for the owner/driver ID
            Map<String, Object> onodFilter = new HashMap<>();
            onodFilter.put("noticeNo", noticeNo);

            List<Map<String, Object>> onodRecords = tableQueryService.query(
                    "ocms_offence_notice_owner_driver", onodFilter);

            if (onodRecords == null || onodRecords.isEmpty()) {
                log.warn("[TS-HST-LOOP] No owner/driver record found for notice {}", noticeNo);
                return null;
            }

            String idNo = (String) onodRecords.get(0).get("idNo");
            String idType = (String) onodRecords.get(0).get("idType");

            // Verify this ID is in ocms_hst (is actually an HST ID)
            Map<String, Object> hstFilter = new HashMap<>();
            hstFilter.put("idNo", idNo);
            hstFilter.put("idType", idType);

            List<Map<String, Object>> hstRecords = tableQueryService.query("ocms_hst", hstFilter);

            if (hstRecords != null && !hstRecords.isEmpty()) {
                return idNo;
            }

            return null;

        } catch (Exception e) {
            log.error("[TS-HST-LOOP] Error finding HST ID for notice {}", noticeNo, e);
            return null;
        }
    }

    /**
     * Check if address is still invalid for a given HST ID
     *
     * @param hstId HST ID number
     * @return true if address is still invalid
     */
    private boolean checkInvalidAddress(String hstId) {
        try {
            // Query ocms_temp_unc_hst_addr for this HST ID
            Map<String, Object> filter = new HashMap<>();
            filter.put("idNo", hstId);
            filter.put("queryReason", "HST");

            List<Map<String, Object>> addressRecords = tableQueryService.query(
                    "ocms_temp_unc_hst_addr", filter);

            if (addressRecords == null || addressRecords.isEmpty()) {
                log.debug("[TS-HST-LOOP] No address record found for HST ID {}", hstId);
                // If no record, assume address is invalid (conservative approach)
                return true;
            }

            // Check if address is invalid
            Map<String, Object> addressRecord = addressRecords.get(0);

            Object invalidTagVal = addressRecord.get("invalidAddressTag");
            String streetName = (String) addressRecord.get("streetName");
            String postalCode = (String) addressRecord.get("postalCode");

            boolean hasInvalidTag = invalidTagVal != null && !invalidTagVal.toString().trim().isEmpty();
            boolean hasNaOrZero = (streetName != null && "NA".equalsIgnoreCase(streetName.trim()))
                    || (postalCode != null && "000000".equals(postalCode.trim()));

            return hasInvalidTag || hasNaOrZero;

        } catch (Exception e) {
            log.error("[TS-HST-LOOP] Error checking address validity for {}", hstId, e);
            // On error, assume address is invalid (conservative approach)
            return true;
        }
    }

    /**
     * Re-apply TS-HST suspension to a notice
     *
     * @param noticeNo Notice number
     * @param hstId HST ID for logging
     * @return true if successful
     */
    @Transactional
    private boolean reapplyTsHstSuspension(String noticeNo, String hstId) {
        log.info("[TS-HST-LOOP] Re-applying TS-HST suspension to notice {} (HST ID: {})", noticeNo, hstId);

        try {
            // Query suspension reason table for noOfDaysForRevival
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            suspReasonFilters.put("reasonOfSuspension", SystemConstant.SuspensionReason.HST);

            List<Map<String, Object>> suspReasonResults = tableQueryService.query(
                    "ocms_suspension_reason", suspReasonFilters);

            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("[TS-HST-LOOP] No suspension reason found for TS-HST");
                return false;
            }

            Object noOfDaysObj = suspReasonResults.get(0).get("noOfDaysForRevival");
            int noOfDaysForRevival = 0;

            if (noOfDaysObj != null) {
                try {
                    if (noOfDaysObj instanceof Number) {
                        noOfDaysForRevival = ((Number) noOfDaysObj).intValue();
                    } else {
                        noOfDaysForRevival = Integer.parseInt(noOfDaysObj.toString().trim());
                    }
                } catch (NumberFormatException e) {
                    log.warn("[TS-HST-LOOP] Invalid noOfDaysForRevival value: {}", noOfDaysObj);
                }
            }

            // Calculate new revival date
            LocalDate currentDate = LocalDate.now();
            LocalDate newRevivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = newRevivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Update VON table with new suspension date and revival date
            Map<String, Object> vonUpdates = new HashMap<>();
            vonUpdates.put("eprDateOfSuspension", LocalDateTime.now());
            vonUpdates.put("dueDateOfRevival", formattedRevivalDate);
            vonUpdates.put("isSync", "N"); // Trigger internet sync

            Map<String, Object> vonFilter = new HashMap<>();
            vonFilter.put("noticeNo", noticeNo);

            List<Map<String, Object>> updatedRecords = tableQueryService.patch(
                    "ocms_valid_offence_notice", vonFilter, vonUpdates);

            if (updatedRecords != null && !updatedRecords.isEmpty()) {
                log.info("[TS-HST-LOOP] Successfully re-applied TS-HST to notice {} with new revival date: {}",
                        noticeNo, formattedRevivalDate);

                // Apply suspension via API
                try {
                    String remarks = String.format("Re-applied TS-HST (looping) due to still-invalid address for HST ID %s", hstId);

                    List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                            Collections.singletonList(noticeNo),
                            SystemConstant.SuspensionType.TEMPORARY,
                            SystemConstant.SuspensionReason.HST,
                            remarks,
                            SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                            SystemConstant.Subsystem.OCMS_CODE,
                            null,
                            noOfDaysForRevival
                    );

                    if (!apiResponses.isEmpty() && suspensionApiClient.isSuccess(apiResponses.get(0))) {
                        log.info("[TS-HST-LOOP] Successfully called suspension API for notice {}", noticeNo);
                    }

                } catch (Exception ex) {
                    log.error("[TS-HST-LOOP] Error calling suspension API: {}", ex.getMessage(), ex);
                }

                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("[TS-HST-LOOP] Error re-applying TS-HST suspension to notice {}", noticeNo, e);
            return false;
        }
    }
}
