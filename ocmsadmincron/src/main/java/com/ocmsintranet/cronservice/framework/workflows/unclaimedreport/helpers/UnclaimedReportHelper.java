package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.helpers;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import com.ocmsintranet.cronservice.framework.common.ExcelReportService;
import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.config.UnclaimedReportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Helper class for Unclaimed Batch Data report generation.
 * This class handles data fetching and Excel report generation using the global Excel helper.
 *
 * Based on OCMS 20 Specification:
 * - Queries ocms_valid_offence_notice, ocms_offence_notice_owner_driver, and ocms_suspended_notice
 * - Finds all notices with active TS-UNC suspensions
 * - Generates Excel report with 11 columns showing unclaimed reminder details
 *
 * Data Sources:
 * - ocmsizmgr.ocms_valid_offence_notice (Notice information)
 * - ocmsizmgr.ocms_offence_notice_owner_driver (Owner/driver information)
 * - ocmsizmgr.ocms_suspended_notice (Suspension records)
 */
@Slf4j
@Component
public class UnclaimedReportHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ExcelReportService excelReportService;
    private final UnclaimedReportConfig reportConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public UnclaimedReportHelper(
            JdbcTemplate jdbcTemplate,
            ExcelReportService excelReportService,
            UnclaimedReportConfig reportConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.excelReportService = excelReportService;
        this.reportConfig = reportConfig;
    }

    /**
     * Fetches unclaimed batch data from the database
     *
     * @return Map containing the fetched data
     */
    public Map<String, Object> fetchUnclaimedBatchData() {
        log.info("[Unclaimed Report] Fetching unclaimed batch data");

        try {
            // Get all notices with active TS-UNC suspensions
            List<Map<String, Object>> unclaimedRecords = getUnclaimedNotices();

            if (unclaimedRecords.isEmpty()) {
                log.info("[Unclaimed Report] No unclaimed notices found");
                return Collections.emptyMap();
            }

            log.info("[Unclaimed Report] Found {} unclaimed notices", unclaimedRecords.size());

            // Log sample records for debugging
            if (!unclaimedRecords.isEmpty()) {
                Map<String, Object> firstRecord = unclaimedRecords.get(0);
                log.info("[Unclaimed Report] Record columns: {}", firstRecord.keySet());
                log.debug("[Unclaimed Report] Sample record: {}", firstRecord);
            }

            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("unclaimedRecords", unclaimedRecords);
            result.put("totalCount", unclaimedRecords.size());

            return result;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error fetching unclaimed batch data: {}", e.getMessage(), e);
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
            log.info("[Unclaimed Report] Generating Excel report using global framework");

            // Prepare unclaimed records for Excel
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unclaimedRecords = processRecordsForExcel(
                    (List<Map<String, Object>>) reportData.getOrDefault("unclaimedRecords", new ArrayList<>()));

            // Create sheet data list
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createMapListSheetData("Unclaimed Data", unclaimedRecords)
            );

            // Generate Excel using global service
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);

            log.info("[Unclaimed Report] Excel report generated successfully using global framework");
            return excelBytes;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Unclaimed Batch Data Excel report", e);
        }
    }

    /**
     * Get all notices with active TS-UNC suspensions
     *
     * Joins:
     * - ocms_suspended_notice (for TS-UNC suspension records)
     * - ocms_valid_offence_notice (for notice details)
     * - ocms_offence_notice_owner_driver (for offender details)
     *
     * @return List of unclaimed notice records
     */
    public List<Map<String, Object>> getUnclaimedNotices() {
        log.info("[Unclaimed Report] Fetching notices with active TS-UNC suspensions");

        String sql = """
            SELECT
                sn.notice_no,
                von.offence_type,
                von.offence_time,
                von.vehicle_no,
                von.processing_stage,
                onod.id_no,
                onod.name,
                sn.date_of_suspension,
                sn.due_date_of_revival,
                CASE
                    WHEN sn.date_of_revival IS NULL THEN 'Active'
                    ELSE 'Revived'
                END AS suspension_status
            FROM ocmsizmgr.ocms_suspended_notice AS sn
            INNER JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON sn.notice_no = von.notice_no
            LEFT JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod
                ON von.notice_no = onod.notice_no AND onod.offender_indicator = 'Y'
            WHERE sn.suspension_type = 'TS'
                AND sn.reason_of_suspension = 'UNC'
                AND sn.date_of_revival IS NULL
            ORDER BY sn.date_of_suspension DESC
            """;

        try {
            log.info("[Unclaimed Report] Executing query for TS-UNC suspensions");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            log.info("[Unclaimed Report] Found {} notices with active TS-UNC suspensions", results.size());

            // Log sample record for debugging
            if (!results.isEmpty()) {
                Map<String, Object> sampleRecord = results.get(0);
                log.info("[Unclaimed Report] Sample record columns: {}", sampleRecord.keySet());
                log.debug("[Unclaimed Report] Sample record data: {}", sampleRecord);
            }

            return results;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error fetching unclaimed notices: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch unclaimed notices", e);
        }
    }

    /**
     * Process records for Excel generation, mapping database fields to Excel column names
     */
    private List<Map<String, Object>> processRecordsForExcel(List<Map<String, Object>> records) {
        List<Map<String, Object>> processedRecords = new ArrayList<>();

        int serialNumber = 1;
        for (Map<String, Object> record : records) {
            Map<String, Object> processedRecord = new LinkedHashMap<>();

            // S/N
            processedRecord.put("serial_number", serialNumber++);

            // Notice Number
            processedRecord.put("notice_no", record.get("notice_no"));

            // Offence Type
            processedRecord.put("offence_type", record.get("offence_type"));

            // Offence Date & Time
            Object offenceTimeObj = record.get("offence_time");
            if (offenceTimeObj != null) {
                if (offenceTimeObj instanceof LocalDateTime) {
                    LocalDateTime offenceTime = (LocalDateTime) offenceTimeObj;
                    processedRecord.put("offence_date_time", offenceTime.format(DATETIME_FORMATTER));
                } else {
                    processedRecord.put("offence_date_time", offenceTimeObj.toString());
                }
            } else {
                processedRecord.put("offence_date_time", "");
            }

            // Vehicle No
            processedRecord.put("vehicle_no", record.get("vehicle_no"));

            // Offender ID
            processedRecord.put("offender_id", record.get("id_no"));

            // Offender Name
            processedRecord.put("offender_name", record.get("name"));

            // Reminder Letter Date (use date_of_suspension as proxy)
            Object suspensionDateObj = record.get("date_of_suspension");
            if (suspensionDateObj != null) {
                if (suspensionDateObj instanceof LocalDateTime) {
                    LocalDateTime suspensionDate = (LocalDateTime) suspensionDateObj;
                    processedRecord.put("reminder_letter_date", suspensionDate.format(DATE_FORMATTER));

                    // Calculate days since letter sent
                    long daysSince = ChronoUnit.DAYS.between(suspensionDate.toLocalDate(), LocalDate.now());
                    processedRecord.put("days_since_letter", daysSince);
                } else {
                    processedRecord.put("reminder_letter_date", suspensionDateObj.toString());
                    processedRecord.put("days_since_letter", "");
                }
            } else {
                processedRecord.put("reminder_letter_date", "");
                processedRecord.put("days_since_letter", "");
            }

            // Current Processing Stage
            processedRecord.put("processing_stage", record.get("processing_stage"));

            // Suspension Status
            processedRecord.put("suspension_status", record.get("suspension_status"));

            processedRecords.add(processedRecord);
        }

        return processedRecords;
    }

    /**
     * Get unclaimed notices count
     *
     * @return Count of notices with active TS-UNC suspensions
     */
    public int getUnclaimedNoticesCount() {
        log.info("[Unclaimed Report] Getting unclaimed notices count");

        String sql = """
            SELECT COUNT(*) AS total_count
            FROM ocmsizmgr.ocms_suspended_notice
            WHERE suspension_type = 'TS'
                AND reason_of_suspension = 'UNC'
                AND date_of_revival IS NULL
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            log.info("[Unclaimed Report] Unclaimed notices count: {}", count);
            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error getting unclaimed notices count: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get unclaimed notices count", e);
        }
    }

    /**
     * Check if there are unprocessed UNC results from MHA/DataHive
     *
     * @return true if unprocessed results exist
     */
    public boolean hasUnprocessedUncResults() {
        log.info("[Unclaimed Report] Checking for unprocessed UNC results in ocms_temp_unc_hst_addr");

        String sql = """
            SELECT COUNT(*) AS count
            FROM ocmsizmgr.ocms_temp_unc_hst_addr
            WHERE query_reason = 'UNC'
                AND (report_generated IS NULL OR report_generated = 'N')
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            boolean hasResults = count != null && count > 0;
            log.info("[Unclaimed Report] Unprocessed UNC results count: {}", count);
            return hasResults;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error checking for unprocessed results: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fetch UNC batch data from ocms_temp_unc_hst_addr
     * This is the data returned from MHA/DataHive for unclaimed reminder queries
     *
     * @return Map containing UNC address data
     */
    public Map<String, Object> fetchUncBatchDataFromTemp() {
        log.info("[Unclaimed Report] Fetching UNC batch data from ocms_temp_unc_hst_addr");

        String sql = """
            SELECT
                t.id_no,
                t.id_type,
                t.notice_no,
                t.street_name,
                t.blk_hse_no,
                t.floor_no,
                t.unit_no,
                t.bldg_name,
                t.postal_code,
                t.address_type,
                t.invalid_addr_tag,
                t.last_change_address_date,
                t.response_date_time,
                t.query_reason
            FROM ocmsizmgr.ocms_temp_unc_hst_addr AS t
            WHERE t.query_reason = 'UNC'
                AND (t.report_generated IS NULL OR t.report_generated = 'N')
            ORDER BY t.response_date_time DESC
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            log.info("[Unclaimed Report] Found {} UNC records from MHA/DataHive", results.size());

            Map<String, Object> reportData = new HashMap<>();
            reportData.put("uncBatchData", results);
            reportData.put("totalCount", results.size());

            return reportData;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error fetching UNC batch data: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Generate Unclaimed Batch Data Report from MHA/DataHive results
     * This report contains 15 columns of address data from ocms_temp_unc_hst_addr
     *
     * @param reportData Map containing UNC batch data
     * @param reportDate Report date
     * @return Excel file as byte array
     */
    public byte[] generateUncBatchDataReport(Map<String, Object> reportData, String reportDate) {
        try {
            log.info("[Unclaimed Report] Generating UNC Batch Data Report");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawData = (List<Map<String, Object>>)
                reportData.getOrDefault("uncBatchData", new ArrayList<>());

            // Process data for Excel (15 columns as per spec)
            List<Map<String, Object>> processedRecords = new ArrayList<>();
            int serialNumber = 1;

            for (Map<String, Object> record : rawData) {
                Map<String, Object> processedRecord = new LinkedHashMap<>();

                // Column 1: S/N
                processedRecord.put("S/N", serialNumber++);

                // Column 2: Notice Number
                processedRecord.put("Notice Number", record.get("notice_no"));

                // Column 3: Offender ID
                processedRecord.put("Offender ID", record.get("id_no"));

                // Column 4: ID Type
                processedRecord.put("ID Type", record.get("id_type"));

                // Column 5: Block/House No
                processedRecord.put("Block/House No", record.get("blk_hse_no"));

                // Column 6: Street Name
                processedRecord.put("Street Name", record.get("street_name"));

                // Column 7: Floor No
                processedRecord.put("Floor No", record.get("floor_no"));

                // Column 8: Unit No
                processedRecord.put("Unit No", record.get("unit_no"));

                // Column 9: Building Name
                processedRecord.put("Building Name", record.get("bldg_name"));

                // Column 10: Postal Code
                processedRecord.put("Postal Code", record.get("postal_code"));

                // Column 11: Address Type
                processedRecord.put("Address Type", record.get("address_type"));

                // Column 12: Invalid Address Tag
                Object invalidTag = record.get("invalid_addr_tag");
                processedRecord.put("Invalid Address Tag",
                    invalidTag != null && !invalidTag.toString().trim().isEmpty() ? "Yes" : "No");

                // Column 13: Last Change Address Date
                Object lastChangeDate = record.get("last_change_address_date");
                if (lastChangeDate instanceof LocalDate) {
                    processedRecord.put("Last Change Date", ((LocalDate) lastChangeDate).format(DATE_FORMATTER));
                } else if (lastChangeDate instanceof LocalDateTime) {
                    processedRecord.put("Last Change Date", ((LocalDateTime) lastChangeDate).format(DATE_FORMATTER));
                } else {
                    processedRecord.put("Last Change Date", lastChangeDate != null ? lastChangeDate.toString() : "");
                }

                // Column 14: MHA/DataHive Response Date
                Object responseDate = record.get("response_date_time");
                if (responseDate instanceof LocalDateTime) {
                    processedRecord.put("Response Date", ((LocalDateTime) responseDate).format(DATETIME_FORMATTER));
                } else {
                    processedRecord.put("Response Date", responseDate != null ? responseDate.toString() : "");
                }

                // Column 15: Query Reason
                processedRecord.put("Query Reason", record.get("query_reason"));

                processedRecords.add(processedRecord);
            }

            // Create sheet data
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createMapListSheetData("UNC Batch Data", processedRecords)
            );

            // Generate Excel
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);

            log.info("[Unclaimed Report] UNC Batch Data Report generated successfully");
            return excelBytes;

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error generating UNC Batch Data Report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate UNC Batch Data Report", e);
        }
    }

    /**
     * Mark UNC results as processed after report generation
     */
    public void markUncResultsAsProcessed() {
        log.info("[Unclaimed Report] Marking UNC results as processed");

        String sql = """
            UPDATE ocmsizmgr.ocms_temp_unc_hst_addr
            SET report_generated = 'Y',
                upd_date = CURRENT_TIMESTAMP,
                upd_user_id = 'SYSTEM_UNC_REPORT'
            WHERE query_reason = 'UNC'
                AND (report_generated IS NULL OR report_generated = 'N')
            """;

        try {
            int updated = jdbcTemplate.update(sql);
            log.info("[Unclaimed Report] Marked {} UNC results as processed", updated);

        } catch (Exception e) {
            log.error("[Unclaimed Report] Error marking results as processed: {}", e.getMessage(), e);
        }
    }
}
