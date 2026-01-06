package com.ocmsintranet.apiservice.utilities.emailutility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import com.ocmsintranet.apiservice.utilities.AzureKeyVaultUtil;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private AzureKeyVaultUtil keyVaultUtil;
    
    @Value("${email.from.address}")
    private String defaultFromEmail;
    
    @Value("${email.from.name}")
    private String defaultFromName;
    
    @Value("${email.enableSmtp}")
    private boolean enableSmtp;
    
    @Value("${ocms.APIM.email.endpoint}")
    private String emailEndpointUrl;
    
    @Value("${ocms.APIM.subscriptionKey}")
    private String apimSubscriptionKeyHeader;
    
    // Adding retry configuration properties
    @Value("${email.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${email.retry.initialDelayMs:2000}")
    private long initialRetryDelayMs;
    
    @Value("${email.retry.maxDelayMs:30000}")
    private long maxRetryDelayMs;
    
    /**
     * Sends an HTML email with optional attachments using direct HTTP call
     * with built-in retry capability
     * 
     * @param request The email request containing all necessary information
     * @return true if email was sent successfully
     */
    public boolean sendEmail(EmailRequest request) {
        if (!enableSmtp) {
            log.warn("SMTP is disabled. Email to {} was not sent", request.getTo());
            return false;
        }
        
        // Set default from address if not provided
        if (request.getFrom() == null || request.getFrom().isEmpty()) {
            request.setFrom(defaultFromEmail);
            log.debug("Using default from address: {}", defaultFromEmail);
        }
        
        // Validate recipient
        if (request.getTo() == null || request.getTo().isEmpty()) {
            log.error("Email recipient is required");
            return false;
        }
        
        log.info("Preparing to send email to: {}", request.getTo());
        
        // Convert EmailRequest to API request format
        Map<String, Object> apiRequest = createApiRequest(request);
        
        // Use the full email endpoint URL directly instead of going through ApimUtil
        log.debug("Using full email endpoint URL: {} for email API call", emailEndpointUrl);
        
        try {
            // Create a RestTemplate to make the HTTP call directly
            RestTemplate restTemplate = new RestTemplate();
            
            // Get the subscription key from Azure Key Vault
            log.debug("Attempting to retrieve subscription key from Azure Key Vault");
            String subscriptionKey;
            try {
                subscriptionKey = keyVaultUtil.getSecret("Ocp-Apim-Subscription-Key");
                log.debug("Successfully retrieved subscription key");
            } catch (Exception e) {
                log.error("Failed to retrieve subscription key from Azure Key Vault: {}", e.getMessage(), e);
                // Fallback to using the value from properties file
                log.debug("Using fallback subscription key from properties");
                subscriptionKey = apimSubscriptionKeyHeader;
            }
            
            // Create headers with the subscription key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
            
            // Create the HTTP entity with headers and body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(apiRequest, headers);
            
            // Implement retry logic with exponential backoff
            int attempts = 0;
            long retryDelay = initialRetryDelayMs;
            Exception lastException = null;
            
            while (attempts < maxRetryAttempts) {
                try {
                    // Make the HTTP call
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            emailEndpointUrl,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                    
                    // Check if the call was successful
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Email sent successfully to: {}", request.getTo());
                        return true;
                    } else {
                        log.warn("Email API returned non-success status code: {}", response.getStatusCode());
                    }
                    
                    // If we got here, the call failed but didn't throw an exception
                    // We'll still retry
                } catch (Exception e) {
                    lastException = e;
                    log.warn("Attempt {} failed: {}", attempts + 1, e.getMessage());
                }
                
                // Increment attempts and calculate next retry delay
                attempts++;
                if (attempts < maxRetryAttempts) {
                    // Exponential backoff with jitter
                    retryDelay = Math.min(retryDelay * 2, maxRetryDelayMs);
                    long jitter = (long) (retryDelay * 0.2 * Math.random());
                    long actualDelay = retryDelay + jitter;
                    
                    log.info("Retrying in {} ms (attempt {}/{})", actualDelay, attempts + 1, maxRetryAttempts);
                    
                    try {
                        Thread.sleep(actualDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry sleep interrupted");
                        break;
                    }
                }
            }
            
            // If we got here, all attempts failed
            if (lastException != null) {
                log.error("Failed to send email after {} attempts: {}", maxRetryAttempts, lastException.getMessage());
            } else {
                log.error("Failed to send email after {} attempts", maxRetryAttempts);
            }
            
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Creates an API request from the EmailRequest
     * Formats the request according to the expected format by the dummy email server
     * Updated to use the new simplified format
     */
    private Map<String, Object> createApiRequest(EmailRequest request) {
        Map<String, Object> apiRequest = new HashMap<>();
        
        // Use the simplified format expected by the updated DummyBrevoController
        // Set the from email directly
        apiRequest.put("from", request.getFrom());
        
        // Set the to email directly
        apiRequest.put("to", request.getTo());
        
        // Add CC if provided
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            apiRequest.put("cc", request.getCc());
        }
        
        // Add subject and HTML content
        apiRequest.put("subject", request.getSubject());
        
        // Ensure HTML content is properly formatted for JSON
        String htmlContent = request.getHtmlContent();
        if (htmlContent != null) {
            // Remove any potentially problematic HTML constructs
            htmlContent = htmlContent.replaceAll("<!--[\\s\\S]*?-->", ""); // Remove HTML comments
            htmlContent = htmlContent.replaceAll("\\s+", " "); // Normalize whitespace
        }
        apiRequest.put("htmlContent", htmlContent);
        
        // Handle attachments if present - but limit size and complexity
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Map<String, Object>> attachments = new ArrayList<>();
            
            // Limit to 3 attachments to reduce complexity
            int attachmentCount = Math.min(request.getAttachments().size(), 3);
            for (int i = 0; i < attachmentCount; i++) {
                EmailRequest.Attachment attachment = request.getAttachments().get(i);
                Map<String, Object> attachmentMap = new HashMap<>();
                attachmentMap.put("fileName", attachment.getFileName());
                attachmentMap.put("fileContent", Base64.getEncoder().encodeToString(attachment.getFileContent()));
                attachments.add(attachmentMap);
            }
            
            apiRequest.put("attachments", attachments);
        }
        
        log.debug("Formatted email request with new simplified structure");
        return apiRequest;
    }
    

}
