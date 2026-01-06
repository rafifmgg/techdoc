package com.ocmsintranet.apiservice.testing.furnish_manual.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualFurnishTestResponse {
    private String scenarioName;
    private String overallResult;
    @Builder.Default
    private List<TestStep> steps = new ArrayList<>();
    @Builder.Default
    private List<VerificationResult> verifications = new ArrayList<>();
    private String errorMessage;
    private Long executionTimeMs;
    private String txnNo;
    private String noticeNo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {
        private String stepName;
        private String status;
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
