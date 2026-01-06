package com.ocmsintranet.apiservice.testing.toppan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO class for Toppan Upload test request
 */
public class ToppanUploadRequest {

    @JsonProperty("preload")
    private Boolean preload;

    @JsonProperty("details")
    private Boolean details;

    @JsonProperty("triggerCron")
    private Boolean triggerCron;

    public ToppanUploadRequest() {
        // Default values set in code, not in properties
        this.preload = true;
        this.details = true;
        this.triggerCron = true;
    }

    public Boolean getPreload() {
        // Return true if null (default behavior)
        return preload != null ? preload : true;
    }

    public void setPreload(Boolean preload) {
        this.preload = preload;
    }

    public Boolean getDetails() {
        // Return true if null (default behavior)
        return details != null ? details : true;
    }

    public void setDetails(Boolean details) {
        this.details = details;
    }

    public Boolean getTriggerCron() {
        // Return true if null (default behavior)
        return triggerCron != null ? triggerCron : true;
    }

    public void setTriggerCron(Boolean triggerCron) {
        this.triggerCron = triggerCron;
    }
}
