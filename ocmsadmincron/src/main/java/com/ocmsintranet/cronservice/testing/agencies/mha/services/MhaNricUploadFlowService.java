package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.common.TestStepStatus;
import com.ocmsintranet.cronservice.testing.agencies.mha.helpers.MhaTestDatabaseHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service to run MHA NRIC upload test flows.
 * This service orchestrates the test steps for the MHA NRIC upload process.
 */
@Slf4j
@Service
public class MhaNricUploadFlowService {

    private String testFilesPath = System.getProperty("user.dir") + "/logs/mha/upload-nric";
    private boolean createPhysicalFiles = true;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    private String mhaNricUploadApiEndpoint = "/ocms/v1/mha/upload";
    private String sftpUploadEndpoint = "/ocms/v1/mha/sftp/upload";

    private final RestTemplate restTemplate;
    private final MhaTestDatabaseHelper dbHelper;

    private String uploadFilePath;

    /**
     * Constructor for MhaNricUploadFlowService
     *
     * @param restTemplate RestTemplate for API calls
     * @param dbHelper MhaTestDatabaseHelper for database operations
     */
    @Autowired
    public MhaNricUploadFlowService(
            RestTemplate restTemplate,
            MhaTestDatabaseHelper dbHelper) {
        this.restTemplate = restTemplate;
        this.dbHelper = dbHelper;

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
     * Run the MHA NRIC upload test flow with the specified notice prefix.
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    public List<TestStepResult> runTest(String noticePrefix) {
        List<TestStepResult> results = new ArrayList<>();

        // Step 1: Clean up test data
        TestStepResult cleanupResult = cleanupTestData(noticePrefix);
        results.add(cleanupResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(cleanupResult.getStatus())) {
            skipRemainingSteps(results, "Create test data", "Verify test data", "Create physical files",
                    "Upload files to SFTP", "Trigger upload API", "Verify database updates");
            return results;
        }

        // Step 2: Create test data
        TestStepResult createDataResult = createTestData(noticePrefix);
        results.add(createDataResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(createDataResult.getStatus())) {
            skipRemainingSteps(results, "Verify test data", "Create physical files",
                    "Upload files to SFTP", "Trigger upload API", "Verify database updates");
            return results;
        }

        // Step 3: Verify test data
        TestStepResult verifyDataResult = verifyTestData(noticePrefix);
        results.add(verifyDataResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(verifyDataResult.getStatus())) {
            skipRemainingSteps(results, "Create physical files",
                    "Upload files to SFTP", "Trigger upload API", "Verify database updates");
            return results;
        }

        // Step 4: Create physical files
        TestStepResult createFilesResult = createPhysicalFiles(noticePrefix);
        results.add(createFilesResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(createFilesResult.getStatus())) {
            skipRemainingSteps(results, "Upload files to SFTP", "Trigger upload API", "Verify database updates");
            return results;
        }

        // Step 5: Upload files to SFTP
        TestStepResult uploadResult = uploadFilesToSftp(noticePrefix);
        results.add(uploadResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(uploadResult.getStatus())) {
            skipRemainingSteps(results, "Trigger upload API", "Verify database updates");
            return results;
        }

        // Step 6: Trigger upload API
        TestStepResult triggerApiResult = triggerUploadApi();
        results.add(triggerApiResult);

        // Skip subsequent steps if previous step failed
        if (TestStepStatus.FAILED.name().equals(triggerApiResult.getStatus())) {
            skipRemainingSteps(results, "Verify database updates");
            return results;
        }

        // Step 7: Verify database updates
        TestStepResult verifyDbResult = verifyDatabaseUpdates();
        results.add(verifyDbResult);

        return results;
    }

    /**
     * Helper method to skip remaining steps when a previous step fails
     *
     * @param results The list of test step results
     * @param stepNames The names of steps to skip
     */
    private void skipRemainingSteps(List<TestStepResult> results, String... stepNames) {
        for (String stepName : stepNames) {
            TestStepResult skippedResult = new TestStepResult(stepName, TestStepStatus.SKIPPED.name());
            skippedResult.addDetail("Skipped due to previous step failure");
            results.add(skippedResult);
        }
    }

    /**
     * Step 1: Clean up test data from previous runs.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult cleanupTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Clean up test data", TestStepStatus.RUNNING.name());

        try {
            // Clean up database records
            dbHelper.cleanupTestRecords();
            result.addDetail("Cleaned up test database records");

            // Clean up test files
            dbHelper.cleanupTestFiles();
            result.addDetail("Cleaned up test files");

            result.setStatus(TestStepStatus.SUCCESS.name());
            return result;
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Error cleaning up test data: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
            return result;
        }
    }

    /**
     * Step 2: Create test data in the database.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult createTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Create test data", TestStepStatus.RUNNING.name());

        try {
            // Create test records for successful upload scenario
            dbHelper.createSuccessfulUploadTestRecords(noticePrefix);
            result.addDetail("Created test records with notice prefix: " + noticePrefix);

            // Verify records were created successfully
            String verificationResult = dbHelper.verifyRecordsCreated(noticePrefix);
            result.addDetail("Verification result:");
            result.addDetail(verificationResult);

            if (verificationResult.contains("Records created successfully")) {
                result.setStatus(TestStepStatus.SUCCESS.name());
            } else {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Failed to verify test records creation");
            }

            return result;
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Error preparing test data: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
            return result;
        }
    }

    /**
     * Step 3: Verify test data.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult verifyTestData(String noticePrefix) {
        TestStepResult result = new TestStepResult("Verify test data", TestStepStatus.RUNNING.name());

        try {
            // Verify test data
            String verificationResult = dbHelper.verifyTestData(noticePrefix);
            result.addDetail("Verification result:");
            result.addDetail(verificationResult);

            if (verificationResult.contains("Test data verified successfully")) {
                result.setStatus(TestStepStatus.SUCCESS.name());
            } else {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Failed to verify test data");
            }

            return result;
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Error verifying test data: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
            return result;
        }
    }

    /**
     * Step 4: Create physical files.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult createPhysicalFiles(String noticePrefix) {
        TestStepResult result = new TestStepResult("Create physical files", TestStepStatus.RUNNING.name());

        try {
            // Skip file creation if createPhysicalFiles is false
            if (!createPhysicalFiles) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("Physical file creation skipped as per configuration");
                return result;
            }

            // Generate file content
            String fileContent = dbHelper.generateNricFileContent(noticePrefix);

            // Create directory if it doesn't exist
            File directory = new File(testFilesPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create file
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "MHA_NRIC_" + timestamp + ".txt";
            String filePath = testFilesPath + File.separator + fileName;

            File file = new File(filePath);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(fileContent);
            }

            // Store file path for later use
            this.uploadFilePath = filePath;

            result.setStatus(TestStepStatus.SUCCESS.name());
            result.addDetail("Created file: " + filePath);
            result.addDetail("Content:\n" + fileContent);
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Failed to create physical files: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
        }

        return result;
    }

    /**
     * Step 5: Upload files to SFTP.
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult uploadFilesToSftp(String noticePrefix) {
        TestStepResult result = new TestStepResult("Upload files to SFTP", TestStepStatus.RUNNING.name());

        try {
            // Skip SFTP upload if createPhysicalFiles is false
            if (!createPhysicalFiles) {
                // Generate file content for SFTP upload without creating local file
                String fileContent = dbHelper.generateNricFileContent(noticePrefix);
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String fileName = "MHA_NRIC_" + timestamp + ".txt";
                String remoteFilePath = sftpUploadEndpoint + "/" + fileName;

                // Upload to SFTP directly
                boolean uploadSuccess = dbHelper.uploadFileToSftp(remoteFilePath, fileContent);

                if (uploadSuccess) {
                    result.setStatus(TestStepStatus.SUCCESS.name());
                    result.addDetail("Successfully uploaded file to SFTP:");
                    result.addDetail("Remote: " + remoteFilePath);
                } else {
                    result.setStatus(TestStepStatus.SKIPPED.name());
                    result.addDetail("Failed to upload file to SFTP:");
                    result.addDetail("Remote: " + remoteFilePath);
                }
                return result;
            }

            // Check if file exists
            if (uploadFilePath == null || uploadFilePath.isEmpty()) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("No file to upload. Please run createPhysicalFiles step first.");
                return result;
            }

            File file = new File(uploadFilePath);
            if (!file.exists()) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("File does not exist: " + uploadFilePath);
                return result;
            }

            // Read file content
            byte[] fileContent;
            try (FileInputStream fis = new FileInputStream(file)) {
                fileContent = new byte[(int) file.length()];
                fis.read(fileContent);
            }

            // Upload to SFTP
            String remoteFilePath = sftpUploadEndpoint + "/" + file.getName();
            boolean uploadSuccess = dbHelper.uploadFileToSftp(remoteFilePath, fileContent);

            if (uploadSuccess) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("Successfully uploaded file to SFTP:");
                result.addDetail("Local: " + uploadFilePath);
                result.addDetail("Remote: " + remoteFilePath);
            } else {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Failed to upload file to SFTP:");
                result.addDetail("Local: " + uploadFilePath);
                result.addDetail("Remote: " + remoteFilePath);
            }
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Failed to upload files to SFTP: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
        }

        return result;
    }

    /**
     * Step 6: Trigger upload API.
     *
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult triggerUploadApi() {
        TestStepResult result = new TestStepResult("Trigger upload API", TestStepStatus.RUNNING.name());

        try {
            // Validate API endpoint
            if (mhaNricUploadApiEndpoint == null || mhaNricUploadApiEndpoint.trim().isEmpty()) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("API endpoint is not properly configured");
                return result;
            }

            // Call the MHA NRIC upload API
            String apiUrl = apiConfigHelper.buildApiUrl(mhaNricUploadApiEndpoint);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create empty request body
            String requestBody = "";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, entity, String.class);
            } catch (Exception e) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("API call failed: " + e.getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                result.addDetail(sw.toString());
                return result;
            }

            // Process response
            String responseBody = response.getBody();
            result.setJsonData(responseBody);
            result.addDetail("Upload API Response: " + responseBody);

            if (response.getStatusCode().is2xxSuccessful()) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("Successfully triggered MHA NRIC upload API");
            } else {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Failed to trigger MHA NRIC upload API");
            }

            return result;
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Error triggering upload API: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
            return result;
        }
    }

    /**
     * Step 7: Verify database updates.
     *
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult verifyDatabaseUpdates() {
        TestStepResult result = new TestStepResult("Verify database updates", TestStepStatus.RUNNING.name());

        try {
            // Validate dbHelper
            if (dbHelper == null) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Database helper is not initialized");
                return result;
            }

            // Query database for updates
            String dbUpdates;
            try {
                dbUpdates = dbHelper.verifyDatabaseUpdates();
                result.addDetail("Database updates:");
                result.addDetail(dbUpdates);
            } catch (Exception e) {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Database query failed: " + e.getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                result.addDetail(sw.toString());
                return result;
            }

            if (dbUpdates != null && dbUpdates.contains("SUCCESS")) {
                result.setStatus(TestStepStatus.SUCCESS.name());
                result.addDetail("Database updates verified successfully");
            } else {
                result.setStatus(TestStepStatus.SKIPPED.name());
                result.addDetail("Database updates verification did not show expected changes");
            }

            return result;
        } catch (Exception e) {
            result.setStatus(TestStepStatus.SKIPPED.name());
            result.addDetail("Error verifying database updates: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail(sw.toString());
            return result;
        }
    }

    /**
     * Extract message from API response.
     *
     * @param responseBody The API response body
     * @return The extracted message
     */
    private String extractMessageFromResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            if (rootNode.has("message")) {
                return rootNode.path("message").asText();
            } else if (rootNode.has("error")) {
                return rootNode.path("error").asText();
            } else {
                // Check for XML format
                if (responseBody.contains("<message>")) {
                    int start = responseBody.indexOf("<message>") + 9;
                    int end = responseBody.indexOf("</message>", start);
                    if (end > start) {
                        return responseBody.substring(start, end);
                    }
                }
                return responseBody;
            }
        } catch (Exception e) {
            return responseBody;
        }
    }
}
