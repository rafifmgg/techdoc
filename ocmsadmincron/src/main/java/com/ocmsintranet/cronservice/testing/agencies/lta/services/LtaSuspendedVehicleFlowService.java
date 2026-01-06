package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LtaSuspendedVehicleFlowService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AzureBlobStorageUtil azureBlobStorageUtil;

    @Autowired
    private SftpUtil sftpUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LtaStandardSnowflakeEnaFlowService ltaStandardSnowflakeEnaFlowService;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    // Test configuration constants
    private static final String TEST_ID = "TEST_STANDARD_SUSPENDED_REVIVED";
    private static final String TEST_NAME = "Standard Vehicle → Suspended → TS-HST Revived → END";
    private static final String STANDARD_VEHICLE = "STD1234A";
    private static final String OWNER_ID = "S1111111A";
    private static final String OWNER_NAME = "TEST SUSPENDED OWNER";
    private static final String NOTICE_NO = "SUS1000001";

    /**
     * Main method to run the Suspended Vehicle TS-HST Revived test flow
     * Expected: Basic owner data updated, then END (early termination due to revived suspension)
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        log.info("Starting LTA Suspended Vehicle Flow Test: {}", TEST_NAME);

        try {
            // Step 1: Initialize test data with suspension configuration
            results.add(initializeTestData());

            // Step 2: Create LTA response file (success case)
            String fileName = createLtaResponseFile();
            TestStepResult createFileResult = new TestStepResult("CREATE_LTA_RESPONSE_FILE", "SUCCESS");
            createFileResult.addDetail("LTA response file created successfully: " + fileName);
            results.add(createFileResult);

            // Step 3: Upload file to SFTP
            results.add(uploadToSftp(fileName));

            // Step 4: Trigger LTA Download Manual API
            results.add(runLtaDownloadManual());

            // Step 5: Wait for processing
            Thread.sleep(30000); // 30 seconds wait

            // Step 6: Verify results (suspension check should cause early termination)
            results.add(verifyResults());

            // Step 7: Cleanup test data
            results.add(cleanup());

            log.info("LTA Suspended Vehicle Flow Test completed successfully");
            return results;

        } catch (Exception e) {
            log.error("LTA Suspended Vehicle Flow Test failed", e);
            TestStepResult failedResult = new TestStepResult("SUSPENDED_VEHICLE_FLOW_TEST", "FAILED");
            failedResult.addDetail("Test failed: " + e.getMessage());
            results.add(failedResult);
            
            // Attempt cleanup even on failure
            try {
                cleanup();
            } catch (Exception cleanupEx) {
                log.error("Cleanup failed", cleanupEx);
            }
            
            return results;
        }
    }

    /**
     * Step 1: Initialize test data with suspension configuration
     * Creates all required records with TS-HST suspension but REVIVED status
     */
    private TestStepResult initializeTestData() {
        try {
            log.info("Initializing test data with suspension configuration");

            String sql = """
                -- Clean up existing test records (in correct order due to foreign keys)
                DELETE FROM ocmsizmgr.ocms_suspended_notice WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = ?;

                -- 1. Insert ocms_valid_offence_notice with SUSPENSION CONFIGURATION
                INSERT INTO ocmsizmgr.ocms_valid_offence_notice 
                (notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, 
                composition_amount, computer_rule_code, pp_code, 
                prev_processing_stage, prev_processing_date,
                last_processing_stage, last_processing_date,
                next_processing_stage, next_processing_date, 
                subsystem_label, 
                suspension_type, epr_reason_of_suspension,
                cre_user_id, cre_date) 
                VALUES 
                (?, ?, 'S', 'O', GETDATE(), 150.00, 203, 'PP04', 
                NULL, NULL, 'NPA', GETDATE(), 'ROV', GETDATE(), 'OCMS',
                'TS', 'HST',
                'SYSTEM', GETDATE());

                -- 2. Insert ocms_offence_notice_detail (WILL BE UPDATED with LTA data)
                INSERT INTO ocmsizmgr.ocms_offence_notice_detail
                (notice_no, 
                cre_date, cre_user_id,
                lta_chassis_number, lta_make_description, lta_primary_colour, lta_diplomatic_flag,
                lta_road_tax_expiry_date, lta_eff_ownership_date, lta_deregistration_date,
                lta_unladen_weight, lta_max_laden_weight)
                VALUES 
                (?, 
                GETDATE(), 'SYSTEM',
                'TEMP_CHASSIS', 'TEMP_MAKE', 'TEMP_COLOUR', 'N',
                NULL, NULL, NULL,
                NULL, NULL);

                -- 3. Insert ocms_offence_notice_owner_driver (WILL BE UPDATED with LTA data)
                INSERT INTO ocmsizmgr.ocms_offence_notice_owner_driver
                (notice_no, owner_driver_indicator,
                cre_date, cre_user_id, life_status,
                lta_processing_date_time, mha_processing_date_time,
                id_type, nric_no, name,
                reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code,
                mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code,
                lta_error_code, passport_place_of_issue,
                lta_reg_address_effective_date, lta_mailing_address_effective_date)
                VALUES 
                (?, 'O',
                GETDATE(), 'SYSTEM', 'A',
                GETDATE(), GETDATE(),
                '1', 'TEMP_ID', 'TEMP_NAME',
                NULL, NULL, NULL, NULL, NULL, NULL,
                NULL, NULL, NULL, NULL, NULL, NULL,
                NULL, NULL,
                NULL, NULL);

                -- 4. Insert ocms_suspended_notice with TS-HST but REVIVED (future date_of_revival)
                INSERT INTO ocmsizmgr.ocms_suspended_notice
                (notice_no, date_of_suspension, sr_no, 
                suspension_source, suspension_type, reason_of_suspension, 
                officer_authorising_suspension, due_date_of_revival, 
                suspension_remarks, date_of_revival, 
                cre_date, cre_user_id)
                VALUES 
                (?, GETDATE(), 1,
                'LTA', 'TS', 'HST', 
                'SYSTEM', NULL,
                'Test suspension for TS-HST', DATEADD(day, 30, GETDATE()),
                GETDATE(), 'SYSTEM');
                """;

            // Execute the SQL with parameters
            jdbcTemplate.update(sql, 
                NOTICE_NO, NOTICE_NO, NOTICE_NO, NOTICE_NO, // cleanup parameters
                NOTICE_NO, STANDARD_VEHICLE, // ocms_valid_offence_notice parameters
                NOTICE_NO, // ocms_offence_notice_detail parameter
                NOTICE_NO, // ocms_offence_notice_owner_driver parameter
                NOTICE_NO  // ocms_suspended_notice parameter
            );

            log.info("Test data initialized successfully with suspension configuration");
            TestStepResult result = new TestStepResult("INITIALIZE_TEST_DATA", "SUCCESS");
            result.addDetail("Test data created with TS-HST suspension but REVIVED status");
            return result;

        } catch (DataAccessException e) {
            log.error("Failed to initialize test data", e);
            TestStepResult result = new TestStepResult("INITIALIZE_TEST_DATA", "FAILED");
            result.addDetail("Failed to create test data: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Create LTA response file with SUCCESS data
     * Creates fixed-width format file with standard vehicle success case
     */
    private String createLtaResponseFile() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;
        String tempDir = "/tmp/lta_test";
        String filePath = tempDir + "/" + fileName;

        // Create directory if not exists
        File directory = new File(tempDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            // Header record (691 characters)
            String headerRecord = String.format("H%-8s%-682s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), "");
            writer.write(headerRecord + "\n");

            // Standard vehicle record with SUCCESS data (error code 0) - use reflection to call createFixedWidthRecord
            String standardRecord = createFixedWidthRecord(
                STANDARD_VEHICLE,     // vehicle_number
                "STDCHASSIS789",      // chassis_number
                "N",                  // diplomatic_flag
                NOTICE_NO,            // notice_number
                "1",                  // owner_id_type
                "SGP",                // passport_place
                OWNER_ID,             // owner_id
                OWNER_NAME,           // owner_name
                "A",                  // address_type
                "789",                // block_house
                "SUSPENDED STREET",   // street_name
                "12",                 // floor_number
                "34",                 // unit_number
                "SUSPENDED BUILDING", // building_name
                "567890",             // postal_code
                "TOYOTA CAMRY",       // make_description
                "BLUE",               // primary_colour
                "",                   // secondary_colour
                LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // road_tax_expiry
                "1200",               // unladen_weight
                "1600",               // max_laden_weight
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), // effective_ownership
                "",                   // deregistration_date
                "0",                  // error_code (SUCCESS)
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), // processing_date
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"))      // processing_time
            );
            writer.write(standardRecord + "\n");

            // Trailer record (691 characters)
            String trailerRecord = String.format("T%-6s%-684s", "000001", "");
            writer.write(trailerRecord + "\n");
        }

        log.info("LTA response file created: {}", filePath);
        return fileName;
    }

    /**
     * Step 3: Upload file to SFTP server
     */
    private TestStepResult uploadToSftp(String fileName) {
        try {
            String localFilePath = "/tmp/lta_test/" + fileName;
            String encryptedFileName = fileName + ".p7";
            String encryptedFilePath = "/tmp/lta_test/" + encryptedFileName;

            // Create encrypted file (simulate encryption by copying)
            File originalFile = new File(localFilePath);
            File encryptedFile = new File(encryptedFilePath);
            java.nio.file.Files.copy(originalFile.toPath(), encryptedFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Upload using Azure Blob Storage SFTP interface
            String blobName = "lta/vrls/output/" + encryptedFileName;
            
            // Read file content as bytes
            byte[] fileContent = java.nio.file.Files.readAllBytes(encryptedFile.toPath());
            
            // Upload to blob storage
            AzureBlobStorageUtil.FileUploadResponse uploadResponse = 
                azureBlobStorageUtil.uploadBytesToBlob(fileContent, blobName);
            
            if (!uploadResponse.isSuccess()) {
                throw new RuntimeException("Failed to upload file to blob storage: " + uploadResponse.getErrorMessage());
            }

            log.info("File uploaded to SFTP: {}", blobName);
            TestStepResult result = new TestStepResult("UPLOAD_TO_SFTP", "SUCCESS");
            result.addDetail("File uploaded successfully to SFTP: " + blobName);
            return result;

        } catch (Exception e) {
            log.error("Failed to upload file to SFTP", e);
            TestStepResult result = new TestStepResult("UPLOAD_TO_SFTP", "FAILED");
            result.addDetail("Failed to upload file: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 4: Trigger LTA Download Manual API
     */
    private TestStepResult runLtaDownloadManual() {
        try {
            String apiEndpoint = apiBaseUrl + "/ocms/v1/lta/download/manual";
            
            ResponseEntity<String> response = restTemplate.postForEntity(apiEndpoint, null, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("LTA Download Manual API called successfully");
                TestStepResult result = new TestStepResult("RUN_LTA_DOWNLOAD_MANUAL", "SUCCESS");
                result.addDetail("API called successfully: " + response.getBody());
                return result;
            } else {
                log.error("LTA Download Manual API failed with status: {}", response.getStatusCode());
                TestStepResult result = new TestStepResult("RUN_LTA_DOWNLOAD_MANUAL", "FAILED");
                result.addDetail("API failed with status: " + response.getStatusCode());
                return result;
            }

        } catch (Exception e) {
            log.error("Failed to call LTA Download Manual API", e);
            TestStepResult result = new TestStepResult("RUN_LTA_DOWNLOAD_MANUAL", "FAILED");
            result.addDetail("API call failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 5: Verify results - Check that processing ended early but basic data was updated
     * Expected: Processing stages unchanged (NPA->ROV), LTA data updated, suspension intact
     */
    private TestStepResult verifyResults() {
        try {
            log.info("Verifying suspended vehicle flow results");

            // Check processing stages (should remain unchanged: NPA->ROV)
            String stagesSql = """
                SELECT notice_no, prev_processing_stage, last_processing_stage, next_processing_stage,
                       suspension_type, epr_reason_of_suspension
                FROM ocmsizmgr.ocms_valid_offence_notice 
                WHERE notice_no = ?
                """;

            List<String> stageResults = jdbcTemplate.query(stagesSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("prev_processing_stage") + "->" + 
                               rs.getString("last_processing_stage") + "->" + 
                               rs.getString("next_processing_stage"));

            boolean stagesUnchanged = stageResults.size() == 1 && stageResults.get(0).contains("NPA->ROV");

            // Check vehicle data was updated (should contain LTA data, not TEMP_ values)
            String vehicleSql = """
                SELECT lta_chassis_number, lta_make_description, lta_primary_colour 
                FROM ocmsizmgr.ocms_offence_notice_detail 
                WHERE notice_no = ?
                """;

            List<String> vehicleResults = jdbcTemplate.query(vehicleSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("lta_chassis_number") + "|" + 
                               rs.getString("lta_make_description") + "|" + 
                               rs.getString("lta_primary_colour"));

            boolean vehicleDataUpdated = vehicleResults.size() == 1 && 
                vehicleResults.get(0).contains("STDCHASSIS789") && 
                vehicleResults.get(0).contains("TOYOTA CAMRY");

            // Check owner data was updated (should contain LTA data, not TEMP_ values)
            String ownerSql = """
                SELECT nric_no, name, reg_street, id_type 
                FROM ocmsizmgr.ocms_offence_notice_owner_driver 
                WHERE notice_no = ?
                """;

            List<String> ownerResults = jdbcTemplate.query(ownerSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("nric_no") + "|" + 
                               rs.getString("name") + "|" + 
                               rs.getString("reg_street"));

            boolean ownerDataUpdated = ownerResults.size() == 1 && 
                ownerResults.get(0).contains(OWNER_ID) && 
                ownerResults.get(0).contains(OWNER_NAME) && 
                ownerResults.get(0).contains("SUSPENDED STREET");

            // Check suspension configuration remains intact
            String suspensionSql = """
                SELECT suspension_type, reason_of_suspension, 
                       CASE WHEN date_of_revival IS NOT NULL THEN 'REVIVED' ELSE 'ACTIVE' END as revival_status
                FROM ocmsizmgr.ocms_suspended_notice 
                WHERE notice_no = ?
                """;

            List<String> suspensionResults = jdbcTemplate.query(suspensionSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("suspension_type") + "-" + 
                               rs.getString("reason_of_suspension") + "-" + 
                               rs.getString("revival_status"));

            boolean suspensionIntact = suspensionResults.size() == 1 && 
                suspensionResults.get(0).equals("TS-HST-REVIVED");

            if (stagesUnchanged && vehicleDataUpdated && ownerDataUpdated && suspensionIntact) {
                log.info("Suspended vehicle flow verification PASSED");
                TestStepResult result = new TestStepResult("VERIFY_RESULTS", "SUCCESS");
                result.addDetail("All verifications PASSED: Stages unchanged, LTA data updated, suspension intact (TS-HST REVIVED)");
                return result;
            } else {
                String details = String.format(
                    "Verification details: StagesUnchanged=%b, VehicleDataUpdated=%b, OwnerDataUpdated=%b, SuspensionIntact=%b", 
                    stagesUnchanged, vehicleDataUpdated, ownerDataUpdated, suspensionIntact);
                log.error("Suspended vehicle flow verification FAILED: {}", details);
                TestStepResult result = new TestStepResult("VERIFY_RESULTS", "FAILED");
                result.addDetail("Verification FAILED: " + details);
                return result;
            }

        } catch (DataAccessException e) {
            log.error("Failed to verify results", e);
            TestStepResult result = new TestStepResult("VERIFY_RESULTS", "FAILED");
            result.addDetail("Failed to verify results: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 6: Cleanup test data
     */
    private TestStepResult cleanup() {
        try {
            log.info("Cleaning up test data");

            String cleanupSql = """
                DELETE FROM ocmsizmgr.ocms_suspended_notice WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = ?;
                """;

            jdbcTemplate.update(cleanupSql, NOTICE_NO, NOTICE_NO, NOTICE_NO, NOTICE_NO);

            // Clean up temporary files
            String tempDir = "/tmp/lta_test";
            File directory = new File(tempDir);
            if (directory.exists()) {
                File[] files = directory.listFiles((dir, name) -> 
                    name.startsWith("VRL-URA-OFFREPLY-D2-") || name.equals("verification_results.txt"));
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }

            log.info("Cleanup completed successfully");
            TestStepResult result = new TestStepResult("CLEANUP", "SUCCESS");
            result.addDetail("Test data and files cleaned up successfully");
            return result;

        } catch (Exception e) {
            log.error("Failed to cleanup test data", e);
            TestStepResult result = new TestStepResult("CLEANUP", "FAILED");
            result.addDetail("Cleanup failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Create fixed-width record by delegating to ENA flow service
     * This reuses the same record creation logic to maintain consistency
     */
    private String createFixedWidthRecord(String vehicleNumber, String chassisNumber, String diplomaticFlag,
                                        String noticeNumber, String ownerIdType, String passportPlace,
                                        String ownerId, String ownerName, String addressType, String blockHouse,
                                        String streetName, String floorNumber, String unitNumber, String buildingName,
                                        String postalCode, String makeDescription, String primaryColour, String secondaryColour,
                                        String roadTaxExpiry, String unladenWeight, String maxLadenWeight,
                                        String effectiveOwnership, String deregistrationDate, String errorCode,
                                        String processingDate, String processingTime) {
        try {
            Method method = ltaStandardSnowflakeEnaFlowService.getClass().getDeclaredMethod("createFixedWidthRecord",
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class);
            method.setAccessible(true);
            
            return (String) method.invoke(ltaStandardSnowflakeEnaFlowService,
                vehicleNumber, chassisNumber, diplomaticFlag, noticeNumber, ownerIdType, passportPlace,
                ownerId, ownerName, addressType, blockHouse, streetName, floorNumber, unitNumber,
                buildingName, postalCode, makeDescription, primaryColour, secondaryColour,
                roadTaxExpiry, unladenWeight, maxLadenWeight, effectiveOwnership, deregistrationDate,
                errorCode, processingDate, processingTime);
                
        } catch (Exception e) {
            log.error("Failed to create fixed width record using reflection", e);
            throw new RuntimeException("Failed to create fixed width record", e);
        }
    }

}
