package com.ocmsintranet.cronservice.testing.suspension_revival.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base Step Execution Helper for Suspension/Revival Tests
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Implements the 4-step test pattern:
 * - Step 1: Load Test Scenarios (from scenario.json)
 * - Step 2: Trigger Process/API (conditionally)
 * - Step 3: Fetch Verification Data (from database)
 * - Step 4: Verify Business Logic
 *
 * This is a base class - extend for module-specific implementations
 */
@Slf4j
@Component
public class SuspensionStepExecutionHelper {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SuspensionTestContext testContext;

    @Autowired
    private SuspensionEndpointHelper endpointHelper;

    @Autowired
    private SuspensionVerificationHelper verificationHelper;

    /**
     * Step 1: Load test scenarios from JSON file
     *
     * @param scenarioFilePath Path to scenario.json file
     * @return Step result
     */
    public TestStepResult executeStep1LoadScenarios(String scenarioFilePath) {
        try {
            log.info("Step 1: Loading test scenarios from {}", scenarioFilePath);

            // Try to load from classpath first
            List<Map<String, Object>> scenarios;
            try {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scenarioFilePath);
                if (inputStream != null) {
                    scenarios = objectMapper.readValue(inputStream, List.class);
                } else {
                    // Fall back to file system
                    File scenarioFile = new File(scenarioFilePath);
                    scenarios = objectMapper.readValue(scenarioFile, List.class);
                }
            } catch (Exception e) {
                // Try relative path from project root
                String fullPath = "src/main/resources/" + scenarioFilePath;
                File scenarioFile = new File(fullPath);
                scenarios = objectMapper.readValue(scenarioFile, List.class);
            }

            if (scenarios == null || scenarios.isEmpty()) {
                return new TestStepResult(
                        "Step 1: Load Test Scenarios",
                        "error",
                        "No scenarios found in file"
                );
            }

            // Extract notice numbers from scenarios
            List<String> noticeNumbers = scenarios.stream()
                    .map(s -> (String) s.get("noticeNo"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Store in context for next steps
            testContext.setScenarios(scenarios);
            testContext.setNoticeNumbers(noticeNumbers);

            // Build response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("scenarios", scenarios);
            jsonResponse.put("noticeNumbers", noticeNumbers);
            jsonResponse.put("count", scenarios.size());

            String message = String.format("Loaded %d scenarios from %s", scenarios.size(), scenarioFilePath);
            log.info(message);

            return new TestStepResult(
                    "Step 1: Load Test Scenarios",
                    "success",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 1 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 1: Load Test Scenarios",
                    "error",
                    "Step 1 failed: " + e.getMessage()
            );
        }
    }

    /**
     * Step 3: Fetch verification data from database
     *
     * @param noticeNumbers List of notice numbers to fetch
     * @return Step result
     */
    public TestStepResult executeStep3FetchData(List<String> noticeNumbers) {
        try {
            log.info("Step 3: Fetching verification data for {} notices", noticeNumbers.size());

            // Fetch suspension data
            Map<String, Object> suspensionResult = endpointHelper.fetchSuspensions(noticeNumbers);
            List<Map<String, Object>> suspensionData = (List<Map<String, Object>>) suspensionResult.get("data");
            testContext.setSuspensionData(suspensionData);

            // Fetch VON data
            Map<String, Object> vonResult = endpointHelper.fetchVonRecords(noticeNumbers);
            List<Map<String, Object>> vonData = (List<Map<String, Object>>) vonResult.get("data");
            testContext.setVonData(vonData);

            // Build response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("suspensions_fetched", suspensionData != null ? suspensionData.size() : 0);
            jsonResponse.put("von_records_fetched", vonData != null ? vonData.size() : 0);
            jsonResponse.put("api_base_url", endpointHelper.getApiBaseUrl());

            String message = String.format(
                    "Fetched %d suspensions and %d VON records",
                    suspensionData != null ? suspensionData.size() : 0,
                    vonData != null ? vonData.size() : 0
            );
            log.info(message);

            return new TestStepResult(
                    "Step 3: Fetch Verification Data",
                    "success",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 3 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 3: Fetch Verification Data",
                    "error",
                    "Step 3 failed: " + e.getMessage()
            );
        }
    }

    /**
     * Step 4: Verify business logic
     *
     * @param scenarios   List of scenarios to verify
     * @param showDetails Show detailed verification results
     * @return Step result
     */
    public TestStepResult executeStep4Verify(List<Map<String, Object>> scenarios, boolean showDetails) {
        try {
            log.info("Step 4: Verifying business logic for {} scenarios", scenarios.size());

            List<Map<String, Object>> verificationResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;

            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String expectedResult = (String) scenario.get("expected_result");
                String suspensionType = (String) scenario.get("suspensionType");
                String reasonOfSuspension = (String) scenario.get("reasonOfSuspension");

                Map<String, Object> result;

                if ("success".equals(expectedResult)) {
                    // Verify suspension created successfully
                    result = verificationHelper.verifySuspensionCreated(
                            noticeNo,
                            suspensionType,
                            reasonOfSuspension,
                            showDetails,
                            testContext
                    );
                } else {
                    // Verify suspension NOT created (rejection)
                    result = verificationHelper.verifySuspensionNotCreated(
                            noticeNo,
                            expectedResult,
                            showDetails,
                            testContext
                    );
                }

                verificationResults.add(result);

                String status = (String) result.get("status");
                if (status != null && status.contains("✅")) {
                    successCount++;
                } else {
                    failedCount++;
                }
            }

            // Build response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("verification_results", verificationResults);
            jsonResponse.put("total_scenarios", scenarios.size());
            jsonResponse.put("success_count", successCount);
            jsonResponse.put("failed_count", failedCount);

            String message = String.format(
                    "Verified %d scenarios: %d passed, %d failed",
                    scenarios.size(),
                    successCount,
                    failedCount
            );
            log.info(message);

            return new TestStepResult(
                    "Step 4: Verify Business Logic",
                    failedCount == 0 ? "success" : "error",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 4 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 4: Verify Business Logic",
                    "error",
                    "Step 4 failed: " + e.getMessage()
            );
        }
    }

    /**
     * Create a skipped step result
     *
     * @param stepName Step name
     * @param reason   Reason for skipping
     * @return Step result
     */
    public TestStepResult createSkippedStep(String stepName, String reason) {
        return new TestStepResult(stepName, "skipped", reason);
    }
}
