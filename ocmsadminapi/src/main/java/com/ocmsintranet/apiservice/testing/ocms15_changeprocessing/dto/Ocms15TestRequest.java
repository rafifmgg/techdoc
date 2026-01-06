package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request DTO for OCMS 15 Change Processing Stage Integration Test
 *
 * This test follows the 5-step pattern:
 * Step 0: Setup Test Data (create notices via /staff-create-notice)
 * Step 1: Load Test Scenarios (from scenario.json)
 * Step 2: Trigger Change Processing Stage API
 * Step 3: Fetch Verification Data
 * Step 4: Verify Business Logic
 *
 * Cleanup: Manual via separate /ocms15-test-cleanup endpoint
 */
@Data
public class Ocms15TestRequest {

    @JsonProperty("details")
    private Boolean details;        // Show detailed verification results

    @JsonProperty("triggerApi")
    private Boolean triggerApi;     // Call the actual change-processing-stage API

    @JsonProperty("testOcmsFlow")
    private Boolean testOcmsFlow;   // Test OCMS manual flow (default: true)

    @JsonProperty("testPlusFlow")
    private Boolean testPlusFlow;   // Test PLUS integration flow (default: false)

    public Ocms15TestRequest() {
        this.details = true;
        this.triggerApi = true;
        this.testOcmsFlow = true;
        this.testPlusFlow = false;
    }

    // Getters return true if null (default behavior)
    public Boolean getDetails() {
        return details != null ? details : true;
    }

    public Boolean getTriggerApi() {
        return triggerApi != null ? triggerApi : true;
    }

    public Boolean getTestOcmsFlow() {
        return testOcmsFlow != null ? testOcmsFlow : true;
    }

    public Boolean getTestPlusFlow() {
        return testPlusFlow != null ? testPlusFlow : false;
    }
}
