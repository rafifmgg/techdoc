package com.ocmsintranet.apiservice.testing.toppan.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.testing.main.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for step execution utilities
 * Orchestrates test steps using EndpointHelper, VerificationHelper, and TestContext
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
     * Execute Step 1 - Create notices by reading scenario.json
     *
     * @return StepResult with notice numbers from scenario.json
     */
    public StepResult executeStep1() {
        log.info("Executing Step 1: Get preload data");

        try {
            // Read scenario.json file
            String scenarioFilePath = "src/main/java/com/ocmsintranet/apiservice/testing/toppan/data/scenario.json";
            File scenarioFile = new File(scenarioFilePath);

            if (!scenarioFile.exists()) {
                log.error("Scenario file not found at: {}", scenarioFilePath);
                return new StepResult("Step 1", "error", "Scenario file not found");
            }

            // Parse JSON file
            List<Map<String, Object>> scenarios = objectMapper.readValue(
                scenarioFile,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Extract notice_no from all scenarios
            List<String> noticeNumbers = scenarios.stream()
                .map(scenario -> (String) scenario.get("notice_no"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toList());

            // Create json response with notice numbers
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("notice_no", noticeNumbers);
            jsonResponse.put("count", noticeNumbers.size());

            log.info("Step 1 completed successfully. Loaded {} notice numbers", noticeNumbers.size());

            return new StepResult("Step 1: Get preload data", "success", "Get preload data completed", jsonResponse);

        } catch (IOException e) {
            log.error("Step 1 failed to read scenario file: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get preload data", "error", "Failed to read scenario file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Step 1 failed with exception: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get preload data", "error", "Step 1 failed: " + e.getMessage());
        }
    }

    /**
     * Execute Step 2 - POST to /lta-enquiry/trigger endpoint
     *
     * @param triggerCron Flag to control whether to trigger the cron (default: true)
     * @return StepResult with response from the endpoint
     */
    public StepResult executeStep2(boolean triggerCron) {
        String endpoint = "/cron/toppan-enquiry/trigger";

        // Skip execution if triggerCron is false
        if (!triggerCron) {
            log.info("Step 2: Skipping cron trigger (triggerCron is set to false)");
            return new StepResult("Step 2: Trigger Cron " + endpoint, "success", "triggerCron is set to false", new HashMap<>());
        }

        log.info("Executing Step 2: Trigger Cron {}", endpoint);

        try {
            // return new StepResult("Step 2: Trigger Cron " + endpoint, "success", "Trigger Cron completed", Object.class);
            // Prepare empty payload
            Map<String, Object> payload = new HashMap<>();

            // Make POST request using EndpointHelper
            Map<String, Object> result = endpointHelper.postToEndpoint(endpoint, payload);

            if ((boolean) result.get("success")) {
                String responseMessage = "Step 2 completed";
                Object jsonResponse = result.get("data");
                log.info("Step 2 completed successfully.");

                return new StepResult("Step 2: Trigger Cron " + endpoint, "success", responseMessage, jsonResponse);
            } else {
                String errorMessage = "Step 2 failed: " + result.get("error");
                return new StepResult("Step 2: Trigger Cron " + endpoint, "error", errorMessage);
            }

        } catch (Exception e) {
            log.error("Step 2 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 2 failed: " + e.getMessage();
            return new StepResult("Step 2: Trigger Cron " + endpoint, "error", errorMessage);
        }
    }

    /**
     * Execute Step 3 - POST to 4 endpoints to collect data for verification
     *
     * @param noticeNumbers List of notice numbers from Step 1
     * @return StepResult with totals from all endpoints
     */
    @SuppressWarnings("unchecked")
    public StepResult executeStep3(List<String> noticeNumbers) {
        log.info("Executing Step 3: Calling POST to 4 endpoints");

        // Prepare shared payload with $limit and noticeNo[$in]
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);

        // Add noticeNo[$in] with comma-separated notice numbers
        if (noticeNumbers != null && !noticeNumbers.isEmpty()) {
            String noticeNoJoined = String.join(",", noticeNumbers);
            payload.put("noticeNo[$in]", noticeNoJoined);
            log.info("Step 3 payload includes {} notice numbers", noticeNumbers.size());
        }

        // POST to 3 endpoints sequentially
        // 1. POST to /validoffencenoticelist
        Map<String, Object> validOffenceResult = endpointHelper.postToEndpoint("/validoffencenoticelist", payload);
        testContext.setValidOffenceNoticeData(validOffenceResult.get("data"));
        if (testContext.getValidOffenceNoticeData() instanceof List) {
            log.info("Step 3: validoffencenoticelist data list size: {}", ((List<?>) testContext.getValidOffenceNoticeData()).size());
        }

        // 2. POST to /requestdriverparticularslist
        Map<String, Object> driverParticularsResult = endpointHelper.postToEndpoint("/requestdriverparticularslist", payload);
        testContext.setDriverParticularsData(driverParticularsResult.get("data"));
        if (testContext.getDriverParticularsData() instanceof List) {
            log.info("Step 3: requestdriverparticularslist data list size: {}", ((List<?>) testContext.getDriverParticularsData()).size());
        }

        // 3. POST to /drivernoticelist
        Map<String, Object> driverNoticeResult = endpointHelper.postToEndpoint("/drivernoticelist", payload);
        testContext.setDriverNoticeData(driverNoticeResult.get("data"));
        if (testContext.getDriverNoticeData() instanceof List) {
            log.info("Step 3: drivernoticelist data list size: {}", ((List<?>) testContext.getDriverNoticeData()).size());
        }

        // 4. POST to /parameterlist (without noticeNo filter, only $limit)
        Map<String, Object> parameterPayload = new HashMap<>();
        parameterPayload.put("$limit", 1000);
        Map<String, Object> parameterListResult = endpointHelper.postToEndpoint("/parameterlist", parameterPayload);
        testContext.setParameterListData(parameterListResult.get("data"));
        if (testContext.getParameterListData() instanceof List) {
            log.info("Step 3: parameterlist data list size: {}", ((List<?>) testContext.getParameterListData()).size());
        }

        // Build response JSON with all totals and statuses
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("total_valid_offence", validOffenceResult.get("total"));
        jsonResponse.put("total_driver_particulars", driverParticularsResult.get("total"));
        jsonResponse.put("total_driver_notice", driverNoticeResult.get("total"));
        jsonResponse.put("total_parameter_list", parameterListResult.get("total"));

        jsonResponse.put("validoffencenoticelist_status", (boolean) validOffenceResult.get("success") ? "success" : "error");
        jsonResponse.put("requestdriverparticularslist_status", (boolean) driverParticularsResult.get("success") ? "success" : "error");
        jsonResponse.put("drivernoticelist_status", (boolean) driverNoticeResult.get("success") ? "success" : "error");
        jsonResponse.put("parameterlist_status", (boolean) parameterListResult.get("success") ? "success" : "error");

        // Add error messages if any endpoint failed
        if (!((boolean) validOffenceResult.get("success")) && validOffenceResult.containsKey("error")) {
            jsonResponse.put("validoffencenoticelist_error", validOffenceResult.get("error"));
        }
        if (!((boolean) driverParticularsResult.get("success")) && driverParticularsResult.containsKey("error")) {
            jsonResponse.put("requestdriverparticularslist_error", driverParticularsResult.get("error"));
        }
        if (!((boolean) driverNoticeResult.get("success")) && driverNoticeResult.containsKey("error")) {
            jsonResponse.put("drivernoticelist_error", driverNoticeResult.get("error"));
        }
        if (!((boolean) parameterListResult.get("success")) && parameterListResult.containsKey("error")) {
            jsonResponse.put("parameterlist_error", parameterListResult.get("error"));
        }

        // Determine message based on success/failure
        boolean allSuccess = (boolean) validOffenceResult.get("success") &&
                           (boolean) driverParticularsResult.get("success") &&
                           (boolean) driverNoticeResult.get("success") &&
                           (boolean) parameterListResult.get("success");

        String responseMessage = allSuccess ? "Step 3 completed" : "Step 3 completed with partial errors";

        log.info("Step 3 completed. validoffencenoticelist: {}, requestdriverparticularslist: {}, drivernoticelist: {}, parameterlist: {}",
                jsonResponse.get("validoffencenoticelist_status"),
                jsonResponse.get("requestdriverparticularslist_status"),
                jsonResponse.get("drivernoticelist_status"),
                jsonResponse.get("parameterlist_status"));

        // Always return success status (Step 3 should not fail the workflow)
        return new StepResult("Step 3: get latest valid offence data", "success", responseMessage, jsonResponse);
    }

    /**
     * Execute Step 4 - Verify data by checking each notice number from Step 3 data
     *
     * @param showDetails Whether to include details field in verification results
     * @return StepResult with verification results
     */
    @SuppressWarnings("unchecked")
    public StepResult executeStep4(boolean showDetails) {
        log.info("Executing Step 4: Verify data from Step 3 (showDetails={})", showDetails);

        try {
            // Extract notice numbers from step3Data
            List<String> noticeNumbers = extractNoticeNumbersFromStep3Data();

            if (noticeNumbers == null || noticeNumbers.isEmpty()) {
                log.warn("Step 4: No notice numbers to verify from Step 3 data");
                return new StepResult("Step 4", "error", "No notice numbers to verify");
            }

            log.info("Step 4: Verifying {} notice numbers", noticeNumbers.size());

            // Loop through each notice number and verify with appropriate flow
            List<Map<String, Object>> verificationResults = new ArrayList<>();
            int successFlowCount = 0;
            int stayInCurrentCount = 0;
            int suspendFlowCount = 0;

            for (String noticeNo : noticeNumbers) {
                Map<String, Object> result;
                if (verificationHelper.isSuccessFlowNotice(noticeNo)) {
                    // Use success flow verification
                    log.debug("Notice {}: Using SUCCESS flow", noticeNo);
                    result = verificationHelper.checkSuccessFlow(noticeNo, showDetails, testContext);
                    successFlowCount++;
                } else if (verificationHelper.isStayInCurrentNotice(noticeNo)) {
                    // Use stay in current verification
                    log.debug("Notice {}: Using STAY_IN_CURRENT flow", noticeNo);
                    result = verificationHelper.checkStayInCurrent(noticeNo, showDetails, testContext);
                    stayInCurrentCount++;
                } else if (verificationHelper.isSuspendFlowNotice(noticeNo)) {
                    // Use suspend flow verification
                    log.debug("Notice {}: Using SUSPEND flow", noticeNo);
                    result = verificationHelper.checkSuspendFlow(noticeNo, showDetails, testContext);
                    suspendFlowCount++;
                } else {
                    result = verificationHelper.checkStayInCurrent(noticeNo, showDetails, testContext);
                    stayInCurrentCount++;
                }
                verificationResults.add(result);
            }

            // Create json response with verification results
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("verification_results", verificationResults);
            jsonResponse.put("total_verified", verificationResults.size());
            jsonResponse.put("success_flow_count", successFlowCount);
            jsonResponse.put("stay_in_current_count", stayInCurrentCount);
            jsonResponse.put("suspend_flow_count", suspendFlowCount);

            log.info("Step 4 completed successfully. Verified {} notice numbers ({} success flow, {} stay in current, {} suspend flow)",
                    verificationResults.size(), successFlowCount, stayInCurrentCount, suspendFlowCount);

            return new StepResult("Step 4", "success", "Step 4 completed", jsonResponse);

        } catch (Exception e) {
            log.error("Step 4 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 4 failed: " + e.getMessage();
            return new StepResult("Step 4", "error", errorMessage);
        }
    }

    /**
     * Extract notice numbers from testContext validOffenceNoticeData
     * Assumes validOffenceNoticeData is already a List (extracted in Step 3)
     *
     * @return List of notice numbers, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    private List<String> extractNoticeNumbersFromStep3Data() {
        try {
            Object validOffenceNoticeData = testContext.getValidOffenceNoticeData();
            log.info("Extracting notice numbers from validOffenceNoticeData. validOffenceNoticeData is null: {}", validOffenceNoticeData == null);

            if (validOffenceNoticeData == null) {
                log.warn("validOffenceNoticeData is null - no data stored from Step 3");
                return new ArrayList<>();
            }

            log.info("validOffenceNoticeData type: {}, Is List: {}",
                    validOffenceNoticeData.getClass().getSimpleName(),
                    validOffenceNoticeData instanceof List);

            // validOffenceNoticeData should already be a List (extracted in Step 3)
            if (validOffenceNoticeData instanceof List) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) validOffenceNoticeData;
                log.info("Data list size: {}", dataList.size());

                List<String> noticeNumbers = new ArrayList<>();
                for (int i = 0; i < dataList.size(); i++) {
                    Map<String, Object> item = dataList.get(i);
                    Object noticeNo = item.get("noticeNo");
                    if (noticeNo instanceof String) {
                        noticeNumbers.add((String) noticeNo);
                    } else {
                        log.warn("Item {} does not have 'noticeNo' field or it's not a String. Available keys: {}",
                                i, item.keySet());
                    }
                }

                log.info("Extracted {} notice numbers from validOffenceNoticeData", noticeNumbers.size());
                return noticeNumbers;
            } else {
                log.error("validOffenceNoticeData is not a List! Type: {}. This should not happen - check Step 3 extraction.",
                        validOffenceNoticeData.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to extract notice numbers from Step 3 data: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Execute Download Step 1 - Get download data by reading scenario.json
     * Filters specific notice numbers: 500405006C, 500405007G, 500405030E, 500405031I
     *
     * @return StepResult with filtered notice numbers from scenario.json
     */
    public StepResult executeDownloadStep1() {
        log.info("Executing Download Step 1: Get download data");

        try {
            // Read scenario.json file
            String scenarioFilePath = "src/main/java/com/ocmsintranet/apiservice/testing/toppan/data/scenario.json";
            File scenarioFile = new File(scenarioFilePath);

            if (!scenarioFile.exists()) {
                log.error("Scenario file not found at: {}", scenarioFilePath);
                return new StepResult("Step 1: Get download data", "error", "Scenario file not found");
            }

            // Parse JSON file
            List<Map<String, Object>> scenarios = objectMapper.readValue(
                scenarioFile,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Define target notice numbers for download
            Set<String> targetNotices = Set.of("500405006C", "500405007G", "500405030E", "500405031I");

            // Filter scenarios by notice number
            List<String> noticeNumbers = scenarios.stream()
                .map(scenario -> (String) scenario.get("notice_no"))
                .filter(noticeNo -> noticeNo != null && targetNotices.contains(noticeNo))
                .collect(Collectors.toList());

            // Create json response with filtered notice numbers
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("notice_no", noticeNumbers);
            jsonResponse.put("count", noticeNumbers.size());

            log.info("Download Step 1 completed successfully. Loaded {} notice numbers (RD2/DN2)", noticeNumbers.size());

            return new StepResult("Step 1: Get download data", "success", "Get download data completed", jsonResponse);

        } catch (IOException e) {
            log.error("Download Step 1 failed to read scenario file: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get download data", "error", "Failed to read scenario file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Download Step 1 failed with exception: {}", e.getMessage(), e);
            return new StepResult("Step 1: Get download data", "error", "Step 1 failed: " + e.getMessage());
        }
    }

    /**
     * Execute Download Step 2 - POST to /cron/toppan-result/trigger endpoint
     *
     * @param triggerCron Flag to control whether to trigger the cron (default: true)
     * @return StepResult with response from the endpoint
     */
    public StepResult executeDownloadStep2(boolean triggerCron) {
        String endpoint = "/cron/toppan-result/trigger";

        // Skip execution if triggerCron is false
        if (!triggerCron) {
            log.info("Download Step 2: Skipping cron trigger (triggerCron is set to false)");
            return new StepResult("Step 2: Trigger Cron " + endpoint, "success", "triggerCron is set to false", new HashMap<>());
        }

        log.info("Executing Download Step 2: Trigger Cron {}", endpoint);

        try {
            // Prepare empty payload
            Map<String, Object> payload = new HashMap<>();

            // Make POST request using EndpointHelper
            Map<String, Object> result = endpointHelper.postToEndpoint(endpoint, payload);

            if ((boolean) result.get("success")) {
                String responseMessage = "Step 2 completed";
                Object jsonResponse = result.get("data");
                log.info("Download Step 2 completed successfully.");

                return new StepResult("Step 2: Trigger Cron " + endpoint, "success", responseMessage, jsonResponse);
            } else {
                String errorMessage = "Step 2 failed: " + result.get("error");
                return new StepResult("Step 2: Trigger Cron " + endpoint, "error", errorMessage);
            }

        } catch (Exception e) {
            log.error("Download Step 2 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 2 failed: " + e.getMessage();
            return new StepResult("Step 2: Trigger Cron " + endpoint, "error", errorMessage);
        }
    }

    /**
     * Execute Download Step 3 - POST to 3 endpoints to collect data for verification
     *
     * @param noticeNumbers List of notice numbers from Step 1
     * @return StepResult with totals from all endpoints
     */
    @SuppressWarnings("unchecked")
    public StepResult executeDownloadStep3(List<String> noticeNumbers) {
        log.info("Executing Download Step 3: Calling POST to 3 endpoints");

        // Prepare shared payload with $limit and noticeNo[$in]
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);

        // Add noticeNo[$in] with comma-separated notice numbers
        if (noticeNumbers != null && !noticeNumbers.isEmpty()) {
            String noticeNoJoined = String.join(",", noticeNumbers);
            payload.put("noticeNo[$in]", noticeNoJoined);
            log.info("Download Step 3 payload includes {} notice numbers", noticeNumbers.size());
        }

        // POST to 3 endpoints sequentially
        // 1. POST to /validoffencenoticelist
        Map<String, Object> validOffenceResult = endpointHelper.postToEndpoint("/validoffencenoticelist", payload);
        testContext.setValidOffenceNoticeData(validOffenceResult.get("data"));
        if (testContext.getValidOffenceNoticeData() instanceof List) {
            log.info("Download Step 3: validoffencenoticelist data list size: {}", ((List<?>) testContext.getValidOffenceNoticeData()).size());
        }

        // 2. POST to /requestdriverparticularslist
        Map<String, Object> driverParticularsResult = endpointHelper.postToEndpoint("/requestdriverparticularslist", payload);
        testContext.setDriverParticularsData(driverParticularsResult.get("data"));
        if (testContext.getDriverParticularsData() instanceof List) {
            log.info("Download Step 3: requestdriverparticularslist data list size: {}", ((List<?>) testContext.getDriverParticularsData()).size());
        }

        // 3. POST to /drivernoticelist
        Map<String, Object> driverNoticeResult = endpointHelper.postToEndpoint("/drivernoticelist", payload);
        testContext.setDriverNoticeData(driverNoticeResult.get("data"));
        if (testContext.getDriverNoticeData() instanceof List) {
            log.info("Download Step 3: drivernoticelist data list size: {}", ((List<?>) testContext.getDriverNoticeData()).size());
        }

        // 4. POST to /parameterlist (without noticeNo filter, only $limit)
        Map<String, Object> parameterPayload = new HashMap<>();
        parameterPayload.put("$limit", 1000);
        Map<String, Object> parameterListResult = endpointHelper.postToEndpoint("/parameterlist", parameterPayload);
        testContext.setParameterListData(parameterListResult.get("data"));
        if (testContext.getParameterListData() instanceof List) {
            log.info("Download Step 3: parameterlist data list size: {}", ((List<?>) testContext.getParameterListData()).size());
        }

        // Build response JSON with all totals and statuses
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("total_valid_offence", validOffenceResult.get("total"));
        jsonResponse.put("total_driver_particulars", driverParticularsResult.get("total"));
        jsonResponse.put("total_driver_notice", driverNoticeResult.get("total"));
        jsonResponse.put("total_parameter_list", parameterListResult.get("total"));

        jsonResponse.put("validoffencenoticelist_status", (boolean) validOffenceResult.get("success") ? "success" : "error");
        jsonResponse.put("requestdriverparticularslist_status", (boolean) driverParticularsResult.get("success") ? "success" : "error");
        jsonResponse.put("drivernoticelist_status", (boolean) driverNoticeResult.get("success") ? "success" : "error");
        jsonResponse.put("parameterlist_status", (boolean) parameterListResult.get("success") ? "success" : "error");

        // Add error messages if any endpoint failed
        if (!((boolean) validOffenceResult.get("success")) && validOffenceResult.containsKey("error")) {
            jsonResponse.put("validoffencenoticelist_error", validOffenceResult.get("error"));
        }
        if (!((boolean) driverParticularsResult.get("success")) && driverParticularsResult.containsKey("error")) {
            jsonResponse.put("requestdriverparticularslist_error", driverParticularsResult.get("error"));
        }
        if (!((boolean) driverNoticeResult.get("success")) && driverNoticeResult.containsKey("error")) {
            jsonResponse.put("drivernoticelist_error", driverNoticeResult.get("error"));
        }
        if (!((boolean) parameterListResult.get("success")) && parameterListResult.containsKey("error")) {
            jsonResponse.put("parameterlist_error", parameterListResult.get("error"));
        }

        // Determine message based on success/failure
        boolean allSuccess = (boolean) validOffenceResult.get("success") &&
                           (boolean) driverParticularsResult.get("success") &&
                           (boolean) driverNoticeResult.get("success") &&
                           (boolean) parameterListResult.get("success");

        String responseMessage = allSuccess ? "Step 3 completed" : "Step 3 completed with partial errors";

        log.info("Download Step 3 completed. validoffencenoticelist: {}, requestdriverparticularslist: {}, drivernoticelist: {}, parameterlist: {}",
                jsonResponse.get("validoffencenoticelist_status"),
                jsonResponse.get("requestdriverparticularslist_status"),
                jsonResponse.get("drivernoticelist_status"),
                jsonResponse.get("parameterlist_status"));

        // Always return success status (Step 3 should not fail the workflow)
        return new StepResult("Step 3: Get valid offence notice data", "success", responseMessage, jsonResponse);
    }

    /**
     * Execute Download Step 4 - Verify data by checking each notice number from Step 3 data
     *
     * @param showDetails Whether to include details field in verification results
     * @return StepResult with verification results
     */
    @SuppressWarnings("unchecked")
    public StepResult executeDownloadStep4(boolean showDetails) {
        log.info("Executing Download Step 4: Verify data from Step 3 (showDetails={})", showDetails);

        try {
            // Extract notice numbers from step3Data
            List<String> noticeNumbers = extractNoticeNumbersFromStep3Data();

            if (noticeNumbers == null || noticeNumbers.isEmpty()) {
                log.warn("Download Step 4: No notice numbers to verify from Step 3 data");
                return new StepResult("Step 4: Verify download data", "error", "No notice numbers to verify");
            }

            log.info("Download Step 4: Verifying {} notice numbers", noticeNumbers.size());

            // Loop through each notice number and verify with success download flow
            List<Map<String, Object>> verificationResults = new ArrayList<>();
            int successFlowCount = 0;

            for (String noticeNo : noticeNumbers) {
                Map<String, Object> result;
                if (verificationHelper.isSuccessDownloadFlow(noticeNo)) {
                    // Use success flow verification (shows VON, RDP, DN details)
                    log.debug("Notice {}: Using SUCCESS DOWNLOAD flow", noticeNo);
                    result = verificationHelper.checkSuccessFlow(noticeNo, showDetails, testContext);
                    successFlowCount++;
                } else {
                    // Not a download notice, skip verification
                    result = new HashMap<>();
                    result.put("notice_no", noticeNo);
                    result.put("status", "⚠️ skipped");
                    result.put("message", "Not a download flow notice");
                    log.debug("Notice {}: Skipped (not in download flow)", noticeNo);
                }
                verificationResults.add(result);
            }

            // Create json response with verification results
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("verification_results", verificationResults);
            jsonResponse.put("total_verified", verificationResults.size());
            jsonResponse.put("success_flow_count", successFlowCount);

            log.info("Download Step 4 completed successfully. Verified {} notice numbers ({} success flow)",
                    verificationResults.size(), successFlowCount);

            return new StepResult("Step 4: Verify download data", "success", "Step 4 completed", jsonResponse);

        } catch (Exception e) {
            log.error("Download Step 4 failed with exception: {}", e.getMessage(), e);
            String errorMessage = "Step 4 failed: " + e.getMessage();
            return new StepResult("Step 4: Verify download data", "error", errorMessage);
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
}
