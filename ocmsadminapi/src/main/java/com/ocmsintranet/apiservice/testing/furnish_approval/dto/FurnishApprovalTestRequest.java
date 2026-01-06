package com.ocmsintranet.apiservice.testing.furnish_approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Furnish Approval Integration Test
 * Used to trigger test scenarios for officer approval workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApprovalTestRequest {

    /**
     * Scenario name from scenario.json
     * Examples: "approval_with_email_sms", "approval_blocked_by_ps"
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
