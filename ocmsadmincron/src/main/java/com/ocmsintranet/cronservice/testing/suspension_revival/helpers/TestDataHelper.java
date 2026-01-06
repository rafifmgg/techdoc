package com.ocmsintranet.cronservice.testing.suspension_revival.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Helper class for creating and cleaning up test data
 * Used by test controllers to setup test scenarios
 *
 * IMPORTANT: Only available in non-production profiles
 */
@Slf4j
@Component
@Profile("!prod")
public class TestDataHelper {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Keep track of test data for cleanup
    private final Set<String> testNoticeNos = new HashSet<>();
    private final Set<Long> testSuspensionIds = new HashSet<>();

    /**
     * Create a test Valid Offence Notice record
     *
     * @param noticeNo Unique notice number (e.g., "900000001A" - range 900000000A-900999999Z for testing)
     * @param vehicleNo Vehicle number (e.g., "TESTROV001")
     * @param nric NRIC/FIN of offender (optional, can be null if UEN provided)
     * @param uen UEN of company (optional, Singapore format: YYYYNNNNNX or NNNNNNNNNX)
     * @return Created notice number
     */
    public String createTestValidOffenceNotice(String noticeNo, String vehicleNo, String nric, String uen) {
        log.info("Creating test Valid Offence Notice: noticeNo={}, vehicleNo={}, nric={}, uen={}",
                noticeNo, vehicleNo, nric, uen);

        String sql = """
            INSERT INTO [ocmsizmgr].[ocms_valid_offence_notice] (
                notice_no, vehicle_no, vehicle_registration_type, vehicle_category,
                offence_notice_type, notice_date_and_time, pp_code, pp_name,
                parking_lot_no, computer_rule_code, composition_amount, amount_payable,
                subsystem_label, warden_no, last_processing_stage, last_processing_date,
                next_processing_stage, next_processing_date,
                cre_date, cre_user_id, upd_date, upd_user_id, an_flag
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                noticeNo,
                vehicleNo,
                "S",  // S = Singapore registered
                "C",  // C = Car
                "O",  // O = Offence notice
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                "A0045",  // Test parking place code
                "TEST PARKING AREA",
                "TEST001",
                10300,  // Test rule code
                70.00,  // Composition amount
                70.00,  // Amount payable
                "004",  // OCMS subsystem
                "TEST001",  // Warden number
                "ENA",  // E-Notification Acknowledgement
                Timestamp.valueOf(LocalDateTime.now()),
                "RD1",  // Reminder 1
                Timestamp.valueOf(LocalDateTime.now().plusDays(7)),
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER",
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER",
                "N"  // Not an adjustment notice
        );

        // Add NRIC if provided
        if (nric != null && !nric.trim().isEmpty()) {
            String updateSql = "UPDATE [ocmsizmgr].[ocms_valid_offence_notice] SET nric = ? WHERE notice_no = ?";
            jdbcTemplate.update(updateSql, nric, noticeNo);
        }

        // Add UEN if provided
        if (uen != null && !uen.trim().isEmpty()) {
            String updateSql = "UPDATE [ocmsizmgr].[ocms_valid_offence_notice] SET uen = ? WHERE notice_no = ?";
            jdbcTemplate.update(updateSql, uen, noticeNo);
        }

        testNoticeNos.add(noticeNo);
        log.info("Test Valid Offence Notice created successfully: {}", noticeNo);
        return noticeNo;
    }

    /**
     * Create a test suspension record directly in database
     * Used for testing revival scenarios
     *
     * @param noticeNo Notice number
     * @param suspensionType Suspension type (TS/PS)
     * @param reasonOfSuspension Reason code (ROV, RIP, RP2, NRO, OLD, ACR, SYS, HST, PTS, PPT, etc.)
     * @param daysToRevive Days until revival (optional, for TS types)
     * @return Generated sr_no
     */
    public Integer createTestSuspension(
            String noticeNo,
            String suspensionType,
            String reasonOfSuspension,
            Integer daysToRevive) {

        log.info("Creating test suspension: noticeNo={}, type={}, reason={}",
                noticeNo, suspensionType, reasonOfSuspension);

        // Get next sr_no
        Integer srNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sr_no), 0) + 1 FROM [ocmsizmgr].[ocms_suspended_notice]",
                Integer.class
        );

        // Calculate due_date_of_revival for TS types
        Timestamp dueDateOfRevival = null;
        if ("TS".equals(suspensionType) && daysToRevive != null) {
            if (daysToRevive > 0) {
                dueDateOfRevival = Timestamp.valueOf(LocalDateTime.now().plusDays(daysToRevive));
            } else if (daysToRevive == 0) {
                // Due today
                dueDateOfRevival = Timestamp.valueOf(LocalDateTime.now());
            } else {
                // Negative days = past due date (for testing already-revived scenarios)
                dueDateOfRevival = Timestamp.valueOf(LocalDateTime.now().plusDays(daysToRevive));
            }
        }

        String sql = """
            INSERT INTO [ocmsizmgr].[ocms_suspended_notice] (
                sr_no, notice_no, date_of_suspension,
                suspension_type, reason_of_suspension,
                suspension_remarks, officer_authorising_suspension,
                suspension_source, due_date_of_revival, date_of_revival,
                cre_date, cre_user_id, upd_date, upd_user_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                srNo,
                noticeNo,
                Timestamp.valueOf(LocalDateTime.now()),
                suspensionType,
                reasonOfSuspension,
                "Test suspension created by test controller",
                "TEST_OFFICER",
                "004", // OCMS
                dueDateOfRevival,
                null, // Not revived yet
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER",
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER"
        );

        testSuspensionIds.add(srNo.longValue());
        log.info("Test suspension created successfully: sr_no={}", srNo);
        return srNo;
    }

    /**
     * Overloaded method for creating test suspension with vehicle details
     * Used by PS-Toppan, TS-DataHive Single, etc.
     *
     * @param noticeNo Notice number
     * @param vehicleNo Vehicle number
     * @param nric NRIC/FIN
     * @param suspensionType Suspension type (TS/PS)
     * @param reasonOfSuspension Reason code
     * @param daysToRevive Days until revival (optional)
     * @return Generated sr_no
     */
    public Integer createTestSuspension(
            String noticeNo,
            String vehicleNo,
            String nric,
            String suspensionType,
            String reasonOfSuspension,
            Integer daysToRevive) {

        // Vehicle and NRIC are already in the VON, so just delegate to main method
        return createTestSuspension(noticeNo, suspensionType, reasonOfSuspension, daysToRevive);
    }

    /**
     * Overloaded method for creating test suspension with LocalDate for revival
     * Used by auto-revival tests
     *
     * @param noticeNo Notice number
     * @param vehicleNo Vehicle number
     * @param nric NRIC/FIN
     * @param suspensionType Suspension type (TS/PS)
     * @param reasonOfSuspension Reason code
     * @param revivalDate Specific revival date
     * @return Generated sr_no
     */
    public Integer createTestSuspension(
            String noticeNo,
            String vehicleNo,
            String nric,
            String suspensionType,
            String reasonOfSuspension,
            java.time.LocalDate revivalDate) {

        log.info("Creating test suspension: noticeNo={}, type={}, reason={}, revivalDate={}",
                noticeNo, suspensionType, reasonOfSuspension, revivalDate);

        // Get next sr_no
        Integer srNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sr_no), 0) + 1 FROM [ocmsizmgr].[ocms_suspended_notice]",
                Integer.class
        );

        // Convert LocalDate to Timestamp
        Timestamp dueDateOfRevival = null;
        if (revivalDate != null) {
            dueDateOfRevival = Timestamp.valueOf(revivalDate.atStartOfDay());
        }

        String sql = """
            INSERT INTO [ocmsizmgr].[ocms_suspended_notice] (
                sr_no, notice_no, date_of_suspension,
                suspension_type, reason_of_suspension,
                suspension_remarks, officer_authorising_suspension,
                suspension_source, due_date_of_revival, date_of_revival,
                cre_date, cre_user_id, upd_date, upd_user_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                srNo,
                noticeNo,
                Timestamp.valueOf(LocalDateTime.now()),
                suspensionType,
                reasonOfSuspension,
                "Test suspension created by test controller",
                "TEST_OFFICER",
                "004", // OCMS
                dueDateOfRevival,
                null, // Not revived yet
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER",
                Timestamp.valueOf(LocalDateTime.now()),
                "TEST_OFFICER"
        );

        testSuspensionIds.add(srNo.longValue());
        log.info("Test suspension created successfully: sr_no={}", srNo);
        return srNo;
    }

    /**
     * Create test DataHive batch file data
     *
     * @param nric NRIC/FIN
     * @param suspensionType Type of suspension (ACR, SYS, etc.)
     * @return File ID
     */
    public Long createTestDatahiveBatchFile(String nric, String suspensionType) {
        log.info("Creating test DataHive batch file: nric={}, suspensionType={}", nric, suspensionType);

        String sql = """
            INSERT INTO datahive_batch_files (
                file_name, nric, suspension_type,
                status, created_date
            ) VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                "TEST_DH_BATCH_" + suspensionType + "_" + System.currentTimeMillis() + ".txt",
                nric,
                suspensionType,
                "PENDING",
                Timestamp.valueOf(LocalDateTime.now())
        );

        Long fileId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM datahive_batch_files WHERE nric = ?",
                Long.class,
                nric
        );

        log.info("Test DataHive batch file created successfully: id={}", fileId);
        return fileId;
    }

    /**
     * Create test notification record for HST notifications
     *
     * @param noticeNo Notice number
     * @param suspensionType Type of suspension (HST, etc.)
     * @return Notification ID
     */
    public Long createTestNotificationRecord(String noticeNo, String suspensionType) {
        log.info("Creating test notification record: noticeNo={}, suspensionType={}", noticeNo, suspensionType);

        String sql = """
            INSERT INTO notification_suspension_queue (
                notice_no, suspension_type, notification_sent,
                status, created_date
            ) VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                noticeNo,
                suspensionType,
                "N",
                "PENDING",
                Timestamp.valueOf(LocalDateTime.now())
        );

        Long notificationId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM notification_suspension_queue WHERE notice_no = ?",
                Long.class,
                noticeNo
        );

        log.info("Test notification record created successfully: id={}", notificationId);
        return notificationId;
    }

    /**
     * Create test retry record for retry mechanism testing
     *
     * @param noticeNo Notice number
     * @param failureReason Reason for failure
     * @param attemptCount Number of retry attempts
     * @return Retry record ID
     */
    public Long createTestRetryRecord(String noticeNo, String failureReason, Integer attemptCount) {
        log.info("Creating test retry record: noticeNo={}, failureReason={}, attemptCount={}",
                noticeNo, failureReason, attemptCount);

        String sql = """
            INSERT INTO suspension_retry_queue (
                notice_no, failure_reason, attempt_count,
                status, last_attempt_date, created_date
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                noticeNo,
                failureReason,
                attemptCount,
                "PENDING",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
        );

        Long retryId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM suspension_retry_queue WHERE notice_no = ?",
                Long.class,
                noticeNo
        );

        log.info("Test retry record created successfully: id={}", retryId);
        return retryId;
    }

    /**
     * Create test LTA file data
     *
     * @param vehicleNo Vehicle number
     * @param fileType File type (e.g., "ROV")
     * @return File ID
     */
    public Long createTestLtaFile(String vehicleNo, String fileType) {
        log.info("Creating test LTA file: vehicleNo={}, fileType={}", vehicleNo, fileType);

        String sql = """
            INSERT INTO lta_files (
                file_name, file_type, vehicle_no,
                status, created_date
            ) VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                "TEST_LTA_" + fileType + "_" + System.currentTimeMillis() + ".txt",
                fileType,
                vehicleNo,
                "PENDING",
                Timestamp.valueOf(LocalDateTime.now())
        );

        Long fileId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM lta_files WHERE vehicle_no = ?",
                Long.class,
                vehicleNo
        );

        log.info("Test LTA file created successfully: id={}", fileId);
        return fileId;
    }

    /**
     * Create test MHA file data
     *
     * @param nric NRIC/FIN
     * @param reasonCode Reason code (RIP, RP2, NRO, OLD)
     * @return File ID
     */
    public Long createTestMhaFile(String nric, String reasonCode) {
        log.info("Creating test MHA file: nric={}, reasonCode={}", nric, reasonCode);

        String sql = """
            INSERT INTO mha_nric_files (
                file_name, nric, reason_code,
                status, created_date
            ) VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                "TEST_MHA_" + reasonCode + "_" + System.currentTimeMillis() + ".txt",
                nric,
                reasonCode,
                "PENDING",
                Timestamp.valueOf(LocalDateTime.now())
        );

        Long fileId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM mha_nric_files WHERE nric = ?",
                Long.class,
                nric
        );

        log.info("Test MHA file created successfully: id={}", fileId);
        return fileId;
    }

    /**
     * Get test suspension details
     *
     * @param noticeNo Notice number
     * @return Suspension details or null if not found
     */
    public Map<String, Object> getSuspensionDetails(String noticeNo) {
        log.info("Retrieving suspension details for noticeNo: {}", noticeNo);

        String sql = """
            SELECT TOP 1 sr_no, notice_no, suspension_type, reason_of_suspension,
                   suspension_remarks, officer_authorising_suspension,
                   suspension_source, date_of_suspension,
                   due_date_of_revival, date_of_revival,
                   revival_remarks, officer_authorising_revival
            FROM [ocmsizmgr].[ocms_suspended_notice]
            WHERE notice_no = ?
            ORDER BY date_of_suspension DESC
            """;

        try {
            return jdbcTemplate.queryForMap(sql, noticeNo);
        } catch (Exception e) {
            log.warn("No suspension found for noticeNo: {}", noticeNo);
            return null;
        }
    }

    /**
     * Check if suspension exists
     *
     * @param noticeNo Notice number
     * @return true if suspension exists
     */
    public boolean suspensionExists(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM [ocmsizmgr].[ocms_suspended_notice] WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if suspension is revived
     *
     * @param noticeNo Notice number
     * @return true if suspension is revived (date_of_revival IS NOT NULL)
     */
    public boolean isRevived(String noticeNo) {
        String sql = """
            SELECT COUNT(*) FROM [ocmsizmgr].[ocms_suspended_notice]
            WHERE notice_no = ? AND date_of_revival IS NOT NULL
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Get all suspensions due for revival
     *
     * @return List of notice numbers due for revival
     */
    public List<String> getSuspensionsDueForRevival() {
        String sql = """
            SELECT notice_no FROM [ocmsizmgr].[ocms_suspended_notice]
            WHERE suspension_type = 'TS'
            AND due_date_of_revival <= CAST(GETDATE() AS DATE)
            AND date_of_revival IS NULL
            ORDER BY due_date_of_revival
            """;

        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Cleanup test data
     * Deletes all test VONs and suspensions created during testing
     *
     * @return Number of records deleted
     */
    public Map<String, Integer> cleanupTestData() {
        log.info("Starting test data cleanup...");

        Map<String, Integer> deletedCounts = new HashMap<>();

        // Delete test suspensions
        if (!testSuspensionIds.isEmpty()) {
            String sql = "DELETE FROM [ocmsizmgr].[ocms_suspended_notice] WHERE sr_no IN (" +
                    String.join(",", testSuspensionIds.stream().map(String::valueOf).toList()) + ")";
            int suspensionCount = jdbcTemplate.update(sql);
            deletedCounts.put("suspensions", suspensionCount);
            log.info("Deleted {} test suspensions", suspensionCount);
        }

        // Delete test VONs
        if (!testNoticeNos.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(testNoticeNos.size(), "?"));
            String sql = "DELETE FROM [ocmsizmgr].[ocms_valid_offence_notice] WHERE notice_no IN (" + placeholders + ")";
            int vonCount = jdbcTemplate.update(sql, testNoticeNos.toArray());
            deletedCounts.put("valid_offence_notices", vonCount);
            log.info("Deleted {} test Valid Offence Notices", vonCount);
        }

        // Delete test LTA files
        String ltaSql = "DELETE FROM lta_files WHERE file_name LIKE 'TEST_LTA_%'";
        int ltaCount = jdbcTemplate.update(ltaSql);
        if (ltaCount > 0) {
            deletedCounts.put("lta_files", ltaCount);
            log.info("Deleted {} test LTA files", ltaCount);
        }

        // Delete test MHA files
        String mhaSql = "DELETE FROM mha_nric_files WHERE file_name LIKE 'TEST_MHA_%'";
        int mhaCount = jdbcTemplate.update(mhaSql);
        if (mhaCount > 0) {
            deletedCounts.put("mha_files", mhaCount);
            log.info("Deleted {} test MHA files", mhaCount);
        }

        // Clear tracking sets
        testNoticeNos.clear();
        testSuspensionIds.clear();

        log.info("Test data cleanup completed: {}", deletedCounts);
        return deletedCounts;
    }

    /**
     * Cleanup specific test data by notice number
     *
     * @param noticeNo Notice number to cleanup
     */
    public void cleanupByNoticeNo(String noticeNo) {
        log.info("Cleaning up test data for noticeNo: {}", noticeNo);

        // Delete suspension
        jdbcTemplate.update("DELETE FROM [ocmsizmgr].[ocms_suspended_notice] WHERE notice_no = ?", noticeNo);

        // Delete VON
        jdbcTemplate.update("DELETE FROM [ocmsizmgr].[ocms_valid_offence_notice] WHERE notice_no = ?", noticeNo);

        // Remove from tracking
        testNoticeNos.remove(noticeNo);

        log.info("Cleanup completed for noticeNo: {}", noticeNo);
    }

    /**
     * Get tracked test notice numbers
     *
     * @return Set of test notice numbers
     */
    public Set<String> getTrackedNoticeNos() {
        return new HashSet<>(testNoticeNos);
    }

    /**
     * Get tracked test suspension IDs
     *
     * @return Set of test suspension sr_no values
     */
    public Set<Long> getTrackedSuspensionIds() {
        return new HashSet<>(testSuspensionIds);
    }
}
