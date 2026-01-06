package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

/**
 * Represents detailed comparison result for suspension record creation in ocms_suspended_notice table
 */
@Data
@Builder
public class SuspensionRecordComparisonResult {

    private String noticeNumber;
    private ComparisonStatus overallStatus;
    private int totalFields;
    private int matchedFields;
    private double matchPercentage;

    // Expected vs actual suspension record data
    private SuspensionRecordData expectedData;
    private SuspensionRecordData actualData;

    @Builder.Default
    private List<FieldComparison> fieldComparisons = new ArrayList<>();

    @Builder.Default
    private List<String> verificationDetails = new ArrayList<>();

    @Builder.Default
    private List<String> businessRuleDetails = new ArrayList<>();

    @Data
    @Builder
    public static class SuspensionRecordData {
        private String noticeNo;
        private LocalDateTime dateOfSuspension;
        private Integer srNo;
        private String suspensionSource;
        private String suspensionType;
        private String reasonOfSuspension;
        private LocalDate dueDateOfRevival;
        private String suspensionRemarks;
        private String officerAuthorisingSuspension;
        private String creUserId;
    }

    @Data
    @Builder
    public static class FieldComparison {
        private String fieldName;
        private String expectedValue;
        private String actualValue;
        private ComparisonStatus status;
        private String businessRule;
        private String notes;
    }

    public enum ComparisonStatus {
        MATCH("✅"),
        MISMATCH("❌"),
        MISSING_RECORD("❌"),
        CREATION_FAILED("❌"),
        PARTIAL_MATCH("🟡"),
        DATE_VARIANCE_ACCEPTABLE("⚠️");

        private final String symbol;

        ComparisonStatus(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public void addFieldComparison(String fieldName, Object expectedValue, Object actualValue, String businessRule) {
        addFieldComparison(fieldName, expectedValue, actualValue, businessRule, null);
    }

    public void addFieldComparison(String fieldName, Object expectedValue, Object actualValue, String businessRule, String notes) {
        String expectedStr = formatValue(expectedValue);
        String actualStr = formatValue(actualValue);

        ComparisonStatus status;
        if (expectedValue == null && actualValue == null) {
            status = ComparisonStatus.MATCH;
        } else if (expectedValue == null || actualValue == null) {
            status = ComparisonStatus.MISMATCH;
        } else {
            // Special handling for date fields (allow small time variance)
            if (fieldName.contains("Date") && expectedValue instanceof LocalDateTime && actualValue instanceof LocalDateTime) {
                LocalDateTime expected = (LocalDateTime) expectedValue;
                LocalDateTime actual = (LocalDateTime) actualValue;
                long secondsDiff = Math.abs(java.time.Duration.between(expected, actual).getSeconds());

                if (secondsDiff <= 60) { // Allow 1 minute variance
                    status = ComparisonStatus.DATE_VARIANCE_ACCEPTABLE;
                } else {
                    status = ComparisonStatus.MISMATCH;
                }
            } else {
                status = expectedStr.equals(actualStr) ? ComparisonStatus.MATCH : ComparisonStatus.MISMATCH;
            }
        }

        FieldComparison comparison = FieldComparison.builder()
            .fieldName(fieldName)
            .expectedValue(expectedStr)
            .actualValue(actualStr)
            .status(status)
            .businessRule(businessRule)
            .notes(notes)
            .build();

        fieldComparisons.add(comparison);

        if (status == ComparisonStatus.MATCH || status == ComparisonStatus.DATE_VARIANCE_ACCEPTABLE) {
            matchedFields++;
        }
        totalFields++;

        // Calculate match percentage
        matchPercentage = totalFields > 0 ? ((double) matchedFields / totalFields) * 100 : 0;

        // Determine overall status
        if (matchedFields == totalFields) {
            overallStatus = ComparisonStatus.MATCH;
        } else if (matchedFields > 0) {
            overallStatus = ComparisonStatus.PARTIAL_MATCH;
        } else {
            overallStatus = ComparisonStatus.CREATION_FAILED;
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
        table.append("\n📊 Suspension Record Verification for Notice: ").append(noticeNumber).append("\n");
        table.append("═".repeat(120)).append("\n");
        table.append(String.format("%-20s | %-20s | %-20s | %-35s | %s\n",
            "Field", "Expected", "Actual", "Business Rule", "Status"));
        table.append("─".repeat(120)).append("\n");

        for (FieldComparison field : fieldComparisons) {
            table.append(String.format("%-20s | %-20s | %-20s | %-35s | %s %s\n",
                field.getFieldName(),
                truncate(field.getExpectedValue(), 20),
                truncate(field.getActualValue(), 20),
                truncate(field.getBusinessRule(), 35),
                field.getStatus().getSymbol(),
                field.getStatus().name()
            ));

            if (field.getNotes() != null) {
                table.append(String.format("%-20s | %-20s | %-20s | %-35s | %s\n",
                    "", "", "", "Note: " + field.getNotes(), ""));
            }
        }

        table.append("─".repeat(120)).append("\n");
        table.append(String.format("📈 Suspension Record Summary: %d/%d fields (%.1f%%) %s\n",
            matchedFields, totalFields, matchPercentage, overallStatus.getSymbol()));

        // Business rules applied
        table.append("\n📋 TS-ACR Suspension Business Rules:\n");
        if (businessRuleDetails.isEmpty()) {
            table.append("  • Standard TS-ACR suspension record creation\n");
            table.append("  • 30-day revival period automatically calculated\n");
            table.append("  • Auto-generated sr_no for suspension tracking\n");
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

    public static SuspensionRecordComparisonResult createExpected(String noticeNumber, Integer expectedSrNo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate revivalDate = now.toLocalDate().plusDays(30);

        SuspensionRecordData expectedData = SuspensionRecordData.builder()
            .noticeNo(noticeNumber)
            .dateOfSuspension(now)
            .srNo(expectedSrNo)
            .suspensionSource(SystemConstant.Subsystem.OCMS_CODE)
            .suspensionType(SystemConstant.SuspensionType.TEMPORARY)
            .reasonOfSuspension(SystemConstant.SuspensionReason.ACR)
            .dueDateOfRevival(revivalDate)
            .suspensionRemarks("TS-ACR")
            .officerAuthorisingSuspension(SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
            .creUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
            .build();

        return SuspensionRecordComparisonResult.builder()
            .noticeNumber(noticeNumber)
            .expectedData(expectedData)
            .build();
    }

    public void performStandardSuspensionComparison() {
        if (expectedData == null) {
            addVerificationDetail("Expected data not initialized");
            overallStatus = ComparisonStatus.CREATION_FAILED;
            return;
        }

        // Standard suspension record field comparisons
        addFieldComparison("noticeNo", expectedData.getNoticeNo(),
            actualData != null ? actualData.getNoticeNo() : null,
            "Notice number must match exactly");

        addFieldComparison("suspensionSource", expectedData.getSuspensionSource(),
            actualData != null ? actualData.getSuspensionSource() : null,
            "Suspension source must be 'OCMS'");

        addFieldComparison("suspensionType", expectedData.getSuspensionType(),
            actualData != null ? actualData.getSuspensionType() : null,
            "TS-ACR requires suspensionType = 'TS'");

        addFieldComparison("reasonOfSuspension", expectedData.getReasonOfSuspension(),
            actualData != null ? actualData.getReasonOfSuspension() : null,
            "TS-ACR requires reasonOfSuspension = 'ACR'");

        addFieldComparison("suspensionRemarks", expectedData.getSuspensionRemarks(),
            actualData != null ? actualData.getSuspensionRemarks() : null,
            "Suspension remarks should be 'TS-ACR'");

        addFieldComparison("officerAuthorisingSuspension", expectedData.getOfficerAuthorisingSuspension(),
            actualData != null ? actualData.getOfficerAuthorisingSuspension() : null,
            "Officer must be 'ocmsiz_app_conn'");

        addFieldComparison("creUserId", expectedData.getCreUserId(),
            actualData != null ? actualData.getCreUserId() : null,
            "Creator must be 'ocmsiz_app_conn'");

        // Date comparisons with tolerance
        addFieldComparison("dateOfSuspension", expectedData.getDateOfSuspension(),
            actualData != null ? actualData.getDateOfSuspension() : null,
            "Suspension date should be current timestamp",
            "Allow up to 1 minute variance");

        addFieldComparison("dueDateOfRevival", expectedData.getDueDateOfRevival(),
            actualData != null ? actualData.getDueDateOfRevival() : null,
            "Revival date should be 30 days from suspension");

        addFieldComparison("srNo", expectedData.getSrNo(),
            actualData != null ? actualData.getSrNo() : null,
            "Sr No should be auto-generated sequential number");

        // Add business rule details
        addBusinessRuleDetail("TS-ACR suspension automatically creates suspension record");
        addBusinessRuleDetail("30-day revival period calculated from suspension date");
        addBusinessRuleDetail("Sequential sr_no generated for notice tracking");
        addBusinessRuleDetail("System user 'ocmsiz_app_conn' assigned as creator and authorizer");

        if (actualData != null) {
            addVerificationDetail("Suspension record found in ocms_suspended_notice for notice: " + noticeNumber);
            addVerificationDetail("TS-ACR suspension record created successfully");
        } else {
            addVerificationDetail("No suspension record found in ocms_suspended_notice for notice: " + noticeNumber);
            overallStatus = ComparisonStatus.MISSING_RECORD;
        }
    }

    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toString();
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).toString();
        }
        return value.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "NULL";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
}