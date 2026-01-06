package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.common.TestStepStatus;
import com.ocmsintranet.cronservice.testing.agencies.mha.helpers.MhaTestDatabaseHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to run MHA NRIC download test flows.
 * This service orchestrates the test steps for the MHA NRIC download process.
 */
@Slf4j
@Service
public class MhaNricDownloadFlowService {

    private String testFilesPath = System.getProperty("user.dir") + "/logs/mha/download-nric";

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    private String mhaNricDownloadApiEndpoint = "/ocms/v1/mha/download/execute";
    private String sftpDownloadEndpoint = "/ocms/v1/mha/sftp/download";

    private final RestTemplate restTemplate;
    private final MhaTestDatabaseHelper dbHelper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor for MhaNricDownloadFlowService
     *
     * @param restTemplate RestTemplate for API calls
     * @param dbHelper MhaTestDatabaseHelper for database operations
     * @param jdbcTemplate JdbcTemplate for database operations
     */
    @Autowired
    public MhaNricDownloadFlowService(
            RestTemplate restTemplate,
            MhaTestDatabaseHelper dbHelper,
            JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.dbHelper = dbHelper;
        this.jdbcTemplate = jdbcTemplate;

        // Ensure test directory exists
        try {
            Path path = Paths.get(testFilesPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("Error creating test directory: {}", e.getMessage());
        }
    }

    /**
     * Run the MHA NRIC download test flow with the specified notice prefix.
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    public List<TestStepResult> runTest(String noticePrefix) {
        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        // Use fixed MHATEST prefix like shell script
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "MHATEST";
        }

        // Add test identification information
        TestStepResult testInfo = new TestStepResult(
            "MHA NRIC Download Test Flow",
            "INFO"
        );
        testInfo.addDetail("🔍 Test ID: MHA_NRIC_DOWNLOAD_FLOW");
        testInfo.addDetail("📝 Test Description: End-to-end test for MHA NRIC download process");
        testInfo.addDetail("🏷️ Notice Prefix: " + noticePrefix);
        testInfo.addDetail("⏱️ Start Time: " + LocalDateTime.now());
        results.add(testInfo);

        try {
            // Step 1: Verify test data exists
            TestStepResult step1 = verifyTestDataExists(noticePrefix);
            results.add(step1);
            if (!TestStepStatus.SUCCESS.name().equals(step1.getStatus())) {
                continueExecution = false;
            }

            // Step 2: Prepare database records for download testing
            if (continueExecution) {
                TestStepResult step2 = prepareDatabaseRecords(noticePrefix);
                results.add(step2);
                if (!TestStepStatus.SUCCESS.name().equals(step2.getStatus())) {
                    continueExecution = false;
                }
            } else {
                results.add(createSkippedStep("Step 2: Prepare database records"));
            }

            // Step 3: Create response files in SFTP
            if (continueExecution) {
                TestStepResult step3 = createResponseFileInSftp(noticePrefix);
                results.add(step3);
                if (!TestStepStatus.SUCCESS.name().equals(step3.getStatus())) {
                    continueExecution = false;
                }
            } else {
                results.add(createSkippedStep("Step 3: Create response files in SFTP"));
            }

            // Step 4: Trigger download API
            if (continueExecution) {
                TestStepResult step4 = triggerMhaNricDownloadApi();
                results.add(step4);
                if (!TestStepStatus.SUCCESS.name().equals(step4.getStatus())) {
                    continueExecution = false;
                }
            } else {
                results.add(createSkippedStep("Step 4: Trigger MHA NRIC download API"));
            }

            // Step 5: Verify DB updates
            if (continueExecution) {
                TestStepResult step5 = verifyDbUpdatesAfterDownload(noticePrefix);
                results.add(step5);
                if (!TestStepStatus.SUCCESS.name().equals(step5.getStatus())) {
                    continueExecution = false;
                }
            } else {
                results.add(createSkippedStep("Step 5: Verify DB updates"));
            }

            // Step 6: Verify job execution history
            if (continueExecution) {
                TestStepResult step6 = verifyJobExecutionHistory(noticePrefix);
                results.add(step6);
            } else {
                results.add(createSkippedStep("Step 6: Verify job execution history"));
            }

        } catch (Exception e) {
            TestStepResult errorStep = new TestStepResult(
                "Unexpected error",
                "FAILED"
            );
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            errorStep.addDetail("❌ Unhandled exception: " + e.getMessage());
            errorStep.addDetail("❌ Stack trace: " + sw.toString());
            results.add(errorStep);
        }

        return results;
    }

    /**
     * Step 2: Prepare database records for download testing
     * Updates next_processing_stage to 'MHA' to align with shell script
     */
    private TestStepResult prepareDatabaseRecords(String noticePrefix) {
        try {
            // Update next_processing_stage to 'MHA' for records matching the notice prefix
            // This aligns with the shell script preparation step
            String updateSql = "UPDATE " + "ocmsizmgr" + ".ocms_valid_offence_notice " +
                             "SET next_processing_stage = 'MHA' " +
                             "WHERE notice_no LIKE ? AND next_processing_stage IS NOT NULL";

            int updatedCount = jdbcTemplate.update(updateSql, noticePrefix + "%");

            TestStepResult result = new TestStepResult("Step 2: Prepare database records", "SUCCESS");
            if (updatedCount > 0) {
                result.addDetail(String.format("✅ Successfully updated %d records with next_processing_stage = 'MHA'", updatedCount));
            } else {
                result.setStatus("WARNING");
                result.addDetail("⚠️ No records found to update for download testing");
            }

            return result;

        } catch (Exception e) {
            TestStepResult result = new TestStepResult("Step 2: Prepare database records", "FAILED");
            result.addDetail("❌ Error preparing database records: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 1: Verify test data exists in the database.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult verifyTestDataExists(String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 1: Verifying test data exists",
            "RUNNING"
        );

        try {
            // Check if test records exist in database
            String verificationResult = dbHelper.verifyRecordsExist(noticePrefix);
            result.addDetail("📋 Verification result:");
            result.addDetail(verificationResult);

            if (verificationResult.contains("Records found")) {
                result.setStatus("SUCCESS");
                result.addDetail("✅ Test data exists in database");
            } else {
                result.setStatus("FAILED");
                result.addDetail("❌ Test data not found in database");
                result.addDetail("⚠️ Please run the upload flow first or create test data manually");
            }

            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            result.setStatus("FAILED");
            result.addDetail("❌ Error verifying test data: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Step 3: Create response files in SFTP.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult createResponseFileInSftp(String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 3: Creating response files in SFTP",
            "RUNNING"
        );

        try {
            // Generate file name with timestamp
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
            
            // Create 3 files like shell script
            String mainFileName = "NRO2URA_" + timestamp;
            String totalsFileName = "REPORT_" + timestamp + ".TOT";
            String exceptionsFileName = "REPORT_" + timestamp + ".EXP";

            // Create main response file content
            String mainFileContent = dbHelper.generateNricResponseFileContent(noticePrefix);
            result.addDetail("✅ Generated main NRIC response file content");

            // Create control totals report content
            String totalsContent = generateControlTotalsReport(timestamp);
            result.addDetail("✅ Generated control totals report content");

            // Create exceptions report content  
            String exceptionsContent = generateExceptionsReport(timestamp);
            result.addDetail("✅ Generated exceptions report content");

            // Save files locally
            String mainFilePath = testFilesPath + "/" + mainFileName;
            String totalsFilePath = testFilesPath + "/" + totalsFileName;
            String exceptionsFilePath = testFilesPath + "/" + exceptionsFileName;
            
            dbHelper.saveFileLocally(mainFilePath, mainFileContent);
            dbHelper.saveFileLocally(totalsFilePath, totalsContent);
            dbHelper.saveFileLocally(exceptionsFilePath, exceptionsContent);
            
            result.addDetail("✅ Saved all 3 response files locally");
            result.addDetail("   📄 Main: " + mainFilePath);
            result.addDetail("   📊 Totals: " + totalsFilePath);
            result.addDetail("   ⚠️ Exceptions: " + exceptionsFilePath);

            // Upload files to SFTP via API (simulate  encryption like shell script)
            String apiUrl = apiConfigHelper.buildApiUrl(sftpDownloadEndpoint);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Upload each file with  suffix to match shell script behavior
            boolean allUploadsSuccessful = true;
            StringBuilder uploadResults = new StringBuilder();

            String[] files = {mainFilePath, totalsFilePath, exceptionsFilePath};
            String[] remoteNames = {mainFileName, totalsFileName + "", exceptionsFileName + ""};
            
            for (int i = 0; i < files.length; i++) {
                try {
                    String requestBody = "{\"filePath\":\"" + files[i] + "\",\"remoteName\":\"" + remoteNames[i] + "\"}";
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        uploadResults.append("✅ ").append(remoteNames[i]).append(" uploaded successfully\n");
                    } else {
                        uploadResults.append("❌ ").append(remoteNames[i]).append(" upload failed\n");
                        allUploadsSuccessful = false;
                    }
                } catch (Exception e) {
                    uploadResults.append("❌ ").append(remoteNames[i]).append(" upload error: ").append(e.getMessage()).append("\n");
                    allUploadsSuccessful = false;
                }
            }

            result.addDetail("📥 SFTP Upload Results:");
            result.addDetail(uploadResults.toString());

            if (allUploadsSuccessful) {
                result.setStatus("SUCCESS");
                result.addDetail("✅ Successfully uploaded all 3 NRIC response files to SFTP");
            } else {
                result.setStatus("PARTIAL");
                result.addDetail("⚠️ Some files failed to upload to SFTP");
            }

            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            result.setStatus("FAILED");
            result.addDetail("❌ Error creating/uploading NRIC response files: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Generate control totals report content like shell script
     */
    private String generateControlTotalsReport(String timestamp) {
        String currentDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now());
        return String.format(
            "DATA SHARING SYSTEM                                                                                                 PAGE : 1\n" +
            "ZUR120OC                                                                                                            DATE : %s\n" +
            "                                 CONTROL TOTALS REPORT FOR MHA NRIC VERIFICATION\n" +
            "                                 ============================================\n" +
            "\n" +
            "                                 TIMESTAMP: %s\n" +
            "                                 TOTAL RECORDS PROCESSED: 3\n" +
            "                                 VALID RECORDS: 3\n" +
            "                                 INVALID RECORDS: 0\n" +
            "\n" +
            "                                 ****  E N D  O F  R E P O R T  ****\n",
            currentDate, timestamp
        );
    }

    /**
     * Generate exceptions report content like shell script
     */
    private String generateExceptionsReport(String timestamp) {
        String currentDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now());
        return String.format(
            "DATA SHARING SYSTEM                                                                                                 PAGE : 1\n" +
            "ZUR120OC                                                                                                            DATE : %s\n" +
            "                                 LIST OF ID NUMBERS WHICH ARE INVALID OR NOT FOUND IN MHA DATABASE\n" +
            "                                 =================================================================\n" +
            "\n" +
            "                                 SERIAL NO   ID NUMBER   EXCEPTION STATUS\n" +
            "                                 =========   =========   ================\n" +
            "                                 1          X1234567X   ID_NOT_FOUND\n" +
            "                                 2          Y5678901Y   INVALID_ID_FORMAT\n" +
            "\n" +
            "                                 ****  E N D  O F  R E P O R T  ****\n",
            currentDate
        );
    }

    /**
     * Step 3: Trigger MHA NRIC download API.
     *
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult triggerMhaNricDownloadApi() {
        TestStepResult result = new TestStepResult(
            "Step 4: Triggering MHA NRIC download API",
            "RUNNING"
        );

        try {
            // Prepare API call
            String apiUrl = apiConfigHelper.buildApiUrl(mhaNricDownloadApiEndpoint);
            result.addDetail("📡 Calling API: " + apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, String.class);

            // Process response
            String responseBody = response.getBody();
            result.setJsonData(responseBody);
            result.addDetail("📥 API Response: " + responseBody);

            if (response.getStatusCode().is2xxSuccessful()) {
                result.setStatus("SUCCESS");
                result.addDetail("✅ Successfully triggered MHA NRIC download API");

                // Extract success status and message from response if available
                if (responseBody != null) {
                    if (responseBody.contains("success") && responseBody.contains("true")) {
                        result.addDetail("✅ API reported successful processing");
                    } else if (responseBody.contains("message")) {
                        result.addDetail("📢 API message: " + extractMessageFromResponse(responseBody));
                    }
                }
            } else {
                result.setStatus("FAILED");
                result.addDetail("❌ Failed to trigger MHA NRIC download API");
            }

            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            result.setStatus("FAILED");
            result.addDetail("❌ Error triggering MHA NRIC download API: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Step 4: Verify database updates after processing.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult verifyDbUpdatesAfterDownload(String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 5: Verifying database updates",
            "RUNNING"
        );

        try {
            // Query database for updated records
            String verificationResult = dbHelper.verifyDownloadDatabaseUpdates(noticePrefix);
            result.addDetail("📋 Database verification result:");
            result.addDetail(verificationResult);

            // Check if verification was successful
            if (verificationResult.contains("successfully verified")) {
                result.setStatus("SUCCESS");
                result.addDetail("✅ Database updates verified successfully");
            } else {
                result.setStatus("FAILED");
                result.addDetail("❌ Failed to verify database updates");
            }

            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            result.setStatus("FAILED");
            result.addDetail("❌ Error verifying database updates: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Step 6: Verify job execution history.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult verifyJobExecutionHistory(String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 6: Verifying job execution history",
            "RUNNING"
        );

        try {
            // Query job execution history
            String jobHistory = dbHelper.queryJobExecutionHistory("MHA_NRIC_DOWNLOAD");
            result.addDetail("📋 Job execution history:");
            result.addDetail(jobHistory);

            if (jobHistory.contains("SUCCESS")) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("✅ Job execution history verified successfully");
            } else {
                result.setStatus(TestStepStatus.WARNING.name());
                result.addDetail("⚠️ Job execution history does not show successful completion");
            }

            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            result.setStatus(TestStepStatus.FAILED.name());
            result.addDetail("❌ Error verifying job execution history: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Create a skipped step result.
     *
     * @param title The title of the step
     * @return TestStepResult with SKIPPED status
     */
    private TestStepResult createSkippedStep(String title) {
        TestStepResult skippedStep = new TestStepResult(title, TestStepStatus.SKIPPED.name());
        skippedStep.addDetail("⚠️ Skipped due to previous step failure");
        return skippedStep;
    }

    /**
     * Extract message from API response.
     *
     * @param responseBody The API response body
     * @return The extracted message
     */
    private String extractMessageFromResponse(String responseBody) {
        // Simple extraction logic - can be enhanced for XML/JSON parsing if needed
        if (responseBody == null) {
            return "No response body";
        }

        if (responseBody.contains("\"message\":")) {
            int start = responseBody.indexOf("\"message\":") + 11;
            int end = responseBody.indexOf("\"", start);
            if (end > start) {
                return responseBody.substring(start, end);
            }
        }

        if (responseBody.contains("<message>")) {
            int start = responseBody.indexOf("<message>") + 9;
            int end = responseBody.indexOf("</message>", start);
            if (end > start) {
                return responseBody.substring(start, end);
            }
        }

        return responseBody;
    }
}
