package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing;

import com.ocmsintranet.apiservice.testing.main.StepResult;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestRequest;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestResponse;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers.StepExecutionHelper;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers.TestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * OCMS 15 Change Processing Stage Integration Test Service Implementation
 *
 * Implements the 4-step test pattern following TESTING_FRAMEWORK_GUIDE.md
 *
 * IMPORTANT: Call POST /v1/ocms15-test-setup BEFORE running this test
 */
@Slf4j
@Service
public class Ocms15TestServiceImpl implements Ocms15TestService {

    @Autowired
    private StepExecutionHelper stepExecutionHelper;

    @Autowired
    private TestContext testContext;

    /**
     * Execute OCMS 15 integration test (4-step pattern)
     *
     * IMPORTANT: This assumes test data has been set up via /ocms15-test-setup
     *
     * @param request Test request configuration
     * @return Test response with step results
     */
    @Override
    public Ocms15TestResponse executeTest(Ocms15TestRequest request) {
        Ocms15TestResponse response = new Ocms15TestResponse();

        try {
            // Clear previous test context
            testContext.clear();

            log.info("Starting OCMS 15 integration test - OCMS: {}, PLUS: {}, TriggerAPI: {}, Details: {}",
                request.getTestOcmsFlow(), request.getTestPlusFlow(), request.getTriggerApi(), request.getDetails());

            // ========== Step 1: Load Test Scenarios ==========
            StepResult step1 = stepExecutionHelper.executeStep1_LoadScenarios(
                request.getTestOcmsFlow(),
                request.getTestPlusFlow()
            );
            response.addStep(step1);

            if (!step1.isSuccess()) {
                log.error("Step 1 failed, aborting test");
                return response;
            }

            // Extract scenarios from step1 details
            @SuppressWarnings("unchecked")
            Map<String, Object> step1Details = (Map<String, Object>) step1.getDetails();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scenarios = (List<Map<String, Object>>) step1Details.get("scenarios");

            if (scenarios == null || scenarios.isEmpty()) {
                log.warn("No scenarios to test");
                return response;
            }

            // Extract notice numbers for Step 3 data fetching
            List<String> noticeNumbers = scenarios.stream()
                .map(s -> (String) s.get("noticeNo"))
                .toList();
            testContext.setCreatedNoticeNumbers(noticeNumbers);

            // ========== Step 2: Trigger Change Processing Stage API ==========
            StepResult step2 = stepExecutionHelper.executeStep2_TriggerApi(scenarios, request.getTriggerApi());
            response.addStep(step2);

            if (!step2.isSuccess() && request.getTriggerApi()) {
                log.warn("Step 2 had failures, but continuing to verification");
            }

            // ========== Step 3: Fetch Verification Data ==========
            StepResult step3 = stepExecutionHelper.executeStep3_FetchData();
            response.addStep(step3);

            if (!step3.isSuccess()) {
                log.error("Step 3 failed, aborting test");
                return response;
            }

            // ========== Step 4: Verify Business Logic ==========
            StepResult step4 = stepExecutionHelper.executeStep4_VerifyLogic(scenarios, request.getDetails());
            response.addStep(step4);

            log.info("OCMS 15 integration test completed - Step 4: {}", step4.getMessage());

        } catch (Exception e) {
            log.error("Error executing OCMS 15 test: {}", e.getMessage(), e);

            StepResult errorStep = new StepResult();
            errorStep.setStepName("Error");
            errorStep.setSuccess(false);
            errorStep.setMessage("Test execution failed: " + e.getMessage());
            errorStep.setDetails(Map.of("error", e.getMessage()));
            response.addStep(errorStep);
        }

        return response;
    }
}
