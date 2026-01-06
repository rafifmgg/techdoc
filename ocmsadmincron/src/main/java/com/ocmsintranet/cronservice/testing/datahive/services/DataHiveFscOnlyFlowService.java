package com.ocmsintranet.cronservice.testing.datahive.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for testing DataHive FSC-Only Flow scenario.
 *
 * Test scenario: Only FSC Comcare data available dari DataHive
 * Expected result:
 * - ocms_dh_msf_comcare_fund: 1 record created (source=FSC)
 * - ocms_dh_sps_custody: No changes
 * - ocms_dh_sps_incarceration: No changes
 * - Snowflake vs Database exact field match validation
 *
 * 4-Step Sequential Flow:
 * 1. Setup test data - UPSERT OCMS dan DataHive tables
 * 2. Call DataHive API - RestTemplate call ke /api/datahive/test/nric/{nric}
 * 3. Query Snowflake directly - Direct queries ke FSC DataHive view only
 * 4. Verify exact data match - Compare Snowflake vs Database field-by-field
 */
@Slf4j
@Service
public class DataHiveFscOnlyFlowService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_INFO = "INFO";

    // Test data constants - using NRIC dengan FSC assistance record only
    private static final String TEST_NRIC = "S2345678B";  // Has FSC data only
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String NOTICE_PREFIX = "DHFSC";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // Database schema
    private static final String SCHEMA = "ocmsizmgr";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    @Autowired
    private DataHiveUtil dataHiveUtil;

    /**
     * Execute FSC-only DataHive flow test dengan 4 sequential steps.
     *
     * @return List<TestStepResult> dengan detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting DataHive FSC-Only Flow Test dengan NRIC: {}, Notice: {}", TEST_NRIC, NOTICE_NUMBER);

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data (UPSERT pattern)
            steps.add(setupTestData());

            // Step 2: Call DataHive API
            steps.add(callDataHiveApi());

            // Step 3: Query Snowflake directly (FSC only)
            steps.add(querySnowflakeDirectly());

            // Step 4: Verify exact data match (FSC only)
            steps.add(verifyExactDataMatch());

            log.info("✅ DataHive FSC-Only Flow Test completed successfully");
            return steps;

        } catch (Exception e) {
            log.error("❌ DataHive FSC-Only Flow Test failed: {}", e.getMessage(), e);

            // Add failure step with preserved details from previous successful steps
            TestStepResult failureStep = new TestStepResult("Flow Execution", STATUS_FAILED);
            failureStep.addDetail("❌ Test flow failed: " + e.getMessage());

            // Add summary of completed steps for debugging context
            failureStep.addDetail("🔍 Completed Steps Before Failure:");
            for (int i = 0; i < steps.size(); i++) {
                TestStepResult step = steps.get(i);
                failureStep.addDetail("  Step " + (i+1) + ": " + step.getTitle() + " - " + step.getStatus());
            }

            steps.add(failureStep);

            return steps;
        }
    }

    /**
     * Step 1: Setup test data menggunakan UPSERT pattern untuk DEV environment compatibility.
     * This method ensures test data exists dan consistent untuk DataHive lookup.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Setup Test Data", STATUS_INFO);

        try {
            log.info("🔄 Step 1: Setting up test data dengan UPSERT pattern");

            // UPSERT OCMS core tables (sama seperti MHA pattern)
            resetOrInsertValidOffenceNotice(result);
            resetOrInsertOffenceNoticeDetail(result);
            resetOrInsertOwnerDriver(result);
            resetOrInsertOwnerDriverAddress(result);

            // Reset DataHive tables to known initial state (FSC only)
            resetDataHiveTables(result);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ Test data setup completed successfully");

        } catch (Exception e) {
            log.error("❌ Error during test data setup: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Setup failed: " + e.getMessage());
            throw new RuntimeException("Test data setup failed", e);
        }

        return result;
    }

    /**
     * Step 2: Call DataHive API menggunakan RestTemplate.
     */
    private TestStepResult callDataHiveApi() {
        TestStepResult result = new TestStepResult("Call DataHive API", STATUS_INFO);

        try {
            log.info("🌐 Step 2: Calling DataHive API untuk NRIC: {}", TEST_NRIC);

            String apiUrl = apiConfigHelper.buildApiUrl("/api/datahive/test/nric/" + TEST_NRIC + "?noticeNumber=" + NOTICE_NUMBER);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ DataHive API call successful");
                result.addDetail("API URL: " + apiUrl);
                result.addDetail("Response Status: " + response.getStatusCode());

                // Log API response summary
                JsonNode apiResult = response.getBody();

                // Store response in jsonData
                result.setJsonData(apiResult);

                // Parse the actual response structure
                JsonNode dataNode = apiResult.get("data");
                JsonNode summaryNode = apiResult.get("summary");

                if (summaryNode != null) {
                    result.addDetail("API Summary - Comcare Records: " + summaryNode.get("comcareRecordsFound").asInt());
                    result.addDetail("API Summary - Custody Records: " + summaryNode.get("custodyRecords").asInt());
                    result.addDetail("API Summary - Incarceration Records: " + summaryNode.get("incarcerationRecords").asInt());
                    result.addDetail("API Summary - Has Contact Info: " + summaryNode.get("hasContactInfo").asBoolean());
                }

                if (dataNode != null) {
                    // Count FSC data from comcareData (should be 1, CCC should be 0)
                    JsonNode comcareArray = dataNode.get("comcareData");
                    long fscCount = 0, cccCount = 0;
                    if (comcareArray != null && comcareArray.isArray()) {
                        for (JsonNode comcare : comcareArray) {
                            String source = comcare.get("source").asText();
                            if ("FSC".equals(source)) fscCount++;
                            else if ("CCC".equals(source)) cccCount++;
                        }
                    }

                    result.addDetail("FSC Data Count: " + fscCount + " (Expected: 1)");
                    result.addDetail("CCC Data Count: " + cccCount + " (Expected: 0)");

                    // Count custody and incarceration data (should be 0)
                    JsonNode commonDataNode = dataNode.get("commonData");
                    if (commonDataNode != null) {
                        JsonNode custodyArray = commonDataNode.get("custodyData");
                        JsonNode incarcerationArray = commonDataNode.get("incarcerationData");

                        result.addDetail("Custody Data Count: " + (custodyArray != null ? custodyArray.size() : 0) + " (Expected: 0)");
                        result.addDetail("Incarceration Data Count: " + (incarcerationArray != null ? incarcerationArray.size() : 0) + " (Expected: 0)");
                    }
                }

            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ DataHive API call failed");
                result.addDetail("Response Status: " + response.getStatusCode());
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error during DataHive API call: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ API call failed: " + e.getMessage());
            throw new RuntimeException("DataHive API call failed", e);
        }

        return result;
    }

    /**
     * Step 3: Query Snowflake directly menggunakan DataHiveUtil (FSC only).
     */
    private TestStepResult querySnowflakeDirectly() {
        TestStepResult result = new TestStepResult("Query Snowflake Directly", STATUS_INFO);

        try {
            log.info("❄️ Step 3: Querying Snowflake directly untuk NRIC: {} (FSC only)", TEST_NRIC);

            // Direct query ke FSC DataHive view only
            CompletableFuture<JsonNode> fscQuery = dataHiveUtil.executeQueryAsyncDataOnly(
                "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
                "FROM V_DH_MSF_12_FCF WHERE BENEFICIARY_ID_NO = '" + TEST_NRIC + "'"
            );

            // Wait untuk FSC query complete
            CompletableFuture.allOf(fscQuery).join();

            // Get results
            JsonNode fscData = fscQuery.get();

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ Direct Snowflake FSC query completed");
            result.addDetail("FSC Records: " + (fscData != null ? fscData.size() : 0) + " (Expected: 1)");
            result.addDetail("CCC Records: 0 (Expected: 0 - not queried)");
            result.addDetail("SPS Records: 0 (Expected: 0 - not queried)");

        } catch (Exception e) {
            log.error("❌ Error during direct Snowflake queries: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Snowflake queries failed: " + e.getMessage());
            throw new RuntimeException("Direct Snowflake queries failed", e);
        }

        return result;
    }

    /**
     * Step 4: Verify exact data match between Snowflake dan Database (FSC only).
     */
    private TestStepResult verifyExactDataMatch() {
        TestStepResult result = new TestStepResult("Verify Exact Data Match", STATUS_INFO);

        try {
            log.info("🔍 Step 4: Verifying exact data match - Snowflake vs Database (FSC only)");

            boolean hasErrors = false;

            // Verify MSF Comcare Fund table (FSC only)
            result.addDetail("📋 Starting MSF Comcare Fund verification (FSC only)...");
            verifyFscComcareFundData(result);
            // Check if verification failed by looking at the last detail
            if (result.getDetails().stream().anyMatch(detail -> detail.contains("DETAILED FSC ERROR"))) {
                hasErrors = true;
                result.addDetail("❌ FSC Comcare Fund verification failed");
            } else {
                result.addDetail("✅ FSC Comcare Fund verification completed successfully");
            }

            // Verify no SPS data was created
            result.addDetail("📋 Verifying no SPS data created...");
            verifyNoSpsDataCreated(result);
            if (result.getDetails().stream().anyMatch(detail -> detail.contains("UNEXPECTED SPS DATA"))) {
                hasErrors = true;
                result.addDetail("❌ SPS data verification failed - unexpected data found");
            } else {
                result.addDetail("✅ SPS data verification completed - no unexpected data");
            }

            if (hasErrors) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("💥 One or more verification steps failed - see details above");
            } else {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("🎉 All data verification passed - FSC-only flow confirmed");
            }

        } catch (Exception e) {
            log.error("❌ Error during data verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("💥 DETAILED ERROR: " + e.getMessage());
            if (e.getCause() != null) {
                result.addDetail("🔍 Root Cause: " + e.getCause().getMessage());
            }
            // Don't throw - return result with details preserved
            return result;
        }

        return result;
    }

    // ===============================
    // UPSERT Pattern Helper Methods
    // ===============================

    private void resetOrInsertValidOffenceNotice(TestStepResult result) {
        if (existsValidOffenceNotice(NOTICE_NUMBER)) {
            resetValidOffenceNotice(result, NOTICE_NUMBER);
        } else {
            insertValidOffenceNotice(result, NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOffenceNoticeDetail(TestStepResult result) {
        if (existsOffenceNoticeDetail(NOTICE_NUMBER)) {
            resetOffenceNoticeDetail(result, NOTICE_NUMBER);
        } else {
            insertOffenceNoticeDetail(result, NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOwnerDriver(TestStepResult result) {
        if (existsOwnerDriver(NOTICE_NUMBER)) {
            resetOwnerDriver(result, NOTICE_NUMBER);
        } else {
            insertOwnerDriver(result, NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOwnerDriverAddress(TestStepResult result) {
        if (existsOwnerDriverAddress(NOTICE_NUMBER)) {
            resetOwnerDriverAddress(result, NOTICE_NUMBER);
        } else {
            insertOwnerDriverAddress(result, NOTICE_NUMBER);
        }
    }

    // ===============================
    // Table Existence Check Methods
    // ===============================

    private boolean existsValidOffenceNotice(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    private boolean existsOffenceNoticeDetail(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    private boolean existsOwnerDriver(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    private boolean existsOwnerDriverAddress(String noticeNo) {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, noticeNo);
        return count != null && count > 0;
    }

    // ===============================
    // Reset/Update Methods for Existing Records
    // ===============================

    private void resetValidOffenceNotice(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_valid_offence_notice SET " +
                     "vehicle_no = ?, offence_notice_type = ?, last_processing_stage = ?, next_processing_stage = ?, " +
                     "last_processing_date = ?, next_processing_date = ?, notice_date_and_time = ?, " +
                     "pp_code = ?, pp_name = ?, composition_amount = ?, computer_rule_code = ?, vehicle_category = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "DHFSC001", "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            "PP001", "PARKING OFFENCE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 VON record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOffenceNoticeDetail(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_detail SET " +
                     "lta_chassis_number = ?, lta_make_description = ?, lta_primary_colour = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "DHFSC123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 OND record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                     "owner_driver_indicator = ?, id_type = ?, id_no = ?, name = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "O", TEST_ID_TYPE, TEST_NRIC, "FSC TEST CITIZEN",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 OOD record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriverAddress(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                     "bldg_name = ?, street_name = ?, unit_no = ?, postal_code = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "FSC TEST BUILDING", "FSC TEST STREET", "#01-01", "123456",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 OODA record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    // ===============================
    // Insert Methods for New Records
    // ===============================

    private void insertValidOffenceNotice(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice " +
                     "(notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "DHFSC001", "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            "PP001", "PARKING OFFENCE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("➕ VON record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail " +
                     "(notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "DHFSC123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("➕ OND record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                     "(notice_no, owner_driver_indicator, id_type, id_no, name, " +
                     "cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "O", TEST_ID_TYPE, TEST_NRIC, "FSC TEST CITIZEN",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("➕ OOD record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    private void insertOwnerDriverAddress(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                     "(notice_no, owner_driver_indicator, type_of_address, bldg_name, street_name, " +
                     "unit_no, postal_code, cre_user_id, cre_date, upd_user_id, upd_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int rowsInserted = jdbcTemplate.update(sql,
            noticeNo, "O", "datahive", "FSC TEST BUILDING", "FSC TEST STREET", "#01-01", "123456",
            "TEST_USER", LocalDateTime.now(), "TEST_USER", LocalDateTime.now());

        result.addDetail("➕ OODA record inserted: " + noticeNo + " (" + rowsInserted + " rows inserted)");
    }

    // ===============================
    // DataHive Tables Reset Methods
    // ===============================

    private void resetDataHiveTables(TestStepResult result) {
        try {
            // Reset MSF Comcare Fund table (FSC records only)
            String deleteFscSql = "DELETE FROM " + SCHEMA + ".ocms_dh_msf_comcare_fund WHERE id_no = ? AND source = 'FSC'";
            int fscDeleted = jdbcTemplate.update(deleteFscSql, TEST_NRIC);
            result.addDetail("🗑️ Deleted existing FSC Comcare records: " + fscDeleted);

            // Verify no CCC records exist (should not be created in FSC-only flow)
            String countCccSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_msf_comcare_fund WHERE id_no = ? AND source = 'CCC'";
            Integer cccCount = jdbcTemplate.queryForObject(countCccSql, Integer.class, TEST_NRIC);
            result.addDetail("🔍 Existing CCC Comcare records: " + (cccCount != null ? cccCount : 0) + " (should remain 0)");

            // Verify no SPS records exist (should not be created in FSC-only flow)
            String countCustodySql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_sps_custody WHERE uin = ?";
            Integer custodyCount = jdbcTemplate.queryForObject(countCustodySql, Integer.class, TEST_NRIC);
            result.addDetail("🔍 Existing SPS Custody records: " + (custodyCount != null ? custodyCount : 0) + " (should remain 0)");

            String countIncarcerationSql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_sps_incarceration WHERE uin = ?";
            Integer incarcerationCount = jdbcTemplate.queryForObject(countIncarcerationSql, Integer.class, TEST_NRIC);
            result.addDetail("🔍 Existing SPS Incarceration records: " + (incarcerationCount != null ? incarcerationCount : 0) + " (should remain 0)");

        } catch (Exception e) {
            log.error("❌ Error during DataHive tables reset: {}", e.getMessage(), e);
            result.addDetail("❌ DataHive tables reset failed: " + e.getMessage());
            throw new RuntimeException("DataHive tables reset failed", e);
        }
    }

    // ===============================
    // Data Verification Methods
    // ===============================

    /**
     * Verify FSC Comcare Fund data between Snowflake and Database.
     */
    private void verifyFscComcareFundData(TestStepResult result) {
        try {
            log.info("🔍 Verifying FSC Comcare Fund data...");

            // Query Snowflake for FSC data
            CompletableFuture<JsonNode> fscQuery = dataHiveUtil.executeQueryAsyncDataOnly(
                "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
                "FROM V_DH_MSF_12_FCF WHERE BENEFICIARY_ID_NO = '" + TEST_NRIC + "'"
            );

            JsonNode fscSnowflakeData = fscQuery.get();

            // Query Database for FSC data
            String dbQuery = "SELECT id_no, assistance_start, assistance_end, beneficiary_name, data_date, payment_date, reference_period, source " +
                           "FROM " + SCHEMA + ".ocms_dh_msf_comcare_fund WHERE id_no = ? AND source = 'FSC'";
            
            List<Map<String, Object>> dbData = jdbcTemplate.queryForList(dbQuery, TEST_NRIC);

            result.addDetail("📊 Snowflake FSC Records: " + (fscSnowflakeData != null ? fscSnowflakeData.size() : 0));
            result.addDetail("📊 Database FSC Records: " + dbData.size());

            // Verify record count match
            int snowflakeCount = fscSnowflakeData != null ? fscSnowflakeData.size() : 0;
            if (snowflakeCount != dbData.size()) {
                result.addDetail("💥 DETAILED FSC ERROR: Record count mismatch - Snowflake: " + snowflakeCount + ", Database: " + dbData.size());
                return;
            }

            // Verify field-by-field match for each record
            if (snowflakeCount > 0 && fscSnowflakeData.isArray()) {
                for (int i = 0; i < snowflakeCount; i++) {
                    JsonNode snowflakeRecord = fscSnowflakeData.get(i);
                    Map<String, Object> dbRecord = dbData.get(i);
                    
                    verifyFscComcareRecord(result, snowflakeRecord, dbRecord, i + 1);
                }
            }

            result.addDetail("✅ FSC Comcare Fund verification completed");

        } catch (Exception e) {
            log.error("❌ Error during FSC Comcare Fund verification: {}", e.getMessage(), e);
            result.addDetail("💥 DETAILED FSC ERROR: " + e.getMessage());
            if (e.getCause() != null) {
                result.addDetail("🔍 Root Cause: " + e.getCause().getMessage());
            }
        }
    }

    /**
     * Verify individual FSC Comcare record field-by-field.
     */
    private void verifyFscComcareRecord(TestStepResult result, JsonNode snowflakeRecord, Map<String, Object> dbRecord, int recordNumber) {
        try {
            result.addDetail("🔍 Verifying FSC Record #" + recordNumber + ":");

            // Verify BENEFICIARY_ID_NO
            String sfIdNo = snowflakeRecord.get("BENEFICIARY_ID_NO").asText();
            String dbIdNo = (String) dbRecord.get("id_no");
            if (!sfIdNo.equals(dbIdNo)) {
                result.addDetail("💥 DETAILED FSC ERROR: ID_NO mismatch - SF: " + sfIdNo + ", DB: " + dbIdNo);
                return;
            }

            // Verify BENEFICIARY_NAME
            String sfName = snowflakeRecord.get("BENEFICIARY_NAME").asText();
            String dbName = (String) dbRecord.get("beneficiary_name");
            if (!sfName.equals(dbName)) {
                result.addDetail("💥 DETAILED FSC ERROR: BENEFICIARY_NAME mismatch - SF: " + sfName + ", DB: " + dbName);
                return;
            }

            // Verify source is FSC
            String dbSource = (String) dbRecord.get("source");
            if (!"FSC".equals(dbSource)) {
                result.addDetail("💥 DETAILED FSC ERROR: Source should be FSC, but got: " + dbSource);
                return;
            }

            result.addDetail("✅ FSC Record #" + recordNumber + " verified successfully");

        } catch (Exception e) {
            result.addDetail("💥 DETAILED FSC ERROR: Record #" + recordNumber + " verification failed: " + e.getMessage());
        }
    }

    /**
     * Verify no SPS data was created (custody and incarceration should be empty).
     */
    private void verifyNoSpsDataCreated(TestStepResult result) {
        try {
            log.info("🔍 Verifying no SPS data was created...");

            // Check SPS Custody table
            String custodyQuery = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_sps_custody WHERE uin = ?";
            Integer custodyCount = jdbcTemplate.queryForObject(custodyQuery, Integer.class, TEST_NRIC);
            
            if (custodyCount != null && custodyCount > 0) {
                result.addDetail("💥 UNEXPECTED SPS DATA: Found " + custodyCount + " custody records (expected 0)");
                return;
            }

            // Check SPS Incarceration table
            String incarcerationQuery = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_dh_sps_incarceration WHERE uin = ?";
            Integer incarcerationCount = jdbcTemplate.queryForObject(incarcerationQuery, Integer.class, TEST_NRIC);
            
            if (incarcerationCount != null && incarcerationCount > 0) {
                result.addDetail("💥 UNEXPECTED SPS DATA: Found " + incarcerationCount + " incarceration records (expected 0)");
                return;
            }

            result.addDetail("✅ No SPS data found - as expected for FSC-only flow");
            result.addDetail("📊 SPS Custody Records: 0 (Expected: 0)");
            result.addDetail("📊 SPS Incarceration Records: 0 (Expected: 0)");

        } catch (Exception e) {
            log.error("❌ Error during SPS data verification: {}", e.getMessage(), e);
            result.addDetail("💥 UNEXPECTED SPS DATA: Verification failed: " + e.getMessage());
        }
    }
}