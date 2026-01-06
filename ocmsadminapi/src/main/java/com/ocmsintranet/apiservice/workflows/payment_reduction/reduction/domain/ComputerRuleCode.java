package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum representing computer rule codes that are eligible for reduction.
 *
 * Based on business rules, only specific rule codes qualify for reduction processing.
 * This enum provides type-safe handling and utility methods for eligibility checks.
 */
public enum ComputerRuleCode {

    /**
     * Rule code 30305
     */
    CODE_30305(30305),

    /**
     * Rule code 31302
     */
    CODE_31302(31302),

    /**
     * Rule code 30302
     */
    CODE_30302(30302),

    /**
     * Rule code 21300
     */
    CODE_21300(21300);

    private final Integer code;

    ComputerRuleCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    /**
     * Find enum value from integer code.
     *
     * @param code The integer rule code
     * @return Optional containing the enum value if found
     */
    public static Optional<ComputerRuleCode> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst();
    }

    /**
     * Check if a code is in the eligible list.
     *
     * @param code The integer rule code to check
     * @return true if the code is eligible for reduction
     */
    public static boolean isEligible(Integer code) {
        return fromCode(code).isPresent();
    }

    /**
     * Get all eligible code values as a set of integers.
     * Useful for database queries or configuration.
     *
     * @return Set of eligible code integers
     */
    public static Set<Integer> getEligibleCodes() {
        return Arrays.stream(values())
                .map(ComputerRuleCode::getCode)
                .collect(Collectors.toSet());
    }
}
