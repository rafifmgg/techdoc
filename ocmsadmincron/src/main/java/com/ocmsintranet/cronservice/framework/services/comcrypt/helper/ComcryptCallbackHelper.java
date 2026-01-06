package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptUploadProcessor;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers.LtaDownloadHelper;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.comcrypt.ComcryptUtils;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for managing COMCRYPT token callbacks.
 *
 * Updated to handle fire-and-forget flow:
 * - When callback is received and no handler is registered
 * - Downloads file from blob using path: {blobFolder}/{fileName}
 * - Encrypts file with received token
 * - Uploads encrypted file to SFTP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptCallbackHelper {

    private final ComcryptOperationHelper operationHelper;
    private final ComcryptWorkflowConfigResolver workflowConfigResolver;
    private final AzureBlobStorageUtil blobStorageUtil;
    private final ComcryptUtils comcryptUtils;
    private final ComcryptUploadProcessor uploadProcessor;
    private final SftpUtil sftpUtil;
    private final LtaDownloadHelper ltaDownloadHelper;
    private final ComcryptBatchJobUpdateHelper batchJobUpdateHelper;

    // Registry of callback handlers by request ID
    private final Map<String, Runnable> callbackHandlers = new ConcurrentHashMap<>();

    /**
     * Register a callback handler for a specific request ID
     */
    public void registerCallbackHandler(String requestId, Runnable handler) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        callbackHandlers.put(requestId.trim(), handler);
        log.info("Registered callback handler for requestId: {}", requestId);
    }

    /**
     * Remove a callback handler for a specific request ID
     */
    public boolean removeCallbackHandler(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return false;
        }
        
        Runnable removed = callbackHandlers.remove(requestId.trim());
        if (removed != null) {
            log.info("Removed callback handler for requestId: {}", requestId);
            return true;
        }
        return false;
    }

    /**
     * Process a token callback for a specific request
     * Updated to use requestId-based routing without workflow type dependencies.
     */
    @Transactional
    public boolean processTokenCallback(String requestId, String token) {
        log.info("[COMCRYPT-CALLBACK-PROCESS] Processing token callback for requestId: {}", requestId);
        
        if (!isValidCallbackInput(requestId, token)) {
            log.error("[COMCRYPT-CALLBACK-ERROR] Invalid token callback - requestId or token is null/empty");
            return false;
        }
        
        OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
        if (operation == null) {
            log.error("[COMCRYPT-CALLBACK-ERROR] No COMCRYPT operation found for requestId: {}", requestId);
            return false;
        }
        
        // Update operation with token
        operation.setToken(token.trim());
        operationHelper.saveOperation(operation);
        log.info("[COMCRYPT-CALLBACK-TOKEN-STORED] Token stored for requestId: {}", requestId);
        
        // Check if there's a registered callback handler for this request ID
        Runnable handler = callbackHandlers.get(requestId);
        if (handler != null) {
            log.info("[COMCRYPT-CALLBACK-HANDLER] Executing registered callback handler for requestId: {}", requestId);
            try {
                handler.run();
                callbackHandlers.remove(requestId); // Remove handler after execution
                log.info("[COMCRYPT-CALLBACK-SUCCESS] Callback handler executed successfully for requestId: {}", requestId);
                return true;
            } catch (Exception e) {
                log.error("[COMCRYPT-CALLBACK-ERROR] Error executing callback handler for requestId: {}: {}", requestId, e.getMessage(), e);
                callbackHandlers.remove(requestId); // Remove handler even if it failed
                return false;
            }
        }
        
        // No callback handler registered - execute fire-and-forget continuation
        log.info("[COMCRYPT-CALLBACK-CONTINUATION] No handler registered, executing fire-and-forget continuation for requestId: {}", requestId);
        return processFireAndForgetContinuation(requestId, token, operation);
    }

    /**
     * Validate callback input parameters
     */
    private boolean isValidCallbackInput(String requestId, String token) {
        return requestId != null && !requestId.trim().isEmpty() &&
               token != null && !token.trim().isEmpty();
    }

    /**
     * Process fire-and-forget continuation for callbacks without registered handlers.
     * Handles both encryption (upload) and decryption (download) based on requestId pattern.
     *
     * @param requestId The request ID
     * @param token The encryption/decryption token
     * @param operation The COMCRYPT operation record
     * @return true if continuation was successful, false otherwise
     */
    private boolean processFireAndForgetContinuation(String requestId, String token, OcmsComcryptOperation operation) {
        log.info("[COMCRYPT-CONTINUATION-START] Starting fire-and-forget continuation for requestId: {}", requestId);

        if (SystemConstant.CryptOperation.DECRYPTION.equals(operation.getOperationType())) {
            return processDecryptionContinuation(requestId, token, operation);
        } else {
            return processEncryptionContinuation(requestId, token, operation);
        }
    }

    /**
     * Process encryption continuation for upload flow.
     * Downloads original file from blob, encrypts with token, uploads encrypted file to SFTP.
     */
    private boolean processEncryptionContinuation(String requestId, String token, OcmsComcryptOperation operation) {
        log.info("[COMCRYPT-ENCRYPT-CONTINUATION] Starting encryption continuation for requestId: {}", requestId);

        try {
            // Step 1: Resolve workflow config from requestId and operation type
            ComcryptWorkflowConfigResolver.WorkflowConfig config = workflowConfigResolver.resolveFromRequestId(requestId, operation.getOperationType());
            log.info("[COMCRYPT-ENCRYPT-CONFIG] Resolved config - sftpServer: {}, blobFolder: {}, sftpFolder: {}, encryptType: {}",
                    config.getSftpServer(), config.getBlobFolder(), config.getSftpFolder(), config.getEncryptType());

            // Step 2: Get fileName from operation record
            String fileName = operation.getFileName();
            if (fileName == null || fileName.trim().isEmpty()) {
                log.error("[COMCRYPT-ENCRYPT-ERROR] FileName is null or empty for requestId: {}", requestId);
                return false;
            }

            // Step 3: Download original file from blob
            String blobFolder = config.getBlobFolder().trim();
            String fullBlobPath = blobFolder + fileName;

            log.info("[COMCRYPT-ENCRYPT-DOWNLOAD] Downloading from blob - fullPath: {}", fullBlobPath);
            byte[] originalContent = blobStorageUtil.downloadFromBlob(fullBlobPath);
            if (originalContent == null || originalContent.length == 0) {
                log.error("[COMCRYPT-ENCRYPT-ERROR] Failed to download file from blob: {}", fullBlobPath);
                return false;
            }
            log.info("[COMCRYPT-ENCRYPT-DOWNLOAD-SUCCESS] Downloaded {} bytes from blob", originalContent.length);

            // Step 4: Encrypt file with token
            log.info("[COMCRYPT-ENCRYPT-PROCESS] Encrypting file with encryptType: {}", config.getEncryptType());
            Map<String, Object> encryptResult = comcryptUtils.encryptFile(
                    config.getEncryptType(),
                    requestId,
                    originalContent,
                    token,
                    fileName);
            if (encryptResult == null || encryptResult.get("content") == null) {
                log.error("[COMCRYPT-ENCRYPT-ERROR] Encryption failed for requestId: {}", requestId);

                // Update CES batch job if this is a CES request
                if (requestId.toUpperCase().contains("CES")) {
                    batchJobUpdateHelper.updateOnEncryptionFailure(requestId, config.getEncryptType());
                }

                return false;
            }
            byte[] encryptedContent = (byte[]) encryptResult.get("content");
            String encryptedFileName = (String) encryptResult.get("filename");

            log.info("[COMCRYPT-ENCRYPT-SUCCESS] Encrypted file size: {} bytes, filename: {}", encryptedContent.length, encryptedFileName);

            // Step 5: Upload encrypted file to SFTP
            String sftpPath = config.getSftpFolder() + "/" + encryptedFileName;
            log.info("[COMCRYPT-ENCRYPT-UPLOAD] Uploading encrypted file to SFTP - server: {}, path: {}", config.getSftpServer(), sftpPath);
            String uploadedPath = uploadProcessor.uploadToSftp(encryptedContent, sftpPath, config.getSftpServer());
            log.info("[COMCRYPT-ENCRYPT-UPLOAD-SUCCESS] Uploaded encrypted file to SFTP: {}", uploadedPath);

            // Step 6: Cleanup - delete operation record from database
            cleanupOperationRecord(requestId);

            log.info("[COMCRYPT-ENCRYPT-COMPLETED] Encryption continuation completed successfully for requestId: {}", requestId);
            return true;

        } catch (Exception e) {
            log.error("[COMCRYPT-ENCRYPT-ERROR] Error in encryption continuation for requestId: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process decryption continuation for download flow.
     * Downloads encrypted file from SFTP, decrypts with token, saves to blob.
     * For LTA workflow: also processes the file and deletes from SFTP.
     */
    private boolean processDecryptionContinuation(String requestId, String token, OcmsComcryptOperation operation) {
        log.info("[COMCRYPT-DECRYPT-CONTINUATION] Starting decryption continuation for requestId: {}", requestId);

        try {
            // Step 1: Resolve workflow config from requestId and operation type
            ComcryptWorkflowConfigResolver.WorkflowConfig config = workflowConfigResolver.resolveFromRequestId(requestId, operation.getOperationType());
            log.info("[COMCRYPT-DECRYPT-CONFIG] Resolved config - sftpServer: {}, blobFolder: {}, sftpFolder: {}, encryptType: {}",
                    config.getSftpServer(), config.getBlobFolder(), config.getSftpFolder(), config.getEncryptType());

            // Check if this is an LTA workflow based on requestId pattern (e.g., OCMSLTA001_REQ_timestamp)
            String appCode = requestId.split("_")[0].toUpperCase();
            boolean isLtaWorkflow = appCode.contains("LTA");

            // Step 2: Get fileName from operation record (encrypted file name with .p7)
            String fileName = operation.getFileName();

            // Check if this is a test file (skip LTA processing for test files)
            boolean isTestFile = fileName != null && fileName.toLowerCase().contains("test");
            if (isTestFile) {
                log.info("[COMCRYPT-DECRYPT-TEST] Test file detected, will skip LTA workflow processing: {}", fileName);
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                log.error("[COMCRYPT-DECRYPT-ERROR] FileName is null or empty for requestId: {}", requestId);
                return false;
            }

            // Step 3: Download encrypted file from SFTP
            String sftpFolder = config.getSftpFolder().trim();
            String fullSftpPath = sftpFolder;
            if (!fullSftpPath.endsWith("/") && !fileName.startsWith("/")) {
                fullSftpPath += "/";
            }
            fullSftpPath += fileName;

            log.info("[COMCRYPT-DECRYPT-DOWNLOAD] Downloading encrypted file from SFTP - server: {}, path: {}", config.getSftpServer(), fullSftpPath);
            byte[] encryptedContent = sftpUtil.downloadFile(config.getSftpServer(), fullSftpPath);
            if (encryptedContent == null || encryptedContent.length == 0) {
                log.error("[COMCRYPT-DECRYPT-ERROR] Failed to download encrypted file from SFTP: {}", fullSftpPath);
                return false;
            }
            log.info("[COMCRYPT-DECRYPT-DOWNLOAD-SUCCESS] Downloaded {} bytes from SFTP", encryptedContent.length);

            // Step 4: Decrypt file with token
            log.info("[COMCRYPT-DECRYPT-PROCESS] Decrypting file with encryptType: {}", config.getEncryptType());
            byte[] decryptedContent = comcryptUtils.decryptFile(
                    config.getEncryptType(),
                    requestId,
                    encryptedContent,
                    token);
            if (decryptedContent == null || decryptedContent.length == 0) {
                log.error("[COMCRYPT-DECRYPT-ERROR] Decryption failed for requestId: {}", requestId);
                return false;
            }
            log.info("[COMCRYPT-DECRYPT-SUCCESS] Decrypted file size: {} bytes", decryptedContent.length);

            // Step 5: Save decrypted file to blob (for archiving)
            // Remove .p7 extension for decrypted filename
            String decryptedFileName = fileName.endsWith(".p7") ? fileName.substring(0, fileName.length() - 3) : fileName;
            String blobFolder = config.getBlobFolder().trim();
            String decryptedBlobPath = blobFolder + decryptedFileName;

            log.info("[COMCRYPT-DECRYPT-SAVE] Saving decrypted file to blob - path: {}", decryptedBlobPath);
            AzureBlobStorageUtil.FileUploadResponse uploadResponse = blobStorageUtil.uploadBytesToBlob(decryptedContent, decryptedBlobPath);
            if (!uploadResponse.isSuccess()) {
                log.error("[COMCRYPT-DECRYPT-ERROR] Failed to save decrypted file to blob: {}", uploadResponse.getErrorMessage());
                return false;
            }
            log.info("[COMCRYPT-DECRYPT-SAVE-SUCCESS] Saved decrypted file to blob: {}", decryptedBlobPath);

            // Step 6: LTA-specific processing - process the decrypted file and delete from SFTP
            // Skip LTA processing for test files
            if (isLtaWorkflow && !isTestFile) {
                log.info("[COMCRYPT-DECRYPT-LTA] Processing decrypted LTA response file: {}", decryptedFileName);
                boolean processed = ltaDownloadHelper.processLtaResponseFile(decryptedContent);
                if (!processed) {
                    log.error("[COMCRYPT-DECRYPT-LTA-ERROR] Failed to process LTA response file: {}", decryptedFileName);
                    // Still delete from SFTP even if processing failed (file has integrity error)
                    log.info("[COMCRYPT-DECRYPT-LTA-DELETE] Deleting file with error from SFTP: {}", fullSftpPath);
                    deleteSftpFile(config.getSftpServer(), fullSftpPath);
                    cleanupOperationRecord(requestId);
                    return false;
                }
                log.info("[COMCRYPT-DECRYPT-LTA-SUCCESS] Successfully processed LTA response file: {}", decryptedFileName);

                // Delete the processed file from SFTP
                log.info("[COMCRYPT-DECRYPT-LTA-DELETE] Deleting processed file from SFTP: {}", fullSftpPath);
                deleteSftpFile(config.getSftpServer(), fullSftpPath);
            }

            // Step 7: Cleanup - delete operation record from database
            cleanupOperationRecord(requestId);

            log.info("[COMCRYPT-DECRYPT-COMPLETED] Decryption continuation completed successfully for requestId: {}", requestId);
            return true;

        } catch (Exception e) {
            log.error("[COMCRYPT-DECRYPT-ERROR] Error in decryption continuation for requestId: {}: {}", requestId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete a file from SFTP server
     */
    private void deleteSftpFile(String sftpServer, String filePath) {
        try {
            boolean deleted = sftpUtil.deleteFile(sftpServer, filePath);
            if (deleted) {
                log.info("[COMCRYPT-SFTP-DELETE-SUCCESS] Successfully deleted file from SFTP: {}", filePath);
            } else {
                log.warn("[COMCRYPT-SFTP-DELETE-WARN] Failed to delete file from SFTP: {}", filePath);
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-SFTP-DELETE-ERROR] Error deleting file from SFTP: {}", filePath, e);
        }
    }

    /**
     * Cleanup operation record from database
     */
    private void cleanupOperationRecord(String requestId) {
        try {
            boolean deleted = operationHelper.deleteOperationByRequestId(requestId);
            if (deleted) {
                log.info("[COMCRYPT-CLEANUP] Deleted operation record for requestId: {}", requestId);
            } else {
                log.warn("[COMCRYPT-CLEANUP-WARN] Failed to delete operation record for requestId: {}", requestId);
            }
        } catch (Exception e) {
            log.warn("[COMCRYPT-CLEANUP-WARN] Error deleting operation record: {}", e.getMessage());
        }
    }

    /**
     * Check if a callback handler is registered for a specific request ID
     */
    public boolean isCallbackHandlerRegistered(String requestId) {
        return requestId != null && callbackHandlers.containsKey(requestId.trim());
    }

    /**
     * Get number of registered callback handlers
     */
    public int getCallbackHandlerCount() {
        return callbackHandlers.size();
    }
}
