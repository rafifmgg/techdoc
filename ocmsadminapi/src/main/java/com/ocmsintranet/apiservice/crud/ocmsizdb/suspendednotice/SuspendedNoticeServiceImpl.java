package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SuspendedNoticeServiceImpl
    extends BaseImplement<SuspendedNotice, SuspendedNoticeId, SuspendedNoticeRepository>
    implements SuspendedNoticeService {

    @PersistenceContext
    private EntityManager entityManager;

    public SuspendedNoticeServiceImpl(SuspendedNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public Map<String, Integer> getMhaControlMetrics(String startDate, String endDate) {
        Map<String, Integer> metrics = new HashMap<>();

        // Initialize keys with default 0
        metrics.put("recordsSubmitted", 0);
        metrics.put("recordsReturned", 0);
        metrics.put("recordsRead", 0);
        metrics.put("recordsMatched", 0);
        metrics.put("invalidUinFinCount", 0);
        metrics.put("validUnmatchedCount", 0);

        try {
            String sql = "SELECT log_text FROM ocmsizmgr.ocms_batch_job "
                    + "WHERE name = 'process_mha_vrls_files' "
                    + "AND CONVERT(date, start_run) BETWEEN :startDate AND :endDate "
                    + "ORDER BY start_run";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);

            @SuppressWarnings("unchecked")
            List<String> logTexts = query.getResultList();

            for (String logText : logTexts) {
                if (logText != null && !logText.isEmpty()) {
                    extractMetricsFromText(logText, metrics);
                }
            }

        } catch (Exception e) {
            log.warn("Error fetching MHA control metrics: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * Parse the Metrics: key: value pairs from the log text and accumulate into metrics map.
     */
    private void extractMetricsFromText(String logText, Map<String, Integer> metrics) {
        int idx = logText.indexOf("Metrics:");
        if (idx < 0) {
            return;
        }

        String metricsStr = logText.substring(idx + "Metrics:".length()).trim();
        String[] pairs = metricsStr.split(",\\s*");
        for (String pair : pairs) {
            String[] kv = pair.split(":\\s*");
            if (kv.length == 2) {
                String key = kv[0].trim();
                try {
                    int value = Integer.parseInt(kv[1].trim());
                    metrics.merge(key, value, (oldVal, newVal) -> oldVal + newVal);
                } catch (NumberFormatException e) {
                    log.debug("Failed to parse metric value for {} from '{}'", kv[0], kv[1]);
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> getRecordsForReport(String fromDate, String toDate, Map<String, Integer> orderByFields) {
        // Valid sortable fields mapping - all 19 fields from query result
        Map<String, String> fieldMapping = new HashMap<>();

        // Row 0: notice_no
        fieldMapping.put("notice_no", "von.notice_no");
        
        // Row 2-3: Processing stages
        fieldMapping.put("last_processing_stage", "von.last_processing_stage");
        fieldMapping.put("next_processing_stage", "von.next_processing_stage");

        // Row 4: next_processing_date
        fieldMapping.put("next_processing_date", "von.next_processing_date");

        // Row 5: processing_date_time
        fieldMapping.put("processing_date_time", "addr.processing_date_time");

        // Row 6: notice_date_and_time
        fieldMapping.put("notice_date_and_time", "von.notice_date_and_time");

        // Row 7-9: Identity fields
        fieldMapping.put("id_type", "nod.id_type");
        fieldMapping.put("id_no", "nod.id_no");
        fieldMapping.put("name", "nod.name");
        fieldMapping.put("date_of_birth", "nod.date_of_birth");
        fieldMapping.put("date_of_death", "nod.date_of_death");

        // Row 10-15: Address fields
        fieldMapping.put("bldg_name", "addr.bldg_name");
        fieldMapping.put("blk_hse_no", "addr.blk_hse_no");
        fieldMapping.put("street_name", "addr.street_name");
        fieldMapping.put("postal_code", "addr.postal_code");
        fieldMapping.put("floor_no", "addr.floor_no");
        fieldMapping.put("unit_no", "addr.unit_no");
        fieldMapping.put("effective_date", "addr.effective_date");
        fieldMapping.put("invalid_addr_tag", "addr.invalid_addr_tag");

        // Row 16-18: Suspension fields
        fieldMapping.put("suspension_type", "von.suspension_type");
        fieldMapping.put("due_date_of_revival", "von.due_date_of_revival");
        fieldMapping.put("epr_reason_of_suspension", "von.epr_reason_of_suspension");

        // Define alphanumeric fields that need natural sorting (e.g., 1, 1A, 2, 2B, 10, 20)
        Set<String> alphanumericFields = new HashSet<>();
        alphanumericFields.add("blk_hse_no");
        alphanumericFields.add("postal_code");
        alphanumericFields.add("unit_no");
        alphanumericFields.add("floor_no");

        // Build ORDER BY clause
        StringBuilder orderByClause = new StringBuilder();
        if (orderByFields != null && !orderByFields.isEmpty()) {
            List<String> orderParts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : orderByFields.entrySet()) {
                String fieldName = entry.getKey();
                Integer direction = entry.getValue();

                // Convert camelCase to snake_case for validation
                String snakeCaseFieldName = camelToSnakeCase(fieldName);

                // Validate field name (try both camelCase and snake_case)
                String dbField = null;
                if (fieldMapping.containsKey(fieldName)) {
                    dbField = fieldMapping.get(fieldName);
                    log.debug("Found field mapping for: {} -> {}", fieldName, dbField);
                } else if (fieldMapping.containsKey(snakeCaseFieldName)) {
                    dbField = fieldMapping.get(snakeCaseFieldName);
                    log.debug("Found field mapping after conversion: {} -> {} -> {}", fieldName, snakeCaseFieldName, dbField);
                } else {
                    log.warn("Invalid sort field: {} (converted to: {}). Skipping.", fieldName, snakeCaseFieldName);
                }

                if (dbField != null) {
                    String sortDirection = (direction != null && direction < 0) ? "DESC" : "ASC";

                    // Check if this is an alphanumeric field that needs natural sorting
                    if (alphanumericFields.contains(snakeCaseFieldName)) {
                        // Use the computed _sort column from SELECT list for natural sorting
                        // Sort by: 1) numeric part (_sort column), 2) full value for alpha suffix
                        orderParts.add(snakeCaseFieldName + "_sort " + sortDirection + ", " + dbField + " " + sortDirection);
                        log.debug("Using natural sort for alphanumeric field: {} via {}_sort column", dbField, snakeCaseFieldName);
                    } else {
                        orderParts.add(dbField + " " + sortDirection);
                    }
                }
            }

            if (!orderParts.isEmpty()) {
                orderByClause.append(" ORDER BY ").append(String.join(", ", orderParts));
            } else {
                log.warn("No valid sort fields found. Using default: von.notice_no");
                orderByClause.append(" ORDER BY von.notice_no");
            }
        } else {
            orderByClause.append(" ORDER BY von.notice_no");
        }

        // Build the full query
        String sql = """
            SELECT
                von.notice_no,
                'SUCCESS' AS data_type,
                von.last_processing_stage,
                von.next_processing_stage,
                von.next_processing_date,
                von.notice_date_and_time,
                addr.processing_date_time,
                addr.effective_date,
                addr.invalid_addr_tag,
                nod.id_type,
                nod.id_no,
                nod.name,
                nod.date_of_birth,
                nod.date_of_death,
                addr.bldg_name,
                addr.blk_hse_no,
                addr.street_name,
                addr.postal_code,
                addr.floor_no,
                addr.unit_no,
                NULL AS suspension_type,
                NULL AS due_date_of_revival,
                NULL AS epr_reason_of_suspension,
                CAST(SUBSTRING(addr.blk_hse_no, PATINDEX('%[0-9]%', addr.blk_hse_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.blk_hse_no, PATINDEX('%[0-9]%', addr.blk_hse_no), LEN(addr.blk_hse_no)) + 'X') - 1) AS INT) AS blk_hse_no_sort,
                CAST(SUBSTRING(addr.postal_code, PATINDEX('%[0-9]%', addr.postal_code), PATINDEX('%[^0-9]%', SUBSTRING(addr.postal_code, PATINDEX('%[0-9]%', addr.postal_code), LEN(addr.postal_code)) + 'X') - 1) AS INT) AS postal_code_sort,
                CAST(SUBSTRING(addr.floor_no, PATINDEX('%[0-9]%', addr.floor_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.floor_no, PATINDEX('%[0-9]%', addr.floor_no), LEN(addr.floor_no)) + 'X') - 1) AS INT) AS floor_no_sort,
                CAST(SUBSTRING(addr.unit_no, PATINDEX('%[0-9]%', addr.unit_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.unit_no, PATINDEX('%[0-9]%', addr.unit_no), LEN(addr.unit_no)) + 'X') - 1) AS INT) AS unit_no_sort
            FROM ocmsizmgr.ocms_offence_notice_owner_driver_addr AS addr
            JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS nod
                ON nod.notice_no = addr.notice_no
            AND nod.owner_driver_indicator = addr.owner_driver_indicator
            JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON von.notice_no = nod.notice_no
            WHERE addr.type_of_address = 'mha_reg'
            AND CONVERT(date, addr.processing_date_time) BETWEEN :fromDate AND :toDate
            AND nod.id_no IS NOT NULL
            AND addr.address_type IS NOT NULL
            AND von.next_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3')
            AND von.suspension_type IS NULL

            UNION ALL

            SELECT
                von.notice_no,
                'ERROR' AS data_type,
                von.last_processing_stage,
                von.next_processing_stage,
                von.next_processing_date,
                von.notice_date_and_time,
                addr.processing_date_time,
                addr.effective_date,
                addr.invalid_addr_tag,
                COALESCE(nod.id_type, '') AS id_type,
                COALESCE(nod.id_no, '')   AS id_no,
                COALESCE(nod.name, '')    AS name,
                nod.date_of_birth,
                nod.date_of_death,
                addr.bldg_name,
                addr.blk_hse_no,
                addr.street_name,
                addr.postal_code,
                addr.floor_no,
                addr.unit_no,
                von.suspension_type,
                von.due_date_of_revival,
                von.epr_reason_of_suspension,
                CAST(SUBSTRING(addr.blk_hse_no, PATINDEX('%[0-9]%', addr.blk_hse_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.blk_hse_no, PATINDEX('%[0-9]%', addr.blk_hse_no), LEN(addr.blk_hse_no)) + 'X') - 1) AS INT) AS blk_hse_no_sort,
                CAST(SUBSTRING(addr.postal_code, PATINDEX('%[0-9]%', addr.postal_code), PATINDEX('%[^0-9]%', SUBSTRING(addr.postal_code, PATINDEX('%[0-9]%', addr.postal_code), LEN(addr.postal_code)) + 'X') - 1) AS INT) AS postal_code_sort,
                CAST(SUBSTRING(addr.floor_no, PATINDEX('%[0-9]%', addr.floor_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.floor_no, PATINDEX('%[0-9]%', addr.floor_no), LEN(addr.floor_no)) + 'X') - 1) AS INT) AS floor_no_sort,
                CAST(SUBSTRING(addr.unit_no, PATINDEX('%[0-9]%', addr.unit_no), PATINDEX('%[^0-9]%', SUBSTRING(addr.unit_no, PATINDEX('%[0-9]%', addr.unit_no), LEN(addr.unit_no)) + 'X') - 1) AS INT) AS unit_no_sort
            FROM ocmsizmgr.ocms_offence_notice_owner_driver_addr AS addr
            JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS nod
                ON nod.notice_no = addr.notice_no
            AND nod.owner_driver_indicator = addr.owner_driver_indicator
            JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON von.notice_no = nod.notice_no
            WHERE addr.type_of_address = 'mha_reg'
            AND nod.id_no IS NOT NULL
            AND addr.address_type IS NOT NULL
            AND CONVERT(date, addr.processing_date_time) BETWEEN :fromDate AND :toDate
            AND (
                    (von.suspension_type = 'TS' AND von.epr_reason_of_suspension = 'NRO')
                OR (von.suspension_type = 'PS' AND von.epr_reason_of_suspension IN ('RIP', 'RP2'))
                )

            """ + orderByClause.toString();

        // Log the final query with parameters
        log.info("=== Executing Suspended Notice Query ===");
        log.info("From Date: {}", fromDate);
        log.info("To Date: {}", toDate);
        log.info("Order By Fields: {}", orderByFields);
        log.info("Generated ORDER BY Clause: {}", orderByClause.toString());
        log.info("Full SQL Query:\n{}", sql);
        log.info("==========================================");

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        log.info("Query executed successfully. Retrieved {} records", results.size());

        // Convert results to List<Map<String, Object>>
        // Using camelCase keys to match ResponseDto field names
        // NOTE: Query now returns 27 columns (23 data + 4 sort columns), but we only map the first 23
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            // Row 0: von.notice_no
            map.put("noticeNo", row[0]);
            // Row 1: 'SUCCESS' AS data_type
            map.put("dataType", row[1]);
            // Row 2: von.last_processing_stage
            map.put("lastProcessingStage", row[2]);
            // Row 3: von.next_processing_stage
            map.put("nextProcessingStage", row[3]);
            // Row 4: von.next_processing_date
            map.put("nextProcessingDate", row[4]);
            // Row 5: von.notice_date_and_time
            map.put("noticeDateAndTime", row[5]);
            // Row 6: addr.processing_date_time
            map.put("processingDateTime", row[6]);
            // Row 7: addr.effective_date
            map.put("effectiveDate", row[7]);
            // Row 8: addr.invalid_addr_tag
            map.put("invalidAddrTag", row[8]);
            // Row 9: nod.id_type
            map.put("idType", row[9]);
            // Row 10: nod.id_no
            map.put("idNo", row[10]);
            // Row 11: nod.name
            map.put("name", row[11]);
            // Row 12: nod.date_of_birth
            map.put("dateOfBirth", row[12]);
            // Row 13: nod.date_of_death
            map.put("dateOfDeath", row[13]);
            // Row 14: addr.bldg_name
            map.put("bldgName", row[14]);
            // Row 15: addr.blk_hse_no
            map.put("blkHseNo", row[15]);
            // Row 16: addr.street_name
            map.put("streetName", row[16]);
            // Row 17: addr.postal_code
            map.put("postalCode", row[17]);
            // Row 18: addr.floor_no
            map.put("floorNo", row[18]);
            // Row 19: addr.unit_no
            map.put("unitNo", row[19]);
            // Row 20: NULL AS suspension_type
            map.put("suspensionType", row[20]);
            // Row 21: NULL AS due_date_of_revival
            map.put("dueDateOfRevival", row[21]);
            // Row 22: NULL AS epr_reason_of_suspension
            map.put("eprReasonOfSuspension", row[22]);
            // Row 23-26: Sort columns (blk_hse_no_sort, postal_code_sort, floor_no_sort, unit_no_sort)
            // These are not mapped as they're only used for ORDER BY
            mappedResults.add(map);
        }

        return mappedResults;
    }

    /**
     * Convert camelCase to snake_case
     * Example: idNo -> id_no, noticeNo -> notice_no, processingDateTime -> processing_date_time
     */
    private String camelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));

        for (int i = 1; i < camelCase.length(); i++) {
            char currentChar = camelCase.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                result.append('_');
                result.append(Character.toLowerCase(currentChar));
            } else {
                result.append(currentChar);
            }
        }

        return result.toString();
    }

    @Override
    public Integer getMaxSrNoForNotice(String noticeNo) {
        return this.repository.findMaxSrNoByNoticeNo(noticeNo);
    }

    @Override
    public List<Map<String, Object>> getPsReportRecords(String fromDate, String toDate, String suspensionSource,
                                                         String officerAuthorisingSupension, String creUserId,
                                                         Map<String, Integer> orderByFields) {
        log.info("Fetching PS Report records from {} to {}", fromDate, toDate);
        log.info("Filters - suspensionSource: {}, officer: {}, creUserId: {}",
                 suspensionSource, officerAuthorisingSupension, creUserId);

        // Build WHERE clause for filters
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" WHERE sn.suspension_type = 'PS'");
        whereClause.append(" AND CONVERT(date, sn.date_of_suspension) BETWEEN :fromDate AND :toDate");

        // Add suspension source filter if provided
        if (suspensionSource != null && !suspensionSource.isEmpty() && !"ALL".equalsIgnoreCase(suspensionSource)) {
            whereClause.append(" AND sn.suspension_source = :suspensionSource");
        }

        // Add officer filter if provided (for PS by Officer report)
        if (officerAuthorisingSupension != null && !officerAuthorisingSupension.isEmpty()) {
            whereClause.append(" AND sn.officer_authorising_supension = :officer");
        }

        // Add PSing officer filter if provided (for PS by Officer report)
        if (creUserId != null && !creUserId.isEmpty()) {
            whereClause.append(" AND sn.cre_user_id = :creUserId");
        }

        // Build ORDER BY clause
        StringBuilder orderByClause = new StringBuilder();
        if (orderByFields != null && !orderByFields.isEmpty()) {
            List<String> orderParts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : orderByFields.entrySet()) {
                String fieldName = entry.getKey();
                Integer direction = entry.getValue();
                String sortDirection = (direction != null && direction < 0) ? "DESC" : "ASC";

                // Map field names to database columns
                String dbField = mapPsReportField(fieldName);
                if (dbField != null) {
                    orderParts.add(dbField + " " + sortDirection);
                }
            }
            if (!orderParts.isEmpty()) {
                orderByClause.append(" ORDER BY ").append(String.join(", ", orderParts));
            } else {
                orderByClause.append(" ORDER BY sn.date_of_suspension ASC");
            }
        } else {
            orderByClause.append(" ORDER BY sn.date_of_suspension ASC"); // Default sort
        }

        // Build the full query based on Functional Doc Section 6.3
        String sql = """
            SELECT
                von.vehicle_no,
                sn.notice_no,
                von.offence_notice_type,
                von.vehicle_registration_type,
                von.vehicle_category,
                von.computer_rule_code,
                von.notice_date_and_time,
                von.pp_code,
                sn.suspension_source,
                sn.date_of_suspension,
                sn.epr_reason_of_suspension AS reason_of_suspension,
                sn.suspension_remarks,
                sn.officer_authorising_supension,
                sn.cre_user_id,
                rf.refund_identified_date,
                prev_sn.epr_reason_of_suspension AS previous_ps_reason,
                prev_sn.date_of_suspension AS previous_ps_date
            FROM ocmsizmgr.ocms_suspended_notice AS sn
            JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON von.notice_no = sn.notice_no
            LEFT JOIN (
                SELECT
                    sn2.notice_no,
                    sn2.epr_reason_of_suspension,
                    sn2.date_of_suspension,
                    ROW_NUMBER() OVER (PARTITION BY sn2.notice_no ORDER BY sn2.sr_no DESC) AS rn
                FROM ocmsizmgr.ocms_suspended_notice AS sn2
                WHERE sn2.suspension_type = 'PS'
                AND sn2.date_of_revival IS NULL
            ) AS prev_sn
                ON prev_sn.notice_no = sn.notice_no
                AND prev_sn.rn = 2
            LEFT JOIN ocmsizmgr.ocms_refund_notice AS rf
                ON rf.notice_no = sn.notice_no
            """ + whereClause.toString() + orderByClause.toString();

        log.info("Executing PS Report query:\n{}", sql);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        // Set optional parameters if filters are applied
        if (suspensionSource != null && !suspensionSource.isEmpty() && !"ALL".equalsIgnoreCase(suspensionSource)) {
            query.setParameter("suspensionSource", suspensionSource);
        }
        if (officerAuthorisingSupension != null && !officerAuthorisingSupension.isEmpty()) {
            query.setParameter("officer", officerAuthorisingSupension);
        }
        if (creUserId != null && !creUserId.isEmpty()) {
            query.setParameter("creUserId", creUserId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        log.info("Retrieved {} PS report records", results.size());

        // Convert results to List<Map<String, Object>>
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("vehicleNo", row[0]);
            map.put("noticeNo", row[1]);
            map.put("offenceNoticeType", row[2]);
            map.put("vehicleRegistrationType", row[3]);
            map.put("vehicleCategory", row[4]);
            map.put("computerRuleCode", row[5]);
            map.put("noticeDateAndTime", row[6]);
            map.put("ppCode", row[7]);
            map.put("suspensionSource", row[8]);
            map.put("dateOfSuspension", row[9]);
            map.put("reasonOfSuspension", row[10]);
            map.put("suspensionRemarks", row[11]);
            map.put("officerAuthorisingSupension", row[12]);
            map.put("creUserId", row[13]);
            map.put("refundIdentifiedDate", row[14]);
            map.put("previousPsReason", row[15]);
            map.put("previousPsDate", row[16]);
            mappedResults.add(map);
        }

        return mappedResults;
    }

    /**
     * Map PS Report field names to database columns
     */
    private String mapPsReportField(String fieldName) {
        Map<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("vehicleNo", "von.vehicle_no");
        fieldMapping.put("noticeNo", "sn.notice_no");
        fieldMapping.put("dateOfSuspension", "sn.date_of_suspension");
        fieldMapping.put("suspensionSource", "sn.suspension_source");
        fieldMapping.put("reasonOfSuspension", "sn.epr_reason_of_suspension");
        fieldMapping.put("officerAuthorisingSupension", "sn.officer_authorising_supension");
        fieldMapping.put("creUserId", "sn.cre_user_id");

        return fieldMapping.get(fieldName);
    }

    @Override
    public List<SuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType) {
        return this.repository.findByNoticeNoAndSuspensionType(noticeNo, suspensionType);
    }

}
