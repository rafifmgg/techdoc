package com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class for Daily Paid Report generation.
 * This class handles data fetching and Excel report generation with 6 sheets.
 *
 * The report includes:
 * - Sheet 1: Summary with counts by payment channel
 * - Sheet 2-4: eService, AXS, Offline payment transactions
 * - Sheet 5: JTC Collections (FP reason)
 * - Sheet 6: Refund Records
 */
@Slf4j
@Component
public class DailyPaidReportHelper {

    private final JdbcTemplate jdbcTemplate;

    public DailyPaidReportHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches paid records from the database for the specified date
     *
     * @param reportDate The date for which to fetch data (format: yyyy-MM-dd)
     * @return Map containing categorized records and counts
     */
    public Map<String, Object> fetchPaidRecords(String reportDate) {
        log.info("[DailyPaidReport] Fetching paid records for date: {}", reportDate);

        try {
            // Parse the date
            LocalDate date = LocalDate.parse(reportDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // SQL query to fetch paid records for a specific date
            String sql = "SELECT " +
                        "von.notice_no, " +
                        "von.notice_date_and_time, " +
                        "von.pp_code, " +
                        "von.vehicle_no, " +
                        "ond.vehicle_make, " +
                        "ond.lta_make_description as vehicle_model, " +
                        "von.vehicle_category, " +
                        "von.vehicle_registration_type, " +
                        "ond.color_of_vehicle as vehicle_colour, " +
                        "von.computer_rule_code, " +
                        "von.pp_name as offence_location, " +
                        "von.amount_payable, " +
                        "von.amount_paid, " +
                        "von.suspension_type, " +
                        "von.crs_reason_of_suspension, " +
                        "von.crs_date_of_suspension, " +
                        "von.epr_reason_of_suspension, " +
                        "wtd.receipt_no, " +
                        "wtd.payment_mode, " +
                        "wtd.payment_amount, " +
                        "wtd.transaction_date_and_time, " +
                        "wtd.receipt_no as transaction_id, " +
                        "wtd.sender " +
                        "FROM ocmsizmgr.ocms_valid_offence_notice von " +
                        "LEFT JOIN ocmsizmgr.ocms_web_txn_detail wtd ON von.notice_no = wtd.offence_notice_no " +
                        "LEFT JOIN ocmsizmgr.ocms_offence_notice_detail ond ON ond.notice_no = von.notice_no " +
                        "WHERE von.crs_reason_of_suspension IN ('PRA', 'FP') " +
                        "AND CAST(von.crs_date_of_suspension AS DATE) = ?";

            // Execute payment records query
            List<Map<String, Object>> allRecords = jdbcTemplate.queryForList(sql, java.sql.Date.valueOf(date));

            log.info("[DailyPaidReport] Found {} total payment records for date: {}", allRecords.size(), reportDate);

            // Deduplicate by notice_no - keep only the latest transaction per notice
            Map<String, Map<String, Object>> latestByNoticeNo = new LinkedHashMap<>();
            for (Map<String, Object> record : allRecords) {
                String noticeNo = record.get("notice_no") != null ? record.get("notice_no").toString() : "";
                if (noticeNo.isEmpty()) continue;

                Map<String, Object> existing = latestByNoticeNo.get(noticeNo);
                if (existing == null) {
                    latestByNoticeNo.put(noticeNo, record);
                } else {
                    // Compare transaction_date_and_time to keep the latest
                    Object currentDateTime = record.get("transaction_date_and_time");
                    Object existingDateTime = existing.get("transaction_date_and_time");

                    if (currentDateTime != null && existingDateTime != null) {
                        java.sql.Timestamp currentTs = currentDateTime instanceof java.sql.Timestamp ?
                            (java.sql.Timestamp) currentDateTime : null;
                        java.sql.Timestamp existingTs = existingDateTime instanceof java.sql.Timestamp ?
                            (java.sql.Timestamp) existingDateTime : null;

                        if (currentTs != null && existingTs != null && currentTs.after(existingTs)) {
                            latestByNoticeNo.put(noticeNo, record);
                        }
                    }
                }
            }

            // Replace allRecords with deduplicated list
            allRecords = new ArrayList<>(latestByNoticeNo.values());
            log.info("[DailyPaidReport] After deduplication: {} unique notices for date: {}", allRecords.size(), reportDate);

            // Separate query for refund records - directly from ocms_refund_notice table
            String refundSql = "SELECT " +
                        "rn.notice_no, " +
                        "rn.refund_notice_id, " +
                        "rn.receipt_no, " +
                        "rn.pp_code, " +
                        "rn.amount_paid, " +
                        "rn.amount_payable, " +
                        "rn.suspension_type, " +
                        "rn.crs_reason_of_suspension, " +
                        "rn.epr_reason_of_suspension, " +
                        "rn.refund_amount, " +
                        "rn.refund_reason, " +
                        "rn.cre_date as refund_date " +
                        "FROM ocmsizmgr.ocms_refund_notice rn " +
                        "WHERE CAST(rn.cre_date AS DATE) = ?";

            List<Map<String, Object>> refundRecords = jdbcTemplate.queryForList(refundSql, java.sql.Date.valueOf(date));
            log.info("[DailyPaidReport] Found {} refund records for date: {}", refundRecords.size(), reportDate);

            // Categorize payment records by sender (wtd.sender)
            List<Map<String, Object>> eServiceRecords = new ArrayList<>();
            List<Map<String, Object>> axsRecords = new ArrayList<>();
            List<Map<String, Object>> offlineRecords = new ArrayList<>();
            List<Map<String, Object>> jtcCollectionRecords = new ArrayList<>();

            for (Map<String, Object> record : allRecords) {
                String sender = record.get("sender") != null ?
                                record.get("sender").toString().toUpperCase() : "";

                // Categorize by sender
                if (sender.contains("URA")) {
                    eServiceRecords.add(record);
                }
                else if (sender.contains("AXS")) {
                    axsRecords.add(record);
                }
                else if (sender.contains("OCMS")) {
                    offlineRecords.add(record);
                }
                else if (sender.contains("JTC")) {
                    jtcCollectionRecords.add(record);
                }
            }

            int totalCount = allRecords.size();

            log.info("[DailyPaidReport] Categorized records - eService: {}, AXS: {}, Offline: {}, JTC: {}, Refund: {}",
                    eServiceRecords.size(), axsRecords.size(), offlineRecords.size(),
                    jtcCollectionRecords.size(), refundRecords.size());

            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("eServiceCount", eServiceRecords.size());
            result.put("axsCount", axsRecords.size());
            result.put("offlineCount", offlineRecords.size());
            result.put("jtcCollectionCount", jtcCollectionRecords.size());
            result.put("refundCount", refundRecords.size());
            result.put("eServiceRecords", eServiceRecords);
            result.put("axsRecords", axsRecords);
            result.put("offlineRecords", offlineRecords);
            result.put("jtcCollectionRecords", jtcCollectionRecords);
            result.put("refundRecords", refundRecords);

            return result;

        } catch (Exception e) {
            log.error("[DailyPaidReport] Error fetching paid records: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Generate Excel report with 6 sheets
     *
     * @param reportData The data to include in the report
     * @param reportDate The date for which the report is generated
     * @return Byte array containing the Excel report
     */
    public byte[] generateExcelReport(Map<String, Object> reportData, String reportDate) {
        log.info("[DailyPaidReport] Generating Excel report for date: {}", reportDate);

        try {
            Workbook workbook = new XSSFWorkbook();

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle timeStyle = createTimeStyle(workbook);

            // Sheet 1: Summary
            createSummarySheet(workbook, reportData, reportDate, titleStyle, headerStyle, dataStyle);

            // Sheet 2: eService Successful Txns
            createPaymentChannelSheet(workbook, reportData, "eService Successful Txns", "eServiceRecords",
                                       headerStyle, dataStyle, dateStyle, timeStyle);

            // Sheet 3: Offline Payments (includes Payment Mode column)
            createOfflinePaymentSheet(workbook, reportData, headerStyle, dataStyle, dateStyle, timeStyle);

            // Sheet 4: AXS Successful Txns
            createPaymentChannelSheet(workbook, reportData, "AXS Successful Txns", "axsRecords",
                                       headerStyle, dataStyle, dateStyle, timeStyle);

            // Sheet 5: JTC Collections
            createJtcCollectionSheet(workbook, reportData, headerStyle, dataStyle, dateStyle, timeStyle);

            // Sheet 6: Refund
            createRefundRecordsSheet(workbook, reportData, headerStyle, dataStyle, dateStyle, timeStyle);

            // Write workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            log.info("[DailyPaidReport] Excel report generated successfully");
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("[DailyPaidReport] Error generating Excel report", e);
            return null;
        }
    }

    /**
     * Create Summary sheet
     */
    @SuppressWarnings("unchecked")
    private void createSummarySheet(Workbook workbook, Map<String, Object> reportData,
                                     String reportDate, CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Summary");
        sheet.setColumnWidth(0, 15000);
        sheet.setColumnWidth(1, 6000);

        // Create left-aligned label style
        CellStyle labelStyle = workbook.createCellStyle();
        XSSFFont labelFont = ((XSSFWorkbook) workbook).createFont();
        labelFont.setFontName("Calibri");
        labelFont.setFontHeight(11);
        labelStyle.setFont(labelFont);
        labelStyle.setAlignment(HorizontalAlignment.LEFT);
        labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create right-aligned data style
        CellStyle valueStyle = workbook.createCellStyle();
        XSSFFont valueFont = ((XSSFWorkbook) workbook).createFont();
        valueFont.setFontName("Calibri");
        valueFont.setFontHeight(11);
        valueStyle.setFont(valueFont);
        valueStyle.setAlignment(HorizontalAlignment.RIGHT);
        valueStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        int rowNum = 0;

        // Main Title - bold and centered
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("OCMS REPORT - DAILY COLLECTIONS OF PARKING OFFENCE NOTICES");

        CellStyle titleCellStyle = workbook.createCellStyle();
        XSSFFont titleFont = ((XSSFWorkbook) workbook).createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeight(14);
        titleFont.setBold(true);
        titleCellStyle.setFont(titleFont);
        titleCellStyle.setAlignment(HorizontalAlignment.CENTER);
        titleCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleCellStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row

        // Date of report generation
        LocalDateTime now = LocalDateTime.now();
        Row genDateRow = sheet.createRow(rowNum++);
        createCell(genDateRow, 0, "Date of report generation", labelStyle);
        createCell(genDateRow, 1, now.format(DateTimeFormatter.ofPattern("d/M/yyyy")), valueStyle);

        // Time of report generation
        Row genTimeRow = sheet.createRow(rowNum++);
        createCell(genTimeRow, 0, "Time of report generation", labelStyle);
        createCell(genTimeRow, 1, now.format(DateTimeFormatter.ofPattern("HHmm")) + "hr", valueStyle);

        // Report date
        Row dateRangeRow = sheet.createRow(rowNum++);
        createCell(dateRangeRow, 0, "Report date", labelStyle);
        LocalDate date = LocalDate.parse(reportDate);
        String formattedDate = String.format("%d/%d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        createCell(dateRangeRow, 1, formattedDate, valueStyle);

        rowNum++; // Empty row
        rowNum++; // Another empty row for spacing

        // Payment counts section
        Row eServiceCountRow = sheet.createRow(rowNum++);
        createCell(eServiceCountRow, 0, "Total No. of Successfully Payments from eService", labelStyle);
        createCell(eServiceCountRow, 1, reportData.get("eServiceCount"), valueStyle);

        Row axsCountRow = sheet.createRow(rowNum++);
        createCell(axsCountRow, 0, "Total No. of Successfully Payments from AXS", labelStyle);
        createCell(axsCountRow, 1, reportData.get("axsCount"), valueStyle);

        Row offlineCountRow = sheet.createRow(rowNum++);
        createCell(offlineCountRow, 0, "Total No. of Offline Payments", labelStyle);
        createCell(offlineCountRow, 1, reportData.get("offlineCount"), valueStyle);

        Row refundCountRow = sheet.createRow(rowNum++);
        createCell(refundCountRow, 0, "Total No. of Refunds", labelStyle);
        createCell(refundCountRow, 1, reportData.get("refundCount"), valueStyle);

        rowNum++; // Empty row
        rowNum++; // Another empty row for spacing

        // Calculate total amounts from records
        List<Map<String, Object>> eServiceRecords = (List<Map<String, Object>>) reportData.get("eServiceRecords");
        List<Map<String, Object>> axsRecords = (List<Map<String, Object>>) reportData.get("axsRecords");
        List<Map<String, Object>> jtcRecords = (List<Map<String, Object>>) reportData.get("jtcCollectionRecords");

        double eServiceAmount = calculateTotalAmount(eServiceRecords);
        double axsAmount = calculateTotalAmount(axsRecords);
        double jtcAmount = calculateTotalAmount(jtcRecords);

        // Amount collected section
        Row eServiceAmountRow = sheet.createRow(rowNum++);
        createCell(eServiceAmountRow, 0, "Total Amount Collected via eService", labelStyle);
        createCell(eServiceAmountRow, 1, String.format("$%,.2f", eServiceAmount), valueStyle);

        Row axsAmountRow = sheet.createRow(rowNum++);
        createCell(axsAmountRow, 0, "Total Amount Collected via AXS", labelStyle);
        createCell(axsAmountRow, 1, String.format("$%,.2f", axsAmount), valueStyle);

        rowNum++; // Empty row

        // JTC Amount with border
        Row jtcAmountRow = sheet.createRow(rowNum++);
        createCell(jtcAmountRow, 0, "Total Amount Collected on Behalf of JTC", labelStyle);

        // Create bordered style for JTC amount
        CellStyle borderedStyle = workbook.createCellStyle();
        borderedStyle.cloneStyleFrom(valueStyle);
        borderedStyle.setBorderBottom(BorderStyle.THIN);
        borderedStyle.setBorderTop(BorderStyle.THIN);
        borderedStyle.setBorderRight(BorderStyle.THIN);
        borderedStyle.setBorderLeft(BorderStyle.THIN);

        Cell jtcAmountCell = jtcAmountRow.createCell(1);
        jtcAmountCell.setCellValue(String.format("$%,.2f", jtcAmount));
        jtcAmountCell.setCellStyle(borderedStyle);
    }

    /**
     * Calculate total amount from list of records
     */
    private double calculateTotalAmount(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Map<String, Object> record : records) {
            Object amount = record.get("payment_amount");
            if (amount != null) {
                try {
                    if (amount instanceof Number) {
                        total += ((Number) amount).doubleValue();
                    } else {
                        total += Double.parseDouble(amount.toString());
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                }
            }
        }
        return total;
    }

    /**
     * Create payment channel sheet (eService, AXS)
     */
    @SuppressWarnings("unchecked")
    private void createPaymentChannelSheet(Workbook workbook, Map<String, Object> reportData,
                                            String sheetName, String recordsKey,
                                            CellStyle headerStyle, CellStyle dataStyle,
                                            CellStyle dateStyle, CellStyle timeStyle) {
        Sheet sheet = workbook.createSheet(sheetName);

        // Set column widths
        sheet.setColumnWidth(0, 5500);  // Date / Time of Payment
        sheet.setColumnWidth(1, 4500);  // Notice No.
        sheet.setColumnWidth(2, 4000);  // Receipt No.
        sheet.setColumnWidth(3, 3500);  // Amount Paid
        sheet.setColumnWidth(4, 3500);  // Amount payable
        sheet.setColumnWidth(5, 3000);  // PP code
        sheet.setColumnWidth(6, 4500);  // CRS Reason of Suspension
        sheet.setColumnWidth(7, 4500);  // EPR Reason of Suspension

        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Date / Time of Payment", headerStyle);
        createCell(headerRow, 1, "Notice No.", headerStyle);
        createCell(headerRow, 2, "Receipt No.", headerStyle);
        createCell(headerRow, 3, "Amount Paid", headerStyle);
        createCell(headerRow, 4, "Amount payable", headerStyle);
        createCell(headerRow, 5, "PP code", headerStyle);
        createCell(headerRow, 6, "CRS Reason of Suspension", headerStyle);
        createCell(headerRow, 7, "EPR Reason of Suspension", headerStyle);

        // Data rows
        List<Map<String, Object>> records = (List<Map<String, Object>>) reportData.getOrDefault(recordsKey, new ArrayList<>());

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            Row row = sheet.createRow(rowNum);

            // Format date/time as "d/M/yyyy H:mm"
            Object dateTime = record.get("transaction_date_and_time");
            String formattedDateTime = formatDateTime(dateTime);

            createCell(row, 0, formattedDateTime, dataStyle);
            createCell(row, 1, record.get("notice_no"), dataStyle);
            createCell(row, 2, record.get("receipt_no"), dataStyle);
            createCell(row, 3, formatAmount(record.get("payment_amount")), dataStyle);
            createCell(row, 4, formatAmount(record.get("amount_payable")), dataStyle);
            createCell(row, 5, record.get("pp_code"), dataStyle);
            createCell(row, 6, record.get("crs_reason_of_suspension"), dataStyle);
            createCell(row, 7, record.get("epr_reason_of_suspension"), dataStyle);

            rowNum++;
        }
    }

    /**
     * Create Offline payment sheet (includes Payment Mode column)
     */
    @SuppressWarnings("unchecked")
    private void createOfflinePaymentSheet(Workbook workbook, Map<String, Object> reportData,
                                           CellStyle headerStyle, CellStyle dataStyle,
                                           CellStyle dateStyle, CellStyle timeStyle) {
        Sheet sheet = workbook.createSheet("Offline Payments");

        // Set column widths
        sheet.setColumnWidth(0, 5500);  // Date / Time of Payment
        sheet.setColumnWidth(1, 4500);  // Notice No.
        sheet.setColumnWidth(2, 4000);  // Receipt No.
        sheet.setColumnWidth(3, 3500);  // Payment Mode
        sheet.setColumnWidth(4, 3500);  // Amount Paid
        sheet.setColumnWidth(5, 3500);  // Amount payable
        sheet.setColumnWidth(6, 3000);  // PP code
        sheet.setColumnWidth(7, 4500);  // CRS Reason of Suspension
        sheet.setColumnWidth(8, 4500);  // EPR Reason of Suspension

        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Date / Time of Payment", headerStyle);
        createCell(headerRow, 1, "Notice No.", headerStyle);
        createCell(headerRow, 2, "Receipt No.", headerStyle);
        createCell(headerRow, 3, "Payment Mode", headerStyle);
        createCell(headerRow, 4, "Amount Paid", headerStyle);
        createCell(headerRow, 5, "Amount payable", headerStyle);
        createCell(headerRow, 6, "PP code", headerStyle);
        createCell(headerRow, 7, "CRS Reason of Suspension", headerStyle);
        createCell(headerRow, 8, "EPR Reason of Suspension", headerStyle);

        // Data rows
        List<Map<String, Object>> records = (List<Map<String, Object>>) reportData.getOrDefault("offlineRecords", new ArrayList<>());

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            Row row = sheet.createRow(rowNum);

            String formattedDateTime = formatDateTime(record.get("transaction_date_and_time"));

            createCell(row, 0, formattedDateTime, dataStyle);
            createCell(row, 1, record.get("notice_no"), dataStyle);
            createCell(row, 2, record.get("receipt_no"), dataStyle);
            createCell(row, 3, record.get("payment_mode"), dataStyle);
            createCell(row, 4, formatAmount(record.get("payment_amount")), dataStyle);
            createCell(row, 5, formatAmount(record.get("amount_payable")), dataStyle);
            createCell(row, 6, record.get("pp_code"), dataStyle);
            createCell(row, 7, record.get("crs_reason_of_suspension"), dataStyle);
            createCell(row, 8, record.get("epr_reason_of_suspension"), dataStyle);

            rowNum++;
        }
    }

    /**
     * Create JTC Collection sheet
     */
    @SuppressWarnings("unchecked")
    private void createJtcCollectionSheet(Workbook workbook, Map<String, Object> reportData,
                                          CellStyle headerStyle, CellStyle dataStyle,
                                          CellStyle dateStyle, CellStyle timeStyle) {
        Sheet sheet = workbook.createSheet("JTC Collections");

        // Set column widths
        sheet.setColumnWidth(0, 4500);  // Notice No
        sheet.setColumnWidth(1, 5500);  // Notice Date And Time
        sheet.setColumnWidth(2, 3000);  // PP Code
        sheet.setColumnWidth(3, 3500);  // Vehicle No
        sheet.setColumnWidth(4, 4500);  // Vehicle Category
        sheet.setColumnWidth(5, 5500);  // Vehicle Registration Type
        sheet.setColumnWidth(6, 4500);  // Computer Rule Code
        sheet.setColumnWidth(7, 4000);  // Composition Amount
        sheet.setColumnWidth(8, 3500);  // Amount Paid
        sheet.setColumnWidth(9, 4000);  // Suspension Type
        sheet.setColumnWidth(10, 5000); // CRS Reason Of Suspension
        sheet.setColumnWidth(11, 5500); // CRS Date Of Suspension (Payment Date)

        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Notice No", headerStyle);
        createCell(headerRow, 1, "Notice Date And Time", headerStyle);
        createCell(headerRow, 2, "PP Code", headerStyle);
        createCell(headerRow, 3, "Vehicle No", headerStyle);
        createCell(headerRow, 4, "Vehicle Category", headerStyle);
        createCell(headerRow, 5, "Vehicle Registration Type", headerStyle);
        createCell(headerRow, 6, "Computer Rule Code", headerStyle);
        createCell(headerRow, 7, "Composition Amount", headerStyle);
        createCell(headerRow, 8, "Amount Paid", headerStyle);
        createCell(headerRow, 9, "Suspension Type", headerStyle);
        createCell(headerRow, 10, "CRS Reason Of Suspension", headerStyle);
        createCell(headerRow, 11, "CRS Date Of Suspension \n(Payment Date)", headerStyle);

        // Data rows - filter for FP reason JTC collections
        List<Map<String, Object>> records = (List<Map<String, Object>>) reportData.getOrDefault("jtcCollectionRecords", new ArrayList<>());

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            String crsReason = record.get("crs_reason_of_suspension") != null ?
                              record.get("crs_reason_of_suspension").toString() : "";
            if (SystemConstant.SuspensionReason.FULL_PAYMENT.equals(crsReason)) {
                Row row = sheet.createRow(rowNum);

                createCell(row, 0, record.get("notice_no"), dataStyle);
                createCell(row, 1, formatDateTime(record.get("notice_date_and_time")), dataStyle);
                createCell(row, 2, record.get("pp_code"), dataStyle);
                createCell(row, 3, record.get("vehicle_no"), dataStyle);
                createCell(row, 4, record.get("vehicle_category"), dataStyle);
                createCell(row, 5, record.get("vehicle_registration_type"), dataStyle);
                createCell(row, 6, record.get("computer_rule_code"), dataStyle);
                createCell(row, 7, formatAmount(record.get("amount_payable")), dataStyle);
                createCell(row, 8, formatAmount(record.get("amount_paid")), dataStyle);
                createCell(row, 9, record.get("suspension_type"), dataStyle);
                createCell(row, 10, record.get("crs_reason_of_suspension"), dataStyle);
                createCell(row, 11, formatDateTime(record.get("crs_date_of_suspension")), dataStyle);

                rowNum++;
            }
        }
    }

    /**
     * Create Refund Records sheet
     */
    @SuppressWarnings("unchecked")
    private void createRefundRecordsSheet(Workbook workbook, Map<String, Object> reportData,
                                          CellStyle headerStyle, CellStyle dataStyle,
                                          CellStyle dateStyle, CellStyle timeStyle) {
        Sheet sheet = workbook.createSheet("Refund");

        // Set column widths
        sheet.setColumnWidth(0, 5500);  // Refund Date
        sheet.setColumnWidth(1, 4500);  // Notice No.
        sheet.setColumnWidth(2, 5000);  // Refund ID
        sheet.setColumnWidth(3, 4500);  // Receipt No.
        sheet.setColumnWidth(4, 3000);  // PP code
        sheet.setColumnWidth(5, 3500);  // Amount Paid
        sheet.setColumnWidth(6, 3500);  // Amount Payable
        sheet.setColumnWidth(7, 3500);  // Refund Amount
        sheet.setColumnWidth(8, 4500);  // CRS Reason of Suspension
        sheet.setColumnWidth(9, 4500);  // EPR Reason of Suspension

        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Refund Date", headerStyle);
        createCell(headerRow, 1, "Notice No.", headerStyle);
        createCell(headerRow, 2, "Refund ID", headerStyle);
        createCell(headerRow, 3, "Receipt No.", headerStyle);
        createCell(headerRow, 4, "PP code", headerStyle);
        createCell(headerRow, 5, "Amount Paid", headerStyle);
        createCell(headerRow, 6, "Amount Payable", headerStyle);
        createCell(headerRow, 7, "Refund Amount", headerStyle);
        createCell(headerRow, 8, "CRS Reason of Suspension", headerStyle);
        createCell(headerRow, 9, "EPR Reason of Suspension", headerStyle);

        // Data rows
        List<Map<String, Object>> records = (List<Map<String, Object>>) reportData.getOrDefault("refundRecords", new ArrayList<>());

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            Row row = sheet.createRow(rowNum);

            createCell(row, 0, formatDateTime(record.get("refund_date")), dataStyle);
            createCell(row, 1, record.get("notice_no"), dataStyle);
            createCell(row, 2, record.get("refund_notice_id"), dataStyle);
            createCell(row, 3, record.get("receipt_no"), dataStyle);
            createCell(row, 4, record.get("pp_code"), dataStyle);
            createCell(row, 5, formatAmount(record.get("amount_paid")), dataStyle);
            createCell(row, 6, formatAmount(record.get("amount_payable")), dataStyle);
            createCell(row, 7, formatAmount(record.get("refund_amount")), dataStyle);
            createCell(row, 8, record.get("crs_reason_of_suspension"), dataStyle);
            createCell(row, 9, record.get("epr_reason_of_suspension"), dataStyle);

            rowNum++;
        }
    }

    /**
     * Format date/time as "d/M/yyyy H:mm"
     */
    private String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "";
        }
        if (dateTime instanceof java.sql.Timestamp) {
            java.sql.Timestamp ts = (java.sql.Timestamp) dateTime;
            return new java.text.SimpleDateFormat("d/M/yyyy H:mm").format(ts);
        } else if (dateTime instanceof java.util.Date) {
            return new java.text.SimpleDateFormat("d/M/yyyy H:mm").format((java.util.Date) dateTime);
        } else {
            return dateTime.toString();
        }
    }

    /**
     * Format amount to 2 decimal places
     */
    private String formatAmount(Object amount) {
        if (amount == null) {
            return "";
        }
        try {
            double value;
            if (amount instanceof Number) {
                value = ((Number) amount).doubleValue();
            } else {
                value = Double.parseDouble(amount.toString());
            }
            return String.format("%.2f", value);
        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }

    /**
     * Create a cell with value and style
     */
    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        } else {
            cell.setCellValue("");
        }

        cell.setCellStyle(style);
    }

    // Styling methods
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(14);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm:ss"));
        return style;
    }
}
