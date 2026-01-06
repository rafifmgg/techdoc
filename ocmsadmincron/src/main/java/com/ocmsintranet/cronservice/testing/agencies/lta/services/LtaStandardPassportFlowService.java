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
 * Service to test the LTA Standard Vehicle with Passport Holder flow
 * This test verifies that a standard vehicle (not suspended) with passport holder
 * (not in exclusion list) will have its processing stage set to RD1 after passport data query
 */
@Service
public class LtaStandardPassportFlowService {
    private static final Logger logger = LoggerFactory.getLogger(LtaStandardPassportFlowService.class);

    // Constants
    private static final String SCHEMA = "ocmsizmgr";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    // Test data constants
    private static final String TEST_ID = "TEST_STANDARD_PASSPORT_RD1";
    private static final String TEST_NAME = "Standard Vehicle → Not Suspended → NOT In Exclusion List → Passport → RD1";
    private static final String STANDARD_VEHICLE = "SMG308Y";
    private static final String OWNER_ID = "E1234567"; // Foreign passport format
    private static final String OWNER_NAME = "Du Yi Min";
    private static final String NOTICE_NO = "EXC1000002"; // Different notice number to avoid conflicts
    private static final String PASSPORT_PLACE = "SGP";

    // Dependencies
    private final JdbcTemplate jdbcTemplate;
    private final SftpUtil sftpUtil;
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    private final RestTemplate restTemplate;

    // API base URL
    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Autowired
    public LtaStandardPassportFlowService(
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
     * Run the test for Standard Vehicle → Not Suspended → NOT In Exclusion List → Passport → RD1 flow
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
     * Initialize test data in database (passport holder, NOT in exclusion list)
     */
    private TestStepResult initializeTestData() {
        TestStepResult result = new TestStepResult("Initialize Test Data", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            cleanupTestData(result);

            // Insert ocms_valid_offence_notice
            String insertVonSql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice " +
                    "(notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, " +
                    "composition_amount, computer_rule_code, pp_code, " +
                    "prev_processing_stage, prev_processing_date, " +
                    "last_processing_stage, last_processing_date, " +
                    "next_processing_stage, next_processing_date, " +
                    "subsystem_label, cre_user_id, cre_date) " +
                    "VALUES (?, ?, 'S', 'O', CURRENT_TIMESTAMP, " +
                    "150.00, 203, 'PP04', " +
                    "NULL, NULL, 'NPA', CURRENT_TIMESTAMP, " +
                    "'ROV', CURRENT_TIMESTAMP, 'OCMS', 'SYSTEM', CURRENT_TIMESTAMP)";
            jdbcTemplate.update(insertVonSql, NOTICE_NO, STANDARD_VEHICLE);
            details.append("✅ Inserted record into ocms_valid_offence_notice\n");

            // Insert ocms_offence_notice_detail
            String insertOndSql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail " +
                    "(notice_no, cre_date, cre_user_id, " +
                    "lta_chassis_number, lta_make_description, lta_primary_colour, lta_diplomatic_flag, " +
                    "lta_road_tax_expiry_date, lta_eff_ownership_date, lta_deregistration_date, " +
                    "lta_unladen_weight, lta_max_laden_weight) " +
                    "VALUES (?, CURRENT_TIMESTAMP, 'SYSTEM', " +
                    "'TEMP_CHASSIS', 'TEMP_MAKE', 'TEMP_COLOUR', 'N', " +
                    "NULL, NULL, NULL, NULL, NULL)";
            jdbcTemplate.update(insertOndSql, NOTICE_NO);
            details.append("✅ Inserted record into ocms_offence_notice_detail\n");

            // Insert ocms_offence_notice_owner_driver (passport holder)
            String insertOodSql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                    "(notice_no, owner_driver_indicator, " +
                    "cre_date, cre_user_id, life_status, " +
                    "lta_processing_date_time, mha_processing_date_time, " +
                    "id_type, nric_no, name, " +
                    "reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code, " +
                    "mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code, " +
                    "lta_error_code, passport_place_of_issue, " +
                    "lta_reg_address_effective_date, lta_mailing_address_effective_date) " +
                    "VALUES (?, 'O', CURRENT_TIMESTAMP, 'SYSTEM', 'A', " +
                    "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, " +
                    "'3', 'TEMP_PASSPORT_ID', 'TEMP_NAME', " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, " +
                    "NULL, ?, NULL, NULL)";
            jdbcTemplate.update(insertOodSql, NOTICE_NO, PASSPORT_PLACE);
            details.append("✅ Inserted passport holder record\n");

            // Ensure NO exclusion list entry
            int exclusionDeleted = jdbcTemplate.update(
                    "DELETE FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?", OWNER_ID);
            details.append("✅ Ensured NO exclusion list entry (deleted " + exclusionDeleted + " records)\n");

            // Verify data
            int vonCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    Integer.class, NOTICE_NO);
            int exclusionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                    Integer.class, OWNER_ID);

            if (vonCount == 1 && exclusionCount == 0) {
                details.append("✅ Verified all records created, NOT in exclusion list\n");
            } else {
                throw new RuntimeException("Failed to verify test data setup");
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

    private TestStepResult createLtaResponseFile() {
        TestStepResult result = new TestStepResult("Create LTA Response File", STATUS_SUCCESS);
        StringBuilder details = new StringBuilder();

        try {
            Path tempDir = Files.createTempDirectory("lta_test_");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "VRL-URA-OFFREPLY-D2-" + timestamp;
            File responseFile = new File(tempDir.toFile(), filename);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(responseFile))) {
                String headerRecord = createAppendixFRecord("H", timestamp, "", "", "", "");
                writer.write(headerRecord);
                writer.newLine();

                String dataRecord = createAppendixFRecord(
                        "D", STANDARD_VEHICLE, "3N1AB7APXHY000124", "N", NOTICE_NO, "3", 
                        PASSPORT_PLACE, OWNER_ID, OWNER_NAME, "C", "47", 
                        "FIGARO STREET", "60", "36", "", "503027", "KIA", 
                        "MAROON", "", "20251231", "1685", "2123", "20200101", "", "0");
                writer.write(dataRecord);
                writer.newLine();

                String trailerRecord = createAppendixFRecord("T", "00000001", "", "", "", "");
                writer.write(trailerRecord);
                writer.newLine();
            }

            details.append("✅ Created LTA response file with passport data\n");
            details.append("✅ ID Type=3, Passport Place=" + PASSPORT_PLACE + "\n");
            result.setJsonData(responseFile.getAbsolutePath());
            
            for (String detail : details.toString().split("\n")) {
                if (!detail.isEmpty()) {
                    result.addDetail(detail);
                }
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to create LTA response file: " + e.getMessage());
        }
        return result;
    }

    private TestStepResult uploadToSftp(TestStepResult previousResult) {
        TestStepResult result = new TestStepResult("Upload to SFTP", STATUS_SUCCESS);

        try {
            String filePath = (String) previousResult.getJsonData();
            File file = new File(filePath);
            String filename = file.getName();
            byte[] fileContent = Files.readAllBytes(file.toPath());

            boolean uploaded = sftpUtil.uploadFile("lta", fileContent, "/vrls/output/" + filename);
            if (!uploaded) throw new RuntimeException("SFTP upload failed");

            boolean uploadedToBlob = azureBlobStorageUtil.uploadBytesToBlob(
                    Files.readAllBytes(file.toPath()), "lta/vrls/output/" + filename).isSuccess();
            if (!uploadedToBlob) throw new RuntimeException("Azure blob upload failed");

            result.addDetail("✅ Uploaded to SFTP and Azure Blob");
            result.setJsonData(filename);
            Thread.sleep(5000);
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Upload failed: " + e.getMessage());
        }
        return result;
    }

    private TestStepResult runLtaDownloadManual() {
        TestStepResult result = new TestStepResult("Run LTA Download Manual API", STATUS_SUCCESS);

        try {
            String url = apiBaseUrl + "/ocms/v1/lta/download/manual";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>("{}"), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                result.addDetail("✅ API call successful: " + response.getStatusCode());
                Thread.sleep(30000); // Wait for processing
                result.addDetail("✅ Processing wait completed");
            } else {
                throw new RuntimeException("API call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ API call failed: " + e.getMessage());
        }
        return result;
    }

    private TestStepResult verifyResults(TestStepResult previousResult) {
        TestStepResult result = new TestStepResult("Verify Results", STATUS_SUCCESS);

        try {
            String nextStage = jdbcTemplate.queryForObject(
                    "SELECT next_processing_stage FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            if ("RD1".equals(nextStage)) {
                result.addDetail("✅ Processing stage is RD1 as expected");
            } else {
                throw new RuntimeException("Processing stage is not RD1, actual: " + nextStage);
            }

            String idType = jdbcTemplate.queryForObject(
                    "SELECT id_type FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    String.class, NOTICE_NO);
            
            String passportPlace = jdbcTemplate.queryForObject(
                    "SELECT passport_place_of_issue FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?",
                    String.class, NOTICE_NO);

            if ("3".equals(idType)) {
                result.addDetail("✅ ID type is 3 (Foreign Passport)");
            } else {
                result.addDetail("❌ ID type is not 3, actual: " + idType);
            }

            if (PASSPORT_PLACE.equals(passportPlace)) {
                result.addDetail("✅ Passport place is " + PASSPORT_PLACE);
            } else {
                result.addDetail("⚠️ Passport place not correct: " + passportPlace);
            }

            int exclusionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?",
                    Integer.class, OWNER_ID);
            
            if (exclusionCount == 0) {
                result.addDetail("✅ Owner is NOT in exclusion list (correct)");
            } else {
                result.addDetail("❌ Owner found in exclusion list (incorrect)");
            }

            String filename = (String) previousResult.getJsonData();
            if (filename != null) {
                boolean fileExists = sftpUtil.fileExists("lta", "/vrls/output/" + filename);
                result.addDetail(fileExists ? "⚠️ File still exists on SFTP" : "✅ File processed and deleted");
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Verification failed: " + e.getMessage());
        }
        return result;
    }

    private TestStepResult cleanup() {
        TestStepResult result = new TestStepResult("Cleanup", STATUS_SUCCESS);
        try {
            cleanupTestData(result);
            result.addDetail("✅ Test data cleaned up");
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Cleanup failed: " + e.getMessage());
        }
        return result;
    }

    private void cleanupTestData(TestStepResult result) {
        try {
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?", NOTICE_NO);
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?", NOTICE_NO);
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?", NOTICE_NO);
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_enotification_exclusion_list WHERE id_no = ?", OWNER_ID);
        } catch (Exception e) {
            logger.error("Failed to clean up test data", e);
        }
    }

    private String createAppendixFRecord(String... fields) {
        StringBuilder record = new StringBuilder();
        for (String field : fields) {
            record.append(field == null ? "" : field);
            record.append(" ".repeat(100 - (field == null ? 0 : field.length())));
        }
        
        int currentLength = record.length();
        if (currentLength < 691) {
            record.append(" ".repeat(691 - currentLength));
        } else if (currentLength > 691) {
            record.setLength(691);
        }
        return record.toString();
    }
}

