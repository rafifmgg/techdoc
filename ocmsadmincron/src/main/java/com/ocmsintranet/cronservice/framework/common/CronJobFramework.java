package com.ocmsintranet.cronservice.framework.common;

import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJobService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.annotation.Async; // Removed to avoid unused import warning

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Consolidated framework for cron job execution, tracking, and status management.
 * This file contains:
 * 1. JobStatus enum - Possible statuses for a cron job
 * 2. CronJobTemplate - Abstract template class implementing the Template Method pattern
 * 3. TrackedCronJobTemplate - Extension that adds automatic job execution history tracking
 */
public class CronJobFramework {

    /**
     * Enum representing possible statuses for a cron job
     */
    public enum JobStatus {
        RUNNING,
        SUCCESS,
        FAILED
    }

    /**
     * Abstract template class for all cron jobs in the system.
     * Implements the Template Method pattern to standardize the workflow execution process.
     */
    public static abstract class CronJobTemplate {
        
        protected final Logger logger = LoggerFactory.getLogger(getClass());
        
        // Job execution status tracking
        private JobStatus jobStatus = null; // No initial status
        private LocalDateTime lastStartTime;
        private LocalDateTime lastEndTime;
        private String lastError;
        private JobResult result; // Track job execution result
        
        /**
         * Template method that defines the skeleton of the cron job execution algorithm.
         * Subclasses should not override this method but implement the abstract methods.
         * 
         * @return CompletableFuture with the job result
         */
        // Removed @Async to ensure proper transaction propagation
        public final CompletableFuture<JobResult> execute() {
            lastStartTime = LocalDateTime.now();
            jobStatus = JobStatus.RUNNING;
            lastError = null;
            
            try {
                logger.info("Starting cron job: {}", getJobName());
                
                // Pre-execution validation
                if (!validatePreConditions()) {
                    logger.warn("Pre-conditions validation failed for job: {}", getJobName());
                    jobStatus = JobStatus.FAILED;
                    lastError = "Pre-conditions validation failed";
                    return CompletableFuture.completedFuture(new JobResult(false, lastError));
                }
                
                // Initialize resources
                initialize();
                
                // Execute the main job logic
                this.result = doExecute();
                
                // Cleanup resources
                cleanup();
                
                // Post-execution processing
                if (this.result.isSuccess()) {
                    logger.info("Job completed successfully: {}", getJobName());
                    jobStatus = JobStatus.SUCCESS;
                } else {
                    logger.error("Job failed: {} - Error: {}", getJobName(), this.result.getMessage());
                    jobStatus = JobStatus.FAILED;
                    lastError = this.result.getMessage();
                }
                
                lastEndTime = LocalDateTime.now();
                
                // Record job completion in history after status has been updated
                if (this instanceof TrackedCronJobTemplate) {
                    ((TrackedCronJobTemplate) this).recordJobCompletionInHistory();
                }
                
                return CompletableFuture.completedFuture(result);
                
            } catch (Exception e) {
                logger.error("Error executing job {}: {}", getJobName(), e.getMessage(), e);
                jobStatus = JobStatus.FAILED;
                lastError = e.getMessage();
                lastEndTime = LocalDateTime.now();
                
                // Record job completion in history after status has been updated in case of exception
                if (this instanceof TrackedCronJobTemplate) {
                    ((TrackedCronJobTemplate) this).recordJobCompletionInHistory();
                }
                
                return CompletableFuture.completedFuture(new JobResult(false, "Unexpected error: " + e.getMessage()));
            } finally {
                // Empty finally block
            }
        }
        
        /**
         * Get the current status of the job
         * 
         * @return JobStatus enum representing the current status
         */
        public JobStatus getStatus() {
            return jobStatus;
        }
        
        /**
         * Get detailed status information about the job
         * 
         * @return JobStatusInfo object with detailed status information
         */
        public JobStatusInfo getStatusInfo() {
            return new JobStatusInfo(
                    getJobName(),
                    jobStatus,
                    lastStartTime,
                    lastEndTime,
                    lastError,
                    result != null ? result.getMessage() : null
            );
        }
        
        /**
         * Reset the job status
         */
        public void reset() {
            jobStatus = null; // No status after reset
            lastStartTime = null;
            lastEndTime = null;
            lastError = null;
            result = null;
        }
        
        /**
         * Get the name of the job
         * 
         * @return String representing the job name
         */
        protected abstract String getJobName();
        
        /**
         * Validate pre-conditions before job execution
         * 
         * @return true if pre-conditions are valid, false otherwise
         */
        protected abstract boolean validatePreConditions();
        
        /**
         * Initialize resources needed for job execution
         */
        protected abstract void initialize();
        
        /**
         * Execute the main job logic
         * 
         * @return JobResult indicating success or failure with a message
         */
        protected abstract JobResult doExecute();
        
        /**
         * Clean up resources after job execution
         */
        protected abstract void cleanup();
        
        /**
         * Class representing the result of a job execution
         */
        public static class JobResult {
            private final boolean success;
            private final String message;
            
            public JobResult(boolean success, String message) {
                this.success = success;
                this.message = message;
            }
            
            public boolean isSuccess() {
                return success;
            }
            
            public String getMessage() {
                return message;
            }
        }
        
        /**
         * Class representing detailed status information about a job
         */
        public static class JobStatusInfo {
            private final String jobName;
            private final JobStatus status;
            private final LocalDateTime startTime;
            private final LocalDateTime endTime;
            private final String error;
            private final String message;
            
            public JobStatusInfo(String jobName, JobStatus status, LocalDateTime startTime, LocalDateTime endTime, String error) {
                this(jobName, status, startTime, endTime, error, null);
            }
            
            public JobStatusInfo(String jobName, JobStatus status, LocalDateTime startTime, LocalDateTime endTime, String error, String message) {
                this.jobName = jobName;
                this.status = status;
                this.startTime = startTime;
                this.endTime = endTime;
                this.error = error;
                this.message = message;
            }
            
            public String getJobName() {
                return jobName;
            }
            
            public JobStatus getStatus() {
                return status;
            }
            
            public LocalDateTime getStartTime() {
                return startTime;
            }
            
            public LocalDateTime getEndTime() {
                return endTime;
            }
            
            public String getError() {
                return error;
            }
            
            public String getMessage() {
                return message;
            }
            
            public long getDurationInSeconds() {
                if (startTime == null || endTime == null) {
                    return 0;
                }
                return java.time.Duration.between(startTime, endTime).getSeconds();
            }
        }
    }

    /**
     * Extension of CronJobTemplate that automatically tracks job execution history
     * using the standard CRUD operations via JobExecutionHistoryService
     */
    @Slf4j
    public static abstract class TrackedCronJobTemplate extends CronJobTemplate {
        
        @Autowired
        private OcmsBatchJobService ocmsBatchJobService;
        
        private OcmsBatchJob currentExecution;
        
        /**
         * Hook method called before the parent execute method
         * Records the job start in the history
         */
        // Hook that will be called during job execution
        // Records the job start in the history
        private void recordJobStartInHistory() {
            try {
                currentExecution = recordJobStart(getJobName());
            } catch (Exception e) {
                log.error("Failed to record job start: {}", getJobName(), e);
                // Continue with execution even if recording fails
            }
        }
        
        @Override
        protected void initialize() {
            // Record job start in history
            recordJobStartInHistory();
            
            // No call to super.initialize() since it's abstract
        }
        
        /**
         * Hook method called after the parent execute method
         * Records the job completion in the history
         */
        // Hook that will be called during job execution
        // Records the job completion in the history
        private void recordJobCompletionInHistory() {
            try {
                if (currentExecution != null) {
                    JobStatus status = getStatus();
                    // Only proceed if we have a valid status (RUNNING, COMPLETED, or FAILED)
                    if (status != null && isValidJobStatus(status)) {
                        // Use the actual error message from the job result for FAILED status
                        // or a descriptive success message for COMPLETED status
                        String message;
                        if (status == JobStatus.FAILED) {
                            // Get the error message from the job status info
                            JobStatusInfo statusInfo = getStatusInfo();
                            message = statusInfo != null ? statusInfo.getError() : null;
                            // If no specific error message is available, provide a generic one
                            if (message == null || message.trim().isEmpty()) {
                                message = "Job failed with unknown error";
                            }
                        } else if (status == JobStatus.SUCCESS) {
                            // Use the message from the job result if available, otherwise use default
                            JobStatusInfo statusInfo = getStatusInfo();
                            message = (statusInfo != null && statusInfo.getMessage() != null) ? 
                                     statusInfo.getMessage() : "Job completed successfully";
                        } else {
                            message = "Job status: " + status.name();
                        }
                        recordJobCompletion(currentExecution, status, message);
                    } else if (status == null) {
                        log.warn("Cannot record job completion for {} - status is null", getJobName());
                    } else {
                        log.warn("Cannot record job completion for {} - invalid status: {}", getJobName(), status);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to record job completion: {}", getJobName(), e);
                // Continue even if recording fails
            }
        }
        
        @Override
        protected void cleanup() {
            log.debug("Cleaning up resources for {}", getJobName());
            
            // Note: We don't record job completion here because the job status hasn't been updated yet
            // The job status is updated after cleanup() in the execute() method
            // The recordJobCompletionInHistory() will be called directly from execute() after status update
            
            // No call to super.cleanup() since it's abstract
        }
        
        /**
         * Create a new job execution history record when a job starts
         * 
         * @param jobName Name of the job
         * @return The created OcmsBatchJob entity
         */
        private OcmsBatchJob recordJobStart(String jobName) {
            log.debug("Recording job start: {}", jobName);

            OcmsBatchJob history = new OcmsBatchJob();
            history.setName(jobName);
            history.setStartRun(LocalDateTime.now());
            history.setLogText("Job started");
            // Status will be NULL for job start

            // Use the standard save method from the base service
            return ocmsBatchJobService.save(history);
        }
        
        /**
         * Update job execution history record when a job completes
         * 
         * @param batchJob The OcmsBatchJob entity to update
         * @param status Final status of the job
         * @param message Result message or error details
         * @return The updated OcmsBatchJob entity
         */
        private OcmsBatchJob recordJobCompletion(OcmsBatchJob history, JobStatus status, String message) {
            log.debug("Recording job completion: {} with status: {} and message: {}", 
                history.getName(), status, message);
            
            // Create a new instance with only the fields we want to update
            OcmsBatchJob updates = new OcmsBatchJob();
            updates.setEndRun(LocalDateTime.now());
            
            // Set status and log text based on the status
            if (isValidJobStatus(status)) {
                if (status == JobStatus.SUCCESS) {
                    log.info("Setting job status to SUCCESS (S) for job {}", history.getName());
                    updates.setRunStatus("S");
                    updates.setLogText(message);
                } else if (status == JobStatus.FAILED) {
                    log.info("Setting job status to FAILED (F) for job {}", history.getName());
                    updates.setRunStatus("F");
                    updates.setLogText(message);
                } else {
                    log.info("Setting job status to FAILED (F) for job {} with status {}", 
                        history.getName(), status);
                    updates.setRunStatus("F");
                    updates.setLogText(message);
                }
            } else {
                log.warn("Invalid job status {} detected. Defaulting to FAILED (F).", status);
                updates.setRunStatus("F");
                updates.setLogText("Job failed with invalid status: " + 
                    (status != null ? status.name() : "null"));
            }
            
            log.info("Setting job message to '{}' for job {}", message, history.getName());
            
            try {
                if (history.getBatchJobId() != null) {
                    // Use patch to update only the specified fields
                    ocmsBatchJobService.patch(history.getBatchJobId(), updates);
                    log.info("Job execution history updated with ID: {} for job {}", history.getBatchJobId(), history.getName());
                } else {
                    // Record doesn't exist, log warning
                    log.warn("Cannot update job execution history - invalid batch job ID");
                }
            } catch (Exception e) {
                log.error("Failed to update job status for job {}: {}", 
                    history.getName(), e.getMessage(), e);
            }
            return history;
        }
        
        /**
         * Validate that a job status is one of the allowed values
         * 
         * @param status The status to validate
         * @return true if the status is valid, false otherwise
         */
        private boolean isValidJobStatus(JobStatus status) {
            // Only RUNNING, COMPLETED, and FAILED are valid statuses
            return status == JobStatus.RUNNING || 
                   status == JobStatus.SUCCESS || 
                   status == JobStatus.FAILED;
        }
    }
}
