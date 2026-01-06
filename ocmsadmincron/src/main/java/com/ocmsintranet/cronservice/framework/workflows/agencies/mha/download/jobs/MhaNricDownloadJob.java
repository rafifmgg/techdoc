package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.jobs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.ComcryptFileProcessingService;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.helpers.MhaNricDownloadHelper;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.helpers.MhaNricRecordValidator;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * MHA NRIC Download Job.
 * This job performs the following steps:
 * 1. List files in MHA SFTP output directory
 * 2. Download and decrypt MHA response files using SliftFileProcessingService
 * 3. Process the files:
 *    a. Main response file (NRO2URA_YYYYMMDDHHMMSS)
 *    b. Control totals report (REPORT_YYYYMMDDHHMMSS.TOT)
 *    c. Exceptions report (REPORT_YYYYMMDDHHMMSS.EXP)
 * 4. Update database records based on verification results
 * 5. Delete processed files from SFTP server
 */
@Slf4j
@Component
public class MhaNricDownloadJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String SFTP_SERVER = "mha";
    
    @Value("${blob.folder.mha.download:/offence/nro/output/}")
    private String blobFolderPath;
    
    @Value("${sftp.folders.mha.download:/nro/output}")
    private String sftpDirectory;
    
    @Value("${sftp.servers.mha.encryption:true}")
    private boolean useEncryption;
    
    // File pattern constants
    private static final Pattern RESPONSE_FILE_PATTERN = Pattern.compile("NRO2URA_(\\d{14})");
    private static final Pattern REPORT_TOT_PATTERN = Pattern.compile("REPORT_(\\d{14})\\.TOT");
    private static final Pattern REPORT_EXP_PATTERN = Pattern.compile("REPORT_(\\d{14})\\.EXP");
    
    // Dependencies
    private final ComcryptFileProcessingService sliftFileProcessingService;
    private final TableQueryService tableQueryService;
    private final SftpUtil sftpUtil;
    private final MhaNricDownloadHelper mhaNricDownloadHelper;
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    private final MhaNricRecordValidator validator;
    
    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();
    
    // Store callback tokens for decryption
    private final Map<String, String> callbackTokens = new ConcurrentHashMap<>();
    
    // Store file content after decryption
    private final Map<String, byte[]> decryptedFiles = new ConcurrentHashMap<>();
    
    // MHA processing metrics
    private int totalRecordsReturned = 0;
    private int recordsRead = 0;
    private int recordsMatched = 0;
    private int invalidUinFinCount = 0;
    private int validUnmatchedCount = 0;

    public MhaNricDownloadJob(
            ComcryptFileProcessingService sliftFileProcessingService,
            TableQueryService tableQueryService,
            SftpUtil sftpUtil,
            MhaNricDownloadHelper mhaNricDownloadHelper,
            AzureBlobStorageUtil azureBlobStorageUtil,
            MhaNricRecordValidator validator) {
        this.sliftFileProcessingService = sliftFileProcessingService;
        this.tableQueryService = tableQueryService;
        this.sftpUtil = sftpUtil;
        this.mhaNricDownloadHelper = mhaNricDownloadHelper;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
        this.validator = validator;
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

    @Value("${cron.mha.download.shedlock.name:MhaNricDownloadJob}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for MHA NRIC download job");
        
        try {
            // Check if required dependencies are initialized
            if (sliftFileProcessingService == null) {
                log.error("SliftFileProcessingService is not initialized");
                return false;
            }
            
            if (tableQueryService == null) {
                log.error("TableQueryService is not initialized");
                return false;
            }
            
            if (sftpUtil == null) {
                log.error("SftpUtil is not initialized");
                return false;
            }
            
            if (mhaNricDownloadHelper == null) {
                log.error("MhaNricDownloadHelper is not initialized");
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
        // Call parent's initialize() first to set up job tracking
        super.initialize();
        
        log.info("Initializing MHA NRIC download job");
        log.info("SLIFT encryption is {}", useEncryption ? "enabled" : "disabled");
        recordJobMetadata("sliftEncryption", String.valueOf(useEncryption));
        jobMetadata.clear();
        callbackTokens.clear();
        decryptedFiles.clear();
        
        // Initialize metrics
        totalRecordsReturned = 0;
        recordsRead = 0;
        recordsMatched = 0;
        invalidUinFinCount = 0;
        validUnmatchedCount = 0;
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up MHA NRIC download job");
        // No specific cleanup needed
    }

    /**
     * Handle SLIFT token callback
     * This method is called by the MhaNricDownloadServiceImpl when a token is received from SLIFT
     * 
     * @param requestId The request ID for which the token was received
     * @param token The received token
     */
    public void handleComcryptCallback(String requestId, String token) {
        log.info("MhaNricDownloadJob: Handling SLIFT token callback for requestId: {}", requestId);
        
        try {
            // Store token in the local map for reference
            callbackTokens.put(requestId, token);
            
            // Update the SLIFT operation record with the token
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("token", token);
            // Use the TableQueryService to update the SLIFT operation record
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", requestId);
            
            // Update the operation record in the database
            tableQueryService.patch("ocms_comcrypt_operation", filters, updateFields);
            
            log.info("MhaNricDownloadJob: Successfully updated SLIFT operation with token for requestId: {}", requestId);
            
            // Process the token callback through the SLIFT service
            // This will trigger the download process with the token
            boolean processed = sliftFileProcessingService.processTokenCallback(requestId, token);
            
            if (processed) {
                log.info("MhaNricDownloadJob: Successfully processed token callback for requestId: {}", requestId);
            } else {
                log.warn("MhaNricDownloadJob: Token callback processing returned false for requestId: {}", requestId);
            }
            
        } catch (Exception e) {
            log.error("MhaNricDownloadJob: Error handling SLIFT token callback for requestId: {}: {}", 
                    requestId, e.getMessage(), e);
        }
    }

    @Override
    protected JobResult doExecute() {
        log.info("Executing MHA NRIC download job");
        
        try {
            // Get total records submitted to MHA (from the request file if available)
            // This would typically be read from the request file or database
            // Step 1: List files in MHA SFTP output directory
            List<String> availableFiles = listMhaResponseFiles();
            if (availableFiles.isEmpty()) {
                log.info("No MHA response files found in SFTP directory");
                return new JobResult(true, "No MHA response files found for processing");
            }
            
            recordJobMetadata("filesFound", String.valueOf(availableFiles.size()));
            
            // Group files by timestamp to process related files together
            Map<String, List<String>> fileGroups = groupFilesByTimestamp(availableFiles);
            log.info("Found {} file groups to process", fileGroups.size());
            
            int successfulGroups = 0;
            List<String> errors = new ArrayList<>();
            List<String> processedFiles = new ArrayList<>();
            
            // Process each group of files
            for (Map.Entry<String, List<String>> group : fileGroups.entrySet()) {
                String timestamp = group.getKey();
                List<String> files = group.getValue();
                
                try {
                    log.info("Processing file group with timestamp {}", timestamp);
                    boolean success = processFileGroup(timestamp, files);
                    
                    if (success) {
                        successfulGroups++;
                        processedFiles.addAll(files);
                    } else {
                        errors.add("Failed to process file group with timestamp " + timestamp);
                    }
                } catch (Exception e) {
                    log.error("Error processing file group with timestamp {}", timestamp, e);
                    errors.add("Error processing file group with timestamp " + timestamp + ": " + e.getMessage());
                }
            }


            
            // Delete processed files from SFTP server
            if (!processedFiles.isEmpty()) {
                try {
                    deleteProcessedFiles(processedFiles);
                    log.info("Successfully deleted {} processed files from SFTP server", processedFiles.size());
                } catch (Exception e) {
                    log.error("Error deleting processed files from SFTP server", e);
                    errors.add("Error deleting processed files: " + e.getMessage());
                }
            }
            
            // Record final job metadata
            recordJobMetadata("successfulGroups", String.valueOf(successfulGroups));
            recordJobMetadata("failedGroups", String.valueOf(fileGroups.size() - successfulGroups));
            recordJobMetadata("filesDeleted", String.valueOf(processedFiles.size()));
            
            // Record final MHA processing metrics
            recordJobMetadata("mhaRecordsRead", String.valueOf(recordsRead));
            recordJobMetadata("mhaRecordsMatched", String.valueOf(recordsMatched));
            recordJobMetadata("mhaInvalidUinFin", String.valueOf(invalidUinFinCount));
            recordJobMetadata("mhaValidUnmatched", String.valueOf(validUnmatchedCount));
            
            // Log final metrics
            log.info("MHA Processing Metrics - " +
                    "Returned: {}, Read: {}, " +
                    "Matched: {}, Invalid UIN/FIN: {}, Valid Unmatched: {}",
                    totalRecordsReturned, recordsRead,
                    recordsMatched, invalidUinFinCount, validUnmatchedCount);
            
            if (errors.isEmpty()) {
                String metrics = String.format(
                    "recordsReturned: %d, recordsRead: %d, recordsMatched: %d, invalidUinFinCount: %d, validUnmatchedCount: %d",
                    totalRecordsReturned, recordsRead, recordsMatched, invalidUinFinCount, validUnmatchedCount
                );
                
                return new JobResult(true, String.format(
                    "Successfully processed %d file groups, deleted %d files. Metrics: %s", 
                    successfulGroups, processedFiles.size(), metrics));
            } else {
                return new JobResult(false, String.format(
                        "Processed %d file groups with %d errors: %s", 
                        successfulGroups, errors.size(), String.join("; ", errors)));
            }
            
        } catch (Exception e) {
            log.error("Error executing MHA NRIC download job", e);
            return new JobResult(false, "Error executing MHA NRIC download job: " + e.getMessage());
        }
    }
    
    /**
     * List MHA response files available in the SFTP directory
     * 
     * @return List of filenames
     */
    private List<String> listMhaResponseFiles() {
        log.info("Listing MHA response files from SFTP directory: {}", sftpDirectory);
        List<String> files = new ArrayList<>();
        
        try {
            // List files in the SFTP directory
            List<String> allFiles = sftpUtil.listFiles(SFTP_SERVER, sftpDirectory);
            
            // Filter for encrypted MHA response files
            for (String file : allFiles) {
                if (RESPONSE_FILE_PATTERN.matcher(file).find() || 
                     REPORT_TOT_PATTERN.matcher(file).find() || 
                     REPORT_EXP_PATTERN.matcher(file).find()) {
                    files.add(file);
                    log.debug("Found MHA response file: {}", file);
                }
            }
            
            log.info("Found {} MHA response files", files.size());
        } catch (Exception e) {
            log.error("Error listing MHA response files", e);
            throw new RuntimeException("Error listing MHA response files", e);
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
        // Extract timestamp from response file
        Matcher responseMatcher = RESPONSE_FILE_PATTERN.matcher(filename);
        if (responseMatcher.find()) {
            return responseMatcher.group(1);
        }
        
        // Extract timestamp from report files
        Matcher totMatcher = REPORT_TOT_PATTERN.matcher(filename);
        if (totMatcher.find()) {
            return totMatcher.group(1);
        }
        
        Matcher expMatcher = REPORT_EXP_PATTERN.matcher(filename);
        if (expMatcher.find()) {
            return expMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Process a group of related files (main response file and reports)
     * 
     * @param timestamp Timestamp of the file group
     * @param files List of files in the group
     * @return true if processing was successful
     */
    private boolean processFileGroup(String timestamp, List<String> files) {
        log.info("Processing file group with timestamp {}: {} files", timestamp, files.size());
        
        String responseFile = null;
        String totFile = null;
        String expFile = null;
        
        // Identify the files in the group
        for (String file : files) {
            if (RESPONSE_FILE_PATTERN.matcher(file).find()) {
                responseFile = file;
            } else if (REPORT_TOT_PATTERN.matcher(file).find()) {
                totFile = file;
            } else if (REPORT_EXP_PATTERN.matcher(file).find()) {
                expFile = file;
            }
        }
        
        // Check if we have all required files
        if (responseFile == null) {
            log.error("Missing main response file for timestamp {}", timestamp);
            return false;
        }
        
        if (totFile == null || expFile == null) {
            log.warn("Missing report file(s) for timestamp {}: TOT={}, EXP={}", 
                    timestamp, totFile != null, expFile != null);
            // Continue processing even if report files are missing
        }
        
        try {
            // Process files sequentially to avoid SLIFT token conflicts
            // First process the main response file
            log.info("Starting sequential file processing for timestamp {}", timestamp);
            
            // Step 1: Download and decrypt the main response file first
            byte[] responseContent = downloadAndDecryptFile(responseFile);
            if (responseContent == null) {
                log.error("Failed to download or decrypt main response file");
                return false;
            }
            log.info("Successfully processed main response file: {}", responseFile);
            
            // Step 2: Download and decrypt the TOT file if available
            byte[] totContent = null;
            if (totFile != null) {
                log.info("Processing TOT file next: {}", totFile);
                totContent = downloadAndDecryptFile(totFile);
                if (totContent != null) {
                    log.info("Successfully processed TOT file: {}", totFile);
                } else {
                    log.warn("Failed to process TOT file, but continuing with workflow");
                }
            }
            
            // Step 3: Download and decrypt the EXP file if available
            byte[] expContent = null;
            if (expFile != null) {
                log.info("Processing EXP file next: {}", expFile);
                expContent = downloadAndDecryptFile(expFile);
                if (expContent != null) {
                    log.info("Successfully processed EXP file: {}", expFile);
                } else {
                    log.warn("Failed to process EXP file, but continuing with workflow");
                }
            }
            
            // Process the files
            List<Map<String, Object>> records = null;
            Map<String, Integer> totals = null;
            List<Map<String, String>> exceptions = null;
                        
            // Process main response file
            records = mhaNricDownloadHelper.processResponseFile(responseContent);
            log.info("Processed {} records from main response file", records.size());

            // Track total records returned by MHA
            totalRecordsReturned = records != null ? records.size() : 0;
            recordJobMetadata("totalRecordsReturned", String.valueOf(totalRecordsReturned));

            // ============================================
            // PINK/RED: VALIDATION & DUPLICATE DETECTION
            // ============================================
            if (records != null && !records.isEmpty()) {
                try {
                    log.info("Starting validation workflow for {} records", records.size());
                    List<Map<String, Object>> validatedRecords = validateAndDeduplicateRecords(
                        records, responseFile, timestamp);
                    records = validatedRecords;
                    log.info("Validation complete: {} good records ready for processing", records.size());
                    recordJobMetadata("timestamp_" + timestamp + "_validated", String.valueOf(records.size()));

                } catch (Exception e) {
                    log.error("Validation workflow failed for timestamp {}, using unvalidated records", timestamp, e);
                    // Continue with original records if validation fails
                }
            }

            // Process control totals report
            if (totContent != null) {
                totals = mhaNricDownloadHelper.processControlTotalsReport(totContent);
                log.info("Processed control totals report with {} entries", totals.size());
                
                // Update metrics from control totals report
                recordsRead = totals.getOrDefault("TOTAL_RECORDS_READ", 0);
                recordsMatched = totals.getOrDefault("RECORDS_MATCHED", 0);
                invalidUinFinCount = totals.getOrDefault("INVALID_UIN_FIN", 0);
                validUnmatchedCount = totals.getOrDefault("VALID_UIN_FIN_UNMATCHED", 0);
                
                log.info("Control Report Metrics - Records Read: {}, Matched: {}, Invalid UIN/FIN: {}, Valid Unmatched: {}",
                        recordsRead, recordsMatched, invalidUinFinCount, validUnmatchedCount);
                        
                // Record metrics in job metadata
                recordJobMetadata("recordsRead", String.valueOf(recordsRead));
                recordJobMetadata("recordsMatched", String.valueOf(recordsMatched));
                recordJobMetadata("invalidUinFinCount", String.valueOf(invalidUinFinCount));
                recordJobMetadata("validUnmatchedCount", String.valueOf(validUnmatchedCount));
            }
            
            // Process exceptions report
            if (expContent != null) {
                exceptions = mhaNricDownloadHelper.processExceptionsReport(expContent);
                log.info("Processed exceptions report with {} entries", exceptions != null ? exceptions.size() : 0);
            }
            
            // Apply status updates
            int updatedCount = mhaNricDownloadHelper.applyStatusUpdates(records, exceptions != null ? exceptions : new ArrayList<>());
            log.info("Applied status updates to {} records", updatedCount);

            // Mark ocms_nro_temp records as processed (OCMS 20 enhancement)
            try {
                List<String> noticeNumbers = records.stream()
                    .map(r -> (String) r.get("uraReferenceNo"))
                    .filter(n -> n != null && !n.isEmpty())
                    .collect(Collectors.toList());

                if (!noticeNumbers.isEmpty()) {
                    int markedCount = mhaNricDownloadHelper.markNroTempRecordsAsProcessed(noticeNumbers);
                    log.info("[OCMS 20] Marked {} ocms_nro_temp records as processed", markedCount);
                    recordJobMetadata("timestamp_" + timestamp + "_nroTempMarked", String.valueOf(markedCount));
                }
            } catch (Exception e) {
                log.error("[OCMS 20] Error marking ocms_nro_temp records as processed: {}", e.getMessage(), e);
                // Continue processing - this is not critical
            }

            // Store UNC/HST results in temp table (OCMS 20 enhancement)
            try {
                int uncHstStored = mhaNricDownloadHelper.storeUncHstResults(records);
                if (uncHstStored > 0) {
                    log.info("[OCMS 20] Stored {} UNC/HST results in ocms_temp_unc_hst_addr", uncHstStored);
                    recordJobMetadata("timestamp_" + timestamp + "_uncHstStored", String.valueOf(uncHstStored));
                }
            } catch (Exception e) {
                log.error("[OCMS 20] Error storing UNC/HST results: {}", e.getMessage(), e);
                // Continue processing - this is not critical
            }

            // Process DataHive NRIC integration after status updates (BATCH VERSION)
            try {
                int dhProcessedCount = mhaNricDownloadHelper.processNRICDataHiveIntegrationBatch(records);
                log.info("Batch DataHive NRIC integration completed for timestamp {}. Processed {} records",
                        timestamp, dhProcessedCount);
                recordJobMetadata("timestamp_" + timestamp + "_dhProcessed", String.valueOf(dhProcessedCount));
            } catch (Exception e) {
                log.error("Error during batch DataHive NRIC integration for timestamp {}", timestamp, e);
                // Continue processing even if DataHive integration fails
            }
            
            recordJobMetadata("timestamp_" + timestamp + "_records", String.valueOf(records.size()));
            recordJobMetadata("timestamp_" + timestamp + "_exceptions", 
                    String.valueOf(exceptions != null ? exceptions.size() : 0));
            recordJobMetadata("timestamp_" + timestamp + "_updated", String.valueOf(updatedCount));
            
            return true;
        } catch (Exception e) {
            log.error("Error processing file group with timestamp {}", timestamp, e);
            return false;
        }
    }
    
    /**
     * Download and decrypt a file from SFTP
     * This method will use SLIFT encryption if enabled, or direct SFTP download if disabled
     * 
     * @param filename Filename to download
     * @return Decrypted file content or null if download/decryption failed
     */
    private byte[] downloadAndDecryptFile(String filename) {
        log.info("Processing file: {}", filename);
        
        try {
            // Check if we already have this file decrypted/downloaded
            if (decryptedFiles.containsKey(filename)) {
                log.info("Using cached content for file: {}", filename);
                return decryptedFiles.get(filename);
            }
            
            // Construct full SFTP path by combining directory and filename
            String fullSftpPath = sftpDirectory;
            if (!fullSftpPath.endsWith("/") && !filename.startsWith("/")) {
                fullSftpPath += "/";
            }
            fullSftpPath += filename;
            
            byte[] content;
            
            if (useEncryption) {
                // Use SLIFT encryption flow
                content = downloadAndDecryptWithSlift(filename, fullSftpPath);
            } else {
                // Use direct SFTP download without encryption
                content = downloadDirectlyFromSftp(filename, fullSftpPath);
            }
            
            if (content != null && content.length > 0) {
                decryptedFiles.put(filename, content);
                return content;
            } else {
                log.error("Downloaded file is empty: {}", filename);
                return null;
            }
        } catch (Exception e) {
            log.error("Error downloading file: {}", filename, e);
            return null;
        }
    }
    
    /**
     * Download and decrypt a file using SLIFT encryption
     * 
     * @param filename Filename to download
     * @param fullSftpPath Full SFTP path to the file
     * @return Decrypted file content or null if download/decryption failed
     */
    private byte[] downloadAndDecryptWithSlift(String filename, String fullSftpPath) {
        log.info("Downloading and decrypting file with SLIFT: {}", filename);
        
        try {
            // Generate a unique request ID for this download
            String requestId = "mha_download_" + System.currentTimeMillis() + "_" + filename.hashCode();
            
            log.info("Downloading file from SFTP path using SLIFT: {}", fullSftpPath);
            
            // Start the download and decryption process using explicit server name
            CompletableFuture<byte[]> future = sliftFileProcessingService.executeDownloadAndDecryptionFlowWithServerName(
                    requestId,
                    SFTP_SERVER,
                    fullSftpPath,
                    blobFolderPath,
                    SystemConstant.CryptOperation.SLIFT);
            
            // Wait for the result or timeout after 5 minutes
            byte[] content = future.get(5, TimeUnit.MINUTES);
            
            if (content != null && content.length > 0) {
                log.info("Successfully downloaded and decrypted file with SLIFT: {} ({} bytes)", filename, content.length);
                return content;
            } else {
                log.error("Downloaded file is empty: {}", filename);
                return null;
            }
        } catch (Exception e) {
            log.error("Error downloading and decrypting file with SLIFT: {}", filename, e);
            return null;
        }
    }
    
    /**
     * Download a file directly from SFTP without encryption
     * Also uploads the file to blob storage for consistency with the encrypted flow
     * 
     * @param filename Filename to download
     * @param fullSftpPath Full SFTP path to the file
     * @return File content or null if download failed
     */
    private byte[] downloadDirectlyFromSftp(String filename, String fullSftpPath) {
        log.info("Downloading file directly from SFTP without encryption: {}", filename);
        
        try {
            // Download the file directly from SFTP
            byte[] content = sftpUtil.downloadFile(SFTP_SERVER, fullSftpPath);
            
            if (content != null && content.length > 0) {
                log.info("Successfully downloaded file directly from SFTP: {} ({} bytes)", filename, content.length);
                
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
                
                return content;
            } else {
                log.error("Downloaded file is empty: {}", filename);
                return null;
            }
        } catch (Exception e) {
            log.error("Error downloading file directly from SFTP: {}", filename, e);
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

    /**
     * PINK/RED VALIDATION WORKFLOW
     * Validates records and handles duplicate UINs
     *
     * @param records Already-parsed records from response file
     * @param filename Source filename for logging
     * @param timestamp Timestamp for logging
     * @return List of validated records with TS-NRO flags added
     */
    private List<Map<String, Object>> validateAndDeduplicateRecords(
            List<Map<String, Object>> records, String filename, String timestamp) {

        log.info("PINK Step 3-9: Validating {} records from {}", records.size(), filename);

        List<MhaNricRecordValidator.ValidatedRecord> allValidated = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        // PINK Step 3-9: Validate each record
        for (Map<String, Object> record : records) {
            MhaNricRecordValidator.ValidatedRecord validated =
                new MhaNricRecordValidator.ValidatedRecord();
            validated.setRecord(record);
            validated.setFilename(filename);

            // Extract key fields (using camelCase field names from MhaFileFormatHelper)
            String uin = (String) record.get("uin");
            String uraRefNo = (String) record.get("uraReferenceNo");
            validated.setUin(uin);
            validated.setUraReferenceNo(uraRefNo);

            // PINK Step 4: Check primary fields (UIN & URA Reference No)
            if (!validator.hasPrimaryFields(record)) {
                validated.getValidationErrors().add("Missing primary fields (UIN or URA Reference No)");
                invalidCount++;
                continue; // Skip to next record
            }

            // PINK Step 5: Check missing critical fields
            if (validator.hasMissingCriticalFields(record)) {
                validated.getValidationErrors().add("Missing critical fields");
                validated.setFlagTsNro(true);
                validated.setTsNroReason("Missing critical fields");
            }

            // PINK Step 6: Check invalid format
            if (validator.hasInvalidFormat(record)) {
                validated.getValidationErrors().add("Invalid field format");
                validated.setFlagTsNro(true);
                validated.setTsNroReason("Invalid field format");
            }

            // PINK Step 7: Check critical field symbols
            if (validator.hasCriticalFieldSymbols(record)) {
                validated.getValidationErrors().add("Critical fields contain symbols");
                validated.setFlagTsNro(true);
                validated.setTsNroReason("Critical fields contain symbols");
            }

            // PINK Step 8: Check invalid address
            if (validator.hasInvalidAddress(record)) {
                validated.getValidationErrors().add("Invalid address");
                validated.setFlagTsNro(true);
                validated.setTsNroReason("Invalid address");
            }

            // PINK Step 9: Check inconsistent life status
            if (validator.hasInconsistentLifeStatus(record)) {
                validated.getValidationErrors().add("Inconsistent life status and date of death");
                validated.setFlagTsNro(true);
                validated.setTsNroReason("Inconsistent life status and date of death");
            }

            // Parse date for duplicate detection (using camelCase field name)
            try {
                String dateStr = (String) record.get("lastChangeAddressDate");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    validated.setDateAddressChange(parseDateForValidation(dateStr));
                }
            } catch (Exception e) {
                log.warn("Could not parse date for UIN {}: {}", uin, e.getMessage());
            }

            // Add TS-NRO flags to record if triggered
            if (validated.isFlagTsNro()) {
                record.put("TS_NRO_FLAG", "Y");
                record.put("TS_NRO_REASON", validated.getTsNroReason());
                log.info("Record UIN {} flagged for TS-NRO: {}", uin, validated.getTsNroReason());
            }

            allValidated.add(validated);
            if (validated.getValidationErrors().isEmpty()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        log.info("PINK validation complete: {} valid, {} invalid/flagged", validCount, invalidCount);

        // RED Step: Handle duplicate UINs
        log.info("RED Step: Checking for duplicate UINs");
        List<MhaNricRecordValidator.ValidatedRecord> deduplicated =
            validator.handleDuplicateUins(allValidated);

        int duplicatesRemoved = allValidated.size() - deduplicated.size();
        if (duplicatesRemoved > 0) {
            log.info("RED: Removed {} duplicate records (kept latest by Date Address Change)",
                duplicatesRemoved);
        }

        // Add TS-NRO flags to records if triggered
        log.info("Processing TS-NRO flags for {} records", deduplicated.size());
        for (MhaNricRecordValidator.ValidatedRecord validated : deduplicated) {
            if (validated.isFlagTsNro()) {
                Map<String, Object> record = validated.getRecord();
                String uin = (String) record.get("UIN");
                record.put("TS_NRO_FLAG", "Y");
                record.put("TS_NRO_REASON", validated.getTsNroReason());
                log.info("Record UIN {} flagged for TS-NRO: {}", uin, validated.getTsNroReason());
            }
        }

        // Extract final good records
        List<Map<String, Object>> goodRecords = deduplicated.stream()
            .map(MhaNricRecordValidator.ValidatedRecord::getRecord)
            .collect(Collectors.toList());

        log.info("Final result: {} good records for processing", goodRecords.size());
        recordJobMetadata("timestamp_" + timestamp + "_invalidRecords", String.valueOf(invalidCount));
        recordJobMetadata("timestamp_" + timestamp + "_duplicatesRemoved", String.valueOf(duplicatesRemoved));

        return goodRecords;
    }

    /**
     * Parse date string for validation workflow
     * Supports multiple formats: yyyyMMdd, yyyy-MM-dd, dd/MM/yyyy
     */
    private LocalDateTime parseDateForValidation(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        try {
            // Try yyyyMMdd format (MHA standard)
            if (dateStr.matches("\\d{8}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                    .atStartOfDay();
            }

            // Try yyyy-MM-dd format
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay();
            }

            // Try dd/MM/yyyy format
            if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    .atStartOfDay();
            }

            log.warn("Could not parse date format: {}", dateStr);
            return null;
        } catch (Exception e) {
            log.warn("Error parsing date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}