package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.jobs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.agencyFileExchange.AgencyFileExchangeService;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptUploadProcessor;
import com.ocmsintranet.cronservice.framework.utilities.FileContentConverter;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.helpers.LtaUploadHelper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * LTA Upload Job - Fire-and-Forget Flow
 *
 * This job uses a fire-and-forget pattern:
 * 1. Query valid offence notices using LtaUploadHelper
 * 2. Generate LTA request file content using AgencyFileExchangeService
 * 3. Request token from COMCRYPT (stores requestId, operationType, fileName in DB)
 * 4. Upload ORIGINAL file to Azure Blob Storage (for callback to download later)
 * 5. Upload ORIGINAL file to SFTP server
 * 6. Update processing stage
 * 7. Job DONE - returns immediately
 *
 * When COMCRYPT calls the callback endpoint:
 * - ComcryptCallbackHelper downloads file from blob
 * - Encrypts with received token
 * - Uploads ENCRYPTED file to SFTP
 */
@Slf4j
@Component
public class LtaUploadJob extends TrackedCronJobTemplate {

    private static final String AGENCY_TYPE = "LTA";
    private static final String SFTP_SERVER = "lta";
    
    @Value("${sftp.folders.lta.upload:/upload}")
    private String sftpDirectory;
    
    @Value("${blob.folder.lta.upload:offence/lta/vrls/input}")
    private String blobFolderPath;

    private final LtaUploadHelper ltaUploadHelper;
    private final AgencyFileExchangeService agencyFileExchangeService;
    private final ComcryptUploadProcessor uploadProcessor;
    private final ComcryptTokenHelper comcryptTokenHelper;

    @Value("${lta.file.prefix:URA2LTA_}")
    private String filePrefix;

    @Value("${ocms.comcrypt.appcode.ltavrls.encrypt}")
    private String appCode;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    public LtaUploadJob(
            LtaUploadHelper ltaUploadHelper,
            AgencyFileExchangeService agencyFileExchangeService,
            ComcryptUploadProcessor uploadProcessor,
            ComcryptTokenHelper comcryptTokenHelper) {
        this.ltaUploadHelper = ltaUploadHelper;
        this.agencyFileExchangeService = agencyFileExchangeService;
        this.uploadProcessor = uploadProcessor;
        this.comcryptTokenHelper = comcryptTokenHelper;
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

    @Value("${cron.lta.upload.shedlock.name:LtaUploadJob}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }
    
    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for LTA upload job");
        
        try {
            // Check if required dependencies are initialized
            if (ltaUploadHelper == null) {
                log.error("LtaUploadHelper is not initialized");
                return false;
            }
            
            if (agencyFileExchangeService == null) {
                log.error("AgencyFileExchangeService is not initialized");
                return false;
            }
            
            if (uploadProcessor == null) {
                log.error("ComcryptUploadProcessor is not initialized");
                return false;
            }

            if (comcryptTokenHelper == null) {
                log.error("ComcryptTokenHelper is not initialized");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating pre-conditions: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        // Call parent initialize which records job start in history
        super.initialize();
        
        // Initialize job metadata
        jobMetadata.clear();
        jobMetadata.put("jobStartTime", LocalDateTime.now().toString());
        
        // Log configuration
        log.info("LTA Upload Job Configuration:");
        log.info("  SFTP Directory: {}", sftpDirectory);
        log.info("  Blob Folder Path: {}", blobFolderPath);
        log.info("  SFTP Server: {}", SFTP_SERVER);
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up LTA upload job");
        // No specific cleanup needed
    }

    @Override
    protected JobResult doExecute() {
        log.info("Starting LTA upload job execution");

        try {
            // Query offence notice data that needs to be uploaded to LTA
            List<Map<String, Object>> offenceNotices;
            try {
                offenceNotices = ltaUploadHelper.queryLtaUploadData();
                if (offenceNotices.isEmpty()) {
                    log.info("No offence notices found for LTA upload. Job completed successfully with no action.");
                    return new JobResult(true, "No offence notices found for LTA upload. Metrics: recordsSubmitted: 0");
                }
            } catch (Exception e) {
                log.error("Error querying offence notice data: {}", e.getMessage(), e);
                return new JobResult(false, "Error querying offence notice data: " + e.getMessage());
            }
            
            log.info("Found {} offence notices for LTA upload", offenceNotices.size());
            
            // Generate a timestamp for the file name
            LocalDateTime timestamp = LocalDateTime.now();
            
            // Generate the file name and content
            String requestFileName = agencyFileExchangeService.generateRequestFilename(AGENCY_TYPE, timestamp);
            byte[] requestFileContent = agencyFileExchangeService.createRequestFileContent(
                    AGENCY_TYPE, 
                    offenceNotices,
                    timestamp);
            
            // Convert to human-readable string for logging
            String readableContent = FileContentConverter.bytesToString(requestFileContent);
            String contentPreview = readableContent.length() > 500 
                    ? readableContent.substring(0, 500) + "..." 
                    : readableContent;
            log.info("File content preview (human-readable): {}", contentPreview);
            
            // Log the file size and encoding
            log.info("File size: {} bytes, File name: {}", requestFileContent.length, requestFileName);

            if (requestFileContent == null || requestFileContent.length == 0) {
                log.error("Failed to generate file content for LTA upload");
                return new JobResult(false, "Failed to generate file content for LTA upload");
            }
            
            log.info("Generated request file: {} with {} bytes", requestFileName, requestFileContent.length);
            
            // Log a preview of the content for verification (first 200 chars)
            if (readableContent != null && readableContent.length() > 0) {
                log.info("File content preview: {}\n", contentPreview);
            }
            
            // REMOVE LATER - Extract container and directory from blobFolderPath
            Boolean isSuccessOriginal = false;
            String[] parts = blobFolderPath.trim().split("/", 2);
            String container = parts[0];
            String directory = parts.length > 1 ? parts[1] : "";
            String fullSftpPath = sftpDirectory + "/" + requestFileName;
            
            try {
                // Upload file to both Azure Blob Storage and SFTP
                String blobPath = uploadProcessor.uploadToBlob(container, directory, requestFileContent, requestFileName);
                String sftpPath = uploadProcessor.uploadToSftp(requestFileContent, fullSftpPath, SFTP_SERVER);
                isSuccessOriginal = true;

                log.info("Successfully uploaded file to Azure Blob Storage: {} and SFTP: {}", blobPath, sftpPath);
                recordJobMetadata("blobPath", blobPath);
                recordJobMetadata("sftpPath", sftpPath);
            } catch (Exception e) {
                log.error("Error during direct upload: {}", e.getMessage(), e);
                return new JobResult(false, "Error during direct upload: " + e.getMessage());
            }

            // Request token from COMCRYPT (fire-and-forget)
            // ComcryptTokenHelper.requestToken creates operation record (requestId, operationType, fileName) and returns requestId
            String metadata = String.format("LTA Upload - %d records", offenceNotices.size());
            String requestId = null;
            try {
                requestId = comcryptTokenHelper.requestToken(
                        appCode,
                        SystemConstant.CryptOperation.ENCRYPTION,
                        requestFileName,
                        metadata);
                if (requestId == null) {
                    log.error("Failed to request COMCRYPT token - requestId is null");
                    //return new JobResult(false, "Failed to request COMCRYPT token");
                }
                log.info("COMCRYPT token requested successfully, requestId: {} (fire-and-forget)", requestId);
                recordJobMetadata("requestId", requestId);
            } catch (Exception e) {
                log.error("Error requesting COMCRYPT token: {}", e.getMessage(), e);
                // return new JobResult(false, "Error requesting COMCRYPT token: " + e.getMessage());
            }

            // Update processing stage
            if (requestId != null || isSuccessOriginal) {
                try {
                    ltaUploadHelper.updateProcessingStage(offenceNotices);
                    log.info("Successfully updated processing stage for {} notices", offenceNotices.size());
                    recordJobMetadata("processingStageUpdated", "true");
                } catch (Exception e) {
                    log.error("Error updating processing stage: {}", e.getMessage(), e);
                    recordJobMetadata("processingStageUpdated", "false");
                    recordJobMetadata("processingStageError", e.getMessage());
                }
            }
            
            recordJobMetadata("requestFileName", requestFileName);
            recordJobMetadata("recordCount", String.valueOf(offenceNotices.size()));

            // Job completed - callback will handle encryption and encrypted file upload
            log.info("LTA upload job completed successfully (fire-and-forget)");
            log.info("Callback will handle: download from blob -> encrypt -> upload encrypted to SFTP");
            return new JobResult(true, String.format(
                    "Successfully processed %d records. Original file uploaded to blob (%s) and SFTP (%s). " +
                    "Awaiting COMCRYPT callback to complete encryption. Metrics: recordsSubmitted: %d",
                    offenceNotices.size(), directory, fullSftpPath, offenceNotices.size()));            
        } catch (Exception e) {
            log.error("Error executing LTA upload job", e);
            return new JobResult(false, "Error executing LTA upload job: " + e.getMessage());
        }
    }
    
    /**
     * Get job metadata for reporting purposes
     * 
     * @return Map of job metadata
     */
    public Map<String, String> getJobMetadata() {
        return new HashMap<>(jobMetadata);
    }
}
