package com.ocmsintranet.cronservice.framework.workflows.agencies.ces.schedulers;

import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.ces.CesAnsVehicleService;
import com.ocmsintranet.cronservice.framework.services.ces.CesOffenceRuleService;
import com.ocmsintranet.cronservice.framework.services.ces.CesWantedVehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CesSchedulerGeneratingFiles {

    private final BatchJobHelper batchJobHelper;
    private final CesAnsVehicleService cesAnsVehicleService;
    private final CesWantedVehicleService cesWantedVehicleService;
    private final CesOffenceRuleService cesOffenceRuleService;

    @Value("${cron.ces.upload.enabled:false}")
    private boolean enabled;

    @Value("${cron.ces.upload.shedlock.name}")
    private String ces_generate;

    public CesSchedulerGeneratingFiles(BatchJobHelper batchJobHelper, CesAnsVehicleService cesAnsVehicleService,
                                       CesWantedVehicleService cesWantedVehicleService,
                                       CesOffenceRuleService cesOffenceRuleService) {
        this.batchJobHelper = batchJobHelper;
        this.cesAnsVehicleService = cesAnsVehicleService;
        this.cesWantedVehicleService = cesWantedVehicleService;
        this.cesOffenceRuleService = cesOffenceRuleService; // Added missing assignment
    }

    @Scheduled(cron = "${cron.ces.upload.schedule:}")
    @SchedulerLock(name = "generate_ces_files", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void executeScheduledJob() {
        if (!enabled) {
            log.info("CES ANS Vehicle scheduler is disabled. Skipping execution.");
            return;
        }
        runJob();
    }

    public OcmsBatchJob runJob(){
        log.info("Starting scheduled CES Generate processing");
        OcmsBatchJob result = null;
        try {

            OcmsBatchJob batchJob = batchJobHelper.createInitialBatchJob(ces_generate);

            // Execute services ONE BY ONE (not in parallel) to avoid token conflicts
            // Each service: request token → wait for callback → encrypt → upload

            // ============ SERVICE 1: ANS Vehicle ============
            log.info("========== [1/3] Starting CES ANS Vehicle service ==========");
            try {
                cesAnsVehicleService.executeCesAnsVehicleFunction(batchJob);
                log.info("========== [1/3] CES ANS Vehicle service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [1/3] CES ANS Vehicle service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[ANS Vehicle] FAILED: " + e.getMessage());
            }

            // Small delay between services to ensure clean token handling
            Thread.sleep(2000);

            // ============ SERVICE 2: Offence Rule ============
            log.info("========== [2/3] Starting CES Offence Rule service ==========");
            try {
                cesOffenceRuleService.executeCesOffenceRuleFunction(batchJob);
                log.info("========== [2/3] CES Offence Rule service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [2/3] CES Offence Rule service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[Offence Rule] FAILED: " + e.getMessage());
            }

            // Small delay between services to ensure clean token handling
            Thread.sleep(2000);

            // ============ SERVICE 3: Wanted Vehicle ============
            log.info("========== [3/3] Starting CES Wanted Vehicle service ==========");
            try {
                cesWantedVehicleService.executeCesWantedVerhicleFunction(batchJob);
                log.info("========== [3/3] CES Wanted Vehicle service COMPLETED ==========");
            } catch (Exception e) {
                log.error("========== [3/3] CES Wanted Vehicle service FAILED: {} ==========", e.getMessage());
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") +
                    "\n[Wanted Vehicle] FAILED: " + e.getMessage());
            }

            // All services have appended their logs to batchJob.logText
            String finalLogText = batchJob.getLogText() != null ? batchJob.getLogText() : "";
            log.info("Accumulated log text: \n{}", finalLogText);

            // Check if any process failed
            // "skip no data" and "success" are both considered SUCCESS
            boolean hasFailure = finalLogText.contains(": failed");

            if (hasFailure) {
                result = batchJobHelper.updateBatchJobFailureCesRep(batchJob, finalLogText);
                log.warn("CES Generate processing completed with FAILURES - run_status set to F");
            } else {
                result = batchJobHelper.updateBatchJobSuccessCesRep(batchJob, finalLogText);
                log.info("Successfully completed CES Generate processing - run_status set to S");
            }

        } catch (Exception e) {
            log.error("Error during CES Generate processing: {}", e.getMessage(), e);
        }
        return result;
    }

}
