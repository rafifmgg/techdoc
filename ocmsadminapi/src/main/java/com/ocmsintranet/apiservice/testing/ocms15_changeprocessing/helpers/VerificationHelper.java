package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Verification Helper - Verifies business logic for OCMS 15 Change Processing Stage
 *
 * Responsibilities:
 * - Step 4: Verify VON stage progression (prev ← last ← next)
 * - Step 4: Verify amount_payable calculation (11 rules from §2.5.1.3)
 * - Step 4: Verify audit trail in change processing table
 * - Step 4: Verify DH/MHA check allow flag in ONOD
 * - Step 4: Verify Excel report contents (16 columns)
 * - Step 4: Verify PLUS validation rules (8 steps from §4.3.2)
 */
@Slf4j
@Component
public class VerificationHelper {

    /**
     * Verify VON stage progression: prev ← last ← next (shift left)
     *
     * @param vonData VON record from database
     * @param expectedPrev Expected prev_processing_stage
     * @param expectedLast Expected last_processing_stage
     * @param expectedNext Expected next_processing_stage
     * @return Verification result map
     */
    public Map<String, Object> verifyStageProgression(
        Map<String, Object> vonData,
        String expectedPrev,
        String expectedLast,
        String expectedNext
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            String actualPrev = (String) vonData.get("prevProcessingStage");
            String actualLast = (String) vonData.get("lastProcessingStage");
            String actualNext = (String) vonData.get("nextProcessingStage");

            boolean prevMatch = Objects.equals(expectedPrev, actualPrev);
            boolean lastMatch = Objects.equals(expectedLast, actualLast);
            boolean nextMatch = Objects.equals(expectedNext, actualNext);

            if (!prevMatch) {
                errors.add("prev_processing_stage mismatch: expected=" + expectedPrev + ", actual=" + actualPrev);
            }
            if (!lastMatch) {
                errors.add("last_processing_stage mismatch: expected=" + expectedLast + ", actual=" + actualLast);
            }
            if (!nextMatch) {
                errors.add("next_processing_stage mismatch: expected=" + expectedNext + ", actual=" + actualNext);
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("actual", Map.of(
                "prev", actualPrev,
                "last", actualLast,
                "next", actualNext
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying stage progression: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify amount_payable calculation (11 rules from spec §2.5.1.3)
     *
     * @param vonData VON record
     * @param expectedAmount Expected amount_payable
     * @param isManualChange Whether this is a manual change (skip calculation)
     * @return Verification result map
     */
    public Map<String, Object> verifyAmountPayable(
        Map<String, Object> vonData,
        BigDecimal expectedAmount,
        boolean isManualChange
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            BigDecimal actualAmount = getBigDecimal(vonData.get("amountPayable"));

            if (isManualChange) {
                // Manual changes: amount should NOT change (already calculated by user)
                result.put("note", "Manual change - amount_payable should remain unchanged");
            }

            boolean match = compareAmounts(expectedAmount, actualAmount);

            if (!match) {
                errors.add("amount_payable mismatch: expected=" + expectedAmount + ", actual=" + actualAmount);
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("actual", actualAmount);
            result.put("expected", expectedAmount);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying amount_payable: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify audit trail in change processing table
     *
     * @param changeData Change processing record
     * @param expectedSource Expected source (OCMS, PLUS, AVSS, SYSTEM)
     * @param expectedStage Expected new_processing_stage
     * @param expectedRemarks Expected remarks (optional)
     * @return Verification result map
     */
    public Map<String, Object> verifyAuditTrail(
        Map<String, Object> changeData,
        String expectedSource,
        String expectedStage,
        String expectedRemarks
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            String actualSource = (String) changeData.get("source");
            String actualStage = (String) changeData.get("newProcessingStage");
            String actualRemarks = (String) changeData.get("remarks");

            boolean sourceMatch = Objects.equals(expectedSource, actualSource);
            boolean stageMatch = Objects.equals(expectedStage, actualStage);
            boolean remarksMatch = expectedRemarks == null || Objects.equals(expectedRemarks, actualRemarks);

            if (!sourceMatch) {
                errors.add("source mismatch: expected=" + expectedSource + ", actual=" + actualSource);
            }
            if (!stageMatch) {
                errors.add("new_processing_stage mismatch: expected=" + expectedStage + ", actual=" + actualStage);
            }
            if (!remarksMatch) {
                errors.add("remarks mismatch: expected=" + expectedRemarks + ", actual=" + actualRemarks);
            }

            // Verify composite key: notice_no + date_of_change + new_processing_stage
            boolean hasCompositeKey = changeData.containsKey("noticeNo")
                && changeData.containsKey("dateOfChange")
                && changeData.containsKey("newProcessingStage");

            if (!hasCompositeKey) {
                errors.add("Missing composite key fields");
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("actual", Map.of(
                "source", actualSource,
                "stage", actualStage,
                "remarks", actualRemarks != null ? actualRemarks : "null"
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying audit trail: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify DH/MHA check allow flag in ONOD table
     *
     * @param onodData ONOD record
     * @param currentStage Current processing stage
     * @return Verification result map
     */
    public Map<String, Object> verifyDhMhaCheckAllow(
        Map<String, Object> onodData,
        String currentStage
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            String actualFlag = (String) onodData.get("dhMhaCheckAllow");

            // DH/MHA check allowed only for specific stages (per spec §4.4)
            Set<String> allowedStages = Set.of("RD1", "RD2", "DN1", "DN2", "WOF", "WOE");
            boolean shouldAllow = allowedStages.contains(currentStage);

            String expectedFlag = shouldAllow ? "Y" : "N";

            boolean match = Objects.equals(expectedFlag, actualFlag);

            if (!match) {
                errors.add("dh_mha_check_allow mismatch: expected=" + expectedFlag + ", actual=" + actualFlag);
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("actual", actualFlag);
            result.put("expected", expectedFlag);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying DH/MHA flag: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify Excel report contents (16 columns from spec §4.5.3)
     *
     * @param reportData Report data extracted from Excel
     * @param noticeNo Notice number to verify
     * @return Verification result map
     */
    public Map<String, Object> verifyExcelReport(
        List<Map<String, Object>> reportData,
        String noticeNo
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            // Find row for this notice
            Map<String, Object> row = reportData.stream()
                .filter(r -> noticeNo.equals(r.get("notice_no")))
                .findFirst()
                .orElse(null);

            if (row == null) {
                errors.add("Notice " + noticeNo + " not found in report");
            } else {
                // Verify 16 columns exist
                List<String> expectedColumns = Arrays.asList(
                    "notice_no",
                    "date_of_change",
                    "prev_processing_stage",
                    "last_processing_stage",
                    "next_processing_stage",
                    "amount_payable",
                    "source",
                    "remarks",
                    "vehicle_no",
                    "offender_id",
                    "pp_code",
                    "pp_name",
                    "notice_date_time",
                    "upd_date",
                    "upd_user_id",
                    "created_date"
                );

                for (String column : expectedColumns) {
                    if (!row.containsKey(column)) {
                        errors.add("Missing column: " + column);
                    }
                }
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("columnCount", row != null ? row.size() : 0);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying Excel report: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify PLUS validation rules (8 steps from spec §4.3.2)
     *
     * @param validationResult Validation result from PLUS API
     * @param expectedPass Whether validation should pass
     * @param expectedReason Expected rejection reason (if expectedPass = false)
     * @return Verification result map
     */
    public Map<String, Object> verifyPlusValidation(
        Map<String, Object> validationResult,
        boolean expectedPass,
        String expectedReason
    ) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            boolean actualPass = Boolean.TRUE.equals(validationResult.get("success"));
            String actualReason = (String) validationResult.get("reason");

            if (expectedPass != actualPass) {
                errors.add("Validation result mismatch: expected pass=" + expectedPass + ", actual pass=" + actualPass);
            }

            if (!expectedPass && !Objects.equals(expectedReason, actualReason)) {
                errors.add("Rejection reason mismatch: expected=" + expectedReason + ", actual=" + actualReason);
            }

            result.put("success", errors.isEmpty());
            result.put("errors", errors);
            result.put("actual", Map.of(
                "pass", actualPass,
                "reason", actualReason != null ? actualReason : "null"
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying PLUS validation: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Comprehensive verification for a single scenario
     *
     * @param scenario Test scenario data
     * @param vonData VON record
     * @param onodData ONOD record
     * @param changeData Change processing records
     * @return Comprehensive verification result
     */
    public Map<String, Object> verifyScenario(
        Map<String, Object> scenario,
        Map<String, Object> vonData,
        Map<String, Object> onodData,
        List<Map<String, Object>> changeData
    ) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> verifications = new HashMap<>();

        try {
            // 1. Verify stage progression
            verifications.put("stageProgression", verifyStageProgression(
                vonData,
                (String) scenario.get("expectedPrev"),
                (String) scenario.get("expectedLast"),
                (String) scenario.get("expectedNext")
            ));

            // 2. Verify amount_payable
            verifications.put("amountPayable", verifyAmountPayable(
                vonData,
                getBigDecimal(scenario.get("expectedAmount")),
                Boolean.TRUE.equals(scenario.get("isManualChange"))
            ));

            // 3. Verify audit trail (latest change record)
            if (!changeData.isEmpty()) {
                Map<String, Object> latestChange = changeData.get(changeData.size() - 1);
                verifications.put("auditTrail", verifyAuditTrail(
                    latestChange,
                    (String) scenario.get("expectedSource"),
                    (String) scenario.get("expectedStage"),
                    (String) scenario.get("expectedRemarks")
                ));
            }

            // 4. Verify DH/MHA flag
            if (onodData != null) {
                verifications.put("dhMhaFlag", verifyDhMhaCheckAllow(
                    onodData,
                    (String) vonData.get("lastProcessingStage")
                ));
            }

            // Check if all verifications passed
            boolean allPassed = verifications.values().stream()
                .allMatch(v -> Boolean.TRUE.equals(((Map<?, ?>) v).get("success")));

            result.put("success", allPassed);
            result.put("verifications", verifications);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying scenario: {}", e.getMessage(), e);
        }

        return result;
    }

    // ========== Helper Methods ==========

    /**
     * Convert object to BigDecimal safely
     */
    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) return new BigDecimal((String) value);
        return BigDecimal.ZERO;
    }

    /**
     * Compare two BigDecimal amounts (with 2 decimal places precision)
     */
    private boolean compareAmounts(BigDecimal expected, BigDecimal actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return expected.setScale(2, java.math.RoundingMode.HALF_UP)
            .compareTo(actual.setScale(2, java.math.RoundingMode.HALF_UP)) == 0;
    }
}
