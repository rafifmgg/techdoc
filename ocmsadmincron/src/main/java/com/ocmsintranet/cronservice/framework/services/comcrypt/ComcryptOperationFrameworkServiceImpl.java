package com.ocmsintranet.cronservice.framework.services.comcrypt;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.*;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptCallbackProcessor;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation of ComcryptOperationFrameworkService.
 * Provides a unified interface for workflows to interact with COMCRYPT for secure file transfer operations.
 * 
 * This implementation provides ONLY the main flow methods that workflow services should use:
 * - executeEncryptionFlow(): Complete encryption workflow
 * - executeDecryptionFlow(): Complete decryption workflow
 * 
 * All other operations are handled internally by helper classes and not exposed to workflow services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComcryptOperationFrameworkServiceImpl implements ComcryptOperationFrameworkService {

    private final ComcryptOperationHelper operationHelper;
    private final ComcryptTokenHelper tokenHelper;
    private final ComcryptFileHelper fileHelper;
    private final ComcryptCallbackHelper callbackHelper;
    private final ComcryptWorkflowConfigResolver workflowConfigResolver;
    private final AzureBlobStorageUtil blobStorageUtil;

    // Also inject ComcryptCallbackProcessor to handle callbacks from ComcryptFileProcessingService
    // This fixes the callback routing issue where handlers registered in ComcryptCallbackProcessor
    // were not being executed because callbacks were only routed to ComcryptCallbackHelper
    private final ComcryptCallbackProcessor callbackProcessor;


    // ================================
    // Callback Management - Updated to use requestId-based routing
    // ================================

    /**
     * Register a callback handler for a specific request ID
     */
    @Override
    public void registerCallbackHandler(String requestId, Runnable handler) {
        callbackHelper.registerCallbackHandler(requestId, handler);
    }

    /**
     * Remove a callback handler for a specific request ID
     * Removes from BOTH registries to ensure cleanup
     */
    @Override
    public boolean removeCallbackHandler(String requestId) {
        boolean helperRemoved = callbackHelper.removeCallbackHandler(requestId);
        boolean processorRemoved = callbackProcessor.removeCallbackHandler(requestId);
        return helperRemoved || processorRemoved;
    }

    /**
     * Check if a callback handler is registered for a specific request ID
     * Checks BOTH registries to ensure all handlers are found
     */
    @Override
    public boolean isCallbackHandlerRegistered(String requestId) {
        return callbackHelper.isCallbackHandlerRegistered(requestId)
                || callbackProcessor.hasCallbackHandler(requestId);
    }

    /**
     * Process token callback - Called by ComcryptTokenReceiverController
     *
     * This method delegates to BOTH callback processors to ensure handlers are found
     * regardless of which processor they were registered with:
     * - ComcryptCallbackHelper: Used by ComcryptOperationFrameworkService flows
     * - ComcryptCallbackProcessor: Used by ComcryptFileProcessingService flows (LTA upload, etc.)
     *
     * The callback is considered successful if EITHER processor handles it successfully.
     */
    public boolean processTokenCallback(String requestId, String token) {
        log.info("[COMCRYPT-CALLBACK-UNIFIED] Processing token callback for requestId: {}", requestId);

        boolean helperProcessed = false;
        boolean processorProcessed = false;

        // First, try ComcryptCallbackHelper (original handler)
        try {
            helperProcessed = callbackHelper.processTokenCallback(requestId, token);
            if (helperProcessed) {
                log.info("[COMCRYPT-CALLBACK-UNIFIED] Callback processed by ComcryptCallbackHelper for requestId: {}", requestId);
            }
        } catch (Exception e) {
            log.warn("[COMCRYPT-CALLBACK-UNIFIED] Error in ComcryptCallbackHelper for requestId: {}: {}", requestId, e.getMessage());
        }

        // Also try ComcryptCallbackProcessor (used by ComcryptFileProcessingService)
        // This ensures handlers registered by LtaUploadJob and similar jobs are executed
        try {
            if (callbackProcessor.hasCallbackHandler(requestId)) {
                processorProcessed = callbackProcessor.processTokenCallback(requestId, token);
                if (processorProcessed) {
                    log.info("[COMCRYPT-CALLBACK-UNIFIED] Callback processed by ComcryptCallbackProcessor for requestId: {}", requestId);
                }
            } else {
                log.debug("[COMCRYPT-CALLBACK-UNIFIED] No handler in ComcryptCallbackProcessor for requestId: {}", requestId);
            }
        } catch (Exception e) {
            log.warn("[COMCRYPT-CALLBACK-UNIFIED] Error in ComcryptCallbackProcessor for requestId: {}: {}", requestId, e.getMessage());
        }

        boolean result = helperProcessed || processorProcessed;
        log.info("[COMCRYPT-CALLBACK-UNIFIED] Callback processing complete for requestId: {}, result: {} (helper: {}, processor: {})",
                requestId, result, helperProcessed, processorProcessed);

        return result;
    }

    // ================================
    // Main Workflow Flow Methods - The ONLY methods workflow services should call
    // ================================

    /**
     * Complete encryption flow - THE MAIN METHOD for encryption
     * This handles the entire encryption process from start to finish
     * 
     * @param encryptType The type of encryption to perform
     * @param appCode The application code for the operation
     * @param fileContent The file content to encrypt
     * @param fileName The name of the file being encrypted
     * @param metadata Additional metadata for the operation
     * @param callbackHandler Optional callback to execute when token is received
     * @return EncryptionFlowResult containing the encrypted content and operation details
     */
    @Override
    public EncryptionFlowResult executeEncryptionFlow(String encryptType, String appCode, byte[] fileContent,
                                                     String fileName, String metadata,
                                                     Runnable callbackHandler) {
        log.info("[COMCRYPT-FLOW-ENCRYPT-START] Executing encryption flow for fileName: {}", fileName);

        try {
            // Step 1: Request token using TokenHelper
            String requestId = tokenHelper.requestToken(appCode, SystemConstant.CryptOperation.ENCRYPTION, fileName, metadata);
            if (requestId == null) {
                log.error("[COMCRYPT-FLOW-ENCRYPT-ERROR] Failed to request token");
                return EncryptionFlowResult.failed("Failed to request token");
            }

            log.info("[COMCRYPT-FLOW-ENCRYPT-TOKEN-REQUESTED] Token requested with requestId: {}", requestId);

            // Step 1.5: Upload fileContent to blob based on requestId (for fire-and-forget callback)
            log.info("[COMCRYPT-FLOW-ENCRYPT-BLOB-UPLOAD] Uploading file to blob for requestId: {}", requestId);

            ComcryptWorkflowConfigResolver.WorkflowConfig config =
                    workflowConfigResolver.resolveFromRequestId(requestId, SystemConstant.CryptOperation.ENCRYPTION);

            String blobFolder = config.getBlobFolder();
            if (blobFolder == null || blobFolder.trim().isEmpty()) {
                log.error("[COMCRYPT-FLOW-ENCRYPT-ERROR] Blob folder not configured for requestId: {}", requestId);
                return EncryptionFlowResult.failed("Blob folder not configured");
            }

            String blobPath = blobFolder.endsWith("/") ? blobFolder + fileName : blobFolder + "/" + fileName;
            log.info("[COMCRYPT-FLOW-ENCRYPT-BLOB-UPLOAD] Uploading {} bytes to blob: {}", fileContent.length, blobPath);

            AzureBlobStorageUtil.FileUploadResponse uploadResponse = blobStorageUtil.uploadBytesToBlob(fileContent, blobPath);
            if (!uploadResponse.isSuccess()) {
                log.error("[COMCRYPT-FLOW-ENCRYPT-ERROR] Failed to upload file to blob: {}", uploadResponse.getErrorMessage());
                return EncryptionFlowResult.failed("Failed to upload file to blob: " + uploadResponse.getErrorMessage());
            }
            log.info("[COMCRYPT-FLOW-ENCRYPT-BLOB-UPLOAD-SUCCESS] File uploaded to blob: {}", blobPath);

            // Step 2: Wait for token using TokenHelper
            Optional<String> tokenOpt = tokenHelper.waitForToken(requestId);
            if (!tokenOpt.isPresent()) {
                log.error("[COMCRYPT-FLOW-ENCRYPT-ERROR] Token not received");
                return EncryptionFlowResult.failed("Token not received");
            }
            
            log.info("[COMCRYPT-FLOW-ENCRYPT-TOKEN-RECEIVED] Token received for requestId: {}", requestId);

            // Step 3: Execute callback if provided
            if (callbackHandler != null) {
                try {
                    log.info("[COMCRYPT-FLOW-ENCRYPT-CALLBACK-START] Executing callback for requestId: {}", requestId);
                    callbackHandler.run();
                    log.info("[COMCRYPT-FLOW-ENCRYPT-CALLBACK-COMPLETED] Callback executed successfully for requestId: {}", requestId);
                } catch (Exception e) {
                    log.warn("[COMCRYPT-FLOW-ENCRYPT-CALLBACK-ERROR] Callback execution failed, but continuing with encryption", e);
                }
            }

            // Step 4: Encrypt the file using FileHelper
            log.info("[COMCRYPT-FLOW-ENCRYPT-FILE-START] Starting file encryption for requestId: {}", requestId);
            Optional<Map<String, Object>> encryptionResult = fileHelper.encryptFile(encryptType, requestId, fileContent);
            if (!encryptionResult.isPresent()) {
                log.error("[COMCRYPT-FLOW-ENCRYPT-ERROR] File encryption failed");
                return EncryptionFlowResult.failed("File encryption failed");
            }
            
            log.info("[COMCRYPT-FLOW-ENCRYPT-FILE-SUCCESS] File encrypted successfully for requestId: {}", requestId);
            
            // Step 6: Cleanup operation record after successful completion
            boolean cleanedUp = operationHelper.deleteOperationByRequestId(requestId);
            if (cleanedUp) {
                log.info("[COMCRYPT-FLOW-ENCRYPT-CLEANUP] Operation record cleaned up successfully for requestId: {}", requestId);
            } else {
                log.warn("[COMCRYPT-FLOW-ENCRYPT-CLEANUP-FAILED] Failed to cleanup operation record for requestId: {}", requestId);
            }
            
            log.info("[COMCRYPT-FLOW-ENCRYPT-COMPLETED] Encryption flow completed successfully for requestId: {}", requestId);

            return EncryptionFlowResult.success(requestId, encryptionResult.get());

        } catch (Exception e) {
            log.error("[COMCRYPT-FLOW-ENCRYPT-EXCEPTION] Error in encryption flow for fileName: {}", fileName, e);
            return EncryptionFlowResult.failed("Encryption flow error: " + e.getMessage());
        }
    }

    /**
     * Complete decryption flow - THE MAIN METHOD for decryption
     * This handles the entire decryption process from start to finish
     * 
     * @param decryptType The type of decryption to perform
     * @param appCode The application code for the operation
     * @param encryptedContent The encrypted content to decrypt
     * @param metadata Additional metadata for the operation
     * @param callbackHandler Optional callback to execute when token is received
     * @return DecryptionFlowResult containing the decrypted content and operation details
     */
    @Override
    public DecryptionFlowResult executeDecryptionFlow(String decryptType, String appCode, byte[] encryptedContent, 
                                                     String metadata, Runnable callbackHandler) {
        log.info("[COMCRYPT-FLOW-DECRYPT-START] Executing decryption flow for encryptedSize: {} bytes", encryptedContent.length);
        
        try {
            // Generate a default filename for decryption operations
            String fileName = "decrypted_file_" + System.currentTimeMillis() + ".dat";
            
            // Step 1: Request token using TokenHelper
            String requestId = tokenHelper.requestToken(appCode, SystemConstant.CryptOperation.DECRYPTION, fileName, metadata);
            if (requestId == null) {
                log.error("[COMCRYPT-FLOW-DECRYPT-ERROR] Failed to request token");
                return DecryptionFlowResult.failed("Failed to request token");
            }
            
            log.info("[COMCRYPT-FLOW-DECRYPT-TOKEN-REQUESTED] Token requested with requestId: {}", requestId);

            // Step 2: Wait for token using TokenHelper
            Optional<String> tokenOpt = tokenHelper.waitForToken(requestId);
            if (!tokenOpt.isPresent()) {
                log.error("[COMCRYPT-FLOW-DECRYPT-ERROR] Token not received");
                return DecryptionFlowResult.failed("Token not received");
            }
            
            log.info("[COMCRYPT-FLOW-DECRYPT-TOKEN-RECEIVED] Token received for requestId: {}", requestId);

            // Step 3: Execute callback if provided
            if (callbackHandler != null) {
                try {
                    log.info("[COMCRYPT-FLOW-DECRYPT-CALLBACK-START] Executing callback for requestId: {}", requestId);
                    callbackHandler.run();
                    log.info("[COMCRYPT-FLOW-DECRYPT-CALLBACK-COMPLETED] Callback executed successfully for requestId: {}", requestId);
                } catch (Exception e) {
                    log.warn("[COMCRYPT-FLOW-DECRYPT-CALLBACK-ERROR] Callback execution failed, but continuing with decryption", e);
                }
            }

            // Step 4: Decrypt the file using FileHelper
            log.info("[COMCRYPT-FLOW-DECRYPT-FILE-START] Starting file decryption for requestId: {}", requestId);
            Optional<byte[]> decryptionResult = fileHelper.decryptFile(decryptType, requestId, encryptedContent);
            if (!decryptionResult.isPresent()) {
                log.error("[COMCRYPT-FLOW-DECRYPT-ERROR] File decryption failed");
                return DecryptionFlowResult.failed("File decryption failed");
            }
            
            log.info("[COMCRYPT-FLOW-DECRYPT-FILE-SUCCESS] File decrypted successfully for requestId: {}", requestId);
            
            // Step 6: Cleanup operation record after successful completion
            boolean cleanedUp = operationHelper.deleteOperationByRequestId(requestId);
            if (cleanedUp) {
                log.info("[COMCRYPT-FLOW-DECRYPT-CLEANUP] Operation record cleaned up successfully for requestId: {}", requestId);
            } else {
                log.warn("[COMCRYPT-FLOW-DECRYPT-CLEANUP-FAILED] Failed to cleanup operation record for requestId: {}", requestId);
            }
            
            log.info("[COMCRYPT-FLOW-DECRYPT-COMPLETED] Decryption flow completed successfully for requestId: {}", requestId);

            return DecryptionFlowResult.success(requestId, decryptionResult.get());

        } catch (Exception e) {
            log.error("[COMCRYPT-FLOW-DECRYPT-EXCEPTION] Error in decryption flow for encryptedSize: {} bytes", encryptedContent.length, e);
            return DecryptionFlowResult.failed("Decryption flow error: " + e.getMessage());
        }
    }

    // ================================
    // Administrative Methods - For maintenance/monitoring only
    // ================================

    /**
     * Handle timed out operations - For maintenance jobs only
     */
    public int handleTimedOutOperations(int timeoutMinutes) {
        return operationHelper.handleTimedOutOperations(timeoutMinutes);
    }

    /**
     * Clean up old operations - For maintenance jobs only
     */
    public int cleanupOldOperations(int retentionDays) {
        return operationHelper.cleanupOldOperations(retentionDays);
    }

    // ================================
    // Result Classes for Flow Methods
    // ================================

    /**
     * Result class for encryption flow operations
     */
    public static class EncryptionFlowResult {
        private final boolean success;
        private final String requestId;
        private final Map<String, Object> encryptionData;
        private final String errorMessage;

        private EncryptionFlowResult(boolean success, String requestId, Map<String, Object> encryptionData, String errorMessage) {
            this.success = success;
            this.requestId = requestId;
            this.encryptionData = encryptionData;
            this.errorMessage = errorMessage;
        }

        public static EncryptionFlowResult success(String requestId, Map<String, Object> encryptionData) {
            return new EncryptionFlowResult(true, requestId, encryptionData, null);
        }

        public static EncryptionFlowResult failed(String errorMessage) {
            return new EncryptionFlowResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getRequestId() { return requestId; }
        public Map<String, Object> getEncryptionData() { return encryptionData; }
        public String getErrorMessage() { return errorMessage; }
        public byte[] getEncryptedContent() { 
            return encryptionData != null ? (byte[]) encryptionData.get("content") : null; 
        }
    }

    /**
     * Result class for decryption flow operations
     */
    public static class DecryptionFlowResult {
        private final boolean success;
        private final String requestId;
        private final byte[] decryptedContent;
        private final String errorMessage;

        private DecryptionFlowResult(boolean success, String requestId, byte[] decryptedContent, String errorMessage) {
            this.success = success;
            this.requestId = requestId;
            this.decryptedContent = decryptedContent;
            this.errorMessage = errorMessage;
        }

        public static DecryptionFlowResult success(String requestId, byte[] decryptedContent) {
            return new DecryptionFlowResult(true, requestId, decryptedContent, null);
        }

        public static DecryptionFlowResult failed(String errorMessage) {
            return new DecryptionFlowResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getRequestId() { return requestId; }
        public byte[] getDecryptedContent() { return decryptedContent; }
        public String getErrorMessage() { return errorMessage; }
    }
}
