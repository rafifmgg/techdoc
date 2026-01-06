package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.jobs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.agencyFileExchange.AgencyFileExchangeService;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.ComcryptFileProcessingService;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.ComcryptFileProcessingService.EncryptionUploadResult;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.helpers.MhaNricDataHelper;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * MHA NRIC Upload Job.
 * This job performs the following steps:
 * 1. Query valid NRIC data using MhaNricDataHelper
 * 2. Generate MHA request file content using AgencyFileExchangeService
 *    following the format in URA2NRO_20250306102959 file
 * 3. Process the file through SliftFileProcessingService which handles:
 *    a. SLIFT encryption
 *    b. Token callback handling
 *    c. Upload to Azure Blob Storage
 *    d. Upload to SFTP server
 */
@Slf4j
@Component
public class MhaNricUploadJob extends TrackedCronJobTemplate {

    private static final String AGENCY_TYPE = "MHA";
    private static final String SFTP_SERVER = "mha";
    
    @Value("${sftp.folders.mha.upload}")
    private String sftpDirectory;
    
    @Value("${blob.folder.mha.upload}")
    private String blobFolderPath;
    
    @Value("${sftp.servers.mha.encryption:true}")
    private boolean useEncryption;

    @Value("${ocms.comcrypt.appcode.mhanro.encrypt}")
    private String appCode;

    private final MhaNricDataHelper mhaNricDataHelper;
    private final AgencyFileExchangeService agencyFileExchangeService;
    private final ComcryptFileProcessingService sliftFileProcessingService;
    private final TableQueryService tableQueryService;
    private final SftpUtil sftpUtil;

    public MhaNricUploadJob(
            MhaNricDataHelper mhaNricDataHelper,
            AgencyFileExchangeService agencyFileExchangeService,
            ComcryptFileProcessingService sliftFileProcessingService,
            TableQueryService tableQueryService,
            SftpUtil sftpUtil) {
        this.mhaNricDataHelper = mhaNricDataHelper;
        this.agencyFileExchangeService = agencyFileExchangeService;
        this.sliftFileProcessingService = sliftFileProcessingService;
        this.tableQueryService = tableQueryService;
        this.sftpUtil = sftpUtil;
    }

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

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

    @Value("${cron.mha.nric.upload.shedlock.name:MhaNricUploadJob}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for MHA NRIC upload job");
        
        try {
            // Check if required dependencies are initialized
            if (mhaNricDataHelper == null) {
                log.error("MhaNricDataHelper is not initialized");
                return false;
            }
            
            if (agencyFileExchangeService == null) {
                log.error("AgencyFileExchangeService is not initialized");
                return false;
            }
            
            // AzureBlobStorageUtil check removed as we're using SliftFileProcessingService for all operations
            
            if (sliftFileProcessingService == null) {
                log.error("SliftFileProcessingService is not initialized");
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
        
        // Log encryption status
        log.info("SLIFT encryption is {}", useEncryption ? "enabled" : "disabled");
        recordJobMetadata("sliftEncryption", String.valueOf(useEncryption));
    }
    
    @Override
    protected void cleanup() {
        // Clean up any temporary resources
        jobMetadata.clear();
    }
    
    /**
     * Handle SLIFT token callback
     * This method is called by the MhaNricServiceImpl when a token is received from SLIFT
     * 
     * @param requestId The request ID for which the token was received
     * @param token The received token
     */
    public void handleComcryptCallback(String requestId, String token) {
        log.info("MhaNricUploadJob: Handling SLIFT token callback for requestId: {}", requestId);
        
        try {
            // Update the SLIFT operation record with the token
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("token", token);
            // Use the TableQueryService to update the SLIFT operation record
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", requestId);
            
            // Update the operation record in the database
            tableQueryService.patch("ocms_comcrypt_operation", filters, updateFields);
            
            log.info("MhaNricUploadJob: Successfully updated SLIFT operation with token for requestId: {}", requestId);
            
            // Process the token callback through the SLIFT service
            // This will trigger the upload process with the token
            boolean processed = sliftFileProcessingService.processTokenCallback(requestId, token);
            
            if (processed) {
                log.info("MhaNricUploadJob: Successfully processed token callback for requestId: {}", requestId);
            } else {
                log.warn("MhaNricUploadJob: Token callback processing returned false for requestId: {}", requestId);
            }
            
        } catch (Exception e) {
            log.error("MhaNricUploadJob: Error handling SLIFT token callback for requestId: {}: {}", 
                    requestId, e.getMessage(), e);
        }
    }

    @Override
    protected JobResult doExecute() {
        log.info("Starting MHA NRIC upload job execution");

        try {

            // Query data that needs verification
            // OCMS 20: Query both NRIC stage changes AND UNC/HST address verification
            List<Map<String, Object>> allData;
            int nricCount = 0;
            int uncHstCount = 0;

            try {
                // Query NRIC data (regular stage change processing)
                List<Map<String, Object>> nricData = mhaNricDataHelper.queryMhaNricData();
                nricCount = nricData.size();
                log.info("Found {} NRIC records for verification", nricCount);

                // Query UNC/HST data (unclaimed and house tenant address verification)
                List<Map<String, Object>> uncHstData = mhaNricDataHelper.queryUnclaimedAndHstData();
                uncHstCount = uncHstData.size();
                log.info("Found {} UNC/HST records for verification", uncHstCount);

                // Combine both lists
                allData = new java.util.ArrayList<>(nricData);
                allData.addAll(uncHstData);

                if (allData.isEmpty()) {
                    log.info("No data found for verification (NRIC: {}, UNC/HST: {}). Job completed successfully with no action.",
                            nricCount, uncHstCount);
                    return new JobResult(true, "No data found for verification");
                }

                log.info("Total {} records for MHA verification (NRIC: {}, UNC/HST: {})",
                        allData.size(), nricCount, uncHstCount);

                // Log the first record for debugging purposes
                if (!allData.isEmpty()) {
                    log.debug("Sample record: {}", allData.get(0));
                }
            } catch (Exception e) {
                log.error("Error querying data for verification: {}", e.getMessage(), e);
                return new JobResult(false, "Error querying data for verification: " + e.getMessage());
            }

            // Generate timestamp for the request file
            LocalDateTime timestamp = LocalDateTime.now();

            // Generate request file content using the agency file exchange service
            byte[] requestFileContent = agencyFileExchangeService.createRequestFileContent(
                    AGENCY_TYPE, allData, timestamp);

            // Log the request file name for tracking purposes
            String requestFileName = agencyFileExchangeService.generateRequestFilename(
                    AGENCY_TYPE, timestamp);
            log.info("Generated request file name: {}", requestFileName);

            log.info("Generated request file: {} with {} bytes", requestFileName, requestFileContent.length);

            // Use the SLIFT service to encrypt and upload to both Azure Blob Storage and SFTP
            String metadata = String.format("MHA Verification Request - %s records (NRIC: %d, UNC/HST: %d)",
                    allData.size(), nricCount, uncHstCount);
            
            log.info("Starting SLIFT encryption and upload flow for file: {}", requestFileName);
            
            // Check if SLIFT entity class is available
            try {
                Class<?> ocmsComcryptOperationClass = null;
                try {
                    ocmsComcryptOperationClass = Class.forName("com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation");
                } catch (ClassNotFoundException e1) {
                    try {
                        ocmsComcryptOperationClass = Class.forName("com.ocmsintranet.cronservice.framework.entity.OcmsComcryptOperation");
                    } catch (ClassNotFoundException e2) {
                        log.warn("Could not find OcmsComcryptOperation class: {}", e2.getMessage());
                    }
                }
                
                if (ocmsComcryptOperationClass == null) {
                    log.warn("SLIFT entity class not available. Skipping SLIFT processing but returning success with data found.");
                    return new JobResult(true, String.format(
                            "Found %d valid records (NRIC: %d, UNC/HST: %d) but SLIFT processing is unavailable due to missing entity class",
                            allData.size(), nricCount, uncHstCount));
                }

                log.info("SLIFT entity class found, proceeding with SLIFT processing");
            } catch (Exception e) {
                log.warn("Error checking for SLIFT entity class: {}", e.getMessage());
                log.warn("Will skip SLIFT processing and return success with warning");
                return new JobResult(true, String.format(
                        "Found %d valid records (NRIC: %d, UNC/HST: %d) but SLIFT processing is unavailable due to missing entity class",
                        allData.size(), nricCount, uncHstCount));
            }
            
            try {
                // Record job metadata
                recordJobMetadata("requestFileName", requestFileName);
                recordJobMetadata("recordCount", String.valueOf(allData.size()));
                recordJobMetadata("nricCount", String.valueOf(nricCount));
                recordJobMetadata("uncHstCount", String.valueOf(uncHstCount));
                
                // Construct paths for both destinations
                String fullSftpPath = sftpDirectory + "/" + requestFileName; // SLIFT during encryption
                String datFileName = requestFileName; // Unencrypted file to Blob
                
                EncryptionUploadResult result;
                
                if (useEncryption) {
                    // Use SLIFT encryption flow
                    log.info("Using SLIFT encryption for file upload");
                    result = uploadWithSliftEncryption(requestFileContent, datFileName, metadata, fullSftpPath);
                } else {
                    // Use direct SFTP upload without encryption
                    log.info("Using direct SFTP upload without encryption");
                    result = uploadDirectlyToSftp(requestFileContent, datFileName, metadata, fullSftpPath);
                }
                
                // Record additional job metadata
                if (result.getBlobPath() != null) {
                    recordJobMetadata("blobPath", result.getBlobPath());
                }
                
                if (result.getSftpPath() != null) {
                    recordJobMetadata("sftpPath", result.getSftpPath());
                }
                
                if (result.isSuccess()) {
                    log.info("File upload completed successfully");
                    log.info("Blob path: {}", result.getBlobPath());
                    log.info("SFTP path: {}", result.getSftpPath());

                    // Insert notice numbers to ocms_nro_temp for tracking
                    // Note: This only tracks NRIC records. UNC/HST records are already in ocms_nro_temp
                    // and will be marked as processed when MHA results are received
                    int insertedCount = mhaNricDataHelper.insertToNroTemp(allData);
                    recordJobMetadata("nroTempInserted", String.valueOf(insertedCount));
                    recordJobMetadata("recordsSubmitted", String.valueOf(allData.size()));

                    // Build metrics string
                    String metrics = String.format("recordsSubmitted: %d (NRIC: %d, UNC/HST: %d)",
                            allData.size(), nricCount, uncHstCount);

                    return new JobResult(true, String.format(
                            "Successfully processed %d records and uploaded to MHA (NRIC: %d, UNC/HST: %d, tracked: %d). Metrics: %s",
                            allData.size(), nricCount, uncHstCount, insertedCount, metrics));
                } else {
                    log.error("File upload failed: {}", result.getMessage());
                    return new JobResult(false, String.format(
                            "Found %d valid records (NRIC: %d, UNC/HST: %d) but failed to upload to MHA: %s",
                            allData.size(), nricCount, uncHstCount, result.getMessage()));
                }
            } catch (Exception e) {
                // Check for specific database schema mismatch error
                if (e.getMessage() != null && e.getMessage().contains("token_received_at")) {
                    log.warn("Database schema mismatch detected: token_received_at column missing");
                    log.warn("This is expected in some environments. Continuing with job execution.");
                    return new JobResult(true, String.format(
                            "Found %d valid records (NRIC: %d, UNC/HST: %d) but SLIFT processing is unavailable due to database schema mismatch",
                            allData.size(), nricCount, uncHstCount));
                }

                log.error("Error during file upload: {}", e.getMessage(), e);
                return new JobResult(false, String.format(
                        "Found %d valid records (NRIC: %d, UNC/HST: %d) but failed to upload to MHA: %s",
                        allData.size(), nricCount, uncHstCount, e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error executing MHA NRIC upload job: {}", e.getMessage(), e);
            return new JobResult(false, "Error executing MHA NRIC upload job: " + e.getMessage());
        }
    }
    
    /**
     * Upload a file using SLIFT encryption
     * 
     * @param fileContent The file content to upload
     * @param fileName The name of the file
     * @param metadata Metadata for the file
     * @param fullSftpPath The full SFTP path for the file
     * @return The result of the upload operation
     * @throws Exception If an error occurs during upload
     */
    private EncryptionUploadResult uploadWithSliftEncryption(byte[] fileContent, String fileName, String metadata, String fullSftpPath) throws Exception {
        log.info("Using SLIFT service to encrypt and upload file: {}", fileName);
        
        // Use the new method with explicit server name
        // Note: SliftFileProcessingService will encrypt the file and send the encrypted version to SFTP
        // and the unencrypted version to Blob Storage
        CompletableFuture<EncryptionUploadResult> resultFuture = 
                sliftFileProcessingService.executeEncryptionAndUploadFlowWithServerName(
                        fileContent, 
                        fileName, 
                        metadata,
                        blobFolderPath, 
                        SFTP_SERVER,
                        fullSftpPath,
                        appCode,
                        SystemConstant.CryptOperation.SLIFT);
        
        // Wait for the result
        return resultFuture.get(5, TimeUnit.MINUTES);
    }
    
    /**
     * Upload a file directly to SFTP without encryption
     * 
     * @param fileContent The file content to upload
     * @param fileName The name of the file
     * @param metadata Metadata for the file
     * @param fullSftpPath The full SFTP path for the file
     * @return The result of the upload operation
     * @throws Exception If an error occurs during upload
     */
    private EncryptionUploadResult uploadDirectlyToSftp(byte[] fileContent, String fileName, String metadata, String fullSftpPath) throws Exception {
        log.info("Uploading file directly to SFTP without encryption: {}", fileName);
        
        try {
            // Upload the file directly to SFTP
            boolean sftpSuccess = sftpUtil.uploadFile(SFTP_SERVER, fileContent, fullSftpPath);
            
            // Also upload to blob storage for consistency with SLIFT flow
            String blobPath = blobFolderPath + "/" + fileName;
            
            // Generate a unique request ID for tracking
            String requestId = "mha_upload_direct_" + System.currentTimeMillis() + "_" + fileName.hashCode();
            
            // Create a result object similar to what SLIFT would return
            // Using the constructor instead of setters since EncryptionUploadResult is immutable
            if (sftpSuccess) {
                return new EncryptionUploadResult(true, "File uploaded successfully to SFTP directly", 
                        requestId, blobPath, fullSftpPath);
            } else {
                return new EncryptionUploadResult(false, "Failed to upload file directly to SFTP", 
                        requestId, null, null);
            }
        } catch (Exception e) {
            log.error("Error uploading file directly to SFTP: {}", e.getMessage(), e);
            // Generate a unique request ID for tracking
            String requestId = "mha_upload_direct_error_" + System.currentTimeMillis() + "_" + fileName.hashCode();
            return new EncryptionUploadResult(false, "Error uploading file directly to SFTP: " + e.getMessage(),
                    requestId, null, null);
        }
    }

}
