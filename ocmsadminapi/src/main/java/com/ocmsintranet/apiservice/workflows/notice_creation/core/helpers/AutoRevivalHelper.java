package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Auto-Revival Helper (Event-Driven and Scheduled Triggers)
 * Provides async, non-blocking methods for automatic revival operations
 *
 * Purpose:
 * - Centralized interface for all auto-revival logic
 * - Async execution (doesn't block main workflow)
 * - Error isolation (revival failure doesn't fail main workflow)
 * - Consistent logging for all auto-revivals
 *
 * Usage:
 * <pre>
 * // In PaymentService.java
 * {@literal @}Autowired
 * private AutoRevivalHelper autoRevivalHelper;
 *
 * public void processPayment(...) {
 *     // Main payment logic
 *     processPaymentTransaction();
 *
 *     // Trigger auto revival asynchronously (one line!)
 *     autoRevivalHelper.reviveAfterPayment(noticeNo);
 * }
 * </pre>
 *
 * Auto-Revival Triggers:
 * 1. Scheduled (Cron Job): Revive expired TS (due_date_of_revival <= sysdate)
 * 2. Event-Driven: Revive after payment
 * 3. Looping TS: Auto re-apply TS-CLV/TS-PDP after revival
 *
 * Looping TS Logic (OCMS 19):
 * - TS-CLV: Always re-apply immediately after revival
 * - TS-HST: Remain suspended until manual lift (handled by HstLoopingSuspendService)
 * - TS-PDP: Conditional re-apply based on furnish application status
 *   - If status = PENDING → re-apply TS-PDP
 *   - If status = APPROVED or REJECTED → do NOT re-apply
 *
 * @author Claude Code
 * @since 2025-11-25 (OCMS 17/18 Auto-Revival)
 * @updated 2025-12-21 (OCMS 19 TS-PDP Conditional Looping)
 */
@Component
@Slf4j
public class AutoRevivalHelper {

    @Autowired
    private TemporaryRevivalHelper tsRevivalHelper;

    @Autowired
    private PermanentRevivalHelper psRevivalHelper;

    @Autowired
    private SuspendedNoticeService suspendedNoticeService;

    @Autowired
    private TemporarySuspensionHelper tsHelper;

    @Autowired
    private OcmsFurnishApplicationService furnishApplicationService;

    // ==================== Public API ====================

    /**
     * Revive TS asynchronously (non-blocking)
     *
     * @param noticeNo Notice number to revive
     * @param revivalReason Revival reason code (e.g., "SPO", "PAY", "MAN")
     * @param remarks Revival remarks
     */
    @Async
    public void reviveTS(String noticeNo, String revivalReason, String remarks) {
        try {
            log.info("[Auto-Revive] Triggering TS revival for notice {} (reason: {})", noticeNo, revivalReason);

            tsRevivalHelper.reviveTS(
                noticeNo,
                revivalReason,          // revivalReason
                remarks,                // revivalRemarks
                "SYSTEM"                // officerAuthorisingRevival
            );

            log.info("[Auto-Revive] Successfully revived TS for notice {}", noticeNo);

        } catch (Exception e) {
            // Log error but don't propagate (don't fail main workflow)
            log.error("[Auto-Revive] Failed to revive TS for notice {}: {}",
                noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Revive PS asynchronously (non-blocking)
     *
     * @param noticeNo Notice number to revive
     * @param revivalReason Revival reason code (e.g., "SPO", "PAY", "MAN")
     * @param remarks Revival remarks
     */
    @Async
    public void revivePS(String noticeNo, String revivalReason, String remarks) {
        try {
            log.info("[Auto-Revive] Triggering PS revival for notice {} (reason: {})", noticeNo, revivalReason);

            psRevivalHelper.revivePS(
                noticeNo,
                revivalReason,          // revivalReason
                remarks,                // revivalRemarks
                "SYSTEM"                // officerAuthorisingRevival
            );

            log.info("[Auto-Revive] Successfully revived PS for notice {}", noticeNo);

        } catch (Exception e) {
            // Log error but don't propagate (don't fail main workflow)
            log.error("[Auto-Revive] Failed to revive PS for notice {}: {}",
                noticeNo, e.getMessage(), e);
        }
    }

    // ==================== Convenience Methods (Specific Triggers) ====================

    /**
     * Revive notice after payment (auto-triggered by PaymentService)
     *
     * Automatically determines suspension type (TS or PS) and revives accordingly
     */
    @Async
    public void reviveAfterPayment(String noticeNo) {
        try {
            log.info("[Auto-Revive] Payment received - checking suspensions for notice {}", noticeNo);

            // Find active suspensions
            List<SuspendedNotice> activeSuspensions = findAllActiveSuspensions(noticeNo);

            if (activeSuspensions.isEmpty()) {
                log.info("[Auto-Revive] No active suspensions for notice {} - skipping", noticeNo);
                return;
            }

            // Revive all active suspensions
            for (SuspendedNotice suspension : activeSuspensions) {
                String suspensionType = suspension.getSuspensionType();
                String remarks = "Auto revival - Notice paid";

                if ("TS".equals(suspensionType)) {
                    reviveTS(noticeNo, "PAY", remarks);
                } else if ("PS".equals(suspensionType)) {
                    revivePS(noticeNo, "PAY", remarks);
                }
            }

        } catch (Exception e) {
            log.error("[Auto-Revive] Error during payment revival for notice {}: {}",
                noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Process expired TS (called by cron job)
     *
     * Logic:
     * 1. Find all TS where due_date_of_revival <= now() AND date_of_revival IS NULL
     * 2. Revive each expired TS
     * 3. Handle looping TS (CLV: re-apply, HST: manual lift only)
     *
     * @return Number of TS revived
     */
    @Async
    public int processExpiredTS() {
        try {
            log.info("[Auto-Revive] Starting expired TS processing");

            // Find all expired TS
            List<SuspendedNotice> expiredTSList = findExpiredTS();

            if (expiredTSList.isEmpty()) {
                log.info("[Auto-Revive] No expired TS found");
                return 0;
            }

            log.info("[Auto-Revive] Found {} expired TS to process", expiredTSList.size());

            int revivedCount = 0;

            for (SuspendedNotice expiredTS : expiredTSList) {
                String noticeNo = expiredTS.getNoticeNo();
                String reasonOfSuspension = expiredTS.getReasonOfSuspension();

                // Revive the expired TS
                reviveTS(noticeNo, "SPO", "Auto revival - TS expired");
                revivedCount++;

                // Handle looping TS
                handleLoopingTS(noticeNo, reasonOfSuspension);
            }

            log.info("[Auto-Revive] Processed {} expired TS", revivedCount);
            return revivedCount;

        } catch (Exception e) {
            log.error("[Auto-Revive] Error processing expired TS: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Handle looping TS (CLV, HST, PDP)
     *
     * CLV (Classified Vehicles): Auto re-apply TS immediately after revival
     * HST (House Tenants): Remain suspended until manual lift (no action)
     * PDP (Pending Driver/Hirer Particulars): Conditional re-apply based on furnish application status
     *
     * @param noticeNo Notice number
     * @param reasonOfSuspension Suspension reason code (CLV, HST, PDP, etc.)
     */
    private void handleLoopingTS(String noticeNo, String reasonOfSuspension) {
        if ("CLV".equals(reasonOfSuspension)) {
            // CLV: Auto re-apply TS-CLV immediately
            log.info("[Auto-Revive] Re-applying TS-CLV for notice {} (looping)", noticeNo);

            try {
                tsHelper.processTS(
                    noticeNo,
                    "OCMS Backend",                    // suspensionSource
                    "CLV",                             // reasonOfSuspension
                    null,                              // daysToRevive (use default)
                    "Auto re-apply CLV (looping)",     // suspensionRemarks
                    "SYSTEM",                          // officerAuthorisingSuspension
                    null,                              // srNo (auto-generated)
                    null                               // caseNo
                );

                log.info("[Auto-Revive] Successfully re-applied TS-CLV for notice {}", noticeNo);

            } catch (Exception e) {
                log.error("[Auto-Revive] Failed to re-apply TS-CLV for notice {}: {}",
                    noticeNo, e.getMessage(), e);
            }

        } else if ("HST".equals(reasonOfSuspension)) {
            // HST: Remain suspended until manual lift (no action)
            log.info("[Auto-Revive] TS-HST expired for notice {} - requires manual lift", noticeNo);

        } else if ("PDP".equals(reasonOfSuspension)) {
            // PDP: Conditional looping - re-apply only if particulars still pending
            log.info("[Auto-Revive] Checking TS-PDP looping for notice {}", noticeNo);

            if (isFurnishApplicationPending(noticeNo)) {
                log.info("[Auto-Revive] Furnish application still pending - re-applying TS-PDP for notice {}", noticeNo);

                try {
                    tsHelper.processTS(
                        noticeNo,
                        "OCMS Backend",                    // suspensionSource
                        "PDP",                             // reasonOfSuspension
                        null,                              // daysToRevive (use default)
                        "Auto re-apply PDP - particulars still pending",  // suspensionRemarks
                        "SYSTEM",                          // officerAuthorisingSuspension
                        null,                              // srNo (auto-generated)
                        null                               // caseNo
                    );

                    log.info("[Auto-Revive] Successfully re-applied TS-PDP for notice {}", noticeNo);

                } catch (Exception e) {
                    log.error("[Auto-Revive] Failed to re-apply TS-PDP for notice {}: {}",
                        noticeNo, e.getMessage(), e);
                }
            } else {
                log.info("[Auto-Revive] Furnish application approved/rejected - TS-PDP not re-applied for notice {}", noticeNo);
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Find all active suspensions for a notice (both TS and PS)
     */
    private List<SuspendedNotice> findAllActiveSuspensions(String noticeNo) {
        try {
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});

            List<SuspendedNotice> allSuspensions = suspendedNoticeService.getAll(queryParams).getData();

            // Filter for active suspensions (date_of_revival IS NULL)
            return allSuspensions.stream()
                .filter(s -> s.getDateOfRevival() == null)
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding active suspensions for notice {}: {}", noticeNo, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Find all expired TS (due_date_of_revival <= now() AND date_of_revival IS NULL)
     */
    private List<SuspendedNotice> findExpiredTS() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Query all active TS
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("suspensionType", new String[]{"TS"});

            List<SuspendedNotice> allTS = suspendedNoticeService.getAll(queryParams).getData();

            // Filter for expired and active TS
            return allTS.stream()
                .filter(ts -> ts.getDateOfRevival() == null)  // Active (not yet revived)
                .filter(ts -> ts.getDueDateOfRevival() != null)  // Has revival date
                .filter(ts -> !ts.getDueDateOfRevival().isAfter(now))  // Expired (due date <= now)
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding expired TS: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Check if furnish application is still pending approval
     * OCMS 19 TS-PDP Conditional Looping Logic:
     * - If latest application status = "P" (PENDING) → return true (re-apply TS-PDP)
     * - If latest application status = "A" (APPROVED) → return false (do not re-apply)
     * - If latest application status = "R" (REJECTED) → return false (do not re-apply)
     * - If no application found → return false (assume not pending)
     *
     * @param noticeNo Notice number
     * @return true if latest furnish application status = PENDING, false otherwise
     */
    private boolean isFurnishApplicationPending(String noticeNo) {
        try {
            // Query furnish applications for this notice
            List<OcmsFurnishApplication> applications = furnishApplicationService.findByNoticeNo(noticeNo);

            if (applications == null || applications.isEmpty()) {
                log.warn("[Auto-Revive] No furnish application found for notice {} - assuming not pending", noticeNo);
                return false;
            }

            // Get latest application (sorted by cre_date DESC)
            // Multiple submissions possible (resubmissions) - use the most recent one
            Optional<OcmsFurnishApplication> latestApp = applications.stream()
                .max(Comparator.comparing(OcmsFurnishApplication::getCreDate));

            if (latestApp.isPresent()) {
                String status = latestApp.get().getStatus();

                log.info("[Auto-Revive] Latest furnish application for notice {} has status: {} (created: {})",
                    noticeNo, status, latestApp.get().getCreDate());

                // Re-apply TS-PDP only if status = "P" (PENDING)
                // Status codes: P = PENDING, A = APPROVED, R = REJECTED
                boolean isPending = "P".equals(status);

                if (isPending) {
                    log.info("[Auto-Revive] Furnish application is PENDING - TS-PDP will be re-applied");
                } else {
                    log.info("[Auto-Revive] Furnish application is {} - TS-PDP will NOT be re-applied",
                        "A".equals(status) ? "APPROVED" : ("R".equals(status) ? "REJECTED" : "UNKNOWN"));
                }

                return isPending;
            }

            log.warn("[Auto-Revive] Could not determine latest furnish application for notice {} - assuming not pending", noticeNo);
            return false;

        } catch (Exception e) {
            log.error("[Auto-Revive] Error checking furnish application status for notice {}: {}",
                noticeNo, e.getMessage(), e);
            // On error, don't re-apply (fail safe - avoid infinite looping)
            return false;
        }
    }
}
