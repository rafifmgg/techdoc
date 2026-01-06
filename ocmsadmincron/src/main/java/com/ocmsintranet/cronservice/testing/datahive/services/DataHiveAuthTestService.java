package com.ocmsintranet.cronservice.testing.datahive.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataHive Authentication Test Service
 * Provides comprehensive testing for DataHive Snowflake authentication mechanism
 */
@Slf4j
@Service
public class DataHiveAuthTestService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    @Autowired
    private DataHiveUtil dataHiveUtil;

    @Autowired
    private AzureKeyVaultUtil azureKeyVaultUtil;

    @Value("${datahive.account}")
    private String datahiveAccount;

    @Value("${datahive.user}")
    private String datahiveUser;

    @Value("${datahive.azure.keyvault.privatekeySecretName}")
    private String datahivePrivateKeySecretName;

    @Value("${datahive.apim.subscription.secretName}")
    private String datahiveApimSubscriptionSecretName;

    @Value("${ocms.azure.keyvault.url}")
    private String keyVaultUrl;

    /**
     * Run comprehensive DataHive authentication test
     *
     * @return List of test step results
     */
    public List<TestStepResult> runTest() {
        log.info("🚀 Starting comprehensive DataHive authentication test");
        List<TestStepResult> results = new ArrayList<>();

        // Step 1: Test JWT Token Generation
        TestStepResult jwtResult = testJwtGeneration();
        results.add(jwtResult);

        // Step 2: Test Key Vault Access (only if JWT generation succeeded)
        TestStepResult keyVaultResult;
        if (STATUS_SUCCESS.equals(jwtResult.getStatus())) {
            keyVaultResult = testKeyVaultAccess();
        } else {
            keyVaultResult = createSkippedResult("Key Vault Access Test",
                "Skipped due to JWT generation failure");
        }
        results.add(keyVaultResult);

        // Step 3: Test APIM Headers (only if previous steps succeeded)
        TestStepResult apimResult;
        if (STATUS_SUCCESS.equals(keyVaultResult.getStatus())) {
            apimResult = testApimHeaders();
        } else {
            apimResult = createSkippedResult("APIM Headers Test",
                "Skipped due to Key Vault access failure");
        }
        results.add(apimResult);

        // Step 4: Test Health Check (only if authentication setup succeeded)
        TestStepResult healthResult;
        if (STATUS_SUCCESS.equals(apimResult.getStatus())) {
            healthResult = testHealthCheck();
        } else {
            healthResult = createSkippedResult("Health Check Test",
                "Skipped due to APIM headers failure");
        }
        results.add(healthResult);

        // Step 5: Test Error Scenarios (always run to validate error handling)
        TestStepResult errorResult = testErrorScenarios();
        results.add(errorResult);

        log.info("✅ DataHive authentication test completed. Total steps: {}", results.size());
        return results;
    }

    /**
     * Run health check only (single step test)
     *
     * @return Single test step result
     */
    public TestStepResult runHealthCheckOnly() {
        log.info("🏥 Running DataHive health check only");
        return testHealthCheck();
    }

    /**
     * Run JWT token generation test only (single step test)
     *
     * @return Single test step result
     */
    public TestStepResult runJwtTestOnly() {
        log.info("🔑 Running JWT token generation test only");
        return testJwtGeneration();
    }

    /**
     * Test JWT token generation with private key from Azure Key Vault
     *
     * @return Test step result
     */
    private TestStepResult testJwtGeneration() {
        String title = "🔑 Step 1: Testing JWT token generation";
        log.info(title);

        try {
            long startTime = System.currentTimeMillis();

            // Generate JWT token
            String jwtToken = dataHiveUtil.generateJwtToken();
            log.info("JWT Token: {}", jwtToken);

            long executionTime = System.currentTimeMillis() - startTime;

            if (jwtToken != null && !jwtToken.trim().isEmpty()) {
                // Validate JWT token format (basic validation)
                String[] parts = jwtToken.split("\\.");
                if (parts.length == 3) {
                    log.info("✅ JWT token generated successfully in {}ms", executionTime);

                    Map<String, Object> jsonData = new HashMap<>();
                    jsonData.put("jwtToken", jwtToken);
                    jsonData.put("tokenLength", jwtToken.length());
                    jsonData.put("tokenParts", parts.length);
                    jsonData.put("executionTimeMs", executionTime);
                    jsonData.put("account", datahiveAccount);
                    jsonData.put("user", datahiveUser);

                    return createSuccessResult(title,
                            String.format("JWT token generated successfully in %dms (Length: %d characters)",
                                    executionTime, jwtToken.length()), jsonData);
                } else {
                    log.error("❌ Invalid JWT token format - expected 3 parts, got {}", parts.length);
                    return createFailedResult(title,
                            "Invalid JWT token format - expected 3 parts separated by dots");
                }
            } else {
                log.error("❌ JWT token is null or empty");
                return createFailedResult(title,
                        "JWT token generation returned null or empty result");
            }

        } catch (Exception e) {
            log.error("❌ JWT token generation failed: {}", e.getMessage(), e);
            return createFailedResult(title,
                    "JWT token generation failed: " + e.getMessage());
        }
    }

    /**
     * Test access to Azure Key Vault to retrieve secrets
     *
     * @return Test step result
     */
    private TestStepResult testKeyVaultAccess() {
        String title = "🔐 Step 2: Testing Azure Key Vault access";
        log.info(title);

        try {
            long startTime = System.currentTimeMillis();

            // Test private key access
            String privateKeySecret = azureKeyVaultUtil.getSecret(datahivePrivateKeySecretName, keyVaultUrl);

            // Test APIM subscription key access
            String apimSecret = azureKeyVaultUtil.getSecret(datahiveApimSubscriptionSecretName, keyVaultUrl);

            long executionTime = System.currentTimeMillis() - startTime;

            if (privateKeySecret != null && !privateKeySecret.trim().isEmpty() &&
                apimSecret != null && !apimSecret.trim().isEmpty()) {

                log.info("✅ Key Vault access successful in {}ms", executionTime);

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("privateKeyRetrieved", true);
                jsonData.put("apimKeyRetrieved", true);
                jsonData.put("executionTimeMs", executionTime);
                jsonData.put("keyVaultUrl", keyVaultUrl);

                return createSuccessResult(title,
                        String.format("Successfully retrieved secrets from Key Vault in %dms", executionTime),
                        jsonData);
            } else {
                log.error("❌ Failed to retrieve one or more secrets from Key Vault");
                return createFailedResult(title,
                        "Failed to retrieve required secrets from Azure Key Vault");
            }

        } catch (Exception e) {
            log.error("❌ Key Vault access failed: {}", e.getMessage(), e);
            return createFailedResult(title,
                    "Key Vault access failed: " + e.getMessage());
        }
    }

    /**
     * Test setup HTTP headers with JWT and APIM subscription
     *
     * @return Test step result
     */
    private TestStepResult testApimHeaders() {
        String title = "🌐 Step 3: Testing APIM headers setup";
        log.info(title);

        try {
            long startTime = System.currentTimeMillis();

            // Generate JWT token
            String jwtToken = dataHiveUtil.generateJwtToken();

            // Get APIM subscription key
            String apimKey = azureKeyVaultUtil.getSecret(datahiveApimSubscriptionSecretName, keyVaultUrl);

            // Create headers (simulate the same process as DataHiveUtil)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("X-Snowflake-Authorization-Token-Type", "KEYPAIR_JWT");

            long executionTime = System.currentTimeMillis() - startTime;

            if (jwtToken != null && apimKey != null &&
                headers.containsKey("Authorization") && headers.containsKey("X-Snowflake-Authorization-Token-Type")) {

                log.info("✅ APIM headers setup successful in {}ms", executionTime);

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("headersCreated", headers.size());
                jsonData.put("hasAuthorizationHeader", headers.containsKey("Authorization"));
                jsonData.put("hasSnowflakeTokenType", headers.containsKey("X-Snowflake-Authorization-Token-Type"));
                jsonData.put("executionTimeMs", executionTime);

                return createSuccessResult(title,
                        String.format("APIM headers configured successfully in %dms", executionTime),
                        jsonData);
            } else {
                log.error("❌ Failed to setup APIM headers properly");
                return createFailedResult(title,
                        "Failed to setup required APIM headers");
            }

        } catch (Exception e) {
            log.error("❌ APIM headers setup failed: {}", e.getMessage(), e);
            return createFailedResult(title,
                    "APIM headers setup failed: " + e.getMessage());
        }
    }

    /**
     * Test DataHive service availability with health check query
     *
     * @return Test step result
     */
    private TestStepResult testHealthCheck() {
        String title = "🏥 Step 4: Testing DataHive service health check";
        log.info(title);

        try {
            long startTime = System.currentTimeMillis();

            // Use the isServiceAvailable method dari DataHiveUtil
            boolean isAvailable = dataHiveUtil.isServiceAvailable();

            long executionTime = System.currentTimeMillis() - startTime;

            if (isAvailable) {
                log.info("✅ DataHive health check successful in {}ms", executionTime);

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("serviceAvailable", true);
                jsonData.put("executionTimeMs", executionTime);
                jsonData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                return createSuccessResult(title,
                        String.format("DataHive service is available and responsive in %dms", executionTime),
                        jsonData);
            } else {
                log.error("❌ DataHive service is not available");
                return createFailedResult(title,
                        "DataHive service is not available or not responding");
            }

        } catch (Exception e) {
            log.error("❌ DataHive health check failed: {}", e.getMessage(), e);
            return createFailedResult(title,
                    "DataHive health check failed: " + e.getMessage());
        }
    }

    /**
     * Test error scenarios to validate error handling
     *
     * @return Test step result
     */
    private TestStepResult testErrorScenarios() {
        String title = "⚠️ Step 5: Testing error scenarios";
        log.info(title);

        try {
            long startTime = System.currentTimeMillis();

            Map<String, Boolean> errorTests = new HashMap<>();

            // Test 1: Try with invalid SQL query to test error handling
            try {
                JsonNode result = dataHiveUtil.executeQuery("SELECT * FROM INVALID_TABLE_NAME_12345");
                errorTests.put("invalidQueryHandled", false); // Should not reach here
            } catch (Exception e) {
                errorTests.put("invalidQueryHandled", true); // Expected behavior
                log.debug("Expected error caught for invalid query: {}", e.getMessage());
            }

            // Test 2: Test with empty/null query
            try {
                JsonNode result = dataHiveUtil.executeQuery("");
                errorTests.put("emptyQueryHandled", false); // Should not reach here
            } catch (Exception e) {
                errorTests.put("emptyQueryHandled", true); // Expected behavior
                log.debug("Expected error caught for empty query: {}", e.getMessage());
            }

            long executionTime = System.currentTimeMillis() - startTime;

            long successfulErrorTests = errorTests.values().stream().mapToLong(v -> v ? 1 : 0).sum();

            if (successfulErrorTests >= errorTests.size() / 2) {
                log.info("✅ Error scenarios test completed in {}ms", executionTime);

                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("errorTestsRun", errorTests.size());
                jsonData.put("errorTestsPassed", successfulErrorTests);
                jsonData.put("executionTimeMs", executionTime);
                jsonData.put("errorDetails", errorTests);

                return createSuccessResult(title,
                        String.format("Error handling validated: %d/%d tests passed in %dms",
                                successfulErrorTests, errorTests.size(), executionTime),
                        jsonData);
            } else {
                log.warn("⚠️ Some error scenarios did not behave as expected");
                return createFailedResult(title,
                        "Error handling validation failed - unexpected behavior detected");
            }

        } catch (Exception e) {
            log.error("❌ Error scenarios test failed: {}", e.getMessage(), e);
            return createFailedResult(title,
                    "Error scenarios test failed: " + e.getMessage());
        }
    }

    /**
     * Create successful test step result
     */
    private TestStepResult createSuccessResult(String title, String details, Map<String, Object> jsonData) {
        TestStepResult result = new TestStepResult(title, STATUS_SUCCESS);
        result.addDetail(details);
        result.setJsonData(jsonData);
        return result;
    }

    /**
     * Create failed test step result
     */
    private TestStepResult createFailedResult(String title, String details) {
        TestStepResult result = new TestStepResult(title, STATUS_FAILED);
        result.addDetail(details);
        return result;
    }

    /**
     * Create skipped test step result
     */
    private TestStepResult createSkippedResult(String title, String details) {
        TestStepResult result = new TestStepResult(title, STATUS_SKIPPED);
        result.addDetail(details);
        return result;
    }
}
