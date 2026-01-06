package com.ocmsintranet.cronservice.framework.workflows.daily_reports.schedulers;

import com.ocmsintranet.cronservice.framework.workflows.daily_reports.services.DailyReportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OCMS 14 & OCMS 19: Scheduler for Daily Report generation jobs
 * Schedules:
 * 1. RIP Hirer/Driver Report at 02:00 AM daily
 * 2. Classified Vehicle Report at 08:00 AM daily (configurable)
 * 3. DIP/MID/FOR Day-End Re-check at 11:59 PM daily (OCMS 19)
 */
@Slf4j
@Component
public class DailyReportScheduler {

    private final DailyReportService dailyReportService;

    @Value("${cron.daily.reports.rip.enabled:false}")
    private boolean ripReportEnabled;

    @Value("${cron.daily.reports.classified.enabled:false}")
    private boolean classifiedReportEnabled;

    @Value("${cron.daily.dipmidfor.recheck.enabled:false}")
    private boolean dipMidForRecheckEnabled;

    public DailyReportScheduler(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /**
     * Scheduled execution of RIP Hirer/Driver Report
     * Default: 02:00 AM daily
     */
    @Scheduled(cron = "${cron.daily.reports.rip.schedule:0 0 2 * * ?}")
    @SchedulerLock(
        name = "generate_rip_hirer_driver_report",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT1H"
    )
    public void scheduleRipHirerDriverReport() {
        if (!ripReportEnabled) {
            log.debug("[Daily Report Scheduler] RIP Hirer/Driver Report is disabled. Skipping execution.");
            return;
        }

        log.info("[Daily Report Scheduler] Starting scheduled execution of RIP Hirer/Driver Report");

        dailyReportService.executeRipHirerDriverReport()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("[Daily Report Scheduler] RIP Hirer/Driver Report completed successfully: {}", result.getMessage());
                    } else {
                        log.error("[Daily Report Scheduler] RIP Hirer/Driver Report failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("[Daily Report Scheduler] Exception occurred during RIP Hirer/Driver Report execution", ex);
                    return null;
                });
    }

    /**
     * Scheduled execution of Classified Vehicle Report
     * Default: 08:00 AM daily
     */
    @Scheduled(cron = "${cron.daily.reports.classified.schedule:0 0 8 * * ?}")
    @SchedulerLock(
        name = "generate_classified_vehicle_report",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT1H"
    )
    public void scheduleClassifiedVehicleReport() {
        if (!classifiedReportEnabled) {
            log.debug("[Daily Report Scheduler] Classified Vehicle Report is disabled. Skipping execution.");
            return;
        }

        log.info("[Daily Report Scheduler] Starting scheduled execution of Classified Vehicle Report");

        dailyReportService.executeClassifiedVehicleReport()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("[Daily Report Scheduler] Classified Vehicle Report completed successfully: {}", result.getMessage());
                    } else {
                        log.error("[Daily Report Scheduler] Classified Vehicle Report failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("[Daily Report Scheduler] Exception occurred during Classified Vehicle Report execution", ex);
                    return null;
                });
    }

    /**
     * OCMS 19: Scheduled execution of DIP/MID/FOR Day-End Re-check
     * Runs at 11:59 PM daily (before midnight stage transition cron)
     * Re-applies PS-DIP/PS-MID/PS-FOR to diplomatic/military/foreign vehicles at RD2/DN2
     * Default: 11:59 PM daily
     */
    @Scheduled(cron = "${cron.daily.dipmidfor.recheck.schedule:0 59 23 * * ?}")
    @SchedulerLock(
        name = "ocms19_dip_mid_for_recheck",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT30M"
    )
    public void scheduleDipMidForRecheck() {
        if (!dipMidForRecheckEnabled) {
            log.debug("[Daily Report Scheduler] DIP/MID/FOR Day-End Re-check is disabled. Skipping execution.");
            return;
        }

        log.info("[Daily Report Scheduler] Starting scheduled execution of DIP/MID/FOR Day-End Re-check");

        dailyReportService.executeDipMidForRecheck()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("[Daily Report Scheduler] DIP/MID/FOR Day-End Re-check completed successfully: {}", result.getMessage());
                    } else {
                        log.error("[Daily Report Scheduler] DIP/MID/FOR Day-End Re-check failed: {}", result.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("[Daily Report Scheduler] Exception occurred during DIP/MID/FOR Day-End Re-check execution", ex);
                    return null;
                });
    }
}
