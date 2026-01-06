package com.ocmsintranet.cronservice.testing.ocms20_reports.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for OCMS 20 report testing
 * Contains all test steps and their results
 */
@Data
@NoArgsConstructor
public class ReportTestResponse {

    @JsonProperty("testName")
    private String testName;

    @JsonProperty("steps")
    private List<TestStepResult> steps;

    @JsonProperty("summary")
    private String summary;

    public ReportTestResponse(String testName) {
        this.testName = testName;
        this.steps = new ArrayList<>();
    }

    public void addStep(TestStepResult step) {
        this.steps.add(step);
    }

    /**
     * Generate summary based on step results
     */
    public void generateSummary() {
        long successCount = steps.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedCount = steps.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        long skippedCount = steps.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count();

        if (failedCount > 0) {
            this.summary = String.format("❌ Test completed with %d failures, %d successes, %d skipped",
                failedCount, successCount, skippedCount);
        } else if (skippedCount > 0) {
            this.summary = String.format("⚠️ Test completed with %d skipped steps, %d successes",
                skippedCount, successCount);
        } else {
            this.summary = String.format("✅ All %d test steps passed successfully", successCount);
        }
    }
}
