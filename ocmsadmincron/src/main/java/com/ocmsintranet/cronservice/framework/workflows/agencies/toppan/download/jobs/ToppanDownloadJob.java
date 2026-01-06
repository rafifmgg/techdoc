package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.helpers.ToppanDownloadHelper;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.helpers.ToppanResponseParser;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.services.ToppanStageTransitionService;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Toppan Download Job - Processes response files from Toppan after print completion
 * 
 * This job orchestrates the complete download workflow to process response files from Toppan
 * after they have completed printing and dispatching reminder notices. It handles all stages
 * of the owner and driver notification workflow.
 * 
 * WORKFLOW OVERVIEW:
 * Owner Path: RD1 (First Request) → RD2 (Second Request - Registered) → RR3 (Final Reminder + Fee) → CPC
 * Driver Path: DN1 (First Notice) → DN2 (Second Notice - Registered) → DR3 (Final Request + Fee) → CPC
 * 
 * PROCESSING STEPS:
 * 1. Lists encrypted response files (.p7) from Toppan SFTP server
 * 2. Downloads files and decrypts using SLIFT encryption service
 * 3. Parses response files to extract processing results and notice statuses
 * 4. Updates database with stage transitions for successful notices
 * 5. Calculates administration fees for RR3/DR3 final reminder stages
 * 6. Updates tracking tables with postal registration numbers (RD2/DN2)
 * 7. Handles failed notices by marking status without advancing stage
 * 8. Archives processed files locally with timestamps
 * 9. Cleans up SFTP and temporary files
 * 
 * RESPONSE FILE TYPES HANDLED:
 * - DPT-URA-LOG-D2-* : Data acknowledgement files (all stages)
 * - DPT-URA-LOG-PDF-* : PDF acknowledgement files
 * - DPT-URA-RD2-D2-* : Registered mail return files for RD2 stage
 * - DPT-URA-DN2-D2-* : Registered mail return files for DN2 stage
 * - DPT-URA-PDF-D2-* : PDF registered mail return files
 * 
 * ERROR HANDLING:
 * - Individual file processing errors do not stop the entire job
 * - Failed notices are updated with error status but stage is not advanced
 * - Critical errors trigger framework notifications and job failure
 * - Comprehensive logging for monitoring and troubleshooting
 * 
 * DEPENDENCIES:
 * - SliftFileProcessingService: For file encryption/decryption
 * - SftpUtil: For secure file transfer operations
 * - TableQueryService: For database operations
 * - ParameterRepository: For configuration values (stage durations, fees)
 * 
 * SCHEDULING:
 * - Runs hourly between 6 AM - 8 PM on weekdays (configurable)
 * - Uses ShedLock for distributed locking in clustered environments
 * - Manual trigger available via REST API endpoint
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToppanDownloadJob extends TrackedCronJobTemplate {
    
    // SFTP server configuration for Toppan file transfer
    private static final String SFTP_SERVER = "toppan";
    
    // SFTP directory where Toppan places response files for download
    @Value("${toppan.sftp.download.directory:/output}")
    private String sftpDownloadDirectory;

    // Local directory for temporary file storage during processing
    @Value("${toppan.local.download.directory:/tmp/toppan/download}")
    private String localDownloadDirectory;

    // Blob folder path for uploading encrypted and decrypted files
    @Value("${blob.folder.toppan.download:/offence/sftp/toppan/output/}")
    private String blobFolderPath;

    // Flag to indicate whether to use encryption (default: false for Toppan)
    @Value("${sftp.servers.toppan.encryption:false}")
    private boolean encryption;

    // Helper service for SFTP operations and file management
    private final ToppanDownloadHelper downloadHelper;

    // Parser service for all types of Toppan response files
    private final ToppanResponseParser responseParser;

    // Service for managing database stage transitions and fee calculations
    private final ToppanStageTransitionService stageTransitionService;

    // Azure Blob Storage utility for uploading encrypted and decrypted files
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    @Value("${cron.toppan.download.shedlock.name:download_toppan_ack_files}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }

    /**
     * Records job metadata for tracking and reporting
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    private void recordJobMetadata(String key, String value) {
        // Store in the metadata map
        jobMetadata.put(key, value);
        // Also log for visibility
        log.info("Job Metadata: {} = {}", key, value);
    }

    /**
     * Validates that all required dependencies are properly initialized before job execution.
     * This ensures the job fails fast if any critical components are missing.
     * 
     * @return true if all pre-conditions are met, false otherwise
     */
    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for Toppan download job");
        
        // Verify SFTP and file management helper is available
        if (downloadHelper == null) {
            log.error("ToppanDownloadHelper is not initialized");
            return false;
        }
        
        // Verify response file parser is available
        if (responseParser == null) {
            log.error("ToppanResponseParser is not initialized");
            return false;
        }
        
        // Verify stage transition service is available
        if (stageTransitionService == null) {
            log.error("ToppanStageTransitionService is not initialized");
            return false;
        }
        
        log.info("All pre-conditions validated successfully");
        return true;
    }
    
    /**
     * Initialize the job with any required setup.
     * Called by the framework before doExecute().
     */
    @Override
    protected void initialize() {
        super.initialize();
        log.info("Initializing Toppan download job at {}", LocalDateTime.now());
        jobMetadata.clear();
    }

    /**
     * Cleanup resources after job execution (success or failure).
     * Ensures temporary files are removed to prevent disk space issues.
     */
    @Override
    protected void cleanup() {
        log.info("Cleaning up Toppan download job resources");
        // Clean up any temporary downloaded files to free disk space
        downloadHelper.cleanupLocalFiles();
        super.cleanup();
    }
    
    /**
     * Main execution method that orchestrates the complete download workflow.
     * 
     * EXECUTION FLOW:
     * 1. List encrypted response files from Toppan SFTP server
     * 2. Download and decrypt files using SLIFT encryption service
     * 3. Parse each response file to extract processing results
     * 4. Update database with stage transitions for successful notices
     * 5. Handle failed notices by updating status without advancing stage
     * 6. Archive processed files and clean up SFTP
     * 7. Clean up staging tables
     * 
     * @return JobResult containing execution status and summary message
     */
    @Override
    protected JobResult doExecute() {
        log.info("Starting Toppan download job execution");
        // Use current timestamp for all database updates to ensure consistency
        LocalDateTime processingDate = LocalDateTime.now();
        
        try {
            // Step 1: Discover response files from SFTP
            // Files are typically named: DPT-URA-{TYPE}-{DETAILS}-{TIMESTAMP}.p7
            log.info("Checking for response files in SFTP directory: {}", sftpDownloadDirectory);
            List<String> responseFiles = downloadHelper.listResponseFiles(SFTP_SERVER, sftpDownloadDirectory);

            // Exit early if no files are ready for processing
            if (responseFiles.isEmpty()) {
                log.info("No response files found to process");
                return new JobResult(true, "No response files found to process");
            }

            log.info("Found {} response files to process", responseFiles.size());
            recordJobMetadata("filesFound", String.valueOf(responseFiles.size()));

            // Step 2: Download files from SFTP (with or without encryption based on config)
            log.info("Downloading files with encryption: {}", encryption);
            Map<String, byte[]> decryptedFiles = downloadHelper.downloadAndDecryptFiles(
                SFTP_SERVER,
                sftpDownloadDirectory,
                responseFiles,
                encryption
            );

            // Fail job if unable to decrypt any files (potential SLIFT/SFTP issues)
            if (decryptedFiles.isEmpty()) {
                log.warn("Failed to download/decrypt any files");
                return new JobResult(false, "Failed to download or decrypt response files");
            }

            log.info("Successfully downloaded and decrypted {} files", decryptedFiles.size());

            // Step 3: Process each decrypted response file individually
            // Track processing statistics for final job result
            int totalProcessed = 0;
            List<String> errors = new ArrayList<>();
            List<String> processedFiles = new ArrayList<>();
            
            // Process each file independently - individual failures don't stop the job
            for (Map.Entry<String, byte[]> entry : decryptedFiles.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileContent = entry.getValue();
                
                try {
                    log.info("Processing response file: {}", fileName);

                    // Remove .p7 encryption extension to get original filename for parsing
                    String baseFileName = fileName.replace(".p7", "");

                    // Check if this is a LOG or PDF file that only needs blob upload (no parsing/processing)
                    String upperFileName = baseFileName.toUpperCase();
                    boolean isBlobOnlyFile = upperFileName.startsWith("DPT-URA-LOG-D2-") ||
                                            upperFileName.startsWith("DPT-URA-LOG-PDF-") ||
                                            upperFileName.startsWith("DPT-URA-PDF-D2-");

                    // Step 3a: Upload encrypted file to blob storage
                    try {
                        // Construct blob path similar to LTA pattern
                        String encryptedBlobPath = blobFolderPath;
                        if (!encryptedBlobPath.endsWith("/") && !fileName.startsWith("/")) {
                            encryptedBlobPath += "/";
                        }
                        encryptedBlobPath += fileName;

                        log.info("Uploading encrypted file to blob storage: {}", encryptedBlobPath);

                        // Download encrypted file from SFTP
                        byte[] encryptedContent = downloadHelper.downloadEncryptedFile(
                            SFTP_SERVER,
                            sftpDownloadDirectory,
                            fileName
                        );

                        if (encryptedContent != null && encryptedContent.length > 0) {
                            // Upload to blob storage using same method as LTA
                            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                                azureBlobStorageUtil.uploadBytesToBlob(encryptedContent, encryptedBlobPath);

                            if (uploadResponse.isSuccess()) {
                                log.info("Successfully uploaded encrypted file to blob: {} ({} bytes)",
                                    encryptedBlobPath, uploadResponse.getFileSize());
                            } else {
                                log.error("Failed to upload encrypted file to blob: {}",
                                    uploadResponse.getErrorMessage());
                                errors.add("Failed to upload encrypted file to blob: " + fileName);
                                continue; // Skip this file - don't delete from SFTP if upload failed
                            }
                        } else {
                            log.error("Failed to download encrypted file from SFTP: {}", fileName);
                            errors.add("Failed to download encrypted file from SFTP: " + fileName);
                            continue; // Skip this file
                        }
                    } catch (Exception uploadError) {
                        log.error("Error uploading encrypted file {}: {}", fileName, uploadError.getMessage(), uploadError);
                        errors.add("Error uploading encrypted file: " + fileName + " - " + uploadError.getMessage());
                        continue; // Skip this file - don't delete from SFTP if upload failed
                    }

                    // For LOG and PDF files, only upload to blob - skip parsing and database updates
                    if (isBlobOnlyFile) {
                        log.info("File {} is a blob-only file (LOG/PDF acknowledgement). Skipping parsing and database updates.", baseFileName);

                        // Upload decrypted file to blob storage
                        try {
                            String decryptedBlobPath = blobFolderPath;
                            if (!decryptedBlobPath.endsWith("/") && !baseFileName.startsWith("/")) {
                                decryptedBlobPath += "/";
                            }
                            decryptedBlobPath += baseFileName;

                            log.info("Uploading decrypted blob-only file to blob storage: {}", decryptedBlobPath);

                            AzureBlobStorageUtil.FileUploadResponse decryptedUploadResponse =
                                azureBlobStorageUtil.uploadBytesToBlob(fileContent, decryptedBlobPath);

                            if (decryptedUploadResponse.isSuccess()) {
                                log.info("Successfully uploaded decrypted blob-only file: {} ({} bytes)",
                                    decryptedBlobPath, decryptedUploadResponse.getFileSize());
                            } else {
                                log.warn("Failed to upload decrypted blob-only file to blob: {}",
                                    decryptedUploadResponse.getErrorMessage());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to upload decrypted blob-only file to blob storage: {}", baseFileName, e);
                        }

                        // Delete from SFTP after successful blob upload
                        boolean deleted = downloadHelper.deleteFromSftp(SFTP_SERVER, sftpDownloadDirectory, fileName);
                        if (deleted) {
                            processedFiles.add(fileName);
                            log.info("Successfully processed and deleted blob-only file: {}", fileName);
                        } else {
                            log.error("SFTP_DELETE_FAILED: Blob-only file {} was uploaded but could not be deleted from SFTP", fileName);
                            errors.add("SFTP deletion failed for blob-only file: " + fileName);
                        }

                        continue; // Skip to next file - no parsing or database updates needed
                    }

                    // Parse the decrypted file content based on file type and format
                    Map<String, Object> parseResult = responseParser.parseResponseFileContent(fileContent, baseFileName);

                    // Skip files that cannot be parsed (may be corrupted or unexpected format)
                    if (parseResult == null || parseResult.isEmpty()) {
                        log.warn("Failed to parse response file: {}", fileName);
                        errors.add("Failed to parse response file: " + fileName);
                        continue;
                    }

                    // Step 3b: Upload decrypted file to blob storage
                    // Follow same pattern as LTA - upload after successful parse
                    try {
                        // Construct blob path for decrypted content
                        String decryptedBlobPath = blobFolderPath;
                        if (!decryptedBlobPath.endsWith("/") && !baseFileName.startsWith("/")) {
                            decryptedBlobPath += "/";
                        }
                        decryptedBlobPath += baseFileName;

                        log.info("Uploading decrypted file to blob storage: {}", decryptedBlobPath);

                        // Upload to Azure Blob Storage
                        AzureBlobStorageUtil.FileUploadResponse decryptedUploadResponse =
                            azureBlobStorageUtil.uploadBytesToBlob(fileContent, decryptedBlobPath);

                        if (decryptedUploadResponse.isSuccess()) {
                            log.info("Successfully uploaded decrypted file to blob: {} ({} bytes)",
                                decryptedBlobPath, decryptedUploadResponse.getFileSize());
                        } else {
                            log.warn("Failed to upload decrypted file to blob: {}, Error: {}",
                                decryptedBlobPath, decryptedUploadResponse.getErrorMessage());
                        }
                    } catch (Exception e) {
                        // Log but don't fail the process if blob upload fails
                        // This follows LTA pattern - decrypted upload failure doesn't stop processing
                        log.warn("Failed to upload decrypted file to blob storage: {}", baseFileName, e);
                    }
                    
                    // Extract processing results from parsed file
                    // Stage indicates which workflow step this response relates to
                    String stage = (String) parseResult.get("stage");  // RD1, RD2, RR3, DN1, DN2, DR3
                    String status = (String) parseResult.get("status");  // Processing status from Toppan
                    List<String> successfulNotices = (List<String>) parseResult.get("successfulNotices");
                    List<String> failedNotices = (List<String>) parseResult.get("failedNotices");
                    Integer successfulCount = (Integer) parseResult.get("successfulCount");
                    // Postal registration numbers only available for RD2/DN2 registered mail
                    Map<String, String> postalRegistrationNumbers = (Map<String, String>) parseResult.get("postalRegistrationNumbers");

                    // TP-PARSE-01: De-duplicate notice lists before database operations
                    if (successfulNotices != null && !successfulNotices.isEmpty()) {
                        int beforeSize = successfulNotices.size();
                        successfulNotices = new ArrayList<>(new LinkedHashSet<>(successfulNotices));
                        int duplicates = beforeSize - successfulNotices.size();
                        if (duplicates > 0) {
                            log.warn("Removed {} duplicate successful notices for stage {}", duplicates, stage);
                        }
                    }

                    if (failedNotices != null && !failedNotices.isEmpty()) {
                        int beforeSize = failedNotices.size();
                        failedNotices = new ArrayList<>(new LinkedHashSet<>(failedNotices));
                        int duplicates = beforeSize - failedNotices.size();
                        if (duplicates > 0) {
                            log.warn("Removed {} duplicate failed notices for stage {}", duplicates, stage);
                        }
                    }

                    log.info("Response for stage {}: Status={}, Successful={}, Failed={}",
                        stage, status,
                        successfulNotices != null ? successfulNotices.size() : 0,
                        failedNotices != null ? failedNotices.size() : 0);
                    
                    // Step 4: Process successful notices - Advance them to next stage
                    // Handles all workflow transitions:
                    // Owner Path: RD1→RD2, RD2→RR3, RR3→CPC
                    // Driver Path: DN1→DN2, DN2→DR3, DR3→CPC
                    if (successfulNotices != null && !successfulNotices.isEmpty()) {
                        log.info("Processing {} successful notices for stage {}", successfulNotices.size(), stage);
                        
                        // Update database with stage transitions and include postal registration
                        // numbers if available (only for RD2/DN2 registered mail stages)
                        Map<String, Object> transitionResult;
                        if (postalRegistrationNumbers != null && !postalRegistrationNumbers.isEmpty()) {
                            log.info("Processing stage transitions with {} postal registration numbers",
                                    postalRegistrationNumbers.size());
                            transitionResult = stageTransitionService.processStageTransitions(
                                stage,
                                successfulNotices,
                                processingDate,
                                postalRegistrationNumbers,
                                baseFileName
                            );
                        } else {
                            // Standard stage transition without postal registration numbers
                            transitionResult = stageTransitionService.processStageTransitions(
                                stage,
                                successfulNotices,
                                processingDate,
                                baseFileName
                            );
                        }
                        
                        // Track processing statistics for job result reporting
                        int updated = (int) transitionResult.getOrDefault("updatedCount", 0);
                        int errorCount = (int) transitionResult.getOrDefault("errorCount", 0);

                        totalProcessed += updated;
                        if (errorCount > 0) {
                            errors.add("Stage transition errors for " + stage + ": " + errorCount + " notices failed");
                        }

                        log.info("Stage transition results for {}: Updated={}, Errors={}", stage, updated, errorCount);
                    } else if (successfulCount != null && successfulCount > 0) {
                        // Handle case where Toppan reports successful count but doesn't list individual notices
                        // This can occur in summary-only acknowledgement files
                        log.warn("Stage {} has {} successful notices but individual notice numbers not available. " +
                                "Manual reconciliation may be required.", stage, successfulCount);
                        // TODO: Consider querying database for notices in this stage to process them
                    }
                    
                    // Step 5: Handle failed notices - Mark as failed without advancing stage
                    // Failed notices remain at current stage for potential retry or manual intervention
                    if (failedNotices != null && !failedNotices.isEmpty()) {
                        log.warn("Found {} failed notices for stage {}", failedNotices.size(), stage);
                    }
                    
                    // Step 6: Archive processed file for audit trail and troubleshooting
                    // COMMENTED OUT: Archive is already handled when uploading to blob storage
                    // String localFilePath = localDownloadDirectory + "/" + fileName;
                    // boolean archiveSuccess = false;
                    // try {
                    //     java.nio.file.Files.write(java.nio.file.Paths.get(localFilePath), fileContent);
                    //     downloadHelper.archiveProcessedFile(localFilePath, stage, processingDate);
                    //     archiveSuccess = true;
                    // } catch (Exception archiveError) {
                    //     log.error("Failed to archive file {}: {}", fileName, archiveError.getMessage());
                    //     errors.add("Failed to archive file: " + fileName + " - " + archiveError.getMessage());
                    //     // Don't delete from SFTP if archive fails - keep for retry
                    //     continue;
                    // }

                    // Step 7: Remove processed file from SFTP to prevent reprocessing
                    // Delete after successful uploads (blob storage already handles archiving)
                    // if (archiveSuccess) {
                    if (true) {  // Skip archive check, blob storage handles it
                        // TP-CLN-01: Track SFTP deletion failures
                        boolean deleted = downloadHelper.deleteFromSftp(SFTP_SERVER, sftpDownloadDirectory, fileName);

                        if (deleted) {
                            processedFiles.add(fileName);
                        } else {
                            log.error("SFTP_DELETE_FAILED: File {} was processed and archived but could not be deleted from SFTP. Manual cleanup required.", fileName);
                            errors.add("SFTP deletion failed for processed file: " + fileName + " (manual cleanup required)");
                        }
                    }
                    
                } catch (Exception e) {
                    // Log individual file processing errors but continue with remaining files
                    log.error("Error processing response file {}: {}", fileName, e.getMessage(), e);
                    errors.add("Error processing file: " + fileName + " - " + e.getMessage());
                }
            }
            
            // Clear any cached decrypted files to free memory
            downloadHelper.clearCache();

            // Record final job metadata
            recordJobMetadata("filesProcessed", String.valueOf(decryptedFiles.size()));
            recordJobMetadata("noticesUpdated", String.valueOf(totalProcessed));
            recordJobMetadata("filesDeleted", String.valueOf(processedFiles.size()));
            recordJobMetadata("errorCount", String.valueOf(errors.size()));

            // Return result following MHA pattern
            if (errors.isEmpty()) {
                return new JobResult(true, String.format(
                        "Successfully processed %d files and updated %d notices",
                        decryptedFiles.size(), totalProcessed));
            } else {
                return new JobResult(false, String.format(
                        "Processed %d files with %d errors: %s",
                        decryptedFiles.size(), errors.size(), String.join("; ", errors)));
            }
            
        } catch (Exception e) {
            log.error("Critical error in Toppan download job: {}", e.getMessage(), e);
            // Throw exception to trigger framework error handling and notifications
            throw new RuntimeException("Toppan download job failed: " + e.getMessage(), e);
        }
    }

}