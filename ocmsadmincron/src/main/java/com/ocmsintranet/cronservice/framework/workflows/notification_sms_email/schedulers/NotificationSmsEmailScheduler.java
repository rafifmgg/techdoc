package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services.NotificationSmsEmailService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for the Notification SMS and Email workflow.
 * This class contains scheduled methods for both main and retry flows.
 */
@Slf4j
@Component
public class NotificationSmsEmailScheduler {

    private final NotificationSmsEmailService notificationService;
    
    @Value("${cron.enareminder.enabled:false}")
    private boolean schedulerEnabled;

    public NotificationSmsEmailScheduler(NotificationSmsEmailService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Scheduled execution of the main notification flow
     */
    @Scheduled(cron = "${cron.enareminder.schedule:0 0 10 * * ?}") // Default: 10 AM daily
    @SchedulerLock(name = "${cron.enareminder.shedlock.name:send_ena_reminder}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduleMainNotification() {
        if (!schedulerEnabled) {
            log.info("Notification scheduler is disabled. Skipping scheduled execution.");
            return;
        }
        
        log.info("Scheduled execution of notification SMS and email workflow");
        
        notificationService.processMainFlow()
                .thenAccept(result -> log.info("Scheduled notification workflow completed with result: {}", result))
                .exceptionally(ex -> {
                    log.error("Error in scheduled notification workflow: {}", ex.getMessage(), ex);
                    return null;
                });
    }

    /**
     * Scheduled execution of the retry notification flow
     */
    @Scheduled(cron = "${cron.enareminder.retry.schedule:0 0 14 * * ?}") // Default: 2 PM daily
    @SchedulerLock(name = "${cron.enareminder.retry.shedlock.name:send_ena_reminder_retry}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduleRetryNotification() {
        if (!schedulerEnabled) {
            log.info("Notification retry scheduler is disabled. Skipping scheduled execution.");
            return;
        }
        
        log.info("Scheduled execution of retry notification SMS and email workflow");
        
        notificationService.processRetryFlow()
                .thenAccept(result -> log.info("Scheduled retry notification workflow completed with result: {}", result))
                .exceptionally(ex -> {
                    log.error("Error in scheduled retry notification workflow: {}", ex.getMessage(), ex);
                    return null;
                });
    }
}
