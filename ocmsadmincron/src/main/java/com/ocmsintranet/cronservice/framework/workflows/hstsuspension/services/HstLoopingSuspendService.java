package com.ocmsintranet.cronservice.framework.workflows.hstsuspension.services;

import com.ocmsintranet.cronservice.framework.workflows.hstsuspension.dto.SuspensionResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for TS-HST Looping Suspension operations
 * OCMS 20: Re-apply TS-HST suspension when revival date approaches (indefinite looping)
 */
public interface HstLoopingSuspendService {

    /**
     * Execute the looping suspension job
     * Checks for TS-HST suspensions approaching revival and re-applies suspension
     *
     * @return CompletableFuture containing the suspension result
     */
    CompletableFuture<SuspensionResult> executeLoopingJob();

    /**
     * Process a specific notice for looping suspension
     *
     * @param noticeNo The notice number to process
     * @return Suspension result
     */
    SuspensionResult processNotice(String noticeNo);
}
