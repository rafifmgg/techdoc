package com.ocmsintranet.apiservice.testing.furnish_manual.helpers;

import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class StepExecutionHelper {

    /**
     * Execute a test step with automatic exception handling and logging.
     * Never fails - catches all exceptions and marks step as SKIPPED.
     *
     * @param response The test response to add step results to
     * @param stepName Name of the step being executed
     * @param stepLogic The logic to execute
     * @param <T> Return type of the step logic
     * @return Result of the step logic, or null if exception occurred
     */
    public <T> T executeStep(ManualFurnishTestResponse response, String stepName, Supplier<T> stepLogic) {
        long stepStart = System.currentTimeMillis();
        log.info(">>> Step: {}", stepName);

        try {
            T result = stepLogic.get();
            long duration = System.currentTimeMillis() - stepStart;

            ManualFurnishTestResponse.TestStep step = ManualFurnishTestResponse.TestStep.builder()
                    .stepName(stepName)
                    .status("SUCCESS")
                    .message("Completed successfully")
                    .durationMs(duration)
                    .build();

            response.getSteps().add(step);
            log.info("<<< Step completed: {} in {}ms", stepName, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - stepStart;
            log.error("Step failed: {} - {}", stepName, e.getMessage(), e);

            ManualFurnishTestResponse.TestStep step = ManualFurnishTestResponse.TestStep.builder()
                    .stepName(stepName)
                    .status("SKIPPED")
                    .message("Failed: " + e.getMessage())
                    .durationMs(duration)
                    .build();

            response.getSteps().add(step);
            return null;
        }
    }
}
