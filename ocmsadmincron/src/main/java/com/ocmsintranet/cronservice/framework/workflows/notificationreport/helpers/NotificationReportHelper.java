package com.ocmsintranet.cronservice.framework.workflows.notificationreport.helpers;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import com.ocmsintranet.cronservice.framework.common.ExcelReportService;
import com.ocmsintranet.cronservice.framework.workflows.notificationreport.config.NotificationReportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for eNotification report generation.
 * This class handles data fetching and Excel report generation using the global Excel helper.
 *
 * This helper provides specialized functionality for generating eNotification reports:
 * 1. Fetches notification data from SMS and email tables using OcmsNotificationRecordsService
 * 2. Processes and categorizes the data into success and error records
 * 3. Generates Excel reports using the global ExcelReportService
 *
 * The Excel report contains three sheets:
 * - Summary: Key-value pairs showing notification statistics by processing stage (ENA, RD1, RD2, etc.)
 * - Error: Detailed list of failed notifications with error messages (12 columns)
 * - Success: Detailed list of successful notifications (11 columns)
 *
 * Data Sources:
 * - ocmsizmgr.ocms_email_notification_records (Email notifications)
 * - ocmsizmgr.ocms_sms_notification_records (SMS notifications)
 * - ocmsizmgr.ocms_offence_notice_owner_driver (Driver/owner information)
 *
 * The class uses the global ExcelReportService and ENotificationReportConfig for
 * consistent formatting and maintainable code.
 */
@Slf4j
@Component
public class NotificationReportHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ExcelReportService excelReportService;
    private final NotificationReportConfig reportConfig;

    @Autowired
    public NotificationReportHelper(
            JdbcTemplate jdbcTemplate,
            ExcelReportService excelReportService,
            NotificationReportConfig reportConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.excelReportService = excelReportService;
        this.reportConfig = reportConfig;
    }

    /**
     * Fetches notification records data from the database for the specified date
     *
     * @param reportDate The date for which to fetch data (format: yyyy-MM-dd)
     * @return Map containing the fetched data and statistics
     */
    public Map<String, Object> fetchNotificationReportsData(String reportDate) {
        log.info("[eNotif Report] Fetching notification data for date: {}", reportDate);

        try {
            // Get notification records using direct database query
            List<Map<String, Object>> allRecords = getNotificationRecordsForDate(reportDate);

            if (allRecords.isEmpty()) {
                log.info("[eNotif Report] No notification records found for date: {}", reportDate);
                return Collections.emptyMap();
            }

            // Separate success and error records
            List<Map<String, Object>> successRecords = allRecords.stream()
                    .filter(record -> "S".equalsIgnoreCase(String.valueOf(record.get("status"))))
                    .collect(Collectors.toList());

            List<Map<String, Object>> errorRecords = allRecords.stream()
                    .filter(record -> !"S".equalsIgnoreCase(String.valueOf(record.get("status"))))
                    .collect(Collectors.toList());

            log.info("[eNotif Report] Found {} total records for date: {}", allRecords.size(), reportDate);
            log.info("[eNotif Report] Success records: {}, Error records: {}", successRecords.size(), errorRecords.size());

            // Log sample records for debugging
            if (!successRecords.isEmpty()) {
                Map<String, Object> firstSuccessRecord = successRecords.get(0);
                log.info("[eNotif Report] Success record columns: {}", firstSuccessRecord.keySet());
                log.debug("[eNotif Report] Sample success record: {}", firstSuccessRecord);
            }

            if (!errorRecords.isEmpty()) {
                Map<String, Object> firstErrorRecord = errorRecords.get(0);
                log.info("[eNotif Report] Error record columns: {}", firstErrorRecord.keySet());
                log.debug("[eNotif Report] Sample error record: {}", firstErrorRecord);
            }

            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("allRecords", allRecords);
            result.put("successRecords", successRecords);
            result.put("errorRecords", errorRecords);
            result.put("totalCount", allRecords.size());
            result.put("successCount", successRecords.size());
            result.put("errorCount", errorRecords.size());

            return result;

        } catch (Exception e) {
            log.error("[eNotif Report] Error fetching notification data: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Generate Excel report using the global Excel generation framework
     *
     * @param reportData The data to include in the report
     * @param reportDate The date for which the report is generated
     * @return Byte array containing the Excel report
     */
    public byte[] generateExcelReport(Map<String, Object> reportData, String reportDate) {
        try {
            log.info("[eNotif Report] Generating Excel report using global framework");

            // Prepare summary data
            Map<String, Object> summaryData = prepareSummaryData(reportData, reportDate);

            // Prepare success and error records
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> successRecords = processRecordsForExcel(
                    (List<Map<String, Object>>) reportData.getOrDefault("successRecords", new ArrayList<>()));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errorRecords = processRecordsForExcel(
                    (List<Map<String, Object>>) reportData.getOrDefault("errorRecords", new ArrayList<>()));

            // Create sheet data list
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createKeyValueSheetData("Summary", summaryData),
                ExcelReportService.createMapListSheetData("Error", errorRecords),
                ExcelReportService.createMapListSheetData("Success", successRecords)
            );

            // Generate Excel using global service
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);
            
            log.info("[eNotif Report] Excel report generated successfully using global framework");
            return excelBytes;

        } catch (Exception e) {
            log.error("[eNotif Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate eNotification Excel report", e);
        }
    }

    /**
     * Prepare summary data for the Summary sheet
     */
    private Map<String, Object> prepareSummaryData(Map<String, Object> reportData, String reportDate) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allRecords = (List<Map<String, Object>>)
                reportData.getOrDefault("allRecords", new ArrayList<>());

        Map<String, Object> summaryData = new LinkedHashMap<>();

        // Report metadata
        summaryData.put("Date of report generation", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        summaryData.put("Time of report generation", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        // Set processing date to day before (report date)
        LocalDate processingDate = LocalDate.parse(reportDate);
        summaryData.put("Processing Date", processingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        summaryData.put("Processing Time", "00:00 - 23:59");

        // Calculate statistics for each processing stage with separators
        String[] stages = {"ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3"};

        for (int i = 0; i < stages.length; i++) {
            String stage = stages[i];
            Map<String, Long> stageStats = calculateStageStatistics(allRecords, stage);

            summaryData.put("Total no. of sms sent for " + stage, stageStats.get("totalSms"));
            summaryData.put("Total no. of successful sms sent for " + stage, stageStats.get("successSms"));
            summaryData.put("Total no. of failed sms sent for " + stage, stageStats.get("failedSms"));
            summaryData.put("Total no. of email sent for " + stage, stageStats.get("totalEmail"));
            summaryData.put("Total no. of successful emails sent for " + stage, stageStats.get("successEmail"));
            summaryData.put("Total no. of failed emails sent for " + stage, stageStats.get("failedEmail"));

            // Add separator row after each stage (except the last one)
            if (i < stages.length - 1) {
                summaryData.put("--- " + stage + " Summary End ---", "");
            }
        }

        return summaryData;
    }

    /**
     * Calculate statistics for a specific processing stage
     */
    private Map<String, Long> calculateStageStatistics(List<Map<String, Object>> records, String stage) {
        Map<String, Long> stats = new HashMap<>();

        // Filter records for this stage
        List<Map<String, Object>> stageRecords = records.stream()
                .filter(record -> stage.equals(record.get("processing_stage")))
                .collect(Collectors.toList());

        // Calculate SMS statistics
        long totalSms = stageRecords.stream()
                .filter(record -> "SMS".equals(record.get("type")))
                .count();

        long successSms = stageRecords.stream()
                .filter(record -> "SMS".equals(record.get("type")) && "S".equalsIgnoreCase(String.valueOf(record.get("status"))))
                .count();

        // Calculate Email statistics
        long totalEmail = stageRecords.stream()
                .filter(record -> "EMAIL".equals(record.get("type")))
                .count();

        long successEmail = stageRecords.stream()
                .filter(record -> "EMAIL".equals(record.get("type")) && "S".equalsIgnoreCase(String.valueOf(record.get("status"))))
                .count();

        stats.put("totalSms", totalSms);
        stats.put("successSms", successSms);
        stats.put("failedSms", totalSms - successSms);
        stats.put("totalEmail", totalEmail);
        stats.put("successEmail", successEmail);
        stats.put("failedEmail", totalEmail - successEmail);

        return stats;
    }


    /**
     * Process records for Excel generation, mapping database fields to Excel column names
     */
    private List<Map<String, Object>> processRecordsForExcel(List<Map<String, Object>> records) {
        List<Map<String, Object>> processedRecords = new ArrayList<>();

        int serialNumber = 1;
        for (Map<String, Object> record : records) {
            Map<String, Object> processedRecord = new LinkedHashMap<>();

            // Extract basic information
            processedRecord.put("serial_number", serialNumber++);
            processedRecord.put("notice_no", record.get("notice_no"));
            processedRecord.put("contact", record.get("contact"));
            processedRecord.put("name", record.get("name"));
            processedRecord.put("id_no", record.get("id_no"));
            processedRecord.put("id_type", record.get("id_type"));
            processedRecord.put("processing_stage", record.get("processing_stage"));

            String msgType = "";
            if (record.get("processing_stage").equals("ENA")) {
                msgType = "eNa";
            } else {
                msgType = "eReminder";
            }
            processedRecord.put("message_type", msgType); 

            // Error message (only for error records)
            processedRecord.put("error_message", record.get("msg_error"));

            // Process send date and time
            Object dateSentObj = record.get("date_sent");
            if (dateSentObj != null) {
                String dateSentStr = dateSentObj.toString();
                // Split date and time if in datetime format
                if (dateSentStr.contains(" ")) {
                    String[] parts = dateSentStr.split(" ", 2);
                    processedRecord.put("send_date", parts[0]);
                    processedRecord.put("send_time", parts.length > 1 ? parts[1] : "");
                } else {
                    processedRecord.put("send_date", dateSentStr);
                    processedRecord.put("send_time", "");
                }
            } else {
                processedRecord.put("send_date", "");
                processedRecord.put("send_time", "");
            }

            processedRecord.put("status", record.get("status"));

            processedRecords.add(processedRecord);
        }

        return processedRecords;
    }

    /**
     * Get notification records for a specific date
     *
     * Combines SMS and email notification records using UNION query
     * and joins with owner/driver information to get complete details.
     *
     * @param reportDate The date for which to fetch records (format: yyyy-MM-dd)
     * @return List of notification records with all relevant fields
     */
    public List<Map<String, Object>> getNotificationRecordsForDate(String reportDate) {
        log.info("[eNotif Service] Fetching notification records for date: {}", reportDate);

        String sql = """
            SELECT
                n.notice_no,
                n.processing_stage,
                CONVERT(VARCHAR(MAX), n.content) AS content,
                n.status,
                n.msg_error,
                n.cre_date,
                n.contact,
                n.date_sent,
                n.type,
                ood.name,
                ood.id_no,
                ood.id_type
            FROM (
                SELECT
                    notice_no,
                    processing_stage,
                    CONVERT(VARCHAR(MAX), content) AS content,
                    status,
                    msg_error,
                    cre_date,
                    email_addr AS contact,
                    date_sent,
                    'EMAIL' AS type
                FROM ocmsizmgr.ocms_email_notification_records
                WHERE date_sent BETWEEN ? AND ?

                UNION ALL

                SELECT
                    notice_no,
                    processing_stage,
                    CONVERT(VARCHAR(MAX), content) AS content,
                    status,
                    msg_status AS msg_error,
                    cre_date,
                    CONCAT(ISNULL(mobile_code, ''), mobile_no) AS contact,
                    date_sent,
                    'SMS' AS type
                FROM ocmsizmgr.ocms_sms_notification_records
                WHERE date_sent BETWEEN ? AND ?
            ) AS n
            LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS ood
                ON n.notice_no = ood.notice_no AND ood.offender_indicator = 'Y'
            ORDER BY n.date_sent DESC
            """;

        try {
            // Prepare date range parameters
            String startDate = reportDate + " 00:00:00.000";
            String endDate = reportDate + " 23:59:59.999";

            log.info("[eNotif Service] Executing query with date range: {} to {}", startDate, endDate);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, startDate, endDate, startDate, endDate);

            log.info("[eNotif Service] Found {} notification records for date: {}", results.size(), reportDate);

            // Log sample record for debugging
            if (!results.isEmpty()) {
                Map<String, Object> sampleRecord = results.get(0);
                log.info("[eNotif Service] Sample record columns: {}", sampleRecord.keySet());
                log.debug("[eNotif Service] Sample record data: {}", sampleRecord);
            }

            return results;

        } catch (Exception e) {
            log.error("[eNotif Service] Error fetching notification records for date {}: {}", reportDate, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch notification records", e);
        }
    }

    /**
     * Get notification records count by status for a specific date
     *
     * @param reportDate The date for which to get counts
     * @return Map with status counts
     */
    public Map<String, Object> getNotificationCountsByStatus(String reportDate) {
        log.info("[eNotif Service] Getting notification counts by status for date: {}", reportDate);

        String sql = """
            SELECT
                COUNT(*) as total_count,
                SUM(CASE WHEN UPPER(status) = 'S' THEN 1 ELSE 0 END) as success_count,
                SUM(CASE WHEN UPPER(status) != 'S' THEN 1 ELSE 0 END) as error_count,
                SUM(CASE WHEN type = 'SMS' THEN 1 ELSE 0 END) as sms_count,
                SUM(CASE WHEN type = 'EMAIL' THEN 1 ELSE 0 END) as email_count
            FROM (
                SELECT status, 'EMAIL' AS type
                FROM ocmsizmgr.ocms_email_notification_records
                WHERE date_sent BETWEEN ? AND ?

                UNION ALL

                SELECT status, 'SMS' AS type
                FROM ocmsizmgr.ocms_sms_notification_records
                WHERE date_sent BETWEEN ? AND ?
            ) AS n
            """;

        try {
            String startDate = reportDate + " 00:00:00.000";
            String endDate = reportDate + " 23:59:59.999";

            Map<String, Object> counts = jdbcTemplate.queryForMap(sql, startDate, endDate, startDate, endDate);

            log.info("[eNotif Service] Notification counts for {}: {}", reportDate, counts);
            return counts;

        } catch (Exception e) {
            log.error("[eNotif Service] Error getting notification counts for date {}: {}", reportDate, e.getMessage(), e);
            throw new RuntimeException("Failed to get notification counts", e);
        }
    }

    /**
     * Get notification records grouped by processing stage and type
     *
     * @param reportDate The date for which to get grouped data
     * @return List of grouped notification statistics
     */
    public List<Map<String, Object>> getNotificationStatsByStageAndType(String reportDate) {
        log.info("[eNotif Service] Getting notification stats by stage and type for date: {}", reportDate);

        String sql = """
            SELECT
                processing_stage,
                type,
                COUNT(*) as total_count,
                SUM(CASE WHEN UPPER(status) = 'S' THEN 1 ELSE 0 END) as success_count,
                SUM(CASE WHEN UPPER(status) != 'S' THEN 1 ELSE 0 END) as error_count
            FROM (
                SELECT processing_stage, status, 'EMAIL' AS type
                FROM ocmsizmgr.ocms_email_notification_records
                WHERE date_sent BETWEEN ? AND ?

                UNION ALL

                SELECT processing_stage, status, 'SMS' AS type
                FROM ocmsizmgr.ocms_sms_notification_records
                WHERE date_sent BETWEEN ? AND ?
            ) AS n
            GROUP BY processing_stage, type
            ORDER BY processing_stage, type
            """;

        try {
            String startDate = reportDate + " 00:00:00.000";
            String endDate = reportDate + " 23:59:59.999";

            List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql, startDate, endDate, startDate, endDate);

            log.info("[eNotif Service] Found {} processing stage/type combinations for date: {}", stats.size(), reportDate);
            return stats;

        } catch (Exception e) {
            log.error("[eNotif Service] Error getting notification stats for date {}: {}", reportDate, e.getMessage(), e);
            throw new RuntimeException("Failed to get notification statistics", e);
        }
    }
}
