package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Notification SMS and Email workflow.
 * This interface defines methods for processing both main and retry flows.
 */
public interface NotificationSmsEmailService {
    
    /**
     * Process the notification workflow (either main or retry flow)
     * 
     * @param isRetry true to run in retry mode, false for main mode
     * @return CompletableFuture with the result of the job execution
     */
    CompletableFuture<String> processNotifications(boolean isRetry);
    
    /**
     * Process the main notification flow
     * 
     * @return CompletableFuture with the result of the job execution
     */
    CompletableFuture<String> processMainFlow();
    
    /**
     * Process the retry notification flow
     * 
     * @return CompletableFuture with the result of the job execution
     */
    CompletableFuture<String> processRetryFlow();
}
