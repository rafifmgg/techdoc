package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of HstAutoSuspendService
 * OCMS 20: Auto-suspend new notices with HST IDs when address is invalid
 *
 * Process Flow:
 * 1. Query ocms_hst for HST IDs that need processing
 * 2. For each HST ID, check if address is invalid in ocms_temp_unc_hst_addr
 * 3. Find associated notices from ocms_offence_notice_owner_driver
 * 4. Apply TS-HST suspension to notices with invalid addresses
 */
@Slf4j
@Service
public class HstAutoSuspendServiceImpl implements HstAutoSuspendService {

    private final TableQueryService tableQueryService;
    private final SuspensionApiClient suspensionApiClient;

    @Autowired
    public HstAutoSuspendServiceImpl(
            TableQueryService tableQueryService,
            SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.suspensionApiClient = suspensionApiClient;
    }

    @Override
    @Async
    public CompletableFuture<SuspensionResult> executeAutoSuspendJob() {
        log.info("[HST-AUTO-SUSPEND] Starting auto-suspend job for new notices with HST IDs");

        try {
            // Get all HST IDs from ocms_hst
            Map<String, Object> hstFilter = new HashMap<>();
            hstFilter.put("$limit", 10000);

            List<Map<String, Object>> hstRecords = tableQueryService.query("ocms_hst", hstFilter);
            log.info("[HST-AUTO-SUSPEND] Found {} HST IDs to process", hstRecords.size());

            int totalProcessed = 0;
            int suspended = 0;
            int skipped = 0;
            int errors = 0;

            // Process each HST ID
            for (Map<String, Object> hstRecord : hstRecords) {
                try {
                    String idNo = (String) hstRecord.get("idNo");
                    String idType = (String) hstRecord.get("idType");

                    if (idNo == null || idNo.isEmpty()) {
                        log.warn("[HST-AUTO-SUSPEND] Skipping HST record with empty idNo");
                        continue;
                    }

                    totalProcessed++;

                    // Check if address is invalid in temp table
                    boolean isInvalidAddress = checkInvalidAddress(idNo, idType);

                    if (isInvalidAddress) {
                        // Find associated notices
                        List<String> noticeNumbers = findAssociatedNotices(idNo, idType);

                        if (!noticeNumbers.isEmpty()) {
                            // Apply TS-HST suspension
                            int suspendedCount = applyTsHstSuspension(noticeNumbers, idNo);
                            suspended += suspendedCount;
                            log.info("[HST-AUTO-SUSPEND] Applied TS-HST to {} notices for HST ID {}",
                                    suspendedCount, idNo);
                        } else {
                            log.debug("[HST-AUTO-SUSPEND] No notices found for HST ID {}", idNo);
                            skipped++;
                        }
                    } else {
                        log.debug("[HST-AUTO-SUSPEND] Address is valid for HST ID {}, skipping", idNo);
                        skipped++;
                    }

                } catch (Exception e) {
                    log.error("[HST-AUTO-SUSPEND] Error processing HST record", e);
                    errors++;
                }
            }

            log.info("[HST-AUTO-SUSPEND] Auto-suspend job completed: processed={}, suspended={}, skipped={}, errors={}",
                    totalProcessed, suspended, skipped, errors);

            SuspensionResult result = SuspensionResult.success(totalProcessed, suspended, skipped);
            result.setErrors(errors);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("[HST-AUTO-SUSPEND] Error executing auto-suspend job", e);
            return CompletableFuture.completedFuture(
                    SuspensionResult.error("Job execution failed: " + e.getMessage())
            );
        }
    }

    @Override
    public SuspensionResult processHstId(String hstId) {
        log.info("[HST-AUTO-SUSPEND] Processing single HST ID: {}", hstId);

        try {
            // Check if address is invalid
            boolean isInvalidAddress = checkInvalidAddress(hstId, "N");

            if (isInvalidAddress) {
                // Find associated notices
                List<String> noticeNumbers = findAssociatedNotices(hstId, "N");

                if (!noticeNumbers.isEmpty()) {
                    // Apply TS-HST suspension
                    int suspendedCount = applyTsHstSuspension(noticeNumbers, hstId);
                    return SuspensionResult.success(1, suspendedCount, 0);
                } else {
                    return SuspensionResult.success(1, 0, 1);
                }
            } else {
                return SuspensionResult.success(1, 0, 1);
            }

        } catch (Exception e) {
            log.error("[HST-AUTO-SUSPEND] Error processing HST ID {}", hstId, e);
            return SuspensionResult.error("Failed to process HST ID: " + e.getMessage());
        }
    }

    /**
     * Check if address is invalid for a given HST ID
     *
     * @param idNo HST ID number
     * @param idType ID type
     * @return true if address is invalid
     */
    private boolean checkInvalidAddress(String idNo, String idType) {
        try {
            // Query ocms_temp_unc_hst_addr for this HST ID
            Map<String, Object> filter = new HashMap<>();
            filter.put("idNo", idNo);
            filter.put("queryReason", "HST");

            List<Map<String, Object>> addressRecords = tableQueryService.query(
                    "ocms_temp_unc_hst_addr", filter);

            if (addressRecords == null || addressRecords.isEmpty()) {
                log.debug("[HST-AUTO-SUSPEND] No address record found for HST ID {}", idNo);
                return false;
            }

            // Check if address is invalid
            Map<String, Object> addressRecord = addressRecords.get(0);

            // Check for invalid address indicators:
            // 1. invalidAddressTag has value
            // 2. streetName = 'NA' OR postalCode = '000000'
            Object invalidTagVal = addressRecord.get("invalidAddressTag");
            String streetName = (String) addressRecord.get("streetName");
            String postalCode = (String) addressRecord.get("postalCode");

            boolean hasInvalidTag = invalidTagVal != null && !invalidTagVal.toString().trim().isEmpty();
            boolean hasNaOrZero = (streetName != null && "NA".equalsIgnoreCase(streetName.trim()))
                    || (postalCode != null && "000000".equals(postalCode.trim()));

            boolean isInvalid = hasInvalidTag || hasNaOrZero;

            log.debug("[HST-AUTO-SUSPEND] Address validity check for {}: invalidTag={}, naOrZero={}, result={}",
                    idNo, hasInvalidTag, hasNaOrZero, isInvalid);

            return isInvalid;

        } catch (Exception e) {
            log.error("[HST-AUTO-SUSPEND] Error checking address validity for {}", idNo, e);
            return false;
        }
    }

    /**
     * Find associated notices for a given HST ID
     *
     * @param idNo HST ID number
     * @param idType ID type
     * @return List of notice numbers
     */
    private List<String> findAssociatedNotices(String idNo, String idType) {
        try {
            // Query ocms_offence_notice_owner_driver for notices with this HST ID
            Map<String, Object> filter = new HashMap<>();
            filter.put("idNo", idNo);
            filter.put("idType", idType);

            List<Map<String, Object>> onodRecords = tableQueryService.query(
                    "ocms_offence_notice_owner_driver", filter);

            List<String> noticeNumbers = new ArrayList<>();
            if (onodRecords != null) {
                for (Map<String, Object> record : onodRecords) {
                    String noticeNo = (String) record.get("noticeNo");
                    if (noticeNo != null && !noticeNo.isEmpty()) {
                        noticeNumbers.add(noticeNo);
                    }
                }
            }

            log.debug("[HST-AUTO-SUSPEND] Found {} notices for HST ID {}", noticeNumbers.size(), idNo);
            return noticeNumbers;

        } catch (Exception e) {
            log.error("[HST-AUTO-SUSPEND] Error finding notices for HST ID {}", idNo, e);
            return new ArrayList<>();
        }
    }

    /**
     * Apply TS-HST suspension to notices
     * TS-HST = Temporary Suspension - House Tenant (indefinite looping suspension)
     *
     * @param noticeNumbers List of notice numbers to suspend
     * @param hstId HST ID for logging
     * @return Number of notices suspended
     */
    @Transactional
    private int applyTsHstSuspension(List<String> noticeNumbers, String hstId) {
        if (noticeNumbers == null || noticeNumbers.isEmpty()) {
            return 0;
        }

        log.info("[TS-HST] Applying TS-HST suspension to {} notices for HST ID {}",
                noticeNumbers.size(), hstId);

        try {
            // Filter for notices that don't already have a suspension
            Map<String, Object> vonFilter = new HashMap<>();
            vonFilter.put("noticeNo.in", noticeNumbers);
            vonFilter.put("suspensionType.null", true);

            List<Map<String, Object>> eligibleVonRecords = tableQueryService.query(
                    "ocms_valid_offence_notice", vonFilter);

            List<String> eligibleNoticeNos = new ArrayList<>();
            if (eligibleVonRecords != null) {
                for (Map<String, Object> record : eligibleVonRecords) {
                    String nn = (String) record.get("noticeNo");
                    if (nn != null && !nn.isEmpty()) {
                        eligibleNoticeNos.add(nn);
                    }
                }
            }

            if (eligibleNoticeNos.isEmpty()) {
                log.info("[TS-HST] No eligible notices for HST ID {} (all have suspension already)", hstId);
                return 0;
            }

            // Query suspension reason table to get noOfDaysForRevival for TS-HST
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            suspReasonFilters.put("reasonOfSuspension", SystemConstant.SuspensionReason.HST);

            List<Map<String, Object>> suspReasonResults = tableQueryService.query(
                    "ocms_suspension_reason", suspReasonFilters);

            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("[TS-HST] No suspension reason found for TS-HST - cannot apply suspension");
                return 0;
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
                    log.warn("[TS-HST] Invalid noOfDaysForRevival value: {}", noOfDaysObj);
                }
            }

            // Calculate revival date
            LocalDate currentDate = LocalDate.now();
            LocalDate revivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = revivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Update VON table
            Map<String, Object> vonUpdates = new HashMap<>();
            vonUpdates.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            vonUpdates.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.HST);
            vonUpdates.put("eprDateOfSuspension", LocalDateTime.now());
            vonUpdates.put("dueDateOfRevival", formattedRevivalDate);
            vonUpdates.put("isSync", "N"); // Trigger internet sync

            Map<String, Object> vonUpdateFilter = new HashMap<>();
            vonUpdateFilter.put("noticeNo.in", eligibleNoticeNos);

            List<Map<String, Object>> updatedRecords = tableQueryService.patch(
                    "ocms_valid_offence_notice", vonUpdateFilter, vonUpdates);

            int updatedCount = updatedRecords != null ? updatedRecords.size() : 0;

            if (updatedCount > 0) {
                log.info("[TS-HST] Successfully updated {} VON records for HST ID {}", updatedCount, hstId);

                // Apply suspension via API
                try {
                    String remarks = String.format("Auto-applied TS-HST due to invalid address for HST ID %s", hstId);

                    List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                            eligibleNoticeNos,
                            SystemConstant.SuspensionType.TEMPORARY,
                            SystemConstant.SuspensionReason.HST,
                            remarks,
                            SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                            SystemConstant.Subsystem.OCMS_CODE,
                            null,
                            noOfDaysForRevival
                    );

                    int successCount = 0;
                    for (Map<String, Object> apiResponse : apiResponses) {
                        if (suspensionApiClient.isSuccess(apiResponse)) {
                            successCount++;
                        }
                    }

                    log.info("[TS-HST] API call completed: {}/{} notices suspended successfully",
                            successCount, eligibleNoticeNos.size());

                } catch (Exception ex) {
                    log.error("[TS-HST] Error calling suspension API: {}", ex.getMessage(), ex);
                }
            }

            return updatedCount;

        } catch (Exception e) {
            log.error("[TS-HST] Error applying TS-HST suspension for HST ID {}", hstId, e);
            throw new RuntimeException("Error applying TS-HST suspension", e);
        }
    }
}
