package com.ocmsintranet.cronservice.testing.suspension_revival.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for Suspension/Revival Test Framework
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * This DTO controls test execution behavior:
 * - details: Show detailed verification results
 * - triggerApi: Actually trigger the API/cron (false = dry run)
 */
public class SuspensionRevivalTestRequest {

    @JsonProperty("details")
    private Boolean details;        // Show detailed verification results

    @JsonProperty("triggerApi")
    private Boolean triggerApi;     // Call the actual API/process

    /**
     * Default constructor - defaults both flags to true
     */
    public SuspensionRevivalTestRequest() {
        this.details = true;
        this.triggerApi = true;
    }

    /**
     * Get details flag - defaults to true if null
     *
     * @return true to show detailed verification results
     */
    public Boolean getDetails() {
        return details != null ? details : true;
    }

    public void setDetails(Boolean details) {
        this.details = details;
    }

    /**
     * Get triggerApi flag - defaults to true if null
     *
     * @return true to actually trigger the API/cron
     */
    public Boolean getTriggerApi() {
        return triggerApi != null ? triggerApi : true;
    }

    public void setTriggerApi(Boolean triggerApi) {
        this.triggerApi = triggerApi;
    }
}
