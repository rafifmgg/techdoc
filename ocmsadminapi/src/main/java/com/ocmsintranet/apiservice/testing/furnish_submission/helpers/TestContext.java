package com.ocmsintranet.apiservice.testing.furnish_submission.helpers;

import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test execution context that holds test state and results
 */
@Data
public class TestContext {

    /**
     * Scenario name being tested
     */
    private String scenarioName;

    /**
     * Test execution steps
     */
    private List<FurnishSubmissionTestResponse.TestStep> steps = new ArrayList<>();

    /**
     * Verification results
     */
    private List<FurnishSubmissionTestResponse.VerificationResult> verifications = new ArrayList<>();

    /**
     * Test data from scenario.json
     */
    private Map<String, Object> scenarioData = new HashMap<>();

    /**
     * Runtime data generated during test execution
     */
    private Map<String, Object> runtimeData = new HashMap<>();

    /**
     * Test start time
     */
    private long startTimeMs;

    /**
     * Whether test encountered a fatal error
     */
    private boolean hasFatalError = false;

    /**
     * Fatal error message
     */
    private String fatalErrorMessage;

    /**
     * Transaction number created during test
     */
    private String txnNo;

    /**
     * Notice number being tested
     */
    private String noticeNo;

    /**
     * Add a test step with status
     */
    public void addStep(String stepName, String status, String message, long durationMs) {
        steps.add(FurnishSubmissionTestResponse.TestStep.builder()
                .stepName(stepName)
                .status(status)
                .message(message)
                .durationMs(durationMs)
                .build());
    }

    /**
     * Add a verification result
     */
    public void addVerification(String checkName, boolean passed, String expected, String actual, String message) {
        verifications.add(FurnishSubmissionTestResponse.VerificationResult.builder()
                .checkName(checkName)
                .passed(passed)
                .expected(expected)
                .actual(actual)
                .message(message)
                .build());
    }

    /**
     * Mark test as having fatal error
     */
    public void setFatalError(String errorMessage) {
        this.hasFatalError = true;
        this.fatalErrorMessage = errorMessage;
    }

    /**
     * Check if test can continue (no fatal error)
     */
    public boolean canContinue() {
        return !hasFatalError;
    }

    /**
     * Build final response
     */
    public FurnishSubmissionTestResponse buildResponse() {
        // Determine overall result
        String overallResult;
        if (hasFatalError) {
            overallResult = "ERROR";
        } else {
            boolean allVerificationsPassed = verifications.stream().allMatch(FurnishSubmissionTestResponse.VerificationResult::isPassed);
            overallResult = allVerificationsPassed ? "PASS" : "FAIL";
        }

        long executionTime = System.currentTimeMillis() - startTimeMs;

        return FurnishSubmissionTestResponse.builder()
                .scenarioName(scenarioName)
                .overallResult(overallResult)
                .steps(steps)
                .verifications(verifications)
                .errorMessage(fatalErrorMessage)
                .executionTimeMs(executionTime)
                .txnNo(txnNo)
                .noticeNo(noticeNo)
                .build();
    }
}
