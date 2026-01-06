package com.ocmsintranet.apiservice.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Utility class for HTTP headers and common HTTP operations
 */
@Component
public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    private static String apimSecretName;
    
    @Value("${ocms.APIM.secretName}")
    public void setApimSecretName(String secretName) {
        HttpUtil.apimSecretName = secretName;
        logger.info("APIM secret name set to: {}", apimSecretName);
    }
    
    /**
     * Creates HTTP headers for APIM requests with JWT authentication
     * 
     * @param jwtToken JWT token for authentication
     * @param apiKey APIM subscription key
     * @return HttpHeaders with proper authentication headers
     */
    public static HttpHeaders createAuthHeaders(String jwtToken, String apiKey) {
        logger.debug("Creating HTTP headers with JWT authentication");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set(apimSecretName, apiKey);
        
        return headers;
    }
    
    /**
     * Creates HTTP headers for GET requests with JWT authentication
     * 
     * @param jwtToken JWT token for authentication
     * @param apiKey APIM subscription key
     * @return HttpHeaders with proper authentication headers
     */
    public static HttpHeaders createGetHeaders(String jwtToken, String apiKey) {
        logger.debug("Creating HTTP headers for GET request with JWT authentication");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set(apimSecretName, apiKey);
        
        return headers;
    }
    
    /**
     * Creates HTTP headers for POST requests with JWT authentication
     * 
     * @param jwtToken JWT token for authentication
     * @param apiKey APIM subscription key
     * @return HttpHeaders with proper authentication headers
     */
    public static HttpHeaders createPostHeaders(String jwtToken, String apiKey) {
        logger.debug("Creating HTTP headers for POST request with JWT authentication");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set(apimSecretName, apiKey);
        
        return headers;
    }
    
    /**
     * Get the APIM secret name that headers should use
     * 
     * @return Current APIM secret name
     */
    public static String getApimSecretName() {
        return apimSecretName;
    }
}