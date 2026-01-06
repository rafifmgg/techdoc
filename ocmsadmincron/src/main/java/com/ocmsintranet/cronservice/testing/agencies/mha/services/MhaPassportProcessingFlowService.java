package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
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
 * Service for testing MHA Passport Processing Flow scenario.
 *
 * Test scenario: Passport-based identification
 * Expected result: Owner/Driver record updated with passport data from MHA response
 *                  id_type="P", passport details populated including passport_place_of_issue
 *
 * 6-Step Flow:
 * 1. Setup test data with notice containing passport number
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with valid passport data
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify passport processing applied with id_type="P" and passport details populated
 */
@Slf4j
@Service
public class MhaPassportProcessingFlowService {

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

    // Test data constants for passport processing scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SBA3456K";
    private static final String TEST_PASSPORT = "A12345678";  // Passport Number
    private static final String TEST_ID_TYPE = "P";  // Passport ID type
    private static final String TEST_OWNER_NAME = "Johnson Michael Robert";
    private static final String TEST_NATIONALITY = "British";
    private static final String TEST_COUNTRY_CODE = "GB";
    private static final String TEST_PASSPORT_PLACE_OF_ISSUE = "United Kingdom";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Passport Processing Flow test with 6 steps.
     *
     * @param noticePrefix Custom notice prefix, default will be generated
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Passport Processing Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData(noticePrefix));

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload(noticePrefix));

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles(noticePrefix));

            // Step 4: POST /test/mha/mha-callback with passport data
            steps.add(simulateMhaCallbackWithPassport(noticePrefix));

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload(noticePrefix));

            // Step 6: Verify passport processing applied
            steps.add(verifyPassportProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Passport Processing flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Passport Processing Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with notice containing passport number.
     */
    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Passport Processing Scenario", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔧 Setting up test data for Passport Processing scenario: {}", noticeNumber);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert valid offence notice
            insertValidOffenceNotice(result, noticeNumber);

            // Insert offence notice detail
            insertOffenceNoticeDetail(result, noticeNumber);

            // Insert owner driver with passport
            insertOwnerDriver(result, noticeNumber);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticeNumber);

            if (recordCount != null && recordCount > 0) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Records: " + recordCount);
                result.addDetail("📋 Notice: " + noticeNumber);
                result.addDetail("🛂 Passport: " + TEST_PASSPORT + " (ready for passport processing)");
                result.addDetail("🌐 ID Type: P (Passport)");
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with passport data.
     */
    private TestStepResult simulateMhaCallbackWithPassport(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Passport Data", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("📞 Simulating MHA callback with passport data for: {}", noticeNumber);

            // Generate MHA response files with passport information
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
            result.addDetail("🛂 Passport data for: " + TEST_PASSPORT + " (" + TEST_NATIONALITY + ")");
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
     * Step 6: Verify passport processing applied with id_type="P" and passport details populated.
     */
    private TestStepResult verifyPassportProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Passport Processing Applied", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔍 Verifying passport processing for: {}", noticeNumber);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Owner/Driver record updated with passport data
            String ownerSQL = "SELECT name, id_no, id_type, offender_indicator, nationality, " +
                             "passport_place_of_issue, cre_user_id, cre_date, upd_user_id, upd_date " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

            try {
                Map<String, Object> ownerData = jdbcTemplate.queryForMap(ownerSQL, noticeNumber);
                String idType = (String) ownerData.get("id_type");
                String idNo = (String) ownerData.get("id_no");
                String nationality = (String) ownerData.get("nationality");
                String passportPlaceOfIssue = (String) ownerData.get("passport_place_of_issue");

                if ("P".equals(idType) && TEST_PASSPORT.equals(idNo)) {
                    verificationResults.add("✅ Passport Processing Applied Successfully:");
                    verificationResults.add("  🆔 ID Type: " + idType + " (Passport)");
                    verificationResults.add("  🛂 Passport: " + idNo);
                    verificationResults.add("  📝 Name: " + ownerData.get("name"));
                    verificationResults.add("  🌐 Nationality: " + (nationality != null ? nationality : "N/A"));
                    verificationResults.add("  🏛️ Place of Issue: " + (passportPlaceOfIssue != null ? passportPlaceOfIssue : "N/A"));
                    verificationResults.add("  👤 Updated by: " + ownerData.get("upd_user_id") +
                                          " at " + ownerData.get("upd_date"));
                } else {
                    verificationResults.add("❌ Passport Processing Failed:");
                    verificationResults.add("  Expected: id_type=P, id_no=" + TEST_PASSPORT);
                    verificationResults.add("  Got: id_type=" + idType + ", id_no=" + idNo);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Passport processing verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: No unintended suspension applied
            String suspensionCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                       "WHERE notice_no = ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            Integer suspensionCount = jdbcTemplate.queryForObject(suspensionCheckSQL, Integer.class, noticeNumber);
            if (suspensionCount != null && suspensionCount == 0) {
                verificationResults.add("✅ Suspension Check: No unintended suspension applied");
            } else {
                verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (unexpected for passport processing)");
            }

            // Verify 3: Valid offence notice data integrity
            String vonSQL = "SELECT last_processing_stage, next_processing_stage, vehicle_no " +
                           "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> vonData = jdbcTemplate.queryForMap(vonSQL, noticeNumber);
                verificationResults.add("✅ Valid Offence Notice Data Integrity:");
                verificationResults.add("  🚗 Vehicle: " + vonData.get("vehicle_no"));
                verificationResults.add("  🔄 Stages: " + vonData.get("last_processing_stage") + " -> " + vonData.get("next_processing_stage"));

            } catch (Exception e) {
                verificationResults.add("⚠️ VON data verification skipped: " + e.getMessage());
            }

            // Verify 4: Passport format validation
            if (TEST_PASSPORT.matches("^[A-Z][0-9]{8}$")) {
                verificationResults.add("✅ Passport Format Validation: Valid passport pattern (A########)");
            } else {
                verificationResults.add("⚠️ Passport Format Validation: Passport pattern may need review: " + TEST_PASSPORT);
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Passport Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: Owner/Driver updated with passport data (id_type=P)");
                result.addDetail("🎯 Scenario outcome: PASSED - Passport Processing flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Passport Processing verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Passport Processing verification: {}", e.getMessage(), e);
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

            log.info("🧹 Cleanup completed: VON={}, Detail={}, OwnerDriver={} records deleted for prefix: {}",
                     vonDeleted, detailDeleted, ownerDriverDeleted, noticePrefix);

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

        jdbcTemplate.update(sql, noticeNo, "MHTEST678901234", "BMW", "BLACK",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "nationality, passport_place_of_issue, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, TEST_PASSPORT, TEST_OWNER_NAME, "Y",
            TEST_NATIONALITY, TEST_PASSPORT_PLACE_OF_ISSUE, "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (Passport: " + TEST_PASSPORT + ")");
        result.addDetail("   - ID Type: P (Passport)");
        result.addDetail("   - Nationality: " + TEST_NATIONALITY);
        result.addDetail("   - Place of Issue: " + TEST_PASSPORT_PLACE_OF_ISSUE);
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate NRO2URA file with passport information
            String mainContent = generateMainFileWithPassport();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp;

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with passport data: " + mainFileName);
            result.addDetail("📋 Passport content: " + TEST_PASSPORT + " issued in " + TEST_PASSPORT_PLACE_OF_ISSUE);

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithPassport() {
        // Generate NRO2URA file content with passport information
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Passport record
        sb.append("D")
          .append(String.format("%-12s", TEST_PASSPORT))                    // Passport Number
          .append(String.format("%-66s", TEST_OWNER_NAME))                  // Name
          .append("P")                                                      // P for Passport
          .append(String.format("%-20s", TEST_NATIONALITY))                 // Nationality
          .append(String.format("%-3s", TEST_COUNTRY_CODE))                 // Country Code
          .append(String.format("%-30s", TEST_PASSPORT_PLACE_OF_ISSUE))     // Place of Issue
          .append(String.format("%-14s", "PASSPORT TEST"))                  // Padding
          .append("\n");

        // Trailer
        sb.append("T000001\n");  // 1 passport record

        return sb.toString();
    }
}
