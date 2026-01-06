package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.services;

import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.jobs.ToppanUploadJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Service to handle SLIFT callbacks for Toppan workflow.
 * Uses requestId-based callbacks and delegates to ToppanUploadJob.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToppanSliftCallbackService implements InitializingBean, DisposableBean {

    private final ToppanUploadJob toppanUploadJob;
        
    /**
     * Service initialization - no longer needs workflow service registration
     * since we use requestId-based callbacks
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Toppan SLIFT callback service initialized - using requestId-based callbacks");
    }
    
    /**
     * Service cleanup - no longer needs workflow service unregistration
     * since we use requestId-based callbacks
     */
    @Override
    public void destroy() throws Exception {
        log.info("Toppan SLIFT callback service shutting down");
    }

    /**
     * Handle SLIFT token callback
     * This method is called by the SliftCallbackHelper when a token is received
     * It delegates to the ToppanUploadJob which contains the actual workflow logic
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
            toppanUploadJob.handleComcryptCallback(requestId, token);
            log.info("Successfully processed SLIFT token callback for Toppan workflow: requestId={}", requestId);
            return true;
            
        } catch (Exception e) {
            log.error("Error handling SLIFT callback for requestId: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }
}