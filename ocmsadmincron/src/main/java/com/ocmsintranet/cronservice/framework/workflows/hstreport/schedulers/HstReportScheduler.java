package com.ocmsintranet.cronservice.framework.workflows.hstreport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.services.HstReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Monthly HST processing.
 * This scheduler runs on the 1st of every month at midnight to queue HST IDs for MHA/DataHive checks.
 *
 * The Monthly HST scheduler is responsible for:
 * 1. Queuing all HST IDs for MHA/DataHive address verification (runs on 1st of month)
 * 2. These queued IDs will be processed by existing MHA/DataHive CRONs
 * 3. Reports are generated separately after MHA/DataHive results are received
 *
 * Features:
 * - Runs automatically on 1st of every month at midnight (configurable via cron.hst.query.schedule property)
 * - Can be enabled/disabled via cron.hst.query.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Queues HST IDs to ocms_temp_nro_queue with queryReason='HST'
 *
 * Configuration properties:
 * - cron.hst.query.enabled: Enable/disable the scheduler (default: false)
 * - cron.hst.query.schedule: Cron expression for scheduling (default: 0 0 0 1 * ?)
 * - cron.hst.query.shedlock.name: Name for ShedLock (default: monthly_hst_query)
 */
@Slf4j
@Component
public class HstReportScheduler {

    private final HstReportService hstReportService;

    @Value("${cron.hst.query.enabled:false}")
    private boolean enabled;

    public HstReportScheduler(HstReportService hstReportService) {
        this.hstReportService = hstReportService;
    }

    /**
     * Scheduled execution of the Monthly HST ID queuing job.
     * The schedule is configured to run on the 1st of every month at midnight (0 0 0 1 * ?).
     *
     * This job queues all HST IDs for MHA/DataHive verification.
     * Actual reports are generated later by HstReportGeneratorScheduler after results are received.
     */
    @Scheduled(cron = "${cron.hst.query.schedule:0 0 0 1 * ?}")
    @SchedulerLock(name = "${cron.hst.query.shedlock.name:monthly_hst_query}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("Monthly HST ID queuing job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of Monthly HST ID queuing job");

        try {
            int queuedCount = hstReportService.queueHstIdsForMonthlyCheck();
            log.info("Monthly HST ID queuing job completed successfully. Queued {} HST IDs for MHA/DataHive check", queuedCount);
        } catch (Exception ex) {
            log.error("Exception occurred during Monthly HST ID queuing job execution", ex);
        }
    }
}
