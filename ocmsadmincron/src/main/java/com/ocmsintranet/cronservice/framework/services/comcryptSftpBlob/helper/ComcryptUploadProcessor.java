package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper;

import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.comcrypt.ComcryptUtils;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Helper class for COMCRYPT upload processing operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptUploadProcessor {

    private final ComcryptOperationHelper operationHelper;
    private final ComcryptCallbackProcessor callbackProcessor;
    private final ComcryptUtils comcryptUtils;
    private final SftpUtil sftpUtil;
    private final AzureBlobStorageUtil blobStorageUtil;

    /**
     * Upload encrypted file to both SFTP and Blob storage
     * 
     * @param requestId The request ID for the operation
     * @param encryptedContent The encrypted file content
     * @param originalContent The original file content
     * @param fileName The original file name
     * @param encryptedFileName The encrypted file name
     * @param blobContainer The blob container name
     * @param blobDirectory The blob directory path
     * @param sftpPath The SFTP path
     * @param sftpServerName The SFTP server name
     * @return True if upload was successful, false otherwise
     */
    public boolean uploadEncryptedFile(
            String requestId,
            byte[] encryptedContent,
            byte[] originalContent,
            String fileName,
            String encryptedFileName,
            String blobContainer,
            String blobDirectory,
            String sftpPath,
            String sftpServerName) {
        
        log.info("Starting upload of encrypted file for requestId: {}", requestId);
        boolean success = true;
        String errorMessage = null;
        
        try {
            // Upload original file to Azure Blob Storage
            String blobPath = null;
            if (blobContainer != null && !blobContainer.isEmpty()) {
                try {
                    log.info("Uploading original file to Azure Blob Storage for requestId: {}", requestId);
                    blobPath = uploadToBlob(
                        blobContainer.trim(), 
                        blobDirectory != null ? blobDirectory.trim() : "", 
                        originalContent, 
                        fileName
                    );
                    log.info("Successfully uploaded to Azure Blob Storage: {}", blobPath);
                } catch (Exception e) {
                    log.error("Azure Blob Storage upload failed for requestId: {}: {}", requestId, e.getMessage(), e);
                    success = false;
                    errorMessage = "Azure Blob Storage upload failed: " + e.getMessage();
                }
            }
            
            // Upload encrypted file to SFTP
            String sftpUploadPath = null;
            if (sftpServerName != null && !sftpServerName.isEmpty() && sftpPath != null && !sftpPath.isEmpty()) {
                try {
                    log.info("Uploading encrypted file to SFTP server: {} at path: {} for requestId: {}", 
                            sftpServerName, sftpPath, requestId);
                    
                    sftpUploadPath = uploadToSftp(
                        encryptedContent, 
                        sftpPath.trim(), 
                        sftpServerName.trim()
                    );
                    log.info("Successfully uploaded to SFTP server: {} at path: {}", sftpServerName, sftpUploadPath);
                } catch (Exception e) {
                    log.error("SFTP upload failed for requestId: {}: {}", requestId, e.getMessage(), e);
                    success = false;
                    errorMessage = (errorMessage != null ? errorMessage + "; " : "") + "SFTP upload failed: " + e.getMessage();
                }
            }
            
            // Update operation status based on upload results
            String finalStatus = success ? SystemConstant.CryptOperation.Status.UPLOADED : SystemConstant.CryptOperation.Status.FAILED;
            operationHelper.updateOperationStatusSafely(requestId, finalStatus, errorMessage);
            
            // Clear file content from database if upload was successful
            if (success) {
                operationHelper.clearFileContentSafely(requestId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Error during file upload for requestId: {}: {}", requestId, e.getMessage(), e);
            operationHelper.updateOperationStatusSafely(requestId, SystemConstant.CryptOperation.Status.FAILED, e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload file to Azure Blob Storage
     * 
     * @param container The blob container name
     * @param directory The blob directory path
     * @param content The file content to upload
     * @param fileName The file name
     * @return The blob path
     * @throws IOException If upload fails
     */
    public String uploadToBlob(String container, String directory, byte[] content, String fileName) throws IOException {
        log.debug("Uploading to blob storage - Container: {}, Directory: {}, File: {}, Size: {} bytes", 
                container, directory, fileName, content.length);
        
        try {
            // Check if content might be Base64 encoded and decode if necessary
            byte[] actualContent = checkAndDecodeBase64Content(content);
            
            log.debug("Content size after potential Base64 decoding: {} bytes", actualContent.length);
            
            // Construct full blob path from container and directory
            String fullBlobPath = container + "/" + (directory.isEmpty() ? "" : directory + "/") + fileName;
            // Use uploadBytesToBlob method instead of uploadToBlob
            String blobPath = blobStorageUtil.uploadBytesToBlob(actualContent, fullBlobPath).getFilePath();
            log.debug("Successfully uploaded to blob storage: {}", blobPath);
            return blobPath;
        } catch (Exception e) {
            log.error("Error uploading to blob storage: {}", e.getMessage(), e);
            throw new IOException("Failed to upload to Azure Blob Storage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if content is likely Base64 encoded and decode if necessary
     * 
     * @param content The content to check and potentially decode
     * @return The original content or decoded content if it was Base64 encoded
     */
    private byte[] checkAndDecodeBase64Content(byte[] content) {
        if (content == null || content.length == 0) {
            return content;
        }
        
        // Convert to string to check if it looks like Base64
        String contentAsString = new String(content, java.nio.charset.StandardCharsets.UTF_8);
        
        // Check if the string looks like Base64 (contains only valid Base64 characters)
        // Base64 strings typically only contain A-Z, a-z, 0-9, +, /, and = (for padding)
        if (contentAsString.matches("^[A-Za-z0-9+/=]+$")) {
            try {
                // Try to decode as Base64
                byte[] decodedContent = java.util.Base64.getDecoder().decode(contentAsString);
                
                // If decoding was successful and produced different content, use the decoded version
                if (decodedContent.length > 0 && decodedContent.length != content.length) {
                    log.info("Detected and decoded Base64 content. Original size: {} bytes, Decoded size: {} bytes", 
                            content.length, decodedContent.length);
                    return decodedContent;
                }
            } catch (IllegalArgumentException e) {
                // Not valid Base64, continue with original content
                log.debug("Content matched Base64 pattern but could not be decoded: {}", e.getMessage());
            }
        }
        
        // Return original content if not Base64 or decoding failed
        return content;
    }
    
    /**
     * Upload file to SFTP server
     * 
     * @param content The file content to upload
     * @param path The SFTP path
     * @param serverName The SFTP server name
     * @return The SFTP path
     * @throws IOException If upload fails
     */
    public String uploadToSftp(byte[] content, String path, String serverName) throws IOException {
        log.debug("Uploading to SFTP server: {} at path: {}, Size: {} bytes", serverName, path, content.length);
        
        try {
            // Use uploadFile method with correct parameter order: serverName, fileContent (byte[]), remoteFilePath
            sftpUtil.uploadFile(serverName, content, path);
            log.debug("Successfully uploaded to SFTP server: {} at path: {}", serverName, path);
            return path;
        } catch (Exception e) {
            log.error("Error uploading to SFTP server {}: {}", serverName, e.getMessage(), e);
            throw new IOException("Failed to upload to SFTP server: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the SFTP utility
     * 
     * @return The SFTP utility
     */
    public SftpUtil getSftpUtil() {
        return sftpUtil;
    }
}