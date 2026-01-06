package com.ocmsintranet.apiservice.workflows.furnish.domain;

/**
 * Enum representing whether the furnished person is a Hirer or Driver.
 * Based on OCMS 41 data dictionary.
 */
public enum OwnerDriverIndicator {
    /**
     * H - Hirer: Person furnished as the hirer of the vehicle
     */
    HIRER("H"),

    /**
     * D - Driver: Person furnished as the driver of the vehicle
     */
    DRIVER("D");

    private final String code;

    OwnerDriverIndicator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static OwnerDriverIndicator fromCode(String code) {
        for (OwnerDriverIndicator indicator : values()) {
            if (indicator.code.equals(code)) {
                return indicator;
            }
        }
        throw new IllegalArgumentException("Unknown owner/driver indicator: " + code);
    }
}
