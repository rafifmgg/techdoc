package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.ComcryptFileProcessingService;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptUploadProcessor;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.models.ToppanStage;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.helpers.ToppanUploadHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified Toppan Letters Generator Job - Main Orchestrator
 *
 * This job runs at 0030hr daily and orchestrates the processing of all 6 stages sequentially:
 * RD1 -> RD2 -> RR3 -> DN1 -> DN2 -> DR3
 *
 * SINGLE BATCH JOB MODEL:
 * - Creates ONE batch job record at start (via TrackedCronJobTemplate)
 * - All 6 stages execute within this single batch job context
 * - Single batch job record updated upon completion
 *
 * Processing Flow:
 * 1. DataHive FIN/UEN enrichment (mandatory, runs before all stages)
 * 2. For each stage sequentially:
 *    a. Query valid offence notices
 *    b. Generate Toppan enquiry file
 *    c. Upload via SLIFT encryption and SFTP/Azure
 *    d. Update processing stage flow
 * 3. Generate consolidated CSR file for all stages
 * 4. Update single batch job record with final status
 *
 * Note: ToppanUploadJob no longer creates batch jobs - it only executes stage logic.
 * This ensures proper batch job tracking with 1 record per daily execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToppanLettersGeneratorJob extends TrackedCronJobTemplate {

    private final ToppanUploadJob toppanUploadJob;
    private final ComcryptFileProcessingService sliftFileProcessingService;
    private final ToppanUploadHelper toppanUploadHelper;

    
    @Value("${toppan.unified.process.all.stages:true}")
    private boolean processAllStages;
    
    @Value("${toppan.unified.stop.on.error:false}")
    private boolean stopOnError;
    
    @Value("${cron.toppan.upload.shedlock.name:generate_toppan_letters}")
    private String jobName;
    
    // SFTP and blob paths for Toppan uploads (from sftp-common.properties)
    @Value("${sftp.folders.toppan.upload:/input}")
    private String sftpUploadPath;
    
    @Value("${blob.folder.toppan.upload:/offence/sftp/toppan/input/}")
    private String blobUploadPath;
    
    private final Map<String, Object> jobSummary = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final Map<ToppanStage, Integer> stageRecordCounts = new HashMap<>();
    
    @Override
    protected String getJobName() {
        return jobName;
    }
    
    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for unified Toppan letters generator");
        
        if (toppanUploadJob == null) {
            log.error("ToppanUploadJob is not initialized");
            return false;
        }
        
        log.info("Pre-conditions validated successfully");
        return true;
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        log.info("Initializing unified Toppan letters generator job at {}", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        jobSummary.clear();
        errors.clear();
        jobSummary.put("startTime", LocalDateTime.now());
        jobSummary.put("processAllStages", processAllStages);
    }
    
    @Override
    protected JobResult doExecute() {
        log.info("Starting unified Toppan letters generation - processing all stages sequentially");
        LocalDateTime jobStartTime = LocalDateTime.now();
        
        Map<String, StageResult> stageResults = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
        
        try {
            // STEP 1: DataHive Enrichment (runs BEFORE all stages as per high-level flow)
            log.info("==========================================");
            log.info("STEP 1: DataHive FIN/UEN Enrichment");
            log.info("==========================================");

            try {
                List<Map<String, Object>> dataHiveNotices = toppanUploadHelper.getNoticesForDataHiveEnrichmentOptimized();

                if (dataHiveNotices.isEmpty()) {
                    log.info("No notices found requiring DataHive enrichment");
                } else {
                    log.info("Found {} notices requiring DataHive FIN/UEN enrichment", dataHiveNotices.size());

                    // Enrich with DataHive (updates database)
                    toppanUploadHelper.enrichNoticesWithDataHive(dataHiveNotices);

                    log.info("DataHive enrichment completed successfully for {} notices", dataHiveNotices.size());
                }
            } catch (Exception e) {
                log.error("DataHive enrichment failed: {}", e.getMessage(), e);
                // DataHive is mandatory - if it fails, the job should fail
                return new JobResult(false, "DataHive enrichment failed: " + e.getMessage());
            }

            // STEP 2: Process all stages in sequence as per the flow diagram
            ToppanStage[] stages = ToppanStage.values();
            log.info("Will process {} stages: RD1, RD2, RR3, DN1, DN2, DR3, ANL (OCMS 10)", stages.length);

            for (ToppanStage stage : stages) {
                String stageName = stage.getCurrentStage();
                log.info("==========================================");
                log.info("Processing stage: {} - {}", stageName, stage.getDescription());
                log.info("==========================================");
                
                StageResult stageResult = new StageResult(stageName);
                stageResult.setStartTime(LocalDateTime.now());
                
                try {
                    // Check if this stage is enabled
                    if (!isStageEnabled(stageName)) {
                        log.info("Stage {} is disabled, skipping", stageName);
                        stageResult.setStatus("SKIPPED");
                        stageResult.setMessage("Stage disabled by configuration");
                        stageResults.put(stageName, stageResult);
                        skippedCount++;
                        continue;
                    }
                    
                    // Execute the stage using ToppanUploadJob (skip individual CSR generation)
                    log.info("Executing Toppan upload for stage: {}", stageName);
                    CompletableFuture<JobResult> futureResult = toppanUploadJob.executeWithStage(stageName, true); // true = skip CSR

                    // Wait for the stage to complete
                    JobResult result = futureResult.get();

                    // Extract record count and file timestamp from the result message if available
                    if (result.isSuccess()) {
                        int recordCount = extractRecordCount(result.getMessage());
                        if (recordCount > 0) {
                            stageRecordCounts.put(stage, recordCount);
                            log.info("Stage {} processed {} records", stageName, recordCount);
                        }

                        // Extract the file timestamp generated by this stage
                        String fileTimestamp = extractFileTimestamp(result.getMessage());
                        if (fileTimestamp != null) {
                            stageResult.setFileTimestamp(fileTimestamp);
                            log.info("Stage {} file timestamp: {}", stageName, fileTimestamp);
                        }
                    }

                    stageResult.setEndTime(LocalDateTime.now());
                    stageResult.setSuccess(result.isSuccess());
                    stageResult.setMessage(result.getMessage());
                    
                    if (result.isSuccess()) {
                        log.info("Stage {} completed successfully: {}", stageName, result.getMessage());
                        stageResult.setStatus("SUCCESS");
                        successCount++;
                    } else {
                        log.error("Stage {} failed: {}", stageName, result.getMessage());
                        stageResult.setStatus("FAILED");
                        failureCount++;
                        errors.add(String.format("Stage %s failed: %s", stageName, result.getMessage()));
                        
                        // Check if we should stop on error
                        if (stopOnError) {
                            log.warn("Stopping job execution due to error in stage {} (stop.on.error=true)", stageName);
                            break;
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Unexpected error processing stage {}: {}", stageName, e.getMessage(), e);
                    stageResult.setEndTime(LocalDateTime.now());
                    stageResult.setStatus("ERROR");
                    stageResult.setSuccess(false);
                    stageResult.setMessage("Unexpected error: " + e.getMessage());
                    failureCount++;
                    errors.add(String.format("Stage %s error: %s", stageName, e.getMessage()));
                    
                    if (stopOnError) {
                        log.warn("Stopping job execution due to unexpected error in stage {}", stageName);
                        break;
                    }
                }
                
                stageResults.put(stageName, stageResult);
                
                // Add a small delay between stages to avoid overwhelming the system
                if (stage != ToppanStage.DR3) { // Don't delay after the last stage
                    Thread.sleep(2000); // 2 second delay
                }
            }
            
            // Generate consolidated CSR file for all processed stages
            if (!stageRecordCounts.isEmpty()) {
                log.info("Generating consolidated CSR file for all stages");
                try {
                    generateConsolidatedCSR(stageRecordCounts, stageResults, jobStartTime);
                } catch (Exception e) {
                    log.error("Failed to generate consolidated CSR file: {}", e.getMessage(), e);
                    errors.add("CSR generation failed: " + e.getMessage());
                }
            }

            // Prepare job summary
            jobSummary.put("endTime", LocalDateTime.now());
            jobSummary.put("duration", java.time.Duration.between(jobStartTime, LocalDateTime.now()).toMillis());
            jobSummary.put("stagesProcessed", successCount + failureCount);
            jobSummary.put("successCount", successCount);
            jobSummary.put("failureCount", failureCount);
            jobSummary.put("skippedCount", skippedCount);
            jobSummary.put("stageResults", stageResults);
            
            // Log summary
            log.info("==========================================");
            log.info("Toppan Letters Generation Job Summary:");
            log.info("  Total Stages: {}", stages.length);
            log.info("  Successful: {}", successCount);
            log.info("  Failed: {}", failureCount);
            log.info("  Skipped: {}", skippedCount);
            log.info("  Duration: {} ms", jobSummary.get("duration"));
            log.info("==========================================");
            
            // Print detailed results
            for (Map.Entry<String, StageResult> entry : stageResults.entrySet()) {
                StageResult sr = entry.getValue();
                log.info("  {} - Status: {}, Message: {}", 
                    sr.getStageName(), sr.getStatus(), sr.getMessage());
            }
            
            // Determine overall job result
            if (failureCount == 0 && successCount > 0) {
                return new JobResult(true, String.format(
                    "Successfully processed %d stages (%d skipped)", 
                    successCount, skippedCount));
            } else if (failureCount > 0 && successCount > 0) {
                // Partial success - framework will handle email notification for errors
                return new JobResult(false, String.format(
                    "Partially completed: %d successful, %d failed, %d skipped. Errors: %s", 
                    successCount, failureCount, skippedCount, String.join("; ", errors)));
            } else if (successCount == 0 && skippedCount == stages.length) {
                return new JobResult(true, "All stages were skipped (disabled)");
            } else {
                return new JobResult(false, String.format(
                    "Job failed: %d failed, %d skipped. Errors: %s", 
                    failureCount, skippedCount, String.join("; ", errors)));
            }
            
        } catch (Exception e) {
            log.error("Critical error in Toppan letters generator job: {}", e.getMessage(), e);
            return new JobResult(false, "Critical error: " + e.getMessage());
        }
    }
    
    @Override
    protected void cleanup() {
        log.info("Cleaning up Toppan letters generator job");
        jobSummary.clear();
        errors.clear();
        stageRecordCounts.clear();
        super.cleanup();
    }
    
    /**
     * Check if a specific stage is enabled
     * This allows for individual stage control if needed
     */
    private boolean isStageEnabled(String stage) {
        // Check stage-specific configuration
        String propertyKey = String.format("toppan.upload.%s.enabled", stage.toLowerCase());
        return getEnvironmentProperty(propertyKey, true);
    }
    
    /**
     * Helper method to get boolean environment property with default
     */
    private boolean getEnvironmentProperty(String key, boolean defaultValue) {
        try {
            String value = System.getProperty(key);
            if (value == null) {
                value = System.getenv(key.replace(".", "_").toUpperCase());
            }
            return value != null ? Boolean.parseBoolean(value) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Extract record count from JobResult message
     * @param message The job result message
     * @return The extracted record count, or 0 if not found
     */
    private int extractRecordCount(String message) {
        if (message == null) {
            return 0;
        }

        // Try to extract number from message like "Successfully processed 10 records..."
        Pattern pattern = Pattern.compile("processed\\s+(\\d+)\\s+record");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse record count from message: {}", message);
            }
        }

        return 0;
    }

    /**
     * Extract file timestamp from JobResult message
     * @param message The job result message
     * @return The extracted timestamp (yyyyMMddHHmmss), or null if not found
     */
    private String extractFileTimestamp(String message) {
        if (message == null) {
            return null;
        }

        // Try to extract timestamp from message like "...timestamp=20251017142751"
        Pattern pattern = Pattern.compile("timestamp=(\\d{14})");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Generate and upload consolidated CSR file for all stages
     * @param stageRecordCounts Map of stages to their record counts
     * @param stageResults Map of stage results containing file timestamps
     * @param processingDate The processing date (for CSR header)
     */
    private void generateConsolidatedCSR(Map<ToppanStage, Integer> stageRecordCounts, Map<String, StageResult> stageResults, LocalDateTime processingDate) {
        try {
            StringBuilder csrContent = new StringBuilder();

            // Format dates (use job processing date for the header)
            String reportDate = processingDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            String dateTime = processingDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

            // CSR Header - centered to align with data width (56 chars total: 38 + 18)
            int totalWidth = 56;
            String title = "Control Summary Report";
            String reportDateLine = "Report Date: " + reportDate;
            String pageLine = "Page: 1";
            String dateTimeLine = "Date & Time: " + dateTime;

            // Center each header line within the total width
            csrContent.append(centerText(title, totalWidth)).append("\n");
            csrContent.append(centerText(reportDateLine, totalWidth)).append("\n");
            csrContent.append(centerText(pageLine, totalWidth)).append("\n");
            csrContent.append(centerText(dateTimeLine, totalWidth)).append("\n");
            csrContent.append("\n");

            // Column headers
            csrContent.append(String.format("%-38s%-18s%n", "Data File Name", "No. of Reminders"));
            csrContent.append(String.format("%-38s%-18s%n", "----------------------------------", "----------------"));

            // Add entry for each stage that was processed
            int totalRecords = 0;
            for (ToppanStage stage : ToppanStage.values()) {
                if (stageRecordCounts.containsKey(stage)) {
                    int count = stageRecordCounts.get(stage);

                    // Use the per-stage file timestamp from StageResult, or fall back to job start time
                    String fileTimestamp = null;
                    StageResult stageResult = stageResults.get(stage.getCurrentStage());
                    if (stageResult != null && stageResult.getFileTimestamp() != null) {
                        fileTimestamp = stageResult.getFileTimestamp();
                    } else {
                        // Fallback to processing date if timestamp not available
                        fileTimestamp = processingDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    }

                    String dataFileName = String.format("URA-DPT-%s-D1-%s",
                        stage.getCurrentStage(),
                        fileTimestamp);
                    log.debug("CSR entry for stage {}: {}", stage.getCurrentStage(), dataFileName);
                    csrContent.append(String.format("%-38s%-18d%n", dataFileName, count));
                    totalRecords += count;
                }
            }

            // Footer
            csrContent.append("\n");
            csrContent.append(String.format("%-38s%-18d%n", "Total number of Reminders", totalRecords));
            csrContent.append("==========\n");

            // Generate CSR file
            byte[] csrFileContent = csrContent.toString().getBytes();
            String csrDateTimeStr = processingDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String csrFileName = String.format("URA-DPT-CTL-S1-%s", csrDateTimeStr);

            log.info("Generated consolidated CSR file: {} with {} bytes for {} total records",
                csrFileName, csrFileContent.length, totalRecords);

            // Upload CSR file using SliftUploadProcessor (same approach as ToppanUploadJob)
            try {
                ComcryptUploadProcessor uploadProcessor = null;
                try {
                    // Use reflection to get the uploadProcessor field from SliftFileProcessingService
                    java.lang.reflect.Field field = sliftFileProcessingService.getClass().getDeclaredField("uploadProcessor");
                    field.setAccessible(true);
                    uploadProcessor = (ComcryptUploadProcessor) field.get(sliftFileProcessingService);
                } catch (Exception e) {
                    log.error("Failed to get SliftUploadProcessor: {}", e.getMessage(), e);
                }

                if (uploadProcessor != null) {
                    // Extract container and directory from blobFolderPath
                    String[] parts = blobUploadPath.trim().split("/", 2);
                    String container = parts[0];
                    String dir = parts.length > 1 ? parts[1] : "";

                    // Upload to Azure Blob Storage
                    String dateStr = processingDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String directory = dir + dateStr;
                    String blobPath = uploadProcessor.uploadToBlob(container, directory, csrFileContent, csrFileName);
                    log.info("Successfully uploaded consolidated CSR file to Azure Blob Storage: {}", blobPath);

                    // Upload to SFTP
                    String sftpPath = sftpUploadPath + "/" + csrFileName;
                    String sftpUploadPath = uploadProcessor.uploadToSftp(csrFileContent, sftpPath, "toppan");
                    log.info("Successfully uploaded consolidated CSR file to SFTP: {}", sftpUploadPath);
                } else {
                    log.warn("SliftUploadProcessor not available, cannot upload CSR file");
                }
            } catch (Exception e) {
                log.error("Failed to upload consolidated CSR file: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error generating consolidated CSR file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate consolidated CSR file", e);
        }
    }

    /**
     * Center text within a given width
     * @param text The text to center
     * @param width The total width to center within
     * @return The centered text with padding
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return String.format("%" + (padding + text.length()) + "s", text);
    }

    /**
     * Inner class to track individual stage results
     */
    private static class StageResult {
        private final String stageName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status; // SUCCESS, FAILED, ERROR, SKIPPED
        private boolean success;
        private String message;
        private String fileTimestamp; // The actual timestamp when the file was generated (yyyyMMddHHmmss)

        public StageResult(String stageName) {
            this.stageName = stageName;
        }

        // Getters and setters
        public String getStageName() { return stageName; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getFileTimestamp() { return fileTimestamp; }
        public void setFileTimestamp(String fileTimestamp) { this.fileTimestamp = fileTimestamp; }
    }
}