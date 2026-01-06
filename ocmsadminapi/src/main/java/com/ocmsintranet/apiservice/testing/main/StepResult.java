package com.ocmsintranet.apiservice.testing.main;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO class for individual step execution result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult {

    @JsonProperty("stepName")
    private String stepName;

    @JsonProperty("status")
    private String status; // "success", "error", "skipped"

    @JsonProperty("message")
    private String message;

    @JsonProperty("json")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object json;

    // Compatibility fields/methods for code that uses different names
    private Boolean success;
    private Object details;
    private String description;
    private String duration;

    // Original constructors that code is using
    public StepResult(String stepName, String status, String message) {
        this.stepName = stepName;
        this.status = status;
        this.message = message;
        this.json = null;
    }

    public StepResult(String stepName, String status, String message, Object json) {
        this.stepName = stepName;
        this.status = status;
        this.message = message;
        this.json = json;
    }

    // Helper methods for boolean success
    public boolean isSuccess() {
        if (success != null) return success;
        return "success".equals(this.status);
    }

    public void setSuccess(boolean success) {
        this.success = success;
        this.status = success ? "success" : "error";
    }

    public Object getDetails() {
        return details != null ? details : json;
    }

    public void setDetails(Object details) {
        this.details = details;
        this.json = details;
    }

    public String getDescription() {
        return description != null ? description : message;
    }

    public void setDescription(String description) {
        this.description = description;
        this.message = description;
    }
}
