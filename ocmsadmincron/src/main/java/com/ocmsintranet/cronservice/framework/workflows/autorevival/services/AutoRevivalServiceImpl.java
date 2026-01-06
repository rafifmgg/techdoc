package com.ocmsintranet.cronservice.framework.workflows.autorevival.services;

import com.ocmsintranet.cronservice.framework.workflows.autorevival.jobs.AutoRevivalJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for Auto-Revival workflow
 *
 * Orchestrates the execution of the auto-revival job
 */
@Slf4j
@Service
public class AutoRevivalServiceImpl implements AutoRevivalService {

    private final AutoRevivalJob autoRevivalJob;

    public AutoRevivalServiceImpl(
            @org.springframework.beans.factory.annotation.Qualifier("suspension_auto_revival") AutoRevivalJob autoRevivalJob) {
        this.autoRevivalJob = autoRevivalJob;
    }

    @Override
    public CompletableFuture<String> executeJob() {
        log.info("Executing auto-revival job via service");

        return autoRevivalJob.execute()
                .thenApply(result -> {
                    log.info("Auto-revival job completed with result: {}", result.getMessage());
                    return result.getMessage();
                })
                .exceptionally(e -> {
                    log.error("Error executing auto-revival job: {}", e.getMessage(), e);
                    return "Error executing auto-revival job: " + e.getMessage();
                });
    }
}
