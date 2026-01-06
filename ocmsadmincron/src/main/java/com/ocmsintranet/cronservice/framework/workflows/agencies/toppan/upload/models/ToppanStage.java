package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.models;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing the different stages of the Toppan upload workflow.
 * Each stage corresponds to a different type of file to be generated.
 *
 * RD1 - Reminder 1 (First reminder)
 * RD2 - Reminder 2 (Second reminder)
 * RR3 - Reminder 3 (Final reminder)
 * DN1 - Demand Notice 1 (First demand notice)
 * DN2 - Demand Notice 2 (Second demand notice)
 * DR3 - Demand Notice 3 (Final demand notice)
 * ANL - Advisory Notice Letter (OCMS 10)
 */
public enum ToppanStage {
    RD1("RD1", "Reminder 1", "RD2", 14),
    RD2("RD2", "Reminder 2", "RR3", 14),
    RR3("RR3", "Reminder 3", "DN1", 14),
    DN1("DN1", "Demand Notice 1", "DN2", 14),
    DN2("DN2", "Demand Notice 2", "DR3", 14),
    DR3("DR3", "Demand Notice 3", null, 0),
    ANL("ANL", "Advisory Notice Letter", null, 0);
    
    private final String currentStage;
    private final String description;
    private final String nextStage;
    private final int durationDays;
    
    ToppanStage(String currentStage, String description, String nextStage, int durationDays) {
        this.currentStage = currentStage;
        this.description = description;
        this.nextStage = nextStage;
        this.durationDays = durationDays;
    }
    
    public String getCurrentStage() {
        return currentStage;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the next stage in the workflow sequence
     * 
     * @return The next stage, or null if this is the final stage
     */
    public String getNextStage() {
        return nextStage;
    }
    
    /**
     * Get the duration in days before the next stage should be processed
     * 
     * @return The duration in days
     */
    public int getDurationDays() {
        return durationDays;
    }
    
    /**
     * Check if a stage string is valid
     * 
     * @param stage The stage string to check
     * @return true if the stage is valid, false otherwise
     */
    public static boolean isValidStage(String stage) {
        if (stage == null || stage.isEmpty()) {
            return false;
        }
        
        return Arrays.stream(values())
                .anyMatch(s -> s.getCurrentStage().equalsIgnoreCase(stage));
    }
    
    /**
     * Get a ToppanStage enum value from a stage string
     * 
     * @param stage The stage string
     * @return Optional containing the ToppanStage if found, empty otherwise
     */
    public static Optional<ToppanStage> fromString(String stage) {
        if (stage == null || stage.isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
                .filter(s -> s.getCurrentStage().equalsIgnoreCase(stage))
                .findFirst();
    }
}
