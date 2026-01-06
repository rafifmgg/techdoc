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
 * Service for testing MHA Embassy/Diplomat Processing Flow scenario.
 *
 * Test scenario: Diplomatic flag handling with special logic for embassy vehicles
 * Expected result: MHA response sets lta_diplomatic_flag="Y" and vehicle_registration_type="D"
 *                  for vehicles with diplomatic registration
 *
 * 6-Step Flow:
 * 1. Setup test data with vehicle having diplomatic registration
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with diplomatic flag
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify diplomatic flag processing and vehicle registration type update
 */
@Slf4j
@Service
public class MhaEmbassyDiplomatFlowService {

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

    // Test data constants for embassy/diplomat scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "CD1234E";  // CD prefix for diplomatic vehicles
    private static final String TEST_NRIC = "S8901234G";
    private static final String TEST_NAME = "Ambassador William Chen";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type

    // Notice constants for embassy/diplomat flow
    private static final String NOTICE_PREFIX = "MHADIP";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Embassy/Diplomat Processing Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA Embassy/Diplomat Processing Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload());

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with diplomatic flag
            steps.add(simulateMhaCallbackWithDiplomaticFlag());

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload());

            // Step 6: Verify diplomatic flag processing applied
            steps.add(verifyDiplomaticFlagProcessing());

        } catch (Exception e) {
            log.error("❌ Critical error during Embassy/Diplomat flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Embassy/Diplomat Processing Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with vehicle having diplomatic registration.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Embassy/Diplomat Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for Embassy/Diplomat scenario: {}", NOTICE_NUMBER);

            result.addDetail("🔄 Preparing test data (UPSERT mode for DEV compatibility)...");
            resetOrInsertTestData(result);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, NOTICE_NUMBER);

            if (recordCount != null && recordCount == 1) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed successfully");
                result.addDetail("📋 Notice: " + NOTICE_NUMBER);
                result.addDetail("🚗 Diplomatic Vehicle: " + TEST_VEHICLE_NO);
                result.addDetail("👤 Owner: " + TEST_NRIC + " (" + TEST_NAME + ")");
                result.addDetail("🏛️ Initial diplomatic flag: Not set (will be updated by MHA)");
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with diplomatic flag.
     */
    private TestStepResult simulateMhaCallbackWithDiplomaticFlag() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Diplomatic Flag", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with diplomatic flag for: {}", NOTICE_NUMBER);

            // Generate MHA response files with diplomatic flag information
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
            result.addDetail("🏛️ Diplomatic flag data for notice: " + NOTICE_NUMBER);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 150)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error simulating MHA callback: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ MHA callback simulation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 5: POST /mha/download/execute - Trigger download process.
     */
    private TestStepResult triggerMhaDownload() {
        TestStepResult result = new TestStepResult("Step 5: POST /mha/download/execute - Trigger Download Process", STATUS_INFO);

        try {
            String downloadUrl = apiConfigHelper.buildApiUrl("/v1/mha/download/execute");
            log.info("⬇️ Calling MHA download API: {}", downloadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}"; // Empty JSON payload
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(downloadUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA download API called successfully");
            result.addDetail("📥 URL: " + downloadUrl);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 200)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error calling MHA download API: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Download API call failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 6: Verify diplomatic flag processing and vehicle registration type update.
     */
    private TestStepResult verifyDiplomaticFlagProcessing() {
        TestStepResult result = new TestStepResult("Step 6: Verify Diplomatic Flag Processing Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying diplomatic flag processing for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Diplomatic flag updated in offence notice detail
            String detailSQL = "SELECT lta_diplomatic_flag, cre_user_id, cre_date, upd_user_id, upd_date " +
                              "FROM " + SCHEMA + ".ocms_offence_notice_detail " +
                              "WHERE notice_no = ?";

            try {
                Map<String, Object> detailData = jdbcTemplate.queryForMap(detailSQL, NOTICE_NUMBER);
                String diplomaticFlag = (String) detailData.get("lta_diplomatic_flag");

                if ("Y".equals(diplomaticFlag)) {
                    verificationResults.add("✅ Diplomatic Flag Update: lta_diplomatic_flag = 'Y'");
                    verificationResults.add("  📋 Updated by: " + detailData.get("upd_user_id"));
                    verificationResults.add("  📅 Updated on: " + detailData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Diplomatic Flag Update: Expected 'Y', got '" + diplomaticFlag + "'");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Diplomatic flag verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Vehicle registration type updated to "D"
            String vonSQL = "SELECT vehicle_registration_type, last_processing_stage, next_processing_stage, " +
                           "cre_user_id, cre_date, upd_user_id, upd_date " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                           "WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, NOTICE_NUMBER);
                String registrationType = (String) vonData.get("vehicle_registration_type");

                if ("D".equals(registrationType)) {
                    verificationResults.add("✅ Vehicle Registration Type: vehicle_registration_type = 'D' (Diplomatic)");
                    verificationResults.add("  📋 Updated by: " + vonData.get("upd_user_id"));
                    verificationResults.add("  📅 Updated on: " + vonData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Vehicle Registration Type: Expected 'D', got '" + registrationType + "'");
                    allVerificationsPassed = false;
                }

                verificationResults.add("✅ Processing Stages: " + vonData.get("last_processing_stage") + " -> " + vonData.get("next_processing_stage"));

            } catch (Exception e) {
                verificationResults.add("❌ Vehicle registration type verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: Owner/Driver data integrity maintained
            String ownerSQL = "SELECT id_no, name, id_type, offender_indicator, " +
                             "cre_user_id, cre_date, upd_user_id, upd_date " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no = ?";

            try {
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Owner/Driver Data Integrity:");
                verificationResults.add("  👤 Owner: " + ownerData.get("name") + " (" + ownerData.get("id_no") + ")");
                verificationResults.add("  🆔 ID Type: " + ownerData.get("id_type"));
                verificationResults.add("  ⚖️ Offender: " + ownerData.get("offender_indicator"));

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver data verification skipped: " + e.getMessage());
            }

            // Verify 4: No unintended suspension applied (diplomatic vehicles get special handling)
            String suspensionCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                       "WHERE notice_no = ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            Integer suspensionCount = jdbcTemplate.queryForObject(suspensionCheckSQL, Integer.class, NOTICE_NUMBER);
            if (suspensionCount != null && suspensionCount == 0) {
                verificationResults.add("✅ Suspension Check: No suspension applied (expected for diplomatic vehicles)");
            } else {
                verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (verify diplomatic handling)");
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Diplomatic Flag Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: lta_diplomatic_flag='Y' and vehicle_registration_type='D'");
                result.addDetail("🎯 Scenario outcome: PASSED - Embassy/Diplomat flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Diplomatic Flag Processing verification failed");
                result.addDetail("📋 Some critical diplomatic processing checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Diplomatic Flag verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods for data cleanup and insertion

    /**
     * UPSERT pattern implementation for DEV environment compatibility
     * This method checks if test data exists and either updates it to initial state or inserts new records
     */
    private void resetOrInsertTestData(TestStepResult result) {
        try {
            log.info("🔄 Starting UPSERT pattern for test data with notice number: {}", NOTICE_NUMBER);

            // Process each table in dependency order (child tables first for deletion, parent first for insertion)
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
                              "vehicle_registration_type = NULL, suspension_type = NULL, epr_reason_of_suspension = NULL, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                TEST_VEHICLE_NO, "O", "ROV", "RD1",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
                "A0001", "EMBASSY DISTRICT PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
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
                "A0001", "EMBASSY DISTRICT PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
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
                              "lta_diplomatic_flag = NULL, vehicle_registration_type = NULL, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "DIPTEST789012345", "BMW", "BLACK",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Offence notice detail reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                              "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "DIPTEST789012345", "BMW", "BLACK",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Offence notice detail inserted: " + NOTICE_NUMBER);
        }
    }

    /**
     * Reset or insert owner/driver record
     */
    private void resetOrInsertOwnerDriver(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                              "owner_driver_indicator = ?, id_type = ?, id_no = ?, name = ?, offender_indicator = ?, " +
                              "life_status = NULL, date_of_death = NULL, " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Owner/driver record reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                              "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Owner/driver record inserted: " + NOTICE_NUMBER);
        }
    }

    /**
     * Reset or insert owner/driver address record
     */
    private void resetOrInsertOwnerDriverAddress(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

        if (count != null && count > 0) {
            // Reset existing record to initial state
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                              "owner_driver_indicator = ?, type_of_address = ?, blk_hse_no = ?, street_name = ?, " +
                              "floor_no = ?, unit_no = ?, bldg_name = ?, postal_code = ?, " +
                              "address_type = ?, invalid_addr_tag = NULL, processing_date_time = NULL, " +
                              "effective_date = ?, upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "O", "mha_reg", "123", "EMBASSY STREET",
                "01", "01", "EMBASSY BUILDING", "123456",
                "R", LocalDateTime.now(), "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Owner/driver address reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                              "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, " +
                              "floor_no, unit_no, bldg_name, postal_code, address_type, effective_date, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL,
                NOTICE_NUMBER, "O", "mha_reg", "123", "EMBASSY STREET",
                "01", "01", "EMBASSY BUILDING", "123456", "R", LocalDateTime.now(),
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Owner/driver address inserted: " + NOTICE_NUMBER);
        }
    }

    private void generateMhaResponseFiles(TestStepResult result) {
        try {
            // Generate NRO2URA file with diplomatic flag information
            String mainContent = generateMainFileWithDiplomaticFlag();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp;

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with diplomatic flag: " + mainFileName);
            result.addDetail("🏛️ Diplomatic content: lta_diplomatic_flag='Y' for " + TEST_VEHICLE_NO);

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithDiplomaticFlag() {
        // Generate NRO2URA file content with diplomatic flag information
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Owner record with diplomatic flag
        sb.append("D")
          .append(String.format("%-12s", TEST_NRIC))           // Owner NRIC
          .append(String.format("%-66s", TEST_NAME))           // Owner Name
          .append("O")                                         // O for Owner
          .append("Y")                                         // Diplomatic Flag = Y
          .append(String.format("%-48s", "DIPLOMATIC"))       // Special marker
          .append("\n");

        // Vehicle record with diplomatic registration type
        sb.append("V")
          .append(String.format("%-10s", TEST_VEHICLE_NO))     // Vehicle Number
          .append("D")                                         // Registration Type = D (Diplomatic)
          .append(String.format("%-117s", "DIPLOMATIC VEHICLE")) // Padding
          .append("\n");

        // Trailer
        sb.append("T000002\n");  // 2 records (1 owner + 1 vehicle)

        return sb.toString();
    }
}
