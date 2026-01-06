package com.ocmsintranet.cronservice.dummyintegrations.mockslift;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock SLIFT Controller for local testing
 * 
 * This controller simulates the SLIFT service for local development and testing.
 * It handles token requests, encryption, and decryption operations without actual cryptography.
 * For encryption/decryption, it simply changes file extensions.
 */
@Slf4j
@RestController
@RequestMapping("/crypt")
@RequiredArgsConstructor
@Profile({"local", "sit"})
public class MockSliftController {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${slift.callback.delay-ms:3000}")
    private long callbackDelayMs;
    
    @Value("${ocms.APIM.baseurl:http://localhost:8083}")
    private String baseUrl;
    
    @Value("${server.servlet.context-path:/}")
    private String contextPath;
    
    @Value("${slift.callback.receiver.path:/crypt/token/receiver/v1}")
    private String callbackReceiverPath;
    
    @Value("${server.servlet.context-path:/}")
    private String configuredContextPath; // Keep the original context path from config
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * Mock endpoint for token request
     * 
     * @param payload The token request payload
     * @param apimKey The APIM subscription key
     * @return Response with success status
     */
    @PostMapping("/token/get/v1")
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Ocp-Apim-Subscription-Key") String apimKey) {
        
        log.info("Mock SLIFT: Received token request: {}", payload);
        
        // Extract request parameters
        String appCode = (String) payload.get("appCode");
        String requestId = (String) payload.get("requestId");
        
        // Validate request
        if (appCode == null || requestId == null) {
            log.error("Mock SLIFT: Invalid token request - missing appCode or requestId");
            return ResponseEntity.badRequest().body(Map.of(
                "responseCode", "HC400",
                "responseDesc", "Missing required parameters"
            ));
        }
        
        // Schedule callback after delay
        scheduler.schedule(() -> sendTokenCallback(appCode, requestId), callbackDelayMs, TimeUnit.MILLISECONDS);
        
        log.info("Mock SLIFT: Token request accepted, callback scheduled in {} ms", callbackDelayMs);
        
        // Return immediate success response
        return ResponseEntity.ok(Map.of(
            "responseCode", "HC200",
            "responseDesc", "Token request received, callback will be sent"
        ));
    }
    
    /**
     * Mock endpoint for file encryption
     * 
     * @param token The SLIFT token
     * @param file The file to be encrypted
     * @param apimKey The APIM subscription key
     * @return Response with "encrypted" file
     */
    @PostMapping(value = "/sliftencrypt/OCMSLTA001/v1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> encryptFile(
            @RequestPart("token") String token,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Ocp-Apim-Subscription-Key") String apimKey) {
        
        log.info("Mock SLIFT: Received encryption request");
        log.info("Mock SLIFT: Headers - Ocp-Apim-Subscription-Key: {}", apimKey);
        log.info("Mock SLIFT: Token: {}", token);
        log.info("Mock SLIFT: File present: {}", file != null);
        
        if (file != null) {
            log.info("Mock SLIFT: File name: {}", file.getOriginalFilename());
            log.info("Mock SLIFT: File empty: {}", file.isEmpty());
            log.info("Mock SLIFT: Content type: {}", file.getContentType());
        }
        
        try {
            // Validate request
            if (token == null || file == null || file.isEmpty()) {
                log.error("Mock SLIFT: Invalid encryption request - missing token or file");
                return ResponseEntity.badRequest().body("Missing required parameters");
            }
            
            // Get the file content
            byte[] fileContent = file.getBytes();
            log.info("Mock SLIFT: File size: {} bytes", fileContent.length);
            
            // For mock encryption, we don't actually encrypt the file,
            // just return it as-is (in a real scenario, this would be encrypted)
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "encrypted-file";
            
            // Log the original filename for debugging
            log.info("Mock SLIFT: Original filename: {}", filename);
            
            // Simply add .p7 extension to the original filename (whatever it is)
            // This preserves any existing extensions like .dat
            if (!filename.endsWith(".p7")) {
                filename += ".p7";
                log.info("Mock SLIFT: Added .p7 extension: {}", filename);
            }
            
            log.info("Mock SLIFT: File encryption completed successfully");
            log.info("Mock SLIFT: Returning file with name: {}", filename);
            
            // Return the "encrypted" file as binary data
            ResponseEntity<?> response = ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(fileContent);
            
            log.info("Mock SLIFT: Response headers: {}", response.getHeaders());
            return response;
            
        } catch (Exception e) {
            log.error("Mock SLIFT: Error processing encryption request", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Mock endpoint for file decryption
     * 
     * @param token The SLIFT token
     * @param encryptedFile The encrypted file
     * @param apimKey The APIM subscription key
     * @return The decrypted file as binary data
     */
    @PostMapping(value = "/sliftdecrypt/OCMSLTA001/v1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> decryptFile(
            @RequestPart("token") String token,
            @RequestPart("file") MultipartFile encryptedFile,
            @RequestHeader("Ocp-Apim-Subscription-Key") String apimKey) {
        
        log.info("Mock SLIFT: Received decryption request");
        log.info("Mock SLIFT: Headers - Ocp-Apim-Subscription-Key: {}", apimKey);
        log.info("Mock SLIFT: Token: {}", token);
        log.info("Mock SLIFT: File present: {}", encryptedFile != null);
        
        if (encryptedFile != null) {
            log.info("Mock SLIFT: File name: {}", encryptedFile.getOriginalFilename());
            log.info("Mock SLIFT: File empty: {}", encryptedFile.isEmpty());
            log.info("Mock SLIFT: Content type: {}", encryptedFile.getContentType());
        }
        
        try {
            // Validate request
            if (token == null || encryptedFile == null || encryptedFile.isEmpty()) {
                log.error("Mock SLIFT: Invalid decryption request - missing token or encryptedFile");
                return ResponseEntity.badRequest().body("Missing required parameters");
            }
            
            // Get the encrypted file content
            byte[] encryptedFileContent = encryptedFile.getBytes();
            log.info("Mock SLIFT: Encrypted file size: {} bytes", encryptedFileContent.length);
            
            // For mock decryption, we don't actually decrypt the file,
            // just return it as-is with the .p7 extension removed
            String filename = encryptedFile.getOriginalFilename() != null ? encryptedFile.getOriginalFilename() : "decrypted-file";
            log.info("Mock SLIFT: Original filename: {}", filename);
            
            // Remove the .p7 extension if present
            if (filename.endsWith(".p7")) {
                filename = filename.substring(0, filename.length() - 3);
                log.info("Mock SLIFT: Removed .p7 extension, new filename: {}", filename);
            }
            
            log.info("Mock SLIFT: File decryption completed successfully");
            log.info("Mock SLIFT: Returning file with name: {}", filename);
            
            // Return the "decrypted" file as binary data
            ResponseEntity<?> response = ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(encryptedFileContent);
            
            log.info("Mock SLIFT: Response headers: {}", response.getHeaders());
            return response;
            
        } catch (Exception e) {
            log.error("Mock SLIFT: Error processing decryption request", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Send token callback to the application
     * 
     * @param appCode The application code
     * @param requestId The original request ID
     */
    private void sendTokenCallback(String appCode, String requestId) {
        try {
            log.info("Mock SLIFT: Sending token callback for requestId: {}", requestId);
            
            // Generate a mock token
            String mockToken = "MOCK_TOKEN_" + System.currentTimeMillis();
            
            // Create callback payload
            ObjectNode callbackPayload = objectMapper.createObjectNode();
            callbackPayload.put("appCode", appCode);
            callbackPayload.put("requestId", requestId);
            callbackPayload.put("token", mockToken);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(callbackPayload), headers);
            
            // Use the callback receiver path directly from properties without manipulation
            String callbackUrl = baseUrl + callbackReceiverPath;
            
            log.info("Mock SLIFT: Sending token callback to: {}", callbackUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(callbackUrl, entity, Map.class);
            
            log.info("Mock SLIFT: Token callback sent successfully. Response: {}", response.getBody());
            
        } catch (Exception e) {
            log.error("Mock SLIFT: Error sending token callback", e);
        }
    }
}
