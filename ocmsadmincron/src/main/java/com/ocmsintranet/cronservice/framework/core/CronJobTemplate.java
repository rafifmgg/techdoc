package com.ocmsintranet.cronservice.framework.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract template class for all cron jobs in the system.
 * Implements the Template Method pattern to standardize the workflow execution process.
 */
public abstract class CronJobTemplate {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    // Job execution status tracking
    private JobStatus jobStatus = null; // No initial status
    private LocalDateTime lastStartTime;
    private LocalDateTime lastEndTime;
    private String lastError;
    
    /**
     * Template method that defines the skeleton of the cron job execution algorithm.
     * Subclasses should not override this method but implement the abstract methods.
     * 
     * @return CompletableFuture with the job result
     */
    @Async
    public final CompletableFuture<JobResult> execute() {
        lastStartTime = LocalDateTime.now();
        setJobStatus(JobStatus.RUNNING);
        lastError = null;
        
        try {
            logger.info("Starting cron job: {}", getJobName());
            
            // Pre-execution validation
            if (!validatePreConditions()) {
                logger.warn("Pre-conditions validation failed for job: {}", getJobName());
                setJobStatus(JobStatus.FAILED);
                lastError = "Pre-conditions validation failed";
                return CompletableFuture.completedFuture(new JobResult(false, lastError));
            }
            
            // Initialize resources
            initialize();
            
            // Execute the main job logic
            JobResult result = doExecute();
            
            // Cleanup resources
            cleanup();
            
            // Post-execution processing
            if (result.isSuccess()) {
                logger.info("Job completed successfully: {}", getJobName());
                setJobStatus(JobStatus.SUCCESS);
            } else {
                logger.error("Job failed: {} - Error: {}", getJobName(), result.getMessage());
                setJobStatus(JobStatus.FAILED);
                lastError = result.getMessage();
            }
            
            lastEndTime = LocalDateTime.now();
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Unexpected error in job: {}", getJobName(), e);
            setJobStatus(JobStatus.FAILED);
            lastError = e.getMessage();
            lastEndTime = LocalDateTime.now();
            return CompletableFuture.completedFuture(new JobResult(false, "Unexpected error: " + e.getMessage()));
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
                lastError
        );
    }
    
    /**
     * Reset the job status
     */
    public void reset() {
        jobStatus = null; // Reset to no status
        lastStartTime = null;
        lastEndTime = null;
        lastError = null;
    }
    
    /**
     * Set the job status with validation
     * 
     * @param status The status to set
     */
    private void setJobStatus(JobStatus status) {
        // Validate that the status is one of the allowed values
        if (status == JobStatus.RUNNING || 
            status == JobStatus.SUCCESS || 
            status == JobStatus.FAILED) {
            jobStatus = status;
        } else {
            logger.warn("Attempted to set invalid job status: {}", status);
            // Default to FAILED if an invalid status is provided
            jobStatus = JobStatus.FAILED;
        }
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
     * Enum representing the possible states of a cron job
     */
    public enum JobStatus {
        RUNNING,
        SUCCESS,
        FAILED
    }
    
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
        
        public JobStatusInfo(String jobName, JobStatus status, LocalDateTime startTime, LocalDateTime endTime, String error) {
            this.jobName = jobName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.error = error;
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
        
        public long getDurationInSeconds() {
            if (startTime == null || endTime == null) {
                return 0;
            }
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
}
