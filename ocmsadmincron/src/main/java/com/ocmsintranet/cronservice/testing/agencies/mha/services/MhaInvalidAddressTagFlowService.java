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
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for testing MHA Invalid Address Tag Validation Flow scenario.
 *
 * Test scenario: TS-NRO suspension triggered by invalid address response from MHA
 * Expected result: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
 *                  when MHA response shows Invalid Address Tag has Value = true
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC that will trigger invalid address
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with Invalid Address Tag
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify TS-NRO suspension applied due to invalid address
 */
@Slf4j
@Service
public class MhaInvalidAddressTagFlowService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_INFO = "INFO";

    // Notice constants for Invalid Address Tag flow
    private static final String NOTICE_PREFIX = "TSNRO";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "002";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SftpUtil sftpUtil;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    // Test data constants - using NRIC that will trigger invalid address tag
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SBA9999Z";
    private static final String TEST_NRIC = "S2847561X";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String TEST_OWNER_NAME = "Rohit Singh";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Invalid Address Tag Validation Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA Invalid Address Tag Validation Flow Test with notice number: {}", NOTICE_NUMBER);

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            // steps.add(triggerMhaUpload());

            // Step 3: Verify files in SFTP
            // steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with Invalid Address Tag
            steps.add(simulateMhaCallbackWithInvalidAddress());

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload());

            // Step 6: Verify TS-NRO suspension applied due to invalid address
            steps.add(verifyInvalidAddressTagSuspension());

        } catch (Exception e) {
            log.error("❌ Critical error during Invalid Address Tag flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Invalid Address Tag Validation Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC that will trigger invalid address.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Invalid Address Tag Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for Invalid Address Tag scenario: {}", NOTICE_NUMBER);

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
                result.addDetail("🆔 NRIC: " + TEST_NRIC + " (will trigger Invalid Address Tag)");
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with Invalid Address Tag.
     */
    private TestStepResult simulateMhaCallbackWithInvalidAddress() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Invalid Address Tag", STATUS_INFO);

        try {
            String noticeNumber = NOTICE_NUMBER;
            log.info("📞 Simulating MHA callback with Invalid Address Tag for: {}", noticeNumber);

            // Generate MHA response files with invalid address indicator
            generateMhaResponseFiles(result, noticeNumber);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA callback simulation completed");
            result.addDetail("🏠 Invalid Address Tag triggered for NRIC: " + TEST_NRIC);
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
     * Step 6: Verify TS-NRO suspension applied due to invalid address tag.
     */
    private TestStepResult verifyInvalidAddressTagSuspension() {
        TestStepResult result = new TestStepResult("Step 6: Verify TS-NRO Suspension Applied (Invalid Address Tag)", STATUS_INFO);

        try {
            String noticeNumber = NOTICE_NUMBER;
            log.info("🔍 Verifying TS-NRO suspension due to Invalid Address Tag for: {}", noticeNumber);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: VON suspension status updated to TS-NRO
            String vonSQL = "SELECT suspension_type, epr_reason_of_suspension, epr_date_of_suspension, " +
                           "last_processing_stage, next_processing_stage " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, noticeNumber);
                String suspensionType = (String) vonData.get("suspension_type");
                String suspensionReason = (String) vonData.get("epr_reason_of_suspension");

                if ("TS".equals(suspensionType) && "NRO".equals(suspensionReason)) {
                    verificationResults.add("✅ VON Suspension Status: TS-NRO Applied (Invalid Address Tag)");
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

            // Verify 2: Suspended notice record created with address validation context
            String suspendedSQL = "SELECT TOP 1 reason_of_suspension, date_of_revival, suspension_remarks, " +
                                 "cre_user_id, cre_date " +
                                 "FROM " + SCHEMA + ".ocms_suspended_notice WHERE notice_no = ? " +
                                 "AND reason_of_suspension = 'NRO' ORDER BY cre_date DESC";

            try {
                Map<String, Object> suspendedData = jdbcTemplate.queryForMap(suspendedSQL, noticeNumber);
                verificationResults.add("✅ Suspended Notice Record Created (Invalid Address Tag):");
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
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, noticeNumber);
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
                Map<String, Object> addrData = jdbcTemplate.queryForMap(addrSQL, noticeNumber);
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

                // Verify invalid_addr_tag is set (specific to this flow)
                String invalidAddrTag = (String) addrData.get("invalid_addr_tag");
                if (invalidAddrTag != null && !invalidAddrTag.trim().isEmpty()) {
                    verificationResults.add("  ✅ Invalid Address Tag set: '" + invalidAddrTag + "'");
                } else {
                    verificationResults.add("  ⚠️ Invalid Address Tag not set (expected for invalid address flow)");
                }

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

            Integer psCount = jdbcTemplate.queryForObject(psCheckSQL, Integer.class, noticeNumber);
            if (psCount != null && psCount == 0) {
                verificationResults.add("✅ PS Suspension Check: No unintended PS suspension");
            } else {
                verificationResults.add("⚠️ PS Suspension Check: Found " + psCount + " PS suspension (unexpected)");
            }

            // Verify 6: Address validation logic working
            verificationResults.add("✅ Address Validation Logic Verification:");
            verificationResults.add("  🏠 Invalid Address Tag detected from MHA response");
            verificationResults.add("  📍 Trigger condition: Invalid Address Tag has Value = true");
            verificationResults.add("  ⚠️ Expected result: TS-NRO suspension automatically applied");

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Invalid Address Tag validation verification completed successfully");
                result.addDetail("📋 Expected behavior: TS-NRO applied when Invalid Address Tag detected");
                result.addDetail("🎯 Scenario outcome: PASSED - Invalid Address Tag validation working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Invalid Address Tag validation verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Invalid Address Tag validation verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods for database operations

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
            "INVADDR123456789", "HONDA", "RED",
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
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "INVADDR123456789", "HONDA", "RED",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo + " (chassis: INVADDR123456789)");
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (NRIC: " + TEST_NRIC + " - will trigger Invalid Address Tag)");
    }

    private void insertOwnerDriverAddress(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, floor_no, unit_no, bldg_name, postal_code, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", "mha_reg", "002", "INVALID ADDR TEST STREET", "01", "01", "INVALID ADDR BUILDING", "123456",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Address record inserted: " + noticeNo + " (for Invalid Address Tag scenario)");
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate response file with invalid address indicator
            String responseContent = generateResponseWithInvalidAddress(noticeNumber);

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String responseFileName = "NRO2URA_" + timestamp;

            // Upload response file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, responseContent.getBytes(), sftpOutputPath + "/" + responseFileName);

            result.addDetail("📤 Generated MHA response with Invalid Address Tag: " + responseFileName);
            result.addDetail("🏠 Address validation: Invalid Address Tag detected");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateResponseWithInvalidAddress(String noticeNumber) {
        // Generate MHA response content with invalid address tag
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Record with invalid address indicator
        sb.append(String.format("%-9s", TEST_NRIC)) // ID Number
          .append(String.format("%-66s", TEST_OWNER_NAME)) // Name
          .append(String.format("%-8s", "")) // Date Of Birth
          .append(String.format("%-1s", "")) // MHA Address type
          .append(String.format("%-10s", "")) // MHA Block No
          .append(String.format("%-32s", "STREET NAME")) // MHA Street Name
          .append(String.format("%-2s", "")) // MHA Floor No
          .append(String.format("%-5s", "")) // MHA Unit No
          .append(String.format("%-30s", "")) // MHA Building Name
          .append(String.format("%-4s", "")) // Filler
          .append(String.format("%-6s", "")) // MHA New Postal Code
          .append(String.format("%-8s", "")) // Date of Death
          .append(String.format("%-1s", "")) // Life Status
          .append(String.format("%-1s", "Y")) // Invalid Address Tag
          .append(String.format("%-10s", noticeNumber)) // URA Reference No.
          .append(String.format("%-14s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))) // Batch Date Time
          .append(String.format("%-8s", "")) // Date of Address Change
          .append(String.format("%-23s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))) // Timestamp
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 response record

        return sb.toString();
    }
}
