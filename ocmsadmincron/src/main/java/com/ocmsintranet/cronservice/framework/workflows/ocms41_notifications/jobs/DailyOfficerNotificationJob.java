package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.jobs;

import com.ocmsintranet.cronservice.framework.core.CronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.EmailNotificationResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services.Ocms41NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Daily job to send email notifications to officers about pending furnish applications
 * Based on OCMS 41 User Story 41.8 and 41.35
 *
 * Schedule: Daily at 9:00 AM
 * Purpose: Alert officers of pending/resubmission applications requiring manual approval
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyOfficerNotificationJob extends CronJobTemplate {

    private final Ocms41NotificationService notificationService;

    @Override
    protected String getJobName() {
        return "OCMS41_DAILY_OFFICER_NOTIFICATION";
    }

    @Override
    protected boolean validatePreConditions() {
        // Check if notification service is available
        if (notificationService == null) {
            log.error("Notification service is not available");
            return false;
        }
        return true;
    }

    @Override
    protected void initialize() {
        log.info("Initializing OCMS 41 daily officer notification job");
    }

    @Override
    protected JobResult doExecute() {
        try {
            log.info("Starting to process pending applications for officer notification");

            // Get pending applications and send notification email
            EmailNotificationResult result = notificationService.sendDailyOfficerNotification();

            if (result.isSuccess()) {
                String message = String.format(
                    "Successfully sent notifications. Total pending: %d, Emails sent: %d, Failed: %d",
                    result.getTotalPendingApplications(),
                    result.getEmailsSent(),
                    result.getEmailsFailed()
                );
                log.info(message);
                return new JobResult(true, message);
            } else {
                log.error("Failed to send officer notifications: {}", result.getMessage());
                return new JobResult(false, result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error executing daily officer notification job", e);
            return new JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up OCMS 41 daily officer notification job");
    }
}
