package com.ocmsintranet.cronservice.utilities.ces;

import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reusable utility for uploading files to both Azure Blob Storage and SFTP
 * Used by multiple REPCCS processes (CASREP, NOTICEEP, REP_ARC, etc.)
 */
@Component
public class FileUploadUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadUtil.class);
    
    @Autowired
    private AzureBlobStorageUtil azureBlobStorageUtil;
    
    @Autowired
    private SftpUtil sftpUtil;

    /**
     * Upload file to Azure Blob Storage only
     * 
     * @param fileContent byte array of file content
     * @param fileName name of the file
     * @param blobPath path for blob storage (e.g., "/REPCCS/IN")
     * @param processName name of the process for logging
     * @return BlobUploadResult with upload result
     */
    public BlobUploadResult uploadToBlob(byte[] fileContent, String fileName, String blobPath, String processName) {
        logger.info("Starting blob upload for {} process: {}", processName, fileName);
        
        BlobUploadResult result = new BlobUploadResult();
        result.setFileName(fileName);
        result.setProcessName(processName);
        
        try {
            String fullBlobPath = blobPath.endsWith("/") ? blobPath + fileName : blobPath + "/" + fileName;
            logger.info("Uploading {} file to Azure Blob Storage: {}", processName, fullBlobPath);
            
            AzureBlobStorageUtil.FileUploadResponse blobUploadResponse = azureBlobStorageUtil.uploadBytesToBlob(
                    fileContent,
                    fullBlobPath
            );
            
            if (!blobUploadResponse.isSuccess()) {
                String errorMsg = "Failed to upload " + processName + " file to blob storage: " + blobUploadResponse.getErrorMessage();
                logger.error(errorMsg);
                result.setSuccess(false);
                result.setError(errorMsg);
                return result;
            }
            
            result.setSuccess(true);
            result.setBlobPath(blobUploadResponse.getFilePath());
            logger.info("Successfully uploaded {} file to blob: {}", processName, blobUploadResponse.getFilePath());
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "Unexpected error during blob upload for " + processName + ": " + e.getMessage();
            logger.error(errorMsg, e);
            result.setSuccess(false);
            result.setError(errorMsg);
            return result;
        }
    }
    
    /**
     * Upload file to SFTP only
     * 
     * @param fileContent byte array of file content
     * @param fileName name of the file
     * @param sftpPath path for SFTP (e.g., "/IN")
     * @param processName name of the process for logging
     * @return SftpUploadResult with upload result
     */
    public SftpUploadResult uploadToSftp(byte[] fileContent, String fileName, String sftpPath, String processName) {
        logger.info("Starting SFTP upload for {} process: {}", processName, fileName);
        
        SftpUploadResult result = new SftpUploadResult();
        result.setFileName(fileName);
        result.setProcessName(processName);
        String sftpServerName = "repccs";
        
        try {
            String fullSftpPath = sftpPath.endsWith("/") ? sftpPath + fileName : sftpPath + "/" + fileName;
            logger.info("Uploading {} file to SFTP server '{}' at path: {}", processName, sftpServerName, fullSftpPath);
            
            boolean sftpUploadSuccess = sftpUtil.uploadFile(sftpServerName, fileContent, fullSftpPath);
            
            if (!sftpUploadSuccess) {
                String errorMsg = "Failed to upload " + processName + " file to SFTP server: " + fullSftpPath;
                logger.error(errorMsg);
                result.setSuccess(false);
                result.setError(errorMsg);
                return result;
            }
            
            result.setSuccess(true);
            result.setSftpPath(fullSftpPath);
            logger.info("Successfully uploaded {} file to SFTP: {}", processName, fullSftpPath);
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "Unexpected error during SFTP upload for " + processName + ": " + e.getMessage();
            logger.error(errorMsg, e);
            result.setSuccess(false);
            result.setError(errorMsg);
            return result;
        }
    }
    
    /**
     * Upload file to both Azure Blob Storage and SFTP (combined method)
     * 
     * @param fileContent byte array of file content
     * @param fileName name of the file
     * @param blobPath path for blob storage (e.g., "/REPCCS/IN")
     * @param sftpPath path for SFTP (e.g., "/IN")
     * @param processName name of the process for logging (e.g., "CASREP", "NOTICEEP")
     * @return FileUploadResult with upload results
     */
    public FileUploadResult uploadFile(byte[] fileContent, String fileName, String blobPath, String sftpPath, String processName) {
        logger.info("Starting combined file upload for {} process: {}", processName, fileName);
        
        FileUploadResult result = new FileUploadResult();
        result.setFileName(fileName);
        result.setProcessName(processName);
        
        // Step 1: Upload to blob
        BlobUploadResult blobResult = uploadToBlob(fileContent, fileName, blobPath, processName);
        result.setBlobSuccess(blobResult.isSuccess());
        result.setBlobPath(blobResult.getBlobPath());
        result.setBlobError(blobResult.getError());
        
        if (!blobResult.isSuccess()) {
            result.setOverallSuccess(false);
            return result;
        }
        
        // Step 2: Upload to SFTP
        SftpUploadResult sftpResult = uploadToSftp(fileContent, fileName, sftpPath, processName);
        result.setSftpSuccess(sftpResult.isSuccess());
        result.setSftpPath(sftpResult.getSftpPath());
        result.setSftpError(sftpResult.getError());
        
        if (!sftpResult.isSuccess()) {
            result.setOverallSuccess(false);
            return result;
        }
        
        // Both uploads successful
        result.setOverallSuccess(true);
        logger.info("Combined file upload completed successfully for {} process: {}", processName, fileName);
        
        return result;
    }
    
    /**
     * Upload file with default REPCCS paths
     * Convenience method for REPCCS processes
     * 
     * @param fileContent byte array of file content
     * @param fileName name of the file
     * @param processName name of the process for logging
     * @return FileUploadResult with upload results
     */
    public FileUploadResult uploadRepccsFile(byte[] fileContent, String fileName, String processName) {
        return uploadFile(fileContent, fileName, "/test", "/IN", processName);
    }
    
    /**
     * Result class for blob upload operations
     */
    public static class BlobUploadResult {
        private String fileName;
        private String processName;
        private boolean success;
        private String blobPath;
        private String error;
        
        // Constructors
        public BlobUploadResult() {}
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getBlobPath() { return blobPath; }
        public void setBlobPath(String blobPath) { this.blobPath = blobPath; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * Result class for SFTP upload operations
     */
    public static class SftpUploadResult {
        private String fileName;
        private String processName;
        private boolean success;
        private String sftpPath;
        private String error;
        
        // Constructors
        public SftpUploadResult() {}
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getSftpPath() { return sftpPath; }
        public void setSftpPath(String sftpPath) { this.sftpPath = sftpPath; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * Result class for combined file upload operations
     */
    public static class FileUploadResult {
        private String fileName;
        private String processName;
        private boolean overallSuccess;
        
        // Blob storage results
        private boolean blobSuccess;
        private String blobPath;
        private String blobError;
        
        // SFTP results
        private boolean sftpSuccess;
        private String sftpPath;
        private String sftpError;
        
        // Constructors
        public FileUploadResult() {}
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        
        public boolean isOverallSuccess() { return overallSuccess; }
        public void setOverallSuccess(boolean overallSuccess) { this.overallSuccess = overallSuccess; }
        
        public boolean isBlobSuccess() { return blobSuccess; }
        public void setBlobSuccess(boolean blobSuccess) { this.blobSuccess = blobSuccess; }
        
        public String getBlobPath() { return blobPath; }
        public void setBlobPath(String blobPath) { this.blobPath = blobPath; }
        
        public String getBlobError() { return blobError; }
        public void setBlobError(String blobError) { this.blobError = blobError; }
        
        public boolean isSftpSuccess() { return sftpSuccess; }
        public void setSftpSuccess(boolean sftpSuccess) { this.sftpSuccess = sftpSuccess; }
        
        public String getSftpPath() { return sftpPath; }
        public void setSftpPath(String sftpPath) { this.sftpPath = sftpPath; }
        
        public String getSftpError() { return sftpError; }
        public void setSftpError(String sftpError) { this.sftpError = sftpError; }
        
        /**
         * Get formatted success message for logging
         */
        public String getSuccessMessage() {
            if (!overallSuccess) {
                return "File upload failed";
            }
            return String.format("Successfully uploaded %s file. Blob: %s, SFTP: %s", 
                    processName, blobPath, sftpPath);
        }
        
        /**
         * Get formatted error message for logging
         */
        public String getErrorMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("File upload failed for ").append(processName).append(": ");
            
            if (!blobSuccess && blobError != null) {
                sb.append("Blob error: ").append(blobError).append("; ");
            }
            
            if (!sftpSuccess && sftpError != null) {
                sb.append("SFTP error: ").append(sftpError);
            }
            
            return sb.toString();
        }
    }
}
