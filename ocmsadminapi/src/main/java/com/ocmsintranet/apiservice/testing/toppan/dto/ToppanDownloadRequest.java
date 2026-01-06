package com.ocmsintranet.apiservice.testing.toppan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO class for Toppan Download test request
 */
public class ToppanDownloadRequest {

    @JsonProperty("triggerCron")
    private Boolean triggerCron;

    @JsonProperty("details")
    private Boolean details;

    public ToppanDownloadRequest() {
        // Default values set in code, not in properties
        this.triggerCron = true;
        this.details = true;
    }

    public Boolean getTriggerCron() {
        // Return true if null (default behavior)
        return triggerCron != null ? triggerCron : true;
    }

    public void setTriggerCron(Boolean triggerCron) {
        this.triggerCron = triggerCron;
    }

    public Boolean getDetails() {
        // Return true if null (default behavior)
        return details != null ? details : true;
    }

    public void setDetails(Boolean details) {
        this.details = details;
    }
}
