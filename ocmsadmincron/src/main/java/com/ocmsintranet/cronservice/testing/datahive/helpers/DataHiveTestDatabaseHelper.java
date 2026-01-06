package com.ocmsintranet.cronservice.testing.datahive.helpers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Helper class for common database operations in DataHive testing scenarios.
 *
 * This helper consolidates all duplicate database UPSERT operations that are
 * shared across multiple DataHive test services to eliminate code duplication.
 *
 * Database Tables Supported:
 * - ocms_valid_offence_notice
 * - ocms_offence_notice_detail
 * - ocms_offence_notice_owner_driver
 * - ocms_offence_notice_owner_driver_addr
 * - ocms_dh_acra_company_detail
 * - ocms_dh_acra_shareholder_info
 * - ocms_dh_acra_board_info
 */
@Slf4j
@Component
public class DataHiveTestDatabaseHelper {

    private static final String SCHEMA = "ocmsizmgr";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===============================
    // Table Existence Check Methods
    // ===============================

    /**
     * Check if valid offence notice record exists
     */
    public boolean existsValidOffenceNotice(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if offence notice detail record exists
     */
    public boolean existsOffenceNoticeDetail(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if owner driver record exists
     */
    public boolean existsOwnerDriver(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if owner driver address record exists
     */
    public boolean existsOwnerDriverAddress(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if company detail record exists
     */
    public boolean existsCompanyDetail(String uen, String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_company_detail WHERE uen = ? AND notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uen, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if shareholder info records exist
     */
    public boolean existsShareholderInfo(String uen, String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info WHERE company_uen = ? AND notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uen, noticeNo);
        return count != null && count > 0;
    }

    /**
     * Check if board info records exist
     */
    public boolean existsBoardInfo(String uen, String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_board_info WHERE entity_uen = ? AND notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, uen, noticeNo);
        return count != null && count > 0;
    }

    // ===============================
    // UPSERT Pattern Methods
    // ===============================

    /**
     * UPSERT pattern for valid offence notice
     */
    public void resetOrInsertValidOffenceNotice(String noticeNo, String vehicleNo, String ppCode, String ppName,
                                               java.math.BigDecimal compositionAmount, Integer computerRuleCode,
                                               TestStepResult result) {
        if (existsValidOffenceNotice(noticeNo)) {
            resetValidOffenceNotice(noticeNo, vehicleNo, ppCode, ppName, compositionAmount, computerRuleCode, result);
        } else {
            insertValidOffenceNotice(noticeNo, vehicleNo, ppCode, ppName, compositionAmount, computerRuleCode, result);
        }
    }

    /**
     * UPSERT pattern for offence notice detail
     */
    public void resetOrInsertOffenceNoticeDetail(String noticeNo, String chassisNumber, String makeDescription,
                                                String primaryColour, TestStepResult result) {
        if (existsOffenceNoticeDetail(noticeNo)) {
            resetOffenceNoticeDetail(noticeNo, chassisNumber, makeDescription, primaryColour, result);
        } else {
            insertOffenceNoticeDetail(noticeNo, chassisNumber, makeDescription, primaryColour, result);
        }
    }

    /**
     * UPSERT pattern for owner driver
     */
    public void resetOrInsertOwnerDriver(String noticeNo, String idType, String idNo, String name, TestStepResult result) {
        if (existsOwnerDriver(noticeNo)) {
            resetOwnerDriver(noticeNo, idType, idNo, name, result);
        } else {
            insertOwnerDriver(noticeNo, idType, idNo, name, result);
        }
    }

    /**
     * UPSERT pattern for owner driver address
     */
    public void resetOrInsertOwnerDriverAddress(String noticeNo, String blkHseNo, String streetName, String floorNo,
                                               String unitNo, String bldgName, String postalCode, TestStepResult result) {
        if (existsOwnerDriverAddress(noticeNo)) {
            resetOwnerDriverAddress(noticeNo, blkHseNo, streetName, floorNo, unitNo, bldgName, postalCode, result);
        } else {
            insertOwnerDriverAddress(noticeNo, blkHseNo, streetName, floorNo, unitNo, bldgName, postalCode, result);
        }
    }

    /**
     * UPSERT pattern for company detail
     */
    public void resetOrInsertCompanyDetail(String uen, String noticeNo, String entityName, String entityType,
                                          LocalDateTime registrationDate, String entityStatusCode, TestStepResult result) {
        if (existsCompanyDetail(uen, noticeNo)) {
            updateCompanyDetail(uen, noticeNo, entityName, entityType, registrationDate, entityStatusCode, result);
        } else {
            insertCompanyDetail(uen, noticeNo, entityName, entityType, registrationDate, entityStatusCode, result);
        }
    }

    // ===============================
    // Reset/Update Methods for Existing Records
    // ===============================

    private void resetValidOffenceNotice(String noticeNo, String vehicleNo, String ppCode, String ppName,
                                        java.math.BigDecimal compositionAmount, Integer computerRuleCode, TestStepResult result) {
        String sql = "UPDATE " + SCHEMA + ".ocms_valid_offence_notice SET " +
                     "vehicle_no = ?, offence_notice_type = ?, last_processing_stage = ?, next_processing_stage = ?, " +
                     "last_processing_date = ?, next_processing_date = ?, notice_date_and_time = ?, " +
                     "pp_code = ?, pp_name = ?, composition_amount = ?, computer_rule_code = ?, vehicle_category = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            vehicleNo, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            ppCode, ppName, compositionAmount, computerRuleCode, "T",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 VON record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOffenceNoticeDetail(String noticeNo, String chassisNumber, String makeDescription,
                                         String primaryColour, TestStepResult result) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_detail SET " +
                     "lta_chassis_number = ?, lta_make_description = ?, lta_primary_colour = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            chassisNumber, makeDescription, primaryColour,
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 OND record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriver(String noticeNo, String idType, String idNo, String name, TestStepResult result) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                "SET name = ?, upd_user_id = ?, upd_date = ? " +
                "WHERE notice_no = ? AND owner_driver_indicator = ? AND id_type = ? AND id_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
                name, "SYSTEM", LocalDateTime.now(),
                noticeNo,
                "O", idType, idNo);

        result.addDetail("🔄 Owner/driver record reset: " + noticeNo + " (ID: " + idNo + ", " + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriverAddress(String noticeNo, String blkHseNo, String streetName, String floorNo,
                                        String unitNo, String bldgName, String postalCode, TestStepResult result) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                "SET blk_hse_no = ?, street_name = ?, floor_no = ?, unit_no = ?, bldg_name = ?, postal_code = ?, " +
                "upd_user_id = ?, upd_date = ? " +
                "WHERE notice_no = ? AND owner_driver_indicator = ? AND type_of_address = 'mha_reg'";

        int rowsUpdated = jdbcTemplate.update(sql,
                blkHseNo, streetName, floorNo, unitNo, bldgName, postalCode,
                "SYSTEM", LocalDateTime.now(),
                noticeNo, "O");

        result.addDetail("🔄 Owner/driver address reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void updateCompanyDetail(String uen, String noticeNo, String entityName, String entityType,
                                    LocalDateTime registrationDate, String entityStatusCode, TestStepResult result) {
        try {
            String updateSql = "UPDATE " + SCHEMA + ".ocms_dh_acra_company_detail SET " +
                    "entity_name = ?, entity_type = ?, registration_date = ?, entity_status_code = ?, " +
                    "upd_user_id = ?, upd_date = ? " +
                    "WHERE uen = ? AND notice_no = ?";

            int rowsUpdated = jdbcTemplate.update(updateSql,
                    entityName, entityType, registrationDate, entityStatusCode,
                    "SYSTEM", LocalDateTime.now(),
                    uen, noticeNo);

            result.addDetail("✅ Company detail data updated: " + uen + " (" + rowsUpdated + " rows)");
        } catch (Exception e) {
            result.addDetail("❌ Company detail data update failed: " + e.getMessage());
        }
    }

    // ===============================
    // Insert Methods for New Records
    // ===============================

    private void insertValidOffenceNotice(String noticeNo, String vehicleNo, String ppCode, String ppName,
                                         java.math.BigDecimal compositionAmount, Integer computerRuleCode, TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice " +
                     "(notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, vehicleNo, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            ppCode, ppName, compositionAmount, computerRuleCode, "T",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ VON record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertOffenceNoticeDetail(String noticeNo, String chassisNumber, String makeDescription,
                                          String primaryColour, TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail " +
                     "(notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, chassisNumber, makeDescription, primaryColour,
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ OND record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertOwnerDriver(String noticeNo, String idType, String idNo, String name, TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                     "(notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "O", idType, idNo, name, "Y",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (ID: " + idNo + ", " + rowsInserted + " rows inserted)");
    }

    private void insertOwnerDriverAddress(String noticeNo, String blkHseNo, String streetName, String floorNo,
                                         String unitNo, String bldgName, String postalCode, TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                     "(notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, " +
                     "floor_no, unit_no, bldg_name, postal_code, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "O", "mha_reg", blkHseNo, streetName,
            floorNo, unitNo, bldgName, postalCode,
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver address record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertCompanyDetail(String uen, String noticeNo, String entityName, String entityType,
                                    LocalDateTime registrationDate, String entityStatusCode, TestStepResult result) {
        try {
            String insertSql = "INSERT INTO " + SCHEMA + ".ocms_dh_acra_company_detail " +
                    "(uen, notice_no, entity_name, entity_type, registration_date, entity_status_code, " +
                    "cre_user_id, cre_date, upd_user_id, upd_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int rowsInserted = jdbcTemplate.update(insertSql,
                    uen, noticeNo, entityName, entityType, registrationDate, entityStatusCode,
                    "SYSTEM", LocalDateTime.now(), "SYSTEM", LocalDateTime.now());

            result.addDetail("✅ Company detail data inserted: " + uen + " (" + rowsInserted + " rows)");
        } catch (Exception e) {
            result.addDetail("❌ Company detail data insert failed: " + e.getMessage());
        }
    }

    // ===============================
    // Bulk Operations for Shareholder/Board Data
    // ===============================

    /**
     * UPSERT shareholder info data in bulk
     */
    public void resetOrInsertShareholderInfo(String uen, String noticeNo, Object[][] shareholderData, TestStepResult result) {
        try {
            int totalProcessed = 0;
            int totalUpdated = 0;
            int totalInserted = 0;

            for (Object[] data : shareholderData) {
                String category = (String) data[0];
                String personIdNo = (String) data[1];
                Integer shareAllottedNo = (Integer) data[2];

                // Check if record exists
                String checkSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info " +
                        "WHERE company_uen = ? AND notice_no = ? AND category = ? AND person_id_no = ?";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                        uen, noticeNo, category, personIdNo);

                if (count != null && count > 0) {
                    // Update existing record
                    String updateSql = "UPDATE " + SCHEMA + ".ocms_dh_acra_shareholder_info SET " +
                            "share_allotted_no = ?, upd_user_id = ?, upd_date = ? " +
                            "WHERE company_uen = ? AND notice_no = ? AND category = ? AND person_id_no = ?";

                    jdbcTemplate.update(updateSql,
                            shareAllottedNo, "SYSTEM", LocalDateTime.now(),
                            uen, noticeNo, category, personIdNo);
                    totalUpdated++;
                } else {
                    // Insert new record
                    String insertSql = "INSERT INTO " + SCHEMA + ".ocms_dh_acra_shareholder_info " +
                            "(company_uen, notice_no, category, person_id_no, share_allotted_no, " +
                            "cre_user_id, cre_date, upd_user_id, upd_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    jdbcTemplate.update(insertSql,
                            uen, noticeNo, category, personIdNo, shareAllottedNo,
                            "SYSTEM", LocalDateTime.now(), "SYSTEM", LocalDateTime.now());
                    totalInserted++;
                }
                totalProcessed++;
            }

            result.addDetail("✅ Shareholder info data upserted: " + uen + " (" + totalProcessed + " processed, " +
                           totalUpdated + " updated, " + totalInserted + " inserted)");

        } catch (Exception e) {
            result.addDetail("❌ Shareholder info data upsert failed: " + e.getMessage());
        }
    }

    /**
     * UPSERT board info data in bulk
     */
    public void resetOrInsertBoardInfo(String uen, String noticeNo, Object[][] boardData, TestStepResult result) {
        try {
            int totalProcessed = 0;
            int totalUpdated = 0;
            int totalInserted = 0;

            for (Object[] data : boardData) {
                String personIdNo = (String) data[0];
                String positionHeldCode = (String) data[1];
                LocalDateTime appointmentDate = (LocalDateTime) data[2];
                LocalDateTime withdrawnDate = (LocalDateTime) data[3];

                // Check if record exists
                String checkSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_board_info " +
                        "WHERE entity_uen = ? AND notice_no = ? AND person_id_no = ? AND position_held_code = ?";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                        uen, noticeNo, personIdNo, positionHeldCode);

                if (count != null && count > 0) {
                    // Update existing record
                    String updateSql = "UPDATE " + SCHEMA + ".ocms_dh_acra_board_info SET " +
                            "position_appointment_date = ?, position_withdrawn_date = ?, reference_period = ?, " +
                            "upd_user_id = ?, upd_date = ? " +
                            "WHERE entity_uen = ? AND notice_no = ? AND person_id_no = ? AND position_held_code = ?";

                    jdbcTemplate.update(updateSql,
                            appointmentDate, withdrawnDate, LocalDateTime.now(),
                            "SYSTEM", LocalDateTime.now(),
                            uen, noticeNo, personIdNo, positionHeldCode);
                    totalUpdated++;
                } else {
                    // Insert new record
                    String insertSql = "INSERT INTO " + SCHEMA + ".ocms_dh_acra_board_info " +
                            "(entity_uen, notice_no, person_id_no, position_held_code, position_appointment_date, position_withdrawn_date, reference_period, " +
                            "cre_user_id, cre_date, upd_user_id, upd_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    jdbcTemplate.update(insertSql,
                            uen, noticeNo, personIdNo, positionHeldCode, appointmentDate, withdrawnDate, LocalDateTime.now(),
                            "SYSTEM", LocalDateTime.now(), "SYSTEM", LocalDateTime.now());
                    totalInserted++;
                }
                totalProcessed++;
            }

            result.addDetail("✅ Board info data upserted: " + uen + " (" + totalProcessed + " processed, " +
                           totalUpdated + " updated, " + totalInserted + " inserted)");

        } catch (Exception e) {
            result.addDetail("❌ Board info data upsert failed: " + e.getMessage());
        }
    }

    // ===============================
    // Verification Helper Methods
    // ===============================

    /**
     * Verify no shareholder data exists for UEN and notice
     */
    public void verifyNoShareholderData(String uen, String noticeNo, TestStepResult result) {
        try {
            String checkSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info WHERE company_uen = ? AND notice_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, uen, noticeNo);

            if (count != null && count > 0) {
                result.addDetail("⚠️ Found existing shareholder data: " + count + " records - will be ignored");
            } else {
                result.addDetail("✅ No existing shareholder data found - correct");
            }

        } catch (Exception e) {
            result.addDetail("❌ Error checking existing shareholder data: " + e.getMessage());
        }
    }

    /**
     * Verify no board data exists for UEN and notice
     */
    public void verifyNoBoardData(String uen, String noticeNo, TestStepResult result) {
        try {
            String checkSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_board_info WHERE entity_uen = ? AND notice_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, uen, noticeNo);

            if (count != null && count > 0) {
                result.addDetail("⚠️ Found existing board data: " + count + " records - will be ignored");
            } else {
                result.addDetail("✅ No existing board data found - correct");
            }

        } catch (Exception e) {
            result.addDetail("❌ Error checking existing board data: " + e.getMessage());
        }
    }

    /**
     * Verify that no shareholder data was processed
     */
    public void verifyNoShareholderDataProcessed(String uen, String noticeNo, TestStepResult result) {
        try {
            String dbQuery = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info " +
                    "WHERE company_uen = ? AND notice_no = ?";

            Integer shareholderCount = jdbcTemplate.queryForObject(dbQuery, Integer.class, uen, noticeNo);

            if (shareholderCount != null && shareholderCount == 0) {
                result.addDetail("✅ CONFIRMED: No shareholder records processed (Expected: 0, Actual: " + shareholderCount + ")");
            } else {
                result.addDetail("❌ ERROR: Found unexpected shareholder records (Expected: 0, Actual: " + shareholderCount + ")");
            }

        } catch (Exception e) {
            result.addDetail("❌ SHAREHOLDER VERIFICATION ERROR: " + e.getMessage());
        }
    }

    /**
     * Verify that no board data was processed
     */
    public void verifyNoBoardDataProcessed(String uen, String noticeNo, TestStepResult result) {
        try {
            String dbQuery = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_acra_board_info " +
                    "WHERE entity_uen = ? AND notice_no = ?";

            Integer boardCount = jdbcTemplate.queryForObject(dbQuery, Integer.class, uen, noticeNo);

            if (boardCount != null && boardCount == 0) {
                result.addDetail("✅ CONFIRMED: No board records processed (Expected: 0, Actual: " + boardCount + ")");
            } else {
                result.addDetail("❌ ERROR: Found unexpected board records (Expected: 0, Actual: " + boardCount + ")");
            }

        } catch (Exception e) {
            result.addDetail("❌ BOARD VERIFICATION ERROR: " + e.getMessage());
        }
    }

    /**
     * Verify address data was created
     */
    public void verifyAddressData(String noticeNo, TestStepResult result) {
        try {
            String dbQuery = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                    "WHERE notice_no = ? AND owner_driver_indicator = 'O' AND type_of_address = 'mha_reg'";

            Integer addressCount = jdbcTemplate.queryForObject(dbQuery, Integer.class, noticeNo);

            if (addressCount != null && addressCount > 0) {
                result.addDetail("✅ Address record found: " + addressCount + " records");
            } else {
                result.addDetail("❌ ERROR: No address record found for notice: " + noticeNo);
            }

        } catch (Exception e) {
            result.addDetail("❌ ADDRESS VERIFICATION ERROR: " + e.getMessage());
        }
    }
}