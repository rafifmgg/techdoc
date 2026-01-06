package com.ocmsintranet.cronservice.testing.notification_sms_email.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for notification test data operations.
 * Handles database operations for the notification SMS/Email test flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTestDataHelper {

    private final JdbcTemplate jdbcTemplate;

    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_PATTERN = "TEST%";
    private static final String MHATEST_PATTERN = "MHATEST%";

    /**
     * Clean up test data from previous test runs.
     * Deletes records from multiple tables where notice_no matches test patterns.
     *
     * @return Total number of records deleted
     */
    public int cleanupTestData() {
        log.info("Cleaning up test data from previous test runs");
        int totalDeleted = 0;

        // Tables to clean up
        String[] tables = {
                "ocms_suspended_notice",
                "ocms_sms_notification_records",
                "ocms_email_notification_records",
                "ocms_hst",
                "ocms_offence_notice_owner_driver",
                "ocms_valid_offence_notice"
        };

        // Delete from each table where notice_no like 'TEST%' or 'MHATEST%'
        for (String table : tables) {
            String sql = String.format("DELETE FROM %s.%s WHERE notice_no LIKE ? OR notice_no LIKE ?",
                    SCHEMA, table);
            int deleted = jdbcTemplate.update(sql, TEST_PATTERN, MHATEST_PATTERN);
            log.info("Deleted {} records from {}", deleted, table);
            totalDeleted += deleted;
        }

        // Delete from exclusion list
        String exclusionSql = String.format("DELETE FROM %s.ocms_enotification_exclusion_list WHERE id_no = ?",
                SCHEMA);
        int deleted = jdbcTemplate.update(exclusionSql, "S9999999Z");
        log.info("Deleted {} records from ocms_enotification_exclusion_list", deleted);
        totalDeleted += deleted;

        return totalDeleted;
    }

    /**
     * Insert test data for all test cases.
     * Inserts records into VON, owner/driver, HST, and exclusion list tables.
     *
     * @param userId User ID for audit fields
     * @return Map with counts of inserted records by table
     */
    public Map<String, Integer> insertTestData(String userId) {
        log.info("Inserting test data");
        Map<String, Integer> insertCounts = new HashMap<>();

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // Insert VON records
        insertCounts.put("von", insertVonRecords(userId, now));

        // Insert owner/driver records
        insertCounts.put("ownerDriver", insertOwnerDriverRecords(userId, now));

        // Insert HST records
        insertCounts.put("hst", insertHstRecords(userId, now));

        // Insert exclusion list record
        insertCounts.put("exclusionList", insertExclusionListRecord(userId, now));

        return insertCounts;
    }

    /**
     * Insert test VON records.
     *
     * @param userId User ID for audit fields
     * @param timestamp Timestamp for audit fields
     * @return Number of records inserted
     */
    private int insertVonRecords(String userId, Timestamp timestamp) {
        log.info("Inserting VON test records");

        List<Object[]> vonRecords = new ArrayList<>();

        // TEST001: SMS/Email sent, NO HST
        vonRecords.add(new Object[]{
                "TEST001", "SBA1234A", "PP001", "PARKING OFFENCE", "ENA", "RD1", null, null,
                userId, timestamp, userId, timestamp
        });

        // TEST002: SMS/Email sent, already has TS-HST
        vonRecords.add(new Object[]{
                "TEST002", "SBA1235B", "PP001", "PARKING OFFENCE", "ENA", "RD1", "TS", "HST",
                userId, timestamp, userId, timestamp
        });

        // TEST003: SMS/Email sent, HST exists → TS-HST applied
        vonRecords.add(new Object[]{
                "TEST003", "SBA1236C", "PP001", "PARKING OFFENCE", "ENA", "RD1", null, null,
                userId, timestamp, userId, timestamp
        });

        // TEST004: SMS/Email sent, HST exists → TS-HST applied
        vonRecords.add(new Object[]{
                "TEST004", "SBA1237D", "PP001", "PARKING OFFENCE", "ENA", "RD1", null, null,
                userId, timestamp, userId, timestamp
        });

        // TEST005: Wrong suspension type (PS/FOR) - not processed
        vonRecords.add(new Object[]{
                "TEST005", "SBA1238E", "PP001", "PARKING OFFENCE", "ENA", "RD1", "PS", "FOR",
                userId, timestamp, userId, timestamp
        });

        // TEST006: Wrong processing stage (NPA) - not processed
        vonRecords.add(new Object[]{
                "TEST006", "SBA1239F", "PP001", "PARKING OFFENCE", "NPA", "ROV", null, null,
                userId, timestamp, userId, timestamp
        });

        String sql = String.format(
                "INSERT INTO %s.ocms_valid_offence_notice " +
                "(notice_no, vehicle_no, pp_code, pp_name, last_processing_stage, next_processing_stage, " +
                "suspension_type, epr_reason_of_suspension, cre_user_id, cre_date, upd_user_id, upd_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                SCHEMA);

        int[] inserted = jdbcTemplate.batchUpdate(sql, vonRecords);
        return inserted.length;
    }

    /**
     * Insert test owner/driver records.
     *
     * @param userId User ID for audit fields
     * @param timestamp Timestamp for audit fields
     * @return Number of records inserted
     */
    private int insertOwnerDriverRecords(String userId, Timestamp timestamp) {
        log.info("Inserting owner/driver test records");

        List<Object[]> ownerDriverRecords = new ArrayList<>();

        // TEST001 owner
        ownerDriverRecords.add(new Object[]{
                "TEST001", "JOHN DOE", "S1234567A", "john@example.com", "91234567", "Y",
                userId, timestamp, userId, timestamp
        });

        // TEST002 owner
        ownerDriverRecords.add(new Object[]{
                "TEST002", "JANE SMITH", "S2345678B", "jane@example.com", "92345678", "Y",
                userId, timestamp, userId, timestamp
        });

        // TEST003 owner
        ownerDriverRecords.add(new Object[]{
                "TEST003", "BOB BROWN", "S3456789C", "bob@example.com", "93456789", "Y",
                userId, timestamp, userId, timestamp
        });

        // TEST004 owner
        ownerDriverRecords.add(new Object[]{
                "TEST004", "ALICE GREEN", "S4567890D", "alice@example.com", "94567890", "Y",
                userId, timestamp, userId, timestamp
        });

        // TEST005 owner
        ownerDriverRecords.add(new Object[]{
                "TEST005", "CHARLIE WHITE", "S5678901E", "charlie@example.com", "95678901", "Y",
                userId, timestamp, userId, timestamp
        });

        // TEST006 owner
        ownerDriverRecords.add(new Object[]{
                "TEST006", "DAVID BLACK", "S6789012F", "david@example.com", "96789012", "Y",
                userId, timestamp, userId, timestamp
        });

        // Exclusion list person
        ownerDriverRecords.add(new Object[]{
                "MHATEST999", "EXCLUDED PERSON", "S9999999Z", "excluded@example.com", "99999999", "Y",
                userId, timestamp, userId, timestamp
        });

        String sql = String.format(
                "INSERT INTO %s.ocms_offence_notice_owner_driver " +
                "(notice_no, name, id_no, email_addr, offender_tel_no, offender_indicator, " +
                "cre_user_id, cre_date, upd_user_id, upd_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                SCHEMA);

        int[] inserted = jdbcTemplate.batchUpdate(sql, ownerDriverRecords);
        return inserted.length;
    }

    /**
     * Insert test HST records.
     *
     * @param userId User ID for audit fields
     * @param timestamp Timestamp for audit fields
     * @return Number of records inserted
     */
    private int insertHstRecords(String userId, Timestamp timestamp) {
        log.info("Inserting HST test records");

        List<Object[]> hstRecords = new ArrayList<>();

        // HST for TEST003
        hstRecords.add(new Object[]{
                "S3456789C", "TEST003", "SBA1236C", timestamp, "Y",
                userId, timestamp, userId, timestamp
        });

        // HST for TEST004
        hstRecords.add(new Object[]{
                "S4567890D", "TEST004", "SBA1237D", timestamp, "Y",
                userId, timestamp, userId, timestamp
        });

        // HST for TEST002 (already has TS-HST)
        hstRecords.add(new Object[]{
                "S2345678B", "TEST002", "SBA1235B", timestamp, "Y",
                userId, timestamp, userId, timestamp
        });

        String sql = String.format(
                "INSERT INTO %s.ocms_hst " +
                "(id_no, notice_no, vehicle_no, processing_date, is_active, " +
                "cre_user_id, cre_date, upd_user_id, upd_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                SCHEMA);

        int[] inserted = jdbcTemplate.batchUpdate(sql, hstRecords);
        return inserted.length;
    }

    /**
     * Insert exclusion list record.
     *
     * @param userId User ID for audit fields
     * @param timestamp Timestamp for audit fields
     * @return Number of records inserted
     */
    private int insertExclusionListRecord(String userId, Timestamp timestamp) {
        log.info("Inserting exclusion list record");

        String sql = String.format(
                "INSERT INTO %s.ocms_enotification_exclusion_list " +
                "(id_no, cre_user_id, cre_date, upd_user_id, upd_date) " +
                "VALUES (?, ?, ?, ?, ?)",
                SCHEMA);

        int inserted = jdbcTemplate.update(sql, "S9999999Z", userId, timestamp, userId, timestamp);
        return inserted;
    }

    /**
     * Verify that test data was inserted correctly.
     * Checks counts and key fields in inserted records.
     *
     * @return Map with verification results by table
     */
    public Map<String, Boolean> verifyDataInsertion() {
        log.info("Verifying data insertion");
        Map<String, Boolean> results = new HashMap<>();

        // Verify VON records
        String vonSql = String.format(
                "SELECT COUNT(*) FROM %s.ocms_valid_offence_notice WHERE notice_no LIKE ?",
                SCHEMA);
        int vonCount = jdbcTemplate.queryForObject(vonSql, Integer.class, TEST_PATTERN);
        results.put("von", vonCount == 6);

        // Verify owner/driver records
        String ownerDriverSql = String.format(
                "SELECT COUNT(*) FROM %s.ocms_offence_notice_owner_driver WHERE notice_no LIKE ? OR notice_no LIKE ?",
                SCHEMA);
        int ownerDriverCount = jdbcTemplate.queryForObject(ownerDriverSql, Integer.class, TEST_PATTERN, MHATEST_PATTERN);
        results.put("ownerDriver", ownerDriverCount == 7);

        // Verify HST records
        String hstSql = String.format(
                "SELECT COUNT(*) FROM %s.ocms_hst WHERE notice_no LIKE ?",
                SCHEMA);
        int hstCount = jdbcTemplate.queryForObject(hstSql, Integer.class, TEST_PATTERN);
        results.put("hst", hstCount == 3);

        // Verify exclusion list record
        String exclusionSql = String.format(
                "SELECT COUNT(*) FROM %s.ocms_enotification_exclusion_list WHERE id_no = ?",
                SCHEMA);
        int exclusionCount = jdbcTemplate.queryForObject(exclusionSql, Integer.class, "S9999999Z");
        results.put("exclusionList", exclusionCount == 1);

        return results;
    }

    /**
     * Test the repository query used to select records for processing.
     * Verifies that the query returns the expected records.
     *
     * @return List of notice numbers returned by the query
     */
    public List<String> testRepositoryQuery() {
        log.info("Testing repository query");

        // This is the query used by the repository to select records for processing
        String sql = String.format(
                "SELECT von.notice_no " +
                "FROM %s.ocms_valid_offence_notice von " +
                "JOIN %s.ocms_offence_notice_owner_driver od ON von.notice_no = od.notice_no " +
                "WHERE von.last_processing_stage = 'ENA' " +
                "AND (von.suspension_type IS NULL OR (von.suspension_type = 'TS' AND von.epr_reason_of_suspension = 'HST')) " +
                "AND od.id_no NOT IN (SELECT id_no FROM %s.ocms_enotification_exclusion_list) " +
                "AND od.offender_indicator = 'Y' " +
                "AND von.notice_no LIKE ?",
                SCHEMA, SCHEMA, SCHEMA);

        List<String> selectedNotices = jdbcTemplate.queryForList(sql, String.class, TEST_PATTERN);
        log.info("Repository query returned {} notices: {}", selectedNotices.size(), selectedNotices);

        return selectedNotices;
    }

    /**
     * Test HST checking logic.
     * Verifies that HST records are correctly identified for suspension.
     *
     * @return Map with HST check results by notice number
     */
    public Map<String, Boolean> testHstChecking() {
        log.info("Testing HST checking logic");
        Map<String, Boolean> results = new HashMap<>();

        // This query checks if a notice has an active HST record
        String sql = String.format(
                "SELECT von.notice_no, " +
                "CASE WHEN hst.notice_no IS NOT NULL THEN 'Y' ELSE 'N' END as has_hst " +
                "FROM %s.ocms_valid_offence_notice von " +
                "JOIN %s.ocms_offence_notice_owner_driver od ON von.notice_no = od.notice_no " +
                "LEFT JOIN %s.ocms_hst hst ON od.id_no = hst.id_no AND hst.is_active = 'Y' " +
                "WHERE von.notice_no LIKE ? " +
                "ORDER BY von.notice_no",
                SCHEMA, SCHEMA, SCHEMA);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, TEST_PATTERN);

        for (Map<String, Object> row : rows) {
            String noticeNo = (String) row.get("notice_no");
            String hasHst = (String) row.get("has_hst");
            results.put(noticeNo, "Y".equals(hasHst));
        }

        return results;
    }

    /**
     * Verify notification records after processing.
     * Checks SMS and email notification records for each test case.
     *
     * @return Map with notification counts by type and notice number
     */
    public Map<String, Map<String, Integer>> verifyNotificationRecords() {
        log.info("Verifying notification records");
        Map<String, Map<String, Integer>> results = new HashMap<>();

        // Check SMS notification records
        String smsSql = String.format(
                "SELECT notice_no, COUNT(*) as count " +
                "FROM %s.ocms_sms_notification_records " +
                "WHERE notice_no LIKE ? " +
                "GROUP BY notice_no",
                SCHEMA);

        List<Map<String, Object>> smsRows = jdbcTemplate.queryForList(smsSql, TEST_PATTERN);
        Map<String, Integer> smsCounts = new HashMap<>();

        for (Map<String, Object> row : smsRows) {
            String noticeNo = (String) row.get("notice_no");
            Integer count = ((Number) row.get("count")).intValue();
            smsCounts.put(noticeNo, count);
        }

        // Check email notification records
        String emailSql = String.format(
                "SELECT notice_no, COUNT(*) as count " +
                "FROM %s.ocms_email_notification_records " +
                "WHERE notice_no LIKE ? " +
                "GROUP BY notice_no",
                SCHEMA);

        List<Map<String, Object>> emailRows = jdbcTemplate.queryForList(emailSql, TEST_PATTERN);
        Map<String, Integer> emailCounts = new HashMap<>();

        for (Map<String, Object> row : emailRows) {
            String noticeNo = (String) row.get("notice_no");
            Integer count = ((Number) row.get("count")).intValue();
            emailCounts.put(noticeNo, count);
        }

        results.put("sms", smsCounts);
        results.put("email", emailCounts);

        return results;
    }

    /**
     * Verify TS-HST suspension updates.
     * Checks VON and suspended notice records for HST-related suspensions.
     *
     * @return Map with suspension results by table and notice number
     */
    public Map<String, Map<String, String>> verifyTsHstSuspension() {
        log.info("Verifying TS-HST suspension updates");
        Map<String, Map<String, String>> results = new HashMap<>();

        // Check VON suspension updates
        String vonSql = String.format(
                "SELECT notice_no, " +
                "CASE WHEN suspension_type IS NULL THEN 'NULL' ELSE suspension_type END as suspension_type, " +
                "CASE WHEN epr_reason_of_suspension IS NULL THEN 'NULL' ELSE epr_reason_of_suspension END as reason " +
                "FROM %s.ocms_valid_offence_notice " +
                "WHERE notice_no LIKE ? " +
                "ORDER BY notice_no",
                SCHEMA);

        List<Map<String, Object>> vonRows = jdbcTemplate.queryForList(vonSql, TEST_PATTERN);
        Map<String, String> vonSuspensions = new HashMap<>();

        for (Map<String, Object> row : vonRows) {
            String noticeNo = (String) row.get("notice_no");
            String suspensionType = (String) row.get("suspension_type");
            vonSuspensions.put(noticeNo, suspensionType);
        }

        // Check suspended notice records
        String suspendedSql = String.format(
                "SELECT notice_no, suspension_type " +
                "FROM %s.ocms_suspended_notice " +
                "WHERE notice_no LIKE ? " +
                "ORDER BY notice_no",
                SCHEMA);

        List<Map<String, Object>> suspendedRows = jdbcTemplate.queryForList(suspendedSql, TEST_PATTERN);
        Map<String, String> suspendedNotices = new HashMap<>();

        for (Map<String, Object> row : suspendedRows) {
            String noticeNo = (String) row.get("notice_no");
            String suspensionType = (String) row.get("suspension_type");
            suspendedNotices.put(noticeNo, suspensionType);
        }

        results.put("von", vonSuspensions);
        results.put("suspendedNotice", suspendedNotices);

        return results;
    }
}
