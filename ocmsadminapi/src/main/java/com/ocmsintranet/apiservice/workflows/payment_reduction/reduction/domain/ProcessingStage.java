package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum representing processing stages that affect reduction eligibility.
 *
 * Different processing stages have different eligibility rules:
 * - For eligible rule codes: must be in the general eligible list
 * - For non-eligible rule codes: must be RR3 or DR3 (special case)
 */
public enum ProcessingStage {

    /**
     * Notice Pending Action
     */
    NPA("NPA"),

    /**
     * Revival
     */
    ROV("ROV"),

    /**
     * Enforcement Action
     */
    ENA("ENA"),

    /**
     * Reminder 1
     */
    RD1("RD1"),

    /**
     * Reminder 2
     */
    RD2("RD2"),

    /**
     * Reminder 3 (special case: eligible even for non-eligible rule codes)
     */
    RR3("RR3"),

    /**
     * Demand Notice 1
     */
    DN1("DN1"),

    /**
     * Demand Notice 2
     */
    DN2("DN2"),

    /**
     * Demand Reminder 3 (special case: eligible even for non-eligible rule codes)
     */
    DR3("DR3");

    private final String stage;

    ProcessingStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    /**
     * Find enum value from string stage code.
     *
     * @param stage The string stage code (e.g., "NPA", "RR3")
     * @return Optional containing the enum value if found
     */
    public static Optional<ProcessingStage> fromStage(String stage) {
        if (stage == null || stage.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalizedStage = stage.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(s -> s.stage.equals(normalizedStage))
                .findFirst();
    }

    /**
     * Check if a stage is in the general eligible list.
     * This applies when the computer rule code is in the eligible list.
     *
     * @param stage The string stage code
     * @return true if the stage is in the general eligible list
     */
    public static boolean isGenerallyEligible(String stage) {
        return fromStage(stage).isPresent();
    }

    /**
     * Check if a stage is RR3 or DR3 (special case eligibility).
     * These stages make a notice eligible for reduction even if the
     * computer rule code is not in the eligible list.
     *
     * @param stage The string stage code
     * @return true if the stage is RR3 or DR3
     */
    public static boolean isSpecialCaseEligible(String stage) {
        Optional<ProcessingStage> stageEnum = fromStage(stage);
        return stageEnum.isPresent() &&
                (stageEnum.get() == RR3 || stageEnum.get() == DR3);
    }

    /**
     * Get all eligible stage codes as a set of strings.
     * Useful for database queries or configuration.
     *
     * @return Set of eligible stage strings
     */
    public static Set<String> getEligibleStages() {
        return Arrays.stream(values())
                .map(ProcessingStage::getStage)
                .collect(Collectors.toSet());
    }

    /**
     * Get special case stages (RR3, DR3) as a set.
     *
     * @return Set containing RR3 and DR3
     */
    public static Set<String> getSpecialCaseStages() {
        return Set.of(RR3.getStage(), DR3.getStage());
    }
}
