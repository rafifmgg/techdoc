package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.services;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for MHA NRIC download operations.
 */
public interface MhaNricDownloadService {
    
    /**
     * Execute the MHA NRIC download job.
     * 
     * @return CompletableFuture with the job result
     */
    CompletableFuture<Boolean> executeJob();
    
    /**
     * Handle SLIFT token callback for decryption.
     * 
     * @param requestId The request ID for which the token was received
     * @param token The received token
     * @return true if the callback was handled successfully
     */
    boolean handleComcryptCallback(String requestId, String token);
}
