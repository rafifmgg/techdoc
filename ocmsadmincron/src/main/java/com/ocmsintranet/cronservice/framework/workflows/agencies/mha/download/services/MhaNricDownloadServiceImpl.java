package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.services;

import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.jobs.MhaNricDownloadJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MhaNricDownloadService.
 * Handles MHA NRIC download operations using requestId-based callbacks.
 */
@Slf4j
@Service
public class MhaNricDownloadServiceImpl implements MhaNricDownloadService, InitializingBean, DisposableBean {

    private final MhaNricDownloadJob mhaNricDownloadJob;
        
    public MhaNricDownloadServiceImpl(MhaNricDownloadJob mhaNricDownloadJob) {
        this.mhaNricDownloadJob = mhaNricDownloadJob;
    }

    @Override
    public CompletableFuture<Boolean> executeJob() {
        log.info("Executing MHA NRIC download job");
        
        return mhaNricDownloadJob.execute()
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        log.info("MHA NRIC download job completed successfully: {}", result.getMessage());
                        return true;
                    } else {
                        log.error("MHA NRIC download job failed: {}", result.getMessage());
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during MHA NRIC download job execution", ex);
                    return false;
                });
    }

    @Override
    public boolean handleComcryptCallback(String requestId, String token) {
        log.info("Handling SLIFT token callback for request ID: {}", requestId);
        try {
            mhaNricDownloadJob.handleComcryptCallback(requestId, token);
            return true;
        } catch (Exception e) {
            log.error("Error handling SLIFT token callback", e);
            return false;
        }
    }
    
    /**
     * Service initialization - no longer needs workflow service registration
     * since we use requestId-based callbacks
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("MHA NRIC download service initialized - using requestId-based callbacks");
    }
    
    /**
     * Service cleanup - no longer needs workflow service unregistration
     * since we use requestId-based callbacks
     */
    @Override
    public void destroy() throws Exception {
        log.info("MHA NRIC download service shutting down");
    }
}
