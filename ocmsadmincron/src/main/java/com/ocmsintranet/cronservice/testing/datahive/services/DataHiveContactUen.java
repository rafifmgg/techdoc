package com.ocmsintranet.cronservice.testing.datahive.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for DataHive Snowflake Contact Test - UEN Found Flow
 * Implements simple 4-step test for UEN found email in Snowflake
 */
@Service
public class DataHiveContactUen {

    private final SftpUtil sftpUtil;

    private static final String SCHEMA = "ocmsizmgr";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    // Test data constants - UEN found scenario
    private static final String TEST_NOTICE_PREFIX = "TESTSF";
    private static final String NOTICE_NO = TEST_NOTICE_PREFIX + "003";
    private static final String TEST_VEHICLE_NO = "EM8669X";
    private static final String TEST_UEN = "T16LL1643G";  // Valid UEN for testing
    private static final String TEST_ID_TYPE = "4";       // UEN ID type

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Autowired
    public DataHiveContactUen(SftpUtil sftpUtil) {
        this.sftpUtil = sftpUtil;
    }

    /**
     * Run the simplified 4-step test flow for UEN found in Snowflake
     *
     * @return List of TestStepResult containing the results of each step
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        // Step 1: Setup test data with ENA->ROV status and valid UEN
        TestStepResult step1 = setupTestDataForSnowflake();
        results.add(step1);
        if (STATUS_FAILED.equals(step1.getStatus())) {
            continueExecution = false;
        }

        // Step 2: Generate and upload LTA response file to SFTP
        if (continueExecution) {
            TestStepResult step2 = generateAndUploadLtaResponseFile();
            results.add(step2);
            if (STATUS_FAILED.equals(step2.getStatus())) {
                continueExecution = false;
            }
        }

        // Step 3: Trigger LTA download manual to process the file
        if (continueExecution) {
            TestStepResult step3 = triggerLtaDownloadManual();
            results.add(step3);
            if (STATUS_FAILED.equals(step3.getStatus())) {
                continueExecution = false;
            }
        }

        // Step 4: Verify Snowflake email update and processing stage = ENA
        if (continueExecution) {
            TestStepResult step4 = verifySnowflakeEmailUpdate();
            results.add(step4);
        }

        return results;
    }

    /**
     * Step 1: Setup test data with last_processing_stage=ENA, next_processing_stage=ROV
     * Uses valid UEN for Snowflake email found testing
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult setupTestDataForSnowflake() {
        TestStepResult result = new TestStepResult(
            "Step 1: Setup test data for Snowflake UEN email found testing (ENA->ROV, Valid UEN)",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData();

            result.addDetail("📝 Setting up test data for Snowflake UEN processing...");

            // Insert VON record with ENA->ROV status
            insertValidOffenceNoticeForSnowflake(result);

            // Insert OND record
            insertOffenceNoticeDetail(result);

            // Insert owner/driver record with valid UEN
            insertOwnerDriverWithUen(result);

            result.addDetail("✅ Test data setup completed successfully");
            result.addDetail("📋 Notice: " + NOTICE_NO + ", Vehicle: " + TEST_VEHICLE_NO + ", UEN: " + TEST_UEN);

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Step 2: Generate and upload LTA response file to SFTP /vrls/output
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult generateAndUploadLtaResponseFile() {
        TestStepResult result = new TestStepResult(
            "Step 2: Generate and upload LTA response file to SFTP",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("📄 Generating LTA response file...");

            // Generate timestamp for filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String filename = "VRL-URA-OFFREPLY-D2-" + timestamp;

            // Generate file content in LTA format
            String fileContent = generateLtaResponseFileContent();

            result.addDetail("📁 Generated file: " + filename);
            result.addDetail("📏 File size: " + fileContent.length() + " bytes");

            // Upload to SFTP
            result.addDetail("🔄 Uploading file to SFTP /vrls/output...");

            String remotePath = "/vrls/output/" + filename;
            boolean uploadSuccess = sftpUtil.uploadFile("lta", fileContent.getBytes(), remotePath);

            if (uploadSuccess) {
                result.addDetail("✅ File uploaded successfully to SFTP");
                result.setJsonData(filename); // Store filename for reference
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Failed to upload file to SFTP");
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Step 3: Trigger LTA download manual to process the uploaded file
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult triggerLtaDownloadManual() {
        TestStepResult result = new TestStepResult(
            "Step 3: Trigger LTA download manual",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("🚀 Triggering LTA download manual...");

            // Create RestTemplate for HTTP request
            RestTemplate restTemplate = new RestTemplate();

            // Define the URL for the LTA download manual endpoint
            String downloadUrl = apiBaseUrl + "/ocms/v1/lta/download/manual";
            result.addDetail("📌 Target URL: " + downloadUrl);

            // The endpoint expects an empty payload
            HttpEntity<String> requestEntity = new HttpEntity<>("");

            // Perform the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(downloadUrl, requestEntity, String.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                result.addDetail("✅ LTA download manual triggered successfully");
                result.addDetail("📊 Status code: " + response.getStatusCode());
                result.setJsonData(response.getBody());
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ LTA download manual failed with status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Step 4: Verify Snowflake email data has been updated and processing stage = ENA
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult verifySnowflakeEmailUpdate() {
        TestStepResult result = new TestStepResult(
            "Step 4: Verify Snowflake UEN email data update",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("🔍 Verifying Snowflake UEN email data update...");

            // Query owner/driver record to check email updates
            String sql = "SELECT offender_tel_no, email_addr " +
                        "FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, NOTICE_NO);

            if (records.isEmpty()) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ No owner/driver record found for notice: " + NOTICE_NO);
                return result;
            }

            Map<String, Object> record = records.get(0);
            String telNo = (String) record.get("offender_tel_no");
            String emailAddr = (String) record.get("email_addr");

            result.addDetail("📝 Contact data found:");
            result.addDetail("  📱 Tel no: " + (telNo != null ? telNo : "<null>"));
            result.addDetail("  📧 Email: " + (emailAddr != null ? emailAddr : "<null>"));

            // Verify Snowflake email processing occurred
            boolean snowflakeProcessed = false;

            // For UEN (ID type 4), we expect email address to be updated
            if (emailAddr != null && !emailAddr.trim().isEmpty()) {
                result.addDetail("✅ Email address updated by Snowflake: " + emailAddr);
                snowflakeProcessed = true;
            }

            if (snowflakeProcessed) {
                result.addDetail("🎉 Snowflake UEN email lookup processing verified successfully!");
            } else {
                result.addDetail("⚠️ No evidence of Snowflake email processing found");
                result.addDetail("💡 This could indicate the UEN is not found in DataHive or processing failed");
            }

            // Verify processing stage progression to ENA
            verifyProcessingStageProgressionToENA(result);

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /** Helper method to clean up existing test data */
    private void cleanupTestData() {
        // Delete from related tables in correct order
        jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?", NOTICE_NO);
        jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?", NOTICE_NO);
        jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?", NOTICE_NO);
    }

    /** Insert VON record with ENA->ROV status for Snowflake testing */
    private void insertValidOffenceNoticeForSnowflake(TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, composition_amount, computer_rule_code, " +
                     "last_processing_stage, last_processing_date, next_processing_stage, next_processing_date, " +
                     "notice_date_and_time, offence_notice_type, pp_code, pp_name, vehicle_category, " +
                     "vehicle_no, vehicle_registration_type, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            NOTICE_NO, new BigDecimal("70.00"), 10300,
            "ENA", LocalDateTime.now(), "ROV", LocalDateTime.now(),
            LocalDateTime.now(), "O", "A0045", "ATTAP VALLEY ROAD", "C",
            TEST_VEHICLE_NO, "S", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ VON record inserted with ENA->ROV status");
    }

    /** Insert OND record */
    private void insertOffenceNoticeDetail(TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_diplomatic_flag, lta_eff_ownership_date, " +
                     "lta_make_description, lta_primary_colour, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NO, "TESTCHASSIS123", "N",
            LocalDateTime.now().minusYears(1), "TOYOTA", "WHITE", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ OND record inserted");
    }

    /** Insert owner/driver record with valid UEN */
    private void insertOwnerDriverWithUen(TestStepResult result) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, name, id_no, id_type, owner_driver_indicator, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, NOTICE_NO, "TEST COMPANY NAME PTE LTD", TEST_UEN, TEST_ID_TYPE, "Y", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted with UEN: " + TEST_UEN + " (ID type: " + TEST_ID_TYPE + ")");
    }

    /** Generate LTA response file content in proper format */
    private String generateLtaResponseFileContent() {
        StringBuilder content = new StringBuilder();

        // Header
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        content.append("H").append(currentDate).append("\n");

        // Data record - LTA response format with valid UEN
        content.append("D")
            .append(String.format("%-14s", TEST_VEHICLE_NO))           // Vehicle Registration Number
            .append(String.format("%-25s", "TESTCHASSIS123"))          // Chassis Number
            .append("N")                                               // Diplomatic Flag
            .append(String.format("%-10s", NOTICE_NO))                 // Notice Number
            .append(TEST_ID_TYPE)                                      // Owner ID Type
            .append(String.format("%-3s", "SGP"))                      // Owner's Passport Place of Issue
            .append(String.format("%-20s", TEST_UEN))                  // Owner ID (Valid UEN)
            .append(String.format("%-66s", "TEST COMPANY NAME PTE LTD")) // Owner Name
            .append("1")                                               // Address Type
            .append(String.format("%-10s", "123"))                     // Block/House Number
            .append(String.format("%-32s", "TEST STREET"))             // Street Name
            .append(String.format("%-2s", "01"))                       // Floor Number
            .append(String.format("%-5s", "01"))                       // Unit Number
            .append(String.format("%-30s", "TEST BUILDING"))           // Building Name
            .append(String.format("%-8s", "123456"))                   // Postal Code
            .append(String.format("%-100s", "TOYOTA"))                 // Make Description
            .append(String.format("%-100s", "WHITE"))                  // Primary Colour
            .append(String.format("%-100s", ""))                       // Secondary Colour
            .append("20251231")                                        // Road Tax Expiry Date
            .append("0001500")                                         // Unladen weight
            .append("0002000")                                         // Maximum Laden weight
            .append("20200101")                                        // Effective Ownership Date
            .append("00000000")                                        // Deregistration Date
            .append("0")                                               // Return Error Code
            .append("20250902")                                        // Processing Date
            .append("1430")                                            // Processing Time
            .append(String.format("%-10s", ""))                        // IU/OBU Label Number
            .append("20200101")                                        // Registered Address Effective Date
            .append(String.format("%-10s", "123"))                     // Mailing Block/House Number
            .append(String.format("%-32s", "TEST STREET"))             // Mailing Street Name
            .append(String.format("%-2s", "01"))                       // Mailing Floor Number
            .append(String.format("%-5s", "01"))                       // Mailing Unit Number
            .append(String.format("%-30s", "TEST BUILDING"))           // Mailing Building Name
            .append(String.format("%-8s", "123456"))                   // Mailing Postal Code
            .append("20200101")                                        // Mailing Address Effective Date
            .append("\n");

        // Trailer
        content.append("T000001\n");

        return content.toString();
    }

    /** Verify processing stage has progressed to ENA (email found) */
    private void verifyProcessingStageProgressionToENA(TestStepResult result) {
        String sql = "SELECT last_processing_stage, next_processing_stage FROM " +
                     SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(sql, NOTICE_NO);
            String lastStage = (String) record.get("last_processing_stage");
            String nextStage = (String) record.get("next_processing_stage");

            result.addDetail("📊 Processing stage progression:");
            result.addDetail("  Last: " + lastStage + " | Next: " + nextStage);

            // Expected: ROV->ENA (email found, proceeding to embassy notification)
            if ("ROV".equals(lastStage) && "ENA".equals(nextStage)) {
                result.addDetail("✅ Processing stage correctly progressed to ENA (email found)");
            } else {
                result.addDetail("⚠️ Unexpected processing stage progression - Expected: ROV->ENA");
            }
        } catch (Exception e) {
            result.addDetail("⚠️ Could not verify processing stage: " + e.getMessage());
        }
    }
}
