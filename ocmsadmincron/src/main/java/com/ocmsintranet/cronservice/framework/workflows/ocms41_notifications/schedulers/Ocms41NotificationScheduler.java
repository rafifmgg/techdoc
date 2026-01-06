package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services.Ocms41NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for OCMS 41 notification workflows
 *
 * Schedules:
 * 1. Daily 9am email to officers (pending applications)
 * 2. PS check cron job (check if notice permanently suspended during review)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ocms41NotificationScheduler {

    private final Ocms41NotificationService notificationService;

    @Value("${cron.ocms41.notification.enabled:false}")
    private boolean notificationEnabled;

    @Value("${cron.ocms41.pscheck.enabled:false}")
    private boolean psCheckEnabled;

    /**
     * Daily email notification to officers at 9:00 AM
     * Sends consolidated list of pending/resubmission applications requiring manual approval
     * Based on OCMS 41 User Story 41.8 and 41.35
     */
    @Scheduled(cron = "${cron.ocms41.notification.schedule:0 0 9 * * ?}") // Default: 9 AM daily
    @SchedulerLock(
        name = "${cron.ocms41.notification.shedlock.name:ocms41_daily_officer_notification}",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT30M"
    )
    public void scheduleDailyOfficerNotification() {
        if (!notificationEnabled) {
            log.debug("OCMS 41 officer notification scheduler is disabled. Skipping execution.");
            return;
        }

        log.info("Starting scheduled OCMS 41 daily officer notification");

        try {
            var result = notificationService.sendDailyOfficerNotification();

            if (result.isSuccess()) {
                log.info("Daily officer notification completed successfully. " +
                    "Pending: {}, Emails sent: {}, Failed: {}",
                    result.getTotalPendingApplications(),
                    result.getEmailsSent(),
                    result.getEmailsFailed()
                );
            } else {
                log.error("Daily officer notification failed: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("Error in scheduled daily officer notification", e);
        }
    }

    /**
     * Permanent Suspension check job
     * Checks if notices have active permanent suspension during review period
     * Auto-rejects applications if suspension is detected
     *
     * Default: Runs every 2 hours during business hours (8am-6pm)
     */
    @Scheduled(cron = "${cron.ocms41.pscheck.schedule:0 0 8-18/2 * * ?}") // Default: Every 2 hours, 8am-6pm
    @SchedulerLock(
        name = "${cron.ocms41.pscheck.shedlock.name:ocms41_ps_check}",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT30M"
    )
    public void schedulePSCheck() {
        if (!psCheckEnabled) {
            log.debug("OCMS 41 PS check scheduler is disabled. Skipping execution.");
            return;
        }

        log.info("Starting scheduled OCMS 41 PS check");

        try {
            var result = notificationService.checkPermanentSuspensions();

            if (result.isSuccess()) {
                log.info("PS check completed successfully. " +
                    "Checked: {}, Suspended: {}, Auto-rejected: {}",
                    result.getTotalChecked(),
                    result.getSuspendedNotices(),
                    result.getRejectedApplications()
                );
            } else {
                log.error("PS check failed: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("Error in scheduled PS check", e);
        }
    }
}
