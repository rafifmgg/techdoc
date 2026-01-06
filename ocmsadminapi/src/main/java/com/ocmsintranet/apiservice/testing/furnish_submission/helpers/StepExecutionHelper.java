package com.ocmsintranet.apiservice.testing.furnish_submission.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper for executing test steps with consistent error handling
 * Never-fail design: All exceptions caught, steps marked as skipped if previous fails
 */
@Component
@Slf4j
public class StepExecutionHelper {

    /**
     * Execute a test step with automatic error handling
     *
     * @param context Test context
     * @param stepName Name of the step
     * @param stepLogic Logic to execute
     */
    public void executeStep(TestContext context, String stepName, StepLogic stepLogic) {
        // Skip if previous step had fatal error
        if (!context.canContinue()) {
            context.addStep(stepName, "SKIPPED", "Previous step failed", 0L);
            return;
        }

        long stepStart = System.currentTimeMillis();
        try {
            log.info("Executing step: {}", stepName);
            stepLogic.execute();
            long duration = System.currentTimeMillis() - stepStart;
            context.addStep(stepName, "SUCCESS", "Step completed successfully", duration);
            log.info("Step completed: {} ({}ms)", stepName, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - stepStart;
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            context.addStep(stepName, "FAILED", "Error: " + errorMsg, duration);
            context.setFatalError("Step failed: " + stepName + " - " + errorMsg);
            log.error("Step failed: {}", stepName, e);
        }
    }

    /**
     * Execute a verification step (does not stop test execution on failure)
     *
     * @param context Test context
     * @param stepName Name of the step
     * @param verificationLogic Verification logic to execute
     */
    public void executeVerification(TestContext context, String stepName, StepLogic verificationLogic) {
        // Skip if previous setup step had fatal error
        if (!context.canContinue()) {
            context.addStep(stepName, "SKIPPED", "Previous step failed", 0L);
            return;
        }

        long stepStart = System.currentTimeMillis();
        try {
            log.info("Executing verification: {}", stepName);
            verificationLogic.execute();
            long duration = System.currentTimeMillis() - stepStart;
            context.addStep(stepName, "SUCCESS", "Verification completed", duration);
            log.info("Verification completed: {} ({}ms)", stepName, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - stepStart;
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            context.addStep(stepName, "FAILED", "Verification error: " + errorMsg, duration);
            // Note: Does NOT set fatal error - verification failures are tracked in verifications list
            log.warn("Verification failed: {}", stepName, e);
        }
    }

    /**
     * Functional interface for step logic
     */
    @FunctionalInterface
    public interface StepLogic {
        void execute() throws Exception;
    }
}
