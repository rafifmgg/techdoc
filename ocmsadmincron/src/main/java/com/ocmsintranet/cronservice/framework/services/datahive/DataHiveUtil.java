package com.ocmsintranet.cronservice.framework.services.datahive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// DataHive uses custom APIM integration with specific headers
import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for DataHive (Snowflake) operations through direct API calls
 */
@Component
public class DataHiveUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataHiveUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private final RestTemplate restTemplate;
    private final AzureKeyVaultUtil azureKeyVaultUtil;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    @Value("${datahive.account}")
    private String datahiveAccount;
    
    @Value("${datahive.user}")
    private String datahiveUser;
    
    @Value("${datahive.azure.keyvault.privatekeyName}")
    private String datahivePrivateKeyName;

    @Value("${datahive.azure.keyvault.privatekeySecretName}")
    private String datahivePrivateKeySecretName;
    
    @Value("${datahive.database:URA_OCMS_DEV}")
    private String defaultDatabase;
    
    @Value("${datahive.schema:PUBLIC}")
    private String defaultSchema;
    
    @Value("${datahive.warehouse:URA_OCMS_WH}")
    private String defaultWarehouse;
    
    @Value("${datahive.role:OCMS_SERVICE_ROLE}")
    private String defaultRole;
    
    @Value("${datahive.api.endpoint.format}")
    private String datahiveApiEndpointFormat;
    
    @Value("${datahive.api.health.endpoint}")
    private String datahiveHealthEndpoint;
    
    @Value("${datahive.apim.header.name}")
    private String datahiveApimHeaderName;
    
    @Value("${datahive.apim.subscription.secretName}")
    private String datahiveApimSubscriptionSecretName;
    
    @Value("${datahive.keyvault.url:${ocms.azure.keyvault.url}}")
    private String datahiveKeyVaultUrl;

    @Value("${ocms.azure.keyvault.url}")
    private String ocmsAzureKeyVaultUrl;
    
    @Value("${datahive.pollingIntervalMs:1000}")
    private long pollingIntervalMs;
    
    @Value("${datahive.maxPollingAttempts:60}")
    private int maxPollingAttempts;
    
    @Autowired
    public DataHiveUtil(RestTemplate restTemplate, AzureKeyVaultUtil azureKeyVaultUtil) {
        this.restTemplate = restTemplate;
        this.azureKeyVaultUtil = azureKeyVaultUtil;
    }
    
    /**
     * Get the Snowflake API base URL
     * 
     * @return Snowflake API base URL
     */
    private String getSnowflakeApiBaseUrl() {
        // Use the configured API endpoint format
        return String.format(datahiveApiEndpointFormat, datahiveAccount);
    }
    
    /**
     * Create HTTP headers with JWT authentication and DataHive-specific APIM headers
     * 
     * @param jwtToken JWT token
     * @return HTTP headers with complete authentication
     */
    private HttpHeaders createAuthHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Snowflake-Authorization-Token-Type", "KEYPAIR_JWT");
        headers.set("Authorization", "Bearer " + jwtToken);
        
        try {
            // Add DataHive-specific APIM subscription key using explicit Key Vault URL
            String apimSubscriptionKey = azureKeyVaultUtil.getSecret(datahiveApimSubscriptionSecretName, ocmsAzureKeyVaultUrl);
            headers.set(datahiveApimHeaderName, apimSubscriptionKey);
        } catch (Exception e) {
            logger.error("Failed to add APIM subscription key to headers: {}", e.getMessage(), e);
        }
        
        return headers;
    }
    
    /**
     * Generate JWT token for DataHive authentication
     * 
     * @return JWT token
     */
    public String generateJwtToken() {
        try {
            logger.debug("Generating JWT token for DataHive authentication");
            if (activeProfile.equals("local") || activeProfile.equals("sit")) {
                return AzureKeyVaultUtil.generateJWTWithPrivateKey(
                        datahiveAccount,
                        datahiveUser,
                        azureKeyVaultUtil,
                        datahivePrivateKeySecretName
                );
            } else {
                return AzureKeyVaultUtil.generateJWT(
                    datahiveAccount,
                    datahiveUser,
                    azureKeyVaultUtil,
                    datahivePrivateKeyName,
                    datahiveKeyVaultUrl
                );
            }
        } catch (Exception e) {
            logger.error("Failed to generate JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
    
    /**
     * Check if DataHive service is available
     * 
     * @return true if service is available
     */
    public boolean isServiceAvailable() {
        try {
            logger.debug("Checking DataHive service availability via APIM");
            
            // Generate JWT token for DataHive authentication
            String jwtToken = generateJwtToken();
            logger.info("Generated JWT token for DataHive health check : {}", jwtToken);            
            
            // For health check, we should use the statements endpoint directly
            String url = getSnowflakeApiBaseUrl() + datahiveHealthEndpoint;
            HttpHeaders headers = createAuthHeaders(jwtToken);
            logger.info("Checking Snowflake health at URL: {}", url);            
            
            // Prepare the request body for DataHive health check
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("statement", "SELECT TABLE_NAME FROM DATA_REQUESTS.INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'URA_2024_09_001'");
            requestBody.put("timeout", 6000);
            requestBody.put("database", defaultDatabase);
            requestBody.put("schema", defaultSchema);
            requestBody.put("warehouse", defaultWarehouse);
            requestBody.put("role", defaultRole);
            
            logger.info("DataHive health check request body: {}", objectMapper.writeValueAsString(requestBody));
                        
            // Create the HTTP entity with headers and body
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Make the API call through APIM
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // Check if the call was successful
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                logger.info("DataHive service health check successful. Response: {}", responseBody);
                return true;
            } else {
                logger.error("DataHive service health check failed with status: {}", responseEntity.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking DataHive service availability: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Execute SQL query synchronously with default database context
     * 
     * @param sql SQL query to execute
     * @return Query result as JsonNode
     */
    public JsonNode executeQuery(String sql) {
        return executeQuery(sql, defaultDatabase, defaultSchema, defaultWarehouse, defaultRole);
    }
    
    /**
     * Execute SQL query synchronously with custom database context
     * 
     * @param sql SQL query to execute
     * @param database Database name
     * @param schema Schema name
     * @param warehouse Warehouse name
     * @param role Role name
     * @return Query result as JsonNode
     */
    public JsonNode executeQuery(String sql, String database, String schema, String warehouse, String role) {
        try {
            logger.debug("Executing SQL query synchronously: {}", sql);
            
            // Generate JWT token once for the entire query execution process
            String jwtToken = generateJwtToken();
            String queryId = startQueryWithToken(sql, database, schema, warehouse, role, jwtToken);
            
            // Poll for results using the same token
            int attempts = 0;
            JsonNode result = null;
            
            while (attempts < maxPollingAttempts) {
                result = getQueryResultWithToken(queryId, jwtToken);
                JsonNode status = result.path("status");
                
                if (status != null && !status.asText().equals("running")) {
                    break;
                }
                
                attempts++;
                Thread.sleep(pollingIntervalMs);
            }
            
            if (attempts >= maxPollingAttempts) {
                throw new RuntimeException("Query execution timed out after " + maxPollingAttempts + " polling attempts");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to execute SQL query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute SQL query", e);
        }
    }
    
    /**
     * Execute SQL query asynchronously with default database context
     * 
     * @param sql SQL query to execute
     * @return CompletableFuture with query result as JsonNode
     */
    public CompletableFuture<JsonNode> executeQueryAsync(String sql) {
        return executeQueryAsync(sql, defaultDatabase, defaultSchema, defaultWarehouse, defaultRole);
    }
    
    /**
     * Execute SQL query asynchronously with default database context and return only the data array
     * 
     * @param sql SQL query to execute
     * @return CompletableFuture with just the data array from the query result
     */
    public CompletableFuture<JsonNode> executeQueryAsyncDataOnly(String sql) {
        return executeQueryAsyncDataOnly(sql, defaultDatabase, defaultSchema, defaultWarehouse, defaultRole);
    }
    
    /**
     * Execute SQL query asynchronously with custom database context
     * 
     * @param sql SQL query to execute
     * @param database Database name
     * @param schema Schema name
     * @param warehouse Warehouse name
     * @param role Role name
     * @return CompletableFuture with query result as JsonNode
     */
    public CompletableFuture<JsonNode> executeQueryAsync(String sql, String database, String schema, String warehouse, String role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting async SQL query execution: {}", sql);
                
                // Generate JWT token once for the entire query execution process
                String jwtToken = generateJwtToken();
                logger.debug("Generated JWT token for async query execution");
                
                // Start the query with the token
                String queryId = startQueryWithToken(sql, database, schema, warehouse, role, jwtToken);
                logger.info("Async query started with ID: {}", queryId);
                
                // Poll for results using the same token
                JsonNode result = null;
                int attempts = 0;
                
                while (attempts < maxPollingAttempts) {
                    logger.debug("Polling for query results (attempt {}/{})", attempts + 1, maxPollingAttempts);
                    JsonNode queryResult = getQueryResultWithToken(queryId, jwtToken);
                    JsonNode status = queryResult.path("status");
                    
                    logger.debug("Query status: {}", status.asText());
                    result = queryResult;
                    
                    if (status != null && !status.asText().equals("running")) {
                        logger.info("Async query completed with status: {}", status.asText());
                        break;
                    }
                    
                    attempts++;
                    Thread.sleep(pollingIntervalMs);
                }
                
                if (attempts >= maxPollingAttempts) {
                    logger.error("Async query execution timed out after {} polling attempts", maxPollingAttempts);
                    throw new RuntimeException("Query execution timed out after " + maxPollingAttempts + " polling attempts");
                }
                
                logger.info("Async query execution completed successfully");
                return result;
            } catch (Exception e) {
                logger.error("Async query execution failed: {}", e.getMessage(), e);
                throw new RuntimeException("Async query execution failed", e);
            }
        }, executorService);
    }
    
    /**
     * Execute SQL query asynchronously with custom database context and return only the data array
     * 
     * @param sql SQL query to execute
     * @param database Database name
     * @param schema Schema name
     * @param warehouse Warehouse name
     * @param role Role name
     * @return CompletableFuture with just the data array from the query result
     */
    public CompletableFuture<JsonNode> executeQueryAsyncDataOnly(String sql, String database, String schema, String warehouse, String role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting async SQL query execution (data only): {}", sql);
                
                // Generate JWT token once for the entire query execution process
                String jwtToken = generateJwtToken();
                logger.debug("Generated JWT token for async query execution");
                
                // Start the query with the token
                String queryId = startQueryWithToken(sql, database, schema, warehouse, role, jwtToken);
                logger.info("Async query started with ID: {}", queryId);
                
                // Poll for results using the same token
                JsonNode result = null;
                int attempts = 0;
                
                while (attempts < maxPollingAttempts) {
                    logger.debug("Polling for query results (attempt {}/{})", attempts + 1, maxPollingAttempts);
                    JsonNode queryResult = getQueryResultWithToken(queryId, jwtToken);
                    JsonNode status = queryResult.path("status");
                    
                    logger.debug("Query status: {}", status.asText());
                    result = queryResult;
                    
                    if (status != null && !status.asText().equals("running")) {
                        logger.info("Async query completed with status: {}", status.asText());
                        break;
                    }
                    
                    attempts++;
                    Thread.sleep(pollingIntervalMs);
                }
                
                if (attempts >= maxPollingAttempts) {
                    logger.error("Async query execution timed out after {} polling attempts", maxPollingAttempts);
                    throw new RuntimeException("Query execution timed out after " + maxPollingAttempts + " polling attempts");
                }
                
                logger.info("Async query execution completed successfully");
                
                // Extract just the data array from the result
                if (result != null && result.has("data")) {
                    JsonNode dataNode = result.get("data");
                    logger.info("Query result contains {} records", dataNode.isArray() ? dataNode.size() : 0);
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        logger.debug("First record from DataHive: {}", dataNode.get(0).toString());
                    }
                    return dataNode;
                } else {
                    logger.warn("Query result does not contain a data field. Full result: {}", 
                            result != null ? result.toString() : "null");
                    // Return an empty array if there's no data
                    return objectMapper.createArrayNode();
                }
            } catch (Exception e) {
                logger.error("Async query execution failed: {}", e.getMessage(), e);
                throw new RuntimeException("Async query execution failed", e);
            }
        }, executorService);
    }
    
    /**
     * Start a query execution with a provided JWT token and return the query ID
     * 
     * @param sql SQL query to execute
     * @param database Database name
     * @param schema Schema name
     * @param warehouse Warehouse name
     * @param role Role name
     * @param jwtToken JWT token to use for authentication
     * @return Query ID for polling results
     */
    private String startQueryWithToken(String sql, String database, String schema, String warehouse, String role, String jwtToken) {
        try {
            logger.debug("Starting SQL query execution: {}", sql);
            
            String url = getSnowflakeApiBaseUrl() + datahiveHealthEndpoint;
            HttpHeaders headers = createAuthHeaders(jwtToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("statement", sql);
            requestBody.put("database", database);
            requestBody.put("schema", schema);
            requestBody.put("warehouse", warehouse);
            requestBody.put("role", role);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            
            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String queryId = responseBody.get("statementHandle").asText();
            logger.debug("Query started with ID: {}", queryId);
            
            return queryId;
        } catch (Exception e) {
            logger.error("Failed to start query with token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start query with token", e);
        }
    }
    
    /**
     * Get query result by query ID using a provided JWT token
     * 
     * @param queryId Query ID
     * @param jwtToken JWT token to use for authentication
     * @return Query result as JsonNode
     */
    private JsonNode getQueryResultWithToken(String queryId, String jwtToken) {
        try {
            logger.debug("Getting query result for ID: {}", queryId);
            
            String url = getSnowflakeApiBaseUrl() + datahiveHealthEndpoint + "/" + queryId;
            HttpHeaders headers = createAuthHeaders(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to get query result with token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get query result with token", e);
        }
    }
    
    /**
     * Parse JSON string to JsonNode
     * 
     * @param json JSON string
     * @return JsonNode
     */
    public JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
    
    /**
     * Get default database
     * 
     * @return Default database name
     */
    public String getDefaultDatabase() {
        return defaultDatabase;
    }
    
    /**
     * Get default schema
     * 
     * @return Default schema name
     */
    public String getDefaultSchema() {
        return defaultSchema;
    }
    
    /**
     * Get default warehouse
     * 
     * @return Default warehouse name
     */
    public String getDefaultWarehouse() {
        return defaultWarehouse;
    }
    
    /**
     * Get default role
     * 
     * @return Default role name
     */
    public String getDefaultRole() {
        return defaultRole;
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        logger.info("Shutting down DataHive executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
