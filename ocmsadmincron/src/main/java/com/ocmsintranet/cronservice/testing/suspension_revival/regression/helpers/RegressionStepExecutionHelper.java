package com.ocmsintranet.cronservice.testing.suspension_revival.regression.helpers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.SuspensionStepExecutionHelper;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.SuspensionTestContext;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RegressionStepExecutionHelper extends SuspensionStepExecutionHelper {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private SuspensionTestContext testContext;

    public TestStepResult executeStep2TriggerProcess(boolean triggerApi, List<Map<String, Object>> scenarios) {
        try {
            log.info("Step 2: Setup Regression Test Data (triggerApi={})", triggerApi);

            if (!triggerApi) {
                return new TestStepResult(
                        "Step 2: Setup Regression Test Data",
                        "success",
                        "Skipped setup (dry run mode)",
                        Map.of("triggered", false, "reason", "triggerApi is set to false")
                );
            }

            int setupCount = 0;
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String vehicleNo = (String) scenario.get("vehicleNo");
                String nric = (String) scenario.get("nric");
                String suspensionType = (String) scenario.get("suspensionType");
                String reasonOfSuspension = (String) scenario.get("reasonOfSuspension");
                String testCase = (String) scenario.get("testCase");

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);

                if ("EXISTING_SUSPENSION".equals(testCase)) {
                    testDataHelper.createTestSuspension(noticeNo, vehicleNo, nric,
                            suspensionType, reasonOfSuspension, (Integer) null);
                }

                setupCount++;
            }

            Map<String, Object> executionResults = new HashMap<>();
            executionResults.put("setup_success", true);
            executionResults.put("records_setup", setupCount);
            testContext.setExecutionResults(executionResults);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("triggered", true);
            jsonResponse.put("records_setup", setupCount);
            jsonResponse.put("setup_success", true);

            String message = String.format(
                    "Regression test data setup completed: %d records created",
                    setupCount
            );

            return new TestStepResult(
                    "Step 2: Setup Regression Test Data",
                    "success",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 2: Setup Regression Test Data",
                    "error",
                    "Step 2 failed: " + e.getMessage()
            );
        }
    }
}
