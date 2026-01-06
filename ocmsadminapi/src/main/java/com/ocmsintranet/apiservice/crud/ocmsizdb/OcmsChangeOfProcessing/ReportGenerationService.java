package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for generating Change Processing Stage Excel reports
 * Based on OCMS CPS Spec §7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final OcmsValidOffenceNoticeService vonService;
    private final OcmsOffenceNoticeOwnerDriverService onodService;
    private final AzureBlobStorageUtil blobStorageUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Retry configuration as per spec §2.5.1 Step 7
    private static final int MAX_GENERATION_RETRIES = 1; // Retry once on generation failure
    private static final int MAX_UPLOAD_RETRIES = 1;     // Retry once on upload failure
    private static final long RETRY_DELAY_MS = 1000;     // 1 second delay between retries

    /**
     * Generate Change Processing Stage Report with retry logic
     * Based on OCMS CPS Spec §2.5.1 Step 7:
     * - Retry once if report generation fails
     * - Retry once if upload to Blob Storage fails
     *
     * @param changeRecords List of change records from ocms_change_of_processing
     * @param userId User who generated the report
     * @return Report URL in Blob Storage
     * @throws ReportGenerationException if report generation fails after retries
     */
    public String generateChangeStageReport(List<OcmsChangeOfProcessing> changeRecords, String userId)
            throws ReportGenerationException {

        log.info("Generating Change Stage Report for {} records, requested by: {}",
                changeRecords.size(), userId);

        // Step 1: Generate Excel report with retry logic
        byte[] reportBytes = generateExcelReportWithRetry(changeRecords);

        // Step 2: Upload to Blob Storage with retry logic
        String reportUrl = uploadReportWithRetry(reportBytes, userId);

        log.info("Report generated and uploaded successfully. URL: {}", reportUrl);
        return reportUrl;
    }

    /**
     * Generate Excel report with retry logic
     * Spec §2.5.1 Step 7: "If report generation fails, the system retries once for failed records"
     *
     * @param changeRecords List of change records
     * @return Excel file as byte array
     * @throws ReportGenerationException if generation fails after retry
     */
    private byte[] generateExcelReportWithRetry(List<OcmsChangeOfProcessing> changeRecords)
            throws ReportGenerationException {

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_GENERATION_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.warn("Retrying report generation (attempt {}/{})", attempt + 1, MAX_GENERATION_RETRIES + 1);
                    Thread.sleep(RETRY_DELAY_MS);
                }

                return generateExcelBytes(changeRecords);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ReportGenerationException("Report generation interrupted", e);
            } catch (Exception e) {
                lastException = e;
                log.error("Report generation failed (attempt {}/{}): {}",
                        attempt + 1, MAX_GENERATION_RETRIES + 1, e.getMessage());
            }
        }

        // All retry attempts exhausted
        log.error("Report generation failed after {} attempts. Stopping sub-flow as per spec.", MAX_GENERATION_RETRIES + 1);
        throw new ReportGenerationException(
                "Report generation failed after " + (MAX_GENERATION_RETRIES + 1) + " attempts: " + lastException.getMessage(),
                lastException);
    }

    /**
     * Upload report to Blob Storage with retry logic
     * Spec §2.5.1 Step 7: "If upload fails, the system retry once"
     *
     * @param reportBytes Excel file bytes
     * @param userId User who generated report
     * @return Report URL from Blob Storage
     * @throws ReportGenerationException if upload fails after retry
     */
    private String uploadReportWithRetry(byte[] reportBytes, String userId)
            throws ReportGenerationException {

        // Generate filename
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String fileName = String.format("ChangeStageReport_%s_%s.xlsx", timestamp, userId);
        String blobPath = "reports/change-stage/" + fileName;

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_UPLOAD_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.warn("Retrying report upload (attempt {}/{})", attempt + 1, MAX_UPLOAD_RETRIES + 1);
                    Thread.sleep(RETRY_DELAY_MS);
                }

                log.info("Uploading report to Blob Storage: {} (attempt {}/{})",
                        blobPath, attempt + 1, MAX_UPLOAD_RETRIES + 1);

                AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                        blobStorageUtil.uploadBytesToBlob(reportBytes, blobPath);

                if (uploadResponse.isSuccess()) {
                    String reportUrl = uploadResponse.getFileUrl();
                    log.info("Report uploaded successfully to: {}", reportUrl);
                    return reportUrl;
                } else {
                    String errorMsg = "Upload failed: " + uploadResponse.getErrorMessage();
                    lastException = new Exception(errorMsg);
                    log.error("Upload attempt {} failed: {}", attempt + 1, errorMsg);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ReportGenerationException("Report upload interrupted", e);
            } catch (Exception e) {
                lastException = e;
                log.error("Upload attempt {} failed with exception: {}", attempt + 1, e.getMessage());
            }
        }

        // All retry attempts exhausted
        log.error("Report upload failed after {} attempts. Stopping sub-flow as per spec.", MAX_UPLOAD_RETRIES + 1);
        throw new ReportGenerationException(
                "Report upload failed after " + (MAX_UPLOAD_RETRIES + 1) + " attempts: " + lastException.getMessage(),
                lastException);
    }

    /**
     * Generate Excel file as byte array
     * Separated from retry logic for cleaner code
     *
     * @param changeRecords List of change records
     * @return Excel file as byte array
     * @throws Exception if Excel generation fails
     */
    private byte[] generateExcelBytes(List<OcmsChangeOfProcessing> changeRecords) throws Exception {
        log.debug("Creating Excel workbook with {} records", changeRecords.size());

        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Change Processing Stage Report");

        // Create header row with styles
        createHeaderRow(sheet, workbook);

        // Populate data rows
        int rowNum = 1;
        for (OcmsChangeOfProcessing record : changeRecords) {
            createDataRow(sheet, rowNum++, record);
        }

        // Auto-size columns for better readability
        for (int i = 0; i < 16; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert workbook to byte array
        byte[] reportBytes;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            reportBytes = outputStream.toByteArray();
        }
        workbook.close();

        log.debug("Excel workbook generated successfully: {} bytes", reportBytes.length);
        return reportBytes;
    }

    /**
     * Create header row with column titles
     */
    private void createHeaderRow(Sheet sheet, Workbook workbook) {
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

        // Define column headers (15 columns as per spec)
        String[] headers = {
            "S/N",                              // 0
            "Notice Number",                    // 1
            "Offence Type",                     // 2
            "Offence Date & Time",              // 3
            "Vehicle No",                       // 4
            "Offender ID",                      // 5
            "Offender Name",                    // 6
            "Previous Processing Stage",        // 7
            "Previous Processing Date",         // 8
            "Current Processing Stage",         // 9
            "Current Processing Date",          // 10
            "Reason for Change",                // 11
            "Remarks",                          // 12
            "Authorised Officer",               // 13
            "Submitted Date",                   // 14
            "Source"                            // 15 - Added for tracking
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create data row for a change record
     */
    private void createDataRow(Sheet sheet, int rowNum, OcmsChangeOfProcessing record) {
        Row row = sheet.createRow(rowNum);

        // Get VON and ONOD data
        Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(record.getNoticeNo());
        OcmsValidOffenceNotice von = vonOpt.orElse(null);

        List<OcmsOffenceNoticeOwnerDriver> onods = onodService.findByNoticeNo(record.getNoticeNo());
        OcmsOffenceNoticeOwnerDriver onod = onods != null && !onods.isEmpty() ? onods.get(0) : null;

        // S/N
        row.createCell(0).setCellValue(rowNum);

        // Notice Number
        row.createCell(1).setCellValue(record.getNoticeNo());

        // Offence Type
        if (von != null && von.getOffenceType() != null) {
            row.createCell(2).setCellValue(von.getOffenceType());
        } else {
            row.createCell(2).setCellValue("");
        }

        // Offence Date & Time
        if (von != null && von.getOffenceTime() != null) {
            row.createCell(3).setCellValue(von.getOffenceTime().format(DATETIME_FORMATTER));
        } else {
            row.createCell(3).setCellValue("");
        }

        // Vehicle No
        if (von != null && von.getVehicleNo() != null) {
            row.createCell(4).setCellValue(von.getVehicleNo());
        } else {
            row.createCell(4).setCellValue("");
        }

        // Offender ID
        if (onod != null && onod.getIdNo() != null) {
            row.createCell(5).setCellValue(onod.getIdNo());
        } else {
            row.createCell(5).setCellValue("");
        }

        // Offender Name
        if (onod != null && onod.getName() != null) {
            row.createCell(6).setCellValue(onod.getName());
        } else {
            row.createCell(6).setCellValue("");
        }

        // Previous Processing Stage
        row.createCell(7).setCellValue(record.getLastProcessingStage() != null ?
                record.getLastProcessingStage() : "");

        // Previous Processing Date
        // Note: We show the date_of_change as when the change happened
        if (record.getDateOfChange() != null) {
            row.createCell(8).setCellValue(record.getDateOfChange().format(DATE_FORMATTER));
        } else {
            row.createCell(8).setCellValue("");
        }

        // Current Processing Stage (new stage after change)
        row.createCell(9).setCellValue(record.getNewProcessingStage() != null ?
                record.getNewProcessingStage() : "");

        // Current Processing Date (when change was applied)
        if (record.getDateOfChange() != null) {
            row.createCell(10).setCellValue(record.getDateOfChange().format(DATE_FORMATTER));
        } else {
            row.createCell(10).setCellValue("");
        }

        // Reason for Change
        row.createCell(11).setCellValue(record.getReasonOfChange() != null ?
                record.getReasonOfChange() : "");

        // Remarks
        row.createCell(12).setCellValue(record.getRemarks() != null ?
                record.getRemarks() : "");

        // Authorised Officer
        row.createCell(13).setCellValue(record.getAuthorisedOfficer() != null ?
                record.getAuthorisedOfficer() : "");

        // Submitted Date
        if (record.getCreDate() != null) {
            row.createCell(14).setCellValue(record.getCreDate().format(DATETIME_FORMATTER));
        } else {
            row.createCell(14).setCellValue("");
        }

        // Source (OCMS/PLUS/AVSS/SYSTEM)
        row.createCell(15).setCellValue(record.getSource() != null ?
                record.getSource() : "OCMS");
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
