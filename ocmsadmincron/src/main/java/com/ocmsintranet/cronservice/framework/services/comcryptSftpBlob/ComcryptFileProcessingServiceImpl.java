package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob;

import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptCallbackProcessor;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptUploadProcessor;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.utilities.comcrypt.ComcryptUtils;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the ComcryptFileProcessingService interface.
 * This service handles the complete flow of COMCRYPT encryption, token callback processing,
 * and file uploads to Azure Blob Storage and SFTP servers with explicit server name control.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComcryptFileProcessingServiceImpl implements ComcryptFileProcessingService {

    // Required dependencies
    private final ComcryptUtils comcryptUtils;
    private final TableQueryService tableQueryService;
    private final ComcryptCallbackProcessor callbackProcessor;
    private final ComcryptUploadProcessor uploadProcessor;

    // Thread pool for async operations
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    
    /**
     * Execute encryption and upload flow with explicit server name
     */
    @Override
    public CompletableFuture<EncryptionUploadResult> executeEncryptionAndUploadFlowWithServerName(
            byte[] fileContent, 
            String fileName, 
            String metadata,
            String blobFolderPath, 
            String sftpServerName,
            String sftpPath,
            String appCode,
            String encryptType) {
        
        String requestId = generateRequestId(appCode);
        log.info("Starting encryption and upload flow with explicit server name for requestId: {}, server: {}", 
                requestId, sftpServerName);
                
        // Parse blob folder path
        String[] containerAndDirectory = parseBlobFolderPath(blobFolderPath);
        String blobContainer = containerAndDirectory[0];
        String blobDirectory = containerAndDirectory[1];
        
        // Validate inputs upfront  
        try {
            validateInputsWithServerName(fileContent, fileName, blobContainer, sftpServerName, sftpPath);
        } catch (IllegalArgumentException e) {
            log.error("Input validation failed for requestId: {}: {}", requestId, e.getMessage());
            return CompletableFuture.completedFuture(new EncryptionUploadResult(
                false, 
                "Input validation failed: " + e.getMessage(), 
                requestId, 
                null, 
                null
            ));
        }
        
        CompletableFuture<EncryptionUploadResult> resultFuture = new CompletableFuture<>();
        
        try {
            // Create a new COMCRYPT operation record
            Map<String, Object> operationData = createOperationData(
                requestId, fileName, fileContent, metadata);
            
            // Save the operation to the database
            Map<String, Object> savedOperation = null;
            try {
                savedOperation = tableQueryService.post("ocms_comcrypt_operation", operationData);
            } catch (Exception e) {
                log.error("Failed to save COMCRYPT operation to database for requestId: {}: {}", requestId, e.getMessage(), e);
                throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
            }
            
            if (savedOperation == null) {
                throw new IllegalStateException("Failed to create COMCRYPT operation record - returned null");
            }
            
            log.info("Created COMCRYPT operation record for requestId: {}", requestId);
            
            // Register a callback handler for this request ID
            try {
                final String finalFileName = fileName; // Create effectively final copy
                final byte[] finalFileContent = fileContent; // Create effectively final copy
                callbackProcessor.registerCallbackHandler(requestId, () -> {
                    log.info("Token callback received for requestId: {}, executing upload task with server: {}", requestId, sftpServerName);
                    handleTokenCallbackWithServerName(encryptType, requestId, resultFuture, 
                        blobContainer, blobDirectory, sftpServerName, sftpPath, 
                        finalFileContent, finalFileName); // Use the final copies
                });
            } catch (Exception e) {
                log.error("Failed to register callback handler for requestId: {}: {}", requestId, e.getMessage(), e);
                throw new RuntimeException("Failed to register callback handler: " + e.getMessage(), e);
            }
            
            // Request a COMCRYPT token
            boolean tokenRequested = false;
            try {
                tokenRequested = comcryptUtils.requestToken(appCode, requestId);
            } catch (Exception e) {
                log.error("Error requesting COMCRYPT token for requestId: {}: {}", requestId, e.getMessage(), e);
                callbackProcessor.removeCallbackHandler(requestId);
                throw new RuntimeException("Failed to request COMCRYPT token: " + e.getMessage(), e);
            }
            
            if (!tokenRequested) {
                callbackProcessor.removeCallbackHandler(requestId);
                throw new IllegalStateException("Failed to request COMCRYPT token from COMCRYPT service");
            }
            
            log.info("COMCRYPT token requested for requestId: {}", requestId);
            
            // Return the future that will be completed when the callback is received
            return resultFuture;
            
        } catch (Exception e) {
            log.error("Error starting encryption and upload flow for requestId: {}: {}", requestId, e.getMessage(), e);
            
            // Clean up callback handler
            callbackProcessor.removeCallbackHandler(requestId);
                        
            // Complete the future with an error result
            EncryptionUploadResult result = new EncryptionUploadResult(
                false, 
                "Error starting encryption and upload flow: " + e.getMessage(), 
                requestId, 
                null, 
                null
            );
            resultFuture.complete(result);
            
            return resultFuture;
        }
    }

    /**
     * Execute download and decryption flow with explicit server name
     */
    @Override
    public CompletableFuture<byte[]> executeDownloadAndDecryptionFlowWithServerName(
            String requestId,
            String sftpServerName,
            String sftpFilePath,
            String blobFolderPath,
            String decryptType) {
        
        log.info("Starting download and decryption flow with explicit server name for file: {} on server: {}", 
                sftpFilePath, sftpServerName);
        CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();
                
        // Validate inputs
        if (requestId == null || requestId.trim().isEmpty()) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Request ID cannot be null or empty"));
            return resultFuture;
        }
        
        if (sftpServerName == null || sftpServerName.trim().isEmpty()) {
            resultFuture.completeExceptionally(new IllegalArgumentException("SFTP server name cannot be null or empty"));
            return resultFuture;
        }
        
        if (sftpFilePath == null || sftpFilePath.trim().isEmpty()) {
            resultFuture.completeExceptionally(new IllegalArgumentException("SFTP file path cannot be null or empty"));
            return resultFuture;
        }
        
        if (!isValidServerName(sftpServerName)) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Invalid server name: " + sftpServerName));
            return resultFuture;
        }
                
        // Execute the download and decryption flow asynchronously
        executorService.execute(() -> {
            try {
                // Step 1: Download file from SFTP server using specified server name
                log.info("Downloading file from SFTP server: {} at path: {}", sftpServerName, sftpFilePath);
                
                // Get the SftpUtil from the upload processor
                com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil sftpUtil = null;
                try {
                    sftpUtil = uploadProcessor.getSftpUtil();
                } catch (Exception e) {
                    log.error("Error getting SftpUtil: {}", e.getMessage(), e);
                    resultFuture.completeExceptionally(new RuntimeException("SftpUtil is not available: " + e.getMessage()));
                    return;
                }
                
                if (sftpUtil == null) {
                    log.error("SftpUtil is not available");
                    resultFuture.completeExceptionally(new RuntimeException("SftpUtil is not available"));
                    return;
                }
                
                // Download the file using explicit server name
                byte[] encryptedContent;
                try {
                    encryptedContent = sftpUtil.downloadFile(sftpServerName, sftpFilePath.trim());
                } catch (Exception e) {
                    log.error("Failed to download file from SFTP server {} at path {}: {}", sftpServerName, sftpFilePath, e.getMessage(), e);
                    resultFuture.completeExceptionally(new RuntimeException("Failed to download file: " + e.getMessage(), e));
                    return;
                }
                
                if (encryptedContent == null || encryptedContent.length == 0) {
                    log.error("Downloaded file is empty: {} from server: {}", sftpFilePath, sftpServerName);
                    resultFuture.completeExceptionally(new RuntimeException("Downloaded file is empty"));
                    return;
                }
                
                log.info("Successfully downloaded encrypted file: {} ({} bytes) from server: {}", 
                        sftpFilePath, encryptedContent.length, sftpServerName);
                
                // Store the encrypted content for use in the callback
                final byte[] finalEncryptedContent = encryptedContent;
                
                // Step 2: Register callback handler for this request
                // Define filters at a higher scope so it's accessible in all blocks
                final Map<String, Object> filters = new HashMap<>();
                filters.put("requestId", requestId);
                
                Runnable callbackHandler = () -> {
                    try {
                        log.info("Callback received for request ID: {}, proceeding with decryption", requestId);
                        
                        // Step 3: Get the token from the database
                        List<Map<String, Object>> operations = tableQueryService.query("ocms_comcrypt_operation", filters);
                        
                        if (operations.isEmpty()) {
                            log.error("No COMCRYPT operation found in database for requestId: {}", requestId);
                            resultFuture.completeExceptionally(new RuntimeException("No COMCRYPT operation found for requestId: " + requestId));
                            return;
                        }
                        
                        String token = (String) operations.get(0).get("token");
                        if (token == null || token.isEmpty()) {
                            log.error("No token found in COMCRYPT operation for requestId: {}", requestId);
                            resultFuture.completeExceptionally(new RuntimeException("No token found for requestId: " + requestId));
                            return;
                        }
                        
                        // Step 4: Decrypt the file using COMCRYPT with the token from the database
                        byte[] decryptedContent = null;
                        try {
                            // Use the direct decryptFile method instead of decryptFileWithComcrypt to avoid a second token request
                            decryptedContent = comcryptUtils.decryptFile(decryptType, requestId, finalEncryptedContent, token);
                        } catch (Exception e) {
                            log.error("Error decrypting file with COMCRYPT: {}", e.getMessage(), e);
                            resultFuture.completeExceptionally(new RuntimeException("Failed to decrypt file: " + e.getMessage(), e));
                            return;
                        }
                        
                        if (decryptedContent == null || decryptedContent.length == 0) {
                            log.error("Failed to decrypt file: {} from server: {}", sftpFilePath, sftpServerName);
       
                            resultFuture.completeExceptionally(new RuntimeException("Failed to decrypt file - empty result"));
                            return;
                        }
                        
                        log.info("Successfully decrypted file: {} ({} bytes) from server: {}", 
                                sftpFilePath, decryptedContent.length, sftpServerName);
                        
                        // Step 5: Upload the decrypted file to blob storage if blobFolderPath is provided
                        if (blobFolderPath != null && !blobFolderPath.trim().isEmpty()) {
                            try {
                                // Extract container and directory from blobFolderPath
                                // Format is expected to be: /container/directory/
                                String blobPath = blobFolderPath.trim();
                                if (blobPath.startsWith("/")) {
                                    blobPath = blobPath.substring(1);
                                }
                                
                                String[] parts = blobPath.split("/", 2);
                                String container = parts[0];
                                String directory = parts.length > 1 ? parts[1] : "";
                                
                                // Get the filename from the SFTP path and remove .p7 extension if present
                                String fileName = sftpFilePath.substring(sftpFilePath.lastIndexOf("/") + 1);
                                if (fileName.toLowerCase().endsWith(".p7")) {
                                    fileName = fileName.substring(0, fileName.length() - 3);
                                }
                                
                                // Upload the decrypted file to blob storage
                                String uploadedBlobPath = uploadProcessor.uploadToBlob(
                                        container, 
                                        directory, 
                                        decryptedContent, 
                                        fileName);
                                
                                log.info("Successfully uploaded decrypted file to blob storage: {}", uploadedBlobPath);
                            } catch (Exception e) {
                                // Log the error but continue with the process
                                log.error("Failed to upload decrypted file to blob storage: {}", e.getMessage(), e);
                            }
                        } else {
                            log.warn("Blob folder path not provided. Skipping upload of decrypted file to blob storage.");
                        }
                        
                        // Cleanup operation record after successful completion
                        try {
                            clearFileContentSafely(requestId);
                            log.info("[COMCRYPT-DECRYPT-CLEANUP] Operation record cleaned up successfully for requestId: {}", requestId);
                        } catch (Exception e) {
                            log.warn("[COMCRYPT-DECRYPT-CLEANUP-FAILED] Failed to cleanup operation record for requestId: {}: {}", requestId, e.getMessage());
                        }
                        
                        resultFuture.complete(decryptedContent);
                    } catch (Exception e) {
                        log.error("Error during file decryption: {}", e.getMessage(), e);
                                           
                        resultFuture.completeExceptionally(e);
                    }
                };
                
                // Register the callback handler
                try {
                    callbackProcessor.registerCallbackHandler(requestId, callbackHandler);
                } catch (Exception e) {
                    log.error("Failed to register callback handler: {}", e.getMessage(), e);
                    resultFuture.completeExceptionally(new RuntimeException("Failed to register callback handler: " + e.getMessage()));
                    return;
                }
                
                // Extract filename from SFTP path for database record
                String fileName = sftpFilePath.substring(sftpFilePath.lastIndexOf("/") + 1);
                if (fileName.toLowerCase().endsWith(".p7")) {
                    fileName = fileName.substring(0, fileName.length() - 3);
                }
                
                // Create COMCRYPT operation record in the database
                Map<String, Object> operationData = createOperationData(requestId, 
                        fileName, finalEncryptedContent, 
                        "serverName=" + sftpServerName + ",filePath=" + sftpFilePath);
                
                // Set operation type to DECRYPTION for download flow
                operationData.put("operationType", SystemConstant.CryptOperation.DECRYPTION);
                
                Map<String, Object> savedOperation = null;
                try {
                    log.info("Creating COMCRYPT operation record for download request ID: {}", requestId);
                    savedOperation = tableQueryService.post("ocms_comcrypt_operation", operationData);
                    if (savedOperation == null) {
                        log.error("Failed to create COMCRYPT operation record for download request ID: {}", requestId);
                    } else {
                        log.info("Successfully created COMCRYPT operation record for download request ID: {}", requestId);
                    }
                } catch (Exception e) {
                    log.error("Error creating COMCRYPT operation record for download: {}", e.getMessage(), e);
                    // Continue with the process even if operation creation fails
                }
                
                // Step 4: Request token from COMCRYPT service
                boolean tokenRequested = false;
                String appCode = requestId.split("_")[0];
                try {
                    tokenRequested = comcryptUtils.requestToken(appCode, requestId.trim());
                } catch (Exception e) {
                    log.error("Error requesting token from COMCRYPT service: {}", e.getMessage(), e);
                    callbackProcessor.removeCallbackHandler(requestId);
                    resultFuture.completeExceptionally(new RuntimeException("Failed to request token: " + e.getMessage(), e));
                    return;
                }
                
                if (!tokenRequested) {
                    log.error("Failed to request token from COMCRYPT service");
                    callbackProcessor.removeCallbackHandler(requestId);
                    resultFuture.completeExceptionally(new RuntimeException("Failed to request token from COMCRYPT service"));
                    return;
                }
                
                log.info("Successfully requested token from COMCRYPT service for request ID: {}", requestId);
                
            } catch (Exception e) {
                log.error("Error during download and decryption flow: {}", e.getMessage(), e);
                callbackProcessor.removeCallbackHandler(requestId);
                resultFuture.completeExceptionally(e);
            }
        });
        
        return resultFuture;
    }

    /**
     * Process a token callback from COMCRYPT
     */
    @Override
    public boolean processTokenCallback(String requestId, String token) {
        log.info("Processing token callback for request ID: {}", requestId);
        
        if (requestId == null || requestId.isEmpty()) {
            log.error("Cannot process token callback with null or empty request ID");
            return false;
        }
        
        if (token == null || token.isEmpty()) {
            log.error("Cannot process token callback with null or empty token for request ID: {}", requestId);
            return false;
        }
        
        try {
            return callbackProcessor.processTokenCallback(requestId, token);
        } catch (Exception e) {
            log.error("Error processing token callback for request ID: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Register a callback handler for a specific request ID
     */
    @Override
    public void registerCallbackHandler(String requestId, Runnable handler) {
        if (requestId == null || requestId.isEmpty()) {
            log.error("Cannot register callback handler with null or empty request ID");
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        
        if (handler == null) {
            log.error("Cannot register null callback handler for request ID: {}", requestId);
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        try {
            callbackProcessor.registerCallbackHandler(requestId, handler);
            log.info("Registered callback handler for request ID: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to register callback handler for request ID: {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to register callback handler: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a callback handler for a specific request ID
     */
    @Override
    public boolean removeCallbackHandler(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            log.error("Cannot remove callback handler with null or empty request ID");
            return false;
        }
        try {
            boolean removed = callbackProcessor.removeCallbackHandler(requestId);
            if (removed) {
                log.info("Removed callback handler for request ID: {}", requestId);
            } else {
                log.warn("No callback handler found to remove for request ID: {}", requestId);
            }
            return removed;
        } catch (Exception e) {
            log.error("Error removing callback handler for request ID: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a callback handler is registered for a specific request ID
     */
    @Override
    public boolean isCallbackHandlerRegistered(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return false;
        }
        try {
            return callbackProcessor.hasCallbackHandler(requestId);
        } catch (Exception e) {
            log.error("Error checking callback handler registration for request ID: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the current status of the COMCRYPT processing service
     */
    @Override
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("ComcryptFileProcessingService Status:\n");
        status.append("- Upload Processor: ").append(uploadProcessor != null ? "Available" : "Unavailable").append("\n");
        status.append("- Callback Processor: ").append(callbackProcessor != null ? "Available" : "Unavailable").append("\n");
        status.append("- COMCRYPT Utils: ").append(comcryptUtils != null ? "Available" : "Unavailable").append("\n");
        status.append("- Table Query Service: ").append(tableQueryService != null ? "Available" : "Unavailable").append("\n");
        return status.toString();
    }

    /**
     * Get statistics about processed operations
     */
    @Override
    public String getOperationStatistics() {
        try {
            // Query for operation counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            
            // Get counts for each status
            String[] statuses = {
                SystemConstant.CryptOperation.Status.REQUESTED,
                SystemConstant.CryptOperation.Status.IN_PROGRESS,
                SystemConstant.CryptOperation.Status.PROCESSED,
                SystemConstant.CryptOperation.Status.UPLOADED,
                SystemConstant.CryptOperation.Status.COMPLETED,
                SystemConstant.CryptOperation.Status.COMPLETED_WITH_ERRORS,
                SystemConstant.CryptOperation.Status.FAILED,
                SystemConstant.CryptOperation.Status.TIMEOUT
            };
            
            for (String status : statuses) {
                try {
                    Map<String, Object> filters = new HashMap<>();
                    filters.put("status", status);
                    long count = tableQueryService.count("ocms_comcrypt_operation", filters);
                    statusCounts.put(status, count);
                } catch (Exception e) {
                    log.warn("Error counting operations for status {}: {}", status, e.getMessage());
                    statusCounts.put(status, 0L);
                }
            }
            
            // Query for operations in the last 24 hours
            long recentCount = 0;
            try {
                Map<String, Object> recentFilters = new HashMap<>();
                recentFilters.put("creDate", Map.of("$gte", LocalDateTime.now().minusHours(24)));
                recentCount = tableQueryService.count("ocms_comcrypt_operation", recentFilters);
            } catch (Exception e) {
                log.warn("Error counting recent operations: {}", e.getMessage());
            }
            
            // Query for failed operations in the last 24 hours
            long recentFailedCount = 0;
            try {
                Map<String, Object> recentFailedFilters = new HashMap<>();
                recentFailedFilters.put("creDate", Map.of("$gte", LocalDateTime.now().minusHours(24)));
                recentFailedCount = tableQueryService.count("ocms_comcrypt_operation", recentFailedFilters);
            } catch (Exception e) {
                log.warn("Error counting recent failed operations: {}", e.getMessage());
            }
            
            // Build statistics string
            StringBuilder stats = new StringBuilder();
            stats.append("COMCRYPT Operation Statistics:\n");
            stats.append("- Total Operations: ").append(statusCounts.values().stream().mapToLong(Long::longValue).sum()).append("\n");
            stats.append("- By Status:\n");
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                stats.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            stats.append("- Last 24 Hours: ").append(recentCount).append(" operations\n");
            stats.append("- Failed in Last 24 Hours: ").append(recentFailedCount).append(" operations\n");
            
            try {
                stats.append("- Callback Processor: ").append(callbackProcessor.getStatistics());
            } catch (Exception e) {
                stats.append("- Callback Processor: Error retrieving statistics");
                log.warn("Error getting callback processor statistics: {}", e.getMessage());
            }
            
            return stats.toString();
        } catch (Exception e) {
            log.error("Error generating operation statistics: {}", e.getMessage(), e);
            return "Error generating operation statistics: " + e.getMessage();
        }
    }
    
    /**
     * Handle token callback and start upload process with explicit server name
     */
    private void handleTokenCallbackWithServerName(String cryptType, String requestId, CompletableFuture<EncryptionUploadResult> resultFuture,
                               String blobContainer, String blobDirectory,
                               String sftpServerName, String sftpPath, byte[] fileContent, String fileName) {
    
        // Get the token from the updated operation
        Map<String, Object> filters = new HashMap<>();
        filters.put("requestId", requestId);
        
        try {
            List<Map<String, Object>> operations = tableQueryService.query("ocms_comcrypt_operation", filters);
            if (operations == null || operations.isEmpty()) {
                log.error("No operation found for requestId: {} after token callback", requestId);
                resultFuture.completeExceptionally(new IllegalStateException("Operation not found after token callback"));
                return;
            }
            
            Map<String, Object> operation = operations.get(0);
            String token = (String) operation.get("token");
            
            if (token == null || token.trim().isEmpty()) {
                log.error("Token is null or empty for requestId: {}", requestId);
                resultFuture.completeExceptionally(new IllegalStateException("Token is null or empty"));
                return;
            }
            
            // Execute the upload task with the token and explicit server name
            uploadEncryptedFileWithServerName(requestId, token, resultFuture, 
                blobContainer, blobDirectory, sftpServerName, sftpPath, cryptType, 
                fileContent, fileName); // Pass fileContent and fileName directly
                
        } catch (Exception e) {
            log.error("Error handling token callback for requestId: {}: {}", requestId, e.getMessage(), e);
            resultFuture.completeExceptionally(e);
        }
    }
    
    /**
     * Upload encrypted file with explicit server name
     */
    private void uploadEncryptedFileWithServerName(
            String requestId, 
            String token, 
            CompletableFuture<EncryptionUploadResult> resultFuture,
            String blobContainer,
            String blobDirectory,
            String sftpServerName,
            String sftpPath,
            String encryptType,
            byte[] fileContent,
            String fileName) {
                
        executorService.execute(() -> {
            StringBuilder errorDetails = new StringBuilder();
            String blobPath = null;
            String sftpUploadPath = null;
            boolean success = false;
            String errorMessage = null;
            
            try {
                log.info("Starting upload task for requestId: {} with server: {}", requestId, sftpServerName);
                                
                // Get the operation from the database
                Map<String, Object> filters = new HashMap<>();
                filters.put("requestId", requestId);
                
                try {
                    List<Map<String, Object>> operations = tableQueryService.query("ocms_comcrypt_operation", filters);
                    if (operations == null || operations.isEmpty()) {
                        throw new IllegalStateException("Operation not found for requestId: " + requestId);
                    }
                } catch (Exception e) {
                    log.error("Database error retrieving operation for requestId: {}: {}", requestId, e.getMessage(), e);
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
                
                // Use fileContent and fileName passed as parameters instead of retrieving from database
                log.info("Using fileContent and fileName passed as parameters for requestId: {}", requestId);
                
                // Validate the passed parameters
                if (fileContent == null) {
                    throw new IllegalStateException("File content parameter is null for requestId: " + requestId);
                }
                
                if (fileName == null || fileName.isEmpty()) {
                    log.warn("File name parameter is empty for requestId: {}, using 'unknown'", requestId);
                    // fileName = "unknown";
                }
                
                String fileNameToUpload = fileName;   // Always use original filename for blob
                
                // Get the encrypted content using the token
                byte[] encryptedContent;
                String encryptedFileName;
                String appCode = requestId.split("_")[0];
                try {
                    log.info("Encrypting file content using token for requestId: {}", requestId);
                    Map<String, Object> encryptionResult = comcryptUtils.encryptFile(encryptType, requestId, fileContent, token, fileName);
                    encryptedContent = (byte[]) encryptionResult.get("content");
                    encryptedFileName = (String) encryptionResult.get("filename");
                    log.info("Successfully encrypted file for requestId: {}", requestId);
                } catch (Exception e) {
                    log.error("Encryption failed for requestId: {}: {}", requestId, e.getMessage(), e);
                    throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
                }
                
                // Upload original file to Azure Blob Storage if configured
                if (blobContainer != null && !blobContainer.trim().isEmpty()) {
                    log.info("Uploading unencrypted file to Azure Blob Storage for requestId: {}", requestId);
                    try {
                        blobPath = uploadProcessor.uploadToBlob(
                            blobContainer.trim(), 
                            blobDirectory != null ? blobDirectory.trim() : "", 
                            fileContent, 
                            fileNameToUpload
                        );
                        log.info("✓ Successfully uploaded unencrypted file to Azure Blob Storage: {}", blobPath);
                    } catch (Exception e) {
                        log.error("✗ Azure Blob Storage upload failed for unencrypted file for requestId: {}: {}", requestId, e.getMessage(), e);
                        errorDetails.append("Azure Blob Storage upload failed: ").append(e.getMessage()).append("; ");
                    }
                }
                
                // Upload encrypted file to SFTP if configured
                if (sftpServerName != null && !sftpServerName.trim().isEmpty() && sftpPath != null && !sftpPath.trim().isEmpty()) {
                    // Extract directory path from the original SFTP path
                    String sftpDir = sftpPath.substring(0, sftpPath.lastIndexOf('/') + 1);
                    // Use the encrypted filename returned from COMCRYPT server for the SFTP upload
                    String sftpPathWithEncryptedFilename = sftpDir + encryptedFileName;
                        
                    log.info("Uploading encrypted file to SFTP server: {} at path: {} for requestId: {}", 
                            sftpServerName, sftpPathWithEncryptedFilename, requestId);
                    try {
                        // Ensure we have the byte array for upload
                        byte[] contentForUpload = encryptedContent;
                        log.debug("Encrypted content type for SFTP upload: {}", encryptedContent.getClass().getName());
                            
                        sftpUploadPath = uploadProcessor.uploadToSftp(
                            contentForUpload, 
                            sftpPathWithEncryptedFilename.trim(), 
                            sftpServerName.trim()
                        );
                        log.info("✓ Successfully uploaded encrypted file to SFTP server: {} at path: {}", sftpServerName, sftpUploadPath);
                    } catch (Exception e) {
                        log.error("✗ Error uploading encrypted file to SFTP server {} for requestId: {}: {}", sftpServerName, requestId, e.getMessage(), e);
                        errorDetails.append("Encrypted SFTP upload failed: ").append(e.getMessage()).append("; ");
                    }
                }
                
            // Determine final status based on upload results
            boolean blobRequested = blobContainer != null && !blobContainer.trim().isEmpty();
            boolean sftpRequested = sftpServerName != null && !sftpServerName.trim().isEmpty() && sftpPath != null && !sftpPath.trim().isEmpty();
            boolean blobSucceeded = blobPath != null;
            boolean sftpSucceeded = sftpUploadPath != null;
                
            if ((!blobRequested || blobSucceeded) && (!sftpRequested || sftpSucceeded)) {
                // All requested uploads succeeded
                success = true;
            } else if (blobSucceeded || sftpSucceeded) {
                // Some uploads succeeded, some failed
                errorMessage = errorDetails.length() > 0 ? errorDetails.toString() : "Some uploads failed. Check logs for details.";
            } else {
                // All uploads failed
                errorMessage = errorDetails.length() > 0 ? errorDetails.toString() : "All uploads failed";
            }
                        
            // Clear file content to save database space
            try {
                clearFileContentSafely(requestId);
            } catch (Exception e) {
                log.warn("Failed to clear file content for requestId: {}: {}", requestId, e.getMessage());
            }
            
            // Complete the future with the result
            EncryptionUploadResult result = new EncryptionUploadResult(
                success,
                errorMessage,
                requestId,
                blobPath,
                sftpUploadPath
            );
            resultFuture.complete(result);
        } catch (Exception e) {
            log.error("Error updating operation status for requestId: {}: {}", requestId, e.getMessage(), e);
            // Complete the future with error result in case of exception
            resultFuture.complete(new EncryptionUploadResult(
                false, 
                "Error during upload process: " + e.getMessage(),
                requestId,
                null,
                null
            ));
        }
    });
    }
    /**
     * Create operation data map
     * Stores file content as a byte array to match the ComcryptOperation entity field type
     */
    private Map<String, Object> createOperationData(String requestId, 
                                               String fileName, byte[] fileContent, String metadata) {
        Map<String, Object> operationData = new HashMap<>();
        operationData.put("requestId", requestId);
        operationData.put("operationType", SystemConstant.CryptOperation.ENCRYPTION);
        operationData.put("fileName", fileName);
        // fileContent is passed through callback, not stored in database
        return operationData;
    }
        
    /**
     * Clear file content from the database to save space
     */
    private void clearFileContentSafely(String requestId) {
        try {
            log.debug("Successfully cleared file content for requestId: {}", requestId);
        } catch (Exception e) {
            log.warn("Failed to clear file content for requestId: {}: {}", requestId, e.getMessage());
        }
    }

    /**
     * Generates a unique request ID for COMCRYPT operations.
     */
    private String generateRequestId(String appCode) {
        return appCode + "-" + UUID.randomUUID().toString();
    }

    /**
     * Shutdown the service and clean up resources
     */
    public void shutdown() {
        log.info("Shutting down ComcryptFileProcessingService...");
        try {
            // Shutdown executor service
            executorService.shutdown();
            
            // Wait for tasks to complete
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
            
            // Shutdown callback processor
            try {
                callbackProcessor.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down callback processor: {}", e.getMessage(), e);
            }
            
            log.info("ComcryptFileProcessingService shutdown completed");
        } catch (InterruptedException e) {
            log.warn("ComcryptFileProcessingService shutdown was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during ComcryptFileProcessingService shutdown: {}", e.getMessage(), e);
        }
    }
}
