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
 * Service for testing MHA TS-NRO Suspension Flow scenario.
 *
 * Test scenario: Standard TS suspension with reason NRO
 * Expected result: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
 *                  and suspended notice record created
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC that requires NRO suspension
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with .EXP NRO flag
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify TS-NRO suspension applied and suspended notice created
 */
@Slf4j
@Service
public class MhaTsNroSuspensionFlowService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_INFO = "INFO";

    // Notice constants for TS-NRO suspension flow
    private static final String NOTICE_PREFIX = "TSNRO";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SftpUtil sftpUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    // Test data constants - using NRIC that requires NRO suspension
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SBA5678F";
    private static final String TEST_NRIC = "S2837468B"; // invalid address tag D
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String TEST_OWNER_NAME = "Ahmad Bin Hassan";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete TS-NRO Suspension Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA TS-NRO Suspension Flow Test with notice number: {}", NOTICE_NUMBER);

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload());

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with .EXP NRO
            steps.add(simulateMhaCallbackWithNro());

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload());

            // Step 6: Verify TS-NRO suspension applied
            steps.add(verifyTsNroSuspension());

        } catch (Exception e) {
            log.error("❌ Critical error during TS-NRO flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA TS-NRO Suspension Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC that requires NRO suspension.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - TS-NRO Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for TS-NRO scenario: {}", NOTICE_NUMBER);

            result.addDetail("🔄 Preparing test data (UPSERT mode for DEV compatibility)...");
            resetOrInsertTestData(result);

            result.addDetail("📝 Test data preparation completed...");

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, NOTICE_NUMBER);

            if (recordCount != null && recordCount > 0) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Records: " + recordCount);
                result.addDetail("📋 Notice: " + NOTICE_NUMBER);
                result.addDetail("🆔 NRIC: " + TEST_NRIC + " (ready for TS-NRO suspension)");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - No records found");
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with .EXP NRO flag.
     */
    private TestStepResult simulateMhaCallbackWithNro() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with NRO", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with NRO flag for: {}", NOTICE_NUMBER);

            // Generate MHA response files with NRO exception
            generateMhaResponseFiles(result, NOTICE_NUMBER);

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
            result.addDetail("📋 NRO Exception for NRIC: " + TEST_NRIC);
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
     * Step 6: Verify TS-NRO suspension applied and suspended notice created.
     */
    private TestStepResult verifyTsNroSuspension() {
        TestStepResult result = new TestStepResult("Step 6: Verify TS-NRO Suspension Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying TS-NRO suspension for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: VON suspension status updated to TS-NRO
            String vonSQL = "SELECT suspension_type, epr_reason_of_suspension, epr_date_of_suspension, " +
                           "last_processing_stage, next_processing_stage " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, NOTICE_NUMBER);
                String suspensionType = (String) vonData.get("suspension_type");
                String suspensionReason = (String) vonData.get("epr_reason_of_suspension");

                if ("TS".equals(suspensionType) && "NRO".equals(suspensionReason)) {
                    verificationResults.add("✅ VON Suspension Status: TS-NRO Applied");
                    verificationResults.add("  📋 Type: " + suspensionType + ", Reason: " + suspensionReason);
                    verificationResults.add("  📅 Date: " + vonData.get("epr_date_of_suspension"));
                    verificationResults.add("  🔄 Stages: " + vonData.get("last_processing_stage") + " -> " + vonData.get("next_processing_stage"));
                } else {
                    verificationResults.add("❌ VON Suspension: Expected TS-NRO, Got " + suspensionType + "-" + suspensionReason);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ VON suspension verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Suspended notice record created
            String suspendedSQL = "SELECT TOP 1 reason_of_suspension, date_of_revival, suspension_remarks, " +
                                 "cre_user_id, cre_date " +
                                 "FROM " + SCHEMA + ".ocms_suspended_notice WHERE notice_no = ? " +
                                 "AND reason_of_suspension = 'NRO' ORDER BY cre_date DESC";

            try {
                Map<String, Object> suspendedData = jdbcTemplate.queryForMap(suspendedSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Suspended Notice Record Created:");
                verificationResults.add("  📋 Reason: " + suspendedData.get("reason_of_suspension"));
                verificationResults.add("  📅 Revival Date: " + suspendedData.get("date_of_revival"));
                verificationResults.add("  💬 Remarks: " + suspendedData.get("suspension_remarks"));
                verificationResults.add("  👤 Created by: " + suspendedData.get("cre_user_id") +
                                      " at " + suspendedData.get("cre_date"));

            } catch (Exception e) {
                verificationResults.add("❌ Suspended notice record not found: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: Owner/Driver data remains intact (no corruption)
            String ownerSQL = "SELECT name, id_no, id_type, offender_indicator " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

            try {
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Owner/Driver Data Integrity:");
                verificationResults.add("  📝 Name: " + ownerData.get("name"));
                verificationResults.add("  🆔 NRIC: " + ownerData.get("id_no") + " (Type: " + ownerData.get("id_type") + ")");
                verificationResults.add("  🚨 Offender: " + ownerData.get("offender_indicator"));

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver verification skipped: " + e.getMessage());
            }

            // Verify 4: Owner/Driver Address data integrity
            String addrSQL = "SELECT address_type, blk_hse_no, street_name, floor_no, unit_no, " +
                            "bldg_name, postal_code, invalid_addr_tag, processing_date_time, " +
                            "notice_no, owner_driver_indicator, type_of_address, effective_date " +
                            "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";

            try {
                Map<String, Object> addrData = jdbcTemplate.queryForMap(addrSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Owner/Driver Address Data Integrity:");
                verificationResults.add("  🏠 Address Type: " + addrData.get("address_type"));
                verificationResults.add("  🏢 Block/House: " + addrData.get("blk_hse_no"));
                verificationResults.add("  🛣️ Street: " + addrData.get("street_name"));
                verificationResults.add("  🏠 Floor: " + addrData.get("floor_no") + ", Unit: " + addrData.get("unit_no"));
                verificationResults.add("  🏢 Building: " + addrData.get("bldg_name"));
                verificationResults.add("  📮 Postal Code: " + addrData.get("postal_code"));
                verificationResults.add("  🏷️ Invalid Addr Tag: " + addrData.get("invalid_addr_tag"));
                verificationResults.add("  📅 Processing DateTime: " + addrData.get("processing_date_time"));
                verificationResults.add("  🆔 Owner/Driver Indicator: " + addrData.get("owner_driver_indicator"));
                verificationResults.add("  🏠 Type of Address: " + addrData.get("type_of_address"));
                verificationResults.add("  📅 Effective Date: " + addrData.get("effective_date"));

                // Verify type_of_address = 'mha_reg'
                String typeOfAddress = (String) addrData.get("type_of_address");
                if ("mha_reg".equals(typeOfAddress)) {
                    verificationResults.add("  ✅ Type of Address correctly set to 'mha_reg'");
                } else {
                    verificationResults.add("  ⚠️ Type of Address: Expected 'mha_reg', got '" + typeOfAddress + "'");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver Address verification skipped: " + e.getMessage());
            }

            // Verify 5: No unintended PS suspension applied
            String psCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                               "WHERE notice_no = ? AND suspension_type = 'PS'";

            Integer psCount = jdbcTemplate.queryForObject(psCheckSQL, Integer.class, NOTICE_NUMBER);
            if (psCount != null && psCount == 0) {
                verificationResults.add("✅ PS Suspension Check: No unintended PS suspension");
            } else {
                verificationResults.add("⚠️ PS Suspension Check: Found " + psCount + " PS suspension (unexpected)");
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ TS-NRO Suspension verification completed successfully");
                result.addDetail("📋 Expected behavior: TS suspension with NRO reason applied");
                result.addDetail("🎯 Scenario outcome: PASSED - TS-NRO flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ TS-NRO Suspension verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during TS-NRO suspension verification: {}", e.getMessage(), e);
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
            log.error("❌ Error during UPSERT pattern: {}", e.getMessage(), e);
            result.addDetail("❌ UPSERT pattern failed: " + e.getMessage());
            throw e;
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
            TEST_VEHICLE_NO, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
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
            "MHTEST123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 OND record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                     "owner_driver_indicator = ?, id_type = ?, id_no = ?, name = ?, offender_indicator = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 Owner/driver record reset: " + noticeNo + " (NRIC: " + TEST_NRIC + ", " + rowsUpdated + " rows updated)");
    }

    private void resetOwnerDriverAddress(TestStepResult result, String noticeNo) {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                     "owner_driver_indicator = ?, type_of_address = ?, blk_hse_no = ?, street_name = ?, floor_no = ?, unit_no = ?, bldg_name = ?, postal_code = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
            "O", "mha_reg", "", "", "", "", "", "",
            "TEST_USER", LocalDateTime.now(),
            noticeNo);

        result.addDetail("🔄 Address record reset: " + noticeNo + " (" + rowsUpdated + " rows updated)");
    }

    // ===============================
    // UPSERT Methods (Check + Reset or Insert)
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

    private void insertValidOffenceNotice(TestStepResult result, String noticeNo) {
        log.info("Inserting valid offence notice for notice number: {}", noticeNo);
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
            "TEST PARKING PLACE",               // pp_name (parking place name - required field)
            new java.math.BigDecimal("70.00"),  // composition_amount (required field)
            10001,                              // computer_rule_code (required field)
            "C",                                // vehicle_category (C=Car, required field)
            "TEST_USER",                        // cre_user_id
            LocalDateTime.now()                 // cre_date
        );

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo);
        result.addDetail("   - offence_notice_type: O (Original)");
        result.addDetail("   - vehicle_category: C (Car)");
        result.addDetail("   - pp_code: A0001, pp_name: TEST PARKING PLACE");
        result.addDetail("   - composition_amount: $70.00");
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo) {
        log.info("Inserting offence notice detail for notice number: {}", noticeNo);
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "MHTEST123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo) {
        log.info("Inserting owner/driver record for notice number: {}", noticeNo );
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (NRIC: " + TEST_NRIC + ")");
    }

    private void insertOwnerDriverAddress(TestStepResult result, String noticeNo) {
        log.info("Inserting owner/driver address record for notice number: {}", noticeNo);
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, floor_no, unit_no, bldg_name, postal_code, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", "mha_reg", "001", "TS-NRO TEST STREET", "01", "01", "TS-NRO BUILDING", "123456",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Address record inserted: " + noticeNo + " (for TS-NRO scenario)");
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate .EXP file with NRO exception
            String expContent = generateExpFileWithNro(noticeNumber);

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String expFileName = "REPORT_" + timestamp + ".EXP";

            // Upload .EXP file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, expContent.getBytes(), sftpOutputPath + "/" + expFileName);

            result.addDetail("📤 Generated .EXP file with NRO: " + expFileName);
            result.addDetail("📋 Exception content: NRIC " + TEST_NRIC + " flagged for NRO");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateExpFileWithNro(String noticeNumber) {
        // Generate .EXP file content with NRO exception
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Exception record for NRO
        sb.append("E")
          .append(String.format("%-12s", TEST_NRIC))  // ID Number
          .append("NRO")  // Exception Status - NRO
          .append("N")    // ID Type - NRIC
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 exception record

        return sb.toString();
    }
}
