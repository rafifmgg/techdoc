package com.ocmsintranet.cronservice.framework.workflows.hstreport.helpers;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import com.ocmsintranet.cronservice.framework.common.ExcelReportService;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.config.HstReportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for Monthly HST report generation.
 * This class handles data fetching and Excel report generation using the global Excel helper.
 *
 * Based on OCMS 20 Specification:
 * - Queries ocms_hst table for all HST records
 * - Joins with ocms_suspended_notice to get TS-HST suspension details
 * - Generates Excel report with 10 columns showing HST and suspension information
 *
 * Data Sources:
 * - ocmsizmgr.ocms_hst (HST records)
 * - ocmsizmgr.ocms_suspended_notice (TS-HST suspension records)
 */
@Slf4j
@Component
public class HstReportHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ExcelReportService excelReportService;
    private final HstReportConfig reportConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public HstReportHelper(
            JdbcTemplate jdbcTemplate,
            ExcelReportService excelReportService,
            HstReportConfig reportConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.excelReportService = excelReportService;
        this.reportConfig = reportConfig;
    }

    /**
     * Fetches monthly HST data from the database
     *
     * @return Map containing the fetched data
     */
    public Map<String, Object> fetchMonthlyHstData() {
        log.info("[HST Report] Fetching monthly HST data");

        try {
            // Get all HST records with suspension details
            List<Map<String, Object>> hstRecords = getHstRecordsWithSuspensions();

            if (hstRecords.isEmpty()) {
                log.info("[HST Report] No HST records found");
                return Collections.emptyMap();
            }

            log.info("[HST Report] Found {} HST records", hstRecords.size());

            // Log sample records for debugging
            if (!hstRecords.isEmpty()) {
                Map<String, Object> firstRecord = hstRecords.get(0);
                log.info("[HST Report] Record columns: {}", firstRecord.keySet());
                log.debug("[HST Report] Sample record: {}", firstRecord);
            }

            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("hstRecords", hstRecords);
            result.put("totalCount", hstRecords.size());

            return result;

        } catch (Exception e) {
            log.error("[HST Report] Error fetching monthly HST data: {}", e.getMessage(), e);
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
            log.info("[HST Report] Generating Excel report using global framework");

            // Prepare HST records for Excel
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hstRecords = processRecordsForExcel(
                    (List<Map<String, Object>>) reportData.getOrDefault("hstRecords", new ArrayList<>()));

            // Create sheet data list
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createMapListSheetData("HST Data", hstRecords)
            );

            // Generate Excel using global service
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);

            log.info("[HST Report] Excel report generated successfully using global framework");
            return excelBytes;

        } catch (Exception e) {
            log.error("[HST Report] Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Monthly HST Excel report", e);
        }
    }

    /**
     * Get all HST records with their associated TS-HST suspensions
     *
     * This query:
     * 1. Gets all records from ocms_hst
     * 2. Joins with ocms_suspended_notice to get TS-HST suspension details
     * 3. Groups by HST record to aggregate suspension information
     *
     * @return List of HST records with suspension details
     */
    public List<Map<String, Object>> getHstRecordsWithSuspensions() {
        log.info("[HST Report] Fetching HST records with TS-HST suspensions");

        String sql = """
            SELECT
                h.id_no,
                h.name,
                h.street_name,
                h.blk_hse_no,
                h.floor_no,
                h.unit_no,
                h.bldg_name,
                h.postal_code,
                h.cre_date AS hst_created_date,
                COUNT(sn.notice_no) AS total_suspended_notices,
                MIN(sn.date_of_suspension) AS first_suspension_date,
                MAX(sn.date_of_suspension) AS latest_suspension_date
            FROM ocmsizmgr.ocms_hst AS h
            LEFT JOIN (
                SELECT DISTINCT onod.id_no, sn.notice_no, sn.date_of_suspension
                FROM ocmsizmgr.ocms_suspended_notice AS sn
                INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod
                    ON sn.notice_no = onod.notice_no
                WHERE sn.suspension_type = 'TS'
                    AND sn.reason_of_suspension = 'HST'
                    AND sn.date_of_revival IS NULL
            ) AS sn ON h.id_no = sn.id_no
            GROUP BY h.id_no, h.name, h.street_name, h.blk_hse_no, h.floor_no,
                     h.unit_no, h.bldg_name, h.postal_code, h.cre_date
            ORDER BY h.cre_date DESC
            """;

        try {
            log.info("[HST Report] Executing query for HST records");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            log.info("[HST Report] Found {} HST records", results.size());

            // For each HST record, fetch associated notice numbers
            for (Map<String, Object> record : results) {
                String idNo = (String) record.get("id_no");
                List<String> noticeNumbers = getNoticeNumbersForHst(idNo);
                record.put("notice_numbers_list", noticeNumbers);
            }

            // Log sample record for debugging
            if (!results.isEmpty()) {
                Map<String, Object> sampleRecord = results.get(0);
                log.info("[HST Report] Sample record columns: {}", sampleRecord.keySet());
                log.debug("[HST Report] Sample record data: {}", sampleRecord);
            }

            return results;

        } catch (Exception e) {
            log.error("[HST Report] Error fetching HST records: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch HST records", e);
        }
    }

    /**
     * Get list of notice numbers with active TS-HST suspensions for a given HST ID
     *
     * @param idNo The HST ID number
     * @return List of notice numbers
     */
    private List<String> getNoticeNumbersForHst(String idNo) {
        String sql = """
            SELECT DISTINCT sn.notice_no
            FROM ocmsizmgr.ocms_suspended_notice AS sn
            INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod
                ON sn.notice_no = onod.notice_no
            WHERE onod.id_no = ?
                AND sn.suspension_type = 'TS'
                AND sn.reason_of_suspension = 'HST'
                AND sn.date_of_revival IS NULL
            ORDER BY sn.notice_no
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, idNo);
            return results.stream()
                    .map(row -> (String) row.get("notice_no"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[HST Report] Error fetching notice numbers for HST ID {}: {}", idNo, e.getMessage(), e);
            return Collections.emptyList();
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

            // Offender ID
            processedRecord.put("id_no", record.get("id_no"));

            // Offender Name
            processedRecord.put("name", record.get("name"));

            // Address (concatenate all address fields)
            StringBuilder address = new StringBuilder();
            appendIfNotNull(address, record.get("blk_hse_no"));
            appendIfNotNull(address, record.get("street_name"));
            appendIfNotNull(address, "#" + record.get("floor_no") + "-" + record.get("unit_no"));
            appendIfNotNull(address, record.get("bldg_name"));
            appendIfNotNull(address, "Singapore " + record.get("postal_code"));
            processedRecord.put("address", address.toString().trim());

            // Notice Numbers (comma-separated)
            @SuppressWarnings("unchecked")
            List<String> noticeNumbersList = (List<String>) record.get("notice_numbers_list");
            if (noticeNumbersList != null && !noticeNumbersList.isEmpty()) {
                String noticeNumbers = String.join(", ", noticeNumbersList);
                processedRecord.put("notice_numbers", noticeNumbers);
            } else {
                processedRecord.put("notice_numbers", "");
            }

            // Total Suspended Notices
            Object totalSuspended = record.get("total_suspended_notices");
            processedRecord.put("total_suspended_notices", totalSuspended != null ? totalSuspended : 0);

            // HST Created Date
            Object hstCreatedObj = record.get("hst_created_date");
            if (hstCreatedObj != null) {
                if (hstCreatedObj instanceof LocalDateTime) {
                    LocalDateTime hstCreated = (LocalDateTime) hstCreatedObj;
                    processedRecord.put("hst_created_date", hstCreated.format(DATETIME_FORMATTER));
                } else {
                    processedRecord.put("hst_created_date", hstCreatedObj.toString());
                }
            } else {
                processedRecord.put("hst_created_date", "");
            }

            // First Suspension Date
            Object firstSuspensionObj = record.get("first_suspension_date");
            if (firstSuspensionObj != null) {
                if (firstSuspensionObj instanceof LocalDateTime) {
                    LocalDateTime firstSuspension = (LocalDateTime) firstSuspensionObj;
                    processedRecord.put("first_suspension_date", firstSuspension.format(DATE_FORMATTER));
                } else {
                    processedRecord.put("first_suspension_date", firstSuspensionObj.toString());
                }
            } else {
                processedRecord.put("first_suspension_date", "");
            }

            // Latest Suspension Date
            Object latestSuspensionObj = record.get("latest_suspension_date");
            if (latestSuspensionObj != null) {
                if (latestSuspensionObj instanceof LocalDateTime) {
                    LocalDateTime latestSuspension = (LocalDateTime) latestSuspensionObj;
                    processedRecord.put("latest_suspension_date", latestSuspension.format(DATE_FORMATTER));
                } else {
                    processedRecord.put("latest_suspension_date", latestSuspensionObj.toString());
                }
            } else {
                processedRecord.put("latest_suspension_date", "");
            }

            // Status (Active if has suspensions, Inactive otherwise)
            int totalSuspendedCount = totalSuspended != null ? (int) totalSuspended : 0;
            processedRecord.put("status", totalSuspendedCount > 0 ? "Active" : "Inactive");

            processedRecords.add(processedRecord);
        }

        return processedRecords;
    }

    /**
     * Helper method to append address field if not null
     */
    private void appendIfNotNull(StringBuilder sb, Object value) {
        if (value != null && !value.toString().trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(value.toString().trim());
        }
    }

    /**
     * Get HST records count
     *
     * @return Count of HST records
     */
    public int getHstRecordsCount() {
        log.info("[HST Report] Getting HST records count");

        String sql = "SELECT COUNT(*) AS total_count FROM ocmsizmgr.ocms_hst";

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            log.info("[HST Report] HST records count: {}", count);
            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("[HST Report] Error getting HST records count: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get HST records count", e);
        }
    }

    /**
     * Check if there are unprocessed HST results from MHA/DataHive
     *
     * @return true if unprocessed results exist
     */
    public boolean hasUnprocessedHstResults() {
        log.info("[HST Report] Checking for unprocessed HST results in ocms_temp_unc_hst_addr");

        String sql = """
            SELECT COUNT(*) AS count
            FROM ocmsizmgr.ocms_temp_unc_hst_addr
            WHERE query_reason = 'HST'
                AND (report_generated IS NULL OR report_generated = 'N')
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            boolean hasResults = count != null && count > 0;
            log.info("[HST Report] Unprocessed HST results count: {}", count);
            return hasResults;

        } catch (Exception e) {
            log.error("[HST Report] Error checking for unprocessed results: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fetch HST data with MHA/DataHive address comparison
     * Joins ocms_hst with ocms_temp_unc_hst_addr to compare old vs new addresses
     *
     * @return Map containing HST records with address comparison
     */
    public Map<String, Object> fetchHstDataWithAddressComparison() {
        log.info("[HST Report] Fetching HST data with MHA/DataHive address comparison");

        String sql = """
            SELECT
                h.id_no,
                h.name,
                h.street_name AS old_street_name,
                h.blk_hse_no AS old_blk_hse_no,
                h.floor_no AS old_floor_no,
                h.unit_no AS old_unit_no,
                h.bldg_name AS old_bldg_name,
                h.postal_code AS old_postal_code,
                t.street_name AS new_street_name,
                t.blk_hse_no AS new_blk_hse_no,
                t.floor_no AS new_floor_no,
                t.unit_no AS new_unit_no,
                t.bldg_name AS new_bldg_name,
                t.postal_code AS new_postal_code,
                t.invalid_addr_tag,
                t.response_date_time
            FROM ocmsizmgr.ocms_hst AS h
            INNER JOIN ocmsizmgr.ocms_temp_unc_hst_addr AS t
                ON h.id_no = t.id_no
            WHERE t.query_reason = 'HST'
                AND (t.report_generated IS NULL OR t.report_generated = 'N')
            ORDER BY h.id_no
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            log.info("[HST Report] Found {} HST records with address comparison", results.size());

            Map<String, Object> reportData = new HashMap<>();
            reportData.put("hstAddressComparison", results);
            reportData.put("totalCount", results.size());

            return reportData;

        } catch (Exception e) {
            log.error("[HST Report] Error fetching HST address comparison data: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Generate Excel report with HST address comparison data
     *
     * @param reportData Map containing address comparison data
     * @param reportDate Report date
     * @return Excel file as byte array
     */
    public byte[] generateHstDataComparisonReport(Map<String, Object> reportData, String reportDate) {
        try {
            log.info("[HST Report] Generating HST Data Comparison Report");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawData = (List<Map<String, Object>>)
                reportData.getOrDefault("hstAddressComparison", new ArrayList<>());

            // Process data for Excel
            List<Map<String, Object>> processedRecords = new ArrayList<>();
            int serialNumber = 1;

            for (Map<String, Object> record : rawData) {
                Map<String, Object> processedRecord = new LinkedHashMap<>();

                processedRecord.put("S/N", serialNumber++);
                processedRecord.put("HST ID", record.get("id_no"));
                processedRecord.put("Name", record.get("name"));

                // Old Address
                StringBuilder oldAddress = new StringBuilder();
                appendIfNotNull(oldAddress, record.get("old_blk_hse_no"));
                appendIfNotNull(oldAddress, record.get("old_street_name"));
                appendIfNotNull(oldAddress, "#" + record.get("old_floor_no") + "-" + record.get("old_unit_no"));
                appendIfNotNull(oldAddress, record.get("old_bldg_name"));
                appendIfNotNull(oldAddress, "Singapore " + record.get("old_postal_code"));
                processedRecord.put("Old Address (HST)", oldAddress.toString().trim());

                // New Address
                StringBuilder newAddress = new StringBuilder();
                appendIfNotNull(newAddress, record.get("new_blk_hse_no"));
                appendIfNotNull(newAddress, record.get("new_street_name"));
                appendIfNotNull(newAddress, "#" + record.get("new_floor_no") + "-" + record.get("new_unit_no"));
                appendIfNotNull(newAddress, record.get("new_bldg_name"));
                appendIfNotNull(newAddress, "Singapore " + record.get("new_postal_code"));
                processedRecord.put("New Address (MHA/DataHive)", newAddress.toString().trim());

                // Address Changed?
                boolean addressChanged = !oldAddress.toString().equals(newAddress.toString());
                processedRecord.put("Address Changed", addressChanged ? "Yes" : "No");

                // Invalid Address Tag
                Object invalidTag = record.get("invalid_addr_tag");
                processedRecord.put("Invalid Address", invalidTag != null && !invalidTag.toString().trim().isEmpty() ? "Yes" : "No");

                // Response Date
                Object responseDate = record.get("response_date_time");
                if (responseDate instanceof LocalDateTime) {
                    processedRecord.put("MHA/DataHive Response Date", ((LocalDateTime) responseDate).format(DATETIME_FORMATTER));
                } else {
                    processedRecord.put("MHA/DataHive Response Date", responseDate != null ? responseDate.toString() : "");
                }

                processedRecords.add(processedRecord);
            }

            // Create sheet data
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createMapListSheetData("HST Address Comparison", processedRecords)
            );

            // Generate Excel
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);

            log.info("[HST Report] HST Data Comparison Report generated successfully");
            return excelBytes;

        } catch (Exception e) {
            log.error("[HST Report] Error generating HST Data Comparison Report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate HST Data Comparison Report", e);
        }
    }

    /**
     * Calculate HST Work Items Statistics (11 data points)
     *
     * @return Map containing statistics
     */
    public Map<String, Object> calculateHstWorkItemsStatistics() {
        log.info("[HST Report] Calculating HST Work Items statistics");

        try {
            Map<String, Object> statistics = new LinkedHashMap<>();

            // 1. Total HST IDs
            int totalHstIds = getHstRecordsCount();
            statistics.put("Total HST IDs", totalHstIds);

            // 2. HST IDs checked this month
            int checkedThisMonth = getHstIdsCheckedThisMonth();
            statistics.put("HST IDs Checked This Month", checkedThisMonth);

            // 3. Addresses that changed
            int addressesChanged = getAddressesChanged();
            statistics.put("Addresses Changed", addressesChanged);

            // 4. Addresses marked invalid
            int addressesInvalid = getAddressesMarkedInvalid();
            statistics.put("Addresses Marked Invalid", addressesInvalid);

            // 5. Addresses now valid
            int addressesValid = checkedThisMonth - addressesInvalid;
            statistics.put("Addresses Now Valid", addressesValid);

            // 6. Total active TS-HST suspensions
            int activeSuspensions = getActiveTsHstSuspensions();
            statistics.put("Total Active TS-HST Suspensions", activeSuspensions);

            // 7. New TS-HST suspensions this month
            int newSuspensions = getNewTsHstSuspensionsThisMonth();
            statistics.put("New TS-HST Suspensions This Month", newSuspensions);

            // 8. TS-HST suspensions reapplied (looping)
            int reappliedSuspensions = getTsHstReappliedThisMonth();
            statistics.put("TS-HST Reapplied (Looping)", reappliedSuspensions);

            // 9. Notices under TS-HST
            int noticesUnderTsHst = getNoticesUnderTsHst();
            statistics.put("Total Notices Under TS-HST", noticesUnderTsHst);

            // 10. HST IDs with valid addresses (eligible for revival)
            statistics.put("HST IDs Eligible for Revival", addressesValid);

            // 11. HST IDs still suspended (invalid addresses)
            statistics.put("HST IDs Still Suspended", addressesInvalid);

            log.info("[HST Report] Calculated statistics: {}", statistics);
            return statistics;

        } catch (Exception e) {
            log.error("[HST Report] Error calculating statistics: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Generate Monthly HST Work Items Report (statistics)
     *
     * @param statistics Map of statistics
     * @param reportDate Report date
     * @return Excel file as byte array
     */
    public byte[] generateHstWorkItemsReport(Map<String, Object> statistics, String reportDate) {
        try {
            log.info("[HST Report] Generating HST Work Items Report");

            // Convert statistics map to list of rows for Excel
            List<Map<String, Object>> rows = new ArrayList<>();

            for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Statistic", entry.getKey());
                row.put("Value", entry.getValue());
                rows.add(row);
            }

            // Create sheet data
            List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
                ExcelReportService.createMapListSheetData("Work Items Statistics", rows)
            );

            // Generate Excel
            byte[] excelBytes = excelReportService.generateReport(reportConfig, sheetDataList, reportDate);

            log.info("[HST Report] HST Work Items Report generated successfully");
            return excelBytes;

        } catch (Exception e) {
            log.error("[HST Report] Error generating HST Work Items Report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate HST Work Items Report", e);
        }
    }

    /**
     * Mark HST results as processed after report generation
     */
    public void markHstResultsAsProcessed() {
        log.info("[HST Report] Marking HST results as processed");

        String sql = """
            UPDATE ocmsizmgr.ocms_temp_unc_hst_addr
            SET report_generated = 'Y',
                upd_date = CURRENT_TIMESTAMP,
                upd_user_id = 'SYSTEM_HST_REPORT'
            WHERE query_reason = 'HST'
                AND (report_generated IS NULL OR report_generated = 'N')
            """;

        try {
            int updated = jdbcTemplate.update(sql);
            log.info("[HST Report] Marked {} HST results as processed", updated);

        } catch (Exception e) {
            log.error("[HST Report] Error marking results as processed: {}", e.getMessage(), e);
        }
    }

    // Helper methods for statistics calculations

    private int getHstIdsCheckedThisMonth() {
        String sql = """
            SELECT COUNT(DISTINCT id_no)
            FROM ocmsizmgr.ocms_temp_unc_hst_addr
            WHERE query_reason = 'HST'
                AND MONTH(response_date_time) = MONTH(CURRENT_DATE)
                AND YEAR(response_date_time) = YEAR(CURRENT_DATE)
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting checked this month count", e);
            return 0;
        }
    }

    private int getAddressesChanged() {
        String sql = """
            SELECT COUNT(*)
            FROM ocmsizmgr.ocms_hst h
            INNER JOIN ocmsizmgr.ocms_temp_unc_hst_addr t ON h.id_no = t.id_no
            WHERE t.query_reason = 'HST'
                AND (h.street_name != t.street_name OR h.postal_code != t.postal_code)
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting addresses changed count", e);
            return 0;
        }
    }

    private int getAddressesMarkedInvalid() {
        String sql = """
            SELECT COUNT(*)
            FROM ocmsizmgr.ocms_temp_unc_hst_addr
            WHERE query_reason = 'HST'
                AND (invalid_addr_tag IS NOT NULL AND invalid_addr_tag != '')
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting invalid addresses count", e);
            return 0;
        }
    }

    private int getActiveTsHstSuspensions() {
        String sql = """
            SELECT COUNT(DISTINCT notice_no)
            FROM ocmsizmgr.ocms_suspended_notice
            WHERE suspension_type = 'TS'
                AND reason_of_suspension = 'HST'
                AND date_of_revival IS NULL
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting active suspensions count", e);
            return 0;
        }
    }

    private int getNewTsHstSuspensionsThisMonth() {
        String sql = """
            SELECT COUNT(DISTINCT notice_no)
            FROM ocmsizmgr.ocms_suspended_notice
            WHERE suspension_type = 'TS'
                AND reason_of_suspension = 'HST'
                AND MONTH(date_of_suspension) = MONTH(CURRENT_DATE)
                AND YEAR(date_of_suspension) = YEAR(CURRENT_DATE)
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting new suspensions count", e);
            return 0;
        }
    }

    private int getTsHstReappliedThisMonth() {
        String sql = """
            SELECT COUNT(*)
            FROM ocmsizmgr.ocms_suspended_notice
            WHERE suspension_type = 'TS'
                AND reason_of_suspension = 'HST'
                AND remarks LIKE '%Re-applied TS-HST (looping)%'
                AND MONTH(date_of_suspension) = MONTH(CURRENT_DATE)
                AND YEAR(date_of_suspension) = YEAR(CURRENT_DATE)
            """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("[HST Report] Error getting reapplied suspensions count", e);
            return 0;
        }
    }

    private int getNoticesUnderTsHst() {
        return getActiveTsHstSuspensions();
    }
}
