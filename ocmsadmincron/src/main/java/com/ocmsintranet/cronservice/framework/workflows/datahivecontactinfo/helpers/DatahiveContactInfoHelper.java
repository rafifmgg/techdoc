package com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.helpers;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import com.ocmsintranet.cronservice.framework.common.ExcelReportService;
import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.config.DatahiveContactInfoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class for Datahive Contact Information report generation.
 * This class handles data fetching and Excel report generation using the global Excel helper.
 *
 * This helper provides specialized functionality for generating Datahive Contact Information reports:
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
 * The class uses the global ExcelReportService and Datahive Contact InformationReportConfig for
 * consistent formatting and maintainable code.
 */
@Slf4j
@Component
public class DatahiveContactInfoHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ExcelReportService excelReportService;
    private final DatahiveContactInfoConfig reportConfig;

    @Autowired
    public DatahiveContactInfoHelper(
            JdbcTemplate jdbcTemplate,
            ExcelReportService excelReportService,
            DatahiveContactInfoConfig reportConfig) {
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
        log.info("[Datahive Contact Information Report] Fetching Datahive Contact Information data for date: {}", reportDate);

        try {
            // Use separate queries for success and error records
            List<Map<String, Object>> successRecords = getSuccessRecordsFromNotifications(reportDate);
            List<Map<String, Object>> errorRecords = getErrorRecordsFromSuspensions(reportDate);

            // Combine for total count
            List<Map<String, Object>> allRecords = new ArrayList<>();
            allRecords.addAll(successRecords);
            allRecords.addAll(errorRecords);

            if (allRecords.isEmpty()) {
                log.info("[Datahive Contact Information Report] No Datahive Contact Information records found for date: {} - generating empty report", reportDate);
            }

            log.info("[Datahive Contact Information Report] Found {} total records for date: {}", allRecords.size(), reportDate);
            log.info("[Datahive Contact Information Report] Success records: {}, Error records: {}", successRecords.size(), errorRecords.size());

            // Log sample records for debugging
            if (!successRecords.isEmpty()) {
                Map<String, Object> firstSuccessRecord = successRecords.get(0);
                log.info("[Datahive Contact Information Report] Success record columns: {}", firstSuccessRecord.keySet());
                log.debug("[Datahive Contact Information Report] Sample success record: {}", firstSuccessRecord);
            }

            if (!errorRecords.isEmpty()) {
                Map<String, Object> firstErrorRecord = errorRecords.get(0);
                log.info("[Datahive Contact Information Report] Error record columns: {}", firstErrorRecord.keySet());
                log.debug("[Datahive Contact Information Report] Sample error record: {}", firstErrorRecord);
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
            log.error("[Datahive Contact Information Report] Error fetching notification data: {}", e.getMessage(), e);
            // Return empty data structure instead of empty map to allow report generation
            Map<String, Object> result = new HashMap<>();
            result.put("allRecords", new ArrayList<>());
            result.put("successRecords", new ArrayList<>());
            result.put("errorRecords", new ArrayList<>());
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("errorCount", 0);
            return result;
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
            log.info("[Datahive Contact Information Report] Generating Excel report using global framework");

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
            
            log.info("[Datahive Contact Information Report] Excel report generated successfully using global framework");
            return excelBytes;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Datahive Contact Information Excel report", e);
        }
    }

    /**
     * Prepare summary data for the Summary sheet (DataHive format)
     */
    private Map<String, Object> prepareSummaryData(Map<String, Object> reportData, String reportDate) {
        Map<String, Object> summaryData = new LinkedHashMap<>();

        // Current date and time for report generation
        LocalDateTime now = LocalDateTime.now();
        String reportGenerationTime = now.format(DateTimeFormatter.ofPattern("HHmm")) + "hr";

        // Processing date (report date)
        LocalDate processingDate = LocalDate.parse(reportDate);
        LocalDateTime processingDateTime = processingDate.atStartOfDay();
        String processingTime = processingDateTime.format(DateTimeFormatter.ofPattern("HHmm")) + "hr";

        // DataHive summary statistics (7 rows as per specification)
        summaryData.put("Date of report generation", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        summaryData.put("Time of report generation", reportGenerationTime);
        summaryData.put("Processing Date", processingDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        summaryData.put("Processing Time", processingTime);

        // Calculate DataHive statistics correctly
        int successCount = (Integer) reportData.getOrDefault("successCount", 0);
        int errorCount = (Integer) reportData.getOrDefault("errorCount", 0);
        int totalSubmitted = successCount + errorCount; // Total records submitted = success + error
        int totalReturned = successCount; // Total returned = only successful records
        int recordsWithErrors = errorCount; // Error records

        summaryData.put("Total no. of records sumitted to DataHive", totalSubmitted);
        summaryData.put("Total no. of records returned by DataHive", totalReturned);
        summaryData.put("No. of records with errors", recordsWithErrors);

        return summaryData;
    }



    /**
     * Process records for Excel generation, mapping database fields to DataHive Excel format
     */
    private List<Map<String, Object>> processRecordsForExcel(List<Map<String, Object>> records) {
        List<Map<String, Object>> processedRecords = new ArrayList<>();

        int serialNumber = 1;
        for (Map<String, Object> record : records) {
            Map<String, Object> processedRecord = new LinkedHashMap<>();

            // S/N - Serial Number
            processedRecord.put("serial_number", serialNumber++);

            // NOTICE NO
            processedRecord.put("notice_no", record.get("notice_no"));

            // DATE/TIME OF OFFENCE - Use the actual database value
            Object offenceDate = record.get("offence_date_time");
            processedRecord.put("offence_date_time", formatToDataHiveFormat(offenceDate));

            // OFFENCE TYPE - Default to 'O' (Other/Standard)
            processedRecord.put("offence_type", record.getOrDefault("offence_type", "O"));

            // PROCESSING STAGE
            processedRecord.put("processing_stage", record.get("processing_stage"));

            // ID NO
            processedRecord.put("id_no", record.get("id_no"));

            // ID TYPE - Use provided value or determine from id_no format
            String idType = (String) record.get("id_type");
            if (idType == null || idType.isEmpty()) {
                idType = determineIdType(String.valueOf(record.get("id_no")));
            }
            processedRecord.put("id_type", idType);

            // NAME
            processedRecord.put("name", record.get("name"));

            // MOBILE NO./EMAIL - Handle different field names from different queries
            Object mobileEmail = record.get("mobile_email");
            if (mobileEmail == null) {
                mobileEmail = record.get("contact"); // Fallback for success records
            }
            processedRecord.put("mobile_email", mobileEmail);

            // DATE/TIME SENT - Use the actual database value
            Object dateSent = record.get("date_time_sent");
            String formattedDateSent = formatToDataHiveFormat(dateSent);
            processedRecord.put("date_time_sent", formattedDateSent);

            // DATASET - Determine from ID type
            processedRecord.put("dataset", determineDatasetFromIdType(idType));

            // DATE/TIME RECEIVED - Use the actual database value
            Object dateReceived = record.get("date_time_received");
            processedRecord.put("date_time_received", formatToDataHiveFormat(dateReceived));

            // ERROR MESSAGE - Handle different field names
            Object errorMessage = record.get("error_message");
            if (errorMessage == null) {
                errorMessage = record.get("msg_error"); // Fallback for success records
            }
            processedRecord.put("error_message", errorMessage);

            processedRecords.add(processedRecord);
        }

        return processedRecords;
    }

    /**
     * Format date/time values to dd/MM/yyyy HH:mm format (e.g., 09/09/2025 03:00)
     */
    private String formatToDataHiveFormat(Object dateTimeObj) {
        if (dateTimeObj == null) {
            return ""; // Return empty string for null values
        }

        try {
            LocalDateTime dateTime;

            // Handle different Java datetime types from SQL Server
            if (dateTimeObj instanceof Timestamp) {
                // SQL Timestamp object
                Timestamp timestamp = (Timestamp) dateTimeObj;
                dateTime = timestamp.toLocalDateTime();
            } else if (dateTimeObj instanceof java.sql.Date) {
                // SQL Date object
                java.sql.Date sqlDate = (java.sql.Date) dateTimeObj;
                dateTime = sqlDate.toLocalDate().atStartOfDay();
            } else if (dateTimeObj instanceof java.util.Date) {
                // Java Date object
                java.util.Date utilDate = (java.util.Date) dateTimeObj;
                dateTime = LocalDateTime.ofInstant(utilDate.toInstant(), java.time.ZoneId.systemDefault());
            } else if (dateTimeObj instanceof LocalDateTime) {
                // Already LocalDateTime
                dateTime = (LocalDateTime) dateTimeObj;
            } else if (dateTimeObj instanceof LocalDate) {
                // LocalDate - convert to start of day
                LocalDate localDate = (LocalDate) dateTimeObj;
                dateTime = localDate.atStartOfDay();
            } else {
                // Try parsing as string
                String dateTimeStr = dateTimeObj.toString().trim();
                if (dateTimeStr.isEmpty() || "null".equalsIgnoreCase(dateTimeStr)) {
                    return ""; // Return empty string for null/empty values
                }

                // Try parsing as LocalDateTime first
                if (dateTimeStr.contains("T")) {
                    // ISO format: remove Z if present and parse
                    String cleanDateStr = dateTimeStr.replace("Z", "");
                    dateTime = LocalDateTime.parse(cleanDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else if (dateTimeStr.contains(" ")) {
                    // Format: yyyy-MM-dd HH:mm:ss or similar
                    if (dateTimeStr.contains(".")) {
                        // Handle milliseconds: yyyy-MM-dd HH:mm:ss.SSS or yyyy-MM-dd HH:mm:ss.S
                        try {
                            dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                        } catch (Exception e2) {
                            // Try with single digit milliseconds
                            dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
                        }
                    } else {
                        // Format: yyyy-MM-dd HH:mm:ss
                        dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                } else if (dateTimeStr.length() == 10) {
                    // Format: yyyy-MM-dd
                    dateTime = LocalDate.parse(dateTimeStr).atStartOfDay();
                } else {
                    // Log the actual string format for debugging
                    log.warn("Unrecognized date format for value '{}' (type: {}), returning empty string", dateTimeStr, dateTimeObj.getClass().getSimpleName());
                    return "";
                }
            }

            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        } catch (Exception e) {
            log.warn("Failed to parse date/time: {} (type: {}), error: {}, returning empty string",
                     dateTimeObj, dateTimeObj.getClass().getSimpleName(), e.getMessage());
            return ""; // Return empty string instead of current time
        }
    }

    /**
     * Determine ID Type based on ID number format
     * N = NRIC, B = Business, F = FIN
     */
    private String determineIdType(String idNo) {
        if (idNo == null || idNo.isEmpty() || "null".equalsIgnoreCase(idNo)) {
            return "-"; // Default to NRIC
        }

        idNo = idNo.trim().toUpperCase();
        if (idNo.isEmpty()) {
            return "N";
        }

        char firstChar = idNo.charAt(0);

        // NRIC format: S/T + 7 digits + letter
        if (firstChar == 'S' || firstChar == 'T') {
            return "N";
        }

        // FIN format: F/G + 7 digits + letter
        if (firstChar == 'F' || firstChar == 'G') {
            return "F";
        }

        // Business registration: Usually starts with other letters (R, etc.)
        return "B";
    }


    /**
     * Determine dataset source based on ID type
     * Singpass for individuals (NRIC/FIN), Corppass for business
     */
    private String determineDatasetFromIdType(String idType) {
        // Business uses Corppass, individuals use Singpass
        if ("B".equals(idType)) {
            return "Corppass";
        } else if ("N".equals(idType) || "F".equals(idType)) {
            return "Singpass";
        } else {
            return "-";
        }
    }

    /**
     * Get error records from ocms_valid_offence_notice with suspension criteria
     *
     * @param reportDate The date for which to fetch error records (format: yyyy-MM-dd)
     * @return List of error records from suspension data
     */
    private List<Map<String, Object>> getErrorRecordsFromSuspensions(String reportDate) {
        log.info("[Datahive Contact Information Service] Fetching error records from suspensions for date: {}", reportDate);

        String sql = """
            SELECT
                von.notice_no,
                von.notice_date_and_time AS offence_date_time,
                ISNULL(von.offence_notice_type, 'O') AS offence_type,
                ISNULL(von.last_processing_stage, 'RD1') AS processing_stage,
                ood.id_no,
                ISNULL(ood.id_type, 'N') AS id_type,
                ood.name,
                COALESCE(ood.email_addr, CONCAT(ISNULL(ood.offender_tel_code, ''), ood.offender_tel_no), '') AS mobile_email,
                von.epr_date_of_suspension AS date_time_sent,
                von.epr_reason_of_suspension AS error_message,
                von.epr_date_of_suspension AS date_time_received
            FROM ocmsizmgr.ocms_valid_offence_notice von
            LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver ood
                ON von.notice_no = ood.notice_no AND ood.offender_indicator = 'Y'
            WHERE von.suspension_type = 'TS'
                AND von.epr_reason_of_suspension = 'SYS'
                AND CAST(von.epr_date_of_suspension AS DATE) = ?
            ORDER BY von.notice_no
            """;

        try {
            LocalDate date = LocalDate.parse(reportDate);
            List<Map<String, Object>> errorRecords = jdbcTemplate.queryForList(sql, date);

            log.info("[Datahive Contact Information Service] Found {} error records from suspensions", errorRecords.size());
            return errorRecords;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Service] Error fetching error records from suspensions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get success records from notification tables (SMS and Email)
     *
     * @param reportDate The date for which to fetch success records (format: yyyy-MM-dd)
     * @return List of successful notification records
     */
    private List<Map<String, Object>> getSuccessRecordsFromNotifications(String reportDate) {
        log.info("[Datahive Contact Information Service] Fetching success records from notification tables for date: {}", reportDate);

        String sql = """
            SELECT
                n.notice_no,
                n.processing_stage,
                n.status,
                n.msg_error,
                n.cre_date,
                n.contact AS mobile_email,
                n.date_sent AS date_time_sent,
                n.date_sent AS date_time_received,
                n.type,
                ood.name,
                ood.id_no,
                ood.id_type AS id_type,
                von.offence_notice_type AS offence_type,
                von.notice_date_and_time AS offence_date_time
            FROM (
                SELECT
                    notice_no,
                    processing_stage,
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
                    status,
                    msg_error,
                    cre_date,
                    CONCAT(ISNULL(mobile_code, ''), mobile_no) AS contact,
                    date_sent,
                    'SMS' AS type
                FROM ocmsizmgr.ocms_sms_notification_records
                WHERE date_sent BETWEEN ? AND ?
            ) n
            LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver ood
                ON n.notice_no = ood.notice_no AND (n.contact = ood.email_addr OR n.contact = CONCAT(ood.offender_tel_code, ood.offender_tel_no) )
            LEFT JOIN ocmsizmgr.ocms_valid_offence_notice von
                ON n.notice_no = von.notice_no
            ORDER BY n.notice_no, n.cre_date
            """;

        try {
            LocalDate date = LocalDate.parse(reportDate);
            LocalDate startDate = date;
            LocalDate endDate = date.plusDays(1);

            List<Map<String, Object>> successRecords = jdbcTemplate.queryForList(sql,
                startDate, endDate, startDate, endDate);

            log.info("[Datahive Contact Information Service] Found {} success records from notifications", successRecords.size());
            return successRecords;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Service] Error fetching success records from notifications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
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
        log.info("[Datahive Contact Information Service] Fetching notification records for date: {}", reportDate);

        String sql = """
            SELECT
                n.notice_no,
                n.processing_stage,
                n.status,
                n.msg_error,
                n.cre_date,
                n.contact,
                n.date_sent,
                n.type,
                ood.name,
                ood.id_no
            FROM (
                SELECT
                    notice_no,
                    processing_stage,
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
                    status,
                    msg_error,
                    cre_date,
                    CONCAT(ISNULL(mobile_code, ''), mobile_no) AS contact,
                    date_sent,
                    'SMS' AS type
                FROM ocmsizmgr.ocms_sms_notification_records
                WHERE date_sent BETWEEN ? AND ?
            ) AS n
            LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS ood
                ON n.notice_no = ood.notice_no AND (n.contact = ood.email_addr OR n.contact = CONCAT(ood.offender_tel_code, ood.offender_tel_no) )
            ORDER BY n.cre_date DESC
            """;

        try {
            // Prepare date range parameters
            String startDate = reportDate + " 00:00:00.000";
            String endDate = reportDate + " 23:59:59.999";

            log.info("[Datahive Contact Information Service] Executing query with date range: {} to {}", startDate, endDate);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, startDate, endDate, startDate, endDate);

            log.info("[Datahive Contact Information Service] Found {} notification records for date: {}", results.size(), reportDate);

            // Log sample record for debugging
            if (!results.isEmpty()) {
                Map<String, Object> sampleRecord = results.get(0);
                log.info("[Datahive Contact Information Service] Sample record columns: {}", sampleRecord.keySet());
                log.debug("[Datahive Contact Information Service] Sample record data: {}", sampleRecord);
            }

            return results;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Service] Error fetching notification records for date {}: {}", reportDate, e.getMessage(), e);
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
        log.info("[Datahive Contact Information Service] Getting notification counts by status for date: {}", reportDate);

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

            log.info("[Datahive Contact Information Service] Notification counts for {}: {}", reportDate, counts);
            return counts;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Service] Error getting notification counts for date {}: {}", reportDate, e.getMessage(), e);
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
        log.info("[Datahive Contact Information Service] Getting notification stats by stage and type for date: {}", reportDate);

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

            log.info("[Datahive Contact Information Service] Found {} processing stage/type combinations for date: {}", stats.size(), reportDate);
            return stats;

        } catch (Exception e) {
            log.error("[Datahive Contact Information Service] Error getting notification stats for date {}: {}", reportDate, e.getMessage(), e);
            throw new RuntimeException("Failed to get notification statistics", e);
        }
    }
}
