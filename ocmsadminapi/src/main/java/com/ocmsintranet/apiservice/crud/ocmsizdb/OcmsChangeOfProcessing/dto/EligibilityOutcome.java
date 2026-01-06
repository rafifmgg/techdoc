package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Eligibility outcome for Change Processing Stage
 * Based on OCMS CPS Spec §3.4
 */
@Data
@NoArgsConstructor
public class EligibilityOutcome {

    private String status; // CHANGEABLE or NON_CHANGEABLE
    private String role;
    private Set<String> allowedStages;
    private String chosenStage;
    private String code;
    private String message;

    private EligibilityOutcome(String status) {
        this.status = status;
    }

    /**
     * Create a CHANGEABLE outcome
     */
    public static EligibilityOutcome changeable(String role, Set<String> allowedStages, String chosenStage) {
        EligibilityOutcome outcome = new EligibilityOutcome("CHANGEABLE");
        outcome.role = role;
        outcome.allowedStages = allowedStages;
        outcome.chosenStage = chosenStage;
        return outcome;
    }

    /**
     * Create a NON_CHANGEABLE outcome
     */
    public static EligibilityOutcome notChangeable(String code, String message) {
        EligibilityOutcome outcome = new EligibilityOutcome("NON_CHANGEABLE");
        outcome.code = code;
        outcome.message = message;
        return outcome;
    }

    public boolean isChangeable() {
        return "CHANGEABLE".equals(status);
    }
}
