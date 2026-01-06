package com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers;

import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNoticeRepository;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.models.RipHirerDriverReportRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OCMS 14: Helper for RIP Hirer/Driver Furnished Report
 * Handles data fetching and Excel generation for daily PS-RP2 Hirer/Driver report
 */
@Slf4j
@Component
public class RipHirerDriverReportHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final OcmsSuspendedNoticeRepository suspendedNoticeRepository;

    @Autowired
    public RipHirerDriverReportHelper(OcmsSuspendedNoticeRepository suspendedNoticeRepository) {
        this.suspendedNoticeRepository = suspendedNoticeRepository;
    }

    /**
     * Fetch PS-RP2 suspended notices where offender is Hirer/Driver
     *
     * @param reportDate Date of suspension (format: yyyy-MM-dd)
     * @return List of report records
     */
    public List<RipHirerDriverReportRecord> fetchRipHirerDriverRecords(String reportDate) {
        log.info("[RIP Hirer/Driver Report] Fetching PS-RP2 Hirer/Driver records for date: {}", reportDate);

        try {
            // Query database using repository method
            List<Map<String, Object>> rawRecords = suspendedNoticeRepository.findPsRp2WithHirerDriverOffender(reportDate);

            if (rawRecords.isEmpty()) {
                log.info("[RIP Hirer/Driver Report] No PS-RP2 Hirer/Driver records found for date: {}", reportDate);
                return new ArrayList<>();
            }

            log.info("[RIP Hirer/Driver Report] Found {} PS-RP2 Hirer/Driver records for date: {}", rawRecords.size(), reportDate);

            // Convert to model objects
            List<RipHirerDriverReportRecord> records = new ArrayList<>();
            for (Map<String, Object> raw : rawRecords) {
                RipHirerDriverReportRecord record = RipHirerDriverReportRecord.builder()
                        .noticeNo(getStringValue(raw, "noticeNo"))
                        .suspensionDate(getLocalDateTimeValue(raw, "suspensionDate"))
                        .srNo(getIntegerValue(raw, "srNo"))
                        .offenderName(getStringValue(raw, "offenderName"))
                        .idType(getStringValue(raw, "idType"))
                        .idNo(getStringValue(raw, "idNo"))
                        .ownerDriverIndicator(getStringValue(raw, "ownerDriverIndicator"))
                        .noticeDate(getLocalDateTimeValue(raw, "noticeDate"))
                        .offenceRuleCode(getStringValue(raw, "offenceRuleCode"))
                        .compositionAmount(getBigDecimalValue(raw, "compositionAmount"))
                        .amountPayable(getBigDecimalValue(raw, "amountPayable"))
                        .vehicleNo(getStringValue(raw, "vehicleNo"))
                        .ppName(getStringValue(raw, "ppName"))
                        .build();

                records.add(record);
            }

            return records;

        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report] Error fetching records: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch RIP Hirer/Driver records", e);
        }
    }

    /**
     * Generate Excel report for RIP Hirer/Driver records
     *
     * @param records List of report records
     * @param reportDate Report generation date
     * @return Byte array containing Excel file
     */
    public byte[] generateExcelReport(List<RipHirerDriverReportRecord> records, String reportDate) {
        log.info("[RIP Hirer/Driver Report] Generating Excel report for {} records", records.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create worksheet
            Sheet sheet = workbook.createSheet("RIP Hirer Driver Report");

            // Create header styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "S/N",
                "Notice No",
                "Vehicle No",
                "Offender Name",
                "ID Type",
                "ID No",
                "Owner/Driver/Hirer",
                "Suspension Date",
                "Notice Date",
                "Offence Rule Code",
                "Place of Offence",
                "Composition Amount ($)",
                "Amount Payable ($)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate data rows
            int rowNum = 1;
            int serialNumber = 1;
            for (RipHirerDriverReportRecord record : records) {
                Row row = sheet.createRow(rowNum++);

                // S/N
                createCell(row, 0, serialNumber++, dataStyle);

                // Notice No
                createCell(row, 1, record.getNoticeNo(), dataStyle);

                // Vehicle No
                createCell(row, 2, record.getVehicleNo(), dataStyle);

                // Offender Name
                createCell(row, 3, record.getOffenderName(), dataStyle);

                // ID Type
                createCell(row, 4, mapIdType(record.getIdType()), dataStyle);

                // ID No
                createCell(row, 5, record.getIdNo(), dataStyle);

                // Owner/Driver/Hirer Indicator
                createCell(row, 6, mapOwnerDriverIndicator(record.getOwnerDriverIndicator()), dataStyle);

                // Suspension Date
                createCell(row, 7, formatDateTime(record.getSuspensionDate()), dataStyle);

                // Notice Date
                createCell(row, 8, formatDateTime(record.getNoticeDate()), dataStyle);

                // Offence Rule Code
                createCell(row, 9, record.getOffenceRuleCode(), dataStyle);

                // Place of Offence
                createCell(row, 10, record.getPpName(), dataStyle);

                // Composition Amount
                createCurrencyCell(row, 11, record.getCompositionAmount(), currencyStyle);

                // Amount Payable
                createCurrencyCell(row, 12, record.getAmountPayable(), currencyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Add some padding
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // Write to output stream
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            log.info("[RIP Hirer/Driver Report] Excel report generated successfully: {} bytes", excelBytes.length);
            return excelBytes;

        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate RIP Hirer/Driver Excel report", e);
        }
    }

    /**
     * Create header style for Excel cells
     */
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

    /**
     * Create data style for Excel cells
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create currency style for Excel cells
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    /**
     * Create a cell with string value
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Create a cell with integer value
     */
    private void createCell(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    /**
     * Create a cell with currency value
     */
    private void createCurrencyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    /**
     * Format LocalDateTime to string
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * Map ID type code to display name
     */
    private String mapIdType(String idType) {
        if (idType == null) return "";
        return switch (idType) {
            case "N" -> "NRIC";
            case "F" -> "FIN";
            case "P" -> "Passport";
            case "B" -> "Business (UEN)";
            default -> idType;
        };
    }

    /**
     * Map owner/driver indicator to display name
     */
    private String mapOwnerDriverIndicator(String indicator) {
        if (indicator == null) return "";
        return switch (indicator) {
            case "O" -> "Owner";
            case "D" -> "Driver";
            case "H" -> "Hirer";
            default -> indicator;
        };
    }

    // Helper methods to extract values from Map

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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
        // Try parsing string
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse LocalDateTime from value: {}", value);
            return null;
        }
    }
}
