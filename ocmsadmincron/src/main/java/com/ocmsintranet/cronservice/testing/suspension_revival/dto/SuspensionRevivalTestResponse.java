package com.ocmsintranet.cronservice.testing.suspension_revival.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Suspension/Revival Test Framework
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Contains results from all 4 test steps:
 * - Step 1: Load Test Scenarios
 * - Step 2: Trigger Process/API
 * - Step 3: Fetch Verification Data
 * - Step 4: Verify Business Logic
 */
public class SuspensionRevivalTestResponse {

    @JsonProperty("steps")
    private List<TestStepResult> steps;

    /**
     * Default constructor - initializes empty steps list
     */
    public SuspensionRevivalTestResponse() {
        this.steps = new ArrayList<>();
    }

    /**
     * Add a step result to the response
     *
     * @param step Step result to add
     */
    public void addStep(TestStepResult step) {
        this.steps.add(step);
    }

    /**
     * Get all step results
     *
     * @return List of step results
     */
    public List<TestStepResult> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStepResult> steps) {
        this.steps = steps;
    }
}
