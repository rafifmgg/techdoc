package com.ocmsintranet.apiservice.workflows.furnish.domain;

/**
 * Enum representing the status of a furnish application.
 * Based on OCMS 41 requirements (User Story 41.9, 41.37, 41.39).
 */
public enum FurnishStatus {
    /**
     * P - Pending: First time submission, pending officer review (suspended with TS-PDP)
     */
    PENDING("P"),

    /**
     * Resubmission - Had an earlier submission which can be pending, approved or rejected.
     * Every submission creates a new record (no edit).
     */
    RESUBMISSION("Resubmission"),

    /**
     * A - Approved: Officer approved the furnish submission
     */
    APPROVED("A"),

    /**
     * R - Rejected: Officer rejected the furnish submission
     */
    REJECTED("R");

    private final String code;

    FurnishStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FurnishStatus fromCode(String code) {
        for (FurnishStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown furnish status code: " + code);
    }
}
