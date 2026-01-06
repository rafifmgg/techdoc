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
public class LtaUenSnowflakeEnaFlowService {

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
    private static final String TEST_ID = "TEST_UEN_SNOWFLAKE_WITH_EMAIL_ENA";
    private static final String TEST_NAME = "UEN ID Type → Corppass with email → ENA";
    private static final String STANDARD_VEHICLE = "SBB1234C";
    private static final String OWNER_ID = "S1234567A"; // UEN format with email in Corppass table
    private static final String OWNER_NAME = "TAN WEI MING";
    private static final String NOTICE_NO = "UEN1000001";
    private static final String EXPECTED_EMAIL = "tan.weiming@email.com";

    /**
     * Main method to run the UEN Snowflake ENA test flow
     * Expected: Standard Vehicle → NOT Suspended → UEN (ID Type 4) → Snowflake call → Email found → ENA
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        log.info("Starting LTA UEN Snowflake ENA Flow Test: {}", TEST_NAME);

        try {
            // Step 1: Initialize test data (NOT suspended, NOT in exclusion list)
            results.add(initializeTestData());

            // Step 2: Verify ENA STAGEDAYS parameter exists
            results.add(verifyEnaStagedays());

            // Step 3: Create LTA response file (UEN success case)
            String fileName = createLtaResponseFile();
            TestStepResult createFileResult = new TestStepResult("CREATE_LTA_RESPONSE_FILE", "SUCCESS");
            createFileResult.addDetail("LTA response file created successfully: " + fileName);
            results.add(createFileResult);

            // Step 4: Upload file to SFTP
            results.add(uploadToSftp(fileName));

            // Step 5: Trigger LTA Download Manual API
            results.add(runLtaDownloadManual());

            // Step 6: Wait for processing
            Thread.sleep(30000); // 30 seconds wait

            // Step 7: Verify results (UEN → Snowflake → Email found → ENA)
            results.add(verifyResults());

            // Step 8: Cleanup test data
            results.add(cleanup());

            log.info("LTA UEN Snowflake ENA Flow Test completed successfully");
            return results;

        } catch (Exception e) {
            log.error("LTA UEN Snowflake ENA Flow Test failed", e);
            TestStepResult failedResult = new TestStepResult("UEN_SNOWFLAKE_ENA_FLOW_TEST", "FAILED");
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
     * Step 1: Initialize test data - UEN (NOT suspended, NOT in exclusion list)
     * Creates records with ID Type = 4 (UEN), no suspension, no exclusion list entry
     */
    private TestStepResult initializeTestData() {
        try {
            log.info("Initializing test data for UEN Snowflake ENA flow");

            String sql = """
                -- Clean up existing test records (in correct order due to foreign keys)
                DELETE FROM ocmsizmgr.ocms_enotification_exclusion_list WHERE id_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = ?;

                -- 1. Insert ocms_valid_offence_notice WITHOUT SUSPENSION (standard processing)
                INSERT INTO ocmsizmgr.ocms_valid_offence_notice 
                (notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, 
                composition_amount, computer_rule_code, pp_code, 
                prev_processing_stage, prev_processing_date,
                last_processing_stage, last_processing_date,
                next_processing_stage, next_processing_date, 
                subsystem_label, 
                cre_user_id, cre_date) 
                VALUES 
                (?, ?, 'S', 'O', GETDATE(), 150.00, 203, 'PP04', 
                NULL, NULL, 'NPA', GETDATE(), 'ROV', GETDATE(), 'OCMS',
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

                -- 3. Insert ocms_offence_notice_owner_driver with UEN (ID Type = 4)
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
                '4', ?, ?,
                '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                '123', 'ORCHARD ROAD', '05', '12', 'LUCKY PLAZA', '238861',
                NULL, NULL,
                NULL, NULL);
                """;

            // Execute the SQL with parameters
            jdbcTemplate.update(sql, 
                OWNER_ID, NOTICE_NO, NOTICE_NO, NOTICE_NO, // cleanup parameters
                NOTICE_NO, STANDARD_VEHICLE, // ocms_valid_offence_notice parameters
                NOTICE_NO, // ocms_offence_notice_detail parameter
                NOTICE_NO, OWNER_ID, OWNER_NAME // ocms_offence_notice_owner_driver parameters
            );

            log.info("Test data initialized successfully for UEN ID Type");
            TestStepResult result = new TestStepResult("INITIALIZE_TEST_DATA", "SUCCESS");
            result.addDetail("Test data created with UEN (ID Type 4), NOT suspended, NOT in exclusion list");
            return result;

        } catch (DataAccessException e) {
            log.error("Failed to initialize test data", e);
            TestStepResult result = new TestStepResult("INITIALIZE_TEST_DATA", "FAILED");
            result.addDetail("Failed to create test data: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Verify ENA STAGEDAYS parameter exists, create if missing
     * This parameter is critical for ENA stage processing
     */
    private TestStepResult verifyEnaStagedays() {
        try {
            log.info("Verifying ENA STAGEDAYS parameter");

            // Check if ENA STAGEDAYS parameter exists
            String checkSql = """
                SELECT value 
                FROM ocmsizmgr.ocms_parameter 
                WHERE code = 'ENA' AND parameter_id = 'STAGEDAYS'
                """;

            List<String> values = jdbcTemplate.query(checkSql, 
                (rs, rowNum) -> rs.getString("value"));

            if (values.isEmpty()) {
                log.warn("ENA STAGEDAYS parameter not found, creating it");
                
                String insertSql = """
                    INSERT INTO ocmsizmgr.ocms_parameter 
                    (code, parameter_id, description, value, cre_date, cre_user_id) 
                    VALUES 
                    ('ENA', 'STAGEDAYS', 'Processing stage duration for ENA', '4', GETDATE(), 'TEST_SYSTEM')
                    """;

                jdbcTemplate.update(insertSql);
                
                TestStepResult result = new TestStepResult("VERIFY_ENA_STAGEDAYS", "SUCCESS");
                result.addDetail("ENA STAGEDAYS parameter created with value: 4 days");
                return result;
            } else {
                String stageDays = values.get(0);
                log.info("ENA STAGEDAYS parameter found: {} days", stageDays);
                
                TestStepResult result = new TestStepResult("VERIFY_ENA_STAGEDAYS", "SUCCESS");
                result.addDetail("ENA STAGEDAYS parameter exists with value: " + stageDays + " days");
                return result;
            }

        } catch (DataAccessException e) {
            log.error("Failed to verify ENA STAGEDAYS parameter", e);
            TestStepResult result = new TestStepResult("VERIFY_ENA_STAGEDAYS", "FAILED");
            result.addDetail("Failed to verify parameter: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Create LTA response file with UEN SUCCESS data
     * Creates fixed-width format file with UEN owner (ID Type = 4)
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

            // UEN vehicle record with SUCCESS data (ID Type = 4, error code 0)
            String uenRecord = createFixedWidthRecord(
                STANDARD_VEHICLE,        // vehicle_number
                "WBAJC5109JB084371",     // chassis_number (BMW chassis)
                "N",                     // diplomatic_flag
                NOTICE_NO,               // notice_number
                "4",                     // owner_id_type (UEN)
                "",                      // passport_place (not applicable for UEN)
                OWNER_ID,                // owner_id (UEN)
                OWNER_NAME,              // owner_name
                "C",                     // address_type (Corporate)
                "47",                    // block_house
                "FIGARO STREET",         // street_name
                "60",                    // floor_number
                "36",                    // unit_number
                "",                      // building_name
                "503027",                // postal_code
                "BMW",                   // make_description
                "BLACK",                 // primary_colour
                "",                      // secondary_colour
                LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // road_tax_expiry
                "1685",                  // unladen_weight
                "2123",                  // max_laden_weight
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), // effective_ownership
                "",                      // deregistration_date
                "0",                     // error_code (SUCCESS)
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), // processing_date
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"))      // processing_time
            );
            writer.write(uenRecord + "\n");

            // Trailer record (691 characters)
            String trailerRecord = String.format("T%-6s%-684s", "000001", "");
            writer.write(trailerRecord + "\n");
        }

        log.info("LTA response file created: {}", filePath);
        return fileName;
    }

    /**
     * Step 4: Upload file to SFTP server
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
     * Step 5: Trigger LTA Download Manual API
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
     * Step 6: Verify results - Check UEN Snowflake ENA flow worked correctly
     * Expected: Processing stage = ENA, Snowflake called, email populated
     */
    private TestStepResult verifyResults() {
        try {
            log.info("Verifying UEN Snowflake ENA flow results");

            // Check processing stages (should be updated to ENA)
            String stagesSql = """
                SELECT notice_no, last_processing_stage, next_processing_stage, next_processing_date
                FROM ocmsizmgr.ocms_valid_offence_notice 
                WHERE notice_no = ?
                """;

            List<String> stageResults = jdbcTemplate.query(stagesSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("last_processing_stage") + "->" + 
                               rs.getString("next_processing_stage"));

            boolean stagesCorrect = stageResults.size() == 1 && stageResults.get(0).contains("->ENA");

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
                vehicleResults.get(0).contains("WBAJC5109JB084371") && 
                vehicleResults.get(0).contains("BMW");

            // Check owner data was updated AND Snowflake was called
            String ownerSql = """
                SELECT nric_no, name, id_type, email_addr, offender_tel_no, datahive_processing_datetime
                FROM ocmsizmgr.ocms_offence_notice_owner_driver 
                WHERE notice_no = ?
                """;

            List<String> ownerResults = jdbcTemplate.query(ownerSql, new Object[]{NOTICE_NO}, 
                (rs, rowNum) -> rs.getString("nric_no") + "|" + 
                               rs.getString("name") + "|" + 
                               rs.getString("id_type") + "|" +
                               (rs.getString("email_addr") != null ? rs.getString("email_addr") : "NULL") + "|" +
                               (rs.getTimestamp("datahive_processing_datetime") != null ? "SNOWFLAKE_CALLED" : "NULL"));

            boolean ownerDataUpdated = ownerResults.size() == 1 && 
                ownerResults.get(0).contains(OWNER_ID) && 
                ownerResults.get(0).contains(OWNER_NAME) && 
                ownerResults.get(0).contains("4"); // UEN ID Type

            boolean snowflakeCalled = ownerResults.size() == 1 && 
                ownerResults.get(0).contains("SNOWFLAKE_CALLED");

            boolean emailFound = ownerResults.size() == 1 && 
                ownerResults.get(0).contains(EXPECTED_EMAIL);

            if (stagesCorrect && vehicleDataUpdated && ownerDataUpdated && snowflakeCalled && emailFound) {
                log.info("UEN Snowflake ENA flow verification PASSED");
                TestStepResult result = new TestStepResult("VERIFY_RESULTS", "SUCCESS");
                result.addDetail("All verifications PASSED: Stage=ENA, Snowflake called, email found (" + EXPECTED_EMAIL + ")");
                return result;
            } else {
                String details = String.format(
                    "Verification details: StagesCorrect=%b, VehicleDataUpdated=%b, OwnerDataUpdated=%b, SnowflakeCalled=%b, EmailFound=%b", 
                    stagesCorrect, vehicleDataUpdated, ownerDataUpdated, snowflakeCalled, emailFound);
                log.error("UEN Snowflake ENA flow verification FAILED: {}", details);
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
     * Step 7: Cleanup test data
     */
    private TestStepResult cleanup() {
        try {
            log.info("Cleaning up test data");

            String cleanupSql = """
                DELETE FROM ocmsizmgr.ocms_enotification_exclusion_list WHERE id_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_owner_driver WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_offence_notice_detail WHERE notice_no = ?;
                DELETE FROM ocmsizmgr.ocms_valid_offence_notice WHERE notice_no = ?;
                """;

            jdbcTemplate.update(cleanupSql, OWNER_ID, NOTICE_NO, NOTICE_NO, NOTICE_NO);

            // Clean up temporary files
            String tempDir = "/tmp/lta_test";
            File directory = new File(tempDir);
            if (directory.exists()) {
                File[] files = directory.listFiles((dir, name) -> 
                    name.startsWith("VRL-URA-OFFREPLY-D2-"));
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
