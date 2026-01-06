package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents detailed comparison result for gazetted flag updates in ocms_offence_notice_owner_driver table
 */
@Data
@Builder
public class GazettedFlagComparisonResult {

    private String noticeNumber;
    private String entityType;
    private boolean isListedCompany;
    private ComparisonStatus overallStatus;
    private String expectedGazettedFlag;
    private String actualGazettedFlag;
    private boolean gazettedFlagMatched;

    // Before and after gazetted flag states
    private GazettedFlagData beforeUpdate;
    private GazettedFlagData afterUpdate;

    @Builder.Default
    private List<String> verificationDetails = new ArrayList<>();

    @Builder.Default
    private List<String> businessRuleDetails = new ArrayList<>();

    @Data
    @Builder
    public static class GazettedFlagData {
        private String gazettedFlag;
        private String updUserId;
        private LocalDateTime updDate;
        private String noticeNo;
        private String ownerDriverIndicator;
    }

    public enum ComparisonStatus {
        MATCH("✅"),
        MISMATCH("❌"),
        SKIPPED_NON_LC("⚠️"),
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

    public void addVerificationDetail(String detail) {
        verificationDetails.add(detail);
    }

    public void addBusinessRuleDetail(String rule) {
        businessRuleDetails.add(rule);
    }

    public String generateComparisonTable() {
        StringBuilder table = new StringBuilder();
        table.append("\n📊 Gazetted Flag Verification for Notice: ").append(noticeNumber).append("\n");
        table.append("═".repeat(90)).append("\n");
        table.append(String.format("%-25s | %-15s | %-15s | %-15s | %s\n",
            "Verification Point", "Expected", "Actual", "Entity Type", "Status"));
        table.append("─".repeat(90)).append("\n");

        // Business rule verification
        table.append(String.format("%-25s | %-15s | %-15s | %-15s | %s %s\n",
            "Listed Company Check",
            isListedCompany ? "LC" : "Non-LC",
            entityType != null ? entityType : "NULL",
            entityType != null ? entityType : "NULL",
            entityType != null && "LC".equals(entityType) == isListedCompany ? "✅" : "❌",
            entityType != null && "LC".equals(entityType) == isListedCompany ? "MATCH" : "MISMATCH"));

        // Gazetted flag verification
        table.append(String.format("%-25s | %-15s | %-15s | %-15s | %s %s\n",
            "Gazetted Flag Update",
            expectedGazettedFlag != null ? expectedGazettedFlag : "N/A",
            actualGazettedFlag != null ? actualGazettedFlag : "NULL",
            entityType != null ? entityType : "NULL",
            overallStatus.getSymbol(),
            overallStatus.name()));

        table.append("─".repeat(90)).append("\n");

        // Business rule summary
        table.append("📋 Business Rules Applied:\n");
        if (businessRuleDetails.isEmpty()) {
            table.append("  • No specific business rules documented\n");
        } else {
            for (String rule : businessRuleDetails) {
                table.append("  • ").append(rule).append("\n");
            }
        }

        // Overall result
        table.append(String.format("\n📈 Gazetted Flag Verification Result: %s %s\n",
            overallStatus.getSymbol(), overallStatus.name()));

        return table.toString();
    }

    public static GazettedFlagComparisonResult createForListedCompany(String noticeNumber, String entityType) {
        return GazettedFlagComparisonResult.builder()
            .noticeNumber(noticeNumber)
            .entityType(entityType)
            .isListedCompany(true)
            .expectedGazettedFlag("Y")
            .build();
    }

    public static GazettedFlagComparisonResult createForNonListedCompany(String noticeNumber, String entityType) {
        return GazettedFlagComparisonResult.builder()
            .noticeNumber(noticeNumber)
            .entityType(entityType)
            .isListedCompany(false)
            .expectedGazettedFlag(null) // No update expected
            .overallStatus(ComparisonStatus.SKIPPED_NON_LC)
            .build();
    }

    public void evaluateComparison() {
        if (!isListedCompany) {
            // Non-LC companies should not have gazetted flag updated
            overallStatus = ComparisonStatus.SKIPPED_NON_LC;
            gazettedFlagMatched = true; // Not applicable
            addBusinessRuleDetail("Non-LC company: gazetted flag update skipped as expected");
        } else {
            // LC companies should have gazetted flag set to 'Y'
            if ("Y".equals(actualGazettedFlag)) {
                overallStatus = ComparisonStatus.MATCH;
                gazettedFlagMatched = true;
                addBusinessRuleDetail("LC company: gazetted flag correctly set to 'Y'");
            } else {
                overallStatus = ComparisonStatus.MISMATCH;
                gazettedFlagMatched = false;
                addBusinessRuleDetail("LC company: gazetted flag should be 'Y' but found: " +
                    (actualGazettedFlag != null ? actualGazettedFlag : "NULL"));
            }
        }
    }
}