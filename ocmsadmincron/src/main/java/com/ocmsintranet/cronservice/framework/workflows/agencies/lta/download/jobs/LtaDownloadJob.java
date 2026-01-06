package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.jobs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers.LtaDownloadHelper;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * LTA Download Job - Fire-and-Forget Flow
 *
 * This job uses a fire-and-forget pattern for encrypted files:
 * 1. List files in LTA SFTP output directory
 * 2. For unencrypted files: Download directly, process, delete from SFTP
 * 3. For encrypted files (.p7):
 *    a. Download encrypted file from SFTP
 *    b. Upload encrypted file to Azure Blob Storage
 *    c. Request decryption token from COMCRYPT (fire-and-forget)
 *    d. Job returns - callback will handle decryption
 *
 * When COMCRYPT calls the callback endpoint:
 * - ComcryptCallbackHelper downloads encrypted file from blob
 * - Decrypts with received token
 * - Saves decrypted file to blob
 * - Processes the decrypted content
 * - Deletes from SFTP
 */
@Slf4j
@Component
public class LtaDownloadJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String SFTP_SERVER = "lta";
    
    @Value("${blob.folder.lta.download:/offence/lta/download/}")
    private String blobFolderPath;
    
    @Value("${sftp.folders.lta.download:/nro/output}")
    private String sftpDirectory;
        
    // File pattern constants for LTA response files
    private static final Pattern LTA_RESPONSE_FILE_PATTERN = Pattern.compile("VRL-URA-OFFREPLY-D2-(\\d{14})");
    
    // Dependencies
    private final SftpUtil sftpUtil;
    private final LtaDownloadHelper ltaDownloadHelper;
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    private final ComcryptTokenHelper comcryptTokenHelper;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    // Store file content after download (for unencrypted files only)
    private final Map<String, byte[]> downloadedFiles = new ConcurrentHashMap<>();

    @Value("${ocms.comcrypt.appcode.ltavrls.decrypt}")
    private String appCode;

    public LtaDownloadJob(
            SftpUtil sftpUtil,
            LtaDownloadHelper ltaDownloadHelper,
            AzureBlobStorageUtil azureBlobStorageUtil,
            ComcryptTokenHelper comcryptTokenHelper) {
        this.sftpUtil = sftpUtil;
        this.ltaDownloadHelper = ltaDownloadHelper;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
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

    @Value("${cron.lta.download.shedlock.name:LtaDownloadJob}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for LTA download job");

        try {
            // Check if required dependencies are initialized
            if (sftpUtil == null) {
                log.error("SftpUtil is not initialized");
                return false;
            }

            if (ltaDownloadHelper == null) {
                log.error("LtaDownloadHelper is not initialized");
                return false;
            }

            if (azureBlobStorageUtil == null) {
                log.error("AzureBlobStorageUtil is not initialized");
                return false;
            }

            if (comcryptTokenHelper == null) {
                log.error("ComcryptTokenHelper is not initialized");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating pre-conditions", e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        // Call parent initialize first
        super.initialize();

        log.info("Initializing LTA download job");
        jobMetadata.clear();
        downloadedFiles.clear();

        // Log configuration
        log.info("LTA Download Job Configuration:");
        log.info("  SFTP Directory: {}", sftpDirectory);
        log.info("  Blob Folder Path: {}", blobFolderPath);
        log.info("  SFTP Server: {}", SFTP_SERVER);
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up LTA download job");
        // Nothing specific to clean up
        
        // Call parent cleanup
        super.cleanup();
    }

    @Override
    protected JobResult doExecute() {
        log.info("Executing LTA download job");
        
        try {
            // Step 1: List files in LTA SFTP output directory
            log.info("Listing LTA response files from SFTP directory: {}", sftpDirectory);
            List<String> files = listLtaResponseFiles();
            
            if (files.isEmpty()) {
                log.info("No LTA response files found in SFTP directory");
                return new JobResult(true, "No LTA response files found to process. Metrics: filesFound: 0, successfulGroups: 0, failedGroups: 0"); // No files to process is not an error
            }
            
            log.info("Found {} LTA response files", files.size());
            recordJobMetadata("filesFound", String.valueOf(files.size()));
            
            // Step 2: Group files by timestamp for processing
            Map<String, List<String>> fileGroups = groupFilesByTimestamp(files);
            log.info("Found {} file groups to process", fileGroups.size());
            
            // Step 3: Process each file group
            int successCount = 0;
            int failCount = 0;
            
            // We no longer need to track processed files here since each file group handles its own deletion
            for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()) {
                String timestamp = entry.getKey();
                List<String> groupFiles = entry.getValue();
                
                log.info("Processing file group with timestamp {}", timestamp);
                boolean success = processFileGroup(timestamp, groupFiles);
                
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            
            recordJobMetadata("successfulGroups", String.valueOf(successCount));
            recordJobMetadata("failedGroups", String.valueOf(failCount));
            
            log.info("LTA download job completed: {} successful groups, {} failed groups", 
                    successCount, failCount);
            
            // Job is considered successful if at least one file group was processed successfully or if there were no files
            if (successCount > 0 || files.isEmpty()) {
                return new JobResult(true, String.format(
                        "Successfully processed %d file groups, %d failed. Metrics: filesFound: %d, successfulGroups: %d, failedGroups: %d",
                        successCount, failCount, files.size(), successCount, failCount));
            } else {
                return new JobResult(false, String.format(
                        "Failed to process all %d file groups. Metrics: filesFound: %d, successfulGroups: 0, failedGroups: %d",
                        fileGroups.size(), files.size(), fileGroups.size()));
            }
            
        } catch (Exception e) {
            log.error("Error executing LTA download job", e);
            return new JobResult(false, "Error executing LTA download job: " + e.getMessage());
        }
    }
    
    /**
     * List LTA response files available in the SFTP directory
     * 
     * @return List of filenames
     */
    private List<String> listLtaResponseFiles() {
        log.info("Listing LTA response files from SFTP directory: {}", sftpDirectory);
        List<String> files = new ArrayList<>();
        
        try {
            // List files in the SFTP directory
            List<String> allFiles = sftpUtil.listFiles(SFTP_SERVER, sftpDirectory);
            
            // Filter for LTA response files (both encrypted and unencrypted)
            for (String file : allFiles) {
                if (LTA_RESPONSE_FILE_PATTERN.matcher(file).find()) {
                    files.add(file);
                    log.debug("Found LTA response file: {} (encrypted: {})", 
                            file, file.endsWith(".p7"));
                }
            }
            
            log.info("Found {} LTA response files", files.size());
        } catch (Exception e) {
            log.error("Error listing LTA response files", e);
            throw new RuntimeException("Error listing LTA response files", e);
        }
        
        return files;
    }
    
    /**
     * Group files by timestamp to process related files together
     * 
     * @param files List of filenames
     * @return Map of timestamp to list of files
     */
    private Map<String, List<String>> groupFilesByTimestamp(List<String> files) {
        Map<String, List<String>> groups = new HashMap<>();
        
        for (String file : files) {
            String timestamp = extractTimestamp(file);
            if (timestamp != null) {
                groups.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(file);
            }
        }
        
        return groups;
    }
    
    /**
     * Extract timestamp from filename
     * 
     * @param filename Filename to extract timestamp from
     * @return Timestamp string or null if not found
     */
    private String extractTimestamp(String filename) {
        // Extract timestamp from LTA response file
        Matcher responseMatcher = LTA_RESPONSE_FILE_PATTERN.matcher(filename);
        if (responseMatcher.find()) {
            return responseMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Process a file group (files with the same timestamp)
     * 
     * @param timestamp Timestamp of the file group
     * @param files List of files in the group
     * @return true if processing was successful
     */
    private boolean processFileGroup(String timestamp, List<String> files) {
        log.info("Processing file group with timestamp {}: {} files", timestamp, files.size());
        
        try {
            // Find the main response file (should be only one per timestamp)
            String responseFile = null;
            for (String file : files) {
                if (file.contains("OFFREPLY")) {
                    responseFile = file;
                    break;
                }
            }
            
            if (responseFile == null) {
                log.error("No response file found in group with timestamp {}", timestamp);
                recordJobMetadata("error_type", "missing_response_file");
                recordJobMetadata("error_details", "No response file found in group with timestamp " + timestamp);
                return false;
            }
            
            log.info("Found response file: {}", responseFile);
            
            // NOTE: Filename-based error detection removed
            // Error codes A, B, C are now detected from the file content
            // in the LtaResponseFileParser class
            
            // Download and decrypt the file
            byte[] decryptedContent = downloadAndDecryptFile(responseFile);

            // Check if this is an encrypted file pending callback (fire-and-forget)
            boolean isEncrypted = responseFile.endsWith(".p7");
            if (decryptedContent == null && isEncrypted) {
                // Fire-and-forget: encrypted file uploaded to blob, token requested
                // Callback will handle: decrypt → process → delete from SFTP
                log.info("Encrypted file {} submitted for async decryption (fire-and-forget)", responseFile);
                recordJobMetadata("status_" + responseFile, "pending_decryption");
                return true; // Return success - processing will continue via callback
            }

            if (decryptedContent == null) {
                log.error("Failed to download or decrypt file: {}", responseFile);
                recordJobMetadata("error_type", "decryption_failure");
                recordJobMetadata("error_details", "Failed to download or decrypt file: " + responseFile);
                return false;
            }

            // Process the decrypted file (for unencrypted files only)
            boolean processed = ltaDownloadHelper.processLtaResponseFile(decryptedContent);
            if (!processed) {
                log.error("Failed to process LTA response file: {}", responseFile);

                // Record the error in job metadata
                recordJobMetadata("error_type", "file_integrity_error");
                recordJobMetadata("error_details", "Error detected in file content: " + responseFile);

                // Delete the file with error from SFTP server
                log.info("Deleting file with error from SFTP server: {}", responseFile);
                List<String> filesToDelete = new ArrayList<>();
                filesToDelete.add(responseFile);
                deleteProcessedFiles(filesToDelete);

                return false;
            }

            // Delete the SFTP file after successful processing (for unencrypted files)
            log.info("Deleting processed file from SFTP server: {}", responseFile);
            List<String> filesToDelete = new ArrayList<>();
            filesToDelete.add(responseFile);
            deleteProcessedFiles(filesToDelete);
            log.info("SFTP file deletion completed for: {}", responseFile);

            return true;
            
        } catch (Exception e) {
            log.error("Error processing LTA file group", e);
            recordJobMetadata("error_type", "processing_exception");
            recordJobMetadata("error_details", e.getMessage());
            return false;
        }
    }
    
    /**
     * Download and decrypt a file from SFTP
     * Auto-detects whether file is encrypted (.p7) or unencrypted and processes accordingly
     * 
     * @param filename Filename to download and decrypt
     * @return Decrypted file content as byte array, or null if failed
     */
    private byte[] downloadAndDecryptFile(String filename) {
        // Check if we already have this file downloaded/decrypted
        if (downloadedFiles.containsKey(filename)) {
            log.info("Using cached content for file: {}", filename);
            return downloadedFiles.get(filename);
        }
        
        // Construct full SFTP path by combining directory and filename
        String fullSftpPath = sftpDirectory;
        if (!fullSftpPath.endsWith("/") && !filename.startsWith("/")) {
            fullSftpPath += "/";
        }
        fullSftpPath += filename;
        
        // Auto-detect file type and choose appropriate processing method
        boolean isEncrypted = filename.endsWith(".p7");
        log.info("Auto-detected file type for {}: encrypted={}", filename, isEncrypted);
        
        // Record file type in metadata
        recordJobMetadata("fileType_" + filename, isEncrypted ? "encrypted" : "unencrypted");
        
        if (isEncrypted) {
            log.info("Processing encrypted file with SLIFT: {}", filename);
            return downloadAndDecryptWithSlift(filename, fullSftpPath);
        } else {
            log.info("Processing unencrypted file directly: {}", filename);
            return downloadDirectlyFromSftp(filename, fullSftpPath);
        }
    }
    
    /**
     * Download file directly from SFTP without encryption
     * 
     * @param filename Filename to download
     * @param fullSftpPath Full SFTP path
     * @return File content as byte array, or null if failed
     */
    private byte[] downloadDirectlyFromSftp(String filename, String fullSftpPath) {
        log.info("Downloading file directly from SFTP (no encryption): {}", filename);
        
        try {
            log.info("Downloading file from SFTP path: {}", fullSftpPath);
            
            // Use SftpUtil to download the file directly
            byte[] content = sftpUtil.downloadFile(SFTP_SERVER, fullSftpPath);
            
            if (content == null || content.length == 0) {
                log.error("Downloaded file is empty or null: {}", filename);
                return null;
            }
            
            // Store the content for reference
            downloadedFiles.put(filename, content);
            
            // Upload to blob storage for consistency with the encrypted flow
            try {
                // Construct the blob path
                String blobPath = blobFolderPath;
                if (!blobPath.endsWith("/") && !filename.startsWith("/")) {
                    blobPath += "/";
                }
                blobPath += filename;
                
                // Use AzureBlobStorageUtil to upload directly to blob storage
                // This bypasses the encryption flow entirely for direct uploads
                AzureBlobStorageUtil.FileUploadResponse uploadResponse = azureBlobStorageUtil.uploadBytesToBlob(
                    content,
                    blobPath
                );
                
                if (uploadResponse.isSuccess()) {
                    log.info("Successfully uploaded file to blob storage: {}", blobPath);
                } else {
                    log.warn("Failed to upload file to blob storage: {}, Error: {}", 
                            blobPath, uploadResponse.getErrorMessage());
                }
            } catch (Exception e) {
                // Log but don't fail the process if blob upload fails
                log.warn("Failed to upload file to blob storage: {}", filename, e);
            }
            
            log.info("Successfully downloaded file directly: {} ({} bytes)", filename, content.length);
            return content;
            
        } catch (Exception e) {
            log.error("Error downloading file directly from SFTP: {}", filename, e);
            return null;
        }
    }
    
    /**
     * Download encrypted file and request decryption token (fire-and-forget)
     *
     * @param filename Filename to download
     * @param fullSftpPath Full SFTP path
     * @return null - decryption will be handled by callback
     */
    private byte[] downloadAndDecryptWithSlift(String filename, String fullSftpPath) {
        log.info("Requesting decryption token (fire-and-forget): {}", filename);

        try {
            // Step 1: Request decryption token (fire-and-forget)
            // Encrypted file will be downloaded from SFTP in callback
            String metadata = String.format("LTA Download - %s", filename);
            String requestId = comcryptTokenHelper.requestToken(
                    appCode,
                    SystemConstant.CryptOperation.DECRYPTION,
                    filename,
                    metadata);

            if (requestId == null) {
                log.error("Failed to request COMCRYPT decryption token for: {}", filename);
                return null;
            }

            log.info("COMCRYPT decryption token requested successfully, requestId: {} (fire-and-forget)", requestId);
            recordJobMetadata("requestId_" + filename, requestId);
            recordJobMetadata("sftpPath_" + filename, fullSftpPath);

            // Return null - decryption will be handled by callback
            // Callback will: download encrypted from SFTP → decrypt → process → delete from SFTP
            log.info("Fire-and-forget: Awaiting callback for decryption of file {}", filename);
            return null;

        } catch (Exception e) {
            log.error("Error in fire-and-forget download for file: {}", filename, e);
            return null;
        }
    }
    
    /**
     * Delete processed files from SFTP server
     * 
     * @param files List of files to delete
     */
    private void deleteProcessedFiles(List<String> files) {
        log.info("Deleting {} processed files from SFTP server", files.size());
        
        for (String file : files) {
            try {
                // Construct full SFTP path by combining directory and filename
                String fullSftpPath = sftpDirectory;
                if (!fullSftpPath.endsWith("/") && !file.startsWith("/")) {
                    fullSftpPath += "/";
                }
                fullSftpPath += file;
                
                log.info("Deleting file from SFTP path: {}", fullSftpPath);
                boolean deleted = sftpUtil.deleteFile(SFTP_SERVER, fullSftpPath);
                if (deleted) {
                    log.info("Successfully deleted file: {}", file);
                } else {
                    log.warn("Failed to delete file: {}", file);
                }
            } catch (Exception e) {
                log.error("Error deleting file: {}", file, e);
                // Continue with other files even if one fails
            }
        }
    }

}
