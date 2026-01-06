package com.ocmsintranet.cronservice.testing.suspension_revival.config.service;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.suspension_revival.config.helpers.ConfigStepExecutionHelper;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConfigTestServiceImpl implements ConfigTestService {

    @Autowired
    private ConfigStepExecutionHelper stepExecutionHelper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/config_scenarios.json";

    @Override
    public SuspensionRevivalTestResponse executeConfigTest(SuspensionRevivalTestRequest request) {
        log.info("Executing Configuration test: details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = new SuspensionRevivalTestResponse();

        TestStepResult step1Result = stepExecutionHelper.executeStep1LoadScenarios(SCENARIO_FILE);
        response.addStep(step1Result);

        List<Map<String, Object>> scenarios = extractScenarios(step1Result);

        TestStepResult step2Result;
        if ("success".equals(step1Result.getStatus()) && scenarios != null) {
            step2Result = stepExecutionHelper.executeStep2TriggerProcess(request.getTriggerApi(), scenarios);
        } else {
            step2Result = stepExecutionHelper.createSkippedStep(
                    "Step 2: Validate Configuration",
                    "Skipped due to Step 1 failure"
            );
        }
        response.addStep(step2Result);

        TestStepResult step3Result = stepExecutionHelper.createSkippedStep(
                "Step 3: Fetch Verification Data",
                "Not applicable for configuration validation"
        );
        response.addStep(step3Result);

        TestStepResult step4Result = stepExecutionHelper.createSkippedStep(
                "Step 4: Verify Business Logic",
                "Validation performed in Step 2"
        );
        response.addStep(step4Result);

        log.info("Configuration test completed: {} steps executed", response.getSteps().size());
        return response;
    }

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
}
