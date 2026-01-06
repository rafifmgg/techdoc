package com.ocmsintranet.cronservice.framework.workflows.autorevival.services;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Auto-Revival workflow
 *
 * Provides methods to execute the auto-revival job
 * which automatically revives suspended notices when their due date is reached
 */
public interface AutoRevivalService {

    /**
     * Execute the auto-revival job
     *
     * @return CompletableFuture with the job result message
     */
    CompletableFuture<String> executeJob();
}
