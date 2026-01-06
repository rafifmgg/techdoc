package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;
import com.ocmsintranet.cronservice.testing.agencies.mha.helpers.MhaTestDatabaseHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service to run MHA NRIC upload test flows.
 * This service orchestrates the test steps for the MHA NRIC upload process.
 */
@Service
public class MhaTestService {

    private String testFilesPath = System.getProperty("user.dir") + "/logs/mha/upload-nric";
    private boolean createPhysicalFiles = true;

    @Value("${ocms.APIM.baseurl}")
    private String apimBaseUrl;

    private String cronBaseUrl;

    private boolean cleanup;

    private String mhaNricUploadApiEndpoint = "/ocms/v1/mha/upload/nric/manual";

    // JdbcTemplate is used by the dbHelper, kept here for potential future direct usage
    private final RestTemplate restTemplate;
    private final MhaTestDatabaseHelper dbHelper;

    // Test configuration
    public static final String TEST_ID = "TEST_MHA_NRIC_UPLOAD";
    public static final String TEST_NAME = "MHA NRIC Upload Processing → No NRIC Data Found";

    /**
     * Constructor for MhaTestService
     *
     * @param restTemplate RestTemplate for API calls
     * @param dbHelper MhaTestDatabaseHelper for database operations
     */
    @Autowired
    public MhaTestService(
            RestTemplate restTemplate,
            MhaTestDatabaseHelper dbHelper) {
        this.restTemplate = restTemplate;
        this.dbHelper = dbHelper;
    }

    /**
     * Cleanup resources when the service is destroyed
     */
    @PreDestroy
    public void cleanup() {
        if (cleanup) {
            dbHelper.cleanupTestFiles();
        }
    }

    /**
     * Clean up all test files in the test directory.
     * This ensures no leftover files remain after tests are complete.
     * Delegates to the database helper class for implementation.
     */
    public void cleanupTestFiles() {
        dbHelper.cleanupTestFiles();
    }

    /**
     * Ensure the test files directory exists and initialize file-related variables in the helper class.
     * This method runs after dependency injection is complete.
     */
    @PostConstruct
    public void ensureTestDirectoryExists() {
        try {
            // Determine the base URL for the cron service
            if (apimBaseUrl != null && !apimBaseUrl.isEmpty()) {
                cronBaseUrl = apimBaseUrl;
                // If we're using a non-local environment, don't clean up test data
                cleanup = apimBaseUrl.contains("localhost") || apimBaseUrl.contains("127.0.0.1");
            } else {
                cronBaseUrl = "http://localhost:8080";
                cleanup = true;
            }

            // Create test files directory if it doesn't exist
            Path path = Paths.get(testFilesPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // Initialize file paths in helper
            dbHelper.setTestFilesPath(testFilesPath);
            dbHelper.setCreatePhysicalFiles(createPhysicalFiles);
            
            // Set up file paths for test files
            String datFileName = "URA2NRO_" + System.currentTimeMillis();
            String encryptedFileName = datFileName + ".enc";
            
            dbHelper.setDatFile(testFilesPath + "/" + datFileName);
            dbHelper.setEncryptedFile(testFilesPath + "/" + encryptedFileName);
            
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Error ensuring test directory exists: " + sw.toString());
        }
    }

    /**
     * Step 1: Create test records in the database.
     * This method creates all required records for the test in the database.
     * Uses MhaTestDatabaseHelper to handle database operations.
     * 
     * @param scenarioName The name of the scenario to create test records for
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    public TestStepResult createTestRecords(String scenarioName, String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 1: Creating test records in database for '" + scenarioName + "' scenario...",
            "RUNNING"
        );
        
        try {
            // Clean up any existing test records first
            dbHelper.cleanupTestRecords();
            result.addDetail("🗑️ Cleaned up existing test records");
            
            // Create test records based on scenario
            switch (scenarioName) {
                case "No NRIC Data Found":
                    dbHelper.createNoNricDataFoundTestRecords();
                    result.addDetail("✅ Created test records for 'No NRIC Data Found' scenario");
                    break;
                case "Successful Upload with SLIFT Encryption":
                    dbHelper.createSuccessfulUploadTestRecords(noticePrefix);
                    result.addDetail("✅ Created test records for 'Successful Upload with SLIFT Encryption' scenario");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown scenario: " + scenarioName);
            }
            
            // Verify records were created successfully
            dbHelper.verifyRecordsCreated(result, noticePrefix);
            
            result.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            result.setStatus("FAILED");
            result.addDetail("❌ Error creating test records: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Step 2: Verify database data before triggering the cron job.
     * This method queries and verifies the initial state of data across all tables
     * to ensure proper test conditions before the cron job is triggered.
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    public TestStepResult displayJoinedTableData(String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 2: Verifying database data before triggering job...",
            "RUNNING"
        );

        try {
            // Query and display joined data for the test notice
            String joinedData = dbHelper.displayJoinedTableData(noticePrefix);
            
            result.addDetail("📋 Joined table data for test notice:");
            result.addDetail(joinedData);
            
            // Verify the data is as expected for the "No NRIC Data Found" scenario
            if (joinedData.contains("next_processing_stage: INVALID")) {
                result.addDetail("✅ Verified next_processing_stage is set to INVALID");
                result.setStatus("SUCCESS");
            } else {
                result.addDetail("❌ next_processing_stage is not set to INVALID as required");
                result.setStatus("FAILED");
            }
            
            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            result.setStatus("FAILED");
            result.addDetail("❌ Error verifying database data: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Step 3: Trigger MHA NRIC upload workflow via API.
     * This method calls the MHA NRIC upload API endpoint to process the test data.
     * 
     * @param scenarioName The name of the scenario being tested
     * @param noticePrefix The prefix for test notice numbers
     * @return TestStepResult containing the results of this step
     */
    public TestStepResult triggerMhaNricUpload(String scenarioName, String noticePrefix) {
        TestStepResult result = new TestStepResult(
            "Step 3: Triggering MHA NRIC upload workflow via API...",
            "RUNNING"
        );

        try {
            // Prepare API call
            String apiUrl = cronBaseUrl + mhaNricUploadApiEndpoint;
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
            
            // Extract success status and message from response
            // Response could be XML or JSON format
            if (responseBody != null) {
                // Check if it's XML format
                if (responseBody.contains("<") && responseBody.contains(">")) {
                    String success = dbHelper.extractXmlValue(responseBody, "success");
                    String message = dbHelper.extractXmlValue(responseBody, "message");
                    String jobName = dbHelper.extractXmlValue(responseBody, "jobName");
                    
                    result.addDetail("📄 Job Name: " + jobName);
                    result.addDetail("📢 Message: " + message);
                    
                    if ("No NRIC Data Found".equals(scenarioName)) {
                        if ("true".equals(success) && message != null && 
                            message.contains("No NRIC data found for verification")) {
                            result.addDetail("✅ API call successful - No NRIC data found as expected");
                            result.setStatus("SUCCESS");
                        } else if ("true".equals(success)) {
                            result.addDetail("✅ API call successful but with unexpected message");
                            result.setStatus("WARNING");
                        } else {
                            result.addDetail("❌ API call failed");
                            result.setStatus("FAILED");
                        }
                    } else if ("Successful Upload with SLIFT Encryption".equals(scenarioName)) {
                        if ("true".equals(success) && message != null && 
                            message.contains("Successfully processed")) {
                            result.addDetail("✅ API call successful - NRIC data processed successfully");
                            result.setStatus("SUCCESS");
                        } else if ("true".equals(success)) {
                            result.addDetail("✅ API call successful but with unexpected message");
                            result.setStatus("WARNING");
                        } else {
                            result.addDetail("❌ API call failed");
                            result.setStatus("FAILED");
                        }
                    } else {
                        // Default case for other scenarios
                        if ("true".equals(success)) {
                            result.addDetail("✅ API call successful");
                            result.setStatus("SUCCESS");
                        } else {
                            result.addDetail("❌ API call failed");
                            result.setStatus("FAILED");
                        }
                    }
                } 
                // Check if it contains the expected message directly
                else if (responseBody.contains("No NRIC data found for verification")) {
                    result.addDetail("✅ API call successful - No NRIC data found as expected");
                    result.setStatus("SUCCESS");
                } else {
                    result.addDetail("⚠️ API call completed but with unexpected response format");
                    result.setStatus("WARNING");
                }
            } else {
                result.addDetail("⚠️ API call returned empty response");
                result.setStatus("WARNING");
            }
            
            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            result.setStatus("FAILED");
            result.addDetail("❌ Error triggering MHA NRIC upload: " + e.getMessage());
            result.addDetail("❌ Stack trace: " + sw.toString());
            return result;
        }
    }

    /**
     * Run the MHA NRIC upload test for "No NRIC Data Found" scenario.
     * This method orchestrates all steps of the test and collects results.
     * 
     * @return List of test step results
     */
    public List<TestStepResult> runNoNricDataFoundTest() {
        return runTestScenario("No NRIC Data Found", "MHATEST001");
    }
    
    /**
     * Run the MHA NRIC upload test for "Successful Upload with SLIFT Encryption" scenario.
     * This method orchestrates all steps of the test and collects results.
     * 
     * @return List of test step results
     */
    public List<TestStepResult> runSuccessfulUploadTest() {
        return runTestScenario("Successful Upload with SLIFT Encryption", "MHATEST005");
    }
    
    /**
     * Generic method to run a test scenario with the specified name and notice number prefix.
     * 
     * @param scenarioName The name of the scenario to run
     * @param noticePrefix The prefix for test notice numbers
     * @return List of test step results
     */
    private List<TestStepResult> runTestScenario(String scenarioName, String noticePrefix) {
        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        // Add test identification information
        TestStepResult testInfo = new TestStepResult(
            "Test Information",
            "INFO"
        );
        testInfo.addDetail("🔍 Test ID: " + TEST_ID);
        testInfo.addDetail("📝 Test Name: " + TEST_NAME);
        testInfo.addDetail("⏱️ Start Time: " + java.time.LocalDateTime.now());
        results.add(testInfo);

        try {
            // Step 1: Create test records
            TestStepResult step1Result = createTestRecords(scenarioName, noticePrefix);
            results.add(step1Result);
            if (!"SUCCESS".equals(step1Result.getStatus())) {
                continueExecution = false;
            }

            // Step 2: Display joined table data
            if (continueExecution) {
                TestStepResult step2 = displayJoinedTableData(noticePrefix);
                results.add(step2);
                if ("FAILED".equals(step2.getStatus())) {
                    continueExecution = false;
                    step2.addDetail("⚠️ Critical step failed, stopping test flow");
                }
            } else {
                TestStepResult skippedStep = new TestStepResult(
                    "Step 2: Verifying database data before triggering job...",
                    "SKIPPED"
                );
                skippedStep.addDetail("⚠️ Skipped due to previous step failure");
                results.add(skippedStep);
            }

            // Step 3: Trigger MHA NRIC upload
            if (continueExecution) {
                TestStepResult step3 = triggerMhaNricUpload(scenarioName, noticePrefix);
                results.add(step3);
            } else {
                TestStepResult skippedStep = new TestStepResult(
                    "Step 3: Triggering MHA NRIC upload workflow via API...",
                    "SKIPPED"
                );
                skippedStep.addDetail("⚠️ Skipped due to previous step failure");
                results.add(skippedStep);
            }

            // Add summary step
            boolean allSuccess = results.stream()
                .filter(step -> !"SKIPPED".equals(step.getStatus()))
                .allMatch(step -> "SUCCESS".equals(step.getStatus()));

            TestStepResult summary = new TestStepResult(
                "Test Flow Summary",
                allSuccess ? "SUCCESS" : "PARTIAL"
            );

            long successCount = results.stream()
                .filter(step -> "SUCCESS".equals(step.getStatus()))
                .count();

            long failedCount = results.stream()
                .filter(step -> "FAILED".equals(step.getStatus()))
                .count();

            long skippedCount = results.stream()
                .filter(step -> "SKIPPED".equals(step.getStatus()))
                .count();

            summary.addDetail("📊 Test Flow Statistics:");
            summary.addDetail("✅ Successful steps: " + successCount);
            summary.addDetail("❌ Failed steps: " + failedCount);
            summary.addDetail("⚠️ Skipped steps: " + skippedCount);
            summary.addDetail("Total steps: " + (successCount + failedCount + skippedCount));

            if (allSuccess) {
                summary.addDetail("🎉 All executed steps completed successfully!");
            } else {
                summary.addDetail("⚠️ Some steps failed or were skipped. Check individual step results for details.");
            }

            results.add(summary);
        } catch (Exception e) {
            TestStepResult errorStep = new TestStepResult(
                "Unexpected error",
                "FAILED"
            );
            errorStep.addDetail("❌ Unhandled exception: " + e.getMessage());
            errorStep.addDetail("❌ Stack trace: " + Arrays.toString(e.getStackTrace()));
            results.add(errorStep);
        }

        return results;
    }

    /**
     * Get the test files path.
     * @return The test files path
     */
    public String getTestFilesPath() {
        return testFilesPath;
    }

    /**
     * Get the DAT file path.
     * @return The DAT file path
     */
    public String getDatFile() {
        return dbHelper.getDatFile();
    }

    /**
     * Get the encrypted file path.
     * @return The encrypted file path
     */
    public String getEncryptedFile() {
        return dbHelper.getEncryptedFile();
    }

    /**
     * Check if physical files should be created.
     * @return True if physical files should be created, false otherwise
     */
    public boolean isCreatePhysicalFiles() {
        return createPhysicalFiles;
    }
    
    /**
     * Get the MHA NRIC upload API endpoint.
     * @return The MHA NRIC upload API endpoint
     */
    public String getMhaNricUploadApiEndpoint() {
        return mhaNricUploadApiEndpoint;
    }
    
    /**
     * Get the cron base URL.
     * @return The cron base URL
     */
    public String getCronBaseUrl() {
        return cronBaseUrl;
    }
}
