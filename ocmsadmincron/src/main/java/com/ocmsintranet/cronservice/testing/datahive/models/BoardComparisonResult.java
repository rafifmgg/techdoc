package com.ocmsintranet.cronservice.testing.datahive.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents detailed comparison result between DataHive and database board member data
 */
@Data
@Builder
public class BoardComparisonResult {

    private String entityUen;
    private String noticeNumber;
    private ComparisonStatus overallStatus;
    private int totalRecords;
    private int matchedRecords;
    private double matchPercentage;

    @Builder.Default
    private List<BoardRecordComparison> recordComparisons = new ArrayList<>();

    @Builder.Default
    private List<String> summaryDetails = new ArrayList<>();

    @Data
    @Builder
    public static class BoardRecordComparison {
        private String personIdNo;
        private String positionHeldCode;
        private LocalDateTime positionAppointmentDate;
        private LocalDateTime positionWithdrawnDate;
        private LocalDateTime referencePeriod;
        private BoardData dataHiveData;
        private BoardData databaseData;
        private ComparisonStatus status;
        @Builder.Default
        private List<FieldComparison> fieldComparisons = new ArrayList<>();
    }

    @Data
    @Builder
    public static class BoardData {
        private String personIdNo;
        private String positionHeldCode;
        private LocalDateTime positionAppointmentDate;
        private LocalDateTime positionWithdrawnDate;
        private LocalDateTime referencePeriod;
        private String entityUen;
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

    public void addRecordComparison(BoardRecordComparison comparison) {
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
        table.append("\n📊 Board Member Data Comparison for UEN: ").append(entityUen).append("\n");
        table.append("═".repeat(120)).append("\n");
        table.append(String.format("%-12s | %-15s | %-20s | %-20s | %-20s | %s\n",
            "Person ID", "Position", "DH Appointment", "DB Appointment", "DH Withdrawn", "Status"));
        table.append("─".repeat(120)).append("\n");

        for (BoardRecordComparison record : recordComparisons) {
            String dhAppointment = formatDate(record.getDataHiveData() != null ?
                record.getDataHiveData().getPositionAppointmentDate() : null);
            String dbAppointment = formatDate(record.getDatabaseData() != null ?
                record.getDatabaseData().getPositionAppointmentDate() : null);
            String dhWithdrawn = formatDate(record.getDataHiveData() != null ?
                record.getDataHiveData().getPositionWithdrawnDate() : null);

            table.append(String.format("%-12s | %-15s | %-20s | %-20s | %-20s | %s %s\n",
                truncate(record.getPersonIdNo(), 12),
                truncate(record.getPositionHeldCode(), 15),
                dhAppointment,
                dbAppointment,
                dhWithdrawn,
                record.getStatus().getSymbol(),
                record.getStatus().name()
            ));
        }

        table.append("─".repeat(120)).append("\n");
        table.append(String.format("📈 Match Summary: %d/%d records (%.1f%%) %s\n",
            matchedRecords, totalRecords, matchPercentage, overallStatus.getSymbol()));

        // Add detailed field comparisons if there are mismatches
        if (overallStatus != ComparisonStatus.MATCH) {
            table.append("\n🔍 Detailed Field Comparisons:\n");
            for (BoardRecordComparison record : recordComparisons) {
                if (record.getStatus() != ComparisonStatus.MATCH && !record.getFieldComparisons().isEmpty()) {
                    table.append(String.format("\n  📋 Board Member: %s, Position: %s\n",
                        record.getPersonIdNo(), record.getPositionHeldCode()));
                    for (FieldComparison field : record.getFieldComparisons()) {
                        table.append(String.format("    %-25s | %-20s | %-20s | %s %s\n",
                            field.getFieldName(),
                            truncate(field.getDataHiveValue(), 20),
                            truncate(field.getDatabaseValue(), 20),
                            field.getStatus().getSymbol(),
                            field.getNotes() != null ? field.getNotes() : ""
                        ));
                    }
                }
            }
        }

        return table.toString();
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "NULL";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "NULL";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
}