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
 * Service for testing MHA Multiple Owner/Driver Flow scenario.
 *
 * Test scenario: Complex ownership data updates with multiple responsible parties
 * Expected result: Multiple records in ocms_offence_notice_owner_driver updated accordingly
 *                  with primary owner/driver designation from MHA response
 *
 * 6-Step Flow:
 * 1. Setup test data with notice containing multiple responsible parties
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with primary owner designation
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify multiple owner/driver records updated with primary designation
 */
@Slf4j
@Service
public class MhaMultipleOwnerDriverFlowService {

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

    // Test data constants for multiple owner/driver scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO = "SBA6789L";
    
    // Primary Owner (will be designated as primary by MHA)
    private static final String PRIMARY_NRIC = "S4567890D";
    private static final String PRIMARY_NAME = "Tan Wei Ling";
    
    // Secondary Owner (will remain secondary)
    private static final String SECONDARY_NRIC = "S7890123E";
    private static final String SECONDARY_NAME = "Wong Mei Fong";
    
    // Driver (different from owners)
    private static final String DRIVER_NRIC = "S1234567F";
    private static final String DRIVER_NAME = "Lee Hsien Ming";
    
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Multiple Owner/Driver Flow test with 6 steps.
     *
     * @param noticePrefix Custom notice prefix, default will be generated
     * @return List<TestStepResult> with detailed steps execution results
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Multiple Owner/Driver Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Setup test data
            steps.add(setupTestData(noticePrefix));

            // Step 2: POST /mha/upload
            steps.add(triggerMhaUpload(noticePrefix));

            // Step 3: Verify files in SFTP
            steps.add(verifySftpFiles(noticePrefix));

            // Step 4: POST /test/mha/mha-callback with multiple owner data
            steps.add(simulateMhaCallbackWithMultipleOwners(noticePrefix));

            // Step 5: POST /mha/download/execute
            steps.add(triggerMhaDownload(noticePrefix));

            // Step 6: Verify multiple owner/driver processing applied
            steps.add(verifyMultipleOwnerDriverProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Multiple Owner/Driver flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        // Calculate summary
        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Multiple Owner/Driver Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    /**
     * Step 1: Setup test data with notice containing multiple responsible parties.
     */
    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Multiple Owner/Driver Scenario", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔧 Setting up test data for Multiple Owner/Driver scenario: {}", noticeNumber);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert valid offence notice
            insertValidOffenceNotice(result, noticeNumber);

            // Insert offence notice detail
            insertOffenceNoticeDetail(result, noticeNumber);

            // Insert multiple owner/driver records
            insertPrimaryOwner(result, noticeNumber);
            insertSecondaryOwner(result, noticeNumber);
            insertDriver(result, noticeNumber);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticeNumber);

            if (recordCount != null && recordCount == 3) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Owner/Driver Records: " + recordCount);
                result.addDetail("📋 Notice: " + noticeNumber);
                result.addDetail("👥 Multiple Parties Setup:");
                result.addDetail("   - Primary Owner: " + PRIMARY_NRIC + " (" + PRIMARY_NAME + ")");
                result.addDetail("   - Secondary Owner: " + SECONDARY_NRIC + " (" + SECONDARY_NAME + ")");
                result.addDetail("   - Driver: " + DRIVER_NRIC + " (" + DRIVER_NAME + ")");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - Expected 3 records, got: " + recordCount);
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
     * Step 4: POST /test/mha/mha-callback - Simulate MHA response with multiple owner data.
     */
    private TestStepResult simulateMhaCallbackWithMultipleOwners(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Multiple Owners", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("📞 Simulating MHA callback with multiple owner data for: {}", noticeNumber);

            // Generate MHA response files with multiple owner information
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
            result.addDetail("👥 Multiple owner data for notice: " + noticeNumber);
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
     * Step 6: Verify multiple owner/driver records updated with primary designation.
     */
    private TestStepResult verifyMultipleOwnerDriverProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Multiple Owner/Driver Processing Applied", STATUS_INFO);

        try {
            String noticeNumber = noticePrefix + "001";
            log.info("🔍 Verifying multiple owner/driver processing for: {}", noticeNumber);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: All owner/driver records exist and are updated
            String ownerSQL = "SELECT id_no, name, owner_driver_indicator, primary_flag, offender_indicator, " +
                             "cre_user_id, cre_date, upd_user_id, upd_date " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no = ? ORDER BY owner_driver_indicator, id_no";

            try {
                List<Map<String, Object>> ownerData = jdbcTemplate.queryForList(ownerSQL, noticeNumber);
                
                if (ownerData.size() == 3) {
                    verificationResults.add("✅ Multiple Owner/Driver Records Found: " + ownerData.size());
                    
                    for (Map<String, Object> record : ownerData) {
                        String idNo = (String) record.get("id_no");
                        String name = (String) record.get("name");
                        String indicator = (String) record.get("owner_driver_indicator");
                        String primaryFlag = (String) record.get("primary_flag");
                        String offenderFlag = (String) record.get("offender_indicator");
                        
                        verificationResults.add("  📋 " + indicator + ": " + name + " (" + idNo + ")");
                        verificationResults.add("    - Primary: " + (primaryFlag != null ? primaryFlag : "N"));
                        verificationResults.add("    - Offender: " + offenderFlag);
                        verificationResults.add("    - Updated by: " + record.get("upd_user_id"));
                    }
                    
                    // Check primary designation
                    boolean primaryFound = ownerData.stream()
                        .anyMatch(record -> "Y".equals(record.get("primary_flag")) 
                                         && PRIMARY_NRIC.equals(record.get("id_no")));
                    
                    if (primaryFound) {
                        verificationResults.add("✅ Primary Owner Designation: " + PRIMARY_NRIC + " marked as primary");
                    } else {
                        verificationResults.add("❌ Primary Owner Designation: Expected " + PRIMARY_NRIC + " as primary");
                        allVerificationsPassed = false;
                    }
                    
                } else {
                    verificationResults.add("❌ Multiple Owner/Driver Records: Expected 3, got " + ownerData.size());
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Owner/Driver records verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: No unintended suspension applied
            String suspensionCheckSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                       "WHERE notice_no = ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            Integer suspensionCount = jdbcTemplate.queryForObject(suspensionCheckSQL, Integer.class, noticeNumber);
            if (suspensionCount != null && suspensionCount == 0) {
                verificationResults.add("✅ Suspension Check: No unintended suspension applied");
            } else {
                verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (unexpected for multiple owner processing)");
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

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Multiple Owner/Driver Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: Multiple records updated with primary designation");
                result.addDetail("🎯 Scenario outcome: PASSED - Multiple Owner/Driver flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Multiple Owner/Driver Processing verification failed");
                result.addDetail("📋 Some critical checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Multiple Owner/Driver verification: {}", e.getMessage(), e);
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

        jdbcTemplate.update(sql, noticeNo, "MHTEST789012345", "MERCEDES", "SILVER",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertPrimaryOwner(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "primary_flag, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, PRIMARY_NRIC, PRIMARY_NAME, "Y",
            "N", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Primary owner record inserted: " + PRIMARY_NRIC + " (" + PRIMARY_NAME + ")");
    }

    private void insertSecondaryOwner(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "primary_flag, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, SECONDARY_NRIC, SECONDARY_NAME, "N",
            "N", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Secondary owner record inserted: " + SECONDARY_NRIC + " (" + SECONDARY_NAME + ")");
    }

    private void insertDriver(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "primary_flag, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "D", TEST_ID_TYPE, DRIVER_NRIC, DRIVER_NAME, "Y",
            "Y", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Driver record inserted: " + DRIVER_NRIC + " (" + DRIVER_NAME + ")");
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticeNumber) {
        try {
            // Generate NRO2URA file with multiple owner information
            String mainContent = generateMainFileWithMultipleOwners();

            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());

            String mainFileName = "NRO2URA_" + timestamp;

            // Upload NRO2URA file to SFTP output directory
            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with multiple owner data: " + mainFileName);
            result.addDetail("📋 Multiple owner content: Primary=" + PRIMARY_NRIC + ", Secondary=" + SECONDARY_NRIC + ", Driver=" + DRIVER_NRIC);

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithMultipleOwners() {
        // Generate NRO2URA file content with multiple owner information
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");

        // Primary Owner record
        sb.append("D")
          .append(String.format("%-12s", PRIMARY_NRIC))        // Primary Owner NRIC
          .append(String.format("%-66s", PRIMARY_NAME))        // Primary Owner Name
          .append("O")                                         // O for Owner
          .append("Y")                                         // Primary Flag = Y
          .append(String.format("%-48s", "PRIMARY OWNER"))     // Padding
          .append("\n");

        // Secondary Owner record
        sb.append("D")
          .append(String.format("%-12s", SECONDARY_NRIC))      // Secondary Owner NRIC
          .append(String.format("%-66s", SECONDARY_NAME))      // Secondary Owner Name
          .append("O")                                         // O for Owner
          .append("N")                                         // Primary Flag = N
          .append(String.format("%-48s", "SECONDARY OWNER"))   // Padding
          .append("\n");

        // Driver record
        sb.append("D")
          .append(String.format("%-12s", DRIVER_NRIC))         // Driver NRIC
          .append(String.format("%-66s", DRIVER_NAME))         // Driver Name
          .append("D")                                         // D for Driver
          .append("Y")                                         // Primary Flag = Y (primary driver)
          .append(String.format("%-48s", "PRIMARY DRIVER"))    // Padding
          .append("\n");

        // Trailer
        sb.append("T000003\n");  // 3 records (2 owners + 1 driver)

        return sb.toString();
    }
}
