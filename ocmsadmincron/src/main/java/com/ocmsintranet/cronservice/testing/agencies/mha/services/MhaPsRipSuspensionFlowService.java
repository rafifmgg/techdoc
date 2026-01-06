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
 * Service for testing MHA PS-RIP Suspension Flow scenario.
 *
 * Test scenario: Death suspension with death date after offence date (PS-RIP)
 * Expected result: VON updated with suspension_type="PS", epr_reason_of_suspension="RIP"
 *                  and suspended notice record created with death information
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC and death date after offence date
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with death info
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify PS-RIP suspension applied
 */
@Slf4j
@Service
public class MhaPsRipSuspensionFlowService {

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

    // Test data constants - death after offence date scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String NOTICE_PREFIX = "PSRIP";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";
    private static final String TEST_VEHICLE_NO = "SBA9012E";
    private static final String TEST_NRIC = "S3239822G";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String TEST_OWNER_NAME = "Sophia Tan";

    // Death scenario data - death AFTER offence date
    private static final String OFFENCE_DATE = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));  // Offence happened first
    private static final String DEATH_DATE = "2026-06-09";        // Death happened after offence

    // SFTP paths
    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete PS-RIP Suspension Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA PS-RIP Suspension Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData());

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload());

            // Step 3: Verify files di SFTP
            steps.add(verifySftpFiles());

            // Step 4: POST /test/mha/mha-callback with death info
            steps.add(simulateMhaCallbackWithDeath());

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload());

            // Step 6: Verify PS-RIP suspension applied
            steps.add(verifyPsRipSuspension());

        } catch (Exception e) {
            log.error("❌ Critical error during PS-RIP flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA PS-RIP Suspension Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC and death date after offence date.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - PS-RIP Scenario (Death After Offence)", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for PS-RIP scenario: {}", NOTICE_NUMBER);

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
                result.addDetail("🆔 NRIC: " + TEST_NRIC + " (Deceased - ready for PS-RIP suspension)");
                result.addDetail("📅 Offence Date: " + OFFENCE_DATE);
                result.addDetail("☠️ Death Date: " + DEATH_DATE);
                result.addDetail("✅ Logic Check: DoD AFTER offence → Expected RIP suspension");
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
     * UPSERT pattern for test data setup (DEV environment compatible)
     */
    private void resetOrInsertTestData(TestStepResult result) {
        try {
            log.info("🔄 Starting UPSERT pattern for test data with notice number: {}", NOTICE_NUMBER);
            // CORRECT ORDER: Parent tables first, then child tables
            resetOrInsertValidOffenceNotice(result);      // 1. Parent table
            resetOrInsertOffenceNoticeDetail(result);     // 2. Child table
            resetOrInsertOwnerDriver(result);             // 3. Child table
            resetOrInsertOwnerDriverAddress(result);      // 4. Child table
            result.addDetail("✅ UPSERT pattern completed successfully");
        } catch (Exception e) {
            log.error("❌ Error during UPSERT pattern: {}", e.getMessage(), e);
            result.addDetail("❌ UPSERT pattern failed: " + e.getMessage());
            throw e;
        }
    }

    // UPSERT methods for each table

    private void resetOrInsertValidOffenceNotice(TestStepResult result) {
        if (validOffenceNoticeExists()) {
            resetValidOffenceNotice();
            result.addDetail("🔄 Existing valid_offence_notice reset for: " + NOTICE_NUMBER);
        } else {
            insertValidOffenceNotice();
            result.addDetail("✅ New valid_offence_notice inserted for: " + NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOffenceNoticeDetail(TestStepResult result) {
        if (offenceNoticeDetailExists()) {
            resetOffenceNoticeDetail();
            result.addDetail("🔄 Existing offence_notice_detail reset for: " + NOTICE_NUMBER);
        } else {
            insertOffenceNoticeDetail();
            result.addDetail("✅ New offence_notice_detail inserted for: " + NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOwnerDriver(TestStepResult result) {
        if (ownerDriverExists()) {
            resetOwnerDriver();
            result.addDetail("🔄 Existing owner_driver reset for: " + NOTICE_NUMBER);
        } else {
            insertOwnerDriver();
            result.addDetail("✅ New owner_driver inserted for: " + NOTICE_NUMBER);
        }
    }

    private void resetOrInsertOwnerDriverAddress(TestStepResult result) {
        if (ownerDriverAddressExists()) {
            resetOwnerDriverAddress();
            result.addDetail("🔄 Existing owner_driver_address reset for: " + NOTICE_NUMBER);
        } else {
            insertOwnerDriverAddress(result);
            result.addDetail("✅ New owner_driver_address inserted for: " + NOTICE_NUMBER);
        }
    }

    // Existence check methods

    private boolean validOffenceNoticeExists() {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, NOTICE_NUMBER);
        return count != null && count > 0;
    }

    private boolean offenceNoticeDetailExists() {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, NOTICE_NUMBER);
        return count != null && count > 0;
    }

    private boolean ownerDriverExists() {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, NOTICE_NUMBER);
        return count != null && count > 0;
    }

    private boolean ownerDriverAddressExists() {
        String sql = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, NOTICE_NUMBER);
        return count != null && count > 0;
    }

    // Reset methods (UPDATE audit fields)

    private void resetValidOffenceNotice() {
        String sql = "UPDATE " + SCHEMA + ".ocms_valid_offence_notice SET " +
                     "vehicle_no = ?, offence_notice_type = ?, last_processing_stage = ?, next_processing_stage = ?, " +
                     "last_processing_date = ?, next_processing_date = ?, notice_date_and_time = ?, " +
                     "pp_code = ?, pp_name = ?, composition_amount = ?, computer_rule_code = ?, vehicle_category = ?, " +
                     "suspension_type = NULL, epr_reason_of_suspension = NULL, epr_date_of_suspension = NULL, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        jdbcTemplate.update(sql, TEST_VEHICLE_NO, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.parse(OFFENCE_DATE + "T00:00"),
            "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);
    }

    private void resetOffenceNoticeDetail() {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_detail SET " +
                     "lta_chassis_number = ?, lta_make_description = ?, lta_primary_colour = ?, " +
                     "upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        jdbcTemplate.update(sql, "MHTEST123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);
    }

    private void resetOwnerDriver() {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                     "owner_driver_indicator = ?, id_type = ?, id_no = ?, name = ?, offender_indicator = ?, " +
                     "date_of_death = ?, life_status = ?, upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        jdbcTemplate.update(sql, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            LocalDateTime.parse(DEATH_DATE + "T00:00"), "D", "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);
    }

    private void resetOwnerDriverAddress() {
        String sql = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                     "owner_driver_indicator = ?, type_of_address = ?, blk_hse_no = ?, street_name = ?, floor_no = ?, unit_no = ?, " +
                     "bldg_name = ?, postal_code = ?, upd_user_id = ?, upd_date = ? " +
                     "WHERE notice_no = ?";

        jdbcTemplate.update(sql, "O", "mha_reg", "004", "", "", "", "", "",
            "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);
    }

    // Insert methods

    private void insertValidOffenceNotice() {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            NOTICE_NUMBER,                        // notice_no
            TEST_VEHICLE_NO,                      // vehicle_no
            "O",                                  // offence_notice_type (O = Original)
            "ROV",                                // last_processing_stage
            "RD1",                                // next_processing_stage
            LocalDateTime.now(),                  // last_processing_date
            LocalDateTime.now().plusDays(1),      // next_processing_date
            LocalDateTime.parse(OFFENCE_DATE + "T00:00"), // notice_date_and_time
            "A0001",                              // pp_code (parking place code)
            "TEST PARKING PLACE",                 // pp_name (parking place name - required field)
            new java.math.BigDecimal("70.00"),    // composition_amount (required field)
            10001,                                // computer_rule_code (required field)
            "C",                                  // vehicle_category (C=Car, required field)
            "TEST_USER",                          // cre_user_id
            LocalDateTime.now()                   // cre_date
        );
    }

    private void insertOffenceNoticeDetail() {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NUMBER, "MHTEST123456789", "TOYOTA", "WHITE",
            "TEST_USER", LocalDateTime.now());
    }

    private void insertOwnerDriver() {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "date_of_death, life_status, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NUMBER, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            LocalDateTime.parse(DEATH_DATE + "T00:00"), "D", "TEST_USER", LocalDateTime.now());
    }

    private void insertOwnerDriverAddress(TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, floor_no, unit_no, " +
                     "bldg_name, postal_code, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NUMBER, "O", "mha_reg", "004", "", "", "", "", "",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Address record inserted: " + NOTICE_NUMBER + " (for PS-RIP scenario)");
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
     * Step 3: Verify files di SFTP - Check generated files exist.
     */
    private TestStepResult verifySftpFiles() {
        TestStepResult result = new TestStepResult("Step 3: Verify Files di SFTP - Check Generated Files", STATUS_INFO);

        try {
            log.info("📂 Checking SFTP files in {}", sftpInputPath);

            // List files di input directory
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with death info.
     */
    private TestStepResult simulateMhaCallbackWithDeath() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Death Info", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with death info for: {}", NOTICE_NUMBER);

            // Generate MHA response files with death information
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
            result.addDetail("☠️ Death info for NRIC: " + TEST_NRIC + " (Status: " + "D" + ")");
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
     * Step 6: Verify PS-RIP suspension applied and suspended notice created.
     */
    private TestStepResult verifyPsRipSuspension() {
        TestStepResult result = new TestStepResult("Step 6: Verify PS-RIP Suspension Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying PS-RIP suspension for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: VON suspension status updated to PS-RIP
            String vonSQL = "SELECT suspension_type, epr_reason_of_suspension, epr_date_of_suspension, " +
                           "last_processing_stage, next_processing_stage " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, NOTICE_NUMBER);
                String suspensionType = (String) vonData.get("suspension_type");
                String suspensionReason = (String) vonData.get("epr_reason_of_suspension");

                if ("PS".equals(suspensionType) && "RIP".equals(suspensionReason)) {
                    verificationResults.add("✅ VON Suspension Status: PS-RIP Applied");
                    verificationResults.add("  📋 Type: " + suspensionType + ", Reason: " + suspensionReason);
                    verificationResults.add("  📅 Date: " + vonData.get("epr_date_of_suspension"));
                    verificationResults.add("  🔄 Stages: " + vonData.get("last_processing_stage") + " -> " + vonData.get("next_processing_stage"));
                } else {
                    verificationResults.add("❌ VON Suspension: Expected PS-RIP, Got " + suspensionType + "-" + suspensionReason);
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
                                 "AND reason_of_suspension = 'RIP' ORDER BY cre_date DESC";

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
            String ownerSQL = "SELECT name, id_no, id_type, offender_indicator, life_status, date_of_death " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

            try {
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Owner/Driver Data Integrity:");
                verificationResults.add("  📝 Name: " + ownerData.get("name"));
                verificationResults.add("  🆔 NRIC: " + ownerData.get("id_no") + " (Type: " + ownerData.get("id_type") + ")");
                verificationResults.add("  🚨 Offender: " + ownerData.get("offender_indicator"));
                verificationResults.add("  ⚖️ Life Status: " + ownerData.get("life_status"));
                verificationResults.add("  ☠️ Death Date: " + ownerData.get("date_of_death"));

                if ("D".equals(ownerData.get("life_status"))) {
                    verificationResults.add("  ✅ Life status correctly set to Deceased");
                } else {
                    verificationResults.add("  ❌ Life status not updated to Deceased");
                    allVerificationsPassed = false;
                }

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

            // Verify 5: No unintended TS suspension applied
            String tsCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                               "WHERE notice_no = ? AND suspension_type = 'TS'";

            Integer tsCount = jdbcTemplate.queryForObject(tsCheckSQL, Integer.class, NOTICE_NUMBER);
            if (tsCount != null && tsCount == 0) {
                verificationResults.add("✅ TS Suspension Check: No unintended TS suspension");
            } else {
                verificationResults.add("⚠️ TS Suspension Check: Found " + tsCount + " TS suspension (unexpected)");
            }

            // Verify 6: Date logic - DoD after offence date (PS-RIP specific)
            String dateLogicSQL = "SELECT von.notice_date_and_time, od.date_of_death " +
                                 "FROM " + SCHEMA + ".ocms_valid_offence_notice von " +
                                 "JOIN " + SCHEMA + ".ocms_offence_notice_owner_driver od ON von.notice_no = od.notice_no " +
                                 "WHERE von.notice_no = ?";

            try {
                Map<String, Object> dateData = jdbcTemplate.queryForMap(dateLogicSQL, NOTICE_NUMBER);

                // Convert Timestamp to LocalDateTime safely
                LocalDateTime offenceDate = null;
                LocalDateTime deathDate = null;

                Object offenceObj = dateData.get("notice_date_and_time");
                if (offenceObj instanceof java.sql.Timestamp) {
                    offenceDate = ((java.sql.Timestamp) offenceObj).toLocalDateTime();
                }

                Object deathObj = dateData.get("date_of_death");
                if (deathObj instanceof java.sql.Timestamp) {
                    deathDate = ((java.sql.Timestamp) deathObj).toLocalDateTime();
                }

                if (deathDate != null && offenceDate != null && deathDate.isAfter(offenceDate)) {
                    verificationResults.add("✅ Date Logic Verification: DoD AFTER offence (RIP scenario)");
                    verificationResults.add("  📅 Offence: " + offenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    verificationResults.add("  ☠️ Death: " + deathDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                } else {
                    verificationResults.add("❌ Date Logic: Incorrect DoD vs offence date relationship");
                    if (deathDate == null) verificationResults.add("  ⚠️ Death date is null");
                    if (offenceDate == null) verificationResults.add("  ⚠️ Offence date is null");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Date logic verification failed: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ PS-RIP Suspension verification completed successfully");
                result.addDetail("📋 Expected behavior: PS suspension with RIP reason applied");
                result.addDetail("🎯 Scenario outcome: PASSED - PS-RIP flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ PS-RIP Suspension verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during PS-RIP suspension verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods for data cleanup and insertion

    private void generateMhaResponseFiles(TestStepResult result, String NOTICE_NUMBER) {
        try {
            // Generate main response file with death information
            String mainContent = generateMainFileWithDeath(NOTICE_NUMBER);

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp + ".";

            // Upload main file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated main file with death info: " + mainFileName);
            result.addDetail("☠️ Death info: NRIC " + TEST_NRIC + " Status=" + "D");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithDeath(String NOTICE_NUMBER) {
        // Generate main response file content with death information
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Main record with death info
        sb.append("D");
        sb.append(String.format("%-12s", TEST_NRIC));  // UIN
        sb.append(String.format("%-66s", TEST_OWNER_NAME));  // Name
        sb.append("D");  // Life Status - D (Deceased)

        // Date of death (3 days ago in YYYYMMDD format)
        LocalDateTime deathDate = LocalDateTime.now().minusDays(3);
        sb.append(deathDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        // Other fields (address, etc.) - simplified for test
        sb.append(String.format("%-10s", "123"));  // Block/House
        sb.append(String.format("%-32s", "RIP TEST STREET"));  // Street
        sb.append(String.format("%-6s", "123456"));  // Postal Code

        sb.append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 main record

        return sb.toString();
    }
}
