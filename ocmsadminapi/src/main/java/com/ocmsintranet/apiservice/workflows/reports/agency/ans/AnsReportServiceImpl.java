package com.ocmsintranet.apiservice.workflows.reports.agency.ans;

import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportResponseDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for Advisory Notice System Reports (OCMS 10 Section 9.2)
 *
 * Generates Ad-Hoc ANS Reports with Excel export containing 2 tabs:
 * - Tab 1: ANS (eNotifications sent via SMS/Email)
 * - Tab 2: AN Letter (Physical letters sent to Toppan)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnsReportServiceImpl implements AnsReportService {

    private final EntityManager entityManager;
    private final AzureBlobStorageUtil blobStorageUtil;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public AnsReportResponseDto generateAdHocReport(AnsReportRequestDto request) throws AnsReportException {
        log.info("Generating Ad-Hoc ANS Report with criteria: eAN={}, letter={}, offence={}, vehicle={}, PP={}, carPark={}",
                request.getDateOfEansSent(), request.getDateOfAnsLetter(), request.getDateOfOffence(),
                request.getVehicleRegistrationType(), request.getPpCode(), request.getCarParkName());

        try {
            // Validate date parameters (no future dates allowed)
            validateDates(request);

            // Query ANS records (SMS/Email notifications)
            List<AnsRecord> ansRecords = queryAnsRecords(request);
            log.info("Found {} ANS records (eNotifications)", ansRecords.size());

            // Query AN Letter records
            List<AnLetterRecord> anLetterRecords = queryAnLetterRecords(request);
            log.info("Found {} AN Letter records", anLetterRecords.size());

            // Generate Excel workbook with 2 tabs
            byte[] excelBytes = generateExcelReport(ansRecords, anLetterRecords);

            // Upload to Blob Storage
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String fileName = String.format("%s-ANS-Report.xlsx", timestamp);
            String blobPath = "ans-reports/" + fileName;

            log.info("Uploading ANS Report to Blob Storage: {}", blobPath);
            AzureBlobStorageUtil.FileUploadResponse uploadResponse = blobStorageUtil.uploadBytesToBlob(excelBytes, blobPath);

            if (!uploadResponse.isSuccess()) {
                throw new AnsReportException("Failed to upload report to Blob Storage: " + uploadResponse.getErrorMessage());
            }

            // Build download URL
            String downloadUrl = buildDownloadUrl(blobPath);

            // Build response
            return AnsReportResponseDto.builder()
                    .downloadUrl(downloadUrl)
                    .fileName(fileName)
                    .fileSize((long) excelBytes.length)
                    .ansCount(ansRecords.size())
                    .anLetterCount(anLetterRecords.size())
                    .totalRecords(ansRecords.size() + anLetterRecords.size())
                    .generatedAt(LocalDateTime.now())
                    .searchCriteria(request)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate Ad-Hoc ANS Report", e);
            throw new AnsReportException("Report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that date parameters are not in the future
     */
    private void validateDates(AnsReportRequestDto request) throws AnsReportException {
        LocalDate today = LocalDate.now();

        if (request.getDateOfEansSent() != null) {
            LocalDate eansDate = LocalDate.parse(request.getDateOfEansSent());
            if (eansDate.isAfter(today)) {
                throw new AnsReportException("Date of eAN sent cannot be a future date");
            }
        }

        if (request.getDateOfAnsLetter() != null) {
            LocalDate letterDate = LocalDate.parse(request.getDateOfAnsLetter());
            if (letterDate.isAfter(today)) {
                throw new AnsReportException("Date of ANS letter cannot be a future date");
            }
        }

        if (request.getDateOfOffence() != null) {
            LocalDate offenceDate = LocalDate.parse(request.getDateOfOffence());
            if (offenceDate.isAfter(today)) {
                throw new AnsReportException("Date of offence cannot be a future date");
            }
        }
    }

    /**
     * Query ANS records (SMS/Email notifications) from database
     * Joins: ocms_sms_notification_records + ocms_email_notification_records + ocms_valid_offence_notice
     */
    private List<AnsRecord> queryAnsRecords(AnsReportRequestDto request) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        // SMS Records
        sql.append("SELECT v.notice_no, v.offence_date, v.vehicle_registration_type, ")
           .append("v.vehicle_registration_no, v.place_of_offence_pp_code, v.place_of_offence, ")
           .append("s.date_sent as notification_date, s.content, s.status, ")
           .append("CONCAT(ISNULL(s.mobile_code, ''), s.mobile_no) as contact, 'SMS' as type ")
           .append("FROM ocmsizmgr.ocms_sms_notification_records s ")
           .append("INNER JOIN ocmsizmgr.ocms_valid_offence_notice v ON s.notice_no = v.notice_no ")
           .append("WHERE v.an_flag = 'Y' ");

        paramIndex = addAnsFilters(sql, parameters, paramIndex, request, "s.date_sent", "v");

        sql.append(" UNION ALL ");

        // Email Records
        sql.append("SELECT v.notice_no, v.offence_date, v.vehicle_registration_type, ")
           .append("v.vehicle_registration_no, v.place_of_offence_pp_code, v.place_of_offence, ")
           .append("e.date_sent as notification_date, CONVERT(VARCHAR(MAX), e.content) as content, e.status, ")
           .append("e.email_addr as contact, 'EMAIL' as type ")
           .append("FROM ocmsizmgr.ocms_email_notification_records e ")
           .append("INNER JOIN ocmsizmgr.ocms_valid_offence_notice v ON e.notice_no = v.notice_no ")
           .append("WHERE v.an_flag = 'Y' ");

        paramIndex = addAnsFilters(sql, parameters, paramIndex, request, "e.date_sent", "v");

        sql.append(" ORDER BY notification_date DESC, notice_no");

        // Execute query
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Map results to DTOs
        List<AnsRecord> records = new ArrayList<>();
        for (Object[] row : results) {
            AnsRecord record = new AnsRecord();
            record.setNoticeNo(convertToString(row[0]));
            record.setOffenceDate(convertToLocalDate(row[1]));
            record.setVehicleRegistrationType(convertToString(row[2]));
            record.setVehicleRegistrationNo(convertToString(row[3]));
            record.setPpCode(convertToString(row[4]));
            record.setCarParkName(convertToString(row[5]));
            record.setNotificationDate(convertToLocalDateTime(row[6]));
            record.setContent(convertToString(row[7]));
            record.setStatus(convertToString(row[8]));
            record.setContact(convertToString(row[9]));
            record.setType(convertToString(row[10]));
            records.add(record);
        }

        return records;
    }

    /**
     * Query AN Letter records from database
     * From: ocms_an_letter + ocms_valid_offence_notice
     */
    private List<AnLetterRecord> queryAnLetterRecords(AnsReportRequestDto request) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        sql.append("SELECT l.notice_no, v.offence_date, v.vehicle_registration_type, ")
           .append("v.vehicle_registration_no, v.place_of_offence_pp_code, v.place_of_offence, ")
           .append("l.date_of_processing as letter_date, l.processing_stage, ")
           .append("l.owner_name, l.owner_blk_hse_no, l.owner_street, l.owner_floor, ")
           .append("l.owner_unit, l.owner_bldg, l.owner_postal_code ")
           .append("FROM ocmsizmgr.ocms_an_letter l ")
           .append("INNER JOIN ocmsizmgr.ocms_valid_offence_notice v ON l.notice_no = v.notice_no ")
           .append("WHERE v.an_flag = 'Y' ");

        paramIndex = addAnLetterFilters(sql, parameters, paramIndex, request, "l.date_of_processing", "v");

        sql.append(" ORDER BY letter_date DESC, l.notice_no");

        // Execute query
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Map results to DTOs
        List<AnLetterRecord> records = new ArrayList<>();
        for (Object[] row : results) {
            AnLetterRecord record = new AnLetterRecord();
            record.setNoticeNo(convertToString(row[0]));
            record.setOffenceDate(convertToLocalDate(row[1]));
            record.setVehicleRegistrationType(convertToString(row[2]));
            record.setVehicleRegistrationNo(convertToString(row[3]));
            record.setPpCode(convertToString(row[4]));
            record.setCarParkName(convertToString(row[5]));
            record.setLetterDate(convertToLocalDateTime(row[6]));
            record.setProcessingStage(convertToString(row[7]));
            record.setOwnerName(convertToString(row[8]));

            // Build full address
            StringBuilder address = new StringBuilder();
            appendIfNotNull(address, row[9]);  // blk/hse
            appendIfNotNull(address, row[10]); // street
            appendIfNotNull(address, row[11]); // floor
            appendIfNotNull(address, row[12]); // unit
            appendIfNotNull(address, row[13]); // building
            appendIfNotNull(address, row[14]); // postal code
            record.setOwnerAddress(address.toString().trim());

            records.add(record);
        }

        return records;
    }

    /**
     * Add filters for ANS records query
     */
    private int addAnsFilters(StringBuilder sql, List<Object> parameters, int paramIndex,
                             AnsReportRequestDto request, String dateSentColumn, String vonAlias) {
        // Date of eAN sent filter
        if (request.getDateOfEansSent() != null && !request.getDateOfEansSent().trim().isEmpty()) {
            sql.append(" AND CAST(").append(dateSentColumn).append(" AS DATE) = ?").append(paramIndex);
            parameters.add(request.getDateOfEansSent());
            paramIndex++;
        }

        // Date of offence filter
        if (request.getDateOfOffence() != null && !request.getDateOfOffence().trim().isEmpty()) {
            sql.append(" AND CAST(").append(vonAlias).append(".offence_date AS DATE) = ?").append(paramIndex);
            parameters.add(request.getDateOfOffence());
            paramIndex++;
        }

        // Vehicle registration type filter
        if (request.getVehicleRegistrationType() != null && !request.getVehicleRegistrationType().trim().isEmpty()) {
            sql.append(" AND ").append(vonAlias).append(".vehicle_registration_type = ?").append(paramIndex);
            parameters.add(request.getVehicleRegistrationType());
            paramIndex++;
        }

        // PP Code filter
        if (request.getPpCode() != null && !request.getPpCode().trim().isEmpty()) {
            sql.append(" AND ").append(vonAlias).append(".place_of_offence_pp_code = ?").append(paramIndex);
            parameters.add(request.getPpCode());
            paramIndex++;
        }

        // Car Park Name filter
        if (request.getCarParkName() != null && !request.getCarParkName().trim().isEmpty()) {
            sql.append(" AND LOWER(").append(vonAlias).append(".place_of_offence) LIKE ?").append(paramIndex);
            parameters.add("%" + request.getCarParkName().toLowerCase() + "%");
            paramIndex++;
        }

        return paramIndex;
    }

    /**
     * Add filters for AN Letter records query
     */
    private int addAnLetterFilters(StringBuilder sql, List<Object> parameters, int paramIndex,
                                  AnsReportRequestDto request, String letterDateColumn, String vonAlias) {
        // Date of ANS letter filter
        if (request.getDateOfAnsLetter() != null && !request.getDateOfAnsLetter().trim().isEmpty()) {
            sql.append(" AND CAST(").append(letterDateColumn).append(" AS DATE) = ?").append(paramIndex);
            parameters.add(request.getDateOfAnsLetter());
            paramIndex++;
        }

        // Date of offence filter
        if (request.getDateOfOffence() != null && !request.getDateOfOffence().trim().isEmpty()) {
            sql.append(" AND CAST(").append(vonAlias).append(".offence_date AS DATE) = ?").append(paramIndex);
            parameters.add(request.getDateOfOffence());
            paramIndex++;
        }

        // Vehicle registration type filter
        if (request.getVehicleRegistrationType() != null && !request.getVehicleRegistrationType().trim().isEmpty()) {
            sql.append(" AND ").append(vonAlias).append(".vehicle_registration_type = ?").append(paramIndex);
            parameters.add(request.getVehicleRegistrationType());
            paramIndex++;
        }

        // PP Code filter
        if (request.getPpCode() != null && !request.getPpCode().trim().isEmpty()) {
            sql.append(" AND ").append(vonAlias).append(".place_of_offence_pp_code = ?").append(paramIndex);
            parameters.add(request.getPpCode());
            paramIndex++;
        }

        // Car Park Name filter
        if (request.getCarParkName() != null && !request.getCarParkName().trim().isEmpty()) {
            sql.append(" AND LOWER(").append(vonAlias).append(".place_of_offence) LIKE ?").append(paramIndex);
            parameters.add("%" + request.getCarParkName().toLowerCase() + "%");
            paramIndex++;
        }

        return paramIndex;
    }

    /**
     * Generate Excel workbook with 2 tabs
     */
    private byte[] generateExcelReport(List<AnsRecord> ansRecords, List<AnLetterRecord> anLetterRecords) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        // Tab 1: ANS (eNotifications)
        Sheet ansSheet = workbook.createSheet("ANS");
        createAnsTab(ansSheet, workbook, ansRecords);

        // Tab 2: AN Letter
        Sheet anLetterSheet = workbook.createSheet("AN Letter");
        createAnLetterTab(anLetterSheet, workbook, anLetterRecords);

        // Convert to bytes
        byte[] bytes;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            bytes = outputStream.toByteArray();
        }
        workbook.close();

        return bytes;
    }

    /**
     * Create Tab 1: ANS (eNotifications)
     */
    private void createAnsTab(Sheet sheet, Workbook workbook, List<AnsRecord> records) {
        // Header row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {
            "S/N", "Notice No", "Offence Date", "Vehicle Type", "Vehicle No",
            "PP Code", "Car Park Name", "Notification Date", "Type", "Contact",
            "Status", "Content"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (AnsRecord record : records) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(rowNum);
            row.createCell(1).setCellValue(record.getNoticeNo() != null ? record.getNoticeNo() : "");
            row.createCell(2).setCellValue(record.getOffenceDate() != null ? record.getOffenceDate().format(DATE_FORMATTER) : "");
            row.createCell(3).setCellValue(record.getVehicleRegistrationType() != null ? record.getVehicleRegistrationType() : "");
            row.createCell(4).setCellValue(record.getVehicleRegistrationNo() != null ? record.getVehicleRegistrationNo() : "");
            row.createCell(5).setCellValue(record.getPpCode() != null ? record.getPpCode() : "");
            row.createCell(6).setCellValue(record.getCarParkName() != null ? record.getCarParkName() : "");
            row.createCell(7).setCellValue(record.getNotificationDate() != null ? record.getNotificationDate().format(DATETIME_FORMATTER) : "");
            row.createCell(8).setCellValue(record.getType() != null ? record.getType() : "");
            row.createCell(9).setCellValue(record.getContact() != null ? record.getContact() : "");
            row.createCell(10).setCellValue(record.getStatus() != null ? record.getStatus() : "");
            row.createCell(11).setCellValue(record.getContent() != null ? record.getContent() : "");
            rowNum++;
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create Tab 2: AN Letter
     */
    private void createAnLetterTab(Sheet sheet, Workbook workbook, List<AnLetterRecord> records) {
        // Header row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {
            "S/N", "Notice No", "Offence Date", "Vehicle Type", "Vehicle No",
            "PP Code", "Car Park Name", "Letter Date", "Processing Stage",
            "Owner Name", "Owner Address"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (AnLetterRecord record : records) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(rowNum);
            row.createCell(1).setCellValue(record.getNoticeNo() != null ? record.getNoticeNo() : "");
            row.createCell(2).setCellValue(record.getOffenceDate() != null ? record.getOffenceDate().format(DATE_FORMATTER) : "");
            row.createCell(3).setCellValue(record.getVehicleRegistrationType() != null ? record.getVehicleRegistrationType() : "");
            row.createCell(4).setCellValue(record.getVehicleRegistrationNo() != null ? record.getVehicleRegistrationNo() : "");
            row.createCell(5).setCellValue(record.getPpCode() != null ? record.getPpCode() : "");
            row.createCell(6).setCellValue(record.getCarParkName() != null ? record.getCarParkName() : "");
            row.createCell(7).setCellValue(record.getLetterDate() != null ? record.getLetterDate().format(DATETIME_FORMATTER) : "");
            row.createCell(8).setCellValue(record.getProcessingStage() != null ? record.getProcessingStage() : "");
            row.createCell(9).setCellValue(record.getOwnerName() != null ? record.getOwnerName() : "");
            row.createCell(10).setCellValue(record.getOwnerAddress() != null ? record.getOwnerAddress() : "");
            rowNum++;
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
        return style;
    }

    /**
     * Build download URL from blob path
     * Format: {first-cors-origin}/ocms/v1/download/blob/{blobPath}?container=ocms
     */
    private String buildDownloadUrl(String blobPath) {
        String cleanPath = blobPath.startsWith("/") ? blobPath.substring(1) : blobPath;
        String baseOrigin = corsAllowedOrigins;
        if (corsAllowedOrigins != null && corsAllowedOrigins.contains(",")) {
            baseOrigin = corsAllowedOrigins.split(",")[0].trim();
        }
        if (baseOrigin != null && baseOrigin.endsWith("/")) {
            baseOrigin = baseOrigin.substring(0, baseOrigin.length() - 1);
        }
        return baseOrigin + "/ocms/v1/download/blob/" + cleanPath + "?container=ocms";
    }

    // Helper methods for type conversion
    private String convertToString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private LocalDate convertToLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        return null;
    }

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime();
        return null;
    }

    private void appendIfNotNull(StringBuilder sb, Object obj) {
        if (obj != null && !obj.toString().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(obj.toString().trim());
        }
    }

    // Inner classes for data records
    @lombok.Data
    private static class AnsRecord {
        private String noticeNo;
        private LocalDate offenceDate;
        private String vehicleRegistrationType;
        private String vehicleRegistrationNo;
        private String ppCode;
        private String carParkName;
        private LocalDateTime notificationDate;
        private String content;
        private String status;
        private String contact;
        private String type; // SMS or EMAIL
    }

    @lombok.Data
    private static class AnLetterRecord {
        private String noticeNo;
        private LocalDate offenceDate;
        private String vehicleRegistrationType;
        private String vehicleRegistrationNo;
        private String ppCode;
        private String carParkName;
        private LocalDateTime letterDate;
        private String processingStage;
        private String ownerName;
        private String ownerAddress;
    }
}
