package com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers;

import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.models.ClassifiedVehicleReportRecord;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.models.ClassifiedVehicleReportSummary;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OCMS 14: Helper for Classified Vehicle (VIP) Report
 * Handles data fetching and Excel generation for daily Type V vehicle report
 */
@Slf4j
@Component
public class ClassifiedVehicleReportHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;

    @Autowired
    public ClassifiedVehicleReportHelper(OcmsValidOffenceNoticeRepository validOffenceNoticeRepository) {
        this.validOffenceNoticeRepository = validOffenceNoticeRepository;
    }

    /**
     * Fetch all Type V notices and amended notices for Classified Vehicle Report
     *
     * @return Map containing Type V notices and amended notices
     */
    public Map<String, Object> fetchClassifiedVehicleRecords() {
        log.info("[Classified Vehicle Report] Fetching Type V and amended notice records");

        try {
            // Fetch Type V notices
            List<Map<String, Object>> rawTypeVNotices = validOffenceNoticeRepository.findAllTypeVNotices();
            log.info("[Classified Vehicle Report] Found {} Type V notices", rawTypeVNotices.size());

            // Fetch amended notices (V→S)
            List<Map<String, Object>> rawAmendedNotices = validOffenceNoticeRepository.findTypeVAmendedToS();
            log.info("[Classified Vehicle Report] Found {} amended notices (V→S)", rawAmendedNotices.size());

            // Convert to model objects
            List<ClassifiedVehicleReportRecord> typeVRecords = convertToTypeVRecords(rawTypeVNotices);
            List<ClassifiedVehicleReportRecord> amendedRecords = convertToAmendedRecords(rawAmendedNotices);

            // Calculate summary
            ClassifiedVehicleReportSummary summary = calculateSummary(typeVRecords, amendedRecords);

            // Prepare result
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("typeVRecords", typeVRecords);
            result.put("amendedRecords", amendedRecords);
            result.put("summary", summary);

            log.info("[Classified Vehicle Report] Summary - Total: {}, Outstanding: {}, Settled: {}, Amended: {}",
                    summary.getTotalNoticesIssued(), summary.getOutstandingNotices(),
                    summary.getSettledNotices(), summary.getAmendedNotices());

            return result;

        } catch (Exception e) {
            log.error("[Classified Vehicle Report] Error fetching records: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Classified Vehicle records", e);
        }
    }

    /**
     * Generate Excel report with 3 sheets: Summary, Type V Detail, Amended Notices
     *
     * @param reportData Map containing Type V records, amended records, and summary
     * @param reportDate Report generation date
     * @return Byte array containing Excel file
     */
    @SuppressWarnings("unchecked")
    public byte[] generateExcelReport(Map<String, Object> reportData, String reportDate) {
        log.info("[Classified Vehicle Report] Generating Excel report");

        List<ClassifiedVehicleReportRecord> typeVRecords =
                (List<ClassifiedVehicleReportRecord>) reportData.get("typeVRecords");
        List<ClassifiedVehicleReportRecord> amendedRecords =
                (List<ClassifiedVehicleReportRecord>) reportData.get("amendedRecords");
        ClassifiedVehicleReportSummary summary =
                (ClassifiedVehicleReportSummary) reportData.get("summary");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Sheet 1: Summary
            createSummarySheet(workbook, summary, headerStyle, dataStyle);

            // Sheet 2: Type V Detail
            createTypeVDetailSheet(workbook, typeVRecords, headerStyle, dataStyle, currencyStyle);

            // Sheet 3: Amended Notices
            createAmendedNoticesSheet(workbook, amendedRecords, headerStyle, dataStyle, currencyStyle);

            // Write to output stream
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            log.info("[Classified Vehicle Report] Excel report generated successfully: {} bytes", excelBytes.length);
            return excelBytes;

        } catch (Exception e) {
            log.error("[Classified Vehicle Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Classified Vehicle Excel report", e);
        }
    }

    /**
     * Create Summary sheet
     */
    private void createSummarySheet(Workbook workbook, ClassifiedVehicleReportSummary summary,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Summary");

        int rowNum = 0;

        // Title row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Classified Vehicle Notices Report - Summary");
        titleCell.setCellStyle(headerStyle);

        // Empty row
        rowNum++;

        // Report Date
        Row dateRow = sheet.createRow(rowNum++);
        createKeyValueRow(dateRow, "Report Date", summary.getReportDate().format(DATE_FORMATTER), headerStyle, dataStyle);

        // Empty row
        rowNum++;

        // Statistics
        createKeyValueRow(sheet.createRow(rowNum++), "Total Notices Issued (Type V)",
                String.valueOf(summary.getTotalNoticesIssued()), headerStyle, dataStyle);
        createKeyValueRow(sheet.createRow(rowNum++), "Outstanding Notices (Unpaid)",
                String.valueOf(summary.getOutstandingNotices()), headerStyle, dataStyle);
        createKeyValueRow(sheet.createRow(rowNum++), "Settled Notices (Paid)",
                String.valueOf(summary.getSettledNotices()), headerStyle, dataStyle);
        createKeyValueRow(sheet.createRow(rowNum++), "Notices Amended (V→S)",
                String.valueOf(summary.getAmendedNotices()), headerStyle, dataStyle);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0) + 2000);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1) + 2000);
    }

    /**
     * Create Type V Detail sheet
     */
    private void createTypeVDetailSheet(Workbook workbook, List<ClassifiedVehicleReportRecord> records,
                                       CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Type V Notices Detail");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "S/N", "Notice No", "Vehicle No", "Notice Date", "Offence Code",
            "Place of Offence", "Composition Amount ($)", "Amount Payable ($)",
            "Payment Status", "Suspension Type", "Suspension Reason"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Populate data rows
        int rowNum = 1;
        int serialNumber = 1;
        for (ClassifiedVehicleReportRecord record : records) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, serialNumber++, dataStyle);
            createCell(row, 1, record.getNoticeNo(), dataStyle);
            createCell(row, 2, record.getVehicleNo(), dataStyle);
            createCell(row, 3, formatDateTime(record.getNoticeDate()), dataStyle);
            createCell(row, 4, record.getOffenceRuleCode(), dataStyle);
            createCell(row, 5, record.getPpName(), dataStyle);
            createCurrencyCell(row, 6, record.getCompositionAmount(), currencyStyle);
            createCurrencyCell(row, 7, record.getAmountPayable(), currencyStyle);
            createCell(row, 8, record.getPaymentStatus(), dataStyle);
            createCell(row, 9, record.getSuspensionType(), dataStyle);
            createCell(row, 10, record.getSuspensionReason(), dataStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    /**
     * Create Amended Notices sheet
     */
    private void createAmendedNoticesSheet(Workbook workbook, List<ClassifiedVehicleReportRecord> records,
                                          CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Amended Notices (V→S)");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "S/N", "Notice No", "Vehicle No", "Notice Date", "Original Type",
            "Amended Type", "Suspension Date (TS-CLV)", "Revival Date (TS-CLV)",
            "Place of Offence", "Composition Amount ($)", "Amount Payable ($)"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Populate data rows
        int rowNum = 1;
        int serialNumber = 1;
        for (ClassifiedVehicleReportRecord record : records) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, serialNumber++, dataStyle);
            createCell(row, 1, record.getNoticeNo(), dataStyle);
            createCell(row, 2, record.getVehicleNo(), dataStyle);
            createCell(row, 3, formatDateTime(record.getNoticeDate()), dataStyle);
            createCell(row, 4, "V (VIP)", dataStyle); // Original type
            createCell(row, 5, record.getCurrentType() != null ? record.getCurrentType() + " (Singapore)" : "S (Singapore)", dataStyle);
            createCell(row, 6, formatDateTime(record.getTsClvSuspensionDate()), dataStyle);
            createCell(row, 7, formatDateTime(record.getTsClvRevivalDate()), dataStyle);
            createCell(row, 8, record.getPpName(), dataStyle);
            createCurrencyCell(row, 9, record.getCompositionAmount(), currencyStyle);
            createCurrencyCell(row, 10, record.getAmountPayable(), currencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    /**
     * Convert raw Type V notice data to model objects
     */
    private List<ClassifiedVehicleReportRecord> convertToTypeVRecords(List<Map<String, Object>> rawRecords) {
        List<ClassifiedVehicleReportRecord> records = new ArrayList<>();
        for (Map<String, Object> raw : rawRecords) {
            ClassifiedVehicleReportRecord record = ClassifiedVehicleReportRecord.builder()
                    .noticeNo(getStringValue(raw, "noticeNo"))
                    .vehicleNo(getStringValue(raw, "vehicleNo"))
                    .vehicleRegistrationType(getStringValue(raw, "vehicleRegistrationType"))
                    .noticeDate(getLocalDateTimeValue(raw, "noticeDate"))
                    .offenceRuleCode(getStringValue(raw, "offenceRuleCode"))
                    .compositionAmount(getBigDecimalValue(raw, "compositionAmount"))
                    .amountPayable(getBigDecimalValue(raw, "amountPayable"))
                    .suspensionType(getStringValue(raw, "suspensionType"))
                    .suspensionReason(getStringValue(raw, "suspensionReason"))
                    .suspensionDate(getLocalDateTimeValue(raw, "suspensionDate"))
                    .ppName(getStringValue(raw, "ppName"))
                    .ppCode(getStringValue(raw, "ppCode"))
                    .paymentStatus(getStringValue(raw, "paymentStatus"))
                    .build();
            records.add(record);
        }
        return records;
    }

    /**
     * Convert raw amended notice data to model objects
     */
    private List<ClassifiedVehicleReportRecord> convertToAmendedRecords(List<Map<String, Object>> rawRecords) {
        List<ClassifiedVehicleReportRecord> records = new ArrayList<>();
        for (Map<String, Object> raw : rawRecords) {
            ClassifiedVehicleReportRecord record = ClassifiedVehicleReportRecord.builder()
                    .noticeNo(getStringValue(raw, "noticeNo"))
                    .vehicleNo(getStringValue(raw, "vehicleNo"))
                    .currentType(getStringValue(raw, "currentType"))
                    .noticeDate(getLocalDateTimeValue(raw, "noticeDate"))
                    .compositionAmount(getBigDecimalValue(raw, "compositionAmount"))
                    .amountPayable(getBigDecimalValue(raw, "amountPayable"))
                    .ppName(getStringValue(raw, "ppName"))
                    .ppCode(getStringValue(raw, "ppCode"))
                    .tsClvSuspensionDate(getLocalDateTimeValue(raw, "suspensionDate"))
                    .tsClvRevivalDate(getLocalDateTimeValue(raw, "revivalDate"))
                    .build();
            records.add(record);
        }
        return records;
    }

    /**
     * Calculate summary statistics
     */
    private ClassifiedVehicleReportSummary calculateSummary(List<ClassifiedVehicleReportRecord> typeVRecords,
                                                           List<ClassifiedVehicleReportRecord> amendedRecords) {
        int totalNotices = typeVRecords.size();
        int outstanding = (int) typeVRecords.stream()
                .filter(r -> "Outstanding".equals(r.getPaymentStatus()))
                .count();
        int settled = (int) typeVRecords.stream()
                .filter(r -> "Settled".equals(r.getPaymentStatus()))
                .count();
        int amended = amendedRecords.size();

        return ClassifiedVehicleReportSummary.builder()
                .reportDate(LocalDate.now())
                .totalNoticesIssued(totalNotices)
                .outstandingNotices(outstanding)
                .settledNotices(settled)
                .amendedNotices(amended)
                .build();
    }

    // Excel styling methods

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private void createKeyValueRow(Row row, String key, String value, CellStyle keyStyle, CellStyle valueStyle) {
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(keyStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void createCurrencyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    // Helper methods to extract values from Map

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime getLocalDateTimeValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse LocalDateTime from value: {}", value);
            return null;
        }
    }
}
