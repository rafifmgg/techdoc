package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.helpers;

import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.domain.ComputerRuleCode;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.domain.ProcessingStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for reduction eligibility rule logic.
 *
 * Encapsulates the business rules for determining whether a notice
 * is eligible for reduction based on computer rule code and processing stage.
 *
 * Business Rules:
 * 1. If computer rule code is in eligible list {30305, 31302, 30302, 21300}:
 *    - Last processing stage must be in {NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3}
 *
 * 2. If computer rule code is NOT in eligible list:
 *    - Last processing stage must be RR3 or DR3 (special case)
 */
@Service
@Slf4j
public class ReductionRuleService {

    /**
     * Check if a computer rule code is in the eligible list.
     *
     * @param ruleCode The computer rule code
     * @return true if the code is eligible
     */
    public boolean isEligibleComputerRuleCode(Integer ruleCode) {
        boolean isEligible = ComputerRuleCode.isEligible(ruleCode);
        log.debug("Computer rule code {} eligibility: {}", ruleCode, isEligible);
        return isEligible;
    }

    /**
     * Check if a processing stage is allowed for eligible computer rule codes.
     *
     * @param stage The last processing stage
     * @return true if the stage is in the allowed list
     */
    public boolean isAllowedLastStageForEligibleCode(String stage) {
        boolean isAllowed = ProcessingStage.isGenerallyEligible(stage);
        log.debug("Processing stage {} is {} for eligible codes",
                stage, isAllowed ? "allowed" : "not allowed");
        return isAllowed;
    }

    /**
     * Check if a processing stage is allowed for non-eligible computer rule codes.
     * Only RR3 and DR3 are allowed as special cases.
     *
     * @param stage The last processing stage
     * @return true if the stage is RR3 or DR3
     */
    public boolean isAllowedLastStageForNonEligibleCode(String stage) {
        boolean isAllowed = ProcessingStage.isSpecialCaseEligible(stage);
        log.debug("Processing stage {} is {} for non-eligible codes (special case)",
                stage, isAllowed ? "allowed" : "not allowed");
        return isAllowed;
    }

    /**
     * Determine if a notice is eligible for reduction based on both
     * computer rule code and last processing stage.
     *
     * This is the main eligibility check that combines both rules.
     *
     * @param ruleCode The computer rule code
     * @param lastProcessingStage The last processing stage
     * @return true if the notice is eligible for reduction
     */
    public boolean isNoticeEligibleForReduction(Integer ruleCode, String lastProcessingStage) {
        log.info("Checking reduction eligibility for rule code: {}, stage: {}",
                ruleCode, lastProcessingStage);

        // Null checks
        if (ruleCode == null || lastProcessingStage == null || lastProcessingStage.trim().isEmpty()) {
            log.warn("Invalid parameters: ruleCode={}, lastProcessingStage={}",
                    ruleCode, lastProcessingStage);
            return false;
        }

        boolean isEligibleCode = isEligibleComputerRuleCode(ruleCode);

        if (isEligibleCode) {
            // Case A: Eligible code - check against general stage list
            boolean stageAllowed = isAllowedLastStageForEligibleCode(lastProcessingStage);
            if (stageAllowed) {
                log.info("Notice is ELIGIBLE: eligible code {} with allowed stage {}",
                        ruleCode, lastProcessingStage);
                return true;
            } else {
                log.warn("Notice is NOT ELIGIBLE: eligible code {} but stage {} not in allowed list",
                        ruleCode, lastProcessingStage);
                return false;
            }
        } else {
            // Case B: Non-eligible code - only RR3/DR3 allowed
            boolean specialCaseAllowed = isAllowedLastStageForNonEligibleCode(lastProcessingStage);
            if (specialCaseAllowed) {
                log.info("Notice is ELIGIBLE: non-eligible code {} but special case stage {} (RR3/DR3)",
                        ruleCode, lastProcessingStage);
                return true;
            } else {
                log.warn("Notice is NOT ELIGIBLE: non-eligible code {} and stage {} is not RR3/DR3",
                        ruleCode, lastProcessingStage);
                return false;
            }
        }
    }

    /**
     * Get a human-readable reason for why a notice is not eligible.
     *
     * @param ruleCode The computer rule code
     * @param lastProcessingStage The last processing stage
     * @return A string describing why the notice is not eligible
     */
    public String getIneligibilityReason(Integer ruleCode, String lastProcessingStage) {
        if (ruleCode == null || lastProcessingStage == null) {
            return "Missing required rule code or processing stage";
        }

        boolean isEligibleCode = isEligibleComputerRuleCode(ruleCode);

        if (isEligibleCode) {
            return String.format("Rule code %d is eligible, but processing stage '%s' is not in the allowed list " +
                    "(NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3)", ruleCode, lastProcessingStage);
        } else {
            return String.format("Rule code %d is not in the eligible list, and processing stage '%s' is not " +
                    "RR3 or DR3 (special case)", ruleCode, lastProcessingStage);
        }
    }
}
