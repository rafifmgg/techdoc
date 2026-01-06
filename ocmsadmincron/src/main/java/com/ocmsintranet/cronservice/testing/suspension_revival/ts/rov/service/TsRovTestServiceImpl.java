package com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.service;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.helpers.TsRovStepExecutionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TS-ROV Test Service Implementation
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Orchestrates 4-step test execution:
 * - Step 1: Load scenarios from ts_rov_scenarios.json
 * - Step 2: Trigger LTA download process
 * - Step 3: Fetch verification data (suspensions, VON, LTA files)
 * - Step 4: Verify business logic
 */
@Slf4j
@Service
public class TsRovTestServiceImpl implements TsRovTestService {

    @Autowired
    private TsRovStepExecutionHelper stepExecutionHelper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/ts_rov_scenarios.json";

    @Override
    public SuspensionRevivalTestResponse executeTsRovTest(SuspensionRevivalTestRequest request) {
        log.info("Executing TS-ROV test: details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = new SuspensionRevivalTestResponse();

        // Execute Step 1: Load scenarios
        TestStepResult step1Result = stepExecutionHelper.executeStep1LoadScenarios(SCENARIO_FILE);
        response.addStep(step1Result);

        // Extract data from Step 1
        List<Map<String, Object>> scenarios = extractScenarios(step1Result);
        List<String> noticeNumbers = extractNoticeNumbers(step1Result);

        // Execute Step 2: Trigger LTA download (conditional on Step 1 success)
        TestStepResult step2Result;
        if ("success".equals(step1Result.getStatus()) && scenarios != null) {
            step2Result = stepExecutionHelper.executeStep2TriggerProcess(
                    request.getTriggerApi(),
                    scenarios
            );
        } else {
            step2Result = stepExecutionHelper.createSkippedStep(
                    "Step 2: Trigger LTA Download Process",
                    "Skipped due to Step 1 failure"
            );
        }
        response.addStep(step2Result);

        // Execute Step 3: Fetch verification data (conditional on Step 2 success)
        TestStepResult step3Result;
        if ("success".equals(step2Result.getStatus()) && noticeNumbers != null) {
            step3Result = stepExecutionHelper.executeStep3FetchData(noticeNumbers);
        } else {
            step3Result = stepExecutionHelper.createSkippedStep(
                    "Step 3: Fetch Verification Data",
                    "Skipped due to Step 2 failure"
            );
        }
        response.addStep(step3Result);

        // Execute Step 4: Verify business logic (conditional on Step 3 success)
        TestStepResult step4Result;
        if ("success".equals(step3Result.getStatus()) && scenarios != null) {
            step4Result = stepExecutionHelper.executeStep4Verify(
                    scenarios,
                    request.getDetails()
            );
        } else {
            step4Result = stepExecutionHelper.createSkippedStep(
                    "Step 4: Verify Business Logic",
                    "Skipped due to Step 3 failure"
            );
        }
        response.addStep(step4Result);

        log.info("TS-ROV test completed: {} steps executed", response.getSteps().size());

        return response;
    }

    /**
     * Extract scenarios from Step 1 result
     */
    private List<Map<String, Object>> extractScenarios(TestStepResult step1Result) {
        try {
            Map<String, Object> json = step1Result.getJson();
            if (json != null && json.containsKey("scenarios")) {
                return (List<Map<String, Object>>) json.get("scenarios");
            }
        } catch (Exception e) {
            log.error("Error extracting scenarios: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract notice numbers from Step 1 result
     */
    private List<String> extractNoticeNumbers(TestStepResult step1Result) {
        try {
            Map<String, Object> json = step1Result.getJson();
            if (json != null && json.containsKey("noticeNumbers")) {
                return (List<String>) json.get("noticeNumbers");
            }
        } catch (Exception e) {
            log.error("Error extracting notice numbers: {}", e.getMessage());
        }
        return null;
    }
}
