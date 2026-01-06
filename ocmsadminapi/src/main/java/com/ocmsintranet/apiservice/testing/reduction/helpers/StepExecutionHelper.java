package com.ocmsintranet.apiservice.testing.reduction.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.testing.main.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for reduction test step execution
 * Orchestrates 4-step reduction test workflow
 */
@Component
@Slf4j
public class StepExecutionHelper {

    private final ObjectMapper objectMapper;
    private final TestContext testContext;
    private final EndpointHelper endpointHelper;
    private final VerificationHelper verificationHelper;

    public StepExecutionHelper(ObjectMapper objectMapper,
                               TestContext testContext,
                               EndpointHelper endpointHelper,
                               VerificationHelper verificationHelper) {
        this.objectMapper = objectMapper;
        this.testContext = testContext;
        this.endpointHelper = endpointHelper;
        this.verificationHelper = verificationHelper;
    }

    /**
     * Execute Step 1 - Load reduction test scenarios from scenario.json
     *
     * @return StepResult with scenario data
     */
    public StepResult executeStep1() {
        log.info("Executing Step 1: Get reduction test scenarios");

        try {
            // Read scenario.json file
            String scenarioFilePath = "src/main/java/com/ocmsintranet/apiservice/testing/reduction/data/scenario.json";
            File scenarioFile = new File(scenarioFilePath);

            if (!scenarioFile.exists()) {
                log.error("Scenario file not found at: {}", scenarioFilePath);
                return new StepResult("Step 1: Get reduction test scenarios", "error", "Scenario file not found");
            }

            // Parse JSON file
            List<Map<String, Object>> scenarios = objectMapper.readValue(
                scenarioFile,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Extract notice numbers from all scenarios
            List<String> noticeNumbers = scenarios.stream()
                .map(scenario -> (String) scenario.get("notice_no"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toList());

            // Create json response with scenarios and notice numbers
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("scenarios", scenarios);
            jsonResponse.put("notice_numbers", noticeNumbers);
            jsonResponse.put("count", noticeNumbers.size());

            log.info("Step 1 completed successfully. Loaded {} reduction test scenarios", noticeNumbers.size());

            return new StepResult("Step 1: Get reduction test scenarios", "success",
                "Loaded " + noticeNumbers.size() + " test scenarios", jsonResponse);

        } catch (IOException e) {
            log.error("Step 1 failed to read scenario file: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get reduction test scenarios", "error",
                "Failed to read scenario file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Step 1 failed with exception: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get reduction test scenarios", "error",
                "Step 1 failed: " + e.getMessage());
        }
    }

    /**
     * Execute Step 2 - Call reduction API for each scenario
     *
     * @param triggerApi Flag to control whether to call the API (default: true)
     * @param scenarios List of scenarios from Step 1
     * @return StepResult with API call results
     */
    @SuppressWarnings("unchecked")
    public StepResult executeStep2(boolean triggerApi, List<Map<String, Object>> scenarios) {
        String endpoint = "/reduction";

        // Skip execution if triggerApi is false
        if (!triggerApi) {
            log.info("Step 2: Skipping reduction API calls (triggerApi is set to false)");
            return new StepResult("Step 2: Call reduction API", "success",
                "triggerApi is set to false", new HashMap<>());
        }

        log.info("Executing Step 2: Call reduction API for {} scenarios", scenarios.size());

        try {
            List<Map<String, Object>> apiResults = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            // Call reduction API for each scenario
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("notice_no");
                String expectedResult = (String) scenario.get("expected_result");

                // Prepare reduction API request payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("notice_no", noticeNo);

                // Get reduction details from scenario
                if (scenario.containsKey("amount_reduced")) {
                    payload.put("amount_reduced", scenario.get("amount_reduced"));
                }
                if (scenario.containsKey("amount_payable")) {
                    payload.put("amount_payable", scenario.get("amount_payable"));
                }
                if (scenario.containsKey("date_of_reduction")) {
                    payload.put("date_of_reduction", scenario.get("date_of_reduction"));
                }
                if (scenario.containsKey("expiry_date_of_reduction")) {
                    payload.put("expiry_date_of_reduction", scenario.get("expiry_date_of_reduction"));
                }
                if (scenario.containsKey("reason_of_reduction")) {
                    payload.put("reason_of_reduction", scenario.get("reason_of_reduction"));
                }
                if (scenario.containsKey("cre_user_id")) {
                    payload.put("cre_user_id", scenario.get("cre_user_id"));
                }

                // Make POST request using EndpointHelper
                Map<String, Object> result = endpointHelper.postToEndpoint(endpoint, payload);

                // Build result entry
                Map<String, Object> apiResult = new HashMap<>();
                apiResult.put("notice_no", noticeNo);
                apiResult.put("expected_result", expectedResult);
                apiResult.put("api_success", result.get("success"));
                apiResult.put("api_response", result.get("data"));

                if ((boolean) result.get("success")) {
                    successCount++;
                } else {
                    failureCount++;
                    apiResult.put("error", result.get("error"));
                }

                apiResults.add(apiResult);
            }

            // Create json response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("api_results", apiResults);
            jsonResponse.put("total_calls", scenarios.size());
            jsonResponse.put("success_count", successCount);
            jsonResponse.put("failure_count", failureCount);

            log.info("Step 2 completed. {} API calls made ({} success, {} failure)",
                scenarios.size(), successCount, failureCount);

            return new StepResult("Step 2: Call reduction API", "success",
                "API calls completed", jsonResponse);

        } catch (Exception e) {
            log.error("Step 2 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 2 failed: " + e.getMessage();
            return new StepResult("Step 2: Call reduction API", "error", errorMessage);
        }
    }

    /**
     * Execute Step 3 - Fetch verification data from Intranet and Internet databases
     *
     * @param noticeNumbers List of notice numbers from Step 1
     * @return StepResult with data fetched from databases
     */
    @SuppressWarnings("unchecked")
    public StepResult executeStep3(List<String> noticeNumbers) {
        log.info("Executing Step 3: Fetch verification data from databases");

        // Prepare shared payload with $limit and noticeNo[$in]
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);

        // Add noticeNo[$in] with comma-separated notice numbers
        if (noticeNumbers != null && !noticeNumbers.isEmpty()) {
            String noticeNoJoined = String.join(",", noticeNumbers);
            payload.put("noticeNo[$in]", noticeNoJoined);
            log.info("Step 3 payload includes {} notice numbers", noticeNumbers.size());
        }

        // 1. POST to /validoffencenoticelist (Intranet VON)
        Map<String, Object> intranetVonResult = endpointHelper.postToEndpoint("/validoffencenoticelist", payload);
        testContext.setIntranetVonData(intranetVonResult.get("data"));
        if (testContext.getIntranetVonData() instanceof List) {
            log.info("Step 3: Intranet VON data list size: {}", ((List<?>) testContext.getIntranetVonData()).size());
        }

        // 2. POST to /evalidoffencenoticelist (Internet VON - if endpoint exists)
        Map<String, Object> internetVonResult = endpointHelper.postToEndpoint("/evalidoffencenoticelist", payload);
        testContext.setInternetVonData(internetVonResult.get("data"));
        if (testContext.getInternetVonData() instanceof List) {
            log.info("Step 3: Internet VON data list size: {}", ((List<?>) testContext.getInternetVonData()).size());
        }

        // 3. POST to /suspendednoticelist (Suspended Notice records)
        Map<String, Object> suspendedNoticeResult = endpointHelper.postToEndpoint("/suspendednoticelist", payload);
        testContext.setSuspendedNoticeData(suspendedNoticeResult.get("data"));
        if (testContext.getSuspendedNoticeData() instanceof List) {
            log.info("Step 3: Suspended Notice data list size: {}", ((List<?>) testContext.getSuspendedNoticeData()).size());
        }

        // 4. POST to /reducedoffenceamountlist (Reduction Log records)
        Map<String, Object> reducedAmountLogResult = endpointHelper.postToEndpoint("/reducedoffenceamountlist", payload);
        testContext.setReducedAmountLogData(reducedAmountLogResult.get("data"));
        if (testContext.getReducedAmountLogData() instanceof List) {
            log.info("Step 3: Reduction Log data list size: {}", ((List<?>) testContext.getReducedAmountLogData()).size());
        }

        // 5. POST to /parameterlist (for eligible rule codes)
        Map<String, Object> parameterPayload = new HashMap<>();
        parameterPayload.put("$limit", 1000);
        Map<String, Object> parameterListResult = endpointHelper.postToEndpoint("/parameterlist", parameterPayload);
        testContext.setParameterListData(parameterListResult.get("data"));
        if (testContext.getParameterListData() instanceof List) {
            log.info("Step 3: Parameter list data size: {}", ((List<?>) testContext.getParameterListData()).size());
        }

        // Build response JSON with all totals and statuses
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("intranet_von_total", intranetVonResult.get("total"));
        jsonResponse.put("internet_von_total", internetVonResult.get("total"));
        jsonResponse.put("suspended_notice_total", suspendedNoticeResult.get("total"));
        jsonResponse.put("reduced_amount_log_total", reducedAmountLogResult.get("total"));
        jsonResponse.put("parameter_list_total", parameterListResult.get("total"));

        jsonResponse.put("intranet_von_status", (boolean) intranetVonResult.get("success") ? "success" : "error");
        jsonResponse.put("internet_von_status", (boolean) internetVonResult.get("success") ? "success" : "error");
        jsonResponse.put("suspended_notice_status", (boolean) suspendedNoticeResult.get("success") ? "success" : "error");
        jsonResponse.put("reduced_amount_log_status", (boolean) reducedAmountLogResult.get("success") ? "success" : "error");
        jsonResponse.put("parameter_list_status", (boolean) parameterListResult.get("success") ? "success" : "error");

        // Determine message based on success/failure
        boolean allSuccess = (boolean) intranetVonResult.get("success") &&
                           (boolean) internetVonResult.get("success") &&
                           (boolean) suspendedNoticeResult.get("success") &&
                           (boolean) reducedAmountLogResult.get("success") &&
                           (boolean) parameterListResult.get("success");

        String responseMessage = allSuccess ? "Step 3 completed" : "Step 3 completed with partial errors";

        log.info("Step 3 completed. Intranet VON: {}, Internet VON: {}, Suspended Notice: {}, Reduction Log: {}, Parameter List: {}",
                jsonResponse.get("intranet_von_status"),
                jsonResponse.get("internet_von_status"),
                jsonResponse.get("suspended_notice_status"),
                jsonResponse.get("reduced_amount_log_status"),
                jsonResponse.get("parameter_list_status"));

        // Always return success status (Step 3 should not fail the workflow)
        return new StepResult("Step 3: Fetch verification data", "success", responseMessage, jsonResponse);
    }

    /**
     * Execute Step 4 - Verify reduction logic for each scenario
     *
     * @param showDetails Whether to include details field in verification results
     * @param scenarios List of scenarios from Step 1
     * @return StepResult with verification results
     */
    @SuppressWarnings("unchecked")
    public StepResult executeStep4(boolean showDetails, List<Map<String, Object>> scenarios) {
        log.info("Executing Step 4: Verify reduction logic (showDetails={})", showDetails);

        try {
            List<Map<String, Object>> verificationResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;

            // Verify each scenario
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("notice_no");
                String expectedResult = (String) scenario.get("expected_result");

                Map<String, Object> result;

                if ("success".equals(expectedResult)) {
                    // Verify successful reduction
                    Object amountReducedObj = scenario.get("amount_reduced");
                    BigDecimal expectedReducedAmount = parseBigDecimal(amountReducedObj);

                    if (expectedReducedAmount != null) {
                        result = verificationHelper.checkReductionSuccess(
                            noticeNo, expectedReducedAmount, showDetails, testContext
                        );
                    } else {
                        result = new HashMap<>();
                        result.put("notice_no", noticeNo);
                        result.put("status", "❌ error");
                        result.put("message", "Invalid amount_reduced in scenario");
                    }
                } else {
                    // Verify rejection
                    String expectedReason = (String) scenario.getOrDefault("rejection_reason", expectedResult);
                    result = verificationHelper.checkReductionRejected(
                        noticeNo, expectedReason, showDetails, testContext
                    );
                }

                // Add scenario info to result
                result.put("scenario", scenario.get("scenario"));
                result.put("expected_result", expectedResult);

                // Count results
                String status = (String) result.get("status");
                if (status != null && status.contains("success")) {
                    successCount++;
                } else {
                    failedCount++;
                }

                verificationResults.add(result);
            }

            // Create json response with verification results
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("verification_results", verificationResults);
            jsonResponse.put("total_verified", verificationResults.size());
            jsonResponse.put("success_count", successCount);
            jsonResponse.put("failed_count", failedCount);

            log.info("Step 4 completed successfully. Verified {} scenarios ({} success, {} failed)",
                    verificationResults.size(), successCount, failedCount);

            return new StepResult("Step 4: Verify reduction logic", "success",
                "Step 4 completed", jsonResponse);

        } catch (Exception e) {
            log.error("Step 4 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 4 failed: " + e.getMessage();
            return new StepResult("Step 4: Verify reduction logic", "error", errorMessage);
        }
    }

    /**
     * Create a skipped step result
     *
     * @param stepName Name of the step
     * @param reason Reason for skipping
     * @return StepResult with skipped status
     */
    public StepResult createSkippedStep(String stepName, String reason) {
        log.info("Skipping {} due to: {}", stepName, reason);
        return new StepResult(stepName, "skipped", reason);
    }

    /**
     * Parse object value to BigDecimal
     *
     * @param value Object to parse
     * @return BigDecimal value, or null if parsing fails
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }
                return new BigDecimal(strValue);
            }
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from value: {} ({})", value, e.getMessage());
        }

        return null;
    }
}
