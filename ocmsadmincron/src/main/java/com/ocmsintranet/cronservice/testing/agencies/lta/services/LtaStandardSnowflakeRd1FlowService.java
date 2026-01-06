package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for testing Standard Vehicle Snowflake RD1 Flow
 * 
 * Test Flow: Standard Vehicle → NOT Suspended → NOT In Exclusion List → Singapore NRIC → Snowflake → RD1
 * Expected Result: RD1 (Snowflake called but NO mobile/email found, uses RD1 STAGEDAYS parameter)
 */
@Service
public class LtaStandardSnowflakeRd1FlowService {

    private static final Logger logger = LoggerFactory.getLogger(LtaStandardSnowflakeRd1FlowService.class);

    // Test Constants - Different from ENA flow
    private static final String TEST_ID = "TEST_STANDARD_SNOWFLAKE_RD1";
    private static final String TEST_NAME = "Standard Vehicle → Not Suspended → NOT In Exclusion List → NOT Passport → Snowflake → RD1";
    private static final String STANDARD_VEHICLE = "SMG308Y";
    private static final String OWNER_ID = "S9999999Z";  // Singapore NRIC format NOT in Snowflake
    private static final String OWNER_NAME = "Test User Without Mobile";
    private static final String NOTICE_NO = "EXC1000004";  // Different from ENA flow
    private static final String ID_TYPE = "1"; // Singapore NRIC
    private static final String TEST_USER = "SYSTEM";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SftpUtil sftpUtil;

    @Autowired
    private AzureBlobStorageUtil azureBlobStorageUtil;

    @Autowired
    private RestTemplate restTemplate;

    // Reuse the ENA service for fixed-width record creation
    @Autowired
    private LtaStandardSnowflakeEnaFlowService ltaStandardSnowflakeEnaFlowService;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    /**
     * Run the complete Standard Snowflake RD1 flow test
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        logger.info("🚀 Starting {} - {}", TEST_ID, TEST_NAME);

        try {
            // Step 1: Initialize test data
            results.add(initializeTestData());

            // Step 2: Verify RD1 STAGEDAYS parameter
            results.add(verifyRd1Stagedays());

            // Step 3: Create LTA response file
            String fileName = createLtaResponseFile();
            results.add(new TestStepResult("Create LTA Response File", "SUCCESS"));
            results.get(results.size()-1).addDetail("Created file: " + fileName + " with standard vehicle success data and Singapore NRIC (no mobile in Snowflake)");

            // Step 4: Upload to SFTP
            results.add(uploadToSftp(fileName));

            // Step 5: Trigger LTA Download Manual API
            results.add(runLtaDownloadManual());

            // Step 6: Wait for processing
            Thread.sleep(30000);  // Wait 30 seconds

            // Step 7: Verify results
            results.add(verifyResults());

            // Step 8: Cleanup
            results.add(cleanup());

            logger.info("✅ {} completed successfully", TEST_ID);

        } catch (Exception e) {
            logger.error("❌ {} failed: {}", TEST_ID, e.getMessage(), e);
            results.add(new TestStepResult("Test Execution", "FAILED"));
            results.get(results.size()-1).addDetail("Test failed with error: " + e.getMessage());
            
            try {
                cleanup();
            } catch (Exception cleanupEx) {
                logger.error("Failed to cleanup after error: {}", cleanupEx.getMessage());
            }
        }

        return results;
    }

    /**
     * Step 1: Initialize test data - same as ENA but different Owner ID
     */
    private TestStepResult initializeTestData() {
        try {
            logger.info("📝 Step 1: Initializing test data...");

            // Clean up existing records
            String cleanupSql = String.format("""
                DELETE FROM ocmsizmgr.ocms_enotification_exclusion_list WHERE id_no = '%s';
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s';
                """, OWNER_ID, NOTICE_NO, NOTICE_NO, NOTICE_NO);

            jdbcTemplate.execute(cleanupSql);

            // Insert VON record (NOT suspended)
            String insertVonSql = String.format("""
                INSERT INTO ocmsizmgr.ocms_valid_offence_notice 
                (notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, 
                composition_amount, computer_rule_code, pp_code, pp_name,
                prev_processing_stage, prev_processing_date,
                last_processing_stage, last_processing_date,
                next_processing_stage, next_processing_date, 
                subsystem_label, cre_user_id, cre_date) 
                VALUES 
                ('%s', '%s', 'S', 'O', GETDATE(), 150.00, 203, 'PP04', 'PARKING OFFENCE',
                NULL, NULL, 'NPA', GETDATE(), 'ROV', GETDATE(), 'OCMS', '%s', GETDATE())
                """, NOTICE_NO, STANDARD_VEHICLE, TEST_USER);

            jdbcTemplate.execute(insertVonSql);

            // Insert OND and OOD records
            String insertOndSql = String.format("""
                INSERT INTO ocmsizmgr.ocms_offence_notice_detail
                (notice_no, cre_date, cre_user_id, lta_chassis_number, lta_make_description, 
                lta_primary_colour, lta_diplomatic_flag, lta_road_tax_expiry_date, 
                lta_eff_ownership_date, lta_deregistration_date, lta_unladen_weight, lta_max_laden_weight)
                VALUES ('%s', GETDATE(), '%s', 'TEMP_CHASSIS', 'TEMP_MAKE', 'TEMP_COLOUR', 'N',
                NULL, NULL, NULL, NULL, NULL)
                """, NOTICE_NO, TEST_USER);

            String insertOodSql = String.format("""
                INSERT INTO ocmsizmgr.ocms_offence_notice_owner_driver
                (notice_no, owner_driver_indicator, cre_date, cre_user_id, life_status,
                lta_processing_date_time, mha_processing_date_time, id_type, nric_no, name,
                reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code,
                mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code,
                lta_error_code, passport_place_of_issue, lta_reg_address_effective_date, lta_mailing_address_effective_date)
                VALUES ('%s', 'O', GETDATE(), '%s', 'A', GETDATE(), GETDATE(),
                '%s', '%s', '%s', '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                NULL, NULL, NULL, NULL)
                """, NOTICE_NO, TEST_USER, ID_TYPE, OWNER_ID, OWNER_NAME);

            jdbcTemplate.execute(insertOndSql);
            jdbcTemplate.execute(insertOodSql);

            // Verify records created
            String verifyVonSql = String.format("SELECT COUNT(*) as count FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s'", NOTICE_NO);
            String verifyExclusionSql = String.format("SELECT COUNT(*) as count FROM ocmsizmgr.ocms_enotification_exclusion_list WHERE id_no = '%s'", OWNER_ID);

            List<Map<String, Object>> vonResults = jdbcTemplate.queryForList(verifyVonSql);
            List<Map<String, Object>> exclusionResults = jdbcTemplate.queryForList(verifyExclusionSql);

            int vonCount = ((Number) vonResults.get(0).get("count")).intValue();
            int exclusionCount = ((Number) exclusionResults.get(0).get("count")).intValue();

            TestStepResult result = new TestStepResult("Initialize Test Data", vonCount == 1 && exclusionCount == 0 ? "SUCCESS" : "FAILED");
            result.addDetail("Created VON, OND, OOD records. Vehicle: " + STANDARD_VEHICLE + ", Owner: " + OWNER_ID + " (Singapore NRIC, NO mobile/email expected)");
            return result;

        } catch (Exception e) {
            logger.error("Failed to initialize test data: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Initialize Test Data", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Verify RD1 STAGEDAYS parameter exists (different from ENA)
     */
    private TestStepResult verifyRd1Stagedays() {
        try {
            logger.info("📝 Step 2: Verifying RD1 STAGEDAYS parameter...");

            String checkParameterSql = "SELECT value FROM ocmsizmgr.ocms_parameter WHERE code = 'RD1' AND parameter_id = 'STAGEDAYS'";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkParameterSql);

            if (results.isEmpty()) {
                logger.warn("RD1 STAGEDAYS parameter not found, creating with default value 4 days");
                
                String createParameterSql = """
                    INSERT INTO ocmsizmgr.ocms_parameter 
                    (code, parameter_id, description, value, cre_date, cre_user_id) 
                    VALUES ('RD1', 'STAGEDAYS', 'Processing stage duration', '4', GETDATE(), 'TEST_SYSTEM')
                    """;
                jdbcTemplate.execute(createParameterSql);
                
                TestStepResult result = new TestStepResult("Verify RD1 STAGEDAYS", "SUCCESS");
                result.addDetail("Created missing RD1 STAGEDAYS parameter with value: 4 days");
                return result;
            } else {
                String stageDays = results.get(0).get("value").toString();
                TestStepResult result = new TestStepResult("Verify RD1 STAGEDAYS", "SUCCESS");
                result.addDetail("RD1 STAGEDAYS parameter found: " + stageDays + " days");
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to verify RD1 STAGEDAYS parameter: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Verify RD1 STAGEDAYS", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Create LTA response file - reuse structure from ENA service
     */
    private String createLtaResponseFile() {
        logger.info("📝 Step 3: Creating LTA response file...");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

        // Header and trailer same as ENA
        String headerRecord = String.format("H%-682s", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        String trailerRecord = String.format("T%-684s", "000001");

        // Data record with different owner ID
        String dataRecord = createFixedWidthRecord(
            STANDARD_VEHICLE, "3N1AB7APXHY000124", "N", NOTICE_NO, ID_TYPE, "",
            OWNER_ID, OWNER_NAME, "C", "47", "FIGARO STREET", "60", "36", "",
            "503027", "KIA", "MAROON", "", 
            LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            "1685", "2123", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            "", "0", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"))
        );

        String fileContent = headerRecord + "\n" + dataRecord + "\n" + trailerRecord + "\n";

        try {
            File tempFile = new File("/tmp/" + fileName);
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(fileContent);
            }
            String tempContent = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
            azureBlobStorageUtil.uploadBytesToBlob(tempContent.getBytes(), "temp/" + fileName);
            tempFile.delete();
            logger.info("✅ Created LTA response file: {}", fileName);
            return fileName;

        } catch (Exception e) {
            logger.error("Failed to create LTA response file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create LTA response file", e);
        }
    }

    // Reuse SFTP upload, API call methods from ENA flow (same implementation)
    private TestStepResult uploadToSftp(String fileName) {
        try {
            logger.info("📝 Step 4: Uploading file to SFTP server...");

            File tempFile = new File("/tmp/" + fileName);
            byte[] fileContent = azureBlobStorageUtil.downloadFromBlob("temp/" + fileName);
            if (fileContent == null) {
                throw new RuntimeException("Failed to download file from blob storage");
            }
            java.nio.file.Files.write(tempFile.toPath(), fileContent);
            
            String encryptedFileName = fileName + ".p7";
            byte[] fileBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            boolean uploadResult = sftpUtil.uploadFile("vrls/output/" + encryptedFileName, fileBytes, null);
            tempFile.delete();

            TestStepResult result = new TestStepResult("Upload to SFTP", uploadResult ? "SUCCESS" : "FAILED");
            result.addDetail(uploadResult ? ("File uploaded successfully to vrls/output/" + encryptedFileName) : "Failed to upload file to SFTP server");
            return result;

        } catch (Exception e) {
            logger.error("Failed to upload to SFTP: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Upload to SFTP", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    private TestStepResult runLtaDownloadManual() {
        try {
            logger.info("📝 Step 5: Triggering LTA Download Manual API...");

            String endpoint = apiBaseUrl + "/ocms/v1/lta/download/manual";
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, "", String.class);

            TestStepResult result = new TestStepResult("Trigger LTA Download API", response.getStatusCode().is2xxSuccessful() ? "SUCCESS" : "FAILED");
            result.addDetail("API call " + (response.getStatusCode().is2xxSuccessful() ? "successful" : "failed") + ". Status: " + response.getStatusCode());
            return result;

        } catch (Exception e) {
            logger.error("Failed to call LTA Download API: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Trigger LTA Download API", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 6: Verify RD1 results (different expected outcome from ENA)
     */
    private TestStepResult verifyResults() {
        try {
            logger.info("📝 Step 6: Verifying processing results...");

            String verifySql = String.format("""
                SELECT von.last_processing_stage, von.next_processing_stage, 
                       von.upd_user_id, von.upd_date,
                       onod.offender_tel_no, onod.email_addr, onod.datahive_processing_datetime
                FROM ocmsizmgr.ocms_valid_offence_notice von
                JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no
                WHERE von.notice_no = '%s'
                """, NOTICE_NO);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(verifySql);

            if (results.isEmpty()) {
                TestStepResult result = new TestStepResult("Verify Results", "FAILED");
                result.addDetail("❌ No record found for notice_no: " + NOTICE_NO);
                return result;
            }

            Map<String, Object> record = results.get(0);
            String nextStage = (String) record.get("next_processing_stage");
            String telNo = (String) record.get("offender_tel_no");
            String emailAddr = (String) record.get("email_addr");

            TestStepResult result = new TestStepResult("Verify Results", "SUCCESS");
            result.addDetail("✅ Processing stages verified:");
            result.addDetail("   - Next Stage: " + nextStage);
            result.addDetail("   - Tel No: " + (telNo != null ? telNo : "NULL (as expected)"));
            result.addDetail("   - Email Addr: " + (emailAddr != null ? emailAddr : "NULL (as expected)"));

            // For RD1 flow, expect RD1 stage and NO tel/email
            if ("RD1".equals(nextStage) && telNo == null && emailAddr == null) {
                result.addDetail("✅ Expected RD1 stage achieved (no tel/email found in Snowflake)");
            } else {
                result.setStatus("FAILED");
                result.addDetail("❌ Expected next_processing_stage RD1 with no tel/email, but got stage: " + nextStage);
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to verify results: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Verify Results", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    private TestStepResult cleanup() {
        try {
            logger.info("📝 Step 7: Cleaning up test data...");

            String cleanupSql = String.format("""
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s';
                """, NOTICE_NO, NOTICE_NO, NOTICE_NO);

            jdbcTemplate.execute(cleanupSql);

            TestStepResult result = new TestStepResult("Cleanup", "SUCCESS");
            result.addDetail("✅ Test data cleaned up successfully for notice: " + NOTICE_NO);
            return result;

        } catch (Exception e) {
            logger.error("Failed to cleanup: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Cleanup", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    // Delegate to ENA service for fixed-width record creation to avoid duplication
    private String createFixedWidthRecord(String vehicleNo, String chassisNo, String diplomaticFlag,
                                        String noticeNo, String idType, String passportPlace,
                                        String ownerId, String ownerName, String addressType,
                                        String blockNo, String streetName, String floorNo,
                                        String unitNo, String buildingName, String postalCode,
                                        String makeDesc, String primaryColour, String secondaryColour,
                                        String roadTaxExpiry, String unladenWeight, String maxWeight,
                                        String effectiveOwnership, String deregDate, String errorCode,
                                        String processDate, String processTime) {
        
        // Use reflection to call the private method from ENA service
        try {
            var method = LtaStandardSnowflakeEnaFlowService.class.getDeclaredMethod("createFixedWidthRecord",
                String.class, String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(ltaStandardSnowflakeEnaFlowService, vehicleNo, chassisNo, diplomaticFlag,
                noticeNo, idType, passportPlace, ownerId, ownerName, addressType, blockNo, streetName, floorNo,
                unitNo, buildingName, postalCode, makeDesc, primaryColour, secondaryColour, roadTaxExpiry,
                unladenWeight, maxWeight, effectiveOwnership, deregDate, errorCode, processDate, processTime);
        } catch (Exception e) {
            logger.error("Failed to create fixed width record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create fixed width record", e);
        }
    }
}
