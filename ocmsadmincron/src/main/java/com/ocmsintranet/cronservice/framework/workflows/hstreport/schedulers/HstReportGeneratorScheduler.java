package com.ocmsintranet.cronservice.framework.workflows.hstreport.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.services.HstReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for HST Report Generation.
 * This scheduler runs daily to check if MHA/DataHive results are ready and generates reports.
 *
 * The HST Report Generator scheduler is responsible for:
 * 1. Checking if new MHA/DataHive results are available in ocms_temp_unc_hst_addr
 * 2. Generating Monthly HST Data Report from the results
 * 3. Generating Monthly HST Work Items Report with statistics
 * 4. Emailing reports to OIC
 *
 * This runs AFTER HstReportScheduler has queued HST IDs and MHA/DataHive CRONs have processed them.
 *
 * Features:
 * - Runs daily at 02:00 AM (after MHA/DataHive CRONs complete)
 * - Can be enabled/disabled via cron.hst.report.generator.enabled property
 * - Uses ShedLock to ensure only one instance runs at a time
 * - Only generates reports when new results are available
 *
 * Configuration properties:
 * - cron.hst.report.generator.enabled: Enable/disable the scheduler (default: false)
 * - cron.hst.report.generator.schedule: Cron expression for scheduling (default: 0 0 2 * * ?)
 * - cron.hst.report.generator.shedlock.name: Name for ShedLock (default: hst_report_generator)
 */
@Slf4j
@Component
public class HstReportGeneratorScheduler {

    private final HstReportService hstReportService;

    @Value("${cron.hst.report.generator.enabled:false}")
    private boolean enabled;

    public HstReportGeneratorScheduler(HstReportService hstReportService) {
        this.hstReportService = hstReportService;
    }

    /**
     * Scheduled execution of the HST report generation job.
     * The schedule is configured to run daily at 02:00 AM (0 0 2 * * ?).
     *
     * This job checks if new MHA/DataHive results are available and generates reports if ready.
     */
    @Scheduled(cron = "${cron.hst.report.generator.schedule:0 0 2 * * ?}")
    @SchedulerLock(name = "${cron.hst.report.generator.shedlock.name:hst_report_generator}", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void scheduledExecution() {
        if (!enabled) {
            log.info("HST report generator job is disabled. Skipping scheduled execution.");
            return;
        }

        log.info("Starting scheduled execution of HST report generator job");

        try {
            ReportResult result = hstReportService.checkAndGenerateReports();

            if (result == null) {
                log.info("No new MHA/DataHive results available. Skipping report generation.");
            } else if (result.isSuccess()) {
                log.info("HST report generator job completed successfully: {}", result.getDetailedMessage());
            } else {
                log.error("HST report generator job failed: {}", result.getMessage());
            }
        } catch (Exception ex) {
            log.error("Exception occurred during HST report generator job execution", ex);
        }
    }
}
