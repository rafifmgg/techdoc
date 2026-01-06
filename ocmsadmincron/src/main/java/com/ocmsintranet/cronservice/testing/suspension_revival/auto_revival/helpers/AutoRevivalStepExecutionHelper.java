package com.ocmsintranet.cronservice.testing.suspension_revival.auto_revival.helpers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.SuspensionStepExecutionHelper;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.SuspensionTestContext;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AutoRevivalStepExecutionHelper extends SuspensionStepExecutionHelper {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private SuspensionTestContext testContext;

    public TestStepResult executeStep2TriggerProcess(boolean triggerApi, List<Map<String, Object>> scenarios) {
        try {
            log.info("Step 2: Trigger Auto-Revival process (triggerApi={})", triggerApi);

            if (!triggerApi) {
                return new TestStepResult(
                        "Step 2: Trigger Auto-Revival Process",
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
                String suspensionType = (String) scenario.get("suspensionType");
                String reasonOfSuspension = (String) scenario.get("reasonOfSuspension");
                String revivalDateOffset = (String) scenario.get("revivalDateOffset");

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);

                LocalDate revivalDate = calculateRevivalDate(revivalDateOffset);
                testDataHelper.createTestSuspension(noticeNo, vehicleNo, nric,
                        suspensionType, reasonOfSuspension, revivalDate);
                setupCount++;
            }

            Map<String, Object> executionResults = new HashMap<>();
            executionResults.put("records_setup", setupCount);
            testContext.setExecutionResults(executionResults);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("triggered", true);
            jsonResponse.put("records_setup", setupCount);

            String message = String.format("Created %d test suspensions for auto-revival", setupCount);

            return new TestStepResult(
                    "Step 2: Create Test Suspensions for Auto-Revival",
                    "success",
                    message,
                    jsonResponse
            );

        } catch (Exception e) {
            log.error("Step 2 failed: {}", e.getMessage(), e);
            return new TestStepResult(
                    "Step 2: Trigger Auto-Revival Process",
                    "error",
                    "Step 2 failed: " + e.getMessage()
            );
        }
    }

    private LocalDate calculateRevivalDate(String offset) {
        if (offset == null || offset.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        switch (offset) {
            case "today":
                return today;
            case "tomorrow":
                return today.plusDays(1);
            case "yesterday":
                return today.minusDays(1);
            case "next_week":
                return today.plusDays(7);
            case "last_week":
                return today.minusDays(7);
            default:
                try {
                    int days = Integer.parseInt(offset);
                    return today.plusDays(days);
                } catch (NumberFormatException e) {
                    log.warn("Invalid revival date offset: {}, using null", offset);
                    return null;
                }
        }
    }
}
