package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.jobs.NotificationSmsEmailJob;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.jobs.NotificationSmsEmailRetryJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for Notification SMS and Email workflow.
 * This service handles both main and retry flows.
 */
@Slf4j
@Service
public class NotificationSmsEmailServiceImpl implements NotificationSmsEmailService {

    private final NotificationSmsEmailJob notificationJob;
    private final NotificationSmsEmailRetryJob notificationRetryJob;

    public NotificationSmsEmailServiceImpl(
            @org.springframework.beans.factory.annotation.Qualifier("send_ena_reminder") NotificationSmsEmailJob notificationJob,
            @org.springframework.beans.factory.annotation.Qualifier("send_ena_reminder_retry") NotificationSmsEmailRetryJob notificationRetryJob) {
        this.notificationJob = notificationJob;
        this.notificationRetryJob = notificationRetryJob;
    }

    @Override
    public CompletableFuture<String> processNotifications(boolean isRetry) {
        return isRetry ? processRetryFlow() : processMainFlow();
    }

    @Override
    public CompletableFuture<String> processMainFlow() {
        log.info("Processing main notification flow");
        
        return notificationJob.execute()
                .thenApply(result -> {
                    log.info("Main notification flow completed with result: {}", result);
                    return result.getMessage();
                })
                .exceptionally(e -> {
                    log.error("Error processing main notification flow: {}", e.getMessage(), e);
                    return "Error processing main notification flow: " + e.getMessage();
                });
    }

    @Override
    public CompletableFuture<String> processRetryFlow() {
        log.info("Processing retry notification flow");
        
        return notificationRetryJob.execute()
                .thenApply(result -> {
                    log.info("Retry notification flow completed with result: {}", result);
                    return result.getMessage();
                })
                .exceptionally(e -> {
                    log.error("Error processing retry notification flow: {}", e.getMessage(), e);
                    return "Error processing retry notification flow: " + e.getMessage();
                });
    }
}
