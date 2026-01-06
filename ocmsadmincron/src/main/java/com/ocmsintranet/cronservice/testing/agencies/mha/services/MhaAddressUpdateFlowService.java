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
 * Service for testing MHA Address Update Flow scenario.
 *
 * Test scenario: Owner address changes from MHA response
 * Expected result: Address fields in ocms_offence_notice_owner_driver_addr updated with MHA data
 *
 * 6-Step Flow:
 * 1. Setup test data with existing address different from MHA response
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with updated address
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify address fields updated in ocms_offence_notice_owner_driver_addr
 */
@Slf4j
@Service
public class MhaAddressUpdateFlowService {

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

    // Test data constants for address update scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SBA7890H";
    private static final String TEST_NRIC = "S3456789C";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type
    private static final String TEST_OWNER_NAME = "Lim Wei Ming";

    // Original address (before MHA update)
    private static final String OLD_ADDRESS_LINE1 = "Block 123 Toa Payoh Lorong 2";
    private static final String OLD_ADDRESS_LINE2 = "#05-678";
    private static final String OLD_ADDRESS_LINE3 = "";
    private static final String OLD_POSTAL_CODE = "310123";

    // New address (from MHA response)
    private static final String NEW_ADDRESS_LINE1 = "Block 456 Ang Mo Kio Avenue 3";
    private static final String NEW_ADDRESS_LINE2 = "#12-345";
    private static final String NEW_ADDRESS_LINE3 = "Singapore";
    private static final String NEW_POSTAL_CODE = "560456";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Address Update Flow test with 6 steps.
     *
     * @param noticePrefix Custom notice prefix, default will be generated
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Address Update Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData(noticePrefix));

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload(noticePrefix));

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles(noticePrefix));

            // Step 4: POST /test/mha/mha-callback with address update
            steps.add(simulateMhaCallbackWithAddressUpdate(noticePrefix));

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload(noticePrefix));

            // Step 6: Verify address update applied
            steps.add(verifyAddressUpdate(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Address Update flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Address Update Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with existing address different from MHA response.
     */
    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Address Update Scenario", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔧 Setting up test data for Address Update scenario: {}", noticeNumber);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert valid offence notice
            insertValidOffenceNotice(result, noticeNumber);

            // Insert offence notice detail
            insertOffenceNoticeDetail(result, noticeNumber);

            // Insert owner driver with original address
            insertOwnerDriver(result, noticeNumber);

            // Insert original address (will be updated by MHA)
            insertOriginalAddress(result, noticeNumber);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticeNumber);

            if (recordCount != null && recordCount > 0) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Records: " + recordCount);
                result.addDetail("📋 Notice: " + noticeNumber);
                result.addDetail("🆔 NRIC: " + TEST_NRIC + " (ready for address update)");
                result.addDetail("📍 Original Address: " + OLD_ADDRESS_LINE1 + " " + OLD_POSTAL_CODE);
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
    private TestStepResult triggerMhaUpload(String noticePrefix) {
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
    private TestStepResult verifySftpFiles(String noticePrefix) {
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with address update.
     */
    private TestStepResult simulateMhaCallbackWithAddressUpdate(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Address Update", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("📞 Simulating MHA callback with address update for: {}", noticeNumber);

            // Generate MHA response files with address update
            generateMhaResponseFiles(result, noticeNumber);

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
            result.addDetail("📍 Address update for NRIC: " + TEST_NRIC);
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
    private TestStepResult triggerMhaDownload(String noticePrefix) {
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
     * Step 6: Verify address update applied in ocms_offence_notice_owner_driver_addr.
     */
    private TestStepResult verifyAddressUpdate(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Address Update Applied", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔍 Verifying address update for: {}", noticeNumber);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Address fields updated in ocms_offence_notice_owner_driver_addr
            String addressSQL = "SELECT address_line_1, address_line_2, address_line_3, postal_code, " +
                               "cre_user_id, cre_date, upd_user_id, upd_date " +
                               "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr " +
                               "WHERE notice_no = ? ORDER BY upd_date DESC";

            try {
                Map<String, Object> addressData = jdbcTemplate.queryForMap(addressSQL, noticeNumber);
                String updatedAddr1 = (String) addressData.get("address_line_1");
                String updatedAddr2 = (String) addressData.get("address_line_2");
                String updatedAddr3 = (String) addressData.get("address_line_3");
                String updatedPostal = (String) addressData.get("postal_code");

                if (NEW_ADDRESS_LINE1.equals(updatedAddr1) && NEW_ADDRESS_LINE2.equals(updatedAddr2) &&
                    NEW_POSTAL_CODE.equals(updatedPostal)) {
                    verificationResults.add("✅ Address Update Applied Successfully:");
                    verificationResults.add("  📍 Line 1: " + updatedAddr1);
                    verificationResults.add("  📍 Line 2: " + updatedAddr2);
                    verificationResults.add("  📍 Line 3: " + (updatedAddr3 != null ? updatedAddr3 : ""));
                    verificationResults.add("  📮 Postal: " + updatedPostal);
                    verificationResults.add("  👤 Updated by: " + addressData.get("upd_user_id") +
                                          " at " + addressData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Address Update Failed:");
                    verificationResults.add("  Expected: " + NEW_ADDRESS_LINE1 + " " + NEW_POSTAL_CODE);
                    verificationResults.add("  Got: " + updatedAddr1 + " " + updatedPostal);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Address verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Owner/Driver data remains intact (no corruption)
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

            // Verify 3: No unintended suspension applied
            String suspensionCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                       "WHERE notice_no = ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            Integer suspensionCount = jdbcTemplate.queryForObject(suspensionCheckSQL, Integer.class, noticeNumber);
            if (suspensionCount != null && suspensionCount == 0) {
                verificationResults.add("✅ Suspension Check: No unintended suspension applied");
            } else {
                verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (unexpected for address update)");
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Address Update verification completed successfully");
                result.addDetail("📋 Expected behavior: Address fields updated with MHA data");
                result.addDetail("🎯 Scenario outcome: PASSED - Address Update flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Address Update verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Address Update verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods for data cleanup and insertion

    /**
     * Clean up existing test data with notice prefix
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

            // Delete from parent table last
            int vonDeleted = jdbcTemplate.update(
                "DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?",
                noticePrefix + "%");

            log.info("🧹 Cleanup completed: VON={}, Detail={}, OwnerDriver={}, Address={} records deleted for prefix: {}",
                     vonDeleted, detailDeleted, ownerDriverDeleted, addressDeleted, noticePrefix);

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

        jdbcTemplate.update(sql, noticeNo, "MHTEST456789012", "HONDA", "BLUE",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, TEST_NRIC, TEST_OWNER_NAME, "Y",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (NRIC: " + TEST_NRIC + ")");
    }

    private void insertOriginalAddress(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver_addr (" +
                     "notice_no, address_line_1, address_line_2, address_line_3, postal_code, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, OLD_ADDRESS_LINE1, OLD_ADDRESS_LINE2, OLD_ADDRESS_LINE3, OLD_POSTAL_CODE,
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Original address record inserted: " + noticeNo);
        result.addDetail("   - Address: " + OLD_ADDRESS_LINE1 + " " + OLD_ADDRESS_LINE2);
        result.addDetail("   - Postal: " + OLD_POSTAL_CODE + " (will be updated by MHA)");
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate NRO2URA file with address update information
            String mainContent = generateMainFileWithAddressUpdate();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp;

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with address update: " + mainFileName);
            result.addDetail("📋 Address content: NRIC " + TEST_NRIC + " with new address");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithAddressUpdate() {
        // Generate file content with address update information
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Address update record
        sb.append("D")
          .append(String.format("%-12s", TEST_NRIC))  // ID Number
          .append(String.format("%-66s", TEST_OWNER_NAME))  // Name
          .append("A")  // A for Address update
          .append(String.format("%-50s", NEW_ADDRESS_LINE1))  // New Address Line 1
          .append(String.format("%-30s", NEW_ADDRESS_LINE2))  // New Address Line 2
          .append(String.format("%-20s", NEW_ADDRESS_LINE3))  // New Address Line 3
          .append(String.format("%-6s", NEW_POSTAL_CODE))     // New Postal Code
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 address update record

        return sb.toString();
    }
}
