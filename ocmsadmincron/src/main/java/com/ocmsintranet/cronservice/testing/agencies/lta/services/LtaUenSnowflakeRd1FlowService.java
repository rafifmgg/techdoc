package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service untuk testing UEN Snowflake RD1 Flow (UEN without email, patch to RD1).
 * Mengkonversi script shell test-uen-snowflake-without-email-RD1.sh ke Java.
 */
@Service
public class LtaUenSnowflakeRd1FlowService {

    private static final Logger logger = LoggerFactory.getLogger(LtaUenSnowflakeRd1FlowService.class);

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AzureBlobStorageUtil azureBlobStorageUtil;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Menjalankan complete flow test untuk UEN Snowflake RD1.
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();

        try {
            // Step 1: Setup test data
            results.add(setupTestData());

            // Step 2: Check RD1 stagedays parameter
            results.add(checkRd1StageParameter());

            // Step 3: Create LTA response file
            results.add(createLtaResponseFile());

            // Step 4: Upload file to Azure Blob Storage
            results.add(uploadFileToAzureBlob());

            // Step 5: Trigger LTA download API
            results.add(triggerLtaDownloadApi());

            // Step 6: Wait for processing
            results.add(waitForProcessing());

            // Step 7: Verify processing results
            results.add(verifyProcessingResults());

            // Step 8: Cleanup test data
            results.add(cleanupTestData());

        } catch (Exception e) {
            logger.error("❌ Error during UEN Snowflake RD1 flow test", e);
            TestStepResult errorResult = new TestStepResult("Error", STATUS_FAILED);
            errorResult.addDetail("Test execution failed: " + e.getMessage());
            results.add(errorResult);
        }

        return results;
    }

    /**
     * Step 1: Setup test data untuk UEN entity tanpa email.
     */
    private TestStepResult setupTestData() {
        try {
            logger.info("🔧 Setting up test data for UEN Snowflake RD1 flow...");

            // Cleanup existing test data first
            cleanupExistingTestData();

            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Insert into ocms_valid_offence_notice
            String vonSql = """
                INSERT INTO ocms_valid_offence_notice
                (offence_notice_id, pp_code, pp_name, vehicle_no, offence_datetime, offence_location,
                 offence_code, violation_type, fine_amount, last_processing_stage, next_processing_stage,
                 cre_user_id, cre_date, upd_user_id, upd_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(vonSql,
                "TESTUEN001", "PP001", "PARKING OFFENCE", "TEST001A",
                currentDateTime, "TEST LOCATION", "1402", "P", 70.00,
                "NEW", "ROV", "SYSTEM", currentDateTime, "SYSTEM", currentDateTime);

            // Insert into ocms_offence_notice_detail
            String ondSql = """
                INSERT INTO ocms_offence_notice_detail
                (offence_notice_id, lta_chassis_number, lta_make_description, lta_primary_colour,
                 lta_secondary_colour, lta_diplomatic_flag, lta_road_tax_expiry_date, lta_unladen_weight,
                 lta_max_laden_weight, lta_eff_ownership_date, cre_user_id, cre_date, upd_user_id, upd_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(ondSql,
                "TESTUEN001", "TESTCHASSIS001", "TOYOTA", "WHITE", "WHITE", "N",
                "20251231", 1200, 1800, "20200101", "SYSTEM", currentDateTime, "SYSTEM", currentDateTime);

            // Insert into ocms_offence_notice_owner_driver (UEN entity)
            String ownerSql = """
                INSERT INTO ocms_offence_notice_owner_driver
                (offence_notice_id, name, id_no, id_type, entity_type, passport_place_of_issue,
                 cre_user_id, cre_date, upd_user_id, upd_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(ownerSql,
                "TESTUEN001", "TEST COMPANY PTE LTD", "201234567A", "4", "C", "SGP",
                "SYSTEM", currentDateTime, "SYSTEM", currentDateTime);

            logger.info("✅ Test data setup completed successfully");
            TestStepResult result = new TestStepResult("Setup Test Data", STATUS_SUCCESS);
            result.addDetail("Test data inserted: VON=TESTUEN001, UEN=201234567A, Vehicle=TEST001A");
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to setup test data", e);
            TestStepResult result = new TestStepResult("Setup Test Data", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Check dan create RD1 stagedays parameter jika belum ada.
     */
    private TestStepResult checkRd1StageParameter() {
        try {
            logger.info("🔍 Checking RD1 stagedays parameter...");

            String checkSql = "SELECT COUNT(*) FROM ocms_parameter WHERE param_code = 'RD1' AND param_name = 'STAGEDAYS'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                String insertSql = """
                    INSERT INTO ocms_parameter (param_code, param_name, param_value, param_description,
                                               cre_user_id, cre_date, upd_user_id, upd_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                jdbcTemplate.update(insertSql,
                    "RD1", "STAGEDAYS", "14", "RD1 Stage Days for Testing",
                    "SYSTEM", currentDateTime, "SYSTEM", currentDateTime);

                logger.info("✅ RD1 STAGEDAYS parameter created");
                TestStepResult result = new TestStepResult("Check RD1 Parameter", STATUS_SUCCESS);
                result.addDetail("RD1 STAGEDAYS parameter created with value: 14");
                return result;
            } else {
                logger.info("✅ RD1 STAGEDAYS parameter already exists");
                TestStepResult result = new TestStepResult("Check RD1 Parameter", STATUS_SUCCESS);
                result.addDetail("RD1 STAGEDAYS parameter already exists");
                return result;
            }

        } catch (Exception e) {
            logger.error("❌ Failed to check RD1 parameter", e);
            TestStepResult result = new TestStepResult("Check RD1 Parameter", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Create LTA response file dengan data UEN (no email found).
     */
    private TestStepResult createLtaResponseFile() {
        try {
            logger.info("📄 Creating LTA response file for UEN (no email)...");

            // Create temp directory
            Path tempDir = Paths.get("/tmp/lta_test");
            Files.createDirectories(tempDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;
            Path filePath = tempDir.resolve(fileName);

            // Create fixed-width record using reflection (similar to ENA flow)
            String fixedWidthRecord = createFixedWidthRecord();

            // Write file with Header, Data, and Trailer
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Header
                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                writer.write("H" + dateStr + "\n");

                // Data record (UEN without email - no error code)
                writer.write(fixedWidthRecord + "\n");

                // Trailer
                writer.write("T000001\n");
            }

            logger.info("✅ LTA response file created: " + fileName);
            TestStepResult result = new TestStepResult("Create LTA Response File", STATUS_SUCCESS);
            result.addDetail("File created: " + fileName + " (691 chars per record)");
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to create LTA response file", e);
            TestStepResult result = new TestStepResult("Create LTA Response File", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Create fixed-width record untuk UEN entity.
     */
    private String createFixedWidthRecord() throws Exception {
        // Use reflection to call LtaUenSnowflakeEnaFlowService.createFixedWidthRecord method
        try {
            Class<?> enaServiceClass = Class.forName("com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaUenSnowflakeEnaFlowService");
            Method createRecordMethod = enaServiceClass.getDeclaredMethod("createFixedWidthRecord");
            createRecordMethod.setAccessible(true);

            // Create instance (we need this for the method call)
            Object enaServiceInstance = enaServiceClass.getDeclaredConstructor().newInstance();

            return (String) createRecordMethod.invoke(enaServiceInstance);
        } catch (Exception e) {
            logger.warn("⚠️  Failed to use reflection, creating record manually", e);
            return createManualFixedWidthRecord();
        }
    }

    /**
     * Manual creation of fixed-width record jika reflection gagal.
     */
    private String createManualFixedWidthRecord() {
        StringBuilder record = new StringBuilder();

        // Fixed positions based on Appendix F specification (691 characters total)
        record.append("D");                                    // RecordType (A1)
        record.append(String.format("%-12s", "TEST001A"));     // Vehicle Registration Number (A12)
        record.append(String.format("%-25s", "TESTCHASSIS001")); // Chassis Number (A25)
        record.append("N");                                    // Diplomatic Flag (A1)
        record.append(String.format("%-10s", "TESTUEN001"));   // Notice Number (A10)
        record.append("4");                                    // Owner ID Type (A1) - UEN
        record.append(String.format("%-3s", "SGP"));           // Owner's Passport Place of Issue (A3)
        record.append(String.format("%-20s", "201234567A"));   // Owner ID (A20)
        record.append(String.format("%-66s", "TEST COMPANY PTE LTD")); // Owner Name (A66)
        record.append("1");                                    // Address Type (A1)
        record.append(String.format("%-10s", "123"));          // Block/House Number (A10)
        record.append(String.format("%-32s", "TEST STREET"));  // Street Name (A32)
        record.append(String.format("%-2s", "01"));            // Floor Number (A2)
        record.append(String.format("%-5s", "01"));            // Unit Number (A5)
        record.append(String.format("%-30s", "TEST BUILDING"));// Building Name (A30)
        record.append(String.format("%-8s", "123456"));        // Postal Code (A8)
        record.append(String.format("%-100s", "TOYOTA"));      // Make Description (A100)
        record.append(String.format("%-100s", "WHITE"));       // Primary Colour (A100)
        record.append(String.format("%-100s", "WHITE"));       // Secondary Colour (A100)
        record.append(String.format("%8s", "20251231"));       // Road Tax Expiry Date (N8)
        record.append(String.format("%7d", 1200));             // Unladen weight (N7)
        record.append(String.format("%7d", 1800));             // Maximum Laden weight (N7)
        record.append(String.format("%8s", "20200101"));       // Effective Ownership Date (N8)
        record.append(String.format("%8s", ""));               // Deregistration Date (N8)
        record.append(" ");                                    // Return Error Code (A1) - no error
        record.append(String.format("%8s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))); // Processing Date (N8)
        record.append(String.format("%4s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm")))); // Processing Time (N4)
        record.append(String.format("%-10s", ""));             // IU/OBU Label Number (A10)
        record.append(String.format("%8s", "20200101"));       // Registered Address Effective Date (N8)
        record.append(String.format("%-10s", "123"));          // Mailing Block/House Number (A10)
        record.append(String.format("%-32s", "TEST STREET"));  // Mailing Street Name (A32)
        record.append(String.format("%-2s", "01"));            // Mailing Floor Number (A2)
        record.append(String.format("%-5s", "01"));            // Mailing Unit Number (A5)
        record.append(String.format("%-30s", "TEST BUILDING"));// Mailing Building Name (A30)
        record.append(String.format("%-8s", "123456"));        // Mailing Postal Code (A8)
        record.append(String.format("%8s", "20200101"));       // Mailing Address Effective Date (N8)

        // Pad to exactly 691 characters
        while (record.length() < 691) {
            record.append(" ");
        }

        return record.toString();
    }

    /**
     * Step 4: Upload file ke Azure Blob Storage (simulating SFTP).
     */
    private TestStepResult uploadFileToAzureBlob() {
        try {
            logger.info("⬆️  Uploading LTA response file to Azure Blob Storage...");

            Path tempDir = Paths.get("/tmp/lta_test");
            File[] files = tempDir.toFile().listFiles((dir, name) -> name.startsWith("VRL-URA-OFFREPLY-D2-"));

            if (files == null || files.length == 0) {
                throw new RuntimeException("No LTA response file found to upload");
            }

            // Get the most recent file
            File fileToUpload = Arrays.stream(files)
                .max(Comparator.comparing(File::getName))
                .orElseThrow(() -> new RuntimeException("No file found"));

            // Read file content
            byte[] fileContent = Files.readAllBytes(fileToUpload.toPath());

            // Upload to Azure Blob Storage
            String blobPath = "lta/vrls/output/" + fileToUpload.getName();
            AzureBlobStorageUtil.FileUploadResponse uploadResponse = 
                azureBlobStorageUtil.uploadBytesToBlob(fileContent, blobPath);
            
            if (uploadResponse.isSuccess()) {
                logger.info("✅ File uploaded successfully to Azure Blob Storage: " + blobPath);
                TestStepResult result = new TestStepResult("Upload File to Azure Blob", STATUS_SUCCESS);
                result.addDetail("File uploaded: " + blobPath + " (" + fileContent.length + " bytes)");
                return result;
            } else {
                throw new RuntimeException("Azure Blob Storage upload failed: " + uploadResponse.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("❌ Failed to upload file to Azure Blob Storage", e);
            TestStepResult result = new TestStepResult("Upload File to Azure Blob", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 5: Trigger LTA download API untuk memproses file.
     */
    private TestStepResult triggerLtaDownloadApi() {
        try {
            logger.info("🚀 Triggering LTA download API...");

            String apiUrl = apiBaseUrl + "/ocms/v1/lta/download/manual";

            // Make POST request with empty body
            String response = restTemplate.postForObject(apiUrl, "", String.class);

            logger.info("✅ LTA download API triggered successfully");
            logger.debug("API Response: " + response);

            TestStepResult result = new TestStepResult("Trigger LTA Download API", STATUS_SUCCESS);
            result.addDetail("API called: " + apiUrl + ", Response: " + (response != null ? response : "Empty"));
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to trigger LTA download API", e);
            TestStepResult result = new TestStepResult("Trigger LTA Download API", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 6: Wait for processing (30 seconds).
     */
    private TestStepResult waitForProcessing() {
        try {
            logger.info("⏳ Waiting for processing (30 seconds)...");
            Thread.sleep(30000); // 30 seconds
            logger.info("✅ Processing wait completed");
            TestStepResult result = new TestStepResult("Wait for Processing", STATUS_SUCCESS);
            result.addDetail("Waited 30 seconds for processing");
            return result;
        } catch (InterruptedException e) {
            logger.error("❌ Wait interrupted", e);
            Thread.currentThread().interrupt();
            TestStepResult result = new TestStepResult("Wait for Processing", STATUS_FAILED);
            result.addDetail("Wait interrupted: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 7: Verify processing results untuk UEN RD1 flow.
     */
    private TestStepResult verifyProcessingResults() {
        try {
            logger.info("✅ Verifying processing results for UEN RD1 flow...");

            StringBuilder details = new StringBuilder();
            boolean allChecksPass = true;

            // Check processing stage update (ROV → RD1)
            String stageSql = """
                SELECT last_processing_stage, next_processing_stage, upd_user_id, upd_date
                FROM ocms_valid_offence_notice
                WHERE offence_notice_id = 'TESTUEN001'
                """;

            Map<String, Object> stageResult = jdbcTemplate.queryForMap(stageSql);
            String lastStage = (String) stageResult.get("last_processing_stage");
            String nextStage = (String) stageResult.get("next_processing_stage");

            if ("ROV".equals(lastStage) && "RD1".equals(nextStage)) {
                details.append("✅ Processing stage updated correctly: ").append(lastStage).append(" → ").append(nextStage).append("\n");
            } else {
                details.append("❌ Processing stage incorrect: ").append(lastStage).append(" → ").append(nextStage).append("\n");
                allChecksPass = false;
            }

            // Check Snowflake call timestamp
            String snowflakeSql = """
                SELECT snowflake_last_call_datetime
                FROM ocms_offence_notice_owner_driver
                WHERE offence_notice_id = 'TESTUEN001'
                """;

            Object snowflakeTimestamp = jdbcTemplate.queryForObject(snowflakeSql, Object.class);
            if (snowflakeTimestamp != null) {
                details.append("✅ Snowflake call recorded: ").append(snowflakeTimestamp).append("\n");
            } else {
                details.append("❌ Snowflake call not recorded\n");
                allChecksPass = false;
            }

            // Check no email found (should be null/empty)
            String emailSql = """
                SELECT email
                FROM ocms_offence_notice_owner_driver
                WHERE offence_notice_id = 'TESTUEN001'
                """;

            String email = jdbcTemplate.queryForObject(emailSql, String.class);
            if (email == null || email.trim().isEmpty()) {
                details.append("✅ No email found (expected for RD1 flow)\n");
            } else {
                details.append("❌ Email found unexpectedly: ").append(email).append("\n");
                allChecksPass = false;
            }

            // Check audit fields updated
            String updUser = (String) stageResult.get("upd_user_id");
            Object updDate = stageResult.get("upd_date");

            if (updUser != null && updDate != null) {
                details.append("✅ Audit fields updated: user=").append(updUser).append(", date=").append(updDate).append("\n");
            } else {
                details.append("❌ Audit fields not properly updated\n");
                allChecksPass = false;
            }

            String status = allChecksPass ? STATUS_SUCCESS : STATUS_FAILED;
            logger.info("📊 Verification completed: " + status);

            TestStepResult result = new TestStepResult("Verify Processing Results", status);
            result.addDetail(details.toString());
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to verify processing results", e);
            TestStepResult result = new TestStepResult("Verify Processing Results", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 8: Cleanup test data.
     */
    private TestStepResult cleanupTestData() {
        try {
            logger.info("🧹 Cleaning up test data...");
            cleanupExistingTestData();

            // Clean up temp files
            Path tempDir = Paths.get("/tmp/lta_test");
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .filter(path -> path.getFileName().toString().startsWith("VRL-URA-OFFREPLY-D2-"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete temp file: " + path, e);
                        }
                    });
            }

            logger.info("✅ Test data cleanup completed");
            TestStepResult result = new TestStepResult("Cleanup Test Data", STATUS_SUCCESS);
            result.addDetail("Test data and temp files cleaned up");
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to cleanup test data", e);
            TestStepResult result = new TestStepResult("Cleanup Test Data", STATUS_FAILED);
            result.addDetail("Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Helper method untuk cleanup existing test data.
     */
    private void cleanupExistingTestData() {
        try {
            jdbcTemplate.update("DELETE FROM ocms_offence_notice_owner_driver WHERE offence_notice_id = 'TESTUEN001'");
            jdbcTemplate.update("DELETE FROM ocms_offence_notice_detail WHERE offence_notice_id = 'TESTUEN001'");
            jdbcTemplate.update("DELETE FROM ocms_valid_offence_notice WHERE offence_notice_id = 'TESTUEN001'");
            logger.debug("Existing test data cleaned up");
        } catch (Exception e) {
            logger.warn("Warning during cleanup: " + e.getMessage());
        }
    }
}
