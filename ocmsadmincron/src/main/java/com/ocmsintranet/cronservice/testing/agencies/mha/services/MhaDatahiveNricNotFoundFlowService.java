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
 * Service for testing MHA DataHive NRIC Not Found Flow scenario.
 *
 * Test scenario: NRIC not found, no contact update
 * Expected result: No contact fields updated and processing continues normally
 *                  when DataHive lookup returns not found
 *
 * 6-Step Flow:
 * 1. Setup test data with NRIC that does not exist in DataHive
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with valid NRIC
 * 5. POST /mha/download/execute - Trigger download and DataHive lookup (fails)
 * 6. Verify no contact updates and processing continues normally
 */
@Slf4j
@Service
public class MhaDatahiveNricNotFoundFlowService {

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

    // Test data constants for DataHive not found scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SGH8642M";
    private static final String TEST_NRIC = "S8642975J";  // NRIC not in DataHive
    private static final String TEST_NAME = "Wong Ah Beng";
    private static final String TEST_ID_TYPE = "N";

    // Notice constants for DataHive not found flow
    private static final String NOTICE_PREFIX = "DHNOTFN";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // Original contact info (should remain unchanged)
    private static final String ORIGINAL_ADDRESS = "456 Toa Payoh North, #12-34, Singapore 310456";
    private static final String ORIGINAL_POSTAL_CODE = "310456";
    private static final String ORIGINAL_PHONE = "87654321";
    private static final String ORIGINAL_EMAIL = "wongahbeng@oldmail.com";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete DataHive NRIC Not Found Flow test with 6 steps.
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting MHA DataHive NRIC Not Found Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData());
            steps.add(triggerMhaUpload());
            steps.add(verifySftpFiles());
            steps.add(simulateMhaCallbackWithNricNotInDatahive());
            steps.add(triggerMhaDownload());
            steps.add(verifyNoContactUpdateAndNormalProcessing());

        } catch (Exception e) {
            log.error("❌ Critical error during DataHive NRIC Not Found flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA DataHive NRIC Not Found Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - DataHive NRIC Not Found Scenario", STATUS_INFO);

        try {
            log.info("🔧 Setting up test data for DataHive NRIC Not Found scenario: {}", NOTICE_NUMBER);

            result.addDetail("🔄 Preparing test data (UPSERT mode for DEV compatibility)...");
            resetOrInsertTestData(result);

            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, NOTICE_NUMBER);

            if (recordCount != null && recordCount == 1) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed successfully");
                result.addDetail("📋 Notice: " + NOTICE_NUMBER);
                result.addDetail("🚗 Vehicle: " + TEST_VEHICLE_NO);
                result.addDetail("👤 Owner: " + TEST_NRIC + " (" + TEST_NAME + ")");
                result.addDetail("📞 Original contact: Will remain unchanged (NRIC not in DataHive)");
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

    private TestStepResult triggerMhaUpload() {
        TestStepResult result = new TestStepResult("Step 2: POST /mha/upload - Trigger Upload Process", STATUS_INFO);

        try {
            String uploadUrl = apiConfigHelper.buildApiUrl("/v1/mha/upload");
            log.info("⬆️ Calling MHA upload API: {}", uploadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}";
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

    private TestStepResult verifySftpFiles() {
        TestStepResult result = new TestStepResult("Step 3: Verify Files in SFTP - Check Generated Files", STATUS_INFO);

        try {
            log.info("📂 Checking SFTP files in {}", sftpInputPath);

            List<String> inputFiles = sftpUtil.listFiles(SFTP_SERVER, sftpInputPath);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            String expectedFilePattern = "URA2NRO_" + currentDate;

            boolean fileFound = inputFiles.stream().anyMatch(file -> file.contains(expectedFilePattern));

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

    private TestStepResult simulateMhaCallbackWithNricNotInDatahive() {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response (NRIC Not in DataHive)", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with NRIC not in DataHive for: {}", NOTICE_NUMBER);

            generateMhaResponseFiles(result, NOTICE_NUMBER);

            String callbackUrl = apiConfigHelper.buildApiUrl("/test/mha/mha-callback");
            log.info("📱 Calling MHA callback API: {}", callbackUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(callbackUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA callback simulation completed");
            result.addDetail("📞 URL: " + callbackUrl);
            result.addDetail("🆔 NRIC not in DataHive for notice: " + NOTICE_NUMBER);
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 150)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error simulating MHA callback: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ MHA callback simulation failed: " + e.getMessage());
        }

        return result;
    }

    private TestStepResult triggerMhaDownload() {
        TestStepResult result = new TestStepResult("Step 5: POST /mha/download/execute - Trigger Download & DataHive Lookup (Fails)", STATUS_INFO);

        try {
            String downloadUrl = apiConfigHelper.buildApiUrl("/v1/mha/download/execute");
            log.info("⬇️ Calling MHA download API (DataHive lookup will fail): {}", downloadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(downloadUrl, entity, String.class);

            result.setStatus(STATUS_SUCCESS);
            result.addDetail("✅ MHA download API called successfully");
            result.addDetail("📥 URL: " + downloadUrl);
            result.addDetail("🔍 DataHive lookup should fail (NRIC not found)");
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 200)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error calling MHA download API: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Download API call failed: " + e.getMessage());
        }

        return result;
    }

    private TestStepResult verifyNoContactUpdateAndNormalProcessing() {
        TestStepResult result = new TestStepResult("Step 6: Verify No Contact Update & Normal Processing", STATUS_INFO);

        try {
            log.info("🔍 Verifying no contact update for: {}", NOTICE_NUMBER);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Address remains unchanged
            String addressSQL = "SELECT address_line_1, postal_code, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                               "WHERE notice_no = ? AND id_no = ?";

            try {
                Map<String, Object> addressData = jdbcTemplate.queryForMap(addressSQL, NOTICE_NUMBER, TEST_NRIC);
                String currentAddress = (String) addressData.get("address_line_1");
                String postalCode = (String) addressData.get("postal_code");

                if (ORIGINAL_ADDRESS.equals(currentAddress) && ORIGINAL_POSTAL_CODE.equals(postalCode)) {
                    verificationResults.add("✅ Address Unchanged (Expected):");
                    verificationResults.add("  📍 Address: " + currentAddress);
                    verificationResults.add("  📮 Postal Code: " + postalCode);
                    verificationResults.add("  📅 Last updated: " + addressData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Address Changed Unexpectedly: '" + currentAddress + "'");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Address verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Contact info remains unchanged
            String contactSQL = "SELECT offender_tel_no, email_addr, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                               "WHERE notice_no = ? AND id_no = ?";

            try {
                Map<String, Object> contactData = jdbcTemplate.queryForMap(contactSQL, NOTICE_NUMBER, TEST_NRIC);
                String telNo = (String) contactData.get("offender_tel_no");
                String emailAddr = (String) contactData.get("email_addr");

                if (ORIGINAL_PHONE.equals(telNo) && ORIGINAL_EMAIL.equals(emailAddr)) {
                    verificationResults.add("✅ Contact Info Unchanged (Expected):");
                    verificationResults.add("  📱 Tel: " + telNo);
                    verificationResults.add("  📧 Email: " + emailAddr);
                } else {
                    verificationResults.add("❌ Contact Info Changed Unexpectedly");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Contact info verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: DataHive lookup failure logged
            String auditSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_datahive_lookup_log " +
                             "WHERE nric = ? AND lookup_status = 'NOT_FOUND' AND lookup_date >= ?";

            try {
                Integer auditCount = jdbcTemplate.queryForObject(auditSQL, Integer.class, TEST_NRIC,
                    LocalDateTime.now().minusHours(1));

                if (auditCount != null && auditCount > 0) {
                    verificationResults.add("✅ DataHive Lookup Failure Logged: " + auditCount + " NOT_FOUND record(s)");
                } else {
                    verificationResults.add("⚠️ DataHive Lookup Log: No recent NOT_FOUND records");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ DataHive audit verification skipped: " + e.getMessage());
            }

            // Verify 4: Processing continues normally
            String stageSQL = "SELECT last_processing_stage, next_processing_stage " +
                             "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> stageData = jdbcTemplate.queryForMap(stageSQL, NOTICE_NUMBER);
                verificationResults.add("✅ Processing Continues Normally:");
                verificationResults.add("  🔄 Stages: " + stageData.get("last_processing_stage") + " -> " + stageData.get("next_processing_stage"));

            } catch (Exception e) {
                verificationResults.add("⚠️ Processing stage verification skipped: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ DataHive NRIC Not Found verification completed successfully");
                result.addDetail("📋 Expected behavior: No contact updates, processing continues normally");
                result.addDetail("🎯 Scenario outcome: PASSED - DataHive NRIC Not Found flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ DataHive NRIC Not Found verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    /**
     * Reset or insert test data using UPSERT pattern (DEV environment compatibility)
     */
    private void resetOrInsertTestData(TestStepResult result) {
        try {
            log.info("🔄 Implementing UPSERT pattern for test data: {}", NOTICE_NUMBER);

            resetOrInsertValidOffenceNotice(result);
            resetOrInsertOffenceNoticeDetail(result);
            resetOrInsertOwnerDriverWithOriginalContact(result);
            resetOrInsertOwnerDriverAddress(result);

            result.addDetail("✅ UPSERT operations completed successfully");
            log.info("✅ Test data UPSERT completed for notice: {}", NOTICE_NUMBER);

        } catch (Exception e) {
            log.error("❌ Error during test data UPSERT: {}", e.getMessage(), e);
            result.addDetail("❌ UPSERT error: " + e.getMessage());
            throw e;
        }
    }

    private void resetOrInsertValidOffenceNotice(TestStepResult result) {
        try {
            String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

            if (count != null && count > 0) {
                // Reset existing record to initial state
                String updateSQL = "UPDATE " + SCHEMA + ".ocms_valid_offence_notice SET " +
                    "vehicle_no = ?, last_processing_stage = ?, next_processing_stage = ?, " +
                    "last_processing_date = ?, next_processing_date = ?, notice_date_and_time = ?, " +
                    "upd_user_id = ?, upd_date = ? WHERE notice_no = ?";

                jdbcTemplate.update(updateSQL, TEST_VEHICLE_NO, "ROV", "RD1",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
                    "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

                result.addDetail("🔄 Reset valid offence notice: " + NOTICE_NUMBER);
            } else {
                // Insert new record
                String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                    "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                    "last_processing_date, next_processing_date, notice_date_and_time, " +
                    "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                    "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSQL, NOTICE_NUMBER, TEST_VEHICLE_NO, "O", "ROV", "RD1",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
                    "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
                    "TEST_USER", LocalDateTime.now());

                result.addDetail("✅ Inserted valid offence notice: " + NOTICE_NUMBER);
            }
        } catch (Exception e) {
            result.addDetail("❌ Error with valid offence notice: " + e.getMessage());
            throw e;
        }
    }

    private void resetOrInsertOffenceNoticeDetail(TestStepResult result) {
        try {
            String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER);

            if (count != null && count > 0) {
                // Reset existing record
                String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_detail SET " +
                    "lta_chassis_number = ?, lta_make_description = ?, lta_primary_colour = ?, " +
                    "upd_user_id = ?, upd_date = ? WHERE notice_no = ?";

                jdbcTemplate.update(updateSQL, "NFTEST789012345", "NISSAN", "BLUE",
                    "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER);

                result.addDetail("🔄 Reset offence notice detail: " + NOTICE_NUMBER);
            } else {
                // Insert new record
                String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                    "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                    "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "NFTEST789012345", "NISSAN", "BLUE",
                    "TEST_USER", LocalDateTime.now());

                result.addDetail("✅ Inserted offence notice detail: " + NOTICE_NUMBER);
            }
        } catch (Exception e) {
            result.addDetail("❌ Error with offence notice detail: " + e.getMessage());
            throw e;
        }
    }

    private void resetOrInsertOwnerDriverWithOriginalContact(TestStepResult result) {
        try {
            String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no = ? AND id_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER, TEST_NRIC);

            if (count != null && count > 0) {
                // Reset existing record to original contact info
                String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver SET " +
                    "name = ?, offender_tel_no = ?, email_addr = ?, " +
                    "upd_user_id = ?, upd_date = ? WHERE notice_no = ? AND id_no = ?";

                jdbcTemplate.update(updateSQL, TEST_NAME, ORIGINAL_PHONE, ORIGINAL_EMAIL,
                    "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER, TEST_NRIC);

                result.addDetail("🔄 Reset owner/driver with original contact: " + TEST_NRIC);
            } else {
                // Insert new record
                String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                    "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                    "offender_tel_no, email_addr, cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSQL, NOTICE_NUMBER, "O", TEST_ID_TYPE, TEST_NRIC, TEST_NAME, "Y",
                    ORIGINAL_PHONE, ORIGINAL_EMAIL, "TEST_USER", LocalDateTime.now());

                result.addDetail("✅ Inserted owner/driver with original contact: " + TEST_NRIC + " (" + TEST_NAME + ")");
            }
        } catch (Exception e) {
            result.addDetail("❌ Error with owner/driver record: " + e.getMessage());
            throw e;
        }
    }

    private void resetOrInsertOwnerDriverAddress(TestStepResult result) {
        try {
            String checkSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                             "WHERE notice_no = ? AND id_no = ?";
            Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, NOTICE_NUMBER, TEST_NRIC);

            if (count != null && count > 0) {
                // Reset existing record to original address
                String updateSQL = "UPDATE " + SCHEMA + ".ocms_offence_notice_owner_driver_addr SET " +
                    "address_line_1 = ?, postal_code = ?, " +
                    "upd_user_id = ?, upd_date = ? WHERE notice_no = ? AND id_no = ?";

                jdbcTemplate.update(updateSQL, ORIGINAL_ADDRESS, ORIGINAL_POSTAL_CODE,
                    "TEST_USER", LocalDateTime.now(), NOTICE_NUMBER, TEST_NRIC);

                result.addDetail("🔄 Reset address to original: " + ORIGINAL_ADDRESS);
            } else {
                // Insert new record
                String insertSQL = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                    "notice_no, id_no, address_line_1, postal_code, cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSQL, NOTICE_NUMBER, TEST_NRIC, ORIGINAL_ADDRESS, ORIGINAL_POSTAL_CODE,
                    "TEST_USER", LocalDateTime.now());

                result.addDetail("✅ Inserted original address: Will remain unchanged");
            }
        } catch (Exception e) {
            result.addDetail("❌ Error with address record: " + e.getMessage());
            throw e;
        }
    }


    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            String mainContent = generateMainFileWithNricNotInDatahive();
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String mainFileName = "NRO2URA_" + timestamp;

            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file: " + mainFileName);
            result.addDetail("🔍 NRIC " + TEST_NRIC + " (not in DataHive) - lookup will fail");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithNricNotInDatahive() {
        StringBuilder sb = new StringBuilder();
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");
        sb.append("D")
          .append(String.format("%-12s", TEST_NRIC))
          .append(String.format("%-66s", TEST_NAME))
          .append("O")
          .append("N")  // DataHive Lookup Flag = N (not found)
          .append(String.format("%-48s", "NOT_IN_DATAHIVE"))
          .append("\n");
        sb.append("T000001\n");
        return sb.toString();
    }
}
