package com.ocmsintranet.cronservice.testing.agencies.toppan.services;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.models.ToppanStage;
import com.ocmsintranet.cronservice.testing.agencies.toppan.helpers.ToppanTestDataHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for testing Toppan upload workflow
 */
@Service
@Slf4j
public class ToppanUploadTestService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    @Autowired
    private ToppanTestDataHelper toppanTestDataHelper;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Run the Toppan upload test flow for the specified stage
     */
    public List<TestStepResult> runTest(String stage) {
        List<TestStepResult> results = new ArrayList<>();

        // Step 1: Validate stage
        TestStepResult step1 = validateStage(stage);
        results.add(step1);
        if (STATUS_FAILED.equals(step1.getStatus())) {
            results.add(createSummary(results));
            return results;
        }

        // Step 2: Insert test data
        TestStepResult step2 = insertTestData(stage);
        results.add(step2);
        if (STATUS_FAILED.equals(step2.getStatus())) {
            results.add(createSummary(results));
            return results;
        }

        // Step 3: Call Toppan upload API
        TestStepResult step3 = callToppanUploadApi(stage);
        results.add(step3);
        if (STATUS_FAILED.equals(step3.getStatus())) {
            results.add(createSummary(results));
            return results;
        }

        // Step 4: Verify results
        TestStepResult step4 = verifyResults(stage);
        results.add(step4);

        // Create summary
        results.add(createSummary(results));

        return results;
    }

    /**
     * Step 1: Validate stage parameter
     */
    private TestStepResult validateStage(String stage) {
        log.info("Step 1: Validating stage parameter: {}", stage);

        try {
            Optional<ToppanStage> stageOpt = ToppanStage.fromString(stage);

            if (!stageOpt.isPresent()) {
                String errorMsg = "Invalid stage: " + stage + ". Must be one of: RD1, RD2, RR3, DN1, DN2, DR3";
                log.error(errorMsg);
                TestStepResult result = new TestStepResult("Validate Stage", STATUS_FAILED);
                result.addDetail(errorMsg);
                return result;
            }

            TestStepResult result = new TestStepResult("Validate Stage", STATUS_SUCCESS);
            result.addDetail("✅ Stage " + stage + " is valid");
            return result;

        } catch (Exception e) {
            log.error("Error validating stage: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Validate Stage", STATUS_FAILED);
            result.addDetail("❌ Error validating stage: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Insert test data
     */
    private TestStepResult insertTestData(String stage) {
        log.info("Step 2: Inserting test data for stage: {}", stage);

        try {
            // Clean up existing test data
            toppanTestDataHelper.cleanupTestData();

            // Insert test records into ocms_valid_offence_notice
            toppanTestDataHelper.insertValidOffenceNotices(stage);

            // Insert test records into ocms_offence_notice_owner_driver
            toppanTestDataHelper.insertOwnerDriverRecords();

            // Insert exclusion records
            toppanTestDataHelper.insertExclusionRecords();

            // Log inserted records for verification
            List<Map<String, Object>> insertedNotices = toppanTestDataHelper.queryTestNotices();
            List<Map<String, Object>> insertedOwnerDrivers = toppanTestDataHelper.queryTestOwnerDrivers();

            String resultMsg = String.format("✅ Successfully inserted test data: %d notices, %d owner/driver records",
                    insertedNotices.size(), insertedOwnerDrivers.size());

            TestStepResult result = new TestStepResult("Insert Test Data", STATUS_SUCCESS);
            result.addDetail(resultMsg);
            return result;

        } catch (Exception e) {
            log.error("Error inserting test data: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Insert Test Data", STATUS_FAILED);
            result.addDetail("❌ Error inserting test data: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Call Toppan upload API
     */
    private TestStepResult callToppanUploadApi(String stage) {
        log.info("Step 3: Calling Toppan upload API for stage: {}", stage);

        try {
            String endpoint = apiBaseUrl + "/ocms/v1/toppan/upload?stage=" + stage;
            log.info("Calling endpoint: {}", endpoint);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, "", String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String resultMsg = String.format("✅ API call successful. Status: %s, Response: %s",
                        response.getStatusCode(), response.getBody());

                // Wait for processing to complete
                log.info("Waiting for processing to complete...");
                Thread.sleep(15000); // 15 seconds

                TestStepResult result = new TestStepResult("Call Toppan Upload API", STATUS_SUCCESS);
                result.addDetail(resultMsg);
                return result;
            } else {
                String errorMsg = String.format("❌ API call failed. Status: %s, Response: %s",
                        response.getStatusCode(), response.getBody());
                log.error(errorMsg);
                TestStepResult result = new TestStepResult("Call Toppan Upload API", STATUS_FAILED);
                result.addDetail(errorMsg);
                return result;
            }

        } catch (Exception e) {
            log.error("Error calling Toppan upload API: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Call Toppan Upload API", STATUS_FAILED);
            result.addDetail("❌ Error calling Toppan upload API: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 4: Verify results
     */
    private TestStepResult verifyResults(String stage) {
        log.info("Step 4: Verifying results for stage: {}", stage);

        try {
            // Query processed notices
            List<Map<String, Object>> processedNotices = toppanTestDataHelper.queryProcessedNotices(stage);

            // Check which notices were processed correctly
            List<String> expectedProcessed = List.of("TPTEST001", "TPTEST002", "TPTEST005");
            List<String> actualProcessed = new ArrayList<>();

            for (Map<String, Object> notice : processedNotices) {
                String noticeNo = (String) notice.get("notice_no");
                String lastProcessingStage = (String) notice.get("last_processing_stage");
                String nextProcessingStage = (String) notice.get("next_processing_stage");

                if (stage.equals(lastProcessingStage) && !stage.equals(nextProcessingStage)) {
                    actualProcessed.add(noticeNo);
                }
            }

            // Verify expected vs actual
            boolean allExpectedProcessed = actualProcessed.containsAll(expectedProcessed);
            boolean noUnexpectedProcessed = expectedProcessed.size() == actualProcessed.size();

            if (allExpectedProcessed && noUnexpectedProcessed) {
                String resultMsg = String.format("✅ All expected notices processed correctly: %s", actualProcessed);
                TestStepResult result = new TestStepResult("Verify Results", STATUS_SUCCESS);
                result.addDetail(resultMsg);
                return result;
            } else {
                String errorMsg = String.format("❌ Verification failed. Expected: %s, Actual: %s",
                        expectedProcessed, actualProcessed);
                log.error(errorMsg);
                TestStepResult result = new TestStepResult("Verify Results", STATUS_FAILED);
                result.addDetail(errorMsg);
                return result;
            }

        } catch (Exception e) {
            log.error("Error verifying results: {}", e.getMessage(), e);
            TestStepResult result = new TestStepResult("Verify Results", STATUS_FAILED);
            result.addDetail("❌ Error verifying results: " + e.getMessage());
            return result;
        }
    }

    /**
     * Create summary of test results
     */
    private TestStepResult createSummary(List<TestStepResult> results) {
        int totalSteps = results.size();
        long successCount = results.stream()
                .filter(result -> STATUS_SUCCESS.equals(result.getStatus()))
                .count();

        boolean allSuccess = successCount == totalSteps;

        String summaryMsg = String.format("%s Toppan Upload Test: %d/%d steps successful",
                allSuccess ? "✅" : "❌", successCount, totalSteps);

        TestStepResult result = new TestStepResult("Test Summary", allSuccess ? STATUS_SUCCESS : STATUS_FAILED);
        result.addDetail(summaryMsg);
        return result;
    }
}
