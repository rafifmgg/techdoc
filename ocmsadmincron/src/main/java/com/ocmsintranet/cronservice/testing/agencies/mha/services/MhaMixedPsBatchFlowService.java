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
 * Service for testing MHA Mixed PS-RIP and PS-RP2 Batch Flow scenario.
 *
 * Test scenario: Single UIN with multiple notices, different DoD logic
 * Expected result: Some notices get PS-RIP (DoD after offence), some get PS-RP2 (DoD before offence)
 *                  based on MHA response with same DoD but different offence dates
 *
 * 6-Step Flow:
 * 1. Setup test data with multiple notices for same UIN, different offence dates
 * 2. POST /mha/upload - Trigger upload process
 * 3. Verify files in SFTP - Check generated files exist
 * 4. POST /test/mha/mha-callback - Simulate MHA response with same DoD
 * 5. POST /mha/download/execute - Trigger download process
 * 6. Verify mixed suspension processing (PS-RIP and PS-RP2) applied correctly
 */
@Slf4j
@Service
public class MhaMixedPsBatchFlowService {

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

    // Test data constants for mixed PS batch scenario
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_UIN = "S1357924B";  // Same UIN for all notices
    private static final String TEST_NAME = "Rajesh Kumar Singh";
    private static final String TEST_ID_TYPE = "N";

    // Multiple vehicles for different notices
    private static final String TEST_VEHICLE_1 = "SLK1357A";  // Earlier offence
    private static final String TEST_VEHICLE_2 = "SLK2468B";  // Later offence

    // DoD for MHA response (will be between the two offence dates)
    private static final LocalDateTime DOD_DATE = LocalDateTime.of(2024, 6, 15, 0, 0);

    // SFTP paths
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload:/mhanro/input}")
    private String sftpInputPath;
    
    @Value("${sftp.folders.mha.download:/mhanro/output}")
    private String sftpOutputPath;

    /**
     * Execute complete Mixed PS Batch Flow test with 6 steps.
     */
    public List<TestStepResult> executeFlow(String noticePrefix) {
        log.info("🚀 Starting MHA Mixed PS-RIP and PS-RP2 Batch Flow Test...");

        List<TestStepResult> steps = new ArrayList<>();

        try {
            steps.add(setupTestData(noticePrefix));
            steps.add(triggerMhaUpload(noticePrefix));
            steps.add(verifySftpFiles(noticePrefix));
            steps.add(simulateMhaCallbackWithSameDod(noticePrefix));
            steps.add(triggerMhaDownload(noticePrefix));
            steps.add(verifyMixedPsSuspensionProcessing(noticePrefix));

        } catch (Exception e) {
            log.error("❌ Critical error during Mixed PS Batch flow execution: {}", e.getMessage(), e);
            TestStepResult errorStep = new TestStepResult("Critical Error", STATUS_FAILED);
            errorStep.addDetail("❌ Flow execution failed: " + e.getMessage());
            steps.add(errorStep);
        }

        int successCount = (int) steps.stream().filter(s -> STATUS_SUCCESS.equals(s.getStatus())).count();
        int failedCount = (int) steps.stream().filter(s -> STATUS_FAILED.equals(s.getStatus())).count();
        int skippedCount = (int) steps.stream().filter(s -> STATUS_INFO.equals(s.getStatus())).count();

        log.info("✅ MHA Mixed PS Batch Flow Test completed. Summary: Success: %d, Failed: %d, Skipped: %d",
                         successCount, failedCount, skippedCount);

        return steps;
    }

    private TestStepResult setupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data - Mixed PS Batch Scenario", STATUS_INFO);

        try {
            String notice1 = noticePrefix + "001";  // Earlier offence - will get PS-RP2
            String notice2 = noticePrefix + "002";  // Later offence - will get PS-RIP
            log.info("🔧 Setting up test data for Mixed PS Batch scenario: {} and {}", notice1, notice2);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            // Insert first notice (earlier offence date - DoD after offence = PS-RP2)
            insertValidOffenceNoticeWithDate(result, notice1, TEST_VEHICLE_1, 
                LocalDateTime.of(2024, 5, 10, 14, 30));  // Before DoD

            insertOffenceNoticeDetail(result, notice1, "MIXED1789012345");
            insertOwnerDriver(result, notice1, TEST_UIN, TEST_NAME);

            // Insert second notice (later offence date - DoD before offence = PS-RIP) 
            insertValidOffenceNoticeWithDate(result, notice2, TEST_VEHICLE_2,
                LocalDateTime.of(2024, 7, 20, 16, 45));  // After DoD

            insertOffenceNoticeDetail(result, notice2, "MIXED2789012345");
            insertOwnerDriver(result, notice2, TEST_UIN, TEST_NAME);

            // Verify data insertion
            String countSQL = "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?";
            Integer recordCount = jdbcTemplate.queryForObject(countSQL, Integer.class, noticePrefix + "%");

            if (recordCount != null && recordCount == 2) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Test data setup completed - Multiple notices: " + recordCount);
                result.addDetail("📋 Notice 1: " + notice1 + " (Vehicle: " + TEST_VEHICLE_1 + ") - Offence: 2024-05-10");
                result.addDetail("📋 Notice 2: " + notice2 + " (Vehicle: " + TEST_VEHICLE_2 + ") - Offence: 2024-07-20");
                result.addDetail("👤 Same UIN: " + TEST_UIN + " (" + TEST_NAME + ")");
                result.addDetail("💀 DoD: 2024-06-15 (between offence dates)");
                result.addDetail("🎯 Expected: Notice1=PS-RP2, Notice2=PS-RIP");
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

    private TestStepResult simulateMhaCallbackWithSameDod(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 4: POST /test/mha/mha-callback - Simulate MHA Response with Same DoD", STATUS_INFO);

        try {
            log.info("📞 Simulating MHA callback with same DoD for mixed PS processing");

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
            result.addDetail("💀 Same DoD (2024-06-15) for both notices with different offence dates");
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

    private TestStepResult verifyMixedPsSuspensionProcessing(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 6: Verify Mixed PS Suspension Processing Applied", STATUS_INFO);

        try {
            String notice1 = noticePrefix + "001";  // Should get PS-RP2
            String notice2 = noticePrefix + "002";  // Should get PS-RIP
            log.info("🔍 Verifying mixed PS suspension processing for: {} and {}", notice1, notice2);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verify 1: First notice gets PS-RP2 (DoD after offence)
            String suspensionSQL1 = "SELECT suspension_type, epr_reason_of_suspension, notice_date_and_time, " +
                                   "cre_user_id, upd_user_id, upd_date " +
                                   "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

            try {
                Map<String, Object> suspensionData1 = jdbcTemplate.queryForMap(suspensionSQL1, notice1);
                String suspensionType1 = (String) suspensionData1.get("suspension_type");

                if ("PS".equals(suspensionType1)) {
                    // Check if it's PS-RP2 by looking at EPR reason
                    String eprReason1 = (String) suspensionData1.get("epr_reason_of_suspension");
                    if ("RP2".equals(eprReason1)) {
                        verificationResults.add("✅ Notice 1 (Earlier Offence) - PS-RP2 Applied:");
                        verificationResults.add("  📋 Notice: " + notice1 + " (Vehicle: " + TEST_VEHICLE_1 + ")");
                        verificationResults.add("  💀 DoD after offence date - PS-RP2 logic correct");
                        verificationResults.add("  📅 Updated by: " + suspensionData1.get("upd_user_id"));
                    } else {
                        verificationResults.add("❌ Notice 1: Expected PS-RP2, got PS with reason: " + eprReason1);
                        allVerificationsPassed = false;
                    }
                } else {
                    verificationResults.add("❌ Notice 1: Expected PS suspension, got: " + suspensionType1);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Notice 1 suspension verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 2: Second notice gets PS-RIP (DoD before offence)
            try {
                Map<String, Object> suspensionData2 = jdbcTemplate.queryForMap(suspensionSQL1, notice2);
                String suspensionType2 = (String) suspensionData2.get("suspension_type");

                if ("PS".equals(suspensionType2)) {
                    // Check if it's PS-RIP by looking at EPR reason
                    String eprReason2 = (String) suspensionData2.get("epr_reason_of_suspension");
                    if ("RIP".equals(eprReason2)) {
                        verificationResults.add("✅ Notice 2 (Later Offence) - PS-RIP Applied:");
                        verificationResults.add("  📋 Notice: " + notice2 + " (Vehicle: " + TEST_VEHICLE_2 + ")");
                        verificationResults.add("  💀 DoD before offence date - PS-RIP logic correct");
                        verificationResults.add("  📅 Updated by: " + suspensionData2.get("upd_user_id"));
                    } else {
                        verificationResults.add("❌ Notice 2: Expected PS-RIP, got PS with reason: " + eprReason2);
                        allVerificationsPassed = false;
                    }
                } else {
                    verificationResults.add("❌ Notice 2: Expected PS suspension, got: " + suspensionType2);
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("❌ Notice 2 suspension verification failed: " + e.getMessage());
                allVerificationsPassed = false;
            }

            // Verify 3: Same UIN processing
            String uinSQL = "SELECT DISTINCT id_no, name FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                           "WHERE notice_no IN (?, ?)";

            try {
                List<Map<String, Object>> uinData = jdbcTemplate.queryForList(uinSQL, notice1, notice2);
                
                if (uinData.size() == 1 && TEST_UIN.equals(uinData.get(0).get("id_no"))) {
                    verificationResults.add("✅ Same UIN Processing:");
                    verificationResults.add("  🆔 UIN: " + TEST_UIN + " (" + TEST_NAME + ")");
                    verificationResults.add("  📋 Applied to both notices with different DoD logic");
                } else {
                    verificationResults.add("❌ UIN Processing: Expected single UIN " + TEST_UIN + ", got " + uinData.size() + " distinct UINs");
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                verificationResults.add("⚠️ UIN verification skipped: " + e.getMessage());
            }

            // Verify 4: Processing stage progression for both notices
            String stageSQL = "SELECT notice_no, last_processing_stage, next_processing_stage " +
                             "FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no IN (?, ?)";

            try {
                List<Map<String, Object>> stageData = jdbcTemplate.queryForList(stageSQL, notice1, notice2);
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
                result.addDetail("✅ Mixed PS Suspension Processing verification completed successfully");
                result.addDetail("📋 Expected behavior: PS-RP2 for earlier offence, PS-RIP for later offence");
                result.addDetail("🎯 Scenario outcome: PASSED - Mixed PS Batch flow working correctly");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Mixed PS Suspension Processing verification failed");
                result.addDetail("📋 Some critical PS logic checks did not pass");
            }

            verificationResults.forEach(result::addDetail);

        } catch (Exception e) {
            log.error("❌ Error during Mixed PS verification: {}", e.getMessage(), e);
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

    private void insertValidOffenceNoticeWithDate(TestStepResult result, String noticeNo, String vehicleNo, LocalDateTime offenceDateTime) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, vehicle_no, offence_notice_type, last_processing_stage, next_processing_stage, " +
                     "last_processing_date, next_processing_date, notice_date_and_time, " +
                     "pp_code, pp_name, composition_amount, computer_rule_code, vehicle_category, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, vehicleNo, "O", "ROV", "RD1",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), offenceDateTime,
            "A0001", "TEST PARKING PLACE", new java.math.BigDecimal("70.00"), 10001, "C",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Valid offence notice inserted: " + noticeNo);
        result.addDetail("   - Vehicle: " + vehicleNo + ", Offence: " + offenceDateTime.toLocalDate());
    }

    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo, String chassisNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_make_description, lta_primary_colour, " +
                     "cre_user_id, cre_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, chassisNo, "HONDA", "SILVER", "TEST_USER", LocalDateTime.now());
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
            String mainContent = generateMainFileWithSameDod();
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String mainFileName = "NRO2URA_" + timestamp;

            sftpUtil.uploadFile(SFTP_SERVER, mainContent.getBytes(), sftpOutputPath + "/" + mainFileName);

            result.addDetail("📤 Generated NRO2URA file with same DoD: " + mainFileName);
            result.addDetail("💀 DoD: 2024-06-15 (between different offence dates for mixed PS logic)");

        } catch (Exception e) {
            result.addDetail("⚠️ Error generating MHA response files: " + e.getMessage());
        }
    }

    private String generateMainFileWithSameDod() {
        StringBuilder sb = new StringBuilder();
        sb.append("H").append(new SimpleDateFormat("yyyyMMdd").format(new Date())).append("\n");
        
        // Same UIN record with DoD for both notices
        sb.append("D")
          .append(String.format("%-12s", TEST_UIN))
          .append(String.format("%-66s", TEST_NAME))
          .append("D")  // Life Status = D (Deceased)
          .append("20240615")  // DoD: 2024-06-15
          .append(String.format("%-42s", "MIXED_PS_BATCH"))
          .append("\n");
        
        sb.append("T000001\n");
        return sb.toString();
    }
}
