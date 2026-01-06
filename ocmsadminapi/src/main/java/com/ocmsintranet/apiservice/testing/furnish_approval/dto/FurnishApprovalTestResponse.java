package com.ocmsintranet.apiservice.testing.furnish_approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Furnish Approval Integration Test
 * Returns test execution results with step-by-step verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApprovalTestResponse {

    /**
     * Scenario name that was tested
     */
    private String scenarioName;

    /**
     * Overall test result: "PASS", "FAIL", "ERROR"
     */
    private String overallResult;

    /**
     * Test execution steps with results
     */
    @Builder.Default
    private List<TestStep> steps = new ArrayList<>();

    /**
     * Verification results
     */
    @Builder.Default
    private List<VerificationResult> verifications = new ArrayList<>();

    /**
     * Error message if test failed
     */
    private String errorMessage;

    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;

    /**
     * Transaction number tested
     */
    private String txnNo;

    /**
     * Notice number tested
     */
    private String noticeNo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {
        private String stepName;
        private String status; // "SUCCESS", "FAILED", "SKIPPED"
        private String message;
        private Long durationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationResult {
        private String checkName;
        private boolean passed;
        private String expected;
        private String actual;
        private String message;
    }
}
