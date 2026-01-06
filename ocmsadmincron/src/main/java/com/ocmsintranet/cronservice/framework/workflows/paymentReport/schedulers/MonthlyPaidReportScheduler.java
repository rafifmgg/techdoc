package com.ocmsintranet.cronservice.framework.workflows.paymentReport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.MonthlyPaidReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Monthly Paid Report generation.
 * This scheduler runs monthly on the 1st day at 3 AM to generate reports for the previous month.
 *
 * The Monthly Paid Report scheduler is responsible for automatically generating monthly reports
 * containing information about paid notices categorized by payment method:
 * - eService transactions
 * - AXS transactions
 * - Offline transactions
 * - JTC Collections (FP reason)
 * - Refund Records
 *
 * These reports are useful for monthly reconciliation and auditing purposes.
 *
 * Features:
 * - Runs automatically at 3 AM on the 1st of every month (configurable via cron.paymentreport.monthly.paid.schedule property)
 * - Can be enabled/disabled via cron.paymentreport.monthly.paid.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Generates Excel report with 6 sheets categorized by payment channel
 * - Sends email with Excel attachment to OIC
 * - Sends failure notification to support team on errors
 *
 * Configuration properties:
 * - cron.paymentreport.monthly.paid.enabled: Enable/disable the scheduler (default: false)
 * - cron.paymentreport.monthly.paid.schedule: Cron expression for scheduling (default: 0 0 3 1 * ? - 3 AM on 1st of month)
 * - cron.paymentreport.monthly.paid.shedlock.name: Name for ShedLock (default: generate_monthly_paid_rpt)
 * - email.report.paymentreport.recipients: Email recipients for reports (shared across all payment reports)
 * - email.report.paymentreport.support.recipients: Email recipients for failed reports
 */
@Slf4j
@Component
public class MonthlyPaidReportScheduler {

    private final MonthlyPaidReportService reportService;

    @Value("${cron.paymentreport.monthly.paid.enabled:false}")
    private boolean enabled;

    public MonthlyPaidReportScheduler(MonthlyPaidReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Scheduled execution of the Monthly Paid Report generation job.
     * The schedule is configured to run at 3 AM on the 1st of every month.
     */
    @Scheduled(cron = "${cron.paymentreport.monthly.paid.schedule:0 0 3 1 * ?}")
    @SchedulerLock(name = "${cron.paymentreport.monthly.paid.shedlock.name:generate_monthly_paid_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT2H")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Monthly Paid Report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Monthly Paid Report generation job");

        reportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Monthly Paid Report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Monthly Paid Report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Monthly Paid Report generation job execution", ex);
                    return null;
                });
    }
}
