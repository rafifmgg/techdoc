package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocmsintranet.apiservice.testing.main.StepResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for OCMS 15 Change Processing Stage Integration Test
 *
 * Contains results for all 4 steps:
 * Step 1: Load Test Scenarios
 * Step 2: Trigger Change Processing Stage API
 * Step 3: Fetch Verification Data
 * Step 4: Verify Business Logic
 *
 * Note: Setup and cleanup are handled separately via:
 * - POST /v1/ocms15-test-setup
 * - DELETE /v1/ocms15-test-cleanup
 */
@Data
public class Ocms15TestResponse {

    @JsonProperty("steps")
    private List<StepResult> steps;

    public Ocms15TestResponse() {
        this.steps = new ArrayList<>();
    }

    public void addStep(StepResult step) {
        this.steps.add(step);
    }
}
