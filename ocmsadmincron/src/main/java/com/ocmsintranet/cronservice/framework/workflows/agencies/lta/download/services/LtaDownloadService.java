package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for LTA download operations.
 * This service handles downloading and processing LTA response files.
 */
public interface LtaDownloadService {
    
    /**
     * Execute the LTA download job.
     * Downloads and processes LTA response files from SFTP.
     * 
     * @return CompletableFuture with job execution result
     */
    CompletableFuture<JobResult> executeLtaDownloadJob();    
}