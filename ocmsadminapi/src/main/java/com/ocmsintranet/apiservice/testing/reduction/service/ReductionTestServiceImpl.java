package com.ocmsintranet.apiservice.testing.reduction.service;

import com.ocmsintranet.apiservice.testing.main.StepResult;
import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestRequest;
import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestResponse;
import com.ocmsintranet.apiservice.testing.reduction.helpers.StepExecutionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service implementation for Reduction testing operations
 * Executes 4-step workflow and always returns results for all steps
 */
@Service
@Slf4j
public class ReductionTestServiceImpl implements ReductionTestService {

    private final StepExecutionHelper stepExecutionHelper;

    public ReductionTestServiceImpl(StepExecutionHelper stepExecutionHelper) {
        this.stepExecutionHelper = stepExecutionHelper;
    }

    /**
     * Execute Reduction test with 4 steps:
     * - Step 1: Load test scenarios from scenario.json
     * - Step 2: Call reduction API for each scenario (execute if Step 1 succeeds)
     * - Step 3: Fetch verification data from databases (execute if Step 2 succeeds)
     * - Step 4: Verify reduction logic (execute if Step 3 succeeds)
     *
     * @param request ReductionTestRequest containing details and triggerApi flags
     * @return ReductionTestResponse with results from all steps (never throws exception)
     */
    @Override
    public ReductionTestResponse executeReductionTest(ReductionTestRequest request) {
        log.info("Starting Reduction test with details={}, triggerApi={}",
            request.getDetails(), request.getTriggerApi());

        ReductionTestResponse response = new ReductionTestResponse();

        // Execute Step 1 (load scenarios)
        StepResult step1Result = stepExecutionHelper.executeStep1();
        response.addStep(step1Result);

        // Extract scenarios from Step 1 result
        List<Map<String, Object>> scenarios = extractScenarios(step1Result);
        List<String> noticeNumbers = extractNoticeNumbers(step1Result);

        // Execute Step 2 based on Step 1 result
        StepResult step2Result;
        if ("success".equals(step1Result.getStatus()) && scenarios != null) {
            // Step 1 succeeded, execute Step 2
            step2Result = stepExecutionHelper.executeStep2(request.getTriggerApi(), scenarios);
        } else {
            // Step 1 failed, skip Step 2
            step2Result = stepExecutionHelper.createSkippedStep(
                "Step 2: Call reduction API",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step2Result);

        // Execute Step 3 based on Step 2 result
        StepResult step3Result;
        if ("success".equals(step2Result.getStatus()) && noticeNumbers != null) {
            // Step 2 succeeded, execute Step 3
            step3Result = stepExecutionHelper.executeStep3(noticeNumbers);
        } else {
            // Step 2 failed or skipped, skip Step 3
            step3Result = stepExecutionHelper.createSkippedStep(
                "Step 3: Fetch verification data",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step3Result);

        // Execute Step 4 based on Step 3 result
        StepResult step4Result;
        if ("success".equals(step3Result.getStatus()) && scenarios != null) {
            // Step 3 succeeded, execute Step 4
            step4Result = stepExecutionHelper.executeStep4(request.getDetails(), scenarios);
        } else {
            // Step 3 failed or skipped, skip Step 4
            step4Result = stepExecutionHelper.createSkippedStep(
                "Step 4: Verify reduction logic",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step4Result);

        log.info("Reduction test completed. Step 1: {}, Step 2: {}, Step 3: {}, Step 4: {}",
                step1Result.getStatus(), step2Result.getStatus(),
                step3Result.getStatus(), step4Result.getStatus());

        return response;
    }

    /**
     * Extract scenarios from Step 1 result
     *
     * @param step1Result Step 1 result containing scenarios in json field
     * @return List of scenarios, or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractScenarios(StepResult step1Result) {
        try {
            Object json = step1Result.getJson();
            if (json instanceof Map) {
                Map<String, Object> jsonMap = (Map<String, Object>) json;
                Object scenariosObj = jsonMap.get("scenarios");
                if (scenariosObj instanceof List) {
                    return (List<Map<String, Object>>) scenariosObj;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract scenarios from Step 1: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract notice numbers from Step 1 result
     *
     * @param step1Result Step 1 result containing notice_numbers in json field
     * @return List of notice numbers, or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<String> extractNoticeNumbers(StepResult step1Result) {
        try {
            Object json = step1Result.getJson();
            if (json instanceof Map) {
                Map<String, Object> jsonMap = (Map<String, Object>) json;
                Object noticeNumbersObj = jsonMap.get("notice_numbers");
                if (noticeNumbersObj instanceof List) {
                    return (List<String>) noticeNumbersObj;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract notice numbers from Step 1: {}", e.getMessage());
        }
        return null;
    }
}
