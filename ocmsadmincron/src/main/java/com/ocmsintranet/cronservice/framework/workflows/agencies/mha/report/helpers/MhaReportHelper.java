package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNoticeService;
import org.springframework.dao.EmptyResultDataAccessException;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class for MHA Daily report generation.
 * This class handles data fetching and Excel report generation.
 * 
 * This helper provides specialized functionality for generating Daily reports:
 * 1. Fetches Daily data from the database using OcmsSuspendedNoticeService with custom queries
 * 2. Processes and categorizes the data into success and error records
 * 3. Generates Excel reports with multiple sheets using Apache POI
 * 
 * The Excel report contains three sheets:
 * - Summary: Overall statistics including total records, success count, and error count
 * - Success: Detailed list of successfully processed Daily records
 * - Error: Records that have issues or are missing required fields
 * 
 * The custom query joins multiple tables:
 * - ocms_suspended_notice
 * - ocms_valid_offence_notice
 * - ocms_notice_owner_driver
 * - ocms_notice_owner_driver_addr
 * 
 * Note: This class requires Apache POI libraries for Excel generation.
 * The required dependencies have been added to build.gradle:
 * - org.apache.poi:poi:${apachePoiVersion}
 * - org.apache.poi:poi-ooxml:${apachePoiVersion}
 * 
 * The class uses SystemConstant values for consistent coding:
 * - SystemConstant.SuspensionType.TEMPORARY for suspension type
 * - SystemConstant.SuspensionReason.NRO for reason of suspension
 */
@Slf4j
@Component
public class MhaReportHelper {

    private final JdbcTemplate jdbcTemplate;
    private final OcmsSuspendedNoticeService ocmsSuspendedNoticeService;
    
    public MhaReportHelper(JdbcTemplate jdbcTemplate, OcmsSuspendedNoticeService ocmsSuspendedNoticeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.ocmsSuspendedNoticeService = ocmsSuspendedNoticeService;
    }

    /**
     * Fetches suspended notice data from the database for the specified date
     * 
     * @param reportDate The date for which to fetch data (format: yyyy-MM-dd)
     * @return Map containing the fetched data and statistics
     */
    public Map<String, Object> fetchSuspendedNoticesData(String reportDate) {
        log.info("[Report] Fetching suspended notice data for date: {}", reportDate);
        
        // Fetch MHA control metrics from the database for the report date
        // Using the same date for both start and end dates to maintain backward compatibility
        Map<String, Integer> controlMetrics = fetchMhaControlMetrics(reportDate, reportDate);
        log.info("[Report] MHA Control Metrics for {}: {}", reportDate, controlMetrics);
        
        try {
            // Query the database for success records (valid_offence_notice focus)
            List<Map<String, Object>> successRecords = ocmsSuspendedNoticeService.getSuccessRecordsForReport(reportDate);
            
            // Query the database for error records (suspended_notice focus)
            List<Map<String, Object>> errorRecords = ocmsSuspendedNoticeService.getErrorRecordsForReport(reportDate);
            // List<Map<String, Object>> errorRecords = Collections.emptyList();
            
            // Calculate total count
            int totalCount = successRecords.size() + errorRecords.size();
            
            if (totalCount == 0) {
                log.info("[Report] No records found for date: {}", reportDate);
                return Collections.emptyMap();
            }
            
            log.info("[Report] Found {} total records for date: {}", totalCount, reportDate);
            log.info("[Report] Success records: {}, Error records: {}", successRecords.size(), errorRecords.size());
            
            // Log sample records for debugging
            if (!successRecords.isEmpty()) {
                Map<String, Object> firstSuccessRecord = successRecords.get(0);
                log.info("[Report] Success record columns: {}", firstSuccessRecord.keySet());
                log.info("[Report] Sample success record: {}", firstSuccessRecord);
            }
            
            if (!errorRecords.isEmpty()) {
                Map<String, Object> firstErrorRecord = errorRecords.get(0);
                log.info("[Report] Error record columns: {}", firstErrorRecord.keySet());
                log.info("[Report] Sample error record: {}", firstErrorRecord);
            }
            
            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("successCount", successRecords.size());
            result.put("errorCount", errorRecords.size());
            result.put("successRecords", successRecords);
            result.put("errorRecords", errorRecords);
            
            // Add MHA control metrics to the result
            result.put("mhaControlMetrics", controlMetrics);
            
            return result;
            
        } catch (Exception e) {
            log.error("[Report] Error fetching suspended notice data: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    // Validation is now handled by separate queries in the repository

    /**
     * Generate Excel report with multiple sheets
     * @param reportData The data to include in the report
     * @param reportDate The date for which the report is generated
     * @return Byte array containing the Excel report
     */
    public byte[] generateExcelReport(Map<String, Object> reportData, String reportDate) {
        try {
            Workbook workbook = new XSSFWorkbook();
            
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle headerdataStyle = createHeaderDataStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle timeStyle = createTimeStyle(workbook);
            
            // Create Summary sheet first
            createSummarySheet(workbook, reportData, reportDate, titleStyle, headerStyle, headerdataStyle, dataStyle);
            
            // Create Success sheet
            createSuccessSheet(workbook, reportData, reportDate, titleStyle, headerStyle, dataStyle, dateStyle, timeStyle);
            
            // Create Error sheet
            createErrorSheet(workbook, reportData, reportDate, titleStyle, headerStyle, dataStyle, dateStyle, timeStyle);
            
            // Write workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            // Save a copy locally for testing
            // saveExcelLocally(outputStream.toByteArray(), reportDate);
            
            log.info("[Report] Excel report generated successfully");
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("[Report] Error generating Excel report", e);
            return null;
        }
    }

    /**
     * Create the Summary sheet in the Excel workbook
     */
    @SuppressWarnings("unchecked")
    private void createSummarySheet(Workbook workbook, Map<String, Object> reportData, 
            String reportDate, CellStyle titleStyle, CellStyle headerStyle, CellStyle headerdataStyle, CellStyle dataStyle) {
        
        Sheet sheet = workbook.createSheet("Summary");
        
        // Get MHA control metrics if available
        Map<String, Integer> mhaControlMetrics = (Map<String, Integer>) reportData.get("mhaControlMetrics");
        if (mhaControlMetrics == null) {
            mhaControlMetrics = new HashMap<>();
        }
        sheet.setColumnWidth(0, 15000); // Wider column for labels
        sheet.setColumnWidth(1, 6000);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("OCMS SUMMARY REPORT - MHA Particulars Check Result");
        titleCell.setCellStyle(titleStyle);
        
        // Empty row
        rowNum++;
        
        // Get success and error records
        List<Map<String, Object>> successRecords = (List<Map<String, Object>>) reportData.get("successRecords");
        List<Map<String, Object>> errorRecords = (List<Map<String, Object>>) reportData.get("errorRecords");
        
        if (successRecords == null) successRecords = new ArrayList<>();
        if (errorRecords == null) errorRecords = new ArrayList<>();
                
        // Filter records by processing stage
        // For "returned" counts - group by next_processing_stage
        Map<String, List<Map<String, Object>>> successByNextStage = new HashMap<>();
        Map<String, List<Map<String, Object>>> errorByNextStage = new HashMap<>();
        // For "submitted" counts - group by last_processing_stage
        Map<String, List<Map<String, Object>>> successByLastStage = new HashMap<>();
        Map<String, List<Map<String, Object>>> errorByLastStage = new HashMap<>();

        // Processing stages to track
        String[] stages = {
            SystemConstant.SuspensionReason.RD1, 
            SystemConstant.SuspensionReason.RD2, 
            SystemConstant.SuspensionReason.RR3, 
            SystemConstant.SuspensionReason.DN1, 
            SystemConstant.SuspensionReason.DN2, 
            SystemConstant.SuspensionReason.DR3
        };

        // Initialize stage maps
        for (String stage : stages) {
            successByNextStage.put(stage, new ArrayList<>());
            errorByNextStage.put(stage, new ArrayList<>());
            successByLastStage.put(stage, new ArrayList<>());
            errorByLastStage.put(stage, new ArrayList<>());
        }

        // Group success records by next_processing_stage (for returned) and last_processing_stage (for submitted)
        for (Map<String, Object> record : successRecords) {
            String nextStage = record.get("next_processing_stage") != null ?
                    record.get("next_processing_stage").toString() : "";
            String lastStage = record.get("last_processing_stage") != null ?
                    record.get("last_processing_stage").toString() : "";

            for (String knownStage : stages) {
                if (knownStage.equals(nextStage)) {
                    successByNextStage.get(knownStage).add(record);
                }
                if (knownStage.equals(lastStage)) {
                    successByLastStage.get(knownStage).add(record);
                }
            }
        }

        // Group error records by next_processing_stage (for returned) and last_processing_stage (for submitted)
        for (Map<String, Object> record : errorRecords) {
            String nextStage = record.get("next_processing_stage") != null ?
                    record.get("next_processing_stage").toString() : "";
            String lastStage = record.get("last_processing_stage") != null ?
                    record.get("last_processing_stage").toString() : "";

            for (String knownStage : stages) {
                if (knownStage.equals(nextStage)) {
                    errorByNextStage.get(knownStage).add(record);
                }
                if (knownStage.equals(lastStage)) {
                    errorByLastStage.get(knownStage).add(record);
                }
            }
        }
        
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        // Date of report generation (current date and time)
        Row genDateRow = sheet.createRow(rowNum++);
        Cell genDateLabelCell = genDateRow.createCell(0);
        genDateLabelCell.setCellValue("Date of report generation");
        genDateLabelCell.setCellStyle(headerStyle);
        
        Cell genDateValueCell = genDateRow.createCell(1);
        genDateValueCell.setCellValue(now.format(dateFormatter) + " " + now.format(timeFormatter));
        genDateValueCell.setCellStyle(dataStyle);
        
        // Processing Date (using reportDate parameter)
        Row procDateRow = sheet.createRow(rowNum++);
        Cell procDateLabelCell = procDateRow.createCell(0);
        procDateLabelCell.setCellValue("Processing start date and time");
        procDateLabelCell.setCellStyle(headerStyle);
        
        Cell procDateValueCell = procDateRow.createCell(1);
        // Parse and format the reportDate to ensure consistent format
        LocalDate reportDateObj = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
        procDateValueCell.setCellValue(reportDateObj.format(dateFormatter) + " 00:00");
        procDateValueCell.setCellStyle(dataStyle);
        
        // Processing Time
        Row procTimeRow = sheet.createRow(rowNum++);
        Cell procTimeLabelCell = procTimeRow.createCell(0);
        procTimeLabelCell.setCellValue("Processing end date and time");
        procTimeLabelCell.setCellStyle(headerStyle);
        
        Cell procTimeValueCell = procTimeRow.createCell(1);
        procTimeValueCell.setCellValue(reportDateObj.format(dateFormatter) + " 23:59");
        procTimeValueCell.setCellStyle(dataStyle);
                
        // Total records submitted to MHA
        Row totalSubmittedRow = sheet.createRow(rowNum++);
        Cell totalSubmittedLabelCell = totalSubmittedRow.createCell(0);
        totalSubmittedLabelCell.setCellValue("Total no. of records submitted to MHA");
        totalSubmittedLabelCell.setCellStyle(headerStyle);
        
        Cell totalSubmittedValueCell = totalSubmittedRow.createCell(1);
        totalSubmittedValueCell.setCellValue(mhaControlMetrics.get("recordsSubmitted"));
        totalSubmittedValueCell.setCellStyle(dataStyle);
        
        // Total records returned by MHA
        Row totalReturnedRow = sheet.createRow(rowNum++);
        Cell totalReturnedLabelCell = totalReturnedRow.createCell(0);
        totalReturnedLabelCell.setCellValue("Total no. of records returned by MHA");
        totalReturnedLabelCell.setCellStyle(headerStyle);

        Cell totalReturnedValueCell = totalReturnedRow.createCell(1);
        totalReturnedValueCell.setCellValue(mhaControlMetrics.get("recordsReturned"));
        totalReturnedValueCell.setCellStyle(dataStyle);
                
        // Number of records read
        Row recordsReadRow = sheet.createRow(rowNum++);
        Cell recordsReadLabelCell = recordsReadRow.createCell(0);
        recordsReadLabelCell.setCellValue("MHA Control Report - Number of records read");
        recordsReadLabelCell.setCellStyle(headerStyle);
        
        Cell recordsReadValueCell = recordsReadRow.createCell(1);
        recordsReadValueCell.setCellValue(mhaControlMetrics.get("recordsRead"));
        recordsReadValueCell.setCellStyle(dataStyle);
        
        // Number of records match
        Row recordsMatchRow = sheet.createRow(rowNum++);
        Cell recordsMatchLabelCell = recordsMatchRow.createCell(0);
        recordsMatchLabelCell.setCellValue("MHA Control Report - Number of records match");
        recordsMatchLabelCell.setCellStyle(headerStyle);
        
        Cell recordsMatchValueCell = recordsMatchRow.createCell(1);
        recordsMatchValueCell.setCellValue(mhaControlMetrics.get("recordsMatched"));
        recordsMatchValueCell.setCellStyle(dataStyle);
        
        // Number of records with invalid UIN/FIN
        Row recordsInvalidRow = sheet.createRow(rowNum++);
        Cell recordsInvalidLabelCell = recordsInvalidRow.createCell(0);
        recordsInvalidLabelCell.setCellValue("MHA Control Report - Number of records with invalid UIN/FIN");
        recordsInvalidLabelCell.setCellStyle(headerStyle);
        
        Cell recordsInvalidValueCell = recordsInvalidRow.createCell(1);
        recordsInvalidValueCell.setCellValue(mhaControlMetrics.get("invalidUinFinCount"));
        recordsInvalidValueCell.setCellStyle(dataStyle);
        
        // Number of records with valid UIN/FIN unmatched
        Row recordsUnmatchedRow = sheet.createRow(rowNum++);
        Cell recordsUnmatchedLabelCell = recordsUnmatchedRow.createCell(0);
        recordsUnmatchedLabelCell.setCellValue("MHA Control Report - Number of records with valid UIN/FIN unmatched");
        recordsUnmatchedLabelCell.setCellStyle(headerStyle);
        
        Cell recordsUnmatchedValueCell = recordsUnmatchedRow.createCell(1);
        recordsUnmatchedValueCell.setCellValue(mhaControlMetrics.get("validUnmatchedCount"));
        recordsUnmatchedValueCell.setCellStyle(dataStyle);
                
        // Empty row before processing stage breakdowns
        rowNum++;
        
        // Now add the processing stage breakdowns
        // RD1
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.RD1, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
        rowNum += 2; // Move past the two rows we added
        rowNum++;

        // RD2
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.RD2, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
        rowNum += 2; // Move past the two rows we added
        rowNum++;

        // RR3
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.RR3, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
        rowNum += 2; // Move past the two rows we added
        rowNum++;

        // DN1
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.DN1, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
        rowNum += 2; // Move past the two rows we added
        rowNum++;

        // DN2
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.DN2, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
        rowNum += 2; // Move past the two rows we added
        rowNum++;

        // DR3
        addProcessingStageSection(sheet, rowNum, SystemConstant.SuspensionReason.DR3, successByLastStage, errorByLastStage, successByNextStage, errorByNextStage, headerStyle, headerdataStyle, dataStyle);
    }
    
    /**
     * Create the Success sheet in the Excel workbook
     */
    private void createSuccessSheet(Workbook workbook, Map<String, Object> reportData, 
            String reportDate, CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle,
            CellStyle dateStyle, CellStyle timeStyle) {
        
        Sheet sheet = workbook.createSheet("Success");
        sheet.setColumnWidth(0, 2000); // S/N
        sheet.setColumnWidth(1, 3000); // Processing Date
        sheet.setColumnWidth(2, 3000); // Processing Time
        sheet.setColumnWidth(3, 3000); // Current Processing Stage
        sheet.setColumnWidth(4, 3000); // Next Processing Stage
        sheet.setColumnWidth(5, 3000); // Notice number
        sheet.setColumnWidth(6, 3000); // ID number
        sheet.setColumnWidth(7, 3000); // Date of Offence
        sheet.setColumnWidth(8, 3000); // Time of Offence
        
        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "S/N", headerStyle);
        createCell(headerRow, 1, "Processing Date", headerStyle);
        createCell(headerRow, 2, "Processing Time", headerStyle);
        createCell(headerRow, 3, "Current Processing Stage", headerStyle);
        createCell(headerRow, 4, "Next Processing Stage", headerStyle);
        createCell(headerRow, 5, "Notice Number", headerStyle);
        createCell(headerRow, 6, "ID Number", headerStyle);
        createCell(headerRow, 7, "Date of Offence", headerStyle);
        createCell(headerRow, 8, "Time of Offence", headerStyle);
        
        // Data rows
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> successRecords = 
                (List<Map<String, Object>>) reportData.getOrDefault("successRecords", new ArrayList<>());
        
        int rowNum = 1; // Start after header row
        for (Map<String, Object> record : successRecords) {
            Row row = sheet.createRow(rowNum++);
            
            // Extract date and time from processing_date_time
            String processingDate = "";
            String processingTime = "";
            if (record.get("processing_date_time") != null) {
                String dateTimeStr = record.get("processing_date_time").toString();
                String[] parts = dateTimeStr.split("\\s+");
                if (parts.length > 0) {
                    processingDate = parts[0];
                    if (parts.length > 1) {
                        // Take only first 5 chars (HH:MM)
                        processingTime = parts[1].length() > 5 ? parts[1].substring(0, 5) : parts[1];
                    }
                }
            }
            
            // Extract date and time from notice_date_and_time for Date of Offence and Time of Offence
            String dateOfOffence = "";
            String timeOfOffence = "";
            if (record.get("notice_date_and_time") != null) {
                String noticeDateTimeStr = record.get("notice_date_and_time").toString();
                String[] noticeParts = noticeDateTimeStr.split("\\s+");
                if (noticeParts.length > 0) {
                    dateOfOffence = noticeParts[0];
                    if (noticeParts.length > 1) {
                        // Take only first 5 chars (HH:MM)
                        timeOfOffence = noticeParts[1].length() > 5 ? noticeParts[1].substring(0, 5) : noticeParts[1];
                    }
                }
            }
            
            // Add S/N (rowNum - 1 because we started from row 1 after header)
            createCell(row, 0, rowNum - 1, dataStyle); // S/N
            createCell(row, 1, processingDate, dateStyle); // Processing Date
            createCell(row, 2, processingTime, timeStyle); // Processing Time
            createCell(row, 3, record.get("last_processing_stage"), dataStyle); // Current Processing Stage
            createCell(row, 4, record.get("next_processing_stage"), dataStyle); // Next Processing Stage
            createCell(row, 5, record.get("notice_no"), dataStyle); // Notice number
            createCell(row, 6, record.get("id_no"), dataStyle); // ID number
            createCell(row, 7, dateOfOffence, dateStyle); // Date of Offence from notice_date_and_time
            createCell(row, 8, timeOfOffence, timeStyle); // Time of Offence from notice_date_and_time
        }
    }

    /**
     * Create the Error sheet in the Excel workbook
     */
    private void createErrorSheet(Workbook workbook, Map<String, Object> reportData, 
            String reportDate, CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle,
            CellStyle dateStyle, CellStyle timeStyle) {
        
        Sheet sheet = workbook.createSheet("Error");
        sheet.setColumnWidth(0, 2000); // S/N
        sheet.setColumnWidth(1, 3000); // Processing Date
        sheet.setColumnWidth(2, 3000); // Processing Time
        sheet.setColumnWidth(3, 4000); // ID Number
        sheet.setColumnWidth(4, 3000); // Notice Number
        sheet.setColumnWidth(5, 5000); // MHA Error Response Code
        sheet.setColumnWidth(6, 3000); // Current Processing Stage
        sheet.setColumnWidth(7, 3000); // Next Processing Stage
        sheet.setColumnWidth(8, 3000); // Date of Offence
        sheet.setColumnWidth(9, 3000); // Time of Offence
        sheet.setColumnWidth(10, 3000); // Suspension Type
        sheet.setColumnWidth(11, 5000); // EPR Reason of Suspension
        
        // Header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "S/N", headerStyle);
        createCell(headerRow, 1, "Processing Date", headerStyle);
        createCell(headerRow, 2, "Processing Time", headerStyle);
        createCell(headerRow, 3, "ID Number", headerStyle);
        createCell(headerRow, 4, "Notice Number", headerStyle);
        createCell(headerRow, 5, "MHA Error Response Code", headerStyle);
        createCell(headerRow, 6, "Current Processing Stage", headerStyle);
        createCell(headerRow, 7, "Next Processing Stage", headerStyle);
        createCell(headerRow, 8, "Date of Offence", headerStyle);
        createCell(headerRow, 9, "Time of Offence", headerStyle);
        createCell(headerRow, 10, "Suspension Type", headerStyle);
        createCell(headerRow, 11, "EPR Reason of Suspension", headerStyle);
        
        // Data rows
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errorRecords = 
                (List<Map<String, Object>>) reportData.getOrDefault("errorRecords", new ArrayList<>());
        
        int rowNum = 1; // Start after header row
        for (Map<String, Object> record : errorRecords) {
            Row row = sheet.createRow(rowNum++);
            
            // Extract date and time from processing_date_time
            String processingDate = "";
            String processingTime = "";
            if (record.get("processing_date_time") != null) {
                String dateTimeStr = record.get("processing_date_time").toString();
                String[] parts = dateTimeStr.split("\\s+");
                if (parts.length > 0) {
                    processingDate = parts[0];
                    if (parts.length > 1) {
                        // Take only first 5 chars (HH:MM)
                        processingTime = parts[1].length() > 5 ? parts[1].substring(0, 5) : parts[1];
                    }
                }
            }
            
            // Extract date and time from notice_date_and_time for Date of Offence and Time of Offence
            String dateOfOffence = "";
            String timeOfOffence = "";
            if (record.get("notice_date_and_time") != null) {
                String noticeDateTimeStr = record.get("notice_date_and_time").toString();
                String[] noticeParts = noticeDateTimeStr.split("\\s+");
                if (noticeParts.length > 0) {
                    dateOfOffence = noticeParts[0];
                    if (noticeParts.length > 1) {
                        // Take only first 5 chars (HH:MM)
                        timeOfOffence = noticeParts[1].length() > 5 ? noticeParts[1].substring(0, 5) : noticeParts[1];
                    }
                }
            }
            
            // Add S/N (rowNum - 1 because we started from row 1 after header)
            createCell(row, 0, rowNum - 1, dataStyle); // S/N
            createCell(row, 1, processingDate, dateStyle); // Processing Date
            createCell(row, 2, processingTime, timeStyle); // Processing Time
            createCell(row, 3, record.get("id_no"), dataStyle); // ID Number
            createCell(row, 4, record.get("notice_no"), dataStyle); // Notice Number
            createCell(row, 5, record.get("suspension_remarks"), dataStyle); // MHA Error Response Code
            createCell(row, 6, record.get("last_processing_stage"), dataStyle); // Current Processing Stage
            createCell(row, 7, record.get("next_processing_stage"), dataStyle); // Next Processing Stage
            createCell(row, 8, dateOfOffence, dateStyle); // Date of Offence from notice_date_and_time
            createCell(row, 9, timeOfOffence, timeStyle); // Time of Offence from notice_date_and_time
            createCell(row, 10, record.get("suspension_type"), dataStyle); // Suspension Type
            createCell(row, 11, record.get("epr_reason_of_suspension"), dataStyle); // EPR Reason of Suspension
        }
    }
    
    /**
     * Add processing stage section to the summary sheet
     * @param successByLastStage Success records grouped by last_processing_stage (for submitted count)
     * @param errorByLastStage Error records grouped by last_processing_stage (for submitted count)
     * @param successByNextStage Success records grouped by next_processing_stage (for returned count)
     * @param errorByNextStage Error records grouped by next_processing_stage (for returned count)
     */
    private void addProcessingStageSection(Sheet sheet, int startRow, String stage,
            Map<String, List<Map<String, Object>>> successByLastStage,
            Map<String, List<Map<String, Object>>> errorByLastStage,
            Map<String, List<Map<String, Object>>> successByNextStage,
            Map<String, List<Map<String, Object>>> errorByNextStage,
            CellStyle headerStyle, CellStyle headerdataStyle, CellStyle dataStyle) {

        // Get records by last_processing_stage for submitted count
        List<Map<String, Object>> successByLast = successByLastStage.get(stage);
        List<Map<String, Object>> errorByLast = errorByLastStage.get(stage);
        int submittedTotal = (successByLast != null ? successByLast.size() : 0) +
                            (errorByLast != null ? errorByLast.size() : 0);

        // Get records by next_processing_stage for returned count
        List<Map<String, Object>> successByNext = successByNextStage.get(stage);
        List<Map<String, Object>> errorByNext = errorByNextStage.get(stage);
        int returnedTotal = (successByNext != null ? successByNext.size() : 0) +
                           (errorByNext != null ? errorByNext.size() : 0);

        // Total submitted - using last_processing_stage
        Row totalSubmittedRow = sheet.createRow(startRow++);
        Cell totalSubmittedLabelCell = totalSubmittedRow.createCell(0);
        totalSubmittedLabelCell.setCellValue("Total no. of records submitted for " + stage);
        totalSubmittedLabelCell.setCellStyle(headerdataStyle);

        Cell totalSubmittedValueCell = totalSubmittedRow.createCell(1);
        totalSubmittedValueCell.setCellValue(submittedTotal);
        totalSubmittedValueCell.setCellStyle(dataStyle);

        // Total returned - using next_processing_stage
        Row totalReturnedRow = sheet.createRow(startRow++);
        Cell totalReturnedLabelCell = totalReturnedRow.createCell(0);
        totalReturnedLabelCell.setCellValue("Total no. of records returned for " + stage);
        totalReturnedLabelCell.setCellStyle(headerdataStyle);

        Cell totalReturnedValueCell = totalReturnedRow.createCell(1);
        totalReturnedValueCell.setCellValue(returnedTotal);
        totalReturnedValueCell.setCellStyle(dataStyle);
    }
    
    /**
     * Create a cell in the Excel sheet
     */
    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof java.util.Date) {
                // For Date objects, use setCellValue(Date) to ensure proper date formatting
                cell.setCellValue((java.util.Date) value);
            } else if (value instanceof LocalDate) {
                // Convert LocalDate to java.util.Date
                LocalDate localDate = (LocalDate) value;
                java.util.Date date = java.sql.Date.valueOf(localDate);
                cell.setCellValue(date);
            } else if (value instanceof LocalDateTime) {
                // Convert LocalDateTime to java.util.Date
                LocalDateTime localDateTime = (LocalDateTime) value;
                java.util.Date date = java.sql.Timestamp.valueOf(localDateTime);
                cell.setCellValue(date);
            } else {
                String strValue = value.toString();
                // Try to parse date strings in format yyyy-MM-dd
                if (strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    try {
                        LocalDate localDate = LocalDate.parse(strValue);
                        java.util.Date date = java.sql.Date.valueOf(localDate);
                        cell.setCellValue(date);
                    } catch (Exception e) {
                        cell.setCellValue(strValue);
                    }
                } else {
                    cell.setCellValue(strValue);
                }
            }
        } else {
            cell.setCellValue("");
        }
        
        cell.setCellStyle(style);
    }

    /**
     * Create header style for Excel sheet
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11 and bold
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in points
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }

    /**
     * Create header data style for Excel sheet
     */
    private CellStyle createHeaderDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11 and bold
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in points
        style.setFont(font);
        
        return style;
    }

    /**
     * Create data style for Excel sheet
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in points
        style.setFont(font);
        
        // Center alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    /**
     * Create date style for Excel sheet (dd/MM/yyyy format)
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in points
        style.setFont(font);
        
        // Center alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // Date format dd/MM/yyyy - using Excel's built-in format code
        CreationHelper createHelper = workbook.getCreationHelper();
        short dateFormat = createHelper.createDataFormat().getFormat("dd/MM/yyyy");
        style.setDataFormat(dateFormat);
        
        return style;
    }
    
    /**
     * Create time style for Excel sheet (HH:mm format)
     */
    private CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in points
        style.setFont(font);
        
        // Center alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // Time format HH:mm - using Excel's built-in format code
        CreationHelper createHelper = workbook.getCreationHelper();
        short timeFormat = createHelper.createDataFormat().getFormat("HH:mm");
        style.setDataFormat(timeFormat);
        
        return style;
    }

    /**
     * Create title style for Excel sheet
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Create Calibri font with size 11
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeight(11); // Font height is in 1/20th of a point
        style.setFont(font);
        
        return style;
    }
    
    /**
     * Fetches MHA control report metrics from the ocms_batch_job table for a date range
     * @param startDate The start date in yyyy-MM-dd format
     * @param endDate The end date in yyyy-MM-dd format
     * @return Map containing the accumulated metrics or empty map if not found
     */
    public Map<String, Integer> fetchMhaControlMetrics(String startDate, String endDate) {
        Map<String, Integer> metrics = new HashMap<>();

        try {
            // Set default values to 0
            metrics.put("recordsSubmitted", 0);
            metrics.put("recordsReturned", 0);
            metrics.put("recordsRead", 0);
            metrics.put("recordsMatched", 0);
            metrics.put("invalidUinFinCount", 0);
            metrics.put("validUnmatchedCount", 0);

            // Parse the date range
            LocalDate startDateObj = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate endDateObj = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

            // Query to get recordsSubmitted from MHA upload job
            String uploadSql = "SELECT log_text FROM ocmsizmgr.ocms_batch_job " +
                       "WHERE name = 'generate_mha_files' " +
                       "AND CONVERT(date, start_run) BETWEEN ? AND ? " +
                       "ORDER BY start_run";

            log.debug("Fetching MHA upload metrics (recordsSubmitted) from {} to {}", startDate, endDate);

            // Execute query for upload job and process results
            List<String> uploadLogTexts = jdbcTemplate.queryForList(
                uploadSql,
                String.class,
                java.sql.Date.valueOf(startDateObj),
                java.sql.Date.valueOf(endDateObj)
            );

            // Process each upload log text to get recordsSubmitted
            for (String logText : uploadLogTexts) {
                if (logText != null && !logText.isEmpty()) {
                    extractMetricsFromText(logText, metrics);
                }
            }

            // Query to get recordsReturned and other metrics from MHA download job (process_mha_files)
            String downloadSql = "SELECT log_text FROM ocmsizmgr.ocms_batch_job " +
                       "WHERE name = 'process_mha_files' " +
                       "AND CONVERT(date, start_run) BETWEEN ? AND ? " +
                       "ORDER BY start_run";

            log.debug("Fetching MHA download metrics (recordsReturned) from {} to {}", startDate, endDate);

            // Execute query for download job and process results
            List<String> downloadLogTexts = jdbcTemplate.queryForList(
                downloadSql,
                String.class,
                java.sql.Date.valueOf(startDateObj),
                java.sql.Date.valueOf(endDateObj)
            );

            // Process each download log text to get recordsReturned and other metrics
            for (String logText : downloadLogTexts) {
                if (logText != null && !logText.isEmpty()) {
                    extractMetricsFromText(logText, metrics);
                }
            }

            log.info("Fetched MHA control metrics from {} to {}: {}",
                    startDate, endDate, metrics);

        } catch (EmptyResultDataAccessException e) {
            log.warn("No MHA jobs found between {} and {}", startDate, endDate);
        } catch (Exception e) {
            log.error("Error fetching MHA control metrics from database", e);
        }

        return metrics;
    }
    
    /**
     * Fallback method to extract metrics from log text using string parsing
     * @param logText The log text to parse
     * @param metrics The metrics map to populate
     */
    /**
     * Extracts metrics from log text and accumulates them in the provided metrics map.
     * Uses Map.merge() to sum values when the same key is encountered multiple times.
     * 
     * @param logText The log text containing metrics in "key: value" format after "Metrics:" marker
     * @param metrics The map to accumulate metrics into (key: metric name, value: accumulated value)
     */
    private void extractMetricsFromText(String logText, Map<String, Integer> metrics) {
        // Look for the Metrics: section in the log text
        int metricsIndex = logText.indexOf("Metrics:");
        if (metricsIndex < 0) {
            log.warn("No 'Metrics:' section found in log text");
            return;
        }
        
        // Get the metrics part of the string
        String metricsStr = logText.substring(metricsIndex + "Metrics:".length()).trim();
        log.debug("Extracting metrics from: {}", metricsStr);
        
        // Split by commas and process each key-value pair
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
     * Save Excel file locally for testing purposes
     * 
     * @param excelData The Excel file as byte array
     * @param reportDate The report date used in the filename
     */
    private void saveExcelLocally(byte[] excelData, String reportDate) {
        try {
            // Define file path - using user home directory for easy access
            String userHome = System.getProperty("user.home");
            String fileName = "MHA_Report_" + reportDate.replace("-", "") + ".xlsx";
            String filePath = userHome + "/MHA_Reports/";
            
            // Create directory if it doesn't exist
            java.io.File directory = new java.io.File(filePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Write file
            java.io.File file = new java.io.File(filePath + fileName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(excelData);
            }
            
            log.info("[Report] Excel file saved locally for testing at: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("[Report] Error saving Excel file locally: {}", e.getMessage(), e);
        }
    }
}
