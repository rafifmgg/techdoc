package com.ocmsintranet.cronservice.testing.suspension_revival.ts.datahive.single.service;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;
import com.ocmsintranet.cronservice.testing.suspension_revival.ts.datahive.single.helpers.TsDatahiveSingleStepExecutionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TsDatahiveSingleTestServiceImpl implements TsDatahiveSingleTestService {

    @Autowired
    private TsDatahiveSingleStepExecutionHelper stepExecutionHelper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/ts_datahive_single_scenarios.json";

    @Override
    public SuspensionRevivalTestResponse executeTsDatahiveSingleTest(SuspensionRevivalTestRequest request) {
        log.info("Executing TS-DataHive Single test: details={}, triggerApi={}",
                request.getDetails(), request.getTriggerApi());

        SuspensionRevivalTestResponse response = new SuspensionRevivalTestResponse();

        TestStepResult step1Result = stepExecutionHelper.executeStep1LoadScenarios(SCENARIO_FILE);
        response.addStep(step1Result);

        List<Map<String, Object>> scenarios = extractScenarios(step1Result);
        List<String> noticeNumbers = extractNoticeNumbers(step1Result);

        TestStepResult step2Result;
        if ("success".equals(step1Result.getStatus()) && scenarios != null) {
            step2Result = stepExecutionHelper.executeStep2TriggerProcess(request.getTriggerApi(), scenarios);
        } else {
            step2Result = stepExecutionHelper.createSkippedStep(
                    "Step 2: Trigger DataHive Single Download Process",
                    "Skipped due to Step 1 failure"
            );
        }
        response.addStep(step2Result);

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

        TestStepResult step4Result;
        if ("success".equals(step3Result.getStatus()) && scenarios != null) {
            step4Result = stepExecutionHelper.executeStep4Verify(scenarios, request.getDetails());
        } else {
            step4Result = stepExecutionHelper.createSkippedStep(
                    "Step 4: Verify Business Logic",
                    "Skipped due to Step 3 failure"
            );
        }
        response.addStep(step4Result);

        log.info("TS-DataHive Single test completed: {} steps executed", response.getSteps().size());
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
