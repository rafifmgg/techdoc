package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

/**
 * Represents detailed comparison result for TS-ACR validation updates in ocms_valid_offence_notice table
 */
@Data
@Builder
public class TsAcrValidationComparisonResult {

    private String noticeNumber;
    private ComparisonStatus overallStatus;
    private int totalFields;
    private int matchedFields;
    private double matchPercentage;

    // Expected vs actual TS-ACR values
    private TsAcrValidationData expectedData;
    private TsAcrValidationData actualData;

    // Before and after validation states
    private TsAcrValidationData beforeUpdate;
    private TsAcrValidationData afterUpdate;

    @Builder.Default
    private List<FieldComparison> fieldComparisons = new ArrayList<>();

    @Builder.Default
    private List<String> verificationDetails = new ArrayList<>();

    @Builder.Default
    private List<String> businessRuleDetails = new ArrayList<>();

    @Data
    @Builder
    public static class TsAcrValidationData {
        private String noticeNo;
        private String suspensionType;
        private String eprReasonOfSuspension;
        private String updUserId;
        private LocalDateTime updDate;
    }

    @Data
    @Builder
    public static class FieldComparison {
        private String fieldName;
        private String expectedValue;
        private String actualValue;
        private ComparisonStatus status;
        private String businessRule;
    }

    public enum ComparisonStatus {
        MATCH("✅"),
        MISMATCH("❌"),
        MISSING_RECORD("❌"),
        UPDATE_FAILED("❌"),
        BUSINESS_RULE_VIOLATION("❌");

        private final String symbol;

        ComparisonStatus(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public void addFieldComparison(String fieldName, String expectedValue, String actualValue, String businessRule) {
        ComparisonStatus status;
        if (expectedValue == null && actualValue == null) {
            status = ComparisonStatus.MATCH;
        } else if (expectedValue == null || actualValue == null) {
            status = ComparisonStatus.MISMATCH;
        } else {
            status = expectedValue.equals(actualValue) ? ComparisonStatus.MATCH : ComparisonStatus.MISMATCH;
        }

        FieldComparison comparison = FieldComparison.builder()
            .fieldName(fieldName)
            .expectedValue(expectedValue != null ? expectedValue : "NULL")
            .actualValue(actualValue != null ? actualValue : "NULL")
            .status(status)
            .businessRule(businessRule)
            .build();

        fieldComparisons.add(comparison);

        if (status == ComparisonStatus.MATCH) {
            matchedFields++;
        }
        totalFields++;

        // Calculate match percentage
        matchPercentage = totalFields > 0 ? ((double) matchedFields / totalFields) * 100 : 0;

        // Determine overall status
        if (matchedFields == totalFields) {
            overallStatus = ComparisonStatus.MATCH;
        } else if (matchedFields > 0) {
            overallStatus = ComparisonStatus.MISMATCH;
        } else {
            overallStatus = ComparisonStatus.UPDATE_FAILED;
        }
    }

    public void addVerificationDetail(String detail) {
        verificationDetails.add(detail);
    }

    public void addBusinessRuleDetail(String rule) {
        businessRuleDetails.add(rule);
    }

    public String generateComparisonTable() {
        StringBuilder table = new StringBuilder();
        table.append("\n📊 TS-ACR Validation Verification for Notice: ").append(noticeNumber).append("\n");
        table.append("═".repeat(100)).append("\n");
        table.append(String.format("%-25s | %-15s | %-15s | %-30s | %s\n",
            "Field", "Expected", "Actual", "Business Rule", "Status"));
        table.append("─".repeat(100)).append("\n");

        for (FieldComparison field : fieldComparisons) {
            table.append(String.format("%-25s | %-15s | %-15s | %-30s | %s %s\n",
                field.getFieldName(),
                truncate(field.getExpectedValue(), 15),
                truncate(field.getActualValue(), 15),
                truncate(field.getBusinessRule(), 30),
                field.getStatus().getSymbol(),
                field.getStatus().name()
            ));
        }

        table.append("─".repeat(100)).append("\n");
        table.append(String.format("📈 TS-ACR Validation Summary: %d/%d fields (%.1f%%) %s\n",
            matchedFields, totalFields, matchPercentage, overallStatus.getSymbol()));

        // Business rules applied
        table.append("\n📋 TS-ACR Business Rules Applied:\n");
        if (businessRuleDetails.isEmpty()) {
            table.append("  • Standard TS-ACR suspension logic applied\n");
        } else {
            for (String rule : businessRuleDetails) {
                table.append("  • ").append(rule).append("\n");
            }
        }

        // Verification details
        if (!verificationDetails.isEmpty()) {
            table.append("\n🔍 Verification Details:\n");
            for (String detail : verificationDetails) {
                table.append("  • ").append(detail).append("\n");
            }
        }

        return table.toString();
    }

    public static TsAcrValidationComparisonResult createExpected(String noticeNumber) {
        TsAcrValidationData expectedData = TsAcrValidationData.builder()
            .noticeNo(noticeNumber)
            .suspensionType(SystemConstant.SuspensionType.TEMPORARY)
            .eprReasonOfSuspension(SystemConstant.SuspensionReason.ACR)
            .build();

        return TsAcrValidationComparisonResult.builder()
            .noticeNumber(noticeNumber)
            .expectedData(expectedData)
            .build();
    }

    public void performStandardTsAcrComparison() {
        // Standard TS-ACR comparison
        addFieldComparison("suspensionType", SystemConstant.SuspensionType.TEMPORARY,
            actualData != null ? actualData.getSuspensionType() : null,
            "TS-ACR requires suspensionType = 'TS'");

        addFieldComparison("eprReasonOfSuspension", SystemConstant.SuspensionReason.ACR,
            actualData != null ? actualData.getEprReasonOfSuspension() : null,
            "TS-ACR requires eprReasonOfSuspension = 'ACR'");

        // Add business rule details
        addBusinessRuleDetail("TS-ACR suspension applied automatically for all UEN processing");
        addBusinessRuleDetail("Temporary Suspension (TS) with ACRA (ACR) reason enforced");

        if (actualData != null) {
            addVerificationDetail("Record found in ocms_valid_offence_notice for notice: " + noticeNumber);
            addVerificationDetail("TS-ACR fields updated successfully");
        } else {
            addVerificationDetail("No record found in ocms_valid_offence_notice for notice: " + noticeNumber);
            overallStatus = ComparisonStatus.MISSING_RECORD;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "NULL";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
}