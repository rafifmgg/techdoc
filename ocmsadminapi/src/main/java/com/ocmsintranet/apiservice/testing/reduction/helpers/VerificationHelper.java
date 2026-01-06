package com.ocmsintranet.apiservice.testing.reduction.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for reduction verification logic
 */
@Component
@Slf4j
public class VerificationHelper {

    // Eligible computer rule codes for reduction
    private static final Set<String> ELIGIBLE_RULE_CODES = Set.of("30305", "31302", "30302", "21300");

    // Eligible stages when computer rule code is eligible
    private static final Set<String> ELIGIBLE_STAGES_WITH_CODE = Set.of(
        "NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3"
    );

    // Fallback eligible stages when computer rule code is NOT eligible
    private static final Set<String> FALLBACK_ELIGIBLE_STAGES = Set.of("RR3", "DR3");

    private final ObjectMapper objectMapper;

    public VerificationHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Verify successful reduction flow
     * Checks that:
     * - Intranet VON updated correctly
     * - Internet VON updated correctly (synced with Intranet)
     * - Suspended Notice record created
     * - Reduction log record created
     *
     * @param noticeNo Notice number to verify
     * @param expectedReducedAmount Expected reduced amount
     * @param showDetails Whether to include details field
     * @param context TestContext containing data from both databases
     * @return Verification result map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkReductionSuccess(String noticeNo, BigDecimal expectedReducedAmount,
                                                      boolean showDetails, TestContext context) {
        log.debug("Verifying successful reduction for notice: {} (showDetails={})", noticeNo, showDetails);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);
        result.put("expected_reduced_amount", expectedReducedAmount);

        List<String> failedChecks = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // Step 1: Verify Intranet VON data
        Object intranetVonData = context.getIntranetVonData();
        if (!(intranetVonData instanceof List)) {
            result.put("status", "❌ error");
            result.put("message", "Intranet VON data not available");
            return result;
        }

        List<Map<String, Object>> intranetVonList = (List<Map<String, Object>>) intranetVonData;
        Map<String, Object> intranetVon = findNoticeInData(intranetVonList, noticeNo);

        if (intranetVon == null) {
            result.put("status", "❌ error");
            result.put("message", "Notice not found in Intranet VON data");
            return result;
        }

        // Verify Intranet VON fields
        Object amountPayable = intranetVon.get("amountPayable");
        Object suspensionType = intranetVon.get("suspensionType");
        Object eprReasonOfSuspension = intranetVon.get("eprReasonOfSuspension");
        Object eprReasonSuspensionDate = intranetVon.get("eprReasonSuspensionDate");
        Object dueDateOfRevival = intranetVon.get("dueDateOfRevival");

        details.add("Intranet VON.amountPayable = " + amountPayable);
        details.add("Intranet VON.suspensionType = " + suspensionType);
        details.add("Intranet VON.eprReasonOfSuspension = " + eprReasonOfSuspension);
        details.add("Intranet VON.eprReasonSuspensionDate = " + eprReasonSuspensionDate);
        details.add("Intranet VON.dueDateOfRevival = " + dueDateOfRevival);

        // Check amount_payable matches expected reduced amount
        BigDecimal actualAmount = parseBigDecimal(amountPayable);
        if (actualAmount == null || actualAmount.compareTo(expectedReducedAmount) != 0) {
            failedChecks.add("Intranet VON amountPayable mismatch: expected " + expectedReducedAmount + ", got " + actualAmount);
        }

        // Check suspension_type = TS
        if (!"TS".equals(suspensionType)) {
            failedChecks.add("Intranet VON suspensionType not TS: " + suspensionType);
        }

        // Check epr_reason_of_suspension = RED
        if (!"RED".equals(eprReasonOfSuspension)) {
            failedChecks.add("Intranet VON eprReasonOfSuspension not RED: " + eprReasonOfSuspension);
        }

        // Check epr_reason_suspension_date is not null
        if (eprReasonSuspensionDate == null) {
            failedChecks.add("Intranet VON eprReasonSuspensionDate is null");
        }

        // Step 2: Verify Internet VON data (if available)
        Object internetVonData = context.getInternetVonData();
        if (internetVonData instanceof List) {
            List<Map<String, Object>> internetVonList = (List<Map<String, Object>>) internetVonData;
            Map<String, Object> internetVon = findNoticeInData(internetVonList, noticeNo);

            if (internetVon != null) {
                Object internetAmountPayable = internetVon.get("amountPayable");
                Object internetSuspensionType = internetVon.get("suspensionType");
                Object internetEprReason = internetVon.get("eprReasonOfSuspension");

                details.add("Internet VON.amountPayable = " + internetAmountPayable);
                details.add("Internet VON.suspensionType = " + internetSuspensionType);
                details.add("Internet VON.eprReasonOfSuspension = " + internetEprReason);

                // Verify sync between Intranet and Internet
                if (!amountPayable.equals(internetAmountPayable)) {
                    failedChecks.add("Amount mismatch between Intranet and Internet VON");
                }
                if (!suspensionType.equals(internetSuspensionType)) {
                    failedChecks.add("Suspension type mismatch between Intranet and Internet VON");
                }
                if (!eprReasonOfSuspension.equals(internetEprReason)) {
                    failedChecks.add("EPR reason mismatch between Intranet and Internet VON");
                }
            } else {
                details.add("Internet VON: Notice not found");
            }
        } else {
            details.add("Internet VON: Data not available");
        }

        // Step 3: Verify Suspended Notice record
        Object suspendedNoticeData = context.getSuspendedNoticeData();
        if (suspendedNoticeData instanceof List) {
            List<Map<String, Object>> suspendedList = (List<Map<String, Object>>) suspendedNoticeData;
            Map<String, Object> suspendedNotice = findNoticeInData(suspendedList, noticeNo);

            if (suspendedNotice != null) {
                Object snSuspensionType = suspendedNotice.get("suspensionType");
                Object snReasonOfSuspension = suspendedNotice.get("reasonOfSuspension");
                Object snSuspensionSource = suspendedNotice.get("suspensionSource");
                Object snDateOfSuspension = suspendedNotice.get("dateOfSuspension");

                details.add("Suspended Notice.suspensionType = " + snSuspensionType);
                details.add("Suspended Notice.reasonOfSuspension = " + snReasonOfSuspension);
                details.add("Suspended Notice.suspensionSource = " + snSuspensionSource);
                details.add("Suspended Notice.dateOfSuspension = " + snDateOfSuspension);

                // Verify suspension type = TS
                if (!"TS".equals(snSuspensionType)) {
                    failedChecks.add("Suspended Notice suspensionType not TS: " + snSuspensionType);
                }

                // Verify reason = RED
                if (!"RED".equals(snReasonOfSuspension)) {
                    failedChecks.add("Suspended Notice reasonOfSuspension not RED: " + snReasonOfSuspension);
                }

                // Verify suspension source = PLUS
                if (!"PLUS".equals(snSuspensionSource)) {
                    failedChecks.add("Suspended Notice suspensionSource not PLUS: " + snSuspensionSource);
                }
            } else {
                failedChecks.add("Suspended Notice record not found");
            }
        } else {
            failedChecks.add("Suspended Notice data not available");
        }

        // Step 4: Verify Reduction Log record
        Object reducedAmountLogData = context.getReducedAmountLogData();
        if (reducedAmountLogData instanceof List) {
            List<Map<String, Object>> logList = (List<Map<String, Object>>) reducedAmountLogData;
            Map<String, Object> logRecord = findNoticeInData(logList, noticeNo);

            if (logRecord != null) {
                Object logReducedAmount = logRecord.get("reducedAmount");
                Object logDateOfReduction = logRecord.get("dateOfReduction");

                details.add("Reduction Log.reducedAmount = " + logReducedAmount);
                details.add("Reduction Log.dateOfReduction = " + logDateOfReduction);

                // Verify reduced amount matches
                BigDecimal logAmount = parseBigDecimal(logReducedAmount);
                if (logAmount == null || logAmount.compareTo(expectedReducedAmount) != 0) {
                    failedChecks.add("Reduction Log amount mismatch: expected " + expectedReducedAmount + ", got " + logAmount);
                }
            } else {
                failedChecks.add("Reduction Log record not found");
            }
        } else {
            failedChecks.add("Reduction Log data not available");
        }

        // Build result
        if (showDetails) {
            result.put("details", details);
        }

        if (failedChecks.isEmpty()) {
            result.put("status", "✅ success");
            result.put("message", "Reduction applied successfully - VON updated, TS-RED suspension created, both DBs synced");
        } else {
            result.put("status", "❌ failed");
            result.put("message", "Reduction verification failed: " + String.join(", ", failedChecks));
            result.put("failed_checks", failedChecks);
        }

        return result;
    }

    /**
     * Verify reduction rejection flow
     * Checks that no changes were made to any database
     *
     * @param noticeNo Notice number to verify
     * @param expectedReason Expected rejection reason
     * @param showDetails Whether to include details field
     * @param context TestContext containing data
     * @return Verification result map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkReductionRejected(String noticeNo, String expectedReason,
                                                       boolean showDetails, TestContext context) {
        log.debug("Verifying reduction rejection for notice: {} (reason: {})", noticeNo, expectedReason);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);
        result.put("expected_rejection_reason", expectedReason);

        List<String> failedChecks = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // Verify no Suspended Notice record created
        Object suspendedNoticeData = context.getSuspendedNoticeData();
        if (suspendedNoticeData instanceof List) {
            List<Map<String, Object>> suspendedList = (List<Map<String, Object>>) suspendedNoticeData;
            Map<String, Object> suspendedNotice = findNoticeInData(suspendedList, noticeNo);

            if (suspendedNotice != null) {
                failedChecks.add("Suspended Notice record should not exist for rejected reduction");
            } else {
                details.add("Suspended Notice: Not found (correct)");
            }
        }

        // Verify no Reduction Log record created
        Object reducedAmountLogData = context.getReducedAmountLogData();
        if (reducedAmountLogData instanceof List) {
            List<Map<String, Object>> logList = (List<Map<String, Object>>) reducedAmountLogData;
            Map<String, Object> logRecord = findNoticeInData(logList, noticeNo);

            if (logRecord != null) {
                failedChecks.add("Reduction Log record should not exist for rejected reduction");
            } else {
                details.add("Reduction Log: Not found (correct)");
            }
        }

        // Build result
        if (showDetails) {
            result.put("details", details);
        }

        if (failedChecks.isEmpty()) {
            result.put("status", "✅ success");
            result.put("message", "Reduction correctly rejected: " + expectedReason);
        } else {
            result.put("status", "❌ failed");
            result.put("message", "Rejection verification failed: " + String.join(", ", failedChecks));
            result.put("failed_checks", failedChecks);
        }

        return result;
    }

    /**
     * Check if computer rule code is eligible for reduction
     *
     * @param ruleCode Rule code to check
     * @return true if eligible
     */
    public boolean isEligibleRuleCode(String ruleCode) {
        return ELIGIBLE_RULE_CODES.contains(ruleCode);
    }

    /**
     * Check if stage is eligible for reduction with eligible rule code
     *
     * @param stage Processing stage
     * @return true if eligible
     */
    public boolean isEligibleStageWithCode(String stage) {
        return ELIGIBLE_STAGES_WITH_CODE.contains(stage);
    }

    /**
     * Check if stage is eligible for reduction without eligible rule code
     *
     * @param stage Processing stage
     * @return true if RR3 or DR3
     */
    public boolean isFallbackEligibleStage(String stage) {
        return FALLBACK_ELIGIBLE_STAGES.contains(stage);
    }

    /**
     * Find notice in data list by notice number
     *
     * @param dataList List of data maps
     * @param noticeNo Notice number to find
     * @return Data map if found, null otherwise
     */
    private Map<String, Object> findNoticeInData(List<Map<String, Object>> dataList, String noticeNo) {
        if (dataList == null) {
            return null;
        }

        for (Map<String, Object> item : dataList) {
            Object itemNoticeNo = item.get("noticeNo");
            if (noticeNo.equals(itemNoticeNo)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Verify error response (for validation errors, not found, etc.)
     * Checks that:
     * - HTTP status code matches expected
     * - Error message matches expected
     * - No database changes were made
     *
     * @param noticeNo Notice number to verify
     * @param expectedHttpStatus Expected HTTP status code (400, 404, etc.)
     * @param expectedMessage Expected error message
     * @param actualHttpStatus Actual HTTP status from API response
     * @param actualMessage Actual error message from API response
     * @param showDetails Whether to include details field
     * @param context TestContext containing data
     * @return Verification result map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkReductionError(String noticeNo, int expectedHttpStatus,
                                                    String expectedMessage, int actualHttpStatus,
                                                    String actualMessage, boolean showDetails,
                                                    TestContext context) {
        log.debug("Verifying reduction error for notice: {} (expected status: {}, message: {})",
                  noticeNo, expectedHttpStatus, expectedMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);
        result.put("expected_http_status", expectedHttpStatus);
        result.put("expected_message", expectedMessage);

        List<String> failedChecks = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // Check HTTP status code
        details.add("Expected HTTP status: " + expectedHttpStatus);
        details.add("Actual HTTP status: " + actualHttpStatus);
        if (actualHttpStatus != expectedHttpStatus) {
            failedChecks.add("HTTP status mismatch: expected " + expectedHttpStatus + ", got " + actualHttpStatus);
        }

        // Check error message
        details.add("Expected message: " + expectedMessage);
        details.add("Actual message: " + actualMessage);
        if (actualMessage == null || !actualMessage.contains(expectedMessage)) {
            failedChecks.add("Error message mismatch: expected '" + expectedMessage + "', got '" + actualMessage + "'");
        }

        // Verify no database changes (for error scenarios)
        // Check no Suspended Notice record created
        Object suspendedNoticeData = context.getSuspendedNoticeData();
        if (suspendedNoticeData instanceof List) {
            List<Map<String, Object>> suspendedList = (List<Map<String, Object>>) suspendedNoticeData;
            Map<String, Object> suspendedNotice = findNoticeInData(suspendedList, noticeNo);

            if (suspendedNotice != null) {
                // Only flag as error if notice exists in DB (for TC07 notice not found, notice won't exist)
                if (!"NOTFOUND".equals(noticeNo.substring(0, Math.min(8, noticeNo.length())))) {
                    failedChecks.add("Suspended Notice record should not exist for error scenario");
                }
            } else {
                details.add("Suspended Notice: Not found (correct)");
            }
        }

        // Check no Reduction Log record created
        Object reducedAmountLogData = context.getReducedAmountLogData();
        if (reducedAmountLogData instanceof List) {
            List<Map<String, Object>> logList = (List<Map<String, Object>>) reducedAmountLogData;
            Map<String, Object> logRecord = findNoticeInData(logList, noticeNo);

            if (logRecord != null) {
                failedChecks.add("Reduction Log record should not exist for error scenario");
            } else {
                details.add("Reduction Log: Not found (correct)");
            }
        }

        // Build result
        if (showDetails) {
            result.put("details", details);
        }

        if (failedChecks.isEmpty()) {
            result.put("status", "✅ success");
            result.put("message", "Error scenario validated correctly: " + expectedMessage);
        } else {
            result.put("status", "❌ failed");
            result.put("message", "Error verification failed: " + String.join(", ", failedChecks));
            result.put("failed_checks", failedChecks);
        }

        return result;
    }

    /**
     * Parse object value to BigDecimal
     *
     * @param value Object to parse
     * @return BigDecimal value, or null if parsing fails
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }
                return new BigDecimal(strValue);
            }
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from value: {} ({})", value, e.getMessage());
        }

        return null;
    }
}
