package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for MHA NRIC verification workflow.
 */
public interface MhaNricService {
    
    /**
     * Execute the MHA NRIC upload workflow
     * 
     * @return CompletableFuture with the job result
     */
    CompletableFuture<JobResult> executeMhaNricUpload();
}
