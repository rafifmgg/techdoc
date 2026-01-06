package com.ocmsintranet.cronservice.framework.workflows.agencies.ces.controllers;

import com.ocmsintranet.cronservice.framework.services.ces.*;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.framework.workflows.agencies.ces.schedulers.CesSchedulerGeneratingFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/crontest/ces/")
public class CesController {

    private static final Logger logger = LoggerFactory.getLogger(CesController.class);

    @Autowired
    private CesDownloadAttachmentService serviceCesDownload;

    @Autowired
    private CesSchedulerGeneratingFiles cesSchedulerGeneratingFiles;

    @Autowired
    private CesAnsVehicleService cesAnsVehicleService;

    public CesController(CesSchedulerGeneratingFiles cesSchedulerGeneratingFiles) {
        this.cesSchedulerGeneratingFiles = cesSchedulerGeneratingFiles;
    }




    @PostMapping("/execute-scheduled-job")
    public ResponseEntity<Map<String, Object>> executeScheduledJob() {
        logger.info("========== REST API: Starting CES_GENERATE_FILES execution ==========");

        Map<String, Object> response = new HashMap<>();

        try {
            OcmsBatchJob result = cesSchedulerGeneratingFiles.runJob();

            if (result != null) {
                boolean isSuccess = "S".equals(result.getRunStatus());

                response.put("success", isSuccess);
                response.put("batchJobId", result.getBatchJobId());
                response.put("batchJobName", result.getName());
                response.put("status", result.getRunStatus());
                response.put("message", result.getLogText());
                response.put("startTime", result.getStartRun());
                response.put("endTime", result.getEndRun());
                response.put("timestamp", java.time.LocalDateTime.now());

                if (isSuccess) {
                    logger.info("CES_GENERATE_FILES completed with SUCCESS status");
                } else {
                    logger.warn("CES_GENERATE_FILES completed with FAILED status: {}", result.getLogText());
                }

                // Always return 200 OK if job executed - status F is a business result, not HTTP error
                return ResponseEntity.ok(response);

            } else {
                logger.error("CES_GENERATE_FILES execution returned null result");
                response.put("success", false);
                response.put("message", "CES_GENERATE_FILES execution returned null result");
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Error during CES_GENERATE_FILES execution: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Error during CES_GENERATE_FILES execution: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            logger.info("========== REST API: CES_GENERATE_FILES execution finished ==========");
        }
    }


    @PostMapping("/download-offence/v1/execute")
    public ResponseEntity<Map<String, Object>> executeDownloadAttachment() {
        logger.info("========== REST API: Starting CES_DOWNLOAD_ATTACHMENT execution ==========");

        Map<String, Object> response = new HashMap<>();

        try {

            OcmsBatchJob result = serviceCesDownload.executeCesDownloadAttachmentFunction();

            if (result != null) {
                boolean isSuccess = "S".equals(result.getRunStatus());

                response.put("success", isSuccess);
                response.put("batchJobId", result.getBatchJobId());
                response.put("batchJobName", result.getName());
                response.put("status", result.getRunStatus());
                response.put("message", result.getLogText());
                response.put("startTime", result.getStartRun());
                response.put("endTime", result.getEndRun());
                response.put("timestamp", java.time.LocalDateTime.now());

                if (isSuccess) {
                    logger.info("CES_DOWNLOAD_ATTACHMENT completed with SUCCESS status");
                } else {
                    logger.warn("CES_DOWNLOAD_ATTACHMENT completed with FAILED status: {}", result.getLogText());
                }

                // Always return 200 OK if job executed - status F is a business result, not HTTP error
                return ResponseEntity.ok(response);

            } else {
                logger.error("CES_DOWNLOAD_ATTACHMENT execution returned null result");
                response.put("success", false);
                response.put("message", "CES_DOWNLOAD_ATTACHMENT execution returned null result");
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Error during CES_DOWNLOAD_ATTACHMENT execution via REST API: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Error during CES_DOWNLOAD_ATTACHMENT execution: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            logger.info("========== REST API: CES_DOWNLOAD_ATTACHMENT execution finished ==========");
        }
    }

}
