package com.ocmsintranet.cronservice.framework.workflows.agencies.repccs.controllers;

import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.framework.workflows.agencies.repccs.schedulers.RepccsShedulerGeneratingFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for CASINANSVEH operations
 * Provides endpoints for executing and monitoring CASINANSVEH batch jobs
 * 
 * This controller handles requests for processing Unqualified Advisory Notices (ANS)
 * and generating CASINANSVEH format files for transfer to government systems.
 */
@RestController
@RequestMapping("/crontest/repccs")
public class RepccGenerateFilesController {
    
    private static final Logger logger = LoggerFactory.getLogger(RepccGenerateFilesController.class);

    private final RepccsShedulerGeneratingFiles repccsShedulerGeneratingFiles;

    public RepccGenerateFilesController(RepccsShedulerGeneratingFiles repccsShedulerGeneratingFiles) {
        this.repccsShedulerGeneratingFiles = repccsShedulerGeneratingFiles;
    }


    @PostMapping("/execute-scheduled-job")
    public ResponseEntity<OcmsBatchJob> executeScheduledJob() {
        try {
            OcmsBatchJob result = repccsShedulerGeneratingFiles.runJob();
            if (result == null) {
                return ResponseEntity.status(500).body(null);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
