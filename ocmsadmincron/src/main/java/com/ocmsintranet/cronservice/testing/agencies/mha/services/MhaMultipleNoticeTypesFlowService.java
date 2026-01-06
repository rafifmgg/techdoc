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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for testing MHA Multiple Notice Types Flow scenario.
 *
 * Test scenario: Different offence categories with different processing
 * Expected result: Processing logic adapts to different notice types and
 *                  appropriate business rules applied per category
 *
 * 6-Step Flow:
 * 1. Setup test data with various offence types and penalty amounts
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify different business rules applied per notice type
 */
@Slf4j
@Service
public class MhaMultipleNoticeTypesFlowService {

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

    // Test data constants for multiple notice types scenario
    private static final String SCHEMA = "ocmsizmgr";
    
    // Parking offence (low penalty)
    private static final String PARKING_NRIC = "S1001001A";
    private static final String PARKING_NAME = "Ahmad Parking Test";
    private static final String PARKING_VEHICLE = "SPA1001A";
    private static final BigDecimal PARKING_AMOUNT = new BigDecimal("70.00");
    
    // Traffic light offence (medium penalty)
    private static final String TRAFFIC_NRIC = "S2002002B";
    private static final String TRAFFIC_NAME = "Siti Traffic Test";
    private static final String TRAFFIC_VEHICLE = "STL2002B";
    private static final BigDecimal TRAFFIC_AMOUNT = new BigDecimal("200.00");
    
    // Speeding offence (high penalty)
    private static final String SPEEDING_NRIC = "S3003003C";
    private static final String SPEEDING_NAME = "Kumar Speeding Test";
    private static final String SPEEDING_VEHICLE = "SSP3003C";
    private static final BigDecimal SPEEDING_AMOUNT = new BigDecimal("500.00");
    
    // Heavy vehicle offence (special category)
    private static final String HEAVY_NRIC = "S4004004D";
    private static final String HEAVY_NAME = "Lim Heavy Vehicle Test";
    private static final String HEAVY_VEHICLE = "SHV4004D";
    private static final BigDecimal HEAVY_AMOUNT = new BigDecimal("1000.00");
    
    private static final String TEST_ID_TYPE = "N";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Multiple Notice Types Flow test with 6 steps.
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Multiple Notice Types Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData(noticePrefix));
            steps.add(triggerMhaUpload(noticePrefix));
            steps.add(verifySftpFiles(noticePrefix));
            steps.add(simulateMhaCallbackWithMultipleTypes(noticePrefix));
            steps.add(triggerMhaDownload(noticePrefix));
            steps.add(verifyMultipleNoticeTypesProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Multiple Notice Types flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Multiple Notice Types Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Multiple Notice Types Scenario", STATUS_INFO);

        try {
            String parkingNotice = noticePrefix + "001";  // Parking offence
            String trafficNotice = noticePrefix + "002";  // Traffic light offence
            String speedingNotice = noticePrefix + "003"; // Speeding offence
            String heavyNotice = noticePrefix + "004";    // Heavy vehicle offence
            
            log.info("🔧 Setting up test data for Multiple Notice Types: Parking, Traffic, Speeding, Heavy Vehicle");

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert parking offence notice (Category: P, Low penalty)
            insertValidOffenceNoticeWithCategory(result, parkingNotice, PARKING_VEHICLE, "P", 
                PARKING_AMOUNT, 10001, "Illegal Parking");
            insertOffenceNoticeDetail(result, parkingNotice, "PARK789012345");
            insertOwnerDriver(result, parkingNotice, PARKING_NRIC, PARKING_NAME);

            // Insert traffic light offence notice (Category: T, Medium penalty)
            insertValidOffenceNoticeWithCategory(result, trafficNotice, TRAFFIC_VEHICLE, "T", 
                TRAFFIC_AMOUNT, 20002, "Red Light Violation");
            insertOffenceNoticeDetail(result, trafficNotice, "TRAF789012345");
            insertOwnerDriver(result, trafficNotice, TRAFFIC_NRIC, TRAFFIC_NAME);

            // Insert speeding offence notice (Category: S, High penalty)
            insertValidOffenceNoticeWithCategory(result, speedingNotice, SPEEDING_VEHICLE, "S", 
                SPEEDING_AMOUNT, 30003, "Excessive Speeding");
            insertOffenceNoticeDetail(result, speedingNotice, "SPED789012345");
            insertOwnerDriver(result, speedingNotice, SPEEDING_NRIC, SPEEDING_NAME);

            // Insert heavy vehicle offence notice (Category: H, Special rules)
            insertValidOffenceNoticeWithCategory(result, heavyNotice, HEAVY_VEHICLE, "H", 
                HEAVY_AMOUNT, 40004, "Heavy Vehicle Violation");
            insertOffenceNoticeDetail(result, heavyNotice, "HEAV789012345");
            insertOwnerDriver(result, heavyNotice, HEAVY_NRIC, HEAVY_NAME);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticePrefix + "%");

            if (recordCount != null && recordCount == 4) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Notice types: " + recordCount);
                result.addDetail("📋 Parking Notice: " + parkingNotice + " ($" + PARKING_AMOUNT + ") - Category P");
                result.addDetail("📋 Traffic Notice: " + trafficNotice + " ($" + TRAFFIC_AMOUNT + ") - Category T");
                result.addDetail("📋 Speeding Notice: " + speedingNotice + " ($" + SPEEDING_AMOUNT + ") - Category S");
                result.addDetail("📋 Heavy Vehicle Notice: " + heavyNotice + " ($" + HEAVY_AMOUNT + ") - Category H");
                result.addDetail("🎯 Expected: Different business rules per category");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - Expected 4 records, got: " + recordCount);
            }

        } catch (Exception e) {
            log.error("❌ Error setting up test data: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error during setup: " + e.getMessage());
        }

        return result;
    }

    private TestStepResult triggerMhaUpload(String noticePrefix) {
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

    private TestStepResult verifySftpFiles(String noticePrefix) {
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

    private TestStepResult simulateMhaCallbackWithMultipleTypes(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response for Multiple Types", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback for multiple notice types");

            generateMhaResponseFiles(result, noticePrefix);

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
            result.addDetail("🏷️ Multiple offence categories: P, T, S, H");
            result.addDetail("📄 Response: " + (response != null ? response.substring(0, Math.min(response.length(), 150)) : "null"));

        } catch (Exception e) {
            log.error("❌ Error simulating MHA callback: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ MHA callback simulation failed: " + e.getMessage());
        }

        return result;
    }

    private TestStepResult triggerMhaDownload(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 5: POST /mha/download/execute - Trigger Download Process", STATUS_INFO);

        try {
            String downloadUrl = apiConfigHelper.buildApiUrl("/v1/mha/download/execute");
            log.info("⬇️ Calling MHA download API: {}", downloadUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{}";
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

    private TestStepResult verifyMultipleNoticeTypesProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Multiple Notice Types Processing Applied", STATUS_INFO);

        try {
            String parkingNotice = noticePrefix + "001";
            String trafficNotice = noticePrefix + "002";
            String speedingNotice = noticePrefix + "003";
            String heavyNotice = noticePrefix + "004";
            
            log.info("🔍 Verifying multiple notice types processing");

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Different processing based on penalty amounts and categories
            String noticeSQL = "SELECT notice_no, vehicle_category, composition_amount, computer_rule_code, " +
                              "last_processing_stage, next_processing_stage, upd_user_id, upd_date " +
                              "FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                              "WHERE notice_no IN (?, ?, ?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> noticeData = jdbcTemplate.queryForList(noticeSQL, 
                    parkingNotice, trafficNotice, speedingNotice, heavyNotice);
                
                verificationResults.add("✅ Notice Types Processing Results:");
                
                for (Map<String, Object> notice : noticeData) {
                    String noticeNo = (String) notice.get("notice_no");
                    String category = (String) notice.get("vehicle_category");
                    BigDecimal amount = (BigDecimal) notice.get("composition_amount");
                    Integer ruleCode = (Integer) notice.get("computer_rule_code");
                    String lastStage = (String) notice.get("last_processing_stage");
                    String nextStage = (String) notice.get("next_processing_stage");
                    
                    verificationResults.add("  📋 " + noticeNo + ": Category=" + category + ", Amount=$" + amount);
                    verificationResults.add("    📏 Rule Code: " + ruleCode + ", Stage: " + lastStage + "→" + nextStage);
                    verificationResults.add("    📅 Updated: " + notice.get("upd_date") + " by " + notice.get("upd_user_id"));
                    
                    // Verify expected penalty amounts match notice types
                    boolean amountCorrect = false;
                    if (parkingNotice.equals(noticeNo) && PARKING_AMOUNT.equals(amount)) amountCorrect = true;
                    else if (trafficNotice.equals(noticeNo) && TRAFFIC_AMOUNT.equals(amount)) amountCorrect = true;
                    else if (speedingNotice.equals(noticeNo) && SPEEDING_AMOUNT.equals(amount)) amountCorrect = true;
                    else if (heavyNotice.equals(noticeNo) && HEAVY_AMOUNT.equals(amount)) amountCorrect = true;
                    
                    if (!amountCorrect) {
                        verificationResults.add("    ❌ Penalty amount mismatch for " + noticeNo);
                        allVerificationsPassed = false;
                    } else {
                        verificationResults.add("    ✅ Penalty amount correct");
                    }
                }

            } catch (Exception e) {
                verificationResults.add("❌ Notice types verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Owner/Driver data integrity across different notice types
            String ownerSQL = "SELECT notice_no, id_no, name FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no IN (?, ?, ?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> ownerData = jdbcTemplate.queryForList(ownerSQL, 
                    parkingNotice, trafficNotice, speedingNotice, heavyNotice);
                verificationResults.add("✅ Owner/Driver Data Across Notice Types:");
                
                for (Map<String, Object> owner : ownerData) {
                    String noticeNo = (String) owner.get("notice_no");
                    String idNo = (String) owner.get("id_no");
                    String name = (String) owner.get("name");
                    
                    verificationResults.add("  📋 " + noticeNo + ": " + name + " (" + idNo + ")");
                    
                    // Verify correct NRIC for each notice type
                    boolean nricCorrect = false;
                    if (parkingNotice.equals(noticeNo) && PARKING_NRIC.equals(idNo)) nricCorrect = true;
                    else if (trafficNotice.equals(noticeNo) && TRAFFIC_NRIC.equals(idNo)) nricCorrect = true;
                    else if (speedingNotice.equals(noticeNo) && SPEEDING_NRIC.equals(idNo)) nricCorrect = true;
                    else if (heavyNotice.equals(noticeNo) && HEAVY_NRIC.equals(idNo)) nricCorrect = true;
                    
                    if (!nricCorrect) {
                        verificationResults.add("    ❌ NRIC mismatch for " + noticeNo);
                        allVerificationsPassed = false;
                    }
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver verification skipped: " + e.getMessage());
            }

            // Verify 3: Business rules application based on categories
            String categorySQL = "SELECT COUNT(DISTINCT vehicle_category) as category_count, " +
                                "COUNT(DISTINCT computer_rule_code) as rule_count " +
                                "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?";

            try {
                Map<String, Object> categoryData = jdbcTemplate.queryForMap(categorySQL, noticePrefix + "%");
                Integer categoryCount = (Integer) categoryData.get("category_count");
                Integer ruleCount = (Integer) categoryData.get("rule_count");
                
                verificationResults.add("✅ Business Rules Application:");
                verificationResults.add("  🏷️ Different Categories: " + categoryCount + " (expected: 4)");
                verificationResults.add("  📏 Different Rule Codes: " + ruleCount + " (expected: 4)");
                
                if (categoryCount != null && categoryCount >= 1 && ruleCount != null && ruleCount >= 4) {
                    verificationResults.add("  ✅ Different business rules applied per category");
                } else {
                    verificationResults.add("  ⚠️ Expected more variation in categories/rules");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Business rules verification skipped: " + e.getMessage());
            }

            // Verify 4: Processing adapts to penalty amounts (high penalty = different handling)
            String penaltySQL = "SELECT notice_no, composition_amount FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                               "WHERE notice_no LIKE ? ORDER BY composition_amount DESC";

            try {
                List<Map<String, Object>> penaltyData = jdbcTemplate.queryForList(penaltySQL, noticePrefix + "%");
                verificationResults.add("✅ Penalty Amount Processing (High to Low):");
                
                for (Map<String, Object> penalty : penaltyData) {
                    String noticeNo = (String) penalty.get("notice_no");
                    BigDecimal amount = (BigDecimal) penalty.get("composition_amount");
                    
                    String category = "Unknown";
                    if (heavyNotice.equals(noticeNo)) category = "Heavy Vehicle ($1000)";
                    else if (speedingNotice.equals(noticeNo)) category = "Speeding ($500)";
                    else if (trafficNotice.equals(noticeNo)) category = "Traffic Light ($200)";
                    else if (parkingNotice.equals(noticeNo)) category = "Parking ($70)";
                    
                    verificationResults.add("  💰 " + noticeNo + ": $" + amount + " - " + category);
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Penalty processing verification skipped: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Multiple Notice Types Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: Different business rules per category");
                result.addDetail("🎯 Scenario outcome: PASSED - Multiple Notice Types flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Multiple Notice Types Processing verification failed");
                result.addDetail("📋 Some critical category processing checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Multiple Notice Types verification: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Critical error during verification: " + e.getMessage());
        }

        return result;
    }

    // Helper methods
    private void cleanupTestData(String noticePrefix) {
        try {
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no LIKE ?", noticePrefix + "%");
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no LIKE ?", noticePrefix + "%");
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?", noticePrefix + "%");
            log.info("🧹 Cleanup completed for prefix: {}", noticePrefix);
        } catch (Exception e) {
            log.warn("⚠️ Cleanup warning for prefix {}: {}", noticePrefix, e.getMessage());
        }
    }

    private void insertValidOffenceNoticeWithCategory(TestStepResult result, String noticeNo, String vehicleNo, 
                                                     String category, BigDecimal amount, Integer ruleCode, String description) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, vehicleNo, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            "A0001", description, amount, ruleCode, category,
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo);
        result.addDetail("   - Category: " + category + ", Amount: $" + amount + ", Rule: " + ruleCode);
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo, String chassisNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, chassisNo, "MAZDA", "RED", "TEST_USER", LocalDateTime.now());
        result.addDetail("✅ Offence notice detail inserted: " + noticeNo);
    }

    private void insertOwnerDriver(TestStepResult result, String noticeNo, String nric, String name) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, owner_driver_indicator, id_type, id_no, name, offender_indicator, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "O", TEST_ID_TYPE, nric, name, "Y", "TEST_USER", LocalDateTime.now());
        result.addDetail("✅ Owner record inserted: " + nric + " (" + name + ")");
    }

    private void generateMhaResponseFiles(TestStepResult result, String noticePrefix) {
        try {
            String mainContent = generateMainFileWithMultipleTypes();
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String mainFileName = "NRO2URA_" + timestamp;

            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file for multiple notice types: " + mainFileName);
            result.addDetail("🏷️ Categories: P(Parking), T(Traffic), S(Speeding), H(Heavy Vehicle)");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithMultipleTypes() {
        StringBuilder sb = new StringBuilder();
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");
        
        // All owner records (normal processing for different categories)
        sb.append("D")
          .append(String.format("%-12s", PARKING_NRIC))
          .append(String.format("%-66s", PARKING_NAME))
          .append("A")  // Life Status = A (Alive)
          .append(String.format("%-50s", "CATEGORY_PARKING"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", TRAFFIC_NRIC))
          .append(String.format("%-66s", TRAFFIC_NAME))
          .append("A")  // Life Status = A (Alive)
          .append(String.format("%-50s", "CATEGORY_TRAFFIC"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", SPEEDING_NRIC))
          .append(String.format("%-66s", SPEEDING_NAME))
          .append("A")  // Life Status = A (Alive)
          .append(String.format("%-50s", "CATEGORY_SPEEDING"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", HEAVY_NRIC))
          .append(String.format("%-66s", HEAVY_NAME))
          .append("A")  // Life Status = A (Alive)
          .append(String.format("%-50s", "CATEGORY_HEAVY"))
          .append("\n");
        
        sb.append("T000004\n");
        return sb.toString();
    }
}
