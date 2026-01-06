package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents detailed comparison result between DataHive and database company data
 */
@Data
@Builder
public class CompanyComparisonResult {

    private String uen;
    private String noticeNumber;
    private ComparisonStatus overallStatus;
    private int totalFields;
    private int matchedFields;
    private double matchPercentage;

    // DataHive source data
    private CompanyData dataHiveData;

    // Database source data
    private CompanyData databaseData;

    // Field-level comparison results
    @Builder.Default
    private List<FieldComparison> fieldComparisons = new ArrayList<>();

    // Summary details
    @Builder.Default
    private List<String> summaryDetails = new ArrayList<>();

    @Data
    @Builder
    public static class CompanyData {
        private String entityName;
        private String entityType;
        private LocalDateTime registrationDate;
        private LocalDateTime deregistrationDate;
        private String entityStatusCode;
        private String companyTypeCode;
        private String uen;
    }

    @Data
    @Builder
    public static class FieldComparison {
        private String fieldName;
        private String dataHiveValue;
        private String databaseValue;
        private ComparisonStatus status;
        private String notes;
    }

    public enum ComparisonStatus {
        MATCH("✅"),
        MISMATCH("❌"),
        MISSING_DATAHIVE("⚠️"),
        MISSING_DATABASE("❌"),
        PARTIAL_MATCH("🟡");

        private final String symbol;

        ComparisonStatus(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public void addFieldComparison(String fieldName, Object dataHiveValue, Object databaseValue) {
        String dhValue = dataHiveValue != null ? dataHiveValue.toString() : "NULL";
        String dbValue = databaseValue != null ? databaseValue.toString() : "NULL";

        ComparisonStatus status;
        if (dataHiveValue == null && databaseValue == null) {
            status = ComparisonStatus.MATCH;
        } else if (dataHiveValue == null) {
            status = ComparisonStatus.MISSING_DATAHIVE;
        } else if (databaseValue == null) {
            status = ComparisonStatus.MISSING_DATABASE;
        } else {
            status = dhValue.equals(dbValue) ? ComparisonStatus.MATCH : ComparisonStatus.MISMATCH;
        }

        FieldComparison comparison = FieldComparison.builder()
            .fieldName(fieldName)
            .dataHiveValue(dhValue)
            .databaseValue(dbValue)
            .status(status)
            .build();

        fieldComparisons.add(comparison);

        if (status == ComparisonStatus.MATCH) {
            matchedFields++;
        }
        totalFields++;

        // Calculate match percentage
        matchPercentage = totalFields > 0 ? ((double) matchedFields / totalFields) * 100 : 0;

        // Determine overall status
        overallStatus = matchedFields == totalFields ?
            ComparisonStatus.MATCH :
            (matchedFields > 0 ? ComparisonStatus.PARTIAL_MATCH : ComparisonStatus.MISMATCH);
    }

    public String generateComparisonTable() {
        StringBuilder table = new StringBuilder();
        table.append("\n📊 Company Data Comparison for UEN: ").append(uen).append("\n");
        table.append("═".repeat(80)).append("\n");
        table.append(String.format("%-20s | %-25s | %-25s | %s\n", "Field", "DataHive", "Database", "Status"));
        table.append("─".repeat(80)).append("\n");

        for (FieldComparison field : fieldComparisons) {
            table.append(String.format("%-20s | %-25s | %-25s | %s %s\n",
                field.getFieldName(),
                truncate(field.getDataHiveValue(), 25),
                truncate(field.getDatabaseValue(), 25),
                field.getStatus().getSymbol(),
                field.getStatus().name()
            ));
        }

        table.append("─".repeat(80)).append("\n");
        table.append(String.format("📈 Match Summary: %d/%d fields (%.1f%%) %s\n",
            matchedFields, totalFields, matchPercentage, overallStatus.getSymbol()));

        return table.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "NULL";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
}