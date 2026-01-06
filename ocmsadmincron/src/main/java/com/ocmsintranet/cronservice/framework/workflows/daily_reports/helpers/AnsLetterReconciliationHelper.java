package com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OCMS 10: ANS Letter Reconciliation Report Helper
 * Reconciles AN letters sent to Toppan (Control Summary Report)
 * vs letters successfully printed (Acknowledgement File)
 */
@Slf4j
@Component
public class AnsLetterReconciliationHelper {

    private final JdbcTemplate jdbcTemplate;

    public AnsLetterReconciliationHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reconciliation record for report
     */
    @Data
    public static class ReconciliationRecord {
        private String noticeNo;
        private String vehicleNo;
        private String offenceDate;
        private String letterType;
        private String dateSentToToppan;
        private String csrStatus; // From Control Summary Report
        private String ackStatus; // From Acknowledgement File
        private String reconciliationStatus; // MATCHED, MISSING, ERROR
        private String remarks;
    }

    /**
     * Reconciliation summary statistics
     */
    @Data
    public static class ReconciliationSummary {
        private int totalSentToToppan;
        private int totalPrintedSuccessfully;
        private int totalMissingInAcknowledgement;
        private int totalErrorsInPrinting;
        private double matchRate;
    }

    /**
     * Fetch AN letters sent to Toppan from ocms_an_letter table
     *
     * @param processDate Date of the CSR/ACK files being reconciled
     * @return List of letters sent to Toppan
     */
    public List<Map<String, Object>> fetchLettersSentToToppan(String processDate) {
        log.info("[ANS Letter Reconciliation Helper] Fetching letters sent to Toppan for date: {}", processDate);

        String sql = "SELECT " +
                "notice_no, " +
                "vehicle_registration_no, " +
                "offence_date, " +
                "letter_type, " +
                "created_on, " +
                "status " +
                "FROM ocmsizmgr.ocms_an_letter " +
                "WHERE CONVERT(date, created_on) = ? " +
                "ORDER BY notice_no";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, processDate);
        log.info("[ANS Letter Reconciliation Helper] Found {} letters sent to Toppan", records.size());

        return records;
    }

    /**
     * Parse Toppan Acknowledgement File content
     *
     * @param ackFileContent Content of the acknowledgement file
     * @return Map of notice_no -> status (SUCCESS/ERROR)
     */
    public Map<String, String> parseAcknowledgementFile(String ackFileContent) {
        log.info("[ANS Letter Reconciliation Helper] Parsing acknowledgement file");

        Map<String, String> ackMap = new HashMap<>();

        if (ackFileContent == null || ackFileContent.isEmpty()) {
            log.warn("[ANS Letter Reconciliation Helper] Acknowledgement file content is empty");
            return ackMap;
        }

        String[] lines = ackFileContent.split("\\r?\\n");

        for (String line : lines) {
            // Skip header or empty lines
            if (line.trim().isEmpty() || line.startsWith("NOTICE_NO")) {
                continue;
            }

            // Expected format: NOTICE_NO,STATUS (e.g., "N202501010001,SUCCESS")
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String noticeNo = parts[0].trim();
                String status = parts[1].trim();
                ackMap.put(noticeNo, status);
            }
        }

        log.info("[ANS Letter Reconciliation Helper] Parsed {} acknowledgement records", ackMap.size());
        return ackMap;
    }

    /**
     * Perform reconciliation between CSR (sent) and ACK (printed)
     *
     * @param sentLetters Letters sent to Toppan
     * @param ackMap Acknowledgement map (notice_no -> status)
     * @return List of reconciliation records
     */
    public List<ReconciliationRecord> performReconciliation(
            List<Map<String, Object>> sentLetters,
            Map<String, String> ackMap) {

        log.info("[ANS Letter Reconciliation Helper] Performing reconciliation");

        List<ReconciliationRecord> records = new ArrayList<>();

        for (Map<String, Object> letter : sentLetters) {
            ReconciliationRecord record = new ReconciliationRecord();

            String noticeNo = (String) letter.get("notice_no");
            record.setNoticeNo(noticeNo);
            record.setVehicleNo((String) letter.get("vehicle_registration_no"));
            record.setOffenceDate(letter.get("offence_date") != null ? letter.get("offence_date").toString() : "");
            record.setLetterType((String) letter.get("letter_type"));
            record.setDateSentToToppan(letter.get("created_on") != null ? letter.get("created_on").toString() : "");
            record.setCsrStatus((String) letter.get("status"));

            // Check if letter exists in acknowledgement file
            String ackStatus = ackMap.get(noticeNo);
            record.setAckStatus(ackStatus != null ? ackStatus : "NOT_FOUND");

            // Determine reconciliation status
            if (ackStatus == null) {
                record.setReconciliationStatus("MISSING");
                record.setRemarks("Letter not found in acknowledgement file");
            } else if ("SUCCESS".equalsIgnoreCase(ackStatus)) {
                record.setReconciliationStatus("MATCHED");
                record.setRemarks("Successfully printed");
            } else if ("ERROR".equalsIgnoreCase(ackStatus)) {
                record.setReconciliationStatus("ERROR");
                record.setRemarks("Printing error reported by Toppan");
            } else {
                record.setReconciliationStatus("UNKNOWN");
                record.setRemarks("Unknown status: " + ackStatus);
            }

            records.add(record);
        }

        log.info("[ANS Letter Reconciliation Helper] Reconciliation completed: {} records", records.size());
        return records;
    }

    /**
     * Calculate summary statistics from reconciliation records
     */
    public ReconciliationSummary calculateSummary(List<ReconciliationRecord> records) {
        ReconciliationSummary summary = new ReconciliationSummary();

        summary.setTotalSentToToppan(records.size());

        long printedSuccessfully = records.stream()
                .filter(r -> "MATCHED".equals(r.getReconciliationStatus()))
                .count();
        summary.setTotalPrintedSuccessfully((int) printedSuccessfully);

        long missing = records.stream()
                .filter(r -> "MISSING".equals(r.getReconciliationStatus()))
                .count();
        summary.setTotalMissingInAcknowledgement((int) missing);

        long errors = records.stream()
                .filter(r -> "ERROR".equals(r.getReconciliationStatus()))
                .count();
        summary.setTotalErrorsInPrinting((int) errors);

        if (summary.getTotalSentToToppan() > 0) {
            summary.setMatchRate((double) printedSuccessfully / summary.getTotalSentToToppan() * 100);
        } else {
            summary.setMatchRate(0.0);
        }

        return summary;
    }

    /**
     * Generate Excel report with reconciliation details
     *
     * @param records Reconciliation records
     * @param summary Summary statistics
     * @param processDate Date of reconciliation
     * @return Excel file as byte array
     */
    public byte[] generateExcelReport(
            List<ReconciliationRecord> records,
            ReconciliationSummary summary,
            String processDate) {

        log.info("[ANS Letter Reconciliation Helper] Generating Excel report for {} records", records.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, summary, processDate, workbook);

            // Create Details Sheet
            Sheet detailsSheet = workbook.createSheet("Reconciliation Details");
            createDetailsSheet(detailsSheet, records, workbook);

            workbook.write(out);
            log.info("[ANS Letter Reconciliation Helper] Excel report generated successfully");
            return out.toByteArray();

        } catch (Exception e) {
            log.error("[ANS Letter Reconciliation Helper] Error generating Excel report", e);
            return null;
        }
    }

    /**
     * Create summary sheet with statistics
     */
    private void createSummarySheet(Sheet sheet, ReconciliationSummary summary, String processDate, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ANS Letter Reconciliation Report - Summary");
        titleCell.setCellStyle(headerStyle);

        // Process Date
        rowNum++;
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Process Date:");
        dateRow.createCell(1).setCellValue(processDate);

        // Generation Time
        Row genTimeRow = sheet.createRow(rowNum++);
        genTimeRow.createCell(0).setCellValue("Generated At:");
        genTimeRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Statistics
        rowNum++;
        Row statHeaderRow = sheet.createRow(rowNum++);
        statHeaderRow.createCell(0).setCellValue("Metric");
        statHeaderRow.createCell(1).setCellValue("Count");
        statHeaderRow.getCell(0).setCellStyle(headerStyle);
        statHeaderRow.getCell(1).setCellStyle(headerStyle);

        Row totalSentRow = sheet.createRow(rowNum++);
        totalSentRow.createCell(0).setCellValue("Total Letters Sent to Toppan");
        totalSentRow.createCell(1).setCellValue(summary.getTotalSentToToppan());

        Row printedRow = sheet.createRow(rowNum++);
        printedRow.createCell(0).setCellValue("Successfully Printed");
        printedRow.createCell(1).setCellValue(summary.getTotalPrintedSuccessfully());

        Row missingRow = sheet.createRow(rowNum++);
        missingRow.createCell(0).setCellValue("Missing in Acknowledgement");
        missingRow.createCell(1).setCellValue(summary.getTotalMissingInAcknowledgement());

        Row errorRow = sheet.createRow(rowNum++);
        errorRow.createCell(0).setCellValue("Printing Errors");
        errorRow.createCell(1).setCellValue(summary.getTotalErrorsInPrinting());

        Row matchRateRow = sheet.createRow(rowNum++);
        matchRateRow.createCell(0).setCellValue("Match Rate (%)");
        matchRateRow.createCell(1).setCellValue(String.format("%.2f%%", summary.getMatchRate()));

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create details sheet with reconciliation records
     */
    private void createDetailsSheet(Sheet sheet, List<ReconciliationRecord> records, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Notice No", "Vehicle No", "Offence Date", "Letter Type",
                "Date Sent to Toppan", "CSR Status", "ACK Status",
                "Reconciliation Status", "Remarks"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (ReconciliationRecord record : records) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(record.getNoticeNo());
            row.createCell(1).setCellValue(record.getVehicleNo());
            row.createCell(2).setCellValue(record.getOffenceDate());
            row.createCell(3).setCellValue(record.getLetterType());
            row.createCell(4).setCellValue(record.getDateSentToToppan());
            row.createCell(5).setCellValue(record.getCsrStatus());
            row.createCell(6).setCellValue(record.getAckStatus());
            row.createCell(7).setCellValue(record.getReconciliationStatus());
            row.createCell(8).setCellValue(record.getRemarks());

            // Apply color coding based on reconciliation status
            CellStyle statusStyle = createDataStyle(workbook);
            if ("MATCHED".equals(record.getReconciliationStatus())) {
                statusStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else if ("ERROR".equals(record.getReconciliationStatus())) {
                statusStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
                statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else if ("MISSING".equals(record.getReconciliationStatus())) {
                statusStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
                statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            row.getCell(7).setCellStyle(statusStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Create data cell style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
