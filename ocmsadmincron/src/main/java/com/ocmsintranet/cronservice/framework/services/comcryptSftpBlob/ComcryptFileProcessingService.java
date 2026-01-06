package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for processing files through COMCRYPT encryption and uploading to various destinations.
 * This service handles the complete workflow of:
 * 1. COMCRYPT encryption request
 * 2. Token callback handling
 * 3. File uploads to Azure Blob Storage
 * 4. File uploads to SFTP servers
 * 
 * Enhanced with explicit server name control and detailed result information.
 */
public interface ComcryptFileProcessingService {

    /**
     * Result class for encryption and upload operations with enhanced error information
     */
    class EncryptionUploadResult {
        private final boolean success;
        private final String message;
        private final String requestId;
        private final String blobPath;
        private final String sftpPath;
        private final String detailedErrorInfo;
        private final long processingTimeMs;
        
        public EncryptionUploadResult(boolean success, String message, String requestId, String blobPath, String sftpPath) {
            this(success, message, requestId, blobPath, sftpPath, null, 0L);
        }
        
        public EncryptionUploadResult(boolean success, String message, String requestId, String blobPath, String sftpPath, 
                                    String detailedErrorInfo, long processingTimeMs) {
            this.success = success;
            this.message = message;
            this.requestId = requestId;
            this.blobPath = blobPath;
            this.sftpPath = sftpPath;
            this.detailedErrorInfo = detailedErrorInfo;
            this.processingTimeMs = processingTimeMs;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRequestId() { return requestId; }
        public String getBlobPath() { return blobPath; }
        public String getSftpPath() { return sftpPath; }
        public String getDetailedErrorInfo() { return detailedErrorInfo; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        
        /**
         * Check if blob upload was requested and successful
         */
        public boolean isBlobUploadSuccess() {
            return blobPath != null && !blobPath.trim().isEmpty();
        }
        
        /**
         * Check if SFTP upload was requested and successful
         */
        public boolean isSftpUploadSuccess() {
            return sftpPath != null && !sftpPath.trim().isEmpty();
        }
        
        /**
         * Check if the operation completed with partial success (some uploads failed)
         */
        public boolean isPartialSuccess() {
            return !success && (isBlobUploadSuccess() || isSftpUploadSuccess());
        }
        
        /**
         * Get a summary of the upload results
         */
        public String getUploadSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Upload Summary: ");
            
            if (isBlobUploadSuccess()) {
                summary.append("Blob=SUCCESS");
            } else {
                summary.append("Blob=FAILED");
            }
            
            summary.append(", ");
            
            if (isSftpUploadSuccess()) {
                summary.append("SFTP=SUCCESS");
            } else {
                summary.append("SFTP=FAILED");
            }
            
            if (processingTimeMs > 0) {
                summary.append(", Time=").append(processingTimeMs).append("ms");
            }
            
            return summary.toString();
        }
        
        @Override
        public String toString() {
            return String.format(
                "EncryptionUploadResult{success=%s, requestId='%s', message='%s', blobPath='%s', sftpPath='%s', processingTime=%dms}",
                success, requestId, message, blobPath, sftpPath, processingTimeMs);
        }
    }
    
    /**
     * Exception class for COMCRYPT processing errors with enhanced context
     */
    class ComcryptProcessingException extends Exception {
        private final String requestId;
        private final String errorCode;
        
        public ComcryptProcessingException(String message, String requestId) {
            super(message);
            this.requestId = requestId;
            this.errorCode = "GENERAL_ERROR";
        }
        
        public ComcryptProcessingException(String message, String requestId, String errorCode, Throwable cause) {
            super(message, cause);
            this.requestId = requestId;
            this.errorCode = errorCode;
        }
        
        public String getRequestId() { return requestId; }
        public String getErrorCode() { return errorCode; }
        
        @Override
        public String toString() {
            return String.format("ComcryptProcessingException{requestId='%s', errorCode='%s', message='%s'}", 
                requestId, errorCode, getMessage());
        }
    }
    
    /**
     * Execute the complete file processing flow asynchronously with explicit server name and full control:
     * 1. Encrypt the file using COMCRYPT
     * 2. Wait for token callback (with timeout)
     * 3. Upload encrypted file to Azure Blob Storage using the full blob folder path
     * 4. Upload encrypted file to SFTP server using the specified server name and path
     *
     * @param fileContent     The content of the file to be encrypted and uploaded
     * @param fileName        The name of the file
     * @param metadata        Metadata for the COMCRYPT operation
     * @param blobFolderPath  Full blob folder path including container (e.g., "/offence/mha/input/") - null to skip blob upload
     * @param sftpServerName  SFTP server name to use (e.g., "mha", "lta", "toppan", "ces", "icms", "repccs") - null to skip SFTP upload
     * @param sftpPath        Full SFTP path for upload (e.g., "/nro/input/filename.dat") - null to skip SFTP upload
     * @return CompletableFuture with the result of the operation (never null)
     * @throws IllegalArgumentException if required parameters are invalid
     */
    CompletableFuture<EncryptionUploadResult> executeEncryptionAndUploadFlowWithServerName(
            byte[] fileContent, 
            String fileName, 
            String metadata,
            String blobFolderPath, 
            String sftpServerName,
            String sftpPath,
            String appCode,
            String encryptionType);

    /**
     * Execute the download and decryption flow asynchronously with explicit server name:
     * 1. Download file from SFTP server using specified server name
     * 2. Request COMCRYPT token
     * 3. Wait for token callback
     * 4. Decrypt the file using COMCRYPT
     *
     * @param requestId      The request ID for this operation
     * @param sftpServerName SFTP server name to use (e.g., "mha", "lta", "toppan", "ces", "icms", "repccs")
     * @param sftpFilePath   Full path to the file on the SFTP server
     * @param blobFolderPath Full blob folder path including container (e.g., "/offence/mha/output/") - optional for storage
     * @return CompletableFuture with the decrypted file content as byte array
     */
    CompletableFuture<byte[]> executeDownloadAndDecryptionFlowWithServerName(
            String requestId,
            String sftpServerName,
            String sftpFilePath,
            String blobFolderPath,
            String decryptType);

    /**
     * Process a token callback from the COMCRYPT service.
     * This method is called by the token receiver controller.
     *
     * @param requestId The request ID from the COMCRYPT callback (must not be null or empty)
     * @param token     The token from the COMCRYPT callback (must not be null or empty)
     * @return true if the callback was processed successfully, false otherwise
     */
    boolean processTokenCallback(String requestId, String token);
    
    /**
     * Register a callback handler for a specific request ID.
     * This is the preferred approach - register a handler that will be called
     * when the COMCRYPT token callback arrives for this specific request ID.
     *
     * @param requestId The request ID from the COMCRYPT operation (must not be null or empty)
     * @param handler   The callback handler to execute when token is received (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     */
    void registerCallbackHandler(String requestId, Runnable handler);
    
    /**
     * Remove a callback handler for a specific request ID
     *
     * @param requestId The request ID to remove the handler for
     * @return true if a handler was removed, false if no handler was registered
     */
    boolean removeCallbackHandler(String requestId);
    
    /**
     * Check if a callback handler is registered for a specific request ID
     *
     * @param requestId The request ID to check
     * @return true if a handler is registered, false otherwise
     */
    boolean isCallbackHandlerRegistered(String requestId);
    
    /**
     * Get the current status of the COMCRYPT processing service
     *
     * @return A string describing the current service status and configuration
     */
    default String getServiceStatus() {
        return "ComcryptFileProcessingService is running";
    }
    
    /**
     * Get statistics about processed operations
     *
     * @return A string containing operation statistics
     */
    default String getOperationStatistics() {
        return "Statistics not available";
    }

    /**
     * Validate input parameters for upload operations with explicit server name
     * 
     * @param fileContent The file content to upload
     * @param fileName The name of the file
     * @param blobContainer The blob container name (can be null to skip blob upload)
     * @param sftpServerName The SFTP server name to use
     * @param sftpPath The full SFTP path for upload (can be null to skip SFTP upload)
     * @throws IllegalArgumentException if required parameters are invalid
     */
    default void validateInputsWithServerName(byte[] fileContent, String fileName, 
                                             String blobContainer, String sftpServerName, String sftpPath) {
        if (fileContent == null || fileContent.length == 0) {
            throw new IllegalArgumentException("File content cannot be null or empty");
        }
        
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        
        // Validate file size (add reasonable limits)
        if (fileContent.length > 100 * 1024 * 1024) { // 100MB limit
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 100MB");
        }
        
        // At least one upload destination must be configured
        boolean hasBlobConfig = blobContainer != null && !blobContainer.trim().isEmpty();
        boolean hasSftpConfig = sftpServerName != null && !sftpServerName.trim().isEmpty() 
                               && sftpPath != null && !sftpPath.trim().isEmpty();
        
        if (!hasBlobConfig && !hasSftpConfig) {
            throw new IllegalArgumentException("At least one upload destination (Azure Blob or SFTP) must be configured");
        }
        
        // Validate SFTP server name is one of the configured servers
        if (hasSftpConfig) {
            String serverName = sftpServerName.trim().toLowerCase();
            if (!isValidServerName(serverName)) {
                throw new IllegalArgumentException("Invalid SFTP server name: " + serverName + 
                    ". Valid servers are: mha, lta, toppan, ces, icms, repccs, azureblob");
            }
        }
    }

    /**
     * Check if a server name is valid based on your SFTP configuration
     * 
     * @param serverName The server name to validate
     * @return true if the server name is valid, false otherwise
     */
    default boolean isValidServerName(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedName = serverName.trim().toLowerCase();
        return normalizedName.equals("mha") || 
               normalizedName.equals("lta") || 
               normalizedName.equals("toppan") || 
               normalizedName.equals("ces") || 
               normalizedName.equals("icms") || 
               normalizedName.equals("repccs") || 
               normalizedName.equals("azureblob");
    }

    /**
     * Parse a full blob folder path into container and directory components
     * 
     * @param blobFolderPath Full blob folder path (e.g., "/offence/mha/input/")
     * @return String[] with container at index 0 and directory at index 1
     */
    default String[] parseBlobFolderPath(String blobFolderPath) {
        if (blobFolderPath == null || blobFolderPath.trim().isEmpty()) {
            return new String[] { "", "" };
        }
        
        try {
            // Remove leading and trailing slashes and trim
            String normalizedPath = blobFolderPath.trim();
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            
            if (normalizedPath.isEmpty()) {
                return new String[] { "", "" };
            }
            
            // Split by first slash
            int firstSlash = normalizedPath.indexOf('/');
            if (firstSlash == -1) {
                // No slash found, treat the whole string as container
                return new String[] { normalizedPath, "" };
            }
            
            String container = normalizedPath.substring(0, firstSlash);
            String directory = normalizedPath.substring(firstSlash + 1);
            
            return new String[] { container.trim(), directory.trim() };
        } catch (Exception e) {
            return new String[] { "", "" };
        }
    }

    /**
     * Get enhanced service configuration information
     * 
     * @return A string containing detailed service configuration
     */
    default String getServiceConfiguration() {
        StringBuilder config = new StringBuilder();
        config.append("ComcryptFileProcessingService Configuration:\n");
        config.append("- Service Status: ").append(getServiceStatus()).append("\n");
        config.append("- Supported SFTP Servers: mha, lta, toppan, ces, icms, repccs, azureblob\n");
        config.append("- Max File Size: 100MB\n");
        config.append("- Supported Operations: encryption, decryption, upload, download\n");
        config.append("- Callback Support: enabled\n");
        return config.toString();
    }

    /**
     * Create a standardized SFTP path from base folder and filename
     * 
     * @param baseFolder The base SFTP folder path (e.g., "/nro/input")
     * @param fileName The filename to append
     * @return The complete SFTP path
     */
    default String createSftpPath(String baseFolder, String fileName) {
        if (baseFolder == null || fileName == null) {
            throw new IllegalArgumentException("Base folder and filename cannot be null");
        }
        
        String normalizedFolder = baseFolder.trim();
        if (!normalizedFolder.endsWith("/")) {
            normalizedFolder += "/";
        }
        
        return normalizedFolder + fileName.trim();
    }

    /**
     * Get default server name - removed workflow dependency
     * 
     * @return Default server name
     */
    default String getDefaultServerName() {
        return "mha";
    }
}