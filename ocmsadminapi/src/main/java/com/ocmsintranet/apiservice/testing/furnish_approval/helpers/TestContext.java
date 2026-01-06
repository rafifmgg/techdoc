package com.ocmsintranet.apiservice.testing.furnish_approval.helpers;

import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestResponse;
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

    private String scenarioName;
    private List<FurnishApprovalTestResponse.TestStep> steps = new ArrayList<>();
    private List<FurnishApprovalTestResponse.VerificationResult> verifications = new ArrayList<>();
    private Map<String, Object> scenarioData = new HashMap<>();
    private Map<String, Object> runtimeData = new HashMap<>();
    private long startTimeMs;
    private boolean hasFatalError = false;
    private String fatalErrorMessage;
    private String txnNo;
    private String noticeNo;

    public void addStep(String stepName, String status, String message, long durationMs) {
        steps.add(FurnishApprovalTestResponse.TestStep.builder()
                .stepName(stepName)
                .status(status)
                .message(message)
                .durationMs(durationMs)
                .build());
    }

    public void addVerification(String checkName, boolean passed, String expected, String actual, String message) {
        verifications.add(FurnishApprovalTestResponse.VerificationResult.builder()
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

    public FurnishApprovalTestResponse buildResponse() {
        String overallResult;
        if (hasFatalError) {
            overallResult = "ERROR";
        } else {
            boolean allVerificationsPassed = verifications.stream().allMatch(FurnishApprovalTestResponse.VerificationResult::isPassed);
            overallResult = allVerificationsPassed ? "PASS" : "FAIL";
        }

        long executionTime = System.currentTimeMillis() - startTimeMs;

        return FurnishApprovalTestResponse.builder()
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
