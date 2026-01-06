package com.ocmsintranet.cronservice.utilities.comcrypt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptOperationHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptNotificationHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for COMCRYPT (Secure Lightweight File Transfer) operations
 * Provides methods for token request, file encryption, and file decryption
 * 
 * This utility is used for secure file transfers with Land Transport Authority (LTA)
 * and other government agencies requiring encrypted file exchange.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptUtils {

    private final RestTemplate restTemplate;
    private final AzureKeyVaultUtil keyVaultUtil;
    private final ObjectMapper objectMapper;
    private final ComcryptOperationHelper operationHelper;
    private final ComcryptNotificationHelper notificationHelper;

    // Track retry attempts per requestId for encrypt/decrypt operations
    private final ConcurrentHashMap<String, Integer> retryTracker = new ConcurrentHashMap<>();

    @Value("${ocms.comcrypt.api.maxRetry:1}")
    private int maxRetryAttempts;

    @Value("${ocms.comcrypt.token}")
    private String comcryptTokenPath;
    
    @Value("${ocms.comcrypt.encrypt.slift}")
    private String sliftEncryptPath;
    
    @Value("${ocms.comcrypt.decrypt.slift}")
    private String sliftDecryptPath;

    @Value("${ocms.comcrypt.encrypt.pgp}")
    private String pgpEncryptPath;
    
    @Value("${ocms.comcrypt.decrypt.pgp}")
    private String pgpDecryptPath;
    
    @Value("${ocms.APIM.baseurl}")
    private String apimBaseUrl;
    
    @Value("${ocms.APIM.subscriptionKey}")
    private String comcryptApimHeader;
    
    @Value("${ocms.APIM.comcrypt.secretName}")
    private String comcryptSecretName;

    private final ComcryptConfig comcryptConfig;
    
    /**
     * Request a token from the COMCRYPT service
     * 
     * @param appCode Application code for COMCRYPT (e.g., OCMSLTA001)
     * @param requestId Unique request ID (typically appCode + timestamp)
     * @return true if token request was successfully sent, false otherwise
     */
    public boolean requestToken(String appCode, String requestId) {
        log.info("Requesting COMCRYPT token for appCode: {}, requestId: {}", appCode, requestId);
        
        try {
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("appCode", appCode);
            requestBody.put("requestId", requestId);
            
            // Get APIM subscription key from Key Vault
            String apimKey = keyVaultUtil.getSecret(comcryptSecretName);
            if (apimKey == null || apimKey.trim().isEmpty()) {
                log.error("Failed to retrieve APIM key from Key Vault for COMCRYPT token request");
                return false;
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(comcryptApimHeader, apimKey);
            
            // Create HTTP entity
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            
            // Combine base URL with path
            String fullUrl = apimBaseUrl + comcryptTokenPath;
            log.debug("Full COMCRYPT token URL: {}", fullUrl);
            
            // Make REST call
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                fullUrl != null ? fullUrl : "",
                HttpMethod.POST,
                entity,
                JsonNode.class
            );
            
            // Process response
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("COMCRYPT token request sent successfully. Token will be received via callback.");
                return true;
            } else {
                log.error("Failed to send COMCRYPT token request. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error requesting COMCRYPT token: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Wait for COMCRYPT token to be received via callback
     * 
     * @param maxWaitTimeMs Maximum time to wait for token in milliseconds
     * @param checkIntervalMs Interval between checks in milliseconds
     * @param requestId The request ID to look up token for (multi-server compatible)
     * @return The received token, or null if timeout occurs
     */
    public String waitForToken(long maxWaitTimeMs, long checkIntervalMs, String requestId) {
        log.info("Waiting for COMCRYPT token callback, max wait time: {} ms, requestId: {}", maxWaitTimeMs, requestId);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxWaitTimeMs;
        
        while (System.currentTimeMillis() < endTime) {
            // Multi-server compatible: Check database for token first
            try {
                OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
                if (operation != null && operation.getToken() != null && !operation.getToken().isEmpty()) {
                    String token = operation.getToken();
                    
                    // Update local memory cache for immediate access
                    comcryptConfig.setComcryptAppToken(token);
                    
                    log.info("COMCRYPT token received via callback for requestId: {}", requestId);
                    return token;
                }
            } catch (Exception e) {
                log.error("Error checking database for COMCRYPT token for requestId: {}", requestId, e);
            }
            
            // Fallback: Check local memory (for single-server compatibility)
            if (comcryptConfig.hasValidToken()) {
                String token = comcryptConfig.getComcryptAppToken();
                log.info("COMCRYPT token found in local memory for requestId: {}", requestId);
                return token;
            }
            
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for COMCRYPT token for requestId: {}", requestId, e);
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        log.error("Timeout waiting for COMCRYPT token for requestId: {} (waited {} ms)", requestId, maxWaitTimeMs);
        return null;
    }
    
    /**
     * Wait for token indefinitely (no timeout)
     * 
     * @param requestId The request ID to look up token for (multi-server compatible)
     * @return The received token, or null if error occurs
     */
    public String waitForToken(String requestId) {
        log.info("Waiting for COMCRYPT token callback indefinitely, requestId: {}", requestId);
        
        while (true) {
            try {
                // Multi-server compatible: Check database for token first
                OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
                if (operation != null && operation.getToken() != null && !operation.getToken().isEmpty()) {
                    String token = operation.getToken();
                    
                    // Update local memory cache for immediate access
                    comcryptConfig.setComcryptAppToken(token);
                    
                    log.info("COMCRYPT token received via callback for requestId: {}", requestId);
                    return token;
                }
            } catch (Exception e) {
                log.error("Error checking database for COMCRYPT token for requestId: {}", requestId, e);
            }
            
            // Fallback: Check local memory (for single-server compatibility)
            if (comcryptConfig.hasValidToken()) {
                String token = comcryptConfig.getComcryptAppToken();
                log.info("COMCRYPT token found in local memory for requestId: {}", requestId);
                return token;
            }
            
            // Wait before checking again (default interval)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for token for requestId: {}", requestId);
                return null;
            }
        }
    }
    
    /**
     * Legacy waitForToken method for backward compatibility
     * 
     * @param maxWaitTimeMs Maximum time to wait for token in milliseconds
     * @param checkIntervalMs Interval between checks in milliseconds
     * @return The received token, or null if timeout occurs
     * @deprecated Use waitForToken(long, long, String) for multi-server compatibility
     */
    @Deprecated
    public String waitForToken(long maxWaitTimeMs, long checkIntervalMs) {
        log.warn("Using deprecated waitForToken() method - consider using waitForToken(long, long, String) for multi-server compatibility");
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxWaitTimeMs;
        
        while (System.currentTimeMillis() < endTime) {
            if (comcryptConfig.hasValidToken()) {
                String token = comcryptConfig.getComcryptAppToken();
                log.info("COMCRYPT token received via callback (legacy method)");
                return token;
            }
            
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for COMCRYPT token", e);
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        log.error("Timeout waiting for COMCRYPT token (legacy method)");
        return null;
    }
    
    /**
     * Encrypt file with COMCRYPT service
     * @param appCode Application code for COMCRYPT
     * @param fileContent Content of the file to encrypt
     * @param token COMCRYPT token to use for encryption
     * @param filename Optional filename to use for the file (required for LTA enquiry)
     * @return Map containing encrypted file content and filename with .p7 extension
     */
    public Map<String, Object> encryptFile(String encryptType, String requestId, byte[] fileContent, String token, String filename) {
        String appCode = requestId.split("_")[0];
        log.info("Encrypting file with COMCRYPT for appCode: {}, token length: {}", appCode, token.length());
        
        try {
            // IMPORTANT: Do NOT decode Base64 here - pass file content as-is
            // The checkAndDecodeBase64Content() was causing HC480 errors by incorrectly
            // decoding binary data that happened to match Base64 patterns.
            // EncryptingFileServices (which works) does NOT do any Base64 decoding.
            byte[] actualFileContent = fileContent;
            if (actualFileContent == null) {
                log.error("Failed to process file content, file content is null");
                return null;
            }
            log.info("File content size: {} bytes (Base64 decoding SKIPPED to match working implementation)", actualFileContent.length);
            
            // Get APIM subscription key from Key Vault
            String apimKey = keyVaultUtil.getSecret(comcryptSecretName);
            if (apimKey == null || apimKey.trim().isEmpty()) {
                log.error("Failed to retrieve APIM key from Key Vault for COMCRYPT encryption");
                return null;
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set(comcryptApimHeader, apimKey);
            
            // For LTA enquiry requests, use the actual filename that is passed in
            // This should be in the format VRL-URA-OFFENQ-D1-YYYYMMDDHHMMSS.dat
            // For other requests, use a default filename
            final String actualFilename;
            if (requestId.startsWith("LTAENQ_") && filename != null && !filename.isEmpty()) {
                // Use the filename passed from the LTA enquiry workflow
                actualFilename = filename;
                log.info("Using passed filename for COMCRYPT request: {}", actualFilename);
            } else if (filename != null && !filename.isEmpty()) {
                // Just use the incoming filename
                actualFilename = filename;
                log.info("Using incoming filename: {}", actualFilename);
            } else {
                // Only if no filename is provided at all, use a simple default
                actualFilename = "file";
                log.info("No filename provided, using default: {}", actualFilename);
            }
            log.info("Using filename for COMCRYPT request: {}", actualFilename);

            ByteArrayResource fileResource = new ByteArrayResource(actualFileContent) {
                @Override
                public String getFilename() {
                    return actualFilename;
                }
            };

            // Create multipart request - IMPORTANT: Add "file" FIRST, then "token"
            // This matches the order used in EncryptingFileServices/ComcryptHelper which works
            LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", fileResource);  // File FIRST (matches working implementation)
            parts.add("token", token);        // Token SECOND
            
            // Create HTTP entity with multipart content
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
            
            // Combine base URL with path based on encryption type
            // Also determine file extension: SLIFT uses .p7, PGP uses .pgp
            String encryptPath;
            String fileExtension;
            if (SystemConstant.CryptOperation.SLIFT.equalsIgnoreCase(encryptType)) {
                encryptPath = sliftEncryptPath;
                fileExtension = ".p7";
            } else if (SystemConstant.CryptOperation.PGP.equalsIgnoreCase(encryptType)) {
                encryptPath = pgpEncryptPath;
                fileExtension = ".PGP";
            } else {
                // Default to SLIFT for backward compatibility
                log.warn("Unknown encryption type '{}', defaulting to SLIFT", encryptType);
                encryptPath = sliftEncryptPath;
                fileExtension = ".p7";
            }
            
            String fullUrl = apimBaseUrl + encryptPath.replace("{appcode}", appCode);
            log.debug("Full COMCRYPT encrypt URL: {}, encryption type: {}", fullUrl, encryptType);
            
            // Make REST call
            ResponseEntity<byte[]> response = restTemplate.exchange(
                fullUrl != null ? fullUrl : "",
                HttpMethod.POST,
                entity,
                byte[].class
            );
            
            // Process response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                // Check Content-Type to detect JSON error response vs binary encrypted content
                MediaType contentType = response.getHeaders().getContentType();
                log.info("Response Content-Type: {}", contentType);

                // Determine expected Content-Type based on encryption type
                // PGP: expects application/octet-stream or binary
                // SLIFT: expects application/octet-stream or binary
                boolean isJsonResponse = contentType != null && contentType.includes(MediaType.APPLICATION_JSON);
                boolean isBinaryResponse = contentType != null && (
                    contentType.includes(MediaType.APPLICATION_OCTET_STREAM) ||
                    contentType.toString().contains("binary") ||
                    contentType.toString().contains("pgp") ||
                    contentType.toString().contains("encrypted")
                );

                // If Content-Type is JSON, this is an error response, not binary encrypted data
                if (isJsonResponse) {
                    String jsonError = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                    log.error("APIM returned JSON error response instead of binary encrypted data");
                    log.error("Encryption type: {}, Expected: binary/octet-stream, Actual: {}", encryptType, contentType);
                    log.error("JSON error response: {}", jsonError);

                    // Parse JSON to extract responseCode
                    Map<String, Object> errorResult = handleJsonErrorResponse(jsonError, requestId,
                            SystemConstant.CryptOperation.ENCRYPTION, filename);
                    if (errorResult != null && "retry".equals(errorResult.get("action"))) {
                        // Retry the operation
                        log.info("[COMCRYPT-ENCRYPT] Retrying encryption for requestId: {}", requestId);
                        return encryptFile(encryptType, requestId, fileContent, token, filename);
                    }
                    return null;
                }

                // Log success for binary response
                if (isBinaryResponse) {
                    log.info("Received expected binary response for {} encryption", encryptType);
                } else {
                    log.warn("Unexpected Content-Type: {} for {} encryption, but proceeding as it's not JSON", contentType, encryptType);
                }

                byte[] encryptedBytes = response.getBody();
                if (encryptedBytes != null) {
                    log.info("Successfully encrypted file. Encrypted size: {} bytes", encryptedBytes.length);
                } else {
                    log.warn("Encrypted file content is null");
                }
                
                // Extract filename from Content-Disposition header if available
                String resultFilename = "encrypted-file" + fileExtension;
                String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename")) {
                    // Extract filename from Content-Disposition header
                    int filenameIndex = contentDisposition.indexOf("filename=") + 9;
                    resultFilename = contentDisposition.substring(filenameIndex);
                    // Remove any quotes if present
                    if (resultFilename.startsWith("\"") && resultFilename.endsWith("\"")) {
                        resultFilename = resultFilename.substring(1, resultFilename.length() - 1);
                    }
                    log.info("Extracted filename from Content-Disposition: {}", resultFilename);
                } else {
                    // If no filename in header, add appropriate extension based on encryption type
                    if (!actualFilename.toLowerCase().endsWith(fileExtension)) {
                        resultFilename = actualFilename + fileExtension;
                    } else {
                        resultFilename = actualFilename;
                    }
                    log.info("Using filename with {} extension: {}", fileExtension, resultFilename);
                }
                
                // Return both the encrypted content and the filename
                Map<String, Object> result = new HashMap<>();
                result.put("content", encryptedBytes);
                result.put("filename", resultFilename);
                return result;
            } else {
                log.error("Failed to encrypt file with COMCRYPT. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error encrypting file with COMCRYPT: {}", e.getMessage(), e);
        }
        
        return null;
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
                    log.info("Detected and decoded Base64 content before COMCRYPT encryption. Original size: {} bytes, Decoded size: {} bytes", 
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
     * Decrypt a file using the COMCRYPT service
     * 
     * @param appCode Application code for COMCRYPT (e.g., OCMSLTA001)
     * @param encryptedContent Encrypted file content as byte array
     * @param token COMCRYPT token obtained from requestToken method
     * @return Decrypted file content as byte array if successful, null otherwise
     */
    public byte[] decryptFile(String decryptType, String requestId, byte[] encryptedContent, String token) {
        String appCode = requestId.split("_")[0];
        log.info("Decrypting file with COMCRYPT for appCode: {}, encrypted size: {} bytes", appCode, encryptedContent.length);
        
        try {
            // Get APIM subscription key from Key Vault
            String apimKey = keyVaultUtil.getSecret(comcryptSecretName);
            if (apimKey == null || apimKey.trim().isEmpty()) {
                log.error("Failed to retrieve APIM key from Key Vault for COMCRYPT decryption");
                return null;
            }
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set(comcryptApimHeader, apimKey);
            
            // Create multipart request
            LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("token", token);
            
            // Add file part
            ByteArrayResource fileResource = new ByteArrayResource(encryptedContent) {
                @Override
                public String getFilename() {
                    return "encrypted_file.p7";
                }
            };
            parts.add("file", fileResource);
            
            // Create HTTP entity with multipart content
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
            
            // Combine base URL with path based on encryption type
            String decryptPath;
            if (SystemConstant.CryptOperation.SLIFT.equalsIgnoreCase(decryptType)) {
                decryptPath = sliftDecryptPath;
            } else if (SystemConstant.CryptOperation.PGP.equalsIgnoreCase(decryptType)) {
                decryptPath = pgpDecryptPath;
            } else {
                // Default to SLIFT for backward compatibility
                log.warn("Unknown encryption type '{}', defaulting to SLIFT", decryptType);
                decryptPath = sliftDecryptPath;
            }
            
            String fullUrl = apimBaseUrl + decryptPath.replace("{appcode}", appCode);
            log.debug("Full COMCRYPT decrypt URL: {}, encryption type: {}", fullUrl, decryptType);
            
            // Make REST call
            ResponseEntity<byte[]> response = restTemplate.exchange(
                fullUrl != null ? fullUrl : "",
                HttpMethod.POST,
                entity,
                byte[].class
            );
            
            // Process response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                // Check Content-Type to detect JSON error response vs binary decrypted content
                MediaType contentType = response.getHeaders().getContentType();
                log.info("Response Content-Type: {}", contentType);

                // Determine expected Content-Type based on decryption type
                // PGP: expects application/octet-stream or binary
                // SLIFT: expects application/octet-stream or binary
                boolean isJsonResponse = contentType != null && contentType.includes(MediaType.APPLICATION_JSON);
                boolean isBinaryResponse = contentType != null && (
                    contentType.includes(MediaType.APPLICATION_OCTET_STREAM) ||
                    contentType.toString().contains("binary") ||
                    contentType.toString().contains("text/plain")
                );

                // If Content-Type is JSON, this is an error response, not binary decrypted data
                if (isJsonResponse) {
                    String jsonError = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                    log.error("APIM returned JSON error response instead of binary decrypted data");
                    log.error("Decryption type: {}, Expected: binary/octet-stream, Actual: {}", decryptType, contentType);
                    log.error("JSON error response: {}", jsonError);

                    // Parse JSON to extract responseCode
                    Map<String, Object> errorResult = handleJsonErrorResponse(jsonError, requestId,
                            SystemConstant.CryptOperation.DECRYPTION, null);
                    if (errorResult != null && "retry".equals(errorResult.get("action"))) {
                        // Retry the operation
                        log.info("[COMCRYPT-DECRYPT] Retrying decryption for requestId: {}", requestId);
                        return decryptFile(decryptType, requestId, encryptedContent, token);
                    }
                    return null;
                }

                // Log success for binary response
                if (isBinaryResponse) {
                    log.info("Received expected binary response for {} decryption", decryptType);
                } else {
                    log.warn("Unexpected Content-Type: {} for {} decryption, but proceeding as it's not JSON", contentType, decryptType);
                }

                byte[] decryptedBytes = response.getBody();
                if (decryptedBytes != null) {
                    log.info("Successfully decrypted file. Decrypted size: {} bytes", decryptedBytes.length);
                } else {
                    log.warn("Decrypted file content is null");
                }
                return decryptedBytes;
            } else {
                log.error("Failed to decrypt file with COMCRYPT. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error decrypting file with COMCRYPT: {}", e.getMessage(), e);
        }

        return null;
    }
    
    /**
     * Convenience method to perform the complete COMCRYPT encryption workflow
     * 
     * @param fileContent File content to encrypt
     * @param baseFileName Optional base filename to use (required for LTA enquiry)
     * @return Map containing encrypted file content and filename with .p7 extension
     */
    public Map<String, Object> encryptFileWithComcrypt(String encryptType, String appCode, byte[] fileContent, String baseFileName) {
        return encryptFileWithComcrypt(encryptType, appCode, fileContent, baseFileName, false);
    }
    
    /**
     * Convenience method to perform the complete COMCRYPT encryption workflow
     * 
     * @param fileContent File content to encrypt
     * @param baseFileName Optional base filename to use (required for LTA enquiry)
     * @param isRetrieval Whether this is for LTA retrieval (true) or enquiry (false)
     * @return Map containing encrypted file content and filename with .p7 extension
     */
    public Map<String, Object> encryptFileWithComcrypt(String encryptType, String appCode, byte[] fileContent, String baseFileName, boolean isRetrieval) {
        try {            
            // Generate a unique request ID
            String requestId = appCode + "_" + System.currentTimeMillis();
            
            // Request token (async, will be received via callback)
            boolean tokenRequested = requestToken(appCode, requestId);
            if (!tokenRequested) {
                log.error("Failed to request COMCRYPT token");
                return null;
            }
            
            // Wait for token to be received via callback (max 30 seconds, check every 500ms)
            // Multi-server compatible: Pass requestId to waitForToken
            String token = waitForToken(30000, 500, requestId);
            if (token == null) {
                log.error("Failed to receive COMCRYPT token via callback for requestId: {}", requestId);
                return null;
            }
            
            // Encrypt file - pass the requestId and baseFileName to ensure correct filename generation
            Map<String, Object> result = encryptFile(encryptType, requestId, fileContent, token, baseFileName);
            
            // Multi-server compatible: Cleanup operation record after successful encryption
            if (result != null) {
                operationHelper.deleteOperationByRequestId(requestId);
                log.info("Cleaned up operation record after successful encryption: {}", requestId);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error in COMCRYPT encryption workflow: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convenience method to perform the complete COMCRYPT decryption workflow
     * 
     * @param encryptedContent Encrypted content to decrypt
     * @return Decrypted file content if successful, null otherwise
     */
    public byte[] decryptFileWithComcrypt(String decryptType, String appCode, byte[] encryptedContent) {
        return decryptFileWithComcrypt(decryptType, appCode, encryptedContent, false);
    }
    
    /**
     * Convenience method to perform the complete COMCRYPT decryption workflow
     * 
     * @param encryptedContent Encrypted content to decrypt
     * @param isRetrieval Whether this is for LTA retrieval (true) or enquiry (false)
     * @return Decrypted file content if successful, null otherwise
     */
    public byte[] decryptFileWithComcrypt(String decryptType, String appCode, byte[] encryptedContent, boolean isRetrieval) {
        try {
            // Generate a unique request ID
            String requestId = appCode + "_" + System.currentTimeMillis();
            
            // Request token (async, will be received via callback)
            boolean tokenRequested = requestToken(appCode, requestId);
            if (!tokenRequested) {
                log.error("Failed to request COMCRYPT token");
                return null;
            }
            
            // Wait for token to be received via callback (max 30 seconds, check every 500ms)
            // Multi-server compatible: Pass requestId to waitForToken
            String token = waitForToken(30000, 500, requestId);
            if (token == null) {
                log.error("Failed to receive COMCRYPT token via callback for requestId: {}", requestId);
                return null;
            }
            
            // Decrypt file
            byte[] result = decryptFile(decryptType, requestId, encryptedContent, token);
            
            // Multi-server compatible: Cleanup operation record after successful decryption
            if (result != null) {
                operationHelper.deleteOperationByRequestId(requestId);
                log.info("Cleaned up operation record after successful decryption: {}", requestId);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error in COMCRYPT decryption workflow: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Handle JSON error response from COMCRYPT API.
     * Parses the response to extract responseCode (HC200, HC500, HC408) and handles accordingly.
     *
     * @param jsonError     The JSON error response string
     * @param requestId     The request ID for tracking
     * @param operationType The type of operation (ENCRYPTION or DECRYPTION)
     * @param fileName      The file name being processed (can be null)
     * @return Map with "action" key: "retry" if should retry, null otherwise
     */
    private Map<String, Object> handleJsonErrorResponse(String jsonError, String requestId,
                                                         String operationType, String fileName) {
        try {
            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(jsonError);
            String responseCode = jsonNode.has("responseCode") ? jsonNode.get("responseCode").asText() : null;
            String description = jsonNode.has("description") ? jsonNode.get("description").asText() : jsonError;

            log.info("[COMCRYPT-ERROR] Parsed error response - responseCode: {}, description: {}",
                    responseCode, description);

            // Check if this is HC200 (success but returned as JSON - unusual case)
            if (ComcryptNotificationHelper.RESPONSE_CODE_SUCCESS.equals(responseCode)) {
                log.warn("[COMCRYPT-ERROR] Received HC200 in JSON response, which is unexpected. Treating as error.");
                // Clear retry tracker and return null (no retry)
                retryTracker.remove(requestId);
                return null;
            }

            // Check if this is a retryable error (HC500 or HC408)
            if (notificationHelper.isErrorResponseCode(responseCode)) {
                int currentRetry = retryTracker.getOrDefault(requestId, 0);

                if (currentRetry < maxRetryAttempts) {
                    // Increment retry counter
                    retryTracker.put(requestId, currentRetry + 1);
                    log.info("[COMCRYPT-ERROR] Retry {} of {} for requestId: {} (responseCode: {})",
                            currentRetry + 1, maxRetryAttempts, requestId, responseCode);

                    // Return action to retry
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "retry");
                    result.put("retryAttempt", currentRetry + 1);
                    return result;
                } else {
                    // Max retries exceeded - send notification
                    log.error("[COMCRYPT-ERROR] Max retries ({}) exceeded for requestId: {} with responseCode: {}",
                            maxRetryAttempts, requestId, responseCode);

                    // Send email notification
                    notificationHelper.sendComcryptErrorNotification(
                            requestId,
                            responseCode,
                            description,
                            operationType,
                            fileName,
                            currentRetry
                    );

                    // Clear retry tracker
                    retryTracker.remove(requestId);
                    return null;
                }
            } else {
                // Unknown or other error code - log and don't retry
                log.error("[COMCRYPT-ERROR] Non-retryable error code: {} for requestId: {}", responseCode, requestId);

                // Send notification for non-retryable errors too
                notificationHelper.sendComcryptErrorNotification(
                        requestId,
                        responseCode != null ? responseCode : "UNKNOWN",
                        description,
                        operationType,
                        fileName,
                        0
                );

                retryTracker.remove(requestId);
                return null;
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-ERROR] Failed to parse JSON error response: {}", e.getMessage(), e);
            // On parse failure, send notification with raw error
            notificationHelper.sendComcryptErrorNotification(
                    requestId,
                    "PARSE_ERROR",
                    jsonError,
                    operationType,
                    fileName,
                    0
            );
            retryTracker.remove(requestId);
            return null;
        }
    }

    /**
     * Clear retry tracker for a specific requestId.
     * Should be called after successful encryption/decryption.
     *
     * @param requestId The request ID to clear
     */
    public void clearRetryTracker(String requestId) {
        if (requestId != null) {
            retryTracker.remove(requestId);
        }
    }
}
