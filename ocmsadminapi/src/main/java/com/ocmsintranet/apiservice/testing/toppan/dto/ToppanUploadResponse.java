package com.ocmsintranet.apiservice.testing.toppan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocmsintranet.apiservice.testing.main.StepResult;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO class for Toppan Upload test response
 * Contains list of step execution results
 */
public class ToppanUploadResponse {

    @JsonProperty("steps")
    private List<StepResult> steps;

    public ToppanUploadResponse() {
        this.steps = new ArrayList<>();
    }

    public List<StepResult> getSteps() {
        return steps;
    }

    public void setSteps(List<StepResult> steps) {
        this.steps = steps;
    }

    public void addStep(StepResult step) {
        this.steps.add(step);
    }
}
