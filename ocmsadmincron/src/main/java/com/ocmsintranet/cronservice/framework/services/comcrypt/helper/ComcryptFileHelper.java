package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.utilities.comcrypt.ComcryptUtils;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for COMCRYPT file encryption/decryption operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptFileHelper {

    private final ComcryptUtils comcryptUtils;
    private final ComcryptOperationHelper operationHelper;
    
    /**
     * Encrypt a file using COMCRYPT
     */
    @Transactional
    public Optional<Map<String, Object>> encryptFile(String encryptType, String requestId, byte[] fileContent) {
        log.info("[COMCRYPT-ENCRYPT-START] Encrypting file for requestId: {}, fileSize: {} bytes", requestId, fileContent.length);
        
        String appCode = requestId.split("_")[0];
        OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
        if (operation == null) {
            log.error("[COMCRYPT-ENCRYPT-ERROR] No COMCRYPT operation found for requestId: {}", requestId);
            return Optional.empty();
        }
        
        String token = operation.getToken();
        if (token == null) {
            log.error("[COMCRYPT-ENCRYPT-ERROR] No token available for encryption, requestId: {}", requestId);
            return Optional.empty();
        }
        
        try {
            log.info("[COMCRYPT-ENCRYPT-PROCESSING] Starting encryption for requestId: {}", requestId);
            
            // Encrypt file using COMCRYPT
            Map<String, Object> encryptionResult = comcryptUtils.encryptFile(
                    encryptType, 
                    requestId, 
                    fileContent, 
                    token, 
                    operation.getFileName());
            
            if (encryptionResult == null) {
                log.error("[COMCRYPT-ENCRYPT-FAILED] Encryption failed for requestId: {}", requestId);
                return Optional.empty();
            }
            
            // Get encrypted content and log result
            byte[] encryptedContent = (byte[]) encryptionResult.get("content");
            String encryptedFilename = (String) encryptionResult.get("filename");
            log.info("[COMCRYPT-ENCRYPT-SUCCESS] File encrypted successfully for requestId: {}, encryptedSize: {} bytes, filename: {}", 
                    requestId, encryptedContent != null ? encryptedContent.length : 0, encryptedFilename);
            
            return Optional.of(encryptionResult);
            
        } catch (Exception e) {
            log.error("[COMCRYPT-ENCRYPT-EXCEPTION] Error encrypting file for requestId: {}", requestId, e);
            return Optional.empty();
        }
    }

    /**
     * Decrypt a file using COMCRYPT
     */
    @Transactional
    public Optional<byte[]> decryptFile(String decryptType, String requestId, byte[] encryptedContent) {
        log.info("[COMCRYPT-DECRYPT-START] Decrypting file for requestId: {}, encryptedSize: {} bytes", requestId, encryptedContent.length);
        
        String appCode = requestId.split("_")[0];
        OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
        if (operation == null) {
            log.error("[COMCRYPT-DECRYPT-ERROR] No COMCRYPT operation found for requestId: {}", requestId);
            return Optional.empty();
        }
        
        String token = operation.getToken();
        if (token == null) {
            log.error("[COMCRYPT-DECRYPT-ERROR] No token available for decryption, requestId: {}", requestId);
            return Optional.empty();
        }
        
        try {
            log.info("[COMCRYPT-DECRYPT-PROCESSING] Starting decryption for requestId: {}", requestId);
            
            // Decrypt file using COMCRYPT
            byte[] decryptedContent = comcryptUtils.decryptFile(
                    decryptType,
                    requestId, 
                    encryptedContent, 
                    token);
            
            if (decryptedContent == null) {
                log.error("[COMCRYPT-DECRYPT-FAILED] Decryption failed for requestId: {}", requestId);
                return Optional.empty();
            }
            
            // Log successful decryption
            log.info("[COMCRYPT-DECRYPT-SUCCESS] File decrypted successfully for requestId: {}, decryptedSize: {} bytes", 
                    requestId, decryptedContent.length);
            
            return Optional.of(decryptedContent);
            
        } catch (Exception e) {
            log.error("[COMCRYPT-DECRYPT-EXCEPTION] Error decrypting file for requestId: {}", requestId, e);
            return Optional.empty();
        }
    }
}