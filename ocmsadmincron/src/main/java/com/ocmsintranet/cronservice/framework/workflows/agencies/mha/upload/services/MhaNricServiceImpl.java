package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.jobs.MhaNricUploadJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for MHA NRIC verification workflow.
 * Delegates actual work to MhaNricUploadJob using requestId-based callbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MhaNricServiceImpl implements MhaNricService, InitializingBean, DisposableBean {

    private final MhaNricUploadJob mhaNricUploadJob;
        
    /**
     * Service initialization - no longer needs workflow service registration
     * since we use requestId-based callbacks
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("MHA NRIC service initialized - using requestId-based callbacks");
    }
    
    /**
     * Service cleanup - no longer needs workflow service unregistration
     * since we use requestId-based callbacks
     */
    @Override
    public void destroy() throws Exception {
        log.info("MHA NRIC service shutting down");
    }

    @Override
    public CompletableFuture<JobResult> executeMhaNricUpload() {
        log.info("Executing MHA NRIC upload workflow");
        
        // Execute the job and return the CompletableFuture directly
        return mhaNricUploadJob.execute();
    }
    
    /**
     * Handle SLIFT token callback
     * This method is called by the SliftCallbackHelper when a token is received
     * It delegates to the MhaNricUploadJob which contains the actual workflow logic
     * 
     * @param requestId The request ID for which the token was received
     * @param token The received token
     * @return true if the callback was handled successfully
     */
    public boolean handleComcryptCallback(String requestId, String token) {
        log.info("Received SLIFT token callback for requestId: {} - delegating to job", requestId);
        
        if (requestId == null || requestId.isEmpty()) {
            log.error("Invalid SLIFT callback: requestId is null or empty");
            return false;
        }
        
        if (token == null || token.isEmpty()) {
            log.error("Invalid SLIFT callback: token is null or empty for requestId: {}", requestId);
            return false;
        }
        
        try {
            // Delegate to the job to handle the callback
            mhaNricUploadJob.handleComcryptCallback(requestId, token);
            log.info("Successfully processed SLIFT token callback for MHA NRIC workflow: requestId={}", requestId);
            return true;
            
        } catch (Exception e) {
            log.error("Error handling SLIFT callback for requestId: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }
}