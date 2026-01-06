package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.EligibilityOutcome;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to check eligibility for Change Processing Stage
 * Based on OCMS CPS Spec §3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {

    private final OcmsValidOffenceNoticeService vonService;
    private final OcmsOffenceNoticeOwnerDriverService onodService;

    // Eligibility error codes (§3.3)
    public static final String NOT_FOUND = "OCMS.CPS.ELIG.NOT_FOUND";
    public static final String ROLE_CONFLICT = "OCMS.CPS.ELIG.ROLE_CONFLICT";
    public static final String NO_STAGE_RULE = "OCMS.CPS.ELIG.NO_STAGE_RULE";
    public static final String INELIGIBLE_STAGE = "OCMS.CPS.ELIG.INELIGIBLE_STAGE";
    public static final String COURT_STAGE = "OCMS.CPS.ELIG.COURT_STAGE";
    public static final String PS_BLOCKED = "OCMS.CPS.ELIG.PS_BLOCKED";
    public static final String TS_BLOCKED = "OCMS.CPS.ELIG.TS_BLOCKED";
    public static final String EXISTING_CHANGE_TODAY = "OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY";

    // Court stages (§3.2 R7)
    private static final Set<String> COURT_STAGES = new HashSet<>(Arrays.asList("CRT", "CRC", "CFI"));

    // Eligible stages per role (§3.2 R4, R5)
    private static final Set<String> DRIVER_STAGES = new HashSet<>(Arrays.asList("DN1", "DN2", "DR3"));
    private static final Set<String> OWNER_HIRER_DIRECTOR_STAGES = new HashSet<>(Arrays.asList("ROV", "RD1", "RD2", "RR3"));

    /**
     * Check eligibility for a notice to change processing stage
     * Based on OCMS CPS Spec §3.4
     *
     * @param noticeNo Notice number
     * @param requestedStage Requested new stage (optional, can be derived)
     * @return EligibilityOutcome
     */
    public EligibilityOutcome checkEligibility(String noticeNo, String requestedStage) {
        log.debug("Checking eligibility for notice: {}, requested stage: {}", noticeNo, requestedStage);

        // Step 1: Query VON and ONOD (§3.1 steps 1-2)
        Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
        OcmsOffenceNoticeOwnerDriver onod = findOnod(noticeNo);

        // Step 2: Check if both not found (§3.1 step 3, §3.2 R1)
        if (!vonOpt.isPresent() && onod == null) {
            return EligibilityOutcome.notChangeable(NOT_FOUND, "Notice not found in VON or ONOD");
        }

        OcmsValidOffenceNotice von = vonOpt.orElse(null);

        // Step 3: Resolve role (§3.1 step 4, §3.2 R2)
        String role = resolveRole(von, onod);
        if (role == null) {
            return EligibilityOutcome.notChangeable(ROLE_CONFLICT, "Cannot determine offender role");
        }

        // Step 4: Check court stage (§3.1 step 5, §3.2 R7)
        if (von != null && isCourtStage(von.getLastProcessingStage())) {
            return EligibilityOutcome.notChangeable(COURT_STAGE, "Notice is at court stage");
        }

        // Step 5: Check PS/TS policy (§3.2 R8) - configurable
        // For now, we allow TS/PS changes (can be configured later)
        if (von != null && "PS".equals(von.getSuspensionType())) {
            // Optional: block PS notices
            // return EligibilityOutcome.notChangeable(PS_BLOCKED, "Notice has permanent suspension");
        }

        // Step 6: Resolve requested stage (§3.1 step 6)
        String chosenStage = requestedStage;
        if (chosenStage == null || chosenStage.trim().isEmpty()) {
            // Derive from StageMap based on current stage and role
            chosenStage = deriveNextStage(von != null ? von.getLastProcessingStage() : null, role);
            if (chosenStage == null) {
                return EligibilityOutcome.notChangeable(NO_STAGE_RULE, "Cannot derive next stage");
            }
        }

        // Step 7: Build eligible set (§3.1 step 7, §3.2 R4, R5)
        Set<String> eligibleStages = getEligibleStages(role);

        // Step 8: Check if requested stage is in eligible set (§3.1 step 8, §3.2 R6)
        if (!eligibleStages.contains(chosenStage)) {
            return EligibilityOutcome.notChangeable(INELIGIBLE_STAGE,
                    "Stage " + chosenStage + " not eligible for role " + role);
        }

        // Step 9: CHANGEABLE
        log.debug("Notice {} is changeable: role={}, chosenStage={}", noticeNo, role, chosenStage);
        return EligibilityOutcome.changeable(role, eligibleStages, chosenStage);
    }

    /**
     * Find ONOD for notice
     * Priority: DRIVER > OWNER > HIRER > DIRECTOR
     */
    private OcmsOffenceNoticeOwnerDriver findOnod(String noticeNo) {
        try {
            // Get ONOD records for this notice
            var onods = onodService.findByNoticeNo(noticeNo);
            if (onods != null && !onods.isEmpty()) {
                // Sort by priority: D > O > H > others
                onods.sort((a, b) -> {
                    int priorityA = getRolePriority(a.getOwnerDriverIndicator());
                    int priorityB = getRolePriority(b.getOwnerDriverIndicator());
                    return Integer.compare(priorityA, priorityB);
                });
                return onods.get(0);
            }
            log.debug("No ONOD found for notice {}", noticeNo);
        } catch (Exception e) {
            log.warn("Error querying ONOD for notice {}: {}", noticeNo, e.getMessage());
        }
        return null;
    }

    /**
     * Get role priority for sorting (lower = higher priority)
     */
    private int getRolePriority(String roleCode) {
        if (roleCode == null) return 999;
        switch (roleCode.toUpperCase()) {
            case "D": return 1; // DRIVER - highest priority
            case "O": return 2; // OWNER
            case "H": return 3; // HIRER
            default: return 4; // Others (DIRECTOR, etc)
        }
    }

    /**
     * Resolve role from VON and ONOD
     * Priority: ONOD (since VON doesn't have ownerDriverIndicator field)
     *
     * NOTE: OcmsValidOffenceNotice entity doesn't have ownerDriverIndicator field.
     * Role information is only available in OcmsOffenceNoticeOwnerDriver.
     */
    private String resolveRole(OcmsValidOffenceNotice von, OcmsOffenceNoticeOwnerDriver onod) {
        // Get role from ONOD
        if (onod != null && onod.getOwnerDriverIndicator() != null) {
            return normalizeRole(onod.getOwnerDriverIndicator());
        }

        // No role information available
        // Default to OWNER if VON exists but no ONOD
        if (von != null) {
            log.debug("No ONOD found for notice {}, defaulting to OWNER role", von.getNoticeNo());
            return "OWNER";
        }

        return null;
    }

    /**
     * Normalize role codes to standard values
     */
    private String normalizeRole(String roleCode) {
        if (roleCode == null) return null;

        switch (roleCode.toUpperCase()) {
            case "D":
            case "DRIVER":
                return "DRIVER";
            case "O":
            case "OWNER":
                return "OWNER";
            case "H":
            case "HIRER":
                return "HIRER";
            case "DIR":
            case "DIRECTOR":
                return "DIRECTOR";
            default:
                return roleCode.toUpperCase();
        }
    }

    /**
     * Check if stage is a court stage
     * Made public for use in search API
     */
    public boolean isCourtStage(String stage) {
        return stage != null && COURT_STAGES.contains(stage);
    }

    /**
     * Get eligible stages based on role
     */
    private Set<String> getEligibleStages(String role) {
        if ("DRIVER".equals(role)) {
            return DRIVER_STAGES;
        } else {
            // OWNER, HIRER, DIRECTOR
            return OWNER_HIRER_DIRECTOR_STAGES;
        }
    }

    /**
     * Derive next stage from StageMap
     * This is a simplified implementation - should be database-driven
     */
    private String deriveNextStage(String currentStage, String role) {
        if (currentStage == null || role == null) {
            return null;
        }

        // Simplified stage derivation logic
        // In production, this should query a StageMap table
        if ("DRIVER".equals(role)) {
            switch (currentStage) {
                case "NPA":
                case "ENA":
                    return "DN1";
                case "DN1":
                    return "DN2";
                case "DN2":
                    return "DR3";
                default:
                    return null;
            }
        } else {
            // OWNER, HIRER, DIRECTOR
            switch (currentStage) {
                case "NPA":
                    return "ROV";
                case "ROV":
                    return "RD1";
                case "RD1":
                    return "RD2";
                case "RD2":
                    return "RR3";
                default:
                    return null;
            }
        }
    }
}
