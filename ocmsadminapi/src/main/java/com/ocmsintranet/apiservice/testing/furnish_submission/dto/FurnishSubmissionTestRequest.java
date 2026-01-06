package com.ocmsintranet.apiservice.testing.furnish_submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Furnish Submission Integration Test
 * Used to trigger test scenarios for eService submission workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishSubmissionTestRequest {

    /**
     * Scenario name from scenario.json
     * Examples: "auto_approval_success", "manual_review_missing_docs"
     */
    private String scenarioName;

    /**
     * Whether to setup test data before running the test
     * Default: true
     */
    @Builder.Default
    private boolean setupData = true;

    /**
     * Whether to cleanup test data after test completion
     * Default: false (keep data for manual verification)
     */
    @Builder.Default
    private boolean cleanupAfterTest = false;
}
