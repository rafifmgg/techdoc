package com.ocmsintranet.cronservice.framework.workflows.daily_reports.services.impl;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs.AnsLetterReconciliationJob;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs.RipHirerDriverReportJob;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs.ClassifiedVehicleReportJob;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs.DipMidForRecheckJob;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.services.DailyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * OCMS 14 & OCMS 10: Service implementation for Daily Report generation jobs
 */
@Slf4j
@Service
public class DailyReportServiceImpl implements DailyReportService {

    private final RipHirerDriverReportJob ripHirerDriverReportJob;
    private final ClassifiedVehicleReportJob classifiedVehicleReportJob;
    private final AnsLetterReconciliationJob ansLetterReconciliationJob;
    private final DipMidForRecheckJob dipMidForRecheckJob;

    public DailyReportServiceImpl(
            RipHirerDriverReportJob ripHirerDriverReportJob,
            ClassifiedVehicleReportJob classifiedVehicleReportJob,
            AnsLetterReconciliationJob ansLetterReconciliationJob,
            DipMidForRecheckJob dipMidForRecheckJob) {
        this.ripHirerDriverReportJob = ripHirerDriverReportJob;
        this.classifiedVehicleReportJob = classifiedVehicleReportJob;
        this.ansLetterReconciliationJob = ansLetterReconciliationJob;
        this.dipMidForRecheckJob = dipMidForRecheckJob;
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeRipHirerDriverReport() {
        log.info("[Daily Report Service] Executing RIP Hirer/Driver Report job (async)");
        return ripHirerDriverReportJob.execute();
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeClassifiedVehicleReport() {
        log.info("[Daily Report Service] Executing Classified Vehicle Report job (async)");
        return classifiedVehicleReportJob.execute();
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeRipHirerDriverReport(String reportDate) {
        log.info("[Daily Report Service] Executing RIP Hirer/Driver Report job for date: {} (async)", reportDate);
        ripHirerDriverReportJob.setReportDate(reportDate);
        return ripHirerDriverReportJob.execute();
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeClassifiedVehicleReport(String reportDate) {
        log.info("[Daily Report Service] Executing Classified Vehicle Report job for date: {} (async)", reportDate);
        classifiedVehicleReportJob.setReportDate(reportDate);
        return classifiedVehicleReportJob.execute();
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeAnsLetterReconciliation(String processDate, String ackFileContent) {
        log.info("[Daily Report Service] Executing ANS Letter Reconciliation job for date: {} (async)", processDate);
        ansLetterReconciliationJob.setReconciliationData(processDate, ackFileContent);
        return ansLetterReconciliationJob.execute();
    }

    @Override
    @Async
    public CompletableFuture<CronJobFramework.CronJobTemplate.JobResult> executeDipMidForRecheck() {
        log.info("[Daily Report Service] Executing DIP/MID/FOR day-end re-check job (async)");
        return dipMidForRecheckJob.execute();
    }
}
