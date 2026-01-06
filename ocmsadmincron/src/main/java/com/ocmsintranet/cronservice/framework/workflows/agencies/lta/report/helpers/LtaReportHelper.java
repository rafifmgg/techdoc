package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.helpers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.dto.LtaReportData;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.mappers.LtaReportDataMapper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for LTA VRLS Vehicle Ownership Check Report Generation.
 * Contains all detailed functions for Excel generation, data processing, and email handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LtaReportHelper {

    private final JdbcTemplate jdbcTemplate;
    private final LtaReportDataMapper ltaReportDataMapper;
    private final EmailService emailService;
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    @Value("${lta.report.output.directory:/offence/lta/vrls/report}")
    private String reportOutputDirectory;

    @Value("${lta.report.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${email.report.recipients}")
    private String emailRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    @Value("${lta.report.azure.blob.enabled:true}")
    private boolean azureBlobEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Query yesterday's LTA VRLS processing records.
     *
     * @param yesterday The date to query
     * @return List of LtaReportData for yesterday
     */
    public List<LtaReportData> queryYesterdayRecords(LocalDate yesterday) {
        log.info("Querying LTA VRLS records for date: {}", yesterday);

        LocalDateTime startTime = yesterday.atStartOfDay(); // 00:00:00
        LocalDateTime endTime = yesterday.atTime(23, 59, 59); // 23:59:59

        String sql = """
                SELECT
                    von.notice_no,
                    von.vehicle_no,
                    von.vehicle_category,
                    von.offence_notice_type,
                    von.notice_date_and_time,
                    von.composition_amount,
                    von.pp_code,
                    von.pp_name,
                        von.suspension_type,
                    von.epr_reason_of_suspension,
                    ond.lta_deregistration_date,
                    onoda.processing_date_time,
                    onod.name,
                    onoda.error_code
                FROM ocmsizmgr.ocms_valid_offence_notice von
                LEFT JOIN ocmsizmgr.ocms_offence_notice_detail ond ON von.notice_no = ond.notice_no
                LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no
                LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver_addr onoda ON von.notice_no = onoda.notice_no  
                WHERE onoda.owner_driver_indicator = 'O'
                AND onoda.cre_date >= ? 
                AND onoda.cre_date < ?
                    AND onoda.type_of_address = 'lta_reg'
                ORDER BY onoda.processing_date_time
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> ltaReportDataMapper.mapFromResultSet(rs),
                    startTime, endTime);
        } catch (Exception e) {
            log.error("Error querying yesterday's LTA VRLS records: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query yesterday's LTA VRLS records", e);
        }
    }

    /**
     * Filter records that have TS-ROV data (critical business rule).
     * Based on current query structure, we'll check for suspension type or EPR reason.
     * This logic may need adjustment when TS-ROV fields are available.
     *
     * @param records List of all records
     * @return List of records with TS-ROV data
     */
    public List<LtaReportData> filterTsRovRecords(List<LtaReportData> records) {
        return records.stream()
                .filter(record -> {
                    // Filter for TS-ROV records: suspensionType = "TS" AND eprReasonOfSuspension = "ROV"
                    boolean isTsSuspensionType = "TS".equals(record.getSuspensionType());
                    boolean isRovReason = "ROV".equals(record.getEprReasonOfSuspension());
                    return isTsSuspensionType && isRovReason;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate Excel report with 4 sheets: Summary, Error, Success, TS-ROV Revived.
     *
     * @param records All records for the report
     * @param reportDate The date for the report
     * @return Excel file as byte array
     */
    public byte[] generateExcelReport(List<LtaReportData> records, LocalDate reportDate) {
        log.info("Generating Excel report for {} records on {}", records.size(), reportDate);

        try {
            // Create data classifications
            List<LtaReportData> successRecords = filterSuccessRecords(records);
            List<LtaReportData> errorRecords = filterErrorRecords(records);
            List<LtaReportData> tsRovRecords = filterTsRovRecords(records);

            log.info("Report data classification:");
            log.info("  Success records: {}", successRecords.size());
            log.info("  Error records: {}", errorRecords.size());
            log.info("  TS-ROV records: {}", tsRovRecords.size());

            // Create workbook with 4 sheets
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                // Create sheets
                Sheet summarySheet = workbook.createSheet("Summary");
                Sheet errorSheet = workbook.createSheet("Error");
                Sheet successSheet = workbook.createSheet("Success");
                Sheet tsRovSheet = workbook.createSheet("TS-ROV Revived");

                // Generate each sheet
                createSummarySheet(summarySheet, workbook, records, successRecords, errorRecords, tsRovRecords, reportDate);
                createErrorSheet(errorSheet, workbook, errorRecords);
                createSuccessSheet(successSheet, workbook, successRecords);
                createTsRovSheet(tsRovSheet, workbook, tsRovRecords);

                // Write to byte array
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    workbook.write(outputStream);
                    log.info("Excel report generated successfully with {} bytes", outputStream.size());
                    return outputStream.toByteArray();
                }
            }

        } catch (Exception e) {
            log.error("Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }


    /**
     * Save Excel report to both local file system and Azure Blob Storage.
     *
     * @param excelData Excel file data
     * @param reportDate Report date
     * @return File path where saved (local path if Azure is disabled, blob path if enabled)
     */
    public String saveExcelReport(byte[] excelData, LocalDate reportDate) {
        String fileName = String.format("LTA_Report_%s.xlsx", reportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        try {
            // Skip local file saving if Azure Blob is enabled (following LtaUploadJob pattern)
            String localFilePath = null;
            if (!azureBlobEnabled) {
                // Only save locally if Azure is disabled (fallback mode)
                Path outputDir = Paths.get(reportOutputDirectory);
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }

                Path localFilePathObj = outputDir.resolve(fileName);
                Files.write(localFilePathObj, excelData);
                localFilePath = localFilePathObj.toString();
                log.info("Excel report saved locally to: {}", localFilePath);
            }

            // If Azure Blob Storage is enabled, also upload to Azure
            if (azureBlobEnabled) {
                try {
                    String azureBlobPath = String.format("offence/lta/vrls/report/%s/%s",
                            reportDate.format(DateTimeFormatter.ofPattern("yyyy/MM")), fileName);

                    AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                            azureBlobStorageUtil.uploadBytesToBlob(excelData, azureBlobPath);

                    if (uploadResponse.isSuccess()) {
                        log.info("Excel report uploaded to Azure Blob Storage: {}", uploadResponse.getFileUrl());
                        log.info("Azure Blob path: {}", uploadResponse.getFilePath());
                        return uploadResponse.getFilePath(); // Return Azure blob path
                    } else {
                        log.error("Failed to upload Excel report to Azure Blob Storage: {}", uploadResponse.getErrorMessage());
                        return localFilePath != null ? localFilePath : "upload_failed";
                    }
                } catch (Exception e) {
                    log.error("Error uploading Excel report to Azure Blob Storage: {}", e.getMessage(), e);
                    return localFilePath != null ? localFilePath : "upload_failed";
                }
            } else {
                log.info("Azure Blob Storage disabled, Excel report saved locally only");
                return localFilePath != null ? localFilePath : "local_save_disabled";
            }

        } catch (IOException e) {
            log.error("Error saving Excel report locally: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save Excel report", e);
        }
    }

    /**
     * Send email notification with Excel attachment using existing EmailService.
     *
     * @param excelData Excel file data
     * @param reportDate Report date
     * @param recordCount Total record count
     * @return true if email sent successfully
     */
    public boolean sendEmailWithExcelAttachment(byte[] excelData, LocalDate reportDate, int recordCount) {
        if (!emailEnabled) {
            log.info("Email notification disabled, skipping email send");
            return true;
        }

        try {
            // Add environment prefix to email subject
            String subject = String.format("[%s] - OCMS LTA VRLS Report - %s (%d records)", 
                environment.toUpperCase(), reportDate.format(DATE_FORMATTER), recordCount);
            String fileName = String.format("LTA_Report_%s.xlsx", reportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // Create HTML email content
            String htmlContent = generateEmailHtmlContent(reportDate, recordCount);

            // Create attachment
            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(excelData);

            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setSubject(subject);
            emailRequest.setHtmlContent(htmlContent);
            emailRequest.setAttachments(Arrays.asList(attachment));

            log.info("Sending LTA report email to: {} with subject: {} and attachment: {}", emailRecipients, subject, fileName);
            log.info("Attachment size: {} bytes", excelData.length);

            // Send using existing EmailService
            boolean emailSent = emailService.sendEmail(emailRequest);

            if (emailSent) {
                log.info("LTA report email sent successfully to: {}", emailRecipients);
            } else {
                log.error("Failed to send LTA report email to: {}", emailRecipients);
            }

            return emailSent;

        } catch (Exception e) {
            log.error("Error sending LTA report email notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate HTML content for the email notification.
     *
     * @param reportDate The report date
     * @param recordCount Total number of records
     * @return HTML content for email
     */
    private String generateEmailHtmlContent(LocalDate reportDate, int recordCount) {
        return String.format("""
            <html>
            <body>
                <h2>LTA VRLS Vehicle Ownership Check Report</h2>
                <p><strong>Report Date:</strong> %s</p>
                <p><strong>Total Records:</strong> %d</p>
                <p>Please find the attached Excel report containing the LTA VRLS processing details.</p>
                <p>The report includes:</p>
                <ul>
                    <li>Summary sheet with overall statistics</li>
                    <li>Error records (codes 1-4)</li>
                    <li>Success records (code 0)</li>
                    <li>TS-ROV records requiring attention</li>
                </ul>
                <br/>
                <p><em>This is an automated report generated by OCMS Admin Cron Service.</em></p>
            </body>
            </html>
            """, reportDate.format(DATE_FORMATTER), recordCount);
    }

    // Private helper methods for Excel generation

    private List<LtaReportData> filterSuccessRecords(List<LtaReportData> records) {
        return records.stream()
                .filter(record -> "0".equals(record.getErrorCode()))
                .collect(Collectors.toList());
    }

    private List<LtaReportData> filterErrorRecords(List<LtaReportData> records) {
        return records.stream()
                .filter(record -> {
                    String code = record.getErrorCode();
                    return "1".equals(code) || "2".equals(code) || "3".equals(code) || "4".equals(code);
                })
                .collect(Collectors.toList());
    }

    /**
     * Create Summary sheet with report statistics
     */
    private void createSummarySheet(Sheet sheet, Workbook workbook, List<LtaReportData> allRecords,
                                  List<LtaReportData> successRecords, List<LtaReportData> errorRecords,
                                  List<LtaReportData> tsRovRecords, LocalDate reportDate) {

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle labelStyle = createLabelStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Row 1: Report Title (merged A1:B1)
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("OCMS SUMMARY REPORT - LTA VRLS Vehicle Ownership Check Result");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Row 2: Empty
        sheet.createRow(rowNum++);

        LocalDateTime now = LocalDateTime.now();

        // Row 3: Date of report generation
        createSummaryRow(sheet, rowNum++, "Date of report generation", now.format(DATETIME_FORMATTER), labelStyle, dataStyle);

        // Row 4: Total submitted
        createSummaryRow(sheet, rowNum++, "Total no. of records submitted to LTA", String.valueOf(allRecords.size()), labelStyle, dataStyle);

        // Row 5: Total returned
        createSummaryRow(sheet, rowNum++, "Total no. of records returned by LTA", String.valueOf(allRecords.size()), labelStyle, dataStyle);

        // Row 6: Code 0 - No Error
        createSummaryRow(sheet, rowNum++, "No. of records with '0' - No Error", String.valueOf(countByCode(allRecords, "0")), labelStyle, dataStyle);

        // Row 7: Code 1 - Reserved
        createSummaryRow(sheet, rowNum++, "No. of records with '1' - Reserved", String.valueOf(countByCode(allRecords, "1")), labelStyle, dataStyle);

        // Row 8: Code 2 - Record Not Found
        createSummaryRow(sheet, rowNum++, "No. of records with '2' - Record Not Found", String.valueOf(countByCode(allRecords, "2")), labelStyle, dataStyle);

        // Row 9: Code 3 - Deregistered
        createSummaryRow(sheet, rowNum++, "No. of records with '3' - Deregistered", String.valueOf(countByCode(allRecords, "3")), labelStyle, dataStyle);

        // Row 10: Code 4 - Invalid Offence Date
        createSummaryRow(sheet, rowNum++, "No. of records with '4' - Invalid Offence Date", String.valueOf(countByCode(allRecords, "4")), labelStyle, dataStyle);

        // Row 11: TS-ROV applied
        createSummaryRow(sheet, rowNum++, "No. of records applied with TS-ROV", String.valueOf(tsRovRecords.size()), labelStyle, dataStyle);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create Error sheet with error records (codes 1-4)
     */
    private void createErrorSheet(Sheet sheet, Workbook workbook, List<LtaReportData> errorRecords) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"S/N", "VRLS Processing Date", "VRLS Processing Time", "LTA Response Code",
                           "Notice No.", "Vehicle No.", "Deregistered Date", "Date of Offence",
                           "Time of Offence", "Car Park Code", "Car Park name", "TS-ROV status"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 1;
        for (LtaReportData record : errorRecords) {
            Row dataRow = sheet.createRow(rowNum);

            dataRow.createCell(0).setCellValue(rowNum); // S/N
            setCellValue(dataRow, 1, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalDate().format(DATE_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 2, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalTime().format(TIME_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 3, record.getErrorCode(), dataStyle);
            setCellValue(dataRow, 4, record.getNoticeNo(), dataStyle);
            setCellValue(dataRow, 5, record.getVehicleNo(), dataStyle);
            setCellValue(dataRow, 6, record.getLtaDeregistrationDate() != null ? record.getLtaDeregistrationDate().format(DATE_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 7, record.getNoticeDateAndTime() != null ? record.getNoticeDateAndTime().toLocalDate().format(DATE_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 8, record.getNoticeDateAndTime() != null ? record.getNoticeDateAndTime().toLocalTime().format(TIME_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 9, record.getPpCode(), dataStyle);
            setCellValue(dataRow, 10, record.getPpName(), dataStyle);
            setCellValue(dataRow, 11, record.getSuspensionType(), dataStyle); // Using suspension type as TS-ROV status

            rowNum++;
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create Success sheet with successful records (code 0)
     */
    private void createSuccessSheet(Sheet sheet, Workbook workbook, List<LtaReportData> successRecords) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"S/N", "VRLS Processing Date", "VRLS Processing Time", "LTA Response Code", "Notice No.", "Vehicle No."};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 1;
        for (LtaReportData record : successRecords) {
            Row dataRow = sheet.createRow(rowNum);

            dataRow.createCell(0).setCellValue(rowNum); // S/N
            setCellValue(dataRow, 1, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalDate().format(DATE_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 2, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalTime().format(TIME_FORMATTER) : "", dataStyle);
            setCellValue(dataRow, 3, record.getErrorCode(), dataStyle);
            setCellValue(dataRow, 4, record.getNoticeNo(), dataStyle);
            setCellValue(dataRow, 5, record.getVehicleNo(), dataStyle);

            rowNum++;
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create TS-ROV Revived sheet with revived records
     */
    private void createTsRovSheet(Sheet sheet, Workbook workbook, List<LtaReportData> tsRovRecords) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"S/N", "VRLS Processing Date", "VRLS Processing Time", "LTA Response Code",
                           "Notice No.", "Vehicle No.", "Date of Offence", "Time of Offence",
                           "Car Park Code", "Car Park name", "Date of Revival"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        // int rowNum = 1;
        // for (LtaReportData record : tsRovRecords) {
        //     Row dataRow = sheet.createRow(rowNum);

        //     dataRow.createCell(0).setCellValue(rowNum); // S/N
        //     setCellValue(dataRow, 1, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalDate().format(DATE_FORMATTER) : "", dataStyle);
        //     setCellValue(dataRow, 2, record.getProcessingDateTime() != null ? record.getProcessingDateTime().toLocalTime().format(TIME_FORMATTER) : "", dataStyle);
        //     setCellValue(dataRow, 3, record.getErrorCode(), dataStyle);
        //     setCellValue(dataRow, 4, record.getNoticeNo(), dataStyle);
        //     setCellValue(dataRow, 5, record.getVehicleNo(), dataStyle);
        //     setCellValue(dataRow, 6, record.getNoticeDateAndTime() != null ? record.getNoticeDateAndTime().toLocalDate().format(DATE_FORMATTER) : "", dataStyle);
        //     setCellValue(dataRow, 7, record.getNoticeDateAndTime() != null ? record.getNoticeDateAndTime().toLocalTime().format(TIME_FORMATTER) : "", dataStyle);
        //     setCellValue(dataRow, 8, record.getPpCode(), dataStyle);
        //     setCellValue(dataRow, 9, record.getPpName(), dataStyle);
        //     setCellValue(dataRow, 10, "N/A", dataStyle); // Date of Revival - would need actual field

        //     rowNum++;
        // }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Helper methods for Excel formatting
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(dataStyle);
    }

    private void setCellValue(Row row, int cellIndex, String value, CellStyle style) {
        Cell cell = row.createCell(cellIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private long countByCode(List<LtaReportData> records, String code) {
        return records.stream()
                .filter(record -> code.equals(record.getErrorCode()))
                .count();
    }

    /**
     * Extract metrics from log text for report tracking.
     * Pattern: "Metrics: key1: value1, key2: value2, ..."
     *
     * This method parses the structured metrics section from job log_text
     * and populates a map with the extracted key-value pairs.
     * Values are accumulated using merge to support aggregation across multiple job runs.
     *
     * @param logText The log text to parse (from ocms_batch_job.log_text)
     * @param metrics The metrics map to populate (values are accumulated)
     */
    public void extractMetricsFromText(String logText, Map<String, Integer> metrics) {
        if (logText == null || logText.isEmpty()) {
            log.warn("Log text is null or empty, cannot extract metrics");
            return;
        }

        int metricsIndex = logText.indexOf("Metrics:");
        if (metricsIndex < 0) {
            log.warn("No 'Metrics:' section found in log text");
            return;
        }

        String metricsStr = logText.substring(metricsIndex + "Metrics:".length()).trim();
        log.debug("Extracting metrics from: {}", metricsStr);

        String[] pairs = metricsStr.split(",\\s*");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":\\s*");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                try {
                    int value = Integer.parseInt(keyValue[1].trim());
                    // Use merge to accumulate values instead of overwriting
                    metrics.merge(key, value, Integer::sum);
                    log.debug("Extracted {} = {} (Accumulated: {})", key, value, metrics.get(key));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse number from '{}' in metrics string: {}", keyValue[1], metricsStr);
                }
            }
        }
    }

    /**
     * Extract metrics from log text and return as a new map.
     * Convenience method that creates and returns a new metrics map.
     *
     * @param logText The log text to parse (from ocms_batch_job.log_text)
     * @return Map containing extracted metrics
     */
    public Map<String, Integer> extractMetricsFromText(String logText) {
        Map<String, Integer> metrics = new HashMap<>();
        extractMetricsFromText(logText, metrics);
        return metrics;
    }
}