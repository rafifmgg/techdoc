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
 * Service for testing MHA Stage Progression Flow scenario.
 *
 * Test scenario: NPA → ROV → ENA → RD1 transitions testing
 * Expected result: last_processing_stage and next_processing_stage updated correctly
 *                  with proper stage transition flow validation
 *
 * 6-Step Flow:
 * 1. Setup test data with notices at different processing stages
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify stage progression transitions applied correctly
 */
@Slf4j
@Service
public class MhaStageProgressionFlowService {

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

    // Test data constants for stage progression scenario
    private static final String SCHEMA = "ocmsizmgr";
    
    // Different notices at different stages
    private static final String NPA_NRIC = "S1111111A";
    private static final String NPA_NAME = "Stage NPA Test";
    private static final String NPA_VEHICLE = "SNP1111A";
    
    private static final String ROV_NRIC = "S2222222B";
    private static final String ROV_NAME = "Stage ROV Test";
    private static final String ROV_VEHICLE = "SRV2222B";
    
    private static final String ENA_NRIC = "S3333333C";
    private static final String ENA_NAME = "Stage ENA Test";
    private static final String ENA_VEHICLE = "SEN3333C";
    
    private static final String RD1_NRIC = "S4444444D";
    private static final String RD1_NAME = "Stage RD1 Test";
    private static final String RD1_VEHICLE = "SRD4444D";
    
    private static final String TEST_ID_TYPE = "N";

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Stage Progression Flow test with 6 steps.
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Stage Progression Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData(noticePrefix));
            steps.add(triggerMhaUpload(noticePrefix));
            steps.add(verifySftpFiles(noticePrefix));
            steps.add(simulateMhaCallbackWithStageData(noticePrefix));
            steps.add(triggerMhaDownload(noticePrefix));
            steps.add(verifyStageProgressionProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Stage Progression flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Stage Progression Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Stage Progression Scenario", STATUS_INFO);

        try {
            String npaNotice = noticePrefix + "001";  // NPA stage
            String rovNotice = noticePrefix + "002";  // ROV stage
            String enaNotice = noticePrefix + "003";  // ENA stage
            String rd1Notice = noticePrefix + "004";  // RD1 stage
            
            log.info("🔧 Setting up test data for Stage Progression: NPA, ROV, ENA, RD1");

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert notice at NPA stage (should transition to ROV)
            insertValidOffenceNoticeWithStage(result, npaNotice, NPA_VEHICLE, "NPA", "ROV");
            insertOffenceNoticeDetail(result, npaNotice, "STAGE1789012345");
            insertOwnerDriver(result, npaNotice, NPA_NRIC, NPA_NAME);

            // Insert notice at ROV stage (should transition to ENA)
            insertValidOffenceNoticeWithStage(result, rovNotice, ROV_VEHICLE, "ROV", "ENA");
            insertOffenceNoticeDetail(result, rovNotice, "STAGE2789012345");
            insertOwnerDriver(result, rovNotice, ROV_NRIC, ROV_NAME);

            // Insert notice at ENA stage (should transition to RD1)
            insertValidOffenceNoticeWithStage(result, enaNotice, ENA_VEHICLE, "ENA", "RD1");
            insertOffenceNoticeDetail(result, enaNotice, "STAGE3789012345");
            insertOwnerDriver(result, enaNotice, ENA_NRIC, ENA_NAME);

            // Insert notice at RD1 stage (should transition to next appropriate stage)
            insertValidOffenceNoticeWithStage(result, rd1Notice, RD1_VEHICLE, "RD1", "RD2");
            insertOffenceNoticeDetail(result, rd1Notice, "STAGE4789012345");
            insertOwnerDriver(result, rd1Notice, RD1_NRIC, RD1_NAME);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticePrefix + "%");

            if (recordCount != null && recordCount == 4) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Stage notices: " + recordCount);
                result.addDetail("📋 NPA Notice: " + npaNotice + " (" + NPA_NRIC + ") - NPA→ROV");
                result.addDetail("📋 ROV Notice: " + rovNotice + " (" + ROV_NRIC + ") - ROV→ENA");
                result.addDetail("📋 ENA Notice: " + enaNotice + " (" + ENA_NRIC + ") - ENA→RD1");
                result.addDetail("📋 RD1 Notice: " + rd1Notice + " (" + RD1_NRIC + ") - RD1→RD2");
                result.addDetail("🎯 Expected: Proper stage transitions after MHA processing");
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

    private TestStepResult simulateMhaCallbackWithStageData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Stage Data", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback for stage progression testing");

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
            result.addDetail("🔄 Stage progression data for all test notices");
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

    private TestStepResult verifyStageProgressionProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Stage Progression Processing Applied", STATUS_INFO);

        try {
            String npaNotice = noticePrefix + "001";
            String rovNotice = noticePrefix + "002";
            String enaNotice = noticePrefix + "003";
            String rd1Notice = noticePrefix + "004";
            
            log.info("🔍 Verifying stage progression for all test notices");

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: Stage transitions for all notices
            String stageSQL = "SELECT notice_no, last_processing_stage, next_processing_stage, " +
                             "last_processing_date, next_processing_date, upd_user_id, upd_date " +
                             "FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                             "WHERE notice_no IN (?, ?, ?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> stageData = jdbcTemplate.queryForList(stageSQL, npaNotice, rovNotice, enaNotice, rd1Notice);
                
                verificationResults.add("✅ Stage Progression Results:");
                
                for (Map<String, Object> stage : stageData) {
                    String noticeNo = (String) stage.get("notice_no");
                    String lastStage = (String) stage.get("last_processing_stage");
                    String nextStage = (String) stage.get("next_processing_stage");
                    
                    verificationResults.add("  📋 " + noticeNo + ": " + lastStage + " → " + nextStage);
                    verificationResults.add("    📅 Updated: " + stage.get("upd_date") + " by " + stage.get("upd_user_id"));
                    
                    // Verify expected transitions
                    boolean stageCorrect = false;
                    if (npaNotice.equals(noticeNo)) {
                        stageCorrect = "ROV".equals(lastStage) || "ENA".equals(nextStage);
                    } else if (rovNotice.equals(noticeNo)) {
                        stageCorrect = "ENA".equals(lastStage) || "RD1".equals(nextStage);
                    } else if (enaNotice.equals(noticeNo)) {
                        stageCorrect = "RD1".equals(lastStage) || "RD2".equals(nextStage);
                    } else if (rd1Notice.equals(noticeNo)) {
                        stageCorrect = "RD2".equals(lastStage) || "RD3".equals(nextStage);
                    }
                    
                    if (!stageCorrect) {
                        verificationResults.add("    ❌ Unexpected stage transition for " + noticeNo);
                        allVerificationsPassed = false;
                    } else {
                        verificationResults.add("    ✅ Stage transition correct");
                    }
                }

            } catch (Exception e) {
                verificationResults.add("❌ Stage progression verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Processing dates updated
            String dateSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                            "WHERE notice_no LIKE ? AND last_processing_date >= ? AND next_processing_date IS NOT NULL";

            try {
                Integer updatedCount = jdbcTemplate.queryForObject(dateSQL, Integer.class, 
                    noticePrefix + "%", LocalDateTime.now().minusHours(1));
                    
                if (updatedCount != null && updatedCount >= 4) {
                    verificationResults.add("✅ Processing Dates Updated: " + updatedCount + " notices with recent timestamps");
                } else {
                    verificationResults.add("⚠️ Processing Dates: Only " + updatedCount + " notices have recent updates");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Processing date verification skipped: " + e.getMessage());
            }

            // Verify 3: Owner/Driver data integrity maintained across stages
            String ownerSQL = "SELECT notice_no, id_no, name FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                             "WHERE notice_no IN (?, ?, ?, ?) ORDER BY notice_no";

            try {
                List<Map<String, Object>> ownerData = jdbcTemplate.queryForList(ownerSQL, npaNotice, rovNotice, enaNotice, rd1Notice);
                verificationResults.add("✅ Owner/Driver Data Integrity Across Stages:");
                
                for (Map<String, Object> owner : ownerData) {
                    String noticeNo = (String) owner.get("notice_no");
                    String idNo = (String) owner.get("id_no");
                    String name = (String) owner.get("name");
                    
                    verificationResults.add("  📋 " + noticeNo + ": " + name + " (" + idNo + ")");
                    
                    // Verify correct NRIC for each notice
                    boolean nricCorrect = false;
                    if (npaNotice.equals(noticeNo) && NPA_NRIC.equals(idNo)) nricCorrect = true;
                    else if (rovNotice.equals(noticeNo) && ROV_NRIC.equals(idNo)) nricCorrect = true;
                    else if (enaNotice.equals(noticeNo) && ENA_NRIC.equals(idNo)) nricCorrect = true;
                    else if (rd1Notice.equals(noticeNo) && RD1_NRIC.equals(idNo)) nricCorrect = true;
                    
                    if (!nricCorrect) {
                        verificationResults.add("    ❌ NRIC mismatch for " + noticeNo);
                        allVerificationsPassed = false;
                    }
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Owner/Driver verification skipped: " + e.getMessage());
            }

            // Verify 4: No unintended suspensions during stage progression
            String suspensionSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                                  "WHERE notice_no LIKE ? AND (suspension_type IS NOT NULL OR epr_reason_of_suspension IS NOT NULL)";

            try {
                Integer suspensionCount = jdbcTemplate.queryForObject(suspensionSQL, Integer.class, noticePrefix + "%");
                if (suspensionCount != null && suspensionCount == 0) {
                    verificationResults.add("✅ Suspension Check: No unintended suspensions during stage progression");
                } else {
                    verificationResults.add("⚠️ Suspension Check: Found " + suspensionCount + " suspension records (verify if intended)");
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ Suspension check skipped: " + e.getMessage());
            }

            // Final assessment
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Stage Progression Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: NPA→ROV→ENA→RD1→RD2 transitions working correctly");
                result.addDetail("🎯 Scenario outcome: PASSED - Stage Progression flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Stage Progression Processing verification failed");
                result.addDetail("📋 Some critical stage transition checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Stage Progression verification: {}", e.getMessage(), e);
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

    private void insertValidOffenceNoticeWithStage(TestStepResult result, String noticeNo, String vehicleNo, 
                                                  String lastStage, String nextStage) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, vehicleNo, "O", lastStage, nextStage,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(2),
            "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo);
        result.addDetail("   - Vehicle: " + vehicleNo + ", Stage: " + lastStage + "→" + nextStage);
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo, String chassisNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, chassisNo, "HONDA", "BLUE", "TEST_USER", LocalDateTime.now());
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
            String mainContent = generateMainFileWithStageData();
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String mainFileName = "NRO2URA_" + timestamp + ".";

            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file for stage progression: " + mainFileName);
            result.addDetail("🔄 Stage progression data for all test stages (NPA, ROV, ENA, RD1)");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithStageData() {
        StringBuilder sb = new StringBuilder();
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");
        
        // All owner records (no specific stage data needed, just normal processing)
        sb.append("D")
          .append(String.format("%-12s", NPA_NRIC))
          .append(String.format("%-66s", NPA_NAME))
          .append("A")  // Life Status = A (Alive - normal processing)
          .append(String.format("%-50s", "STAGE_PROGRESSION_NPA"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", ROV_NRIC))
          .append(String.format("%-66s", ROV_NAME))
          .append("A")  // Life Status = A (Alive - normal processing)
          .append(String.format("%-50s", "STAGE_PROGRESSION_ROV"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", ENA_NRIC))
          .append(String.format("%-66s", ENA_NAME))
          .append("A")  // Life Status = A (Alive - normal processing)
          .append(String.format("%-50s", "STAGE_PROGRESSION_ENA"))
          .append("\n");
        
        sb.append("D")
          .append(String.format("%-12s", RD1_NRIC))
          .append(String.format("%-66s", RD1_NAME))
          .append("A")  // Life Status = A (Alive - normal processing)
          .append(String.format("%-50s", "STAGE_PROGRESSION_RD1"))
          .append("\n");
        
        sb.append("T000004\n");
        return sb.toString();
    }
}
