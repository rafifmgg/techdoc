package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for patching Next Processing Date (NPD) after revival.
 *
 * OCMS 19 - Revive Suspensions
 * Specification Requirement (v1_Revival_Rules.md:181):
 * "(b) If Next Processing Date is past date, patch Next Processing Date to +2 days to cater for DataHive run"
 *
 * Purpose:
 * - After revival, DataHive/MHA needs 2 days to run address lookup
 * - NPD must be patched to ensure notice doesn't skip processing stages
 * - Different codes have different patching rules
 *
 * Exceptions:
 * - TS-ROV: Skip NPD patching (no need to prepare next stage)
 * - TS-NRO: Always patch NPD to +2 days (even if NPD is future date)
 */
@Component
@Slf4j
public class NpdPatchingHelper {

    // ========================================
    // SUSPENSION CODES THAT REQUIRE NPD PATCHING
    // ========================================

    /**
     * TS codes that require NPD patching after revival.
     * Based on OCMS 19 v2.0 specification.
     */
    private static final Set<String> TS_CODES_REQUIRING_NPD_PATCH = new HashSet<>(Arrays.asList(
        "ACR",  // Application for Court Review
        "APE",  // Appeal
        "APP",  // Appeal Allowed (Partial Payment)
        "CCE",  // Cancellation of Court Extension
        "MS",   // Missing System
        "NRO",  // Non-Receiving Officer (Special handling - always +2 days)
        "PDP"   // Post-Dated Payment
    ));

    /**
     * PS codes that require NPD patching after revival.
     * Based on OCMS 19 v2.0 specification.
     */
    private static final Set<String> PS_CODES_REQUIRING_NPD_PATCH = new HashSet<>(Arrays.asList(
        "DIP",  // Diplomatic Vehicle
        "FOR",  // Foreign Vehicle
        "RIP",  // Registered in Possession
        "RP2",  // Registered in Possession 2
        "SCT",  // Sent to Court
        "SLC",  // Subject to Litigation Check
        "SSV",  // Subject to Special Verification
        "VCT"   // Void Court
    ));

    // ========================================
    // EXCEPTION CODES
    // ========================================

    /**
     * TS-ROV: Registration of Vehicle.
     * Exception: Skip NPD patching (no need to prepare next stage).
     */
    private static final String TS_ROV = "ROV";

    /**
     * TS-NRO: Non-Receiving Officer.
     * Exception: Always patch NPD to +2 days (even if NPD is future date).
     */
    private static final String TS_NRO = "NRO";

    /**
     * Default number of days to add to NPD for DataHive processing.
     */
    private static final int NPD_PATCH_DAYS = 2;

    // ========================================
    // PUBLIC API
    // ========================================

    /**
     * Patch Next Processing Date (NPD) after revival if required.
     *
     * Logic:
     * 1. Check if suspension code requires NPD patching
     * 2. Handle TS-ROV exception (skip patching)
     * 3. Handle TS-NRO exception (always patch to +2 days)
     * 4. For other codes: Only patch if NPD is past date
     *
     * @param notice Notice to patch
     * @param suspensionType Suspension type ("TS" or "PS")
     * @param suspensionCode Suspension code (e.g., "ACR", "DIP", "ROV")
     * @return true if NPD was patched, false otherwise
     */
    public boolean patchNpdIfRequired(OcmsValidOffenceNotice notice, String suspensionType, String suspensionCode) {
        if (notice == null) {
            log.warn("[NPD Patch] Notice is null - skipping");
            return false;
        }

        if (suspensionType == null || suspensionCode == null) {
            log.warn("[NPD Patch] Suspension type or code is null - skipping");
            return false;
        }

        String noticeNo = notice.getNoticeNo();
        log.info("[NPD Patch] Checking if NPD patching required for notice {}: type={}, code={}",
            noticeNo, suspensionType, suspensionCode);

        // Step 1: Check if code requires NPD patching
        if (!requiresNpdPatching(suspensionType, suspensionCode)) {
            log.info("[NPD Patch] Notice {}: Code {}-{} does not require NPD patching",
                noticeNo, suspensionType, suspensionCode);
            return false;
        }

        // Step 2: Handle TS-ROV exception (skip patching)
        if ("TS".equals(suspensionType) && TS_ROV.equals(suspensionCode)) {
            log.info("[NPD Patch] Notice {}: TS-ROV exception - skipping NPD patching",
                noticeNo);
            return false;
        }

        // Step 3: Get current NPD
        LocalDateTime currentNpd = notice.getNextProcessingDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newNpd = now.plusDays(NPD_PATCH_DAYS);

        // Step 4: Handle TS-NRO exception (always patch to +2 days)
        if ("TS".equals(suspensionType) && TS_NRO.equals(suspensionCode)) {
            log.info("[NPD Patch] Notice {}: TS-NRO exception - always patching NPD to +2 days (current={}, new={})",
                noticeNo, currentNpd, newNpd);
            notice.setNextProcessingDate(newNpd);
            return true;
        }

        // Step 5: For other codes, only patch if NPD is past date
        if (currentNpd == null || currentNpd.isBefore(now)) {
            log.info("[NPD Patch] Notice {}: NPD is past date - patching to +2 days (old={}, new={})",
                noticeNo, currentNpd, newNpd);
            notice.setNextProcessingDate(newNpd);
            return true;
        } else {
            log.info("[NPD Patch] Notice {}: NPD is future date - no patching needed (npd={})",
                noticeNo, currentNpd);
            return false;
        }
    }

    /**
     * Check if a suspension code requires NPD patching.
     *
     * @param suspensionType Suspension type ("TS" or "PS")
     * @param suspensionCode Suspension code
     * @return true if NPD patching is required
     */
    public boolean requiresNpdPatching(String suspensionType, String suspensionCode) {
        if (suspensionType == null || suspensionCode == null) {
            return false;
        }

        if ("TS".equals(suspensionType)) {
            return TS_CODES_REQUIRING_NPD_PATCH.contains(suspensionCode);
        } else if ("PS".equals(suspensionType)) {
            return PS_CODES_REQUIRING_NPD_PATCH.contains(suspensionCode);
        }

        return false;
    }

    /**
     * Get the set of TS codes that require NPD patching.
     * Useful for testing and validation.
     *
     * @return Unmodifiable set of TS codes
     */
    public Set<String> getTsCodesRequiringNpdPatch() {
        return new HashSet<>(TS_CODES_REQUIRING_NPD_PATCH);
    }

    /**
     * Get the set of PS codes that require NPD patching.
     * Useful for testing and validation.
     *
     * @return Unmodifiable set of PS codes
     */
    public Set<String> getPsCodesRequiringNpdPatch() {
        return new HashSet<>(PS_CODES_REQUIRING_NPD_PATCH);
    }

    /**
     * Get the number of days to add for NPD patching.
     *
     * @return Number of days (currently 2)
     */
    public int getNpdPatchDays() {
        return NPD_PATCH_DAYS;
    }
}
