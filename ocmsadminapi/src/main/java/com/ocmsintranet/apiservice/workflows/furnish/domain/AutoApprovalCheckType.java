package com.ocmsintranet.apiservice.workflows.furnish.domain;

/**
 * Enum representing the types of auto-approval validation checks.
 * Based on OCMS 41 User Story 41.5 and requirements.
 */
public enum AutoApprovalCheckType {
    /**
     * Check if notice is permanently suspended
     */
    NOTICE_PERMANENTLY_SUSPENDED("Notice is permanently suspended"),

    /**
     * Check if furnished ID is already a current offender in OCMS
     */
    FURNISHED_ID_CURRENT_OFFENDER("Furnished ID already current offender in OCMS"),

    /**
     * Check if furnished ID already exists in Hirer or Driver Details
     * (regardless of current offender status)
     */
    FURNISHED_ID_IN_HIRER_DRIVER("Furnished ID already in Hirer or Driver Details (regardless of current offender status)"),

    /**
     * Check if hirer/driver particulars already present (any ID)
     */
    HIRER_DRIVER_PARTICULARS_EXISTS("Hirer/Driver's particulars already present (any ID)"),

    /**
     * Check if owner/hirer (furnisher/submitter) is no longer current offender
     */
    OWNER_NOT_CURRENT_OFFENDER("Owner/Hirer (furnisher) no longer current offender");

    private final String description;

    AutoApprovalCheckType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
