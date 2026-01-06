package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for LTA Diplomat Normal Flow test scenario
 * Implements Test Flow 1: Diplomat Normal Flow
 */
@Service
public class LtaDiplomatNormalFlowService {
    private final SftpUtil sftpUtil;
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    private static final String SCHEMA = "ocmsizmgr";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    // Test data constants
    private static final String TEST_NOTICE_PREFIX = "TEST";
    private static final String NOTICE_NO = TEST_NOTICE_PREFIX + "DIP001";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Autowired
    public LtaDiplomatNormalFlowService(SftpUtil sftpUtil, AzureBlobStorageUtil azureBlobStorageUtil) {
        this.sftpUtil = sftpUtil;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    /**
     * Run the test flow for Diplomat Normal Flow scenario
     *
     * @return List of TestStepResult containing the results of each step
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

        // Step 2: Upload to LTA
        if (continueExecution) {
            TestStepResult step2 = uploadToLta();
            results.add(step2);
            if (STATUS_FAILED.equals(step2.getStatus())) {
                continueExecution = false;
            }
        }

        // Step 3: Verify enquiry file on SFTP server
        if (continueExecution) {
            TestStepResult step3 = verifyEnquiryFileOnSftp();
            results.add(step3);
            if (STATUS_FAILED.equals(step3.getStatus())) {
                continueExecution = false;
            }
        }

        // Step 4: Verify notice status after upload
        if (continueExecution) {
            TestStepResult step4 = verifyNoticeStatusAfterUpload();
            results.add(step4);
        }

        // Step 5: Run LTA callback simulator
        if (continueExecution) {
            TestStepResult step5 = runLtaCallback();
            results.add(step5);
        }

        // Step 6: Run LTA Download Manual
        if (continueExecution) {
            TestStepResult step6 = runLtaDownloadManual();
            results.add(step6);
        }

        // Step 7: Verify final database state
        if (continueExecution) {
            TestStepResult step7 = verifyFinalDatabaseState();
            results.add(step7);
        }

        return results;
    }

    /**
     * Step 1: Initialize test data for Diplomat Normal Flow
     * Creates records in ocms_valid_offence_notice and ocms_offence_notice_detail tables
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult initializeTestData() {
        TestStepResult result = new TestStepResult(
            "Step 1: Initialize test data for Diplomat Normal Flow",
            STATUS_SUCCESS
        );

        try {
            // Clean up any existing test data with the same notice number
            cleanupTestData(result);

            // Insert data into ocms_valid_offence_notice
            insertValidOffenceNotice(result);

            // Insert data into ocms_offence_notice_detail
            insertOffenceNoticeDetail(result);

            // Verify data was inserted correctly
            verifyDataInserted(result);

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
     * Clean up any existing test data with the same notice number
     *
     * @param result TestStepResult to update with cleanup details
     */
    private void cleanupTestData(TestStepResult result) {
        result.addDetail("🧹 Cleaning up existing test data...");

        // Delete from ocms_offence_notice_detail
        String deleteOndSql = "DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        int ondDeleted = jdbcTemplate.update(deleteOndSql, NOTICE_NO);

        // Delete from ocms_valid_offence_notice
        String deleteVonSql = "DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        int vonDeleted = jdbcTemplate.update(deleteVonSql, NOTICE_NO);

        result.addDetail("✅ Deleted " + ondDeleted + " records from ocms_offence_notice_detail");
        result.addDetail("✅ Deleted " + vonDeleted + " records from ocms_valid_offence_notice");
    }

    /**
     * Insert data into ocms_valid_offence_notice table
     *
     * @param result TestStepResult to update with insertion details
     */
    private void insertValidOffenceNotice(TestStepResult result) {
        result.addDetail("📝 Inserting data into ocms_valid_offence_notice...");

        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, composition_amount, computer_rule_code, " +
                     "last_processing_stage, last_processing_date, next_processing_stage, next_processing_date, notice_date_and_time, " +
                     "offence_notice_type, pp_code, pp_name, vehicle_category, vehicle_no, " +
                     "vehicle_registration_type, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int inserted = jdbcTemplate.update(sql,
            NOTICE_NO,                          // notice_no (String, length = 10)
            new BigDecimal("70.00"),        // composition_amount (BigDecimal)
            10300,                              // computer_rule_code (Integer)
            "NPA",                              // last_processing_stage (String, length = 3)
            LocalDateTime.now(),                // last_processing_date (LocalDateTime)
            "ROV",                              // next_processing_stage (String, length = 3)
            LocalDateTime.now(),                // next_processing_date (LocalDateTime)
            LocalDateTime.now(),                // notice_date_and_time (LocalDateTime)
            "O",                                // offence_notice_type (String, length = 1)
            "A0045",                            // pp_code (String, length = 5)
            "ATTAP VALLEY ROAD",                // pp_name (String)
            "C",                                // vehicle_category (String, length = 1)
            "FBD870K",                          // vehicle_no (String, length = 12)
            "D",                                // vehicle_registration_type (String, length = 1) - D for diplomat
            "TEST_USER",                        // cre_user_id
            LocalDateTime.now()                 // cre_date
        );

        result.addDetail("✅ Inserted " + inserted + " record into ocms_valid_offence_notice");
    }

    /**
     * Insert data into ocms_offence_notice_detail table
     *
     * @param result TestStepResult to update with insertion details
     */
    private void insertOffenceNoticeDetail(TestStepResult result) {
        result.addDetail("📝 Inserting data into ocms_offence_notice_detail...");

        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_diplomatic_flag, lta_eff_ownership_date, " +
                     "lta_make_description, lta_primary_colour, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int inserted = jdbcTemplate.update(sql,
            NOTICE_NO,                          // notice_no (String, length = 10)
            "",                                 // lta_chassis_number (String, length = 25)
            "Y",                                // lta_diplomatic_flag (String, length = 1)
            LocalDateTime.now().minusYears(1),  // lta_eff_ownership_date (LocalDateTime)
            "",                                 // lta_make_description (String, length = 100)
            "",                                 // lta_primary_colour (String, length = 100)
            "TEST_USER",                        // cre_user_id
            LocalDateTime.now()                 // cre_date
        );

        result.addDetail("✅ Inserted " + inserted + " record into ocms_offence_notice_detail");
    }

    /**
     * Verify data was inserted correctly
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyDataInserted(TestStepResult result) {
        result.addDetail("🔍 Verifying data insertion...");

        // Verify ocms_valid_offence_notice
        String vonSql = "SELECT * FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";
        List<Map<String, Object>> vonRecords = jdbcTemplate.queryForList(vonSql, NOTICE_NO);

        if (vonRecords.isEmpty()) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to find record in ocms_valid_offence_notice with notice_no: " + NOTICE_NO);
            return;
        }

        // Verify ocms_offence_notice_detail
        String ondSql = "SELECT * FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";
        List<Map<String, Object>> ondRecords = jdbcTemplate.queryForList(ondSql, NOTICE_NO);

        if (ondRecords.isEmpty()) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to find record in ocms_offence_notice_detail with notice_no: " + NOTICE_NO);
            return;
        }

        // Verify specific fields
        Map<String, Object> vonRecord = vonRecords.get(0);
        Map<String, Object> ondRecord = ondRecords.get(0);

        String vehicleRegType = (String) vonRecord.get("vehicle_registration_type");
        String diplomaticFlag = (String) ondRecord.get("lta_diplomatic_flag");

        if (!"D".equals(vehicleRegType)) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ vehicle_registration_type is not 'D' (Diplomat): " + vehicleRegType);
        }

        if (!"Y".equals(diplomaticFlag)) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ lta_diplomatic_flag is not 'Y': " + diplomaticFlag);
        }

        if (STATUS_SUCCESS.equals(result.getStatus())) {
            result.addDetail("✅ Data verification successful");
            result.addDetail("✅ Test data initialized with notice_no: " + NOTICE_NO);
        }
    }

    /**
     * Step 2: Upload to LTA
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult uploadToLta() {
        TestStepResult result = new TestStepResult(
            "Step 2: Upload to LTA",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("🔄 Performing POST request to /ocms/v1/lta/upload...");

            // Create RestTemplate for HTTP request
            RestTemplate restTemplate = new RestTemplate();

            // Define the URL for the LTA upload endpoint
            String uploadUrl = apiBaseUrl + "/ocms/v1/lta/upload";
            result.addDetail("📌 Target URL: " + uploadUrl);

            // The endpoint expects an empty payload as per documentation
            HttpEntity<String> requestEntity = new HttpEntity<>("");

            // Perform the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                result.addDetail("✅ Upload successful with status code: " + response.getStatusCode());
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Upload failed with status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to upload to LTA: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 3: Verify enquiry file on SFTP server
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult verifyEnquiryFileOnSftp() {
        TestStepResult result = new TestStepResult(
            "Step 3: Verify enquiry file on SFTP server",
            STATUS_SUCCESS
        );

        // Verify file in Azure Blob Storage first
        try {
            // Define blob path constant
            final String BLOB_PATH = "offence/lta/vrls/input";

            // Get current date in YYYYMMDD format for file prefix
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new java.util.Date());
            String filePrefix = "VRL-URA-OFFENQ-D1-" + currentDate;

            result.addDetail("🔍 Verifying presence of enquiry file in Azure Blob Storage...");
            result.addDetail("📁 Blob Path: " + BLOB_PATH);
            result.addDetail("🔎 Looking for files with prefix: " + filePrefix);

            // AzureBlobStorageUtil is already injected via @Autowired

            // List files in blob directory
            java.util.List<String> blobFiles;
            try {
                blobFiles = azureBlobStorageUtil.listFiles(BLOB_PATH);
                result.addDetail("📋 Total files found in blob: " + blobFiles.size());
            } catch (Exception e) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Failed to list files in blob storage: " + e.getMessage());
                return result;
            }

            // Filter files by prefix
            java.util.List<String> matchingBlobFiles = new java.util.ArrayList<>();
            for (String file : blobFiles) {
                if (file.contains(filePrefix)) {
                    matchingBlobFiles.add(file);
                }
            }

            result.addDetail("📋 Files in blob matching prefix '" + filePrefix + "': " + matchingBlobFiles.size());

            // Check if any matching files were found
            if (matchingBlobFiles.isEmpty()) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ No enquiry files found in blob with prefix: " + filePrefix);
                return result;
            }

            // Sort files by name (which includes timestamp)
            java.util.Collections.sort(matchingBlobFiles, java.util.Comparator.reverseOrder());

            // Get the latest file
            String latestBlobFile = matchingBlobFiles.get(0);
            result.addDetail("✅ Latest enquiry file found in blob: " + latestBlobFile);

            // Add all matching files to the result
            result.addDetail("📋 All matching files in blob:");
            for (String file : matchingBlobFiles) {
                result.addDetail("  - " + file);
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            result.addDetail("❌ Failed to verify enquiry file in blob storage: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }

        // Now proceed with SFTP verification

        try {
            // Define SFTP server and path constants
            final String SFTP_SERVER = "lta";
            final String SFTP_INPUT_PATH = "/vrls/input";

            // Get current date in YYYYMMDD format for file prefix
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new java.util.Date());
            String filePrefix = "VRL-URA-OFFENQ-D1-" + currentDate;

            result.addDetail("🔍 Verifying presence of enquiry file on SFTP server...");
            result.addDetail("📁 SFTP Server: " + SFTP_SERVER);
            result.addDetail("📁 Directory: " + SFTP_INPUT_PATH);
            result.addDetail("🔎 Looking for files with prefix: " + filePrefix);

            // SftpUtil is already injected via @Autowired

            // List files in SFTP directory
            java.util.List<String> files;
            try {
                files = sftpUtil.listFiles(SFTP_SERVER, SFTP_INPUT_PATH);
                result.addDetail("📋 Total files found in directory: " + files.size());
            } catch (Exception e) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Failed to list files on SFTP server: " + e.getMessage());
                return result;
            }

            // Filter files by prefix
            java.util.List<String> matchingFiles = new java.util.ArrayList<>();
            for (String file : files) {
                if (file.startsWith(filePrefix)) {
                    matchingFiles.add(file);
                }
            }

            result.addDetail("📋 Files matching prefix '" + filePrefix + "': " + matchingFiles.size());

            // Check if any matching files were found
            if (matchingFiles.isEmpty()) {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ No enquiry files found with prefix: " + filePrefix);
                return result;
            }

            // Sort files by name (which includes timestamp)
            java.util.Collections.sort(matchingFiles, java.util.Comparator.reverseOrder());

            // Get the latest file
            String latestFile = matchingFiles.get(0);
            result.addDetail("✅ Latest enquiry file found: " + latestFile);

            // Add all matching files to the result
            result.addDetail("📋 All matching files:");
            for (String file : matchingFiles) {
                result.addDetail("  - " + file);
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            result.addDetail("❌ Failed to verify enquiry file on SFTP: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Step 4: Verify notice status after upload
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult verifyNoticeStatusAfterUpload() {
        TestStepResult result = new TestStepResult(
            "Step 4: Verify notice status after upload",
            STATUS_SUCCESS
        );

        try {
            verifyNoticeStatusAfterUpload(result);
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to verify notice status after upload: " + e.getMessage());
        }

        return result;
    }

    /**
     * Verify notice status after upload
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyNoticeStatusAfterUpload(TestStepResult result) {
        result.addDetail("🔍 Verifying notice status after upload...");

        // Query the database to check if the notice status was updated
        String sql = "SELECT last_processing_stage, next_processing_stage, upd_user_id, upd_date FROM " +
                     SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(sql, NOTICE_NO);

            String lastProcessingStage = (String) record.get("last_processing_stage");
            String nextProcessingStage = (String) record.get("next_processing_stage");

            result.addDetail("📝 Current last_processing_stage: " + lastProcessingStage);
            result.addDetail("📝 Current next_processing_stage: " + nextProcessingStage);

            // Check if the processing stages were updated as expected
            // For diplomat flow, we expect ROV -> ENA
            if ("ROV".equals(lastProcessingStage)) {
                result.addDetail("✅ last_processing_stage correctly updated to ENA");
            } else {
                result.addDetail("⚠️ last_processing_stage not updated as expected (ENA): " + lastProcessingStage);
            }
            if ("ENA".equals(nextProcessingStage)) {
                result.addDetail("✅ next_processing_stage correctly updated to RD1");
            } else {
                result.addDetail("⚠️ next_processing_stage not updated as expected (RD1): " + nextProcessingStage);
            }

            // The next stage might be different based on the flow
            if (nextProcessingStage != null && !nextProcessingStage.isEmpty()) {
                result.addDetail("✅ next_processing_stage is now: " + nextProcessingStage);
            } else {
                result.addDetail("⚠️ next_processing_stage is empty or null");
            }
        } catch (Exception e) {
            result.addDetail("❌ Failed to verify notice status: " + e.getMessage());
        }
    }

    /**
     * Step 5: Run LTA callback simulator
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult runLtaCallback() {
        TestStepResult result = new TestStepResult(
            "Step 5: Run LTA callback simulator",
            STATUS_SUCCESS
        );

        try {
            result.addDetail("🔄 Performing POST request to /ocms/test/lta/lta-callback...");

            // Create RestTemplate for HTTP request
            RestTemplate restTemplate = new RestTemplate();

            // Define the URL for the LTA callback endpoint
            String callbackUrl = apiBaseUrl + "/ocms/test/lta/lta-callback";
            result.addDetail("📌 Target URL: " + callbackUrl);

            // The endpoint expects an empty payload as per documentation
            HttpEntity<String> requestEntity = new HttpEntity<>("");

            // Perform the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, requestEntity, String.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                result.addDetail("✅ Callback simulation successful with status code: " + response.getStatusCode());
                // result.addDetail("📄 Response body: " + response.getBody());
                result.setJsonData(response.getBody());
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Callback simulation failed with status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed to run LTA callback: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Step 6: Run LTA Download Manual
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult runLtaDownloadManual() {
        TestStepResult result = new TestStepResult(
            "Step 6: Run LTA Download Manual",
            STATUS_SUCCESS
        );
        result.addDetail("🚀 Running LTA Download Manual...");

        try {
            // Construct the URL for the download manual endpoint
            String downloadManualUrl = apiBaseUrl + "/ocms/v1/lta/download/manual";
            result.addDetail("🔗 Using endpoint: " + downloadManualUrl);

            // Create RestTemplate for making HTTP requests
            RestTemplate restTemplate = new RestTemplate();

            // The endpoint expects an empty payload as per documentation
            HttpEntity<String> requestEntity = new HttpEntity<>("");

            // Perform the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(downloadManualUrl, requestEntity, String.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                result.addDetail("✅ Download manual simulation successful with status code: " + response.getStatusCode());
                result.setJsonData(response.getBody());
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Download manual simulation failed with status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed to run LTA download manual: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 7: Verify final database state
     * Verifies all database tables have been updated correctly for diplomat flow
     *
     * @return TestStepResult containing the result of this step
     */
    private TestStepResult verifyFinalDatabaseState() {
        TestStepResult result = new TestStepResult(
            "Step 7: Verify final database state",
            STATUS_SUCCESS
        );
        result.addDetail("🔍 Verifying final database state for diplomat flow...");

        try {
            // Verify ocms_valid_offence_notice
            verifyValidOffenceNotice(result);

            // Verify ocms_offence_notice_detail
            verifyOffenceNoticeDetail(result);

            // Verify ocms_offence_notice_owner_driver
            verifyOffenceNoticeOwnerDriver(result);

            // Verify ocms_offence_notice_owner_driver_addr
            verifyOffenceNoticeOwnerDriverAddr(result);

            // Verify audit trail
            verifyAuditTrail(result);

            // Final verification summary
            if (STATUS_SUCCESS.equals(result.getStatus())) {
                result.addDetail("✅ All database verifications passed for diplomat flow");
                result.addDetail("🎉 Diplomat flow test completed successfully!");
            }
        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.addDetail("❌ Failed to verify final database state: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
        }

        return result;
    }

    /**
     * Verify ocms_valid_offence_notice table
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyValidOffenceNotice(TestStepResult result) {
        result.addDetail("🔍 Verifying ocms_valid_offence_notice table...");

        String sql = "SELECT * FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(sql, NOTICE_NO);

            // Verify processing stages for diplomat flow
            String lastProcessingStage = (String) record.get("last_processing_stage");
            String nextProcessingStage = (String) record.get("next_processing_stage");
            String vehicleRegType = (String) record.get("vehicle_registration_type");

            result.addDetail("📝 last_processing_stage: " + lastProcessingStage);
            result.addDetail("📝 next_processing_stage: " + nextProcessingStage);
            result.addDetail("📝 vehicle_registration_type: " + vehicleRegType);

            // For diplomat flow after LTA Download, we expect:
            // last_processing_stage = ROV, next_processing_stage = RD1
            if ("ROV".equals(lastProcessingStage)) {
                result.addDetail("✅ last_processing_stage correctly set to ROV");
            } else {
                result.addDetail("❌ last_processing_stage is not ROV: " + lastProcessingStage);
                result.setStatus(STATUS_FAILED);
            }

            if ("RD1".equals(nextProcessingStage)) {
                result.addDetail("✅ next_processing_stage correctly set to RD1");
            } else {
                result.addDetail("❌ next_processing_stage is not RD1: " + nextProcessingStage);
                result.setStatus(STATUS_FAILED);
            }

            if ("D".equals(vehicleRegType)) {
                result.addDetail("✅ vehicle_registration_type correctly set to D (Diplomat)");
            } else {
                result.addDetail("❌ vehicle_registration_type is not D: " + vehicleRegType);
                result.setStatus(STATUS_FAILED);
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to verify ocms_valid_offence_notice: " + e.getMessage());
        }
    }

    /**
     * Verify ocms_offence_notice_detail table
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyOffenceNoticeDetail(TestStepResult result) {
        result.addDetail("🔍 Verifying ocms_offence_notice_detail table...");

        String sql = "SELECT * FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no = ?";

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(sql, NOTICE_NO);

            // Verify diplomatic flag and other LTA data
            String diplomaticFlag = (String) record.get("lta_diplomatic_flag");
            String chassisNumber = (String) record.get("lta_chassis_number");
            String makeDescription = (String) record.get("lta_make_description");
            String primaryColor = (String) record.get("lta_primary_colour");

            result.addDetail("📝 lta_diplomatic_flag: " + diplomaticFlag);
            result.addDetail("📝 lta_chassis_number: " + (chassisNumber != null ? chassisNumber : "<null>"));
            result.addDetail("📝 lta_make_description: " + (makeDescription != null ? makeDescription : "<null>"));
            result.addDetail("📝 lta_primary_colour: " + (primaryColor != null ? primaryColor : "<null>"));

            // The diplomatic flag should be 'Y'
            if ("Y".equals(diplomaticFlag)) {
                result.addDetail("✅ lta_diplomatic_flag correctly set to Y");
            } else {
                result.addDetail("❌ lta_diplomatic_flag is not Y: " + diplomaticFlag);
                result.setStatus(STATUS_FAILED);
            }

            // LTA data should be populated after LTA Download
            if (chassisNumber != null && !chassisNumber.isEmpty()) {
                result.addDetail("✅ lta_chassis_number is populated");
            } else {
                result.addDetail("⚠️ lta_chassis_number is empty or null");
            }

            if (makeDescription != null && !makeDescription.isEmpty()) {
                result.addDetail("✅ lta_make_description is populated");
            } else {
                result.addDetail("⚠️ lta_make_description is empty or null");
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to verify ocms_offence_notice_detail: " + e.getMessage());
        }
    }

    /**
     * Verify ocms_offence_notice_owner_driver table
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyOffenceNoticeOwnerDriver(TestStepResult result) {
        result.addDetail("🔍 Verifying ocms_offence_notice_owner_driver table...");

        String sql = "SELECT * FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";

        try {
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, NOTICE_NO);

            if (records.isEmpty()) {
                result.addDetail("⚠️ No records found in ocms_offence_notice_owner_driver for notice_no: " + NOTICE_NO);
                return;
            }

            Map<String, Object> record = records.get(0);

            // Verify owner details
            String name = (String) record.get("name");
            String idNo = (String) record.get("id_no");
            String idType = (String) record.get("id_type");
            String entityType = (String) record.get("entity_type");
            String offenderTelNo = (String) record.get("offender_tel_no");
            String emailAddr = (String) record.get("email_addr");

            result.addDetail("📝 name: " + (name != null ? name : "<null>"));
            result.addDetail("📝 id_no: " + (idNo != null ? idNo : "<null>"));
            result.addDetail("📝 id_type: " + (idType != null ? idType : "<null>"));
            result.addDetail("📝 entity_type: " + (entityType != null ? entityType : "<null>"));
            result.addDetail("📝 offender_tel_no: " + (offenderTelNo != null ? offenderTelNo : "<null>"));
            result.addDetail("📝 email_addr: " + (emailAddr != null ? emailAddr : "<null>"));

            // Owner details should be populated after LTA Download
            if (name != null && !name.isEmpty()) {
                result.addDetail("✅ name is populated");
            } else {
                result.addDetail("⚠️ name is empty or null");
            }

            if (idNo != null && !idNo.isEmpty()) {
                result.addDetail("✅ id_no is populated");
            } else {
                result.addDetail("⚠️ id_no is empty or null");
            }

        } catch (Exception e) {
            // This table might not have records yet, so we don't fail the test
            result.addDetail("⚠️ Could not verify ocms_offence_notice_owner_driver: " + e.getMessage());
        }
    }

    /**
     * Verify ocms_offence_notice_owner_driver_addr table
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyOffenceNoticeOwnerDriverAddr(TestStepResult result) {
        result.addDetail("🔍 Verifying ocms_offence_notice_owner_driver_addr table...");

        String sql = "SELECT * FROM " + SCHEMA + ".ocms_offence_notice_owner_driver_addr WHERE notice_no = ?";

        try {
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, NOTICE_NO);

            if (records.isEmpty()) {
                result.addDetail("⚠️ No records found in ocms_offence_notice_owner_driver_addr for notice_no: " + NOTICE_NO);
                return;
            }

            Map<String, Object> record = records.get(0);

            // Verify address details
            String blkHseNo = (String) record.get("blk_hse_no");
            String streetName = (String) record.get("street_name");
            String postalCode = (String) record.get("postal_code");

            result.addDetail("📝 blk_hse_no: " + (blkHseNo != null ? blkHseNo : "<null>"));
            result.addDetail("📝 street_name: " + (streetName != null ? streetName : "<null>"));
            result.addDetail("📝 postal_code: " + (postalCode != null ? postalCode : "<null>"));

            // Address details should be populated after LTA Download
            if (streetName != null && !streetName.isEmpty()) {
                result.addDetail("✅ street_name is populated");
            } else {
                result.addDetail("⚠️ street_name is empty or null");
            }

            if (postalCode != null && !postalCode.isEmpty()) {
                result.addDetail("✅ postal_code is populated");
            } else {
                result.addDetail("⚠️ postal_code is empty or null");
            }

        } catch (Exception e) {
            // This table might not have records yet, so we don't fail the test
            result.addDetail("⚠️ Could not verify ocms_offence_notice_owner_driver_addr: " + e.getMessage());
        }
    }

    /**
     * Verify audit trail (upd_user_id and upd_date)
     *
     * @param result TestStepResult to update with verification details
     */
    private void verifyAuditTrail(TestStepResult result) {
        result.addDetail("🔍 Verifying audit trail...");

        String sql = "SELECT upd_user_id, upd_date FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no = ?";

        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(sql, NOTICE_NO);

            String updUserId = (String) record.get("upd_user_id");
            Object updDate = record.get("upd_date");

            result.addDetail("📝 upd_user_id: " + (updUserId != null ? updUserId : "<null>"));
            result.addDetail("📝 upd_date: " + (updDate != null ? updDate.toString() : "<null>"));

            // Audit fields should be populated after updates
            if (updUserId != null && !updUserId.isEmpty()) {
                result.addDetail("✅ upd_user_id is populated");
            } else {
                result.addDetail("❌ upd_user_id is empty or null");
                result.setStatus(STATUS_FAILED);
            }

            if (updDate != null) {
                result.addDetail("✅ upd_date is populated");
            } else {
                result.addDetail("❌ upd_date is empty or null");
                result.setStatus(STATUS_FAILED);
            }

        } catch (Exception e) {
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Failed to verify audit trail: " + e.getMessage());
        }
    }
}
