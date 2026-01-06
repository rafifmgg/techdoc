package com.ocmsintranet.apiservice.testing.reduction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO class for Reduction test request
 */
public class ReductionTestRequest {

    @JsonProperty("details")
    private Boolean details;

    @JsonProperty("triggerApi")
    private Boolean triggerApi;

    public ReductionTestRequest() {
        // Default values
        this.details = true;
        this.triggerApi = true;
    }

    public Boolean getDetails() {
        // Return true if null (default behavior)
        return details != null ? details : true;
    }

    public void setDetails(Boolean details) {
        this.details = details;
    }

    public Boolean getTriggerApi() {
        // Return true if null (default behavior)
        return triggerApi != null ? triggerApi : true;
    }

    public void setTriggerApi(Boolean triggerApi) {
        this.triggerApi = triggerApi;
    }
}
