package com.ocmsintranet.cronservice.testing.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to represent the result of a test step
 * Used to track the status and details of each step in a test flow
 *
 * Aligned with OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Format matches StepResult from testing/main/StepResult.java
 */
public class TestStepResult {
    @JsonProperty("stepName")
    private String stepName;

    @JsonProperty("status")
    private String status; // "success", "error", "skipped"

    @JsonProperty("message")
    private String message;

    @JsonProperty("json")
    private Map<String, Object> json;

    /**
     * Constructor with step name and status
     */
    public TestStepResult(String stepName, String status) {
        this.stepName = stepName;
        this.status = status;
        this.message = "";
        this.json = new HashMap<>();
    }

    /**
     * Constructor with all fields
     */
    public TestStepResult(String stepName, String status, String message) {
        this.stepName = stepName;
        this.status = status;
        this.message = message;
        this.json = new HashMap<>();
    }

    /**
     * Constructor with all fields including JSON data
     */
    public TestStepResult(String stepName, String status, String message, Map<String, Object> json) {
        this.stepName = stepName;
        this.status = status;
        this.message = message;
        this.json = json != null ? json : new HashMap<>();
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getJson() {
        return json;
    }

    public void setJson(Map<String, Object> json) {
        this.json = json;
    }

    /**
     * Add data to the JSON response
     */
    public void addJsonData(String key, Object value) {
        this.json.put(key, value);
    }

    // Legacy support for existing code
    private List<String> details;

    @Deprecated
    public void addDetail(String detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
    }

    @Deprecated
    public List<String> getDetails() {
        return details;
    }

    @Deprecated
    public void setDetails(List<String> details) {
        this.details = details;
    }

    @Deprecated
    public String getTitle() {
        return stepName;
    }

    @Deprecated
    public void setTitle(String title) {
        this.stepName = title;
    }

    @Deprecated
    public Object getJsonData() {
        return json;
    }

    @Deprecated
    public void setJsonData(Object jsonData) {
        if (jsonData instanceof Map) {
            this.json = (Map<String, Object>) jsonData;
        }
    }
}
