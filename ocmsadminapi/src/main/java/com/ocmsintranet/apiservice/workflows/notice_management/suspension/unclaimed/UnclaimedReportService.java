package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating Unclaimed Reminder Excel reports
 * Based on OCMS 20 Spec
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnclaimedReportService {

    private final AzureBlobStorageUtil blobStorageUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate Unclaimed Reminder Report
     *
     * @param unclaimedRecords List of unclaimed records submitted by OIC
     * @param userId User who generated the report
     * @return Report file path in Blob Storage
     * @throws ReportGenerationException if report generation fails
     */
    public String generateUnclaimedReminderReport(List<UnclaimedReminderDto> unclaimedRecords, String userId)
            throws ReportGenerationException {

        log.info("Generating Unclaimed Reminder Report for {} records, requested by: {}",
                unclaimedRecords.size(), userId);

        try {
            // Create Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Unclaimed Reminder Report");

            // Create header row with styles
            createUnclaimedReminderHeaderRow(sheet, workbook);

            // Populate data rows
            int rowNum = 1;
            for (UnclaimedReminderDto record : unclaimedRecords) {
                createUnclaimedReminderDataRow(sheet, rowNum++, record);
            }

            // Auto-size columns
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert workbook to byte array
            byte[] reportBytes;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                reportBytes = outputStream.toByteArray();
            }
            workbook.close();

            // Generate filename
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String fileName = String.format("%s-Unclaimed-Report.xlsx", timestamp);
            String blobPath = "unclaimed/" + fileName;

            // Upload to Blob Storage
            log.info("Uploading Unclaimed Reminder Report to Blob Storage: {}", blobPath);
            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                    blobStorageUtil.uploadBytesToBlob(reportBytes, blobPath);

            if (!uploadResponse.isSuccess()) {
                throw new ReportGenerationException("Failed to upload report to Blob Storage: "
                        + uploadResponse.getErrorMessage());
            }

            String reportPath = uploadResponse.getFilePath();
            log.info("Unclaimed Reminder Report generated successfully. Path: {}", reportPath);

            return reportPath;

        } catch (Exception e) {
            log.error("Failed to generate Unclaimed Reminder Report", e);
            throw new ReportGenerationException("Report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create header row for Unclaimed Reminder Report
     */
    private void createUnclaimedReminderHeaderRow(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Define column headers
        String[] headers = {
            "S/N",
            "Notice Number",
            "Date of Reminder",
            "Reminder Returned",
            "ID Number",
            "ID Type",
            "Owner/Hirer/Driver Indicator",
            "Date of Return",
            "Reason of Return",
            "Remarks on Envelope"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create data row for an unclaimed reminder record
     */
    private void createUnclaimedReminderDataRow(Sheet sheet, int rowNum, UnclaimedReminderDto record) {
        Row row = sheet.createRow(rowNum);

        // S/N
        row.createCell(0).setCellValue(rowNum);

        // Notice Number
        row.createCell(1).setCellValue(record.getNoticeNo() != null ? record.getNoticeNo() : "");

        // Date of Reminder
        if (record.getDateOfLetter() != null) {
            row.createCell(2).setCellValue(record.getDateOfLetter().format(DATE_FORMATTER));
        } else {
            row.createCell(2).setCellValue("");
        }

        // Reminder Returned (processing stage)
        row.createCell(3).setCellValue(record.getLastProcessingStage() != null ?
                record.getLastProcessingStage() : "");

        // ID Number
        row.createCell(4).setCellValue(record.getIdNumber() != null ? record.getIdNumber() : "");

        // ID Type
        row.createCell(5).setCellValue(record.getIdType() != null ? record.getIdType() : "");

        // Owner/Hirer/Driver Indicator
        row.createCell(6).setCellValue(record.getOwnerHirerIndicator() != null ?
                record.getOwnerHirerIndicator() : "");

        // Date of Return
        if (record.getDateOfReturn() != null) {
            row.createCell(7).setCellValue(record.getDateOfReturn().format(DATE_FORMATTER));
        } else {
            row.createCell(7).setCellValue("");
        }

        // Reason of Return
        row.createCell(8).setCellValue(record.getReasonForUnclaim() != null ?
                record.getReasonForUnclaim() : "");

        // Remarks on Envelope
        row.createCell(9).setCellValue(record.getUnclaimRemarks() != null ?
                record.getUnclaimRemarks() : "");
    }

    /**
     * Generate Unclaimed Batch Data Report (from MHA/DataHive results)
     *
     * @param batchDataRecords List of batch data records with MHA/DataHive results
     * @param userId User who generated the report
     * @return Report file path in Blob Storage
     * @throws ReportGenerationException if report generation fails
     */
    public String generateUnclaimedBatchDataReport(List<UnclaimedBatchDataDto> batchDataRecords, String userId)
            throws ReportGenerationException {

        log.info("Generating Unclaimed Batch Data Report for {} records, requested by: {}",
                batchDataRecords.size(), userId);

        try {
            // Create Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Unclaimed Batch Data Report");

            // Create header row with styles
            createBatchDataHeaderRow(sheet, workbook);

            // Populate data rows
            int rowNum = 1;
            for (UnclaimedBatchDataDto record : batchDataRecords) {
                createBatchDataRow(sheet, rowNum++, record);
            }

            // Auto-size columns
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert workbook to byte array
            byte[] reportBytes;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                reportBytes = outputStream.toByteArray();
            }
            workbook.close();

            // Generate filename
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String fileName = String.format("%s-Unclaimed-Batch-Data-Report.xlsx", timestamp);
            String blobPath = "unclaimed/" + fileName;

            // Upload to Blob Storage
            log.info("Uploading Unclaimed Batch Data Report to Blob Storage: {}", blobPath);
            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                    blobStorageUtil.uploadBytesToBlob(reportBytes, blobPath);

            if (!uploadResponse.isSuccess()) {
                throw new ReportGenerationException("Failed to upload report to Blob Storage: "
                        + uploadResponse.getErrorMessage());
            }

            String reportPath = uploadResponse.getFilePath();
            log.info("Unclaimed Batch Data Report generated successfully. Path: {}", reportPath);

            return reportPath;

        } catch (Exception e) {
            log.error("Failed to generate Unclaimed Batch Data Report", e);
            throw new ReportGenerationException("Report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create header row for Batch Data Report
     */
    private void createBatchDataHeaderRow(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Define column headers
        String[] headers = {
            "S/N",
            "Notice Number",
            "ID Number",
            "Offender Name",
            "Previous Address",
            "New Address",
            "Address Source",
            "Date Retrieved"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create data row for batch data record
     */
    private void createBatchDataRow(Sheet sheet, int rowNum, UnclaimedBatchDataDto record) {
        Row row = sheet.createRow(rowNum);

        // S/N
        row.createCell(0).setCellValue(rowNum);

        // Notice Number
        row.createCell(1).setCellValue(record.getNoticeNo() != null ? record.getNoticeNo() : "");

        // ID Number
        row.createCell(2).setCellValue(record.getIdNo() != null ? record.getIdNo() : "");

        // Offender Name
        row.createCell(3).setCellValue(record.getOffenderName() != null ? record.getOffenderName() : "");

        // Previous Address
        row.createCell(4).setCellValue(record.getPreviousAddress() != null ?
                record.getPreviousAddress() : "");

        // New Address
        row.createCell(5).setCellValue(record.getNewAddress() != null ? record.getNewAddress() : "");

        // Address Source
        row.createCell(6).setCellValue(record.getAddressSource() != null ?
                record.getAddressSource() : "");

        // Date Retrieved
        if (record.getDateRetrieved() != null) {
            row.createCell(7).setCellValue(record.getDateRetrieved().format(DATETIME_FORMATTER));
        } else {
            row.createCell(7).setCellValue("");
        }
    }

    /**
     * Custom exception for report generation errors
     */
    public static class ReportGenerationException extends Exception {
        public ReportGenerationException(String message) {
            super(message);
        }

        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
