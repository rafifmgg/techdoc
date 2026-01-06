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
 * Service for testing Standard Vehicle Snowflake ENA Flow
 * 
 * Test Flow: Standard Vehicle → NOT Suspended → NOT In Exclusion List → Singapore NRIC → Snowflake → ENA
 * Expected Result: ENA (Embassy Notification Acknowledgment) with STAGEDAYS parameter usage
 */
@Service
public class LtaStandardSnowflakeEnaFlowService {

    private static final Logger logger = LoggerFactory.getLogger(LtaStandardSnowflakeEnaFlowService.class);

    // Test Constants
    private static final String TEST_ID = "TEST_STANDARD_SNOWFLAKE_ENA";
    private static final String TEST_NAME = "Standard Vehicle → Not Suspended → NOT In Exclusion List → Singapore NRIC → Snowflake → ENA";
    private static final String STANDARD_VEHICLE = "SMG308Y";
    private static final String OWNER_ID = "S1111111A";  // Singapore NRIC format
    private static final String OWNER_NAME = "Test User With Mobile";
    private static final String NOTICE_NO = "EXC1000003";
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

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    /**
     * Run the complete Standard Snowflake ENA flow test
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        logger.info("🚀 Starting {} - {}", TEST_ID, TEST_NAME);

        try {
            // Step 1: Initialize test data
            results.add(initializeTestData());

            // Step 2: Verify ENA STAGEDAYS parameter
            results.add(verifyEnaStagedays());

            // Step 3: Create LTA response file
            String fileName = createLtaResponseFile();
            results.add(new TestStepResult("Create LTA Response File", "SUCCESS"));
            results.get(results.size()-1).addDetail("Created file: " + fileName + " with standard vehicle success data and Singapore NRIC");

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
            
            // Attempt cleanup on failure
            try {
                cleanup();
            } catch (Exception cleanupEx) {
                logger.error("Failed to cleanup after error: {}", cleanupEx.getMessage());
            }
        }

        return results;
    }

    /**
     * Step 1: Initialize test data
     * Creates VON, OND, OOD records with Singapore NRIC (NOT in exclusion list)
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

            // Insert OND record
            String insertOndSql = String.format("""
                INSERT INTO ocmsizmgr.ocms_offence_notice_detail
                (notice_no, cre_date, cre_user_id, lta_chassis_number, lta_make_description, 
                lta_primary_colour, lta_diplomatic_flag, lta_road_tax_expiry_date, 
                lta_eff_ownership_date, lta_deregistration_date, lta_unladen_weight, lta_max_laden_weight)
                VALUES 
                ('%s', GETDATE(), '%s', 'TEMP_CHASSIS', 'TEMP_MAKE', 'TEMP_COLOUR', 'N',
                NULL, NULL, NULL, NULL, NULL)
                """, NOTICE_NO, TEST_USER);

            jdbcTemplate.execute(insertOndSql);

            // Insert OOD record with Singapore NRIC
            String insertOodSql = String.format("""
                INSERT INTO ocmsizmgr.ocms_offence_notice_owner_driver
                (notice_no, owner_driver_indicator, cre_date, cre_user_id, life_status,
                lta_processing_date_time, mha_processing_date_time, id_type, nric_no, name,
                reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code,
                mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code,
                lta_error_code, passport_place_of_issue, lta_reg_address_effective_date, lta_mailing_address_effective_date)
                VALUES
                ('%s', 'O', GETDATE(), '%s', 'A', GETDATE(), GETDATE(),
                '%s', '%s', '%s', '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                NULL, NULL, NULL, NULL)
                """, NOTICE_NO, TEST_USER, ID_TYPE, OWNER_ID, OWNER_NAME);

            jdbcTemplate.execute(insertOodSql);

            // Verify records created and NOT in exclusion list
            String verifyVonSql = String.format(
                "SELECT COUNT(*) as count FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s'", NOTICE_NO);
            String verifyExclusionSql = String.format(
                "SELECT COUNT(*) as count FROM ocmsizmgr.ocms_enotification_exclusion_list WHERE id_no = '%s'", OWNER_ID);

            List<Map<String, Object>> vonResults = jdbcTemplate.queryForList(verifyVonSql);
            List<Map<String, Object>> exclusionResults = jdbcTemplate.queryForList(verifyExclusionSql);

            int vonCount = ((Number) vonResults.get(0).get("count")).intValue();
            int exclusionCount = ((Number) exclusionResults.get(0).get("count")).intValue();

            if (vonCount == 1 && exclusionCount == 0) {
                TestStepResult result = new TestStepResult("Initialize Test Data", "SUCCESS");
                result.addDetail("Created VON, OND, OOD records successfully. Verified owner NOT in exclusion list. " +
                    "Vehicle: " + STANDARD_VEHICLE + ", Owner: " + OWNER_ID + " (Singapore NRIC)");
                return result;
            } else {
                TestStepResult result = new TestStepResult("Initialize Test Data", "FAILED");
                result.addDetail("Data verification failed. VON count: " + vonCount + ", Exclusion count: " + exclusionCount);
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to initialize test data: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Initialize Test Data", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Verify ENA STAGEDAYS parameter exists
     */
    private TestStepResult verifyEnaStagedays() {
        try {
            logger.info("📝 Step 2: Verifying ENA STAGEDAYS parameter...");

            String checkParameterSql = """
                SELECT value FROM ocmsizmgr.ocms_parameter 
                WHERE code = 'ENA' AND parameter_id = 'STAGEDAYS'
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkParameterSql);

            if (results.isEmpty()) {
                logger.warn("ENA STAGEDAYS parameter not found, creating with default value 4 days");
                
                String createParameterSql = """
                    INSERT INTO ocmsizmgr.ocms_parameter 
                    (code, parameter_id, description, value, cre_date, cre_user_id) 
                    VALUES 
                    ('ENA', 'STAGEDAYS', 'Processing stage duration', '4', GETDATE(), 'TEST_SYSTEM')
                    """;

                jdbcTemplate.execute(createParameterSql);
                
                TestStepResult result = new TestStepResult("Verify ENA STAGEDAYS", "SUCCESS");
                result.addDetail("Created missing ENA STAGEDAYS parameter with value: 4 days");
                return result;
            } else {
                String stageDays = results.get(0).get("value").toString();
                TestStepResult result = new TestStepResult("Verify ENA STAGEDAYS", "SUCCESS");
                result.addDetail("ENA STAGEDAYS parameter found: " + stageDays + " days");
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to verify ENA STAGEDAYS parameter: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Verify ENA STAGEDAYS", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Create LTA response file with success data
     */
    private String createLtaResponseFile() {
        logger.info("📝 Step 3: Creating LTA response file...");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

        // Header record (691 characters)
        String headerRecord = String.format("H%-682s", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        // Data record with Singapore NRIC success data
        String dataRecord = createFixedWidthRecord(
            STANDARD_VEHICLE, "3N1AB7APXHY000124", "N", NOTICE_NO, ID_TYPE, "",
            OWNER_ID, OWNER_NAME, "C", "47", "FIGARO STREET", "60", "36", "",
            "503027", "KIA", "MAROON", "", 
            LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            "1685", "2123", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            "", "0", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"))
        );

        // Trailer record (691 characters)
        String trailerRecord = String.format("T%-684s", "000001");

        // Combine all records
        String fileContent = headerRecord + "\n" + dataRecord + "\n" + trailerRecord + "\n";

        try {
            // Create temp file and upload to Azure Blob
            File tempFile = new File("/tmp/" + fileName);
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(fileContent);
            }
            // Upload to blob storage
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

    /**
     * Step 4: Upload file to SFTP server
     */
    private TestStepResult uploadToSftp(String fileName) {
        try {
            logger.info("📝 Step 4: Uploading file to SFTP server...");

            // Download from temp blob storage
            File tempFile = new File("/tmp/" + fileName);
            byte[] fileContent = azureBlobStorageUtil.downloadFromBlob("temp/" + fileName);
            if (fileContent == null) {
                throw new RuntimeException("Failed to download file from blob storage");
            }
            java.nio.file.Files.write(tempFile.toPath(), fileContent);
            
            String encryptedFileName = fileName + ".p7";
            // Read file content and upload to SFTP
            byte[] fileBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            boolean uploadResult = sftpUtil.uploadFile("vrls/output/" + encryptedFileName, fileBytes, null);
            tempFile.delete();

            if (uploadResult) {
                TestStepResult result = new TestStepResult("Upload to SFTP", "SUCCESS");
                result.addDetail("File uploaded successfully to vrls/output/" + encryptedFileName);
                return result;
            } else {
                TestStepResult result = new TestStepResult("Upload to SFTP", "FAILED");
                result.addDetail("Failed to upload file to SFTP server");
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to upload to SFTP: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Upload to SFTP", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 5: Trigger LTA Download Manual API
     */
    private TestStepResult runLtaDownloadManual() {
        try {
            logger.info("📝 Step 5: Triggering LTA Download Manual API...");

            String endpoint = apiBaseUrl + "/ocms/v1/lta/download/manual";
            logger.info("🚀 Calling API: {}", endpoint);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, "", String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                TestStepResult result = new TestStepResult("Trigger LTA Download API", "SUCCESS");
                result.addDetail("API call successful. Status: " + response.getStatusCode() + 
                    ", Response: " + response.getBody());
                return result;
            } else {
                TestStepResult result = new TestStepResult("Trigger LTA Download API", "FAILED");
                result.addDetail("API call failed. Status: " + response.getStatusCode());
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to call LTA Download API: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Trigger LTA Download API", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 6: Verify processing results
     */
    private TestStepResult verifyResults() {
        try {
            logger.info("📝 Step 6: Verifying processing results...");

            // Verify processing stage changes
            String verifySql = String.format(
                "SELECT last_processing_stage, next_processing_stage, upd_user_id, upd_date " +
                "FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s'", NOTICE_NO);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(verifySql);

            if (results.isEmpty()) {
                TestStepResult result = new TestStepResult("Verify Results", "FAILED");
                result.addDetail("❌ No record found for notice_no: " + NOTICE_NO);
                return result;
            }

            Map<String, Object> record = results.get(0);
            String lastStage = (String) record.get("last_processing_stage");
            String nextStage = (String) record.get("next_processing_stage");
            String updUser = (String) record.get("upd_user_id");
            Object updDate = record.get("upd_date");

            TestStepResult result = new TestStepResult("Verify Results", "SUCCESS");
            result.addDetail("✅ Processing stages verified:");
            result.addDetail("   - Last Stage: " + lastStage);
            result.addDetail("   - Next Stage: " + nextStage);
            result.addDetail("   - Updated by: " + updUser);
            result.addDetail("   - Updated date: " + updDate);

            // For Standard Snowflake ENA flow, expect ENA stage
            if ("ENA".equals(nextStage)) {
                result.addDetail("✅ Expected ENA stage achieved for Snowflake user");
            } else {
                result.setStatus("FAILED");
                result.addDetail("❌ Expected next_processing_stage ENA but got: " + nextStage);
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to verify results: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Verify Results", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 7: Cleanup test data
     */
    private TestStepResult cleanup() {
        try {
            logger.info("📝 Step 7: Cleaning up test data...");

            // Clean up database records
            String cleanupSql = String.format("""
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = '%s';
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = '%s';
                """, NOTICE_NO, NOTICE_NO, NOTICE_NO);

            jdbcTemplate.execute(cleanupSql);

            TestStepResult result = new TestStepResult("Cleanup", "SUCCESS");
            result.addDetail("✅ Test data cleaned up successfully");
            result.addDetail("   - Removed VON, OND, OOD records for notice: " + NOTICE_NO);
            return result;

        } catch (Exception e) {
            logger.error("Failed to cleanup: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Cleanup", "FAILED");
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Create fixed-width LTA response record
     */
    private String createFixedWidthRecord(String vehicleNo, String chassisNo, String diplomaticFlag,
                                        String noticeNo, String idType, String passportPlace,
                                        String ownerId, String ownerName, String addressType,
                                        String blockNo, String streetName, String floorNo,
                                        String unitNo, String buildingName, String postalCode,
                                        String makeDesc, String primaryColour, String secondaryColour,
                                        String roadTaxExpiry, String unladenWeight, String maxWeight,
                                        String effectiveOwnership, String deregDate, String errorCode,
                                        String processDate, String processTime) {

        StringBuilder record = new StringBuilder("D"); // Record type
        
        // Vehicle Registration Number (A12)
        record.append(String.format("%-12s", vehicleNo != null ? vehicleNo.substring(0, Math.min(vehicleNo.length(), 12)) : ""));
        
        // Chassis Number (A25)
        record.append(String.format("%-25s", chassisNo != null ? chassisNo.substring(0, Math.min(chassisNo.length(), 25)) : ""));
        
        // Diplomatic Flag (A1)
        record.append(String.format("%-1s", diplomaticFlag != null ? diplomaticFlag : "N"));
        
        // Notice Number (A10)
        record.append(String.format("%-10s", noticeNo != null ? noticeNo.substring(0, Math.min(noticeNo.length(), 10)) : ""));
        
        // Owner ID Type (A1)
        record.append(String.format("%-1s", idType != null ? idType : ""));
        
        // Owner's Passport Place of Issue (A3)
        record.append(String.format("%-3s", passportPlace != null ? passportPlace.substring(0, Math.min(passportPlace.length(), 3)) : ""));
        
        // Owner ID (A20)
        record.append(String.format("%-20s", ownerId != null ? ownerId.substring(0, Math.min(ownerId.length(), 20)) : ""));
        
        // Owner Name (A66)
        record.append(String.format("%-66s", ownerName != null ? ownerName.substring(0, Math.min(ownerName.length(), 66)) : ""));
        
        // Address Type (A1)
        record.append(String.format("%-1s", addressType != null ? addressType : ""));
        
        // Block/House Number (A10)
        record.append(String.format("%-10s", blockNo != null ? blockNo.substring(0, Math.min(blockNo.length(), 10)) : ""));
        
        // Street Name (A32)
        record.append(String.format("%-32s", streetName != null ? streetName.substring(0, Math.min(streetName.length(), 32)) : ""));
        
        // Floor Number (A2)
        record.append(String.format("%-2s", floorNo != null ? floorNo.substring(0, Math.min(floorNo.length(), 2)) : ""));
        
        // Unit Number (A5)
        record.append(String.format("%-5s", unitNo != null ? unitNo.substring(0, Math.min(unitNo.length(), 5)) : ""));
        
        // Building Name (A30)
        record.append(String.format("%-30s", buildingName != null ? buildingName.substring(0, Math.min(buildingName.length(), 30)) : ""));
        
        // Postal Code (A8)
        record.append(String.format("%-8s", postalCode != null ? postalCode.substring(0, Math.min(postalCode.length(), 8)) : ""));
        
        // Make Description (A100)
        record.append(String.format("%-100s", makeDesc != null ? makeDesc.substring(0, Math.min(makeDesc.length(), 100)) : ""));
        
        // Primary Colour (A100)
        record.append(String.format("%-100s", primaryColour != null ? primaryColour.substring(0, Math.min(primaryColour.length(), 100)) : ""));
        
        // Secondary Colour (A100)
        record.append(String.format("%-100s", secondaryColour != null ? secondaryColour.substring(0, Math.min(secondaryColour.length(), 100)) : ""));
        
        // Road Tax Expiry Date (N8)
        record.append(String.format("%8s", roadTaxExpiry != null ? roadTaxExpiry : "        "));
        
        // Unladen weight (N7)
        record.append(String.format("%7s", unladenWeight != null ? unladenWeight : "       "));
        
        // Maximum Laden weight (N7)
        record.append(String.format("%7s", maxWeight != null ? maxWeight : "       "));
        
        // Effective Ownership Date (N8)
        record.append(String.format("%8s", effectiveOwnership != null ? effectiveOwnership : "        "));
        
        // Deregistration Date (N8)
        record.append(String.format("%8s", deregDate != null ? deregDate : "        "));
        
        // Return Error Code (A1) - Important: This is where error code goes
        record.append(String.format("%-1s", errorCode != null ? errorCode : "0"));
        
        // Processing Date (N8)
        record.append(String.format("%8s", processDate != null ? processDate : "        "));
        
        // Processing Time (N4)
        record.append(String.format("%4s", processTime != null ? processTime : "    "));
        
        // IU/OBU Label Number (A10)
        record.append(String.format("%-10s", ""));
        
        // Registered Address Effective Date (N8)
        record.append(String.format("%8s", "        "));
        
        // Mailing Block/ House Number (A10)
        record.append(String.format("%-10s", blockNo != null ? blockNo.substring(0, Math.min(blockNo.length(), 10)) : ""));
        
        // Mailing Street Name (A32)
        record.append(String.format("%-32s", streetName != null ? streetName.substring(0, Math.min(streetName.length(), 32)) : ""));
        
        // Mailing Floor Number (A2)
        record.append(String.format("%-2s", floorNo != null ? floorNo.substring(0, Math.min(floorNo.length(), 2)) : ""));
        
        // Mailing Unit Number (A5)
        record.append(String.format("%-5s", unitNo != null ? unitNo.substring(0, Math.min(unitNo.length(), 5)) : ""));
        
        // Mailing Building Name (A30)
        record.append(String.format("%-30s", buildingName != null ? buildingName.substring(0, Math.min(buildingName.length(), 30)) : ""));
        
        // Mailing Postal Code (A8)
        record.append(String.format("%-8s", postalCode != null ? postalCode.substring(0, Math.min(postalCode.length(), 8)) : ""));
        
        // Mailing Address Effective Date (N8)
        record.append(String.format("%8s", "        "));
        
        // Pad to 691 characters total
        while (record.length() < 691) {
            record.append(" ");
        }
        
        return record.toString();
    }
}
