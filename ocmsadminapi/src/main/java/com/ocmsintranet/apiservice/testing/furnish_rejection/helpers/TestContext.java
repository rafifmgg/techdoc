package com.ocmsintranet.apiservice.testing.furnish_rejection.helpers;

import com.ocmsintranet.apiservice.testing.furnish_rejection.dto.FurnishRejectionTestResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test execution context for Furnish Rejection Test
 */
@Data
public class TestContext {

    private String scenarioName;
    private List<FurnishRejectionTestResponse.TestStep> steps = new ArrayList<>();
    private List<FurnishRejectionTestResponse.VerificationResult> verifications = new ArrayList<>();
    private Map<String, Object> scenarioData = new HashMap<>();
    private Map<String, Object> runtimeData = new HashMap<>();
    private long startTimeMs;
    private boolean hasFatalError = false;
    private String fatalErrorMessage;
    private String txnNo;
    private String noticeNo;

    public void addStep(String stepName, String status, String message, long durationMs) {
        steps.add(FurnishRejectionTestResponse.TestStep.builder()
                .stepName(stepName)
                .status(status)
                .message(message)
                .durationMs(durationMs)
                .build());
    }

    public void addVerification(String checkName, boolean passed, String expected, String actual, String message) {
        verifications.add(FurnishRejectionTestResponse.VerificationResult.builder()
                .checkName(checkName)
                .passed(passed)
                .expected(expected)
                .actual(actual)
                .message(message)
                .build());
    }

    public void setFatalError(String errorMessage) {
        this.hasFatalError = true;
        this.fatalErrorMessage = errorMessage;
    }

    public boolean canContinue() {
        return !hasFatalError;
    }

    public FurnishRejectionTestResponse buildResponse() {
        String overallResult;
        if (hasFatalError) {
            overallResult = "ERROR";
        } else {
            boolean allVerificationsPassed = verifications.stream()
                    .allMatch(FurnishRejectionTestResponse.VerificationResult::isPassed);
            overallResult = allVerificationsPassed ? "PASS" : "FAIL";
        }

        long executionTime = System.currentTimeMillis() - startTimeMs;

        return FurnishRejectionTestResponse.builder()
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
