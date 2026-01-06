package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents detailed comparison result between DataHive and database shareholder data
 */
@Data
@Builder
public class ShareholderComparisonResult {

    private String companyUen;
    private String noticeNumber;
    private ComparisonStatus overallStatus;
    private int totalRecords;
    private int matchedRecords;
    private double matchPercentage;

    @Builder.Default
    private List<ShareholderRecordComparison> recordComparisons = new ArrayList<>();

    @Builder.Default
    private List<String> summaryDetails = new ArrayList<>();

    @Data
    @Builder
    public static class ShareholderRecordComparison {
        private String category;
        private String personIdNo;
        private String companyProfileUen;
        private Integer shareAllottedNo;
        private ShareholderData dataHiveData;
        private ShareholderData databaseData;
        private ComparisonStatus status;
        @Builder.Default
        private List<FieldComparison> fieldComparisons = new ArrayList<>();
    }

    @Data
    @Builder
    public static class ShareholderData {
        private String category;
        private String personIdNo;
        private String companyProfileUen;
        private Integer shareAllottedNo;
        private String companyUen;
    }

    @Data
    @Builder
    public static class FieldComparison {
        private String fieldName;
        private String dataHiveValue;
        private String databaseValue;
        private ComparisonStatus status;
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

    public void addRecordComparison(ShareholderRecordComparison comparison) {
        recordComparisons.add(comparison);
        totalRecords++;

        if (comparison.getStatus() == ComparisonStatus.MATCH) {
            matchedRecords++;
        }

        // Calculate match percentage
        matchPercentage = totalRecords > 0 ? ((double) matchedRecords / totalRecords) * 100 : 0;

        // Determine overall status
        overallStatus = matchedRecords == totalRecords ?
            ComparisonStatus.MATCH :
            (matchedRecords > 0 ? ComparisonStatus.PARTIAL_MATCH : ComparisonStatus.MISMATCH);
    }

    public String generateComparisonTable() {
        StringBuilder table = new StringBuilder();
        table.append("\n📊 Shareholder Data Comparison for UEN: ").append(companyUen).append("\n");
        table.append("═".repeat(100)).append("\n");
        table.append(String.format("%-15s | %-12s | %-15s | %-12s | %-12s | %s\n",
            "Category", "Person ID", "Profile UEN", "DH Shares", "DB Shares", "Status"));
        table.append("─".repeat(100)).append("\n");

        for (ShareholderRecordComparison record : recordComparisons) {
            String dhShares = record.getDataHiveData() != null && record.getDataHiveData().getShareAllottedNo() != null ?
                record.getDataHiveData().getShareAllottedNo().toString() : "NULL";
            String dbShares = record.getDatabaseData() != null && record.getDatabaseData().getShareAllottedNo() != null ?
                record.getDatabaseData().getShareAllottedNo().toString() : "NULL";

            table.append(String.format("%-15s | %-12s | %-15s | %-12s | %-12s | %s %s\n",
                truncate(record.getCategory(), 15),
                truncate(record.getPersonIdNo(), 12),
                truncate(record.getCompanyProfileUen(), 15),
                dhShares,
                dbShares,
                record.getStatus().getSymbol(),
                record.getStatus().name()
            ));
        }

        table.append("─".repeat(100)).append("\n");
        table.append(String.format("📈 Match Summary: %d/%d records (%.1f%%) %s\n",
            matchedRecords, totalRecords, matchPercentage, overallStatus.getSymbol()));

        // Add detailed field comparisons if there are mismatches
        if (overallStatus != ComparisonStatus.MATCH) {
            table.append("\n🔍 Detailed Field Comparisons:\n");
            for (ShareholderRecordComparison record : recordComparisons) {
                if (record.getStatus() != ComparisonStatus.MATCH && !record.getFieldComparisons().isEmpty()) {
                    table.append(String.format("\n  📋 Record Category: %s, Person ID: %s\n",
                        record.getCategory(), record.getPersonIdNo()));
                    for (FieldComparison field : record.getFieldComparisons()) {
                        table.append(String.format("    %-20s | %-15s | %-15s | %s\n",
                            field.getFieldName(),
                            truncate(field.getDataHiveValue(), 15),
                            truncate(field.getDatabaseValue(), 15),
                            field.getStatus().getSymbol()
                        ));
                    }
                }
            }
        }

        return table.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "NULL";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
}