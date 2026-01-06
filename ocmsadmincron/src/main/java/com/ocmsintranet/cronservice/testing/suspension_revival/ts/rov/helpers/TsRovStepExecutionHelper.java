package com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.helpers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services.LtaDownloadService;
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

/**
 * TS-ROV Step Execution Helper
 *
 * Implements Step 2 (Trigger LTA Download) for TS-ROV tests
 * Extends base helper for Step 1, 3, 4 functionality
 */
@Slf4j
@Component
public class TsRovStepExecutionHelper extends SuspensionStepExecutionHelper {

    @Autowired
    private LtaDownloadService ltaDownloadService;

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private SuspensionTestContext testContext;

    /**
     * Step 2: Trigger LTA download process
     *
     * @param triggerApi  Whether to actually trigger the API
     * @param scenarios   Scenarios to process
     * @return Step result
     */
    public TestStepResult executeStep2TriggerProcess(boolean triggerApi, List<Map<String, Object>> scenarios) {
        try {
            log.info("Step 2: Trigger LTA download process (triggerApi={})", triggerApi);

            if (!triggerApi) {
                Map<String, Object> jsonResponse = new HashMap<>();
                jsonResponse.put("triggered", false);
                jsonResponse.put("reason", "triggerApi is set to false (dry run)");

                return new TestStepResult(
                        "Step 2: Trigger LTA Download Process",
                        "success",
                        "Skipped triggering API (dry run mode)",
                        jsonResponse
                );
            }

            // Create test data for each scenario
            int setupCount = 0;
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String vehicleNo = (String) scenario.get("vehicleNo");
                String nric = (String) scenario.get("nric");

                // Create test VON
                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);

                // Create test LTA ROV file
                testDataHelper.createTestLtaFile(vehicleNo, "ROV");

                setupCount++;
            }

            log.info("Created {} test VON + LTA file records", setupCount);

            // Trigger LTA download service
            var jobResult = ltaDownloadService.executeLtaDownloadJob().get();

            // Store execution results in context
            Map<String, Object> executionResults = new HashMap<>();
            executionResults.put("job_success", jobResult.isSuccess());
            executionResults.put("job_message", jobResult.getMessage());
            executionResults.put("records_setup", setupCount);
            testContext.setExecutionResults(executionResults);

            // Build response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("triggered", true);
            jsonResponse.put("records_setup", setupCount);
            jsonResponse.put("job_success", jobResult.isSuccess());
            jsonResponse.put("job_message", jobResult.getMessage());

            String message = String.format(
                    "LTA download executed: %d records setup, job %s",
                    setupCount,
                    jobResult.isSuccess() ? "succeeded" : "failed"
            );

            log.info(message);

            return new TestStepResult(
                    "Step 2: Trigger LTA Download Process",
                    jobResult.isSuccess() ? "success" : "error",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 2: Trigger LTA Download Process",
                    "error",
                    "Step 2 failed: " + e.getMessage()
            );
        }
    }
}
