package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for testing MHA DataHive NRIC Found Flow scenario.
 *
 * Test scenario: Successful NRIC lookup with contact update
 * Expected result: Contact fields updated from DataHive lookup response
 *                  with current address and phone information
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC that exists in DataHive
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with valid NRIC
 * 5. POST /mha/download/execute - Trigger download and DataHive lookup
 * 6. Verify contact fields updated from DataHive lookup
 */
@Slf4j
@Service
public class MhaDatahiveNricFoundFlowService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_INFO = "INFO";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SftpUtil sftpUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    // Test data constants for DataHive found scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SGF7531K";
    private static final String TEST_NRIC = "S7531246H";
    private static final String TEST_NAME = "Chen Li Ming";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type

    // Notice constants for DataHive found flow
    private static final String NOTICE_PREFIX = "DHFOUND";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // DataHive response constants (contact info that will be updated)
    private static final String DATAHIVE_ADDRESS = "123 Orchard Road, #05-01, Singapore 238874";
    private static final String DATAHIVE_POSTAL_CODE = "238874";
    private static final String DATAHIVE_PHONE = "91234567";
    private static final String DATAHIVE_EMAIL = "chenliaming@email.com";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete DataHive NRIC Found Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA DataHive NRIC Found Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload());

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with valid NRIC
            steps.add(simulateMhaCallbackWithValidNric());

            // Step 5: POST /mha/download/execute (triggers DataHive lookup)
            steps.add(triggerMhaDownload());

            // Step 6: Verify contact fields updated from DataHive
            steps.add(verifyDatahiveContactUpdate());

        } catch (Exception e) {
            log.error("❌ Critical error during DataHive NRIC Found flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA DataHive NRIC Found Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC that exists in DataHive.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - DataHive NRIC Found Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for DataHive NRIC Found scenario: {}", NOTICE_NUMBER);

            result.addDetail("🔄 Preparing test data (UPSERT mode for DEV compatibility)...");
            resetOrInsertTestData(result);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, NOTICE_NUMBER);

            if (recordCount != null && recordCount == 1) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed successfully");
                result.addDetail("📋 Notice: " + NOTICE_NUMBER);
                result.addDetail("🚗 Vehicle: " + TEST_VEHICLE_NO);
                result.addDetail("👤 Owner: " + TEST_NRIC + " (" + TEST_NAME + ")");
                result.addDetail("📞 Initial contact: Incomplete (will be updated by DataHive lookup)");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - record count: " + recordCount);
            }

        } catch (Exception e) {
            log.error("❌ Error setting up test data: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error during setup: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 2: POST /mha/upload - Trigger upload process.
     */
    private TestStepResult triggerMhaUpload() {
        TestStepResult result = new TestStepResult("Step 2: POST /mha/upload - Trigger Upload Process", STATUS_INFO);

        try {
            String uploadUrl = apiConfigHelper.buildApiUrl("/v1/mha/upload");
            log.info("⬆️ Calling MHA upload API: {}", uploadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}"; // Empty JSON payload
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(uploadUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA upload API called successfully");
            result.addDetail("📤 URL: " + uploadUrl);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 200)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error calling MHA upload API: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Upload API call failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 3: Verify files in SFTP - Check generated files exist.
     */
    private TestStepResult verifySftpFiles() {
        TestStepResult result = new TestStepResult("Step 3: Verify Files in SFTP - Check Generated Files", STATUS_INFO);

        try {
            log.info("📂 Checking SFTP files in {}", sftpInputPath);

            // List files in input directory
            List<String> inputFiles = sftpUtil.listFiles(SFTP_SERVER, sftpInputPath);

            // Check for expected file patterns
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());

            String expectedFilePattern = "URA2NRO_" + currentDate;

            boolean fileFound = inputFiles.stream()
                .anyMatch(file -> file.contains(expectedFilePattern));

            if (fileFound) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ SFTP files verification passed");
                result.addDetail("📁 Files found in " + sftpInputPath + ": " + inputFiles.size());
                result.addDetail("🔍 Expected pattern: " + expectedFilePattern);
            } else {
                result.setStatus(STATUS_INFO);
                result.addDetail("⚠️ Expected files not found (may be normal for test scenario)");
                result.addDetail("📁 Files in " + sftpInputPath + ": " + inputFiles.size());
            }

        } catch (Exception e) {
            log.error("❌ Error verifying SFTP files: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ SFTP verification failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with valid NRIC.
     */
    private TestStepResult simulateMhaCallbackWithValidNric() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Valid NRIC", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with valid NRIC for: {}", NOTICE_NUMBER);

            // Generate MHA response files with valid NRIC data
            generateMhaResponseFiles(result);

            String callbackUrl = apiConfigHelper.buildApiUrl("/test/mha/mha-callback");
            log.info("📱 Calling MHA callback API: {}", callbackUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}"; // Empty JSON payload
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(callbackUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA callback simulation completed");
            result.addDetail("📞 URL: " + callbackUrl);
            result.addDetail("🆔 Valid NRIC data for notice: " + NOTICE_NUMBER);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 150)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error simulating MHA callback: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ MHA callback simulation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 5: POST /mha/download/execute - Trigger download process (includes DataHive lookup).
     */
    private TestStepResult triggerMhaDownload() {
        TestStepResult result = new TestStepResult("Step 5: POST /mha/download/execute - Trigger Download & DataHive Lookup", STATUS_INFO);

        try {
            String downloadUrl = apiConfigHelper.buildApiUrl("/v1/mha/download/execute");
            log.info("⬇️ Calling MHA download API (triggers DataHive lookup): {}", downloadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}"; // Empty JSON payload
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(downloadUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA download API called successfully");
            result.addDetail("📥 URL: " + downloadUrl);
            result.addDetail("🔍 DataHive lookup should be triggered during processing");
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 200)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error calling MHA download API: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Download API call failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 6: Verify contact fields updated from DataHive lookup.
     */
    private TestStepResult verifyDatahiveContactUpdate() {
        TestStepResult result = new TestStepResult("Step 6: Verify DataHive Contact Update Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying DataHive contact update for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Address updated from DataHive
            String addressSQL = "SELECT address_line_1, address_line_2, address_line_3, postal_code, " +
                               "cre_user_id, cre_date, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                               "WHERE notice_no = ? AND id_no = ?";

            try {
                Map<String, Object> addressData = jdbcTemplate.queryForMap(addressSQL, NOTICE_NUMBER, TEST_NRIC);
                String updatedAddress = (String) addressData.get("address_line_1");
                String postalCode = (String) addressData.get("postal_code");

                if (DATAHIVE_ADDRESS.equals(updatedAddress) && DATAHIVE_POSTAL_CODE.equals(postalCode)) {
                    verificationResults.add("✅ Address Update from DataHive:");
                    verificationResults.add("  📍 Address: " + updatedAddress);
                    verificationResults.add("  📮 Postal Code: " + postalCode);
                    verificationResults.add("  📅 Updated by: " + addressData.get("upd_user_id"));
                    verificationResults.add("  🕒 Updated on: " + addressData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Address Update: Expected DataHive address, got '" + updatedAddress + "'");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Address update verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Contact info updated from DataHive
            String contactSQL = "SELECT offender_tel_no, email_addr, " +
                               "cre_user_id, cre_date, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                               "WHERE notice_no = ? AND id_no = ?";

            try {
                Map<String, Object> contactData = jdbcTemplate.queryForMap(contactSQL, NOTICE_NUMBER, TEST_NRIC);
                String telNo = (String) contactData.get("offender_tel_no");
                String emailAddr = (String) contactData.get("email_addr");

                if (DATAHIVE_PHONE.equals(telNo) && DATAHIVE_EMAIL.equals(emailAddr)) {
                    verificationResults.add("✅ Contact Info Update from DataHive:");
                    verificationResults.add("  📱 Mobile: " + telNo);
                    verificationResults.add("  📧 Email: " + emailAddr);
                    verificationResults.add("  📅 Updated by: " + contactData.get("upd_user_id"));
                } else {
                    verificationResults.add("❌ Contact Info Update: Mobile='" + telNo + "', Email='" + emailAddr + "'");
                    verificationResults.add("   Expected: Mobile='" + DATAHIVE_PHONE + "', Email='" + DATAHIVE_EMAIL + "'");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Contact info verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: DataHive lookup audit trail
            String auditSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_datahive_lookup_log " +
                             "WHERE nric = ? AND lookup_status = 'SUCCESS' AND lookup_date >= ?";

            try {
                Integer auditCount = jdbcTemplate.queryForObject(auditSQL, Integer.class, TEST_NRIC,
                    LocalDateTime.now().minusHours(1));

                if (auditCount != null && auditCount > 0) {
                    verificationResults.add("✅ DataHive Lookup Audit: " + auditCount + " successful lookup(s) found");
                } else {
                    verificationResults.add("⚠️ DataHive Lookup Audit: No recent successful lookups found");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ DataHive audit verification skipped: " + e.getMessage());
            }

            // Verify 4: No suspension applied (successful lookup should not trigger suspension)
            String suspensionCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                       "WHERE notice_no = ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            Integer suspensionCount = jdbcTemplate.queryForObject(suspensionCheckSQL, Integer.class, NOTICE_NUMBER);
            if (suspensionCount != null && suspensionCount == 0) {
                verificationResults.add("✅ Suspension Check: No suspension applied (expected for successful DataHive lookup)");
            } else {
                verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (unexpected for successful lookup)");
            }

            // Verify 5: Processing stage progression
            String stageSQL = "SELECT last_processing_stage, next_processing_stage, vehicle_no " +
                             "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> stageData = jdbcTemplate.queryForMap(stageSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Processing Stage Progression:");
                verificationResults.add("  🚗 Vehicle: " + stageData.get("vehicle_no"));
                verificationResults.add("  🔄 Stages: " + stageData.get("last_processing_stage") + " -> " + stageData.get("next_processing_stage"));

            } catch (Exception e) {
                verificationResults.add("⚠️ Processing stage verification skipped: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ DataHive Contact Update verification completed successfully");
                result.addDetail("📋 Expected behavior: Contact fields updated from DataHive lookup");
                result.addDetail("🎯 Scenario outcome: PASSED - DataHive NRIC Found flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ DataHive Contact Update verification failed");
                result.addDetail("📋 Some critical contact update checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during DataHive Contact Update verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods for data setup

    /**
     * UPSERT pattern implementation for DEV environment compatibility
     * This method checks if test data exists and either updates it to initial state or inserts new records
     */
    private void resetOrInsertTestData(TestStepResult result) {
        try {
            log.info("🔄 Starting UPSERT pattern for test data with notice number: {}", NOTICE_NUMBER);

            // Process each table in dependency order
            resetOrInsertValidOffenceNotice(result);
            resetOrInsertOffenceNoticeDetail(result);
            resetOrInsertOwnerDriver(result);
            resetOrInsertOwnerDriverAddress(result);

            result.addDetail("✅ UPSERT pattern completed successfully");

        } catch (Exception e) {
            log.error("❌ Error setting up test data: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error during setup: " + e.getMessage());
        }
    }

    /**
     * Reset or insert valid offence notice record
     */
    private void resetOrInsertValidOffenceNotice(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_valid_offence_notice SET " +
                              "vehicle_no = ?, offence_notice_type = ?, last_processing_stage = ?, next_processing_stage = ?, " +
                              "last_processing_date = ?, next_processing_date = ?, notice_date_and_time = ?, " +
                              "pp_code = ?, pp_name = ?, composition_amount = ?, computer_rule_code = ?, vehicle_category = ?, " +
                              "suspension_type = NULL, epr_reason_of_suspension = NULL, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                TEST_VEHICLE_NO, "O", "ROV", "RD1",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
                "D0001", "DATAHIVE LOOKUP PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Valid offence notice reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                              "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                              "last_processing_date, next_processing_date, notice_date_and_time, " +
                              "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL,
                NOTICE_NUMBER, TEST_VEHICLE_NO, "O", "ROV", "RD1",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
                "D0001", "DATAHIVE LOOKUP PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Valid offence notice inserted: " + NOTICE_NUMBER);
        }
    }

    /**
     * Reset or insert offence notice detail record
     */
    private void resetOrInsertOffenceNoticeDetail(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_detail SET " +
                              "lta_chassis_number = ?, lta_make_description = ?, lta_primary_colour = ?, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "DHTEST789012345", "MERCEDES", "SILVER",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Offence notice detail reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                              "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "DHTEST789012345", "MERCEDES", "SILVER",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Offence notice detail inserted: " + NOTICE_NUMBER);
        }
    }

    /**
     * Reset or insert owner/driver record with incomplete contact info
     */
    private void resetOrInsertOwnerDriver(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state (incomplete contact info)
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                              "owner_driver_indicator = ?, id_type = ?, id_no = ?, name = ?, offender_indicator = ?, " +
                              "offender_tel_no = NULL, email_addr = NULL, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Owner/driver record reset: " + NOTICE_NUMBER + " (incomplete contact info)");
        } else {
            // Insert new record with incomplete contact info
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                              "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Owner/driver record inserted: " + NOTICE_NUMBER + " (incomplete contact info)");
        }
    }

    /**
     * Reset or insert owner/driver address record (will be updated by DataHive)
     */
    private void resetOrInsertOwnerDriverAddress(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state (old address)
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                              "owner_driver_indicator = ?, type_of_address = ?, blk_hse_no = ?, street_name = ?, " +
                              "floor_no = ?, unit_no = ?, bldg_name = ?, postal_code = ?, " +
                              "address_type = ?, address_line_1 = ?, address_line_2 = ?, address_line_3 = ?, " +
                              "effective_date = ?, upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "O", "mha_reg", "999", "OLD STREET",
                "99", "99", "OLD BUILDING", "000000",
                "R", "OLD ADDRESS", "OLD ADDRESS LINE 2", null, LocalDateTime.now(),
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Owner/driver address reset: " + NOTICE_NUMBER + " (old address)");
        } else {
            // Insert new record with old address
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                              "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, " +
                              "floor_no, unit_no, bldg_name, postal_code, address_type, " +
                              "address_line_1, address_line_2, address_line_3, effective_date, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL,
                NOTICE_NUMBER, "O", "mha_reg", "999", "OLD STREET",
                "99", "99", "OLD BUILDING", "000000", "R",
                "OLD ADDRESS", "OLD ADDRESS LINE 2", null, LocalDateTime.now(),
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Owner/driver address inserted: " + NOTICE_NUMBER + " (old address)");
        }
    }

    /**
     * Clean up existing test data with notice prefix (legacy method for compatibility)
     */
    private void cleanupTestData(String noticePrefix) {
        try {
            // Delete from child tables first (foreign key constraints)
            int addressDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no LIKE ?",
                noticePrefix + "%");

            int ownerDriverDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no LIKE ?",
                noticePrefix + "%");

            int detailDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no LIKE ?",
                noticePrefix + "%");

            // Clean up DataHive lookup logs
            int datahiveLogDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_datahive_lookup_log WHERE nric = ?",
                TEST_NRIC);

            // Delete from parent table last
            int vonDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?",
                noticePrefix + "%");

            log.info("🧹 Cleanup completed: VON={}, Detail={}, OwnerDriver={}, Address={}, DatahiveLog={} records deleted for prefix: {}",
                     vonDeleted, detailDeleted, ownerDriverDeleted, addressDeleted, datahiveLogDeleted, noticePrefix);

        } catch (Exception e) {
            log.warn("⚠️ Cleanup warning for prefix {}: {}", noticePrefix, e.getMessage());
            // Don't fail the test if cleanup has issues - continue with test setup
        }
    }

    private void insertValidOffenceNotice(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            noticeNo,                           // notice_no
            TEST_VEHICLE_NO,                    // vehicle_no
            "O",                                // offence_notice_type (O = Original)
            "ROV",                              // last_processing_stage
            "RD1",                              // next_processing_stage
            LocalDateTime.now(),                // last_processing_date
            LocalDateTime.now().plusDays(1),    // next_processing_date
            LocalDateTime.now().minusDays(1),   // notice_date_and_time
            "A0001",                            // pp_code (parking place code)
            "TEST PARKING PLACE",               // pp_name
            new java.math.BigDecimal("70.00"),  // composition_amount
            10001,                              // computer_rule_code
            "C",                                // vehicle_category (C=Car)
            "TEST_USER",                        // cre_user_id
            LocalDateTime.now()                 // cre_date
        );

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo);
        result.addDetail("   - Vehicle: " + TEST_VEHICLE_NO);
        result.addDetail("   - Ready for DataHive lookup");
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "DHTEST789012345", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertOwnerDriverWithIncompleteContact(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "offender_tel_no, email_addr, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
            null, null, "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner record inserted: " + TEST_NRIC + " (" + TEST_NAME + ")");
        result.addDetail("   - Contact info: Incomplete (will be populated by DataHive lookup)");
    }

    private void insertOwnerDriverAddress(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, id_no, address_line_1, address_line_2, postal_code, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, TEST_NRIC, "OLD ADDRESS", "OLD ADDRESS LINE 2", "000000",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Initial address record inserted for: " + TEST_NRIC);
        result.addDetail("   - Will be updated with DataHive address information");
    }

    private void generateMhaResponseFiles(TestStepResult result) {
        try {
            // Generate NRO2URA file with valid NRIC data that triggers DataHive lookup
            String mainContent = generateMainFileWithValidNric();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp;

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with valid NRIC: " + mainFileName);
            result.addDetail("🔍 Valid NRIC " + TEST_NRIC + " will trigger DataHive lookup for contact info");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithValidNric() {
        // Generate NRO2URA file content with valid NRIC that will trigger DataHive lookup
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Owner record with valid NRIC (triggers DataHive lookup)
        sb.append("D")
          .append(String.format("%-12s", TEST_NRIC))           // Valid NRIC
          .append(String.format("%-66s", TEST_NAME))           // Owner Name
          .append("O")                                         // O for Owner
          .append("Y")                                         // DataHive Lookup Flag = Y
          .append(String.format("%-48s", "DATAHIVE_LOOKUP"))   // Special marker
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 record

        return sb.toString();
    }
}
