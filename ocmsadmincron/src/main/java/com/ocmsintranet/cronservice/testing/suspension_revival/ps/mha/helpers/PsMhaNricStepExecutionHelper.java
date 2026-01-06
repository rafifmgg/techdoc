package com.ocmsintranet.cronservice.testing.suspension_revival.ps.mha.helpers;

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
public class PsMhaNricStepExecutionHelper extends SuspensionStepExecutionHelper {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private SuspensionTestContext testContext;

    public TestStepResult executeStep2TriggerProcess(boolean triggerApi, List<Map<String, Object>> scenarios) {
        try {
            log.info("Step 2: Trigger MHA NRIC download process (triggerApi={})", triggerApi);

            if (!triggerApi) {
                return new TestStepResult(
                        "Step 2: Trigger MHA NRIC Download Process",
                        "success",
                        "Skipped triggering API (dry run mode)",
                        Map.of("triggered", false, "reason", "triggerApi is set to false")
                );
            }

            int setupCount = 0;
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String vehicleNo = (String) scenario.get("vehicleNo");
                String nric = (String) scenario.get("nric");
                String reasonOfSuspension = (String) scenario.get("reasonOfSuspension");

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);
                testDataHelper.createTestMhaFile(nric, reasonOfSuspension);
                setupCount++;
            }

            Map<String, Object> executionResults = new HashMap<>();
            executionResults.put("records_setup", setupCount);
            testContext.setExecutionResults(executionResults);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("triggered", true);
            jsonResponse.put("records_setup", setupCount);

            String message = String.format("Created %d test MHA file records", setupCount);

            return new TestStepResult(
                    "Step 2: Create Test MHA File Records",
                    "success",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 2: Trigger MHA NRIC Download Process",
                    "error",
                    "Step 2 failed: " + e.getMessage()
            );
        }
    }
}
