package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services;

import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for HST Auto-Suspend operations
 * OCMS 20: Auto-suspend new notices with HST IDs when address is invalid
 */
public interface HstAutoSuspendService {

    /**
     * Execute the auto-suspend job for new notices with HST IDs
     * Checks ocms_hst for new notices and applies TS-HST if address is invalid
     *
     * @return CompletableFuture containing the suspension result
     */
    CompletableFuture<SuspensionResult> executeAutoSuspendJob();

    /**
     * Process a specific HST ID for auto-suspension
     *
     * @param hstId The HST ID to process
     * @return Suspension result
     */
    SuspensionResult processHstId(String hstId);
}
