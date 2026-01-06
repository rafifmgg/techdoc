package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.jobs.LtaUploadJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for LTA upload workflow.
 * Delegates actual work to LtaUploadJob using requestId-based callbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LtaUploadServiceImpl implements LtaUploadService, InitializingBean, DisposableBean {

    private final LtaUploadJob ltaUploadJob;
        
    /**
     * Service initialization - no longer needs workflow service registration
     * since we use requestId-based callbacks
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("LTA upload service initialized - using requestId-based callbacks");
    }
    
    /**
     * Service cleanup - no longer needs workflow service unregistration
     * since we use requestId-based callbacks
     */
    @Override
    public void destroy() throws Exception {
        log.info("LTA upload service shutting down");
    }

    @Override
    public CompletableFuture<JobResult> executeLtaUpload() {
        log.info("Executing LTA upload workflow");
        
        // Execute the job and return the CompletableFuture directly
        return ltaUploadJob.execute();
    }
}
