package com.ocmsintranet.cronservice.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for making API calls with built-in retry capability
 */
@Slf4j
@Component
@org.springframework.context.annotation.Lazy
@RequiredArgsConstructor
public class ApimUtil {

    private final AzureKeyVaultUtil keyVaultUtil;

    @Value("${ocms.APIM.subscriptionKey}")
    private String apimSubscriptionKeyHeader;
    
    @Value("${ocms.APIM.secretName}")
    private String apimSubscriptionKeySecretName;
    
    @Value("${ocms.APIM.baseurl}")
    private String apimBaseUrl;
    
    @Value("${ocms.azure.keyvault.url}")
    private String keyVaultUrl;
    
    @Value("${ocms.api.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${ocms.api.retry.initialDelayMs:2000}")
    private long initialDelayMs;
    
    @Value("${ocms.api.retry.maxDelayMs:30000}")
    private long maxDelayMs;

    // Create WebClient with base URL
    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(apimBaseUrl)
                .build();
    }


    public <T> T callApimEndpoint(
            String path, 
            HttpMethod method,
            String apimSubscriptionKey,
            Object body,
            Map<String, String> queryParams,
            Class<T> responseType) {
        // Call the overloaded method with null JWT token
        return callApimEndpoint(path, method, apimSubscriptionKey, body, queryParams, responseType, null);
    }
    

    public <T> T callApimEndpoint(
            String path, 
            HttpMethod method,
            String apimSubscriptionKey,
            Object body,
            Map<String, String> queryParams,
            Class<T> responseType,
            String jwtToken) {
        
        // Start retry loop
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= maxRetryAttempts) {
            try {
                if (retryCount > 0) {
                    log.info("Retry attempt {} of {} for API call to {}", retryCount, maxRetryAttempts, path);
                } else {
                    log.info("Calling APIM endpoint: {} with path: {} and method: {}", apimBaseUrl, path, method);
                }
                
                // Create WebClient request spec
                WebClient.RequestBodySpec requestSpec = getWebClient()
                        .method(method)
                        .uri(uriBuilder -> {
                            uriBuilder = uriBuilder.path(path);
                            
                            // Add query parameters if provided
                            if (queryParams != null && !queryParams.isEmpty()) {
                                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                                    uriBuilder = uriBuilder.queryParam(entry.getKey(), entry.getValue());
                                }
                            }
                            
                            // Log the complete URL being constructed
                            String fullUrl = uriBuilder.build().toString();
                            log.info("Full URL being called: {}", fullUrl);
                            
                            return uriBuilder.build();
                        })
                        .header(apimSubscriptionKeyHeader, apimSubscriptionKey)
                        .contentType(MediaType.APPLICATION_JSON);
                
                // Add JWT token if provided
                if (jwtToken != null && !jwtToken.isEmpty()) {
                    log.info("Adding JWT token to request headers");
                    requestSpec = requestSpec.header("Authorization", "Bearer " + jwtToken);
                }
                
                // Add body if provided
                WebClient.RequestHeadersSpec<?> headersSpec;
                if (body != null) {
                    // Log the request body
                    try {
                        if (body instanceof String) {
                            log.info("Request body: {}", body);
                        } else {
                            ObjectMapper objectMapper = new ObjectMapper();
                            log.info("Request body: {}", objectMapper.writeValueAsString(body));
                        }
                    } catch (Exception e) {
                        log.warn("Could not log request body: {}", e.getMessage());
                    }
                    headersSpec = requestSpec.body(BodyInserters.fromValue(body));
                } else {
                    headersSpec = requestSpec;
                }
                
                // Execute the request and get response
                T response = headersSpec
                        .retrieve()
                        .bodyToMono(responseType)
                        .block();  // Block to get synchronous response
                
                log.info("APIM call completed successfully");
                return response;
                
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                if (retryCount <= maxRetryAttempts) {
                    // Calculate exponential backoff delay with jitter
                    long delayMs = calculateBackoffDelay(retryCount, initialDelayMs, maxDelayMs);
                    
                    log.warn("API call failed, retrying in {} ms. Retry {}/{}. Error: {}", 
                            delayMs, retryCount, maxRetryAttempts, e.getMessage());
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    // Check if it's a connection refused error and log more concisely
                    if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                        log.error("API call to {} failed after {} retries: Connection refused to {}", 
                                path, maxRetryAttempts, e.getMessage().replaceAll(".*Connection refused: ", ""));
                    } else {
                        log.error("API call to {} failed after {} retries: {}", path, maxRetryAttempts, e.getMessage());
                    }
                }
            }
        }
        
        // Create a more concise error message for connection refused errors
        String errorMessage = lastException.getMessage();
        if (errorMessage != null && errorMessage.contains("Connection refused")) {
            String endpoint = errorMessage.replaceAll(".*Connection refused: ", "");
            throw new RuntimeException("Failed to call APIM endpoint after " + maxRetryAttempts + " retries: Connection refused: " + endpoint);
        } else {
            throw new RuntimeException("Failed to call APIM endpoint after " + maxRetryAttempts + " retries: " + errorMessage, lastException);
        }
    }

    /**
     * Simplified method to call an APIM GET endpoint with subscription key
     * 
     * @param path The API path
     * @param responseType The expected response class type
     * @return The response body
     */
    public <T> T callApimGet(String path, Class<T> responseType) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.GET,
                apimSubscriptionKey,
                null,
                null,
                responseType);
    }

    /**
     * Call an APIM GET endpoint with subscription key and JWT token
     * 
     * @param path The API path
     * @param responseType The expected response class type
     * @param jwtToken JWT token for authentication
     * @return The response body
     */
    public <T> T callApimGet(String path, Class<T> responseType, String jwtToken) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.GET,
                apimSubscriptionKey,
                null,
                null,
                responseType,
                jwtToken);
    }

    /**
     * Call an APIM GET endpoint with subscription key and query parameters
     * 
     * @param path The API path
     * @param queryParams Query parameters as key-value pairs
     * @param responseType The expected response class type
     * @return The response body
     */
    public <T> T callApimGet(String path, Map<String, String> queryParams, Class<T> responseType) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.GET,
                apimSubscriptionKey,
                null,
                queryParams,
                responseType);
    }
    
    /**
     * Call an APIM GET endpoint with subscription key, query parameters, and JWT token
     *
     * @param path The API path
     * @param queryParams Query parameters as key-value pairs
     * @param responseType The expected response class type
     * @param jwtToken JWT token for authentication
     * @return The response body
     */
    public <T> T callApimGet(String path, Map<String, String> queryParams, Class<T> responseType, String jwtToken) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);

        return callApimEndpoint(
                path,
                HttpMethod.GET,
                apimSubscriptionKey,
                null,
                queryParams,
                responseType,
                jwtToken);
    }

    /**
     * Call an APIM GET endpoint with a custom subscription key secret name
     * This is useful for services like SMS that may use a different API subscription
     *
     * @param path The API path
     * @param queryParams Query parameters as key-value pairs (can be null)
     * @param responseType The expected response class type
     * @param customSecretName The custom secret name to retrieve the subscription key
     * @return The response body
     */
    public <T> T callApimGetCustomSecret(String path, Map<String, String> queryParams, Class<T> responseType, String customSecretName) {
        // Get subscription key from Azure Key Vault using the custom secret name with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(customSecretName, keyVaultUrl);

        return callApimEndpoint(
                path,
                HttpMethod.GET,
                apimSubscriptionKey,
                null,
                queryParams,
                responseType);
    }

    /**
     * Call an APIM POST endpoint with subscription key
     * 
     * @param path The API path
     * @param body The request body
     * @param responseType The expected response class type
     * @return The response body
     */
    public <T> T callApimPost(String path, Object body, Class<T> responseType) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.POST,
                apimSubscriptionKey,
                body,
                null,
                responseType);
    }
    
    /**
     * Call an APIM POST endpoint with a custom subscription key secret name
     * This is useful for services like SMS that may use a different API subscription
     * 
     * @param path The API path
     * @param body The request body
     * @param responseType The expected response class type
     * @param customSecretName The custom secret name to retrieve the subscription key
     * @return The response body
     */
    public <T> T callApimPostCustomSecret(String path, Object body, Class<T> responseType, String customSecretName) {
        // Get subscription key from Azure Key Vault using the custom secret name with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(customSecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.POST,
                apimSubscriptionKey,
                body,
                null,
                responseType);
    }
    
    /**
     * Call an APIM POST endpoint with subscription key and JWT token
     * 
     * @param path The API path
     * @param body The request body
     * @param responseType The expected response class type
     * @param jwtToken JWT token for authentication
     * @return The response body
     */
    public <T> T callApimPost(String path, Object body, Class<T> responseType, String jwtToken) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.POST,
                apimSubscriptionKey,
                body,
                null,
                responseType,
                jwtToken);
    }
    
    /**
     * Call an APIM PUT endpoint with subscription key
     * 
     * @param path The API path
     * @param body The request body
     * @param responseType The expected response class type
     * @return The response body
     */
    public <T> T callApimPut(String path, Object body, Class<T> responseType) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.PUT,
                apimSubscriptionKey,
                body,
                null,
                responseType);
    }
    
    /**
     * Call an APIM PUT endpoint with subscription key and JWT token
     * 
     * @param path The API path
     * @param body The request body
     * @param responseType The expected response class type
     * @param jwtToken JWT token for authentication
     * @return The response body
     */
    public <T> T callApimPut(String path, Object body, Class<T> responseType, String jwtToken) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.PUT,
                apimSubscriptionKey,
                body,
                null,
                responseType,
                jwtToken);
    }
    
    /**
     * Call an APIM DELETE endpoint with subscription key
     * 
     * @param path The API path
     * @param responseType The expected response class type
     * @return The response body
     */
    public <T> T callApimDelete(String path, Class<T> responseType) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.DELETE,
                apimSubscriptionKey,
                null,
                null,
                responseType);
    }
    
    /**
     * Call an APIM DELETE endpoint with subscription key and JWT token
     * 
     * @param path The API path
     * @param responseType The expected response class type
     * @param jwtToken JWT token for authentication
     * @return The response body
     */
    public <T> T callApimDelete(String path, Class<T> responseType, String jwtToken) {
        // Get subscription key from Azure Key Vault with explicit URL
        String apimSubscriptionKey = keyVaultUtil.getSecret(apimSubscriptionKeySecretName, keyVaultUrl);
        
        return callApimEndpoint(
                path, 
                HttpMethod.DELETE,
                apimSubscriptionKey,
                null,
                null,
                responseType,
                jwtToken);
    }
    
    /**
     * Calculate exponential backoff delay with jitter
     * 
     * @param retryCount Current retry attempt (starting from 1)
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay in milliseconds
     * @return The calculated delay in milliseconds
     */
    /**
     * Call an APIM POST endpoint with multipart file upload
     * 
     * @param path The API path
     * @param file The file to upload
     * @param customSecretName Custom secret name for subscription key
     * @return The response as JsonNode
     */
    public JsonNode callApimPostMultipart(String path, MultipartFile file, String customSecretName) {
        try {
            // Get subscription key from Azure Key Vault
            String apimSubscriptionKey = keyVaultUtil.getSecret(customSecretName, keyVaultUrl);
            
            log.info("Calling APIM multipart POST endpoint: {}", path);
            
            // Create multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            
            // Build the request with multipart content type
            WebClient.ResponseSpec responseSpec = getWebClient()
                    .post()
                    .uri(path)
                    .header(apimSubscriptionKeyHeader, apimSubscriptionKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve();
            
            // Get the response
            String responseBody = responseSpec
                    .bodyToMono(String.class)
                    .block();
            
            // Parse response to JsonNode
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(responseBody);
            
        } catch (Exception e) {
            log.error("Error calling APIM multipart endpoint {}: {}", path, e.getMessage(), e);
            throw new RuntimeException("Failed to call APIM multipart endpoint: " + e.getMessage(), e);
        }
    }
    
    private long calculateBackoffDelay(int retryCount, long initialDelayMs, long maxDelayMs) {
        // Calculate exponential backoff
        long expBackoff = initialDelayMs * (long)Math.pow(2, (double)(retryCount - 1));
        
        // Add jitter (between 0% and 15% of the calculated delay)
        double jitterPct = Math.random() * 0.15;
        long jitter = (long)(expBackoff * jitterPct);
        
        // Calculate total delay with jitter
        long delay = expBackoff + jitter;
        
        // Ensure delay doesn't exceed maximum
        return Math.min(delay, maxDelayMs);
    }
}
