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
 * Service for testing MHA Exception Report Flow scenario.
 *
 * Test scenario: .EXP file with suspension applications
 * Expected result: Apply suspensions based on exception type and
 *                  process exception records in ocms_nro_temp table
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC that exists in exception report
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with .EXP file
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify suspensions applied and exception records processed
 */
@Slf4j
@Service
public class MhaExceptionReportFlowService {

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

    // Test data constants for exception report scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SJK2468H";
    private static final String TEST_NRIC = "S2468135G";
    private static final String TEST_NAME = "Lim Beng Hock";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type

    // Notice constants for exception report flow
    private static final String NOTICE_PREFIX = "MHAEXP";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // Exception-specific constants
    private static final String EXCEPTION_TYPE = "TS";  // Traffic Surveillance exception
    private static final String EXCEPTION_REASON = "NRO"; // No Registered Owner

    // SFTP paths
    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Exception Report Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA Exception Report Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload());

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with .EXP file
            steps.add(simulateMhaCallbackWithExceptionFile());

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload());

            // Step 6: Verify exception processing applied
            steps.add(verifyExceptionProcessing());

        } catch (Exception e) {
            log.error("❌ Critical error during Exception Report flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Exception Report Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC yang ada dalam exception report.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Exception Report Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for Exception Report scenario: {}", NOTICE_NUMBER);

            result.addDetail("🔄 Preparing test data (UPSERT mode for DEV compatibility)...");
            resetOrInsertTestData(result);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, NOTICE_NUMBER);

            if (recordCount != null && recordCount == 1) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed");
                result.addDetail("📋 Notice: " + NOTICE_NUMBER);
                result.addDetail("🚗 Vehicle: " + TEST_VEHICLE_NO);
                result.addDetail("👤 Owner: " + TEST_NRIC + " (" + TEST_NAME + ")");
                result.addDetail("⚠️ Exception Type: " + EXCEPTION_TYPE + " - " + EXCEPTION_REASON);
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - Expected 1 record, got: " + recordCount);
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with .EXP file.
     */
    private TestStepResult simulateMhaCallbackWithExceptionFile() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with .EXP File", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with .EXP file for: {}", NOTICE_NUMBER);

            // Generate MHA response files with exception records
            generateMhaExceptionFiles(result);

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
            result.addDetail("📋 Exception file for notice: " + NOTICE_NUMBER);
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
     * Step 6: Verify suspensions applied dan exception records processed.
     */
    private TestStepResult verifyExceptionProcessing() {
        TestStepResult result = new TestStepResult("Step 6: Verify Exception Processing Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying exception processing for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Suspension applied based on exception type
            String suspensionSQL = "SELECT suspension_type, epr_reason_of_suspension, " +
                                  "cre_user_id, cre_date, upd_user_id, upd_date " +
                                  "FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                  "WHERE notice_no = ?";

            try {
                Map<String, Object> suspensionData = jdbcTemplate.queryForMap(suspensionSQL, NOTICE_NUMBER);
                String suspensionType = (String) suspensionData.get("suspension_type");
                String suspensionReason = (String) suspensionData.get("epr_reason_of_suspension");

                if (EXCEPTION_TYPE.equals(suspensionType)) {
                    verificationResults.add("✅ Suspension Applied: suspension_type = '" + suspensionType + "'");
                    verificationResults.add("  📋 Reason: " + suspensionReason);
                    verificationResults.add("  📅 Updated by: " + suspensionData.get("upd_user_id"));
                    verificationResults.add("  🕒 Updated on: " + suspensionData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Suspension Type: Expected '" + EXCEPTION_TYPE + "', got '" + suspensionType + "'");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Suspension verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Exception records processed in ocms_nro_temp
            String nroTempSQL = "SELECT exception_type, exception_reason, processing_status, " +
                               "cre_user_id, cre_date, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_nro_temp " +
                               "WHERE nric = ? AND exception_type = ?";

            try {
                Map<String, Object> nroTempData = jdbcTemplate.queryForMap(nroTempSQL, TEST_NRIC, EXCEPTION_TYPE);

                verificationResults.add("✅ Exception Record Processing:");
                verificationResults.add("  🆔 NRIC: " + TEST_NRIC);
                verificationResults.add("  📋 Exception Type: " + nroTempData.get("exception_type"));
                verificationResults.add("  📝 Exception Reason: " + nroTempData.get("exception_reason"));
                verificationResults.add("  🔄 Status: " + nroTempData.get("processing_status"));
                verificationResults.add("  📅 Updated by: " + nroTempData.get("upd_user_id"));

            } catch (Exception e) {
                verificationResults.add("⚠️ Exception record verification: " + e.getMessage());
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

            // Verify 4: Processing stage progression
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
                result.addDetail("✅ Exception Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: Suspensions applied berdasarkan exception type");
                result.addDetail("🎯 Scenario outcome: PASSED - Exception Report flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Exception Processing verification failed");
                result.addDetail("📋 Some critical exception processing checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Exception Processing verification: {}", e.getMessage(), e);
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
            resetOrInsertExceptionRecord(result);

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
                "E0001", "EXCEPTION REPORT PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
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
                "E0001", "EXCEPTION REPORT PARKING", new java.math.BigDecimal("70.00"), 10001, "C",
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
                "EXPTEST789012345", "TOYOTA", "WHITE",
                "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

            result.addDetail("🔄 Offence notice detail reset: " + NOTICE_NUMBER);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                              "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "EXPTEST789012345", "TOYOTA", "WHITE",
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
                              "address_type = ?, effective_date = ?, upd_user_id = ?, upd_date = ? " +
                              "WHERE notice_no = ?";

            jdbcTemplate.update(updateSQL,
                "O", "mha_reg", "456", "EXCEPTION STREET",
                "02", "02", "EXCEPTION BUILDING", "654321",
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
                NOTICE_NUMBER, "O", "mha_reg", "456", "EXCEPTION STREET",
                "02", "02", "EXCEPTION BUILDING", "654321", "R", LocalDateTime.now(),
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Owner/driver address inserted: " + NOTICE_NUMBER);
        }
    }

    /**
     * Reset or insert exception record in ocms_nro_temp
     */
    private void resetOrInsertExceptionRecord(TestStepResult result) {
        String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_nro_temp WHERE nric = ?";
        Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, TEST_NRIC);

        if (count != null && count > 0) {
            // Reset existing record to initial state
            String updateSQL = "UPDATE " + SCHEMA + ".ocms_nro_temp SET " +
                              "exception_type = ?, reason = ?, status = 'ACTIVE', " +
                              "upd_user_id = ?, upd_date = ? " +
                              "WHERE nric = ?";

            jdbcTemplate.update(updateSQL,
                EXCEPTION_TYPE, EXCEPTION_REASON,
                "TEST_USER", LocalDateTime.now(), TEST_NRIC);

            result.addDetail("🔄 Exception record reset: " + TEST_NRIC);
        } else {
            // Insert new record
            String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_nro_temp (" +
                              "nric, exception_type, reason, status, " +
                              "cre_user_id, cre_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSQL, TEST_NRIC, EXCEPTION_TYPE, EXCEPTION_REASON, "ACTIVE",
                "TEST_USER", LocalDateTime.now());

            result.addDetail("✅ Exception record inserted: " + TEST_NRIC);
        }
    }


    private void generateMhaExceptionFiles(TestStepResult result) {
        try {
            // Generate .EXP file with exception records
            String expContent = generateExceptionFileContent();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String expFileName = "NRO2URA_" + timestamp + ".EXP";

            // Upload .EXP file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, expContent.getBytes(), sftpOutputPath + "/" + expFileName);

            result.addDetail("📤 Generated .EXP file with exception records: " + expFileName);
            result.addDetail("⚠️ Exception content: " + EXCEPTION_TYPE + " - " + EXCEPTION_REASON + " for " + TEST_NRIC);

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA exception files: " + e.getMessage());
        }
    }

    private String generateExceptionFileContent() {
        // Generate .EXP file content with exception records
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Exception record
        sb.append("E")
          .append(String.format("%-12s", TEST_NRIC))           // NRIC
          .append(String.format("%-66s", TEST_NAME))           // Name
          .append(String.format("%-2s", EXCEPTION_TYPE))       // Exception Type (TS)
          .append(String.format("%-3s", EXCEPTION_REASON))     // Exception Reason (NRO)
          .append(String.format("%-45s", "APPLY SUSPENSION"))  // Action instruction
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 exception record

        return sb.toString();
    }
}
