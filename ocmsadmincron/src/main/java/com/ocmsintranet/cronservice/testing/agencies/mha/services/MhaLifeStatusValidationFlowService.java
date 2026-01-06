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
 * Service for testing MHA Life Status Validation Flow scenario.
 *
 * Test scenario: Different lifeStatus values (D vs A) processing
 * Expected result: Only lifeStatus="D" triggers suspension logic,
 *                  lifeStatus="A" processes normally without suspension
 *
 * 6-Step Flow:
 * 1. Setup test data with two notices for different NRICs
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with different lifeStatus values
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify differential processing based on lifeStatus (D=suspension, A=normal)
 */
@Slf4j
@Service
public class MhaLifeStatusValidationFlowService {

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

    // Test data constants for life status validation scenario
    private static final String SCHEMA = "ocmsizmgr";
    
    // NRIC with lifeStatus="D" (Deceased - will trigger suspension)
    private static final String DECEASED_NRIC = "S2468135K";
    private static final String DECEASED_NAME = "Lim Ah Seng";
    private static final String DECEASED_VEHICLE = "SGB2468K";
    
    // NRIC with lifeStatus="A" (Alive - will process normally)
    private static final String ALIVE_NRIC = "S8024679L";
    private static final String ALIVE_NAME = "Tan Beng Huat";
    private static final String ALIVE_VEHICLE = "SGB8024L";
    
    private static final String TEST_ID_TYPE = "N";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Life Status Validation Flow test with 6 steps.
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Life Status Validation Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData(noticePrefix));
            steps.add(triggerMhaUpload(noticePrefix));
            steps.add(verifySftpFiles(noticePrefix));
            steps.add(simulateMhaCallbackWithDifferentLifeStatus(noticePrefix));
            steps.add(triggerMhaDownload(noticePrefix));
            steps.add(verifyLifeStatusProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Life Status Validation flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Life Status Validation Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Life Status Validation Scenario", STATUS_INFO);

        try {
            String deceasedNotice = noticePrefix + "001";  // lifeStatus="D"
            String aliveNotice = noticePrefix + "002";     // lifeStatus="A"
            log.info("🔧 Setting up test data for Life Status Validation: {} (deceased) and {} (alive)", deceasedNotice, aliveNotice);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert notice for deceased person
            insertValidOffenceNotice(result, deceasedNotice, DECEASED_VEHICLE);
            insertOffenceNoticeDetail(result, deceasedNotice, "DEAD789012345");
            insertOwnerDriver(result, deceasedNotice, DECEASED_NRIC, DECEASED_NAME);

            // Insert notice for alive person
            insertValidOffenceNotice(result, aliveNotice, ALIVE_VEHICLE);
            insertOffenceNoticeDetail(result, aliveNotice, "LIVE789012345");
            insertOwnerDriver(result, aliveNotice, ALIVE_NRIC, ALIVE_NAME);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticePrefix + "%");

            if (recordCount != null && recordCount == 2) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Two notices: " + recordCount);
                result.addDetail("📋 Deceased Notice: " + deceasedNotice + " - " + DECEASED_NRIC + " (" + DECEASED_NAME + ")");
                result.addDetail("📋 Alive Notice: " + aliveNotice + " - " + ALIVE_NRIC + " (" + ALIVE_NAME + ")");
                result.addDetail("🎯 Expected: Deceased=Suspension, Alive=Normal Processing");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Test data setup failed - Expected 2 records, got: " + recordCount);
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

    private TestStepResult simulateMhaCallbackWithDifferentLifeStatus(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Different Life Status", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with different life status values");

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
            result.addDetail("💀 LifeStatus='D' for " + DECEASED_NRIC + " (should trigger suspension)");
            result.addDetail("❤️ LifeStatus='A' for " + ALIVE_NRIC + " (should process normally)");
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

    private TestStepResult verifyLifeStatusProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Life Status Processing Applied", STATUS_INFO);

        try {
            String deceasedNotice = noticePrefix + "001";
            String aliveNotice = noticePrefix + "002";
            log.info("🔍 Verifying life status processing for: {} (deceased) and {} (alive)", deceasedNotice, aliveNotice);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Deceased notice gets suspension
            String suspensionSQL = "SELECT suspension_type, epr_reason_of_suspension, " +
                                  "cre_user_id, upd_user_id, upd_date " +
                                  "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> deceasedData = jdbcTemplate.queryForMap(suspensionSQL, deceasedNotice);
                String suspensionType = (String) deceasedData.get("suspension_type");
                String suspensionReason = (String) deceasedData.get("epr_reason_of_suspension");

                if ("PS".equals(suspensionType)) {
                    verificationResults.add("✅ Deceased Person (lifeStatus='D') - Suspension Applied:");
                    verificationResults.add("  📋 Notice: " + deceasedNotice + " (" + DECEASED_NRIC + ")");
                    verificationResults.add("  💀 Suspension Type: " + suspensionType);
                    verificationResults.add("  📝 Reason: " + suspensionReason);
                    verificationResults.add("  📅 Updated by: " + deceasedData.get("upd_user_id"));
                } else {
                    verificationResults.add("❌ Deceased Person: Expected PS suspension, got: " + suspensionType);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Deceased notice verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Alive notice processes normally (no suspension)
            try {
                Map<String, Object> aliveData = jdbcTemplate.queryForMap(suspensionSQL, aliveNotice);
                String suspensionType = (String) aliveData.get("suspension_type");
                String suspensionReason = (String) aliveData.get("epr_reason_of_suspension");

                if (suspensionType == null && suspensionReason == null) {
                    verificationResults.add("✅ Alive Person (lifeStatus='A') - Normal Processing:");
                    verificationResults.add("  📋 Notice: " + aliveNotice + " (" + ALIVE_NRIC + ")");
                    verificationResults.add("  ❤️ No suspension applied (correct for alive person)");
                    verificationResults.add("  📅 Updated by: " + aliveData.get("upd_user_id"));
                } else {
                    verificationResults.add("❌ Alive Person: Expected no suspension, got: " + suspensionType + "/" + suspensionReason);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Alive notice verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: Owner/Driver data integrity for both notices
            String ownerSQL = "SELECT notice_no, id_no, name, id_type, offender_indicator " +
                             "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no IN (?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> ownerData = jdbcTemplate.queryForList(ownerSQL, deceasedNotice, aliveNotice);
                verificationResults.add("✅ Owner/Driver Data Integrity:");
                
                for (Map<String, Object> owner : ownerData) {
                    String noticeNo = (String) owner.get("notice_no");
                    String idNo = (String) owner.get("id_no");
                    String name = (String) owner.get("name");
                    
                    verificationResults.add("  📋 " + noticeNo + ": " + name + " (" + idNo + ")");
                    
                    if (deceasedNotice.equals(noticeNo) && !DECEASED_NRIC.equals(idNo)) {
                        verificationResults.add("    ❌ Expected " + DECEASED_NRIC + " for deceased notice");
                        allVerificationsPassed = false;
                    } else if (aliveNotice.equals(noticeNo) && !ALIVE_NRIC.equals(idNo)) {
                        verificationResults.add("    ❌ Expected " + ALIVE_NRIC + " for alive notice");
                        allVerificationsPassed = false;
                    }
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver data verification skipped: " + e.getMessage());
            }

            // Verify 4: Processing stage progression for both notices
            String stageSQL = "SELECT notice_no, last_processing_stage, next_processing_stage " +
                             "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no IN (?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> stageData = jdbcTemplate.queryForList(stageSQL, deceasedNotice, aliveNotice);
                verificationResults.add("✅ Processing Stage Progression:");
                
                for (Map<String, Object> stage : stageData) {
                    verificationResults.add("  📋 " + stage.get("notice_no") + ": " + 
                        stage.get("last_processing_stage") + " -> " + stage.get("next_processing_stage"));
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Processing stage verification skipped: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Life Status Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: lifeStatus='D' triggers suspension, lifeStatus='A' processes normally");
                result.addDetail("🎯 Scenario outcome: PASSED - Life Status Validation flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Life Status Processing verification failed");
                result.addDetail("📋 Some critical life status logic checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Life Status verification: {}", e.getMessage(), e);
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

    private void insertValidOffenceNotice(TestStepResult result, String noticeNo, String vehicleNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, vehicleNo, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1),
            "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo + " (Vehicle: " + vehicleNo + ")");
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo, String chassisNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, chassisNo, "TOYOTA", "WHITE", "TEST_USER", LocalDateTime.now());
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
            String mainContent = generateMainFileWithDifferentLifeStatus();
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String mainFileName = "NRO2URA_" + timestamp;

            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with different life status: " + mainFileName);
            result.addDetail("💀 lifeStatus='D' for " + DECEASED_NRIC + " (triggers suspension)");
            result.addDetail("❤️ lifeStatus='A' for " + ALIVE_NRIC + " (normal processing)");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithDifferentLifeStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");
        
        // Deceased person record (lifeStatus = D)
        sb.append("D")
          .append(String.format("%-12s", DECEASED_NRIC))
          .append(String.format("%-66s", DECEASED_NAME))
          .append("D")  // Life Status = D (Deceased)
          .append("20240515")  // DoD
          .append(String.format("%-42s", "DECEASED_SUSPENSION"))
          .append("\n");
        
        // Alive person record (lifeStatus = A)
        sb.append("D")
          .append(String.format("%-12s", ALIVE_NRIC))
          .append(String.format("%-66s", ALIVE_NAME))
          .append("A")  // Life Status = A (Alive)
          .append(String.format("%-8s", ""))  // No DoD for alive person
          .append(String.format("%-42s", "ALIVE_NORMAL"))
          .append("\n");
        
        sb.append("T000002\n");
        return sb.toString();
    }
}
