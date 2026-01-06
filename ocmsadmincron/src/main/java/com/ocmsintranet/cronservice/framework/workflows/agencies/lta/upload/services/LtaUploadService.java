package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for LTA upload workflow.
 * Provides methods to execute the workflow and handle SLIFT callbacks.
 */
public interface LtaUploadService {

    /**
     * Execute the LTA upload workflow.
     * This will query valid offence notices, generate a file, encrypt it,
     * and upload it to Azure Blob Storage and LTA SFTP server.
     * 
     * @return CompletableFuture with the job result
     */
    CompletableFuture<JobResult> executeLtaUpload();    
}
