package com.ocmsintranet.cronservice.framework.workflows.agencies.repccs.schedulers;

import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.repccs.RepccsListedVehOcmsToRepccsService;
import com.ocmsintranet.cronservice.framework.services.repccs.RepccsOffenceRuleService;
import com.ocmsintranet.cronservice.framework.services.repccs.RepccsansvehService;
import com.ocmsintranet.cronservice.framework.services.repccs.RepccsnopoarcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class RepccsShedulerGeneratingFiles {

    private final BatchJobHelper batchJobHelper;
    private final RepccsansvehService repccsansvehService;
    private final RepccsListedVehOcmsToRepccsService repccsListedVehOcmsToRepccsService;
    private final RepccsnopoarcService repccsnopoarcService;
    private final RepccsOffenceRuleService repccsOffenceRuleService;

    @Value("${cron.repccs.upload.enabled:false}")
    private boolean enabled;

    @Value("${cron.repccs.upload.shedlock.name:}")
    private String rep_generate;

    public RepccsShedulerGeneratingFiles(BatchJobHelper batchJobHelper, RepccsansvehService repccsansvehService,
                                         RepccsListedVehOcmsToRepccsService repccsListedVehOcmsToRepccsService,
                                         RepccsnopoarcService repccsnopoarcService, RepccsOffenceRuleService repccsOffenceRuleService) {
        this.batchJobHelper = batchJobHelper;
        this.repccsansvehService = repccsansvehService;
        this.repccsListedVehOcmsToRepccsService = repccsListedVehOcmsToRepccsService;
        this.repccsnopoarcService = repccsnopoarcService;
        this.repccsOffenceRuleService = repccsOffenceRuleService;
    }

    @Scheduled(cron = "${cron.repccs.upload.schedule:}")
    @SchedulerLock(name = "generate_repcss_files", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void executeScheduledJob() {
        if (!enabled) {
            log.info("REPCCS scheduler is disabled. Skipping execution.");
            return;
        }
        runJob();
    }

    public OcmsBatchJob runJob() {
        log.info("Starting scheduled REPCCS Generate processing");
        OcmsBatchJob result = null;
        try {

            OcmsBatchJob batchJob = batchJobHelper.createInitialBatchJob(rep_generate);

            // Execute services ONE BY ONE (not in parallel) to avoid token conflicts
            // Each service: request token → wait for callback → encrypt → upload

            // ============ SERVICE 1: ANS Vehicle ============
            log.info("========== [1/4] Starting REPCCS ANS Vehicle service ==========");
            try {
                repccsansvehService.executeOcmsinansvehFunction(batchJob);
                log.info("========== [1/4] REPCCS ANS Vehicle service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [1/4] REPCCS ANS Vehicle service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[ANS Vehicle] FAILED: " + e.getMessage());
            }

            // Small delay between services to ensure clean token handling
            Thread.sleep(2000);

            // ============ SERVICE 2: Offence Rule ============
            log.info("========== [2/4] Starting REPCCS Offence Rule service ==========");
            try {
                repccsOffenceRuleService.executeUpdatedOffenceRuleFunction(batchJob);
                log.info("========== [2/4] REPCCS Offence Rule service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [2/4] REPCCS Offence Rule service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[Offence Rule] FAILED: " + e.getMessage());
            }

            // Small delay between services to ensure clean token handling
            Thread.sleep(2000);

            // ============ SERVICE 3: Listed Vehicle ============
            log.info("========== [3/4] Starting REPCCS Listed Vehicle service ==========");
            try {
                repccsListedVehOcmsToRepccsService.executeListedVehOcmsToRepccsFunction(batchJob);
                log.info("========== [3/4] REPCCS Listed Vehicle service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [3/4] REPCCS Listed Vehicle service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[Listed Vehicle] FAILED: " + e.getMessage());
            }

            // Small delay between services to ensure clean token handling
            Thread.sleep(2000);

            // ============ SERVICE 4: NOPO Archival ============
            log.info("========== [4/4] Starting REPCCS NOPO Archival service ==========");
            try {
                repccsnopoarcService.executeNopoarcFunction(batchJob);
                log.info("========== [4/4] REPCCS NOPO Archival service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [4/4] REPCCS NOPO Archival service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[NOPO Archival] FAILED: " + e.getMessage());
            }

            // All services have appended their logs to batchJob.logText
            String finalLogText = batchJob.getLogText() != null ? batchJob.getLogText() : "";
            log.info("Accumulated log text: \n{}", finalLogText);

            // Check if any process failed
            // "skip no data" and "success" are both considered SUCCESS
            boolean hasFailure = finalLogText.contains(": failed");

            if (hasFailure) {
                result = batchJobHelper.updateBatchJobFailureCesRep(batchJob, finalLogText);
                log.warn("REPCCS Generate processing completed with FAILURES - run_status set to F");
            } else {
                result = batchJobHelper.updateBatchJobSuccessCesRep(batchJob, finalLogText);
                log.info("Successfully completed REPCCS Generate processing - run_status set to S");
            }

        } catch (Exception e) {
            log.error("Error during REPCCS Generate processing: {}", e.getMessage(), e);
        }
        return result;
    }
}