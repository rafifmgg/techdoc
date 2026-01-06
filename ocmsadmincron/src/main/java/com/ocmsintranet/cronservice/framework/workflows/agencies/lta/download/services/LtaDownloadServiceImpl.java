package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.jobs.LtaDownloadJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of LtaDownloadService.
 * Handles LTA download operations using requestId-based callbacks.
 */
@Slf4j
@Service
public class LtaDownloadServiceImpl implements LtaDownloadService, InitializingBean, DisposableBean {
    
    private final LtaDownloadJob ltaDownloadJob;
        
    public LtaDownloadServiceImpl(LtaDownloadJob ltaDownloadJob) {
        this.ltaDownloadJob = ltaDownloadJob;
    }
    
    @Override
    public CompletableFuture<JobResult> executeLtaDownloadJob() {
        log.info("Executing LTA download job through service");
        
        return ltaDownloadJob.execute()
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        log.info("LTA download job completed successfully: {}", result.getMessage());
                        return result;
                    } else {
                        log.error("LTA download job failed: {}", result.getMessage());
                        return result;
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during LTA download job execution", ex);
                    return new JobResult(false, "Exception during job execution: " + ex.getMessage());
                });
    }
        
    /**
     * Service initialization - no longer needs workflow service registration
     * since we use requestId-based callbacks
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("LTA download service initialized - using requestId-based callbacks");
    }
    
    /**
     * Service cleanup - no longer needs workflow service unregistration
     * since we use requestId-based callbacks
     */
    @Override
    public void destroy() throws Exception {
        log.info("LTA download service shutting down");
    }
}