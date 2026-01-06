package com.ocmsintranet.cronservice.framework.helper;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Helper class for managing batch job lifecycle
 * Handles creation, updates, and persistence of batch jobs
 */
@Component
public class BatchJobHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchJobHelper.class);
    
    @Autowired
    private OcmsBatchJobService batchJobService;
    
    /**
     * Create initial batch job record when cron job starts
     * 
     * @param jobName Name of the job (e.g., "REP_ARC_PROCESSING")
     * @return OcmsBatchJob entity with initial values
     */
    public OcmsBatchJob createInitialBatchJob(String jobName) {
        logger.info("Creating initial batch job record for: {}", jobName);

        LocalDateTime now = LocalDateTime.now();


        OcmsBatchJob batchJob = new OcmsBatchJob();
        //batchJob.setBatchJobId(1);
        batchJob.setName(jobName);
        batchJob.setRunStatus(SystemConstant.BatchJob.STATUS_RUNNING);
        batchJob.setStartRun(now);
        batchJob.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText("");

        // Save to database and get the generated ID
        OcmsBatchJob savedJob = batchJobService.save(batchJob);
        
        logger.info("Initial batch job created with ID: {} for job: {}", savedJob.getBatchJobId(), jobName);
        return savedJob;
    }
    
    /**
     * Update batch job with success status
     * 
     * @param batchJob Existing batch job to update
     * @param successMessage Success message to log
     * @return Updated OcmsBatchJob entity
     */
    public OcmsBatchJob updateBatchJobSuccess(OcmsBatchJob batchJob, String successMessage) {
        logger.info("Updating batch job to success: {}", batchJob.getName());
        batchJob.setBatchJobId(batchJob.getBatchJobId());
        batchJob.setRunStatus(SystemConstant.BatchJob.STATUS_COMPLETED);
        batchJob.setEndRun(LocalDateTime.now());
        batchJob.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText(successMessage);

        // Update in database
        OcmsBatchJob updatedJob = batchJobService.save(batchJob);
        
        logger.info("Batch job updated to success: {}", batchJob.getName());
        return updatedJob;
    }
    
    /**
     * Update batch job with failure status
     * 
     * @param batchJob Existing batch job to update
     * @param errorMessage Error message to log
     * @return Updated OcmsBatchJob entity
     */
    public OcmsBatchJob updateBatchJobFailure(OcmsBatchJob batchJob, String errorMessage) {
        logger.error("Updating batch job to failure: {} - Error: {}", batchJob.getName(), errorMessage);

        batchJob.setBatchJobId(batchJob.getBatchJobId());
        batchJob.setRunStatus(SystemConstant.BatchJob.STATUS_FAILED);
        batchJob.setEndRun(LocalDateTime.now());
        batchJob.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText(SystemConstant.CronJob.LOG_ERROR_PREFIX + errorMessage);
        
        // Update in database
        OcmsBatchJob updatedJob = batchJobService.save(batchJob);
        
        logger.info("Batch job updated to failure: {}", batchJob.getName());
        return updatedJob;
    }
    
    /**
     * Update batch job for no data found scenario (still considered success)
     * 
     * @param batchJob Existing batch job to update
     * @param noDataMessage Message indicating no data was found
     * @return Updated OcmsBatchJob entity
     */
    public OcmsBatchJob updateBatchJobNoData(OcmsBatchJob batchJob, String noDataMessage) {
        logger.info("Updating batch job for no data scenario: {}", batchJob.getName());


        batchJob.setBatchJobId(batchJob.getBatchJobId());
        batchJob.setRunStatus(SystemConstant.BatchJob.STATUS_NO_DATA);
        batchJob.setEndRun(LocalDateTime.now());
        batchJob.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText(SystemConstant.CronJob.LOG_NO_DATA_FOUND);
        
        // Update in database
        OcmsBatchJob updatedJob = batchJobService.save(batchJob);
        
        logger.info("Batch job updated for no data: {}", batchJob.getName());
        return updatedJob;
    }
    
    /**
     * Calculate execution duration in milliseconds
     * 
     * @param batchJob Batch job to calculate duration for
     * @return duration in milliseconds, or 0 if start/end times are null
     */
    public long getExecutionDurationMs(OcmsBatchJob batchJob) {
        if (batchJob.getStartRun() == null) {
            return 0;
        }
        
        LocalDateTime endTime = batchJob.getEndRun() != null ? batchJob.getEndRun() : LocalDateTime.now();
        return ChronoUnit.MILLIS.between(batchJob.getStartRun(), endTime);
    }
    
    /**
     * Check if batch job completed successfully
     * 
     * @param batchJob Batch job to check
     * @return true if status is SUCCESS, false otherwise
     */
    public boolean isSuccess(OcmsBatchJob batchJob) {
        return SystemConstant.BatchJob.STATUS_COMPLETED.equals(batchJob.getRunStatus());
    }
    
    /**
     * Check if batch job failed
     * 
     * @param batchJob Batch job to check
     * @return true if status is FAILED, false otherwise
     */
    public boolean isFailed(OcmsBatchJob batchJob) {
        return SystemConstant.BatchJob.STATUS_FAILED.equals(batchJob.getRunStatus());
    }
    
    /**
     * Check if batch job is still running
     * 
     * @param batchJob Batch job to check
     * @return true if status is RUNNING, false otherwise
     */
    public boolean isRunning(OcmsBatchJob batchJob) {
        return SystemConstant.BatchJob.STATUS_RUNNING.equals(batchJob.getRunStatus());
    }
    
    /**
     * Persist batch job to database
     * This method is now redundant since save operations are done in other methods,
     * but kept for backward compatibility and explicit persistence calls
     * 
     * @param batchJob Batch job to persist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistBatchJob(OcmsBatchJob batchJob) {
        logger.info("========== BATCH JOB PERSISTENCE ==========");
        logger.info("Job Name: {}", batchJob.getName());
        logger.info("Status: {}", batchJob.getRunStatus());
        logger.info("Start Time: {}", batchJob.getStartRun());
        logger.info("End Time: {}", batchJob.getEndRun());
        logger.info("Duration: {} ms", getExecutionDurationMs(batchJob));
        logger.info("Log Text: {}", batchJob.getLogText());
        logger.info("Created By: {}", batchJob.getCreUserId());
        logger.info("Updated By: {}", batchJob.getUpdUserId());
        
        // Save to database
        OcmsBatchJob savedJob = batchJobService.save(batchJob);
        logger.info("Batch job persisted with ID: {}", savedJob.getBatchJobId());
        
        logger.info("========== END BATCH JOB PERSISTENCE ==========");
    }

    public OcmsBatchJob updateBatchJobWithWarning(OcmsBatchJob job, String warningMessage) {
        job.setRunStatus("F");
        job.setLogText("WARNING: " + warningMessage);
        OcmsBatchJob savedJob = batchJobService.save(job);
        logger.info("Batch job updated with warning status: {}", job.getName());
        return savedJob;
    }

    public OcmsBatchJob updateBatchJobFailureCesRep(OcmsBatchJob batchJob, String errorMessage) {
        logger.error("Updating batch job to failure: {} - Error: {}", batchJob.getName(), errorMessage);

        batchJob.setBatchJobId(batchJob.getBatchJobId());
        batchJob.setRunStatus("F");
        batchJob.setEndRun(LocalDateTime.now());
        batchJob.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText(errorMessage);

        // Update in database
        OcmsBatchJob updatedJob = batchJobService.save(batchJob);

        logger.info("Batch job updated to failure: {}", batchJob.getName());
        return updatedJob;
    }

    public OcmsBatchJob updateBatchJobSuccessCesRep(OcmsBatchJob batchJob, String successMessage) {
        logger.info("Updating batch job to success: {}", batchJob.getName());
        batchJob.setBatchJobId(batchJob.getBatchJobId());
        batchJob.setRunStatus("S");
        batchJob.setEndRun(LocalDateTime.now());
        batchJob.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        batchJob.setLogText(successMessage);

        // Update in database
        OcmsBatchJob updatedJob = batchJobService.save(batchJob);

        logger.info("Batch job updated to success: {}", batchJob.getName());
        return updatedJob;
    }
}
