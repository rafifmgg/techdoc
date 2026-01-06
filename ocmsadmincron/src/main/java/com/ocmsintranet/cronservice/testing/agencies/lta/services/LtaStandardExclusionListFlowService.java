package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service to test the LTA Standard Vehicle with Owner in Exclusion List flow
 * This test verifies that a standard vehicle (not suspended) with owner in exclusion list
 * will have its processing stage set to RD1 after LTA download
 */
@Service
public class LtaStandardExclusionListFlowService {
    private static final Logger logger = LoggerFactory.getLogger(LtaStandardExclusionListFlowService.class);

    // Constants
    private static final String SCHEMA = "ocmsizmgr";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    // Test data constants
    private static final String TEST_ID = "TEST_STANDARD_EXCLUSION_LIST";
    private static final String TEST_NAME = "Standard Vehicle → Not Suspended → In Exclusion List → RD1";
    private static final String STANDARD_VEHICLE = "SMG308Y";
    private static final String OWNER_ID = "S9012384Z";
    private static final String OWNER_NAME = "Du Yi Min";
    private static final String NOTICE_NO = "EXC1000001";

    // Dependencies
    private final JdbcTemplate jdbcTemplate;
    private final SftpUtil sftpUtil;
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    private final RestTemplate restTemplate;

    // API base URL
    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Autowired
    public LtaStandardExclusionListFlowService(
            JdbcTemplate jdbcTemplate,
            SftpUtil sftpUtil,
            AzureBlobStorageUtil azureBlobStorageUtil,
            RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sftpUtil = sftpUtil;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
        this.restTemplate = restTemplate;
    }

    /**
     * Run the test for Standard Vehicle → Not Suspended → In Exclusion List → RD1 flow
     * @return List of TestStepResult objects for each step
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        // Step 1: Initialize test data
        TestStepResult step1 = initializeTestData();
        results.add(step1);
        if (STATUS_FAILED.equals(step1.getStatus())) {
            continueExecution = false;
        }

        // Step 2: Create LTA response file
        TestStepResult step2 = null;
        if (continueExecution) {
            step2 = createLtaResponseFile();
            results.add(step2);
            if (STATUS_FAILED.equals(step2.getStatus())) {
                continueExecution = false;
            }
        } else {
            step2 = new TestStepResult("Create LTA Response File", STATUS_SKIPPED);
            step2.addDetail("Previous step failed");
            results.add(step2);
        }

        // Step 3: Upload to SFTP
        TestStepResult step3 = null;
        if (continueExecution) {
            step3 = uploadToSftp(step2);
            results.add(step3);
            if (STATUS_FAILED.equals(step3.getStatus())) {
                continueExecution = false;
            }
        } else {
            step3 = new TestStepResult("Upload to SFTP", STATUS_SKIPPED);
            step3.addDetail("Previous step failed");
            results.add(step3);
        }

        // Step 4: Run LTA download manual API
        TestStepResult step4 = null;
        if (continueExecution) {
            step4 = runLtaDownloadManual();
            results.add(step4);
            if (STATUS_FAILED.equals(step4.getStatus())) {
                continueExecution = false;
            }
        } else {
            step4 = new TestStepResult("Run LTA Download Manual API", STATUS_SKIPPED);
            step4.addDetail("Previous step failed");
            results.add(step4);
        }

        // Step 5: Verify results
        TestStepResult step5 = null;
        if (continueExecution) {
            step5 = verifyResults(step3);
            results.add(step5);
        } else {
            step5 = new TestStepResult("Verify Results", STATUS_SKIPPED);
            step5.addDetail("Previous step failed");
            results.add(step5);
        }

        // Step 6: Cleanup
        TestStepResult step6 = cleanup();
        results.add(step6);

        return results;
    }

    /**
     * Initialize test data in database
     * @return TestStepResult with status and details
     */
    private TestStepResult initializeTestData() {
        TestStepResult result = new TestStepResult("Initialize Test Data", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            // Clean up existing test data
            cleanupTestData(result);

            // Insert data into ocms_valid_offence_notice
            String insertVonSql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice " +
                    "(notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, " +
                    "composition_amount, computer_rule_code, pp_code, " +
                    "prev_processing_stage, prev_processing_date, " +
                    "last_processing_stage, last_processing_date, " +
                    "next_processing_stage, next_processing_date, " +
                    "subsystem_label, " +
                    "cre_user_id, cre_date) " +
                    "VALUES (?, ?, 'S', 'O', CURRENT_TIMESTAMP, " +
                    "150.00, 203, 'PP04', " +
                    "NULL, NULL, " +
                    "'NPA', CURRENT_TIMESTAMP, " +
                    "'ROV', CURRENT_TIMESTAMP, " +
                    "'OCMS', " +
                    "'SYSTEM', CURRENT_TIMESTAMP)";

            jdbcTemplate.update(insertVonSql, NOTICE_NO, STANDARD_VEHICLE);
            details.append("✅ Inserted record into ocms_valid_offence_notice\n");

            // Insert data into ocms_offence_notice_detail
            String insertOndSql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail " +
                    "(notice_no, " +
                    "cre_date, cre_user_id, " +
                    "lta_chassis_number, lta_make_description, lta_primary_colour, lta_diplomatic_flag, " +
                    "lta_road_tax_expiry_date, lta_eff_ownership_date, lta_deregistration_date, " +
                    "lta_unladen_weight, lta_max_laden_weight) " +
                    "VALUES (?, " +
                    "CURRENT_TIMESTAMP, 'SYSTEM', " +
                    "'TEMP_CHASSIS', 'TEMP_MAKE', 'TEMP_COLOUR', 'N', " +
                    "NULL, NULL, NULL, " +
                    "NULL, NULL)";

            jdbcTemplate.update(insertOndSql, NOTICE_NO);
            details.append("✅ Inserted record into ocms_offence_notice_detail\n");

            // Insert data into ocms_offence_notice_owner_driver
            String insertOodSql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                    "(notice_no, owner_driver_indicator, " +
                    "cre_date, cre_user_id, life_status, " +
                    "lta_processing_date_time, mha_processing_date_time, " +
                    "id_type, nric_no, name, " +
                    "reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code, " +
                    "mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code, " +
                    "lta_error_code, passport_place_of_issue, " +
                    "lta_reg_address_effective_date, lta_mailing_address_effective_date) " +
                    "VALUES (?, 'O', " +
                    "CURRENT_TIMESTAMP, 'SYSTEM', 'A', " +
                    "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, " +
                    "'1', 'TEMP_ID', 'TEMP_NAME', " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, NULL, " +
                    "NULL, NULL)";

            jdbcTemplate.update(insertOodSql, NOTICE_NO);
            details.append("✅ Inserted record into ocms_offence_notice_owner_driver\n");

            // Insert data into ocms_enotification_exclusion_list
            String insertExclusionSql = "INSERT INTO " + SCHEMA + ".ocms_enotification_exclusion_list " +
                    "(id_no, remarks, cre_date, cre_user_id) " +
                    "VALUES (?, 'Test exclusion for automated testing', CURRENT_TIMESTAMP, 'SYSTEM')";

            jdbcTemplate.update(insertExclusionSql, OWNER_ID);
            details.append("✅ Inserted record into ocms_enotification_exclusion_list\n");

            // Verify data was inserted correctly
            int vonCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    Integer.class, NOTICE_NO);

            int ondCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?",
                    Integer.class, NOTICE_NO);

            int oodCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    Integer.class, NOTICE_NO);

            int exclusionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                    Integer.class, OWNER_ID);

            if (vonCount == 1 && ondCount == 1 && oodCount == 1 && exclusionCount == 1) {
                details.append("✅ Verified all records were inserted correctly\n");
            } else {
                throw new RuntimeException("Failed to verify inserted records");
            }

            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to initialize test data: " + e.getMessage());
            logger.error("Failed to initialize test data", e);
        }

        return result;
    }

    /**
     * Create LTA response file with fixed-width format
     * @return TestStepResult with status and details
     */
    private TestStepResult createLtaResponseFile() {
        TestStepResult result = new TestStepResult("Create LTA Response File", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();
        Path tempDir = null;
        File responseFile = null;

        try {
            // Create temporary directory
            tempDir = Files.createTempDirectory("lta_test_");

            // Generate filename with timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "VRL-URA-OFFREPLY-D2-" + timestamp;
            responseFile = new File(tempDir.toFile(), filename);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(responseFile))) {
                // Write header record
                String headerRecord = createAppendixFRecord(
                        "H", // Record type
                        "VRL-URA-OFFREPLY-D2", // File type
                        timestamp, // File creation date/time
                        "000001", // Sequence number
                        "", // Filler
                        ""); // Filler
                writer.write(headerRecord);
                writer.newLine();

                // Write data record for standard vehicle
                String dataRecord = createAppendixFRecord(
                        "D", // Record type
                        STANDARD_VEHICLE, // Vehicle number
                        "CAR", // Vehicle type
                        "TOYOTA", // Make
                        "COROLLA", // Model
                        "RED", // Primary color
                        "", // Secondary color
                        "N", // Suspended flag
                        "N", // Diplomatic flag
                        "20251231", // Road tax expiry date
                        "1200", // Unladen weight
                        "1800", // Max laden weight
                        "ABC123456789", // Chassis number
                        "20200101", // Effective ownership date
                        OWNER_ID, // Owner ID
                        "NRIC", // ID type
                        OWNER_NAME, // Owner name
                        "INDIVIDUAL", // Entity type
                        "", // Passport place of issue
                        "123 MAIN ST", // Address line 1
                        "", // Address line 2
                        "", // Address line 3
                        "SINGAPORE", // Address line 4
                        "123456"); // Postal code
                writer.write(dataRecord);
                writer.newLine();

                // Write trailer record
                String trailerRecord = createAppendixFRecord(
                        "T", // Record type
                        "00000001", // Record count
                        "", // Filler
                        "", // Filler
                        "", // Filler
                        ""); // Filler
                writer.write(trailerRecord);
                writer.newLine();
            }

            details.append("✅ Created LTA response file: " + responseFile.getAbsolutePath() + "\n");
            details.append("✅ File size: " + responseFile.length() + " bytes\n");

            // Store file path in result for next steps
            result.setJsonData(responseFile.getAbsolutePath());
            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to create LTA response file: " + e.getMessage());
            logger.error("Failed to create LTA response file", e);
        }

        return result;
    }

    /**
     * Upload LTA response file to SFTP server
     * @param previousResult TestStepResult from createLtaResponseFile step
     * @return TestStepResult with status and details
     */
    private TestStepResult uploadToSftp(TestStepResult previousResult) {
        TestStepResult result = new TestStepResult("Upload to SFTP", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            String filePath = (String) previousResult.getJsonData();
            if (filePath == null) {
                throw new RuntimeException("File path not found in previous step result");
            }

            File file = new File(filePath);
            String filename = file.getName();

            // Upload file to SFTP server
            String sftpServer = "lta";
            String sftpPath = "/vrls/output/";

            // Read file content
            byte[] fileContent = Files.readAllBytes(file.toPath());

            // Upload file to SFTP server
            boolean uploaded = sftpUtil.uploadFile(sftpServer, fileContent, sftpPath + filename);
            if (!uploaded) {
                throw new RuntimeException("Failed to upload file to SFTP server");
            }

            details.append("✅ Uploaded file to SFTP server: " + sftpServer + ":" + sftpPath + filename + "\n");

            // Upload file to Azure Blob Storage (simulating encryption)
            String containerName = "lta";
            String blobPath = "vrls/output/" + filename;

            boolean uploadedToBlob = azureBlobStorageUtil.uploadBytesToBlob(Files.readAllBytes(file.toPath()), containerName + "/" + blobPath).isSuccess();
            if (!uploadedToBlob) {
                throw new RuntimeException("Failed to upload file to Azure Blob Storage");
            }

            details.append("✅ Uploaded file to Azure Blob Storage: " + containerName + "/" + blobPath + "\n");

            // Store filename in result for next steps
            result.setJsonData(filename);
            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }

            // Wait for 5 seconds to ensure file is available
            Thread.sleep(5000);
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to upload file to SFTP: " + e.getMessage());
            logger.error("Failed to upload file to SFTP", e);
        }

        return result;
    }

    /**
     * Run LTA download manual API
     * @return TestStepResult with status and details
     */
    private TestStepResult runLtaDownloadManual() {
        TestStepResult result = new TestStepResult("Run LTA Download Manual API", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            // Call LTA download manual API
            String url = apiBaseUrl + "/ocms/v1/lta/download/manual";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                details.append("✅ Successfully called LTA download manual API\n");
                details.append("✅ Response status code: " + response.getStatusCode() + "\n");

                // Wait for 30 seconds to allow processing to complete
                details.append("⏳ Waiting 30 seconds for processing to complete...\n");
                Thread.sleep(30000);
                details.append("✅ Wait completed\n");
            } else {
                throw new RuntimeException("API call failed with status code: " + response.getStatusCode());
            }

            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to run LTA download manual API: " + e.getMessage());
            logger.error("Failed to run LTA download manual API", e);
        }

        return result;
    }

    /**
     * Verify results after LTA download
     * @param previousResult TestStepResult from uploadToSftp step containing filename
     * @return TestStepResult with status and details
     */
    private TestStepResult verifyResults(TestStepResult previousResult) {
        TestStepResult result = new TestStepResult("Verify Results", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            // Verify processing stages (should be NPA->ROV->RD1)
            String nextStage = jdbcTemplate.queryForObject(
                    "SELECT next_processing_stage FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String lastStage = jdbcTemplate.queryForObject(
                    "SELECT last_processing_stage FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            // Check suspension status
            Integer suspensionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ? AND suspension_type IS NOT NULL",
                    Integer.class, NOTICE_NO);
            
            String suspensionStatus = (suspensionCount > 0) ? "SUSPENDED" : "NOT_SUSPENDED";
            
            if ("RD1".equals(nextStage)) {
                details.append("✅ Processing stage is set to RD1 as expected\n");
                details.append("✅ Last processing stage: " + lastStage + "\n");
                details.append("✅ Suspension status: " + suspensionStatus + "\n");
            } else {
                throw new RuntimeException("Processing stage is not RD1, actual: " + nextStage);
            }

            // Verify vehicle data was updated with LTA data (should not be TEMP_ values)
            String chassisNumber = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(lta_chassis_number, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String makeDescription = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(lta_make_description, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String primaryColour = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(lta_primary_colour, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            if (!"TEMP_CHASSIS".equals(chassisNumber) && !"NULL".equals(chassisNumber)) {
                details.append("✅ Vehicle chassis data updated: " + chassisNumber + "\n");
            } else {
                details.append("⚠️ Vehicle chassis not updated: " + chassisNumber + "\n");
            }
            
            if (!"TEMP_MAKE".equals(makeDescription) && !"NULL".equals(makeDescription)) {
                details.append("✅ Vehicle make data updated: " + makeDescription + "\n");
            } else {
                details.append("⚠️ Vehicle make not updated: " + makeDescription + "\n");
            }
            
            if (!"TEMP_COLOUR".equals(primaryColour) && !"NULL".equals(primaryColour)) {
                details.append("✅ Vehicle colour data updated: " + primaryColour + "\n");
            } else {
                details.append("⚠️ Vehicle colour not updated: " + primaryColour + "\n");
            }

            // Verify owner data was updated with LTA data (should not be TEMP_ values)
            String ownerNric = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(nric_no, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String ownerName = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(name, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String ownerStreet = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(reg_street, 'NULL') FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            if (!"TEMP_ID".equals(ownerNric) && !"NULL".equals(ownerNric)) {
                details.append("✅ Owner NRIC data updated: " + ownerNric + "\n");
            } else {
                details.append("⚠️ Owner NRIC not updated: " + ownerNric + "\n");
            }
            
            if (!"TEMP_NAME".equals(ownerName) && !"NULL".equals(ownerName)) {
                details.append("✅ Owner name data updated: " + ownerName + "\n");
            } else {
                details.append("⚠️ Owner name not updated: " + ownerName + "\n");
            }
            
            if (ownerStreet != null && !"NULL".equals(ownerStreet)) {
                details.append("✅ Owner address data updated: " + ownerStreet + "\n");
            }

            // Verify exclusion list record remains intact
            int exclusionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                    Integer.class, OWNER_ID);
            
            if (exclusionCount == 1) {
                String exclusionRemarks = jdbcTemplate.queryForObject(
                        "SELECT remarks FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                        String.class, OWNER_ID);
                details.append("✅ Exclusion list record still exists\n");
                details.append("✅ Exclusion remarks: " + exclusionRemarks + "\n");
            } else {
                throw new RuntimeException("Exclusion list record not found");
            }

            // Verify file is processed and deleted from SFTP
            String filename = (String) previousResult.getJsonData();
            if (filename != null) {
                boolean fileExists = sftpUtil.fileExists("lta", "/vrls/output/" + filename);
                if (!fileExists) {
                    details.append("✅ File is processed and deleted from SFTP\n");
                } else {
                    details.append("⚠️ File still exists on SFTP server\n");
                }
            } else {
                details.append("⚠️ Could not verify file deletion - filename not available\n");
            }

            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to verify results: " + e.getMessage());
            logger.error("Failed to verify results", e);
        }

        return result;
    }

    /**
     * Clean up test data and temporary files
     * @return TestStepResult with status and details
     */
    private TestStepResult cleanup() {
        TestStepResult result = new TestStepResult("Cleanup", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            cleanupTestData(result);
            details.append("✅ Test data cleaned up successfully\n");

            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to clean up: " + e.getMessage());
            logger.error("Failed to clean up", e);
        }

        return result;
    }

    /**
     * Clean up test data from database
     * @param result TestStepResult to append details to
     */
    private void cleanupTestData(TestStepResult result) {
        StringBuilder details = new StringBuilder();

        try {
            // Delete from ocms_offence_notice_owner_driver
            int oodDeleted = jdbcTemplate.update(
                    "DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    NOTICE_NO);
            details.append("✅ Deleted " + oodDeleted + " records from ocms_offence_notice_owner_driver\n");

            // Delete from ocms_offence_notice_detail
            int ondDeleted = jdbcTemplate.update(
                    "DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?",
                    NOTICE_NO);
            details.append("✅ Deleted " + ondDeleted + " records from ocms_offence_notice_detail\n");

            // Delete from ocms_valid_offence_notice
            int vonDeleted = jdbcTemplate.update(
                    "DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    NOTICE_NO);
            details.append("✅ Deleted " + vonDeleted + " records from ocms_valid_offence_notice\n");

            // Delete from ocms_enotification_exclusion_list
            int exclusionDeleted = jdbcTemplate.update(
                    "DELETE FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                    OWNER_ID);
            details.append("✅ Deleted " + exclusionDeleted + " records from ocms_enotification_exclusion_list\n");

            // Append details to result if provided
            if (result != null) {
                for (String detail : details.toString().split("\n")) {
                    if (!detail.isEmpty()) {
                        result.addDetail(detail);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to clean up test data", e);
            if (result != null) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Failed to clean up test data: " + e.getMessage());
            }
        }
    }

    /**
     * Create a fixed-width record for Appendix F format
     * Total length must be 691 characters
     * @param fields Variable number of fields to include in the record
     * @return Fixed-width record string
     */
    private String createAppendixFRecord(String... fields) {
        StringBuilder record = new StringBuilder();

        for (String field : fields) {
            record.append(field == null ? "" : field);
            record.append(" ".repeat(100 - (field == null ? 0 : field.length())));
        }

        // Pad or truncate to exactly 691 characters
        int currentLength = record.length();
        if (currentLength < 691) {
            record.append(" ".repeat(691 - currentLength));
        } else if (currentLength > 691) {
            record.setLength(691);
        }

        return record.toString();
    }
}
