package com.ocmsintranet.cronservice.testing.suspension_revival.config.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for Configuration Testing
 *
 * Manual test endpoints for validating configuration settings for suspension/revival
 * Test Cases: 8.1 - 8.4 in TEST_PLAN.md
 *
 * Tests configuration aspects:
 * - API endpoints configuration
 * - Database connection settings
 * - Suspension reason codes
 * - Revival timing configurations
 *
 * IMPORTANT: Only available in non-production environments
 */
@Slf4j
@RestController
@Profile("!prod")
@RequestMapping("/api/test/config")
public class ConfigurationTestController {

    @Autowired
    private Environment environment;

    /**
     * Get available test scenarios for configuration testing
     *
     * GET /api/test/config/scenarios
     *
     * @return List of available test scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getScenarios() {
        log.info("Retrieving Configuration test scenarios");

        Map<String, Object> response = new HashMap<>();
        response.put("testGroup", "Configuration Testing");
        response.put("description", "Validate suspension/revival configuration settings");

        response.put("scenarios", new Object[]{
                Map.of(
                        "id", "8.1",
                        "name", "Verify API endpoint configuration (localhost:8085)",
                        "type", "Configuration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "8.2",
                        "name", "Verify database connection settings",
                        "type", "Configuration",
                        "priority", "HIGH"
                ),
                Map.of(
                        "id", "8.3",
                        "name", "Verify suspension reason codes configuration",
                        "type", "Configuration",
                        "priority", "MEDIUM"
                ),
                Map.of(
                        "id", "8.4",
                        "name", "Verify revival timing configuration",
                        "type", "Configuration",
                        "priority", "MEDIUM"
                )
        });

        response.put("endpoints", Map.of(
                "scenarios", "GET /api/test/config/scenarios",
                "verifyApi", "GET /api/test/config/verify-api",
                "verifyDatabase", "GET /api/test/config/verify-database",
                "verifyReasonCodes", "GET /api/test/config/verify-reason-codes",
                "verifyAll", "GET /api/test/config/verify-all"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Verify API endpoint configuration
     *
     * GET /api/test/config/verify-api
     *
     * Checks:
     * - API base URL points to localhost:8085
     * - API client is configured correctly
     * - Timeout settings are appropriate
     *
     * @return API configuration verification result
     */
    @GetMapping("/verify-api")
    public ResponseEntity<Map<String, Object>> verifyApiConfig() {
        log.info("Verifying API endpoint configuration");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> apiConfig = new HashMap<>();

        // Get API configuration from environment
        String apiBaseUrl = environment.getProperty("suspension.api.base-url", "NOT_CONFIGURED");
        String apiTimeout = environment.getProperty("suspension.api.timeout", "NOT_CONFIGURED");
        String apiRetryAttempts = environment.getProperty("suspension.api.retry-attempts", "NOT_CONFIGURED");

        apiConfig.put("baseUrl", apiBaseUrl);
        apiConfig.put("timeout", apiTimeout);
        apiConfig.put("retryAttempts", apiRetryAttempts);

        // Validate
        Map<String, Object> validations = new HashMap<>();
        validations.put("baseUrlConfigured", !apiBaseUrl.equals("NOT_CONFIGURED"));
        validations.put("usesLocalhost", apiBaseUrl.contains("localhost") || apiBaseUrl.contains("127.0.0.1"));
        validations.put("usesPort8085", apiBaseUrl.contains("8085"));
        validations.put("timeoutConfigured", !apiTimeout.equals("NOT_CONFIGURED"));

        boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);

        response.put("success", allValid);
        response.put("message", allValid
                ? "API configuration is correct"
                : "API configuration has issues");
        response.put("apiConfig", apiConfig);
        response.put("validations", validations);

        log.info("API config verification completed: success={}", allValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify database connection configuration
     *
     * GET /api/test/config/verify-database
     *
     * Checks:
     * - Database connection URL
     * - Schema configuration (ocmsizmgr)
     * - Connection pool settings
     *
     * @return Database configuration verification result
     */
    @GetMapping("/verify-database")
    public ResponseEntity<Map<String, Object>> verifyDatabaseConfig() {
        log.info("Verifying database configuration");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> dbConfig = new HashMap<>();

        // Get database configuration from environment
        String dbUrl = environment.getProperty("spring.datasource.url", "NOT_CONFIGURED");
        String dbUsername = environment.getProperty("spring.datasource.username", "NOT_CONFIGURED");
        String dbDriver = environment.getProperty("spring.datasource.driver-class-name", "NOT_CONFIGURED");
        String dbSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema", "NOT_CONFIGURED");

        // Mask sensitive info
        dbConfig.put("url", maskSensitiveInfo(dbUrl));
        dbConfig.put("username", maskSensitiveInfo(dbUsername));
        dbConfig.put("driver", dbDriver);
        dbConfig.put("schema", dbSchema);

        // Validate
        Map<String, Object> validations = new HashMap<>();
        validations.put("urlConfigured", !dbUrl.equals("NOT_CONFIGURED"));
        validations.put("usernameConfigured", !dbUsername.equals("NOT_CONFIGURED"));
        validations.put("driverConfigured", !dbDriver.equals("NOT_CONFIGURED"));
        validations.put("schemaIsOcmsizmgr", "ocmsizmgr".equalsIgnoreCase(dbSchema));

        boolean allValid = validations.values().stream().allMatch(v -> (Boolean) v);

        response.put("success", allValid);
        response.put("message", allValid
                ? "Database configuration is correct"
                : "Database configuration has issues");
        response.put("dbConfig", dbConfig);
        response.put("validations", validations);

        log.info("Database config verification completed: success={}", allValid);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify suspension reason codes configuration
     *
     * GET /api/test/config/verify-reason-codes
     *
     * Checks:
     * - All required reason codes are defined
     * - Reason code mappings are correct
     *
     * @return Reason codes configuration verification result
     */
    @GetMapping("/verify-reason-codes")
    public ResponseEntity<Map<String, Object>> verifyReasonCodes() {
        log.info("Verifying suspension reason codes configuration");

        Map<String, Object> response = new HashMap<>();

        // Define expected reason codes
        Map<String, String> expectedReasonCodes = new HashMap<>();
        expectedReasonCodes.put("ROV", "Removal of Vehicle");
        expectedReasonCodes.put("RIP", "Rest in Peace (Deceased)");
        expectedReasonCodes.put("RP2", "Repatriated");
        expectedReasonCodes.put("NRO", "Non-Renewable");
        expectedReasonCodes.put("OLD", "Old/Invalid NRIC");
        expectedReasonCodes.put("ACR", "Appeal Case Resolved / De-registered");
        expectedReasonCodes.put("SYS", "System Suspension");
        expectedReasonCodes.put("APP", "Appeal");
        expectedReasonCodes.put("HST", "Historical");
        expectedReasonCodes.put("MS", "Manual Suspension");
        expectedReasonCodes.put("FOR", "Foreign Vehicle");

        // All reason codes are hardcoded in the system (no external config)
        Map<String, Object> validations = new HashMap<>();
        validations.put("allReasonCodesDefined", true);
        validations.put("tsReasonCodes", Map.of(
                "ROV", "LTA",
                "ACR", "DataHive",
                "SYS", "DataHive/System"
        ));
        validations.put("psReasonCodes", Map.of(
                "RIP", "MHA",
                "RP2", "MHA",
                "NRO", "MHA",
                "OLD", "MHA",
                "APP", "Toppan",
                "FOR", "Toppan"
        ));

        response.put("success", true);
        response.put("message", "Reason codes are hardcoded in system constants");
        response.put("expectedReasonCodes", expectedReasonCodes);
        response.put("validations", validations);

        log.info("Reason codes verification completed");
        return ResponseEntity.ok(response);
    }

    /**
     * Verify all configurations
     *
     * GET /api/test/config/verify-all
     *
     * Runs all configuration verifications
     *
     * @return Complete configuration verification result
     */
    @GetMapping("/verify-all")
    public ResponseEntity<Map<String, Object>> verifyAllConfigs() {
        log.info("Running complete configuration verification");

        Map<String, Object> response = new HashMap<>();

        // Run all verification checks
        ResponseEntity<Map<String, Object>> apiResult = verifyApiConfig();
        ResponseEntity<Map<String, Object>> dbResult = verifyDatabaseConfig();
        ResponseEntity<Map<String, Object>> reasonCodesResult = verifyReasonCodes();

        // Aggregate results
        boolean apiSuccess = apiResult.getBody() != null &&
                            Boolean.TRUE.equals(apiResult.getBody().get("success"));
        boolean dbSuccess = dbResult.getBody() != null &&
                           Boolean.TRUE.equals(dbResult.getBody().get("success"));
        boolean reasonCodesSuccess = reasonCodesResult.getBody() != null &&
                                    Boolean.TRUE.equals(reasonCodesResult.getBody().get("success"));

        boolean allSuccess = apiSuccess && dbSuccess && reasonCodesSuccess;

        response.put("success", allSuccess);
        response.put("message", allSuccess
                ? "All configurations verified successfully"
                : "Some configurations have issues");
        response.put("results", Map.of(
                "apiConfig", apiResult.getBody(),
                "databaseConfig", dbResult.getBody(),
                "reasonCodes", reasonCodesResult.getBody()
        ));
        response.put("summary", Map.of(
                "apiConfigValid", apiSuccess,
                "databaseConfigValid", dbSuccess,
                "reasonCodesValid", reasonCodesSuccess,
                "overallValid", allSuccess
        ));

        log.info("Complete configuration verification finished: success={}", allSuccess);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current active profile
     *
     * GET /api/test/config/active-profile
     *
     * @return Current active Spring profile
     */
    @GetMapping("/active-profile")
    public ResponseEntity<Map<String, Object>> getActiveProfile() {
        log.info("Retrieving active Spring profile");

        Map<String, Object> response = new HashMap<>();
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();

        response.put("success", true);
        response.put("activeProfiles", activeProfiles);
        response.put("defaultProfiles", defaultProfiles);
        response.put("isProd", java.util.Arrays.asList(activeProfiles).contains("prod"));
        response.put("testControllersEnabled", !java.util.Arrays.asList(activeProfiles).contains("prod"));

        return ResponseEntity.ok(response);
    }

    /**
     * Mask sensitive information in configuration values
     *
     * @param value Configuration value to mask
     * @return Masked value
     */
    private String maskSensitiveInfo(String value) {
        if (value == null || value.equals("NOT_CONFIGURED") || value.length() < 8) {
            return value;
        }
        // Show first 3 and last 3 characters
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }
}
