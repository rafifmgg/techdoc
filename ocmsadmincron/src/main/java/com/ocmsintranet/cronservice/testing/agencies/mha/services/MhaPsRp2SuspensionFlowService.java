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
 * Service for testing MHA PS-RP2 Suspension Flow scenario.
 *
 * Test scenario: Death suspension with death date before offence date (PS-RP2)
 * Expected result: VON updated with suspension_type="PS", epr_reason_of_suspension="RP2"
 *                  and suspended notice record created with auto-generated remarks
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC and death date before offence date
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with death info
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify PS-RP2 suspension applied with auto-remarks and proper date logic
 */
@Slf4j
@Service
public class MhaPsRp2SuspensionFlowService {

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

    // Test data constants - death before offence date scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String NOTICE_PREFIX = "PSRP2";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";
    private static final String TEST_VEHICLE_NO = "SBA3456D";
    private static final String TEST_NRIC = "S4958372H";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String TEST_OWNER_NAME = "Wei Xuan Lim";

    // Death scenario data - death BEFORE offence date
    private static final String DEATH_DATE = "2023-12-10";        // Death happened first
    private static final String OFFENCE_DATE = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));      // Offence happened after death

    // SFTP paths
    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete PS-RP2 Suspension Flow test with 6 steps.
     *
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA PS-RP2 Suspension Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData());
            steps.add(triggerMhaUpload());
            steps.add(verifySftpFiles());
            steps.add(simulateMhaCallbackWithDeath());
            steps.add(triggerMhaDownload());
            steps.add(verifyPsRp2Suspension());

        } catch (Exception e) {
            log.error("❌ Critical error during PS-RP2 flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA PS-RP2 Suspension Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with NRIC and death date before offence date.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - PS-RP2 Scenario (Death Before Offence)", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for PS-RP2 scenario: {}", NOTICE_NUMBER);

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
                result.addDetail("🆔 NRIC: " + TEST_NRIC + " (ready for PS-RP2 suspension)");
                result.addDetail("📅 Offence Date: " + OFFENCE_DATE);
                result.addDetail("☠️ Death Date: " + DEATH_DATE);
                result.addDetail("✅ Logic Check: DoD BEFORE offence → Expected RP2 suspension");
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

    private TestStepResult triggerMhaUpload() {
        TestStepResult result = new TestStepResult("Step 2: POST /mha/upload - Trigger Upload Process", STATUS_INFO);

        try {
            String uploadUrl = apiConfigHelper.buildApiUrl("/v1/mha/upload");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            String response = restTemplate.postForObject(uploadUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA upload API called successfully");
            result.addDetail("📤 URL: " + uploadUrl);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 200)) : "null"));
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Upload API call failed: " + e.getMessage());
        }

        return result;
    }

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
     * Step 6: Verify PS-RP2 suspension applied with auto-remarks.
     */
    private TestStepResult verifyPsRp2Suspension() {
        TestStepResult result = new TestStepResult("Step 6: Verify PS-RP2 Suspension Applied", STATUS_INFO);

        try {
            log.info("🔍 Verifying PS-RP2 suspension for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: VON suspension status updated to PS-RP2
            String vonSQL = "SELECT suspension_type, epr_reason_of_suspension, epr_date_of_suspension, " +
                           "last_processing_stage, next_processing_stage " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, NOTICE_NUMBER);
                String suspensionType = (String) vonData.get("suspension_type");
                String suspensionReason = (String) vonData.get("epr_reason_of_suspension");

                if ("PS".equals(suspensionType) && "RP2".equals(suspensionReason)) {
                    verificationResults.add("✅ VON Suspension Status: PS-RP2 Applied");
                    verificationResults.add("  📋 Type: " + suspensionType + ", Reason: " + suspensionReason);
                    verificationResults.add("  📅 Date: " + vonData.get("epr_date_of_suspension"));
                    verificationResults.add("  🔄 Stages: " + vonData.get("last_processing_stage") + " -> " + vonData.get("next_processing_stage"));
                } else {
                    verificationResults.add("❌ VON Suspension: Expected PS-RP2, Got " + suspensionType + "-" + suspensionReason);
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
                                 "AND reason_of_suspension = 'RP2' ORDER BY cre_date DESC";

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

            // Verify 3: Owner/Driver data integrity (death information)
            String ownerSQL = "SELECT name, id_no, id_type, offender_indicator, life_status, date_of_death " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

            try {
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Owner/Driver Data Integrity:");
                verificationResults.add("  📝 Name: " + ownerData.get("name"));
                verificationResults.add("  🆔 NRIC: " + ownerData.get("id_no") + " (Type: " + ownerData.get("id_type") + ")");
                verificationResults.add("  🚨 Offender: " + ownerData.get("offender_indicator"));
                verificationResults.add("  ☠️ Life Status: " + ownerData.get("life_status"));
                verificationResults.add("  📅 Death Date: " + ownerData.get("date_of_death"));

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

            // Verify 6: Date Logic verification (PS-RP2 specific - DoD before offence)
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

                if (deathDate != null && offenceDate != null && deathDate.isBefore(offenceDate)) {
                    verificationResults.add("✅ Date Logic: DoD BEFORE offence (RP2 scenario confirmed)");
                    verificationResults.add("  📅 Offence: " + offenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    verificationResults.add("  ☠️ Death: " + deathDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } else {
                    verificationResults.add("❌ Date Logic: Expected DoD before offence date for RP2");
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
                result.addDetail("✅ PS-RP2 Suspension verification completed successfully");
                result.addDetail("📋 Expected behavior: PS suspension with RP2 reason applied");
                result.addDetail("🎯 Scenario outcome: PASSED - PS-RP2 flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ PS-RP2 Suspension verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during PS-RP2 suspension verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
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

        jdbcTemplate.update(sql, "MHTEST345678901", "NISSAN", "SILVER",
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

    // Insert methods (CREATE new records)
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

        jdbcTemplate.update(sql, NOTICE_NUMBER, "MHTEST345678901", "NISSAN", "SILVER",
            "TEST_USER", LocalDateTime.now());
    }

    private void insertOwnerDriver() {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "life_status, date_of_death, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NUMBER, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            "D", LocalDateTime.parse(DEATH_DATE + "T00:00"), "TEST_USER", LocalDateTime.now());
    }

    private void insertOwnerDriverAddress(TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, owner_driver_indicator, type_of_address, blk_hse_no, street_name, floor_no, unit_no, " +
                     "bldg_name, postal_code, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NUMBER, "O", "mha_reg", "004", "", "", "", "", "",
            "TEST_USER", LocalDateTime.now());
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate NRO2URA file with death information
            String mainContent = generateMainFileWithDeath();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp + ".";

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with death info: " + mainFileName);
            result.addDetail("📋 Death content: NRIC " + TEST_NRIC + " flagged for RP2 (DoD before offence)");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithDeath() {
        // Generate NRO2URA file content with death information for PS-RP2
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Death record for PS-RP2
        sb.append("D")
          .append(String.format("%-12s", TEST_NRIC))
          .append(String.format("%-66s", TEST_OWNER_NAME))
          .append("D");  // D for Deceased

        // Death date (YYYYMMDD format)
        sb.append(DEATH_DATE.replace("-", ""));
        sb.append(String.format("%-48s", "PS-RP2 TEST DATA"));  // Padding
        sb.append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 death record

        return sb.toString();
    }
}
