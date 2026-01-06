package com.ocmsintranet.cronservice.framework.workflows.paymentReport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.DailyPaidReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Daily Paid Report generation.
 * This scheduler runs daily at 3 AM to generate reports for yesterday's data.
 *
 * The Daily Paid Report scheduler is responsible for automatically generating daily reports
 * containing information about paid notices categorized by payment method:
 * - eService transactions
 * - AXS transactions
 * - Offline transactions
 * - JTC Collections (FP reason)
 * - Refund Records
 *
 * These reports are useful for daily reconciliation and auditing purposes.
 *
 * Features:
 * - Runs automatically at 3 AM every day (configurable via cron.daily.paid.report.schedule property)
 * - Can be enabled/disabled via cron.daily.paid.report.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel report with 6 sheets categorized by payment channel
 * - Sends email with Excel attachment to OIC
 * - Sends failure notification to support team on errors
 *
 * Configuration properties:
 * - cron.paymentreport.daily.paid.enabled: Enable/disable the scheduler (default: false)
 * - cron.paymentreport.daily.paid.schedule: Cron expression for scheduling (default: 0 0 3 * * ? - 3 AM daily)
 * - cron.paymentreport.daily.paid.shedlock.name: Name for ShedLock (default: generate_daily_paid_rpt)
 * - email.report.paymentreport.recipients: Email recipients for reports (shared across all payment reports)
 * - email.report.paymentreport.support.recipients: Email recipients for failed reports
 */
@Slf4j
@Component
public class DailyPaidReportScheduler {

    private final DailyPaidReportService reportService;

    @Value("${cron.paymentreport.daily.paid.enabled:false}")
    private boolean enabled;

    public DailyPaidReportScheduler(DailyPaidReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Scheduled execution of the Daily Paid Report generation job.
     * The schedule is configured to run at 3 AM every day.
     */
    @Scheduled(cron = "${cron.paymentreport.daily.paid.schedule:0 0 3 * * ?}")
    @SchedulerLock(name = "${cron.paymentreport.daily.paid.shedlock.name:generate_daily_paid_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT2H")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Daily Paid Report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Daily Paid Report generation job");

        reportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Daily Paid Report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Daily Paid Report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Daily Paid Report generation job execution", ex);
                    return null;
                });
    }
}
