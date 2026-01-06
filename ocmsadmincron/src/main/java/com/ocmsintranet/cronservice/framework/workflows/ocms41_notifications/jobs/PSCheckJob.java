package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.jobs;

import com.ocmsintranet.cronservice.framework.core.CronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PSCheckResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services.Ocms41NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Job to check if notices are permanently suspended during the review period
 * Based on OCMS 41 requirements
 *
 * Schedule: Configurable (e.g., multiple times per day)
 * Purpose: Check if notices have active permanent suspension (PS) during review
 *          and auto-reject applications if suspension is detected
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PSCheckJob extends CronJobTemplate {

    private final Ocms41NotificationService notificationService;

    @Override
    protected String getJobName() {
        return "OCMS41_PS_CHECK";
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
        log.info("Initializing OCMS 41 PS check job");
    }

    @Override
    protected JobResult doExecute() {
        try {
            log.info("Starting to check for permanently suspended notices during review");

            // Check for permanent suspensions and auto-reject applications
            PSCheckResult result = notificationService.checkPermanentSuspensions();

            if (result.isSuccess()) {
                String message = String.format(
                    "PS check completed. Checked: %d, Suspended notices found: %d, Applications auto-rejected: %d",
                    result.getTotalChecked(),
                    result.getSuspendedNotices(),
                    result.getRejectedApplications()
                );
                log.info(message);
                return new JobResult(true, message);
            } else {
                log.error("Failed to complete PS check: {}", result.getMessage());
                return new JobResult(false, result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error executing PS check job", e);
            return new JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up OCMS 41 PS check job");
    }
}
