package com.ocmsintranet.cronservice.framework.workflows.paymentReport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.PaymentExceptionReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Payment Exception Report generation.
 * This scheduler runs daily at 4 AM to generate reports for payment exceptions.
 *
 * The Payment Exception Report scheduler is responsible for automatically generating daily reports
 * containing information about payment exceptions including:
 * - PS-PRA: Permanent Suspension - Payment Refund Approved
 * - TS-PAM: Temporary Suspension - Payment Amount Mismatch
 * - Refund notices created on the report date
 *
 * These reports are useful for monitoring and auditing payment-related issues.
 *
 * Features:
 * - Runs automatically at 4 AM every day (configurable via cron.paymentreport.exception.schedule property)
 * - Can be enabled/disabled via cron.paymentreport.exception.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Sends HTML email with exception records in table format
 * - Different email recipients for success vs failure cases
 *
 * Configuration properties:
 * - cron.paymentreport.exception.enabled: Enable/disable the scheduler (default: false)
 * - cron.paymentreport.exception.schedule: Cron expression for scheduling (default: 0 0 4 * * ?)
 * - cron.paymentreport.exception.shedlock.name: Name for ShedLock (default: generate_payment_exception_rpt)
 * - email.report.paymentreport.recipients: Email recipients for reports (shared across all payment reports)
 * - email.report.paymentreport.support.recipients: Email recipients for failed reports
 */
@Slf4j
@Component
public class PaymentExceptionReportScheduler {

    private final PaymentExceptionReportService reportService;

    @Value("${cron.paymentreport.exception.enabled:false}")
    private boolean enabled;

    public PaymentExceptionReportScheduler(PaymentExceptionReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Scheduled execution of the Payment Exception Report generation job.
     * The schedule is configured to run at 4 AM every day.
     */
    @Scheduled(cron = "${cron.paymentreport.exception.schedule:0 0 4 * * ?}")
    @SchedulerLock(name = "${cron.paymentreport.exception.shedlock.name:generate_payment_exception_rpt}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Payment Exception Report generation job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Payment Exception Report generation job");

        reportService.executeJob()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Scheduled Payment Exception Report generation job completed successfully: {}", result.getDetailedMessage());
                    } else {
                        log.error("Scheduled Payment Exception Report generation job failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception occurred during scheduled Payment Exception Report generation job execution", ex);
                    return null;
                });
    }
}
