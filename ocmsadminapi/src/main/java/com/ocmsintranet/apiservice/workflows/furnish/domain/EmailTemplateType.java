package com.ocmsintranet.apiservice.workflows.furnish.domain;

/**
 * Enum representing email template types for furnish workflow.
 * Based on OCMS 41 email templates.
 */
public enum EmailTemplateType {
    /**
     * Template 1: Email to Driver Furnished (approval)
     */
    DRIVER_FURNISHED("DRIVER_FURNISHED"),

    /**
     * Template 3: Email to Hirer Furnished (approval)
     */
    HIRER_FURNISHED("HIRER_FURNISHED"),

    /**
     * Template 4: Email to Owner - Rejected (Documents Required)
     */
    REJECTED_DOCS_REQUIRED("REJECTED_DOCS_REQUIRED"),

    /**
     * Template 5: Email to Owner - Multiple Hirers Furnished
     */
    REJECTED_MULTIPLE_HIRERS("REJECTED_MULTIPLE_HIRERS"),

    /**
     * Template 6: Email to Owner - Discrepancy in Rental Agreement
     */
    REJECTED_RENTAL_DISCREPANCY("REJECTED_RENTAL_DISCREPANCY"),

    /**
     * Template 7: General Template (rejection)
     */
    REJECTED_GENERAL("REJECTED_GENERAL");

    private final String code;

    EmailTemplateType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static EmailTemplateType fromCode(String code) {
        for (EmailTemplateType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown email template type: " + code);
    }
}
