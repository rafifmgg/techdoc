package com.ocmsintranet.cronservice.framework.services.comcrypt;

/**
 * Framework service for COMCRYPT operations.
 * Provides a unified interface for workflows to interact with COMCRYPT for secure file transfer operations.
 * This service is designed to be used by multiple workflows that require file encryption/decryption.
 */
public interface ComcryptOperationFrameworkService {

    // ================================
    // Callback Management
    // ================================

    /**
     * Register a callback handler for a specific request ID
     * This is the preferred approach - register a handler that will be called
     * when the COMCRYPT token callback arrives for this specific request ID.
     */
    void registerCallbackHandler(String requestId, Runnable handler);

    /**
     * Remove a callback handler for a specific request ID
     */
    boolean removeCallbackHandler(String requestId);

    /**
     * Check if a callback handler is registered for a specific request ID
     */
    boolean isCallbackHandlerRegistered(String requestId);

    /**
     * Process token callback - Called by ComcryptTokenReceiverController
     */
    boolean processTokenCallback(String requestId, String token);

    // ================================
    // Main Workflow Flow Methods
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
    ComcryptOperationFrameworkServiceImpl.EncryptionFlowResult executeEncryptionFlow(String encryptType, String appCode, byte[] fileContent, 
                                                     String fileName, String metadata, 
                                                     Runnable callbackHandler);

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
    ComcryptOperationFrameworkServiceImpl.DecryptionFlowResult executeDecryptionFlow(String decryptType, String appCode, byte[] encryptedContent, 
                                                     String metadata, Runnable callbackHandler);

    // ================================
    // Administrative Methods
    // ================================

    /**
     * Handle timed out operations - For maintenance jobs only
     */
    int handleTimedOutOperations(int timeoutMinutes);

    /**
     * Clean up old operations - For maintenance jobs only
     */
    int cleanupOldOperations(int retentionDays);
}