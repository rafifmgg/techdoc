package com.ocmsintranet.cronservice.testing.datahive.controllers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveAuthTestService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveContactLookupFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveCompleteSuccessFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveFscOnlyFlowService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataHive Snowflake Testing Scenario Controller
 * Provides endpoints for testing DataHive Snowflake authentication and connectivity
 */
@Slf4j
@RestController
@RequestMapping("/test/datahive")
public class DataHiveScenarioController {

    @Value("${test.endpoints.enabled:true}")
    private boolean testEndpointsEnabled;

    private final DataHiveAuthTestService dataHiveAuthTestService;
    private final DataHiveContactLookupFlowService dataHiveContactLookupFlowService;
    private final DataHiveCompleteSuccessFlowService dataHiveCompleteSuccessFlowService;
    private final DataHiveFscOnlyFlowService dataHiveFscOnlyFlowService;

    @Autowired
    public DataHiveScenarioController(DataHiveAuthTestService dataHiveAuthTestService,
                                    DataHiveContactLookupFlowService dataHiveContactLookupFlowService,
                                    DataHiveCompleteSuccessFlowService dataHiveCompleteSuccessFlowService,
                                    DataHiveFscOnlyFlowService dataHiveFscOnlyFlowService) {
        this.dataHiveAuthTestService = dataHiveAuthTestService;
        this.dataHiveContactLookupFlowService = dataHiveContactLookupFlowService;
        this.dataHiveCompleteSuccessFlowService = dataHiveCompleteSuccessFlowService;
        this.dataHiveFscOnlyFlowService = dataHiveFscOnlyFlowService;
    }

    /**
     * Test Flow: DataHive Snowflake Authentication
     *
     * Input: DataHive configuration dan Azure Key Vault access
     * Process: JWT generation → APIM headers → Health check → Error validation
     * Verification:
     *   - JWT token generated successfully
     *   - Key Vault credentials accessible
     *   - DataHive connection established
     *   - Error scenarios handled gracefully
     */
    @PostMapping("/auth-test")
    public ResponseEntity<?> runDataHiveAuthTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        log.info("🧪 Starting DataHive Snowflake Authentication Test");

        try {
            List<TestStepResult> results = dataHiveAuthTestService.runTest();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive Authentication Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive Authentication Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Test execution failed: " + e.getMessage());
        }
    }

    /**
     * Test Flow: DataHive Service Health Check Only
     *
     * Input: None (uses default configuration)
     * Process: Simple health check ke DataHive endpoint
     * Verification:
     *   - Service is reachable
     *   - Authentication works
     *   - Simple query executes successfully
     */
    @PostMapping("/health-check")
    public ResponseEntity<?> runDataHiveHealthCheck() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        log.info("🏥 Starting DataHive Health Check");

        try {
            TestStepResult result = dataHiveAuthTestService.runHealthCheckOnly();

            Map<String, Object> response = new HashMap<>();
            response.put("healthCheck", result);
            response.put("isHealthy", "SUCCESS".equals(result.getStatus()));

            log.info("✅ DataHive Health Check completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive Health Check failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Health check failed: " + e.getMessage());
        }
    }

    /**
     * Test Flow: DataHive JWT Token Generation Only
     *
     * Input: None (uses configuration from properties)
     * Process: Generate JWT token menggunakan private key
     * Verification:
     *   - Private key accessible dari Azure Key Vault
     *   - JWT token generated successfully
     *   - Token format valid
     */
    @PostMapping("/jwt-test")
    public ResponseEntity<?> runJwtTokenTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        log.info("🔑 Starting JWT Token Generation Test");

        try {
            TestStepResult result = dataHiveAuthTestService.runJwtTestOnly();

            Map<String, Object> response = new HashMap<>();
            response.put("jwtTest", result);
            response.put("tokenGenerated", "SUCCESS".equals(result.getStatus()));

            log.info("✅ JWT Token Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ JWT Token Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("JWT test failed: " + e.getMessage());
        }
    }

    /**
     * Test Flow: DataHive Contact Lookup
     *
     * Input: Pre-defined test IDs (NRIC, FIN, Passport, Invalid ID)
     * Process: Setup test data → Execute contact lookups → Update database → Verify results → Cleanup
     * Verification:
     *   - Contact information successfully retrieved and stored
     *   - Database updated with correct audit trail
     *   - Error handling for invalid IDs
     *   - All test data properly cleaned up
     *
     * @return Response dengan test results dan summary statistics
     */
    @PostMapping("/contact-lookup-flow")
    public ResponseEntity<Map<String, Object>> runContactLookupFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🔍 Starting DataHive Contact Lookup Flow Test");

        try {
            List<TestStepResult> results = dataHiveContactLookupFlowService.runTest();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive Contact Lookup Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive Contact Lookup Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Complete NRIC Success Flow
     *
     * Input: NRIC S9107346C dengan complete MSF dan SPS records
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - All DataHive views return data untuk comprehensive citizen profile
     *   - ocms_dh_msf_comcare_fund: 2 records created (source=FSC, source=CCC)
     *   - ocms_dh_sps_custody: 1 record created dengan custody status
     *   - ocms_dh_sps_incarceration: 1 record created dengan combined release+offence data
     *   - Snowflake vs Database exact field match validation
     */
    @PostMapping("/complete-success-flow")
    public ResponseEntity<Map<String, Object>> runCompleteSuccessFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🚀 Starting DataHive Complete NRIC Success Flow Test");

        try {
            List<TestStepResult> results = dataHiveCompleteSuccessFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive Complete Success Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive Complete Success Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive FSC-Only Flow
     *
     * Input: NRIC S2345678B dengan FSC assistance record only
     * Process: Setup test data → Call DataHive API → Query Snowflake (FSC only) → Verify data match
     * Verification:
     *   - ocms_dh_msf_comcare_fund: 1 record created (source=FSC)
     *   - ocms_dh_sps_custody: No changes
     *   - ocms_dh_sps_incarceration: No changes
     *   - Field-by-field validation between Snowflake and Database
     */
    @PostMapping("/fsc-only-flow")
    public ResponseEntity<Map<String, Object>> runFscOnlyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🚀 Starting DataHive FSC-Only Flow Test");

        try {
            List<TestStepResult> results = dataHiveFscOnlyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive FSC-Only Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive FSC-Only Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate summary statistics dari test results
     *
     * @param results List hasil test steps
     * @return Map containing summary statistics
     */
    private Map<String, Object> calculateSummary(List<TestStepResult> results) {
        Map<String, Object> summary = new HashMap<>();

        long successCount = results.stream()
                .mapToLong(r -> "SUCCESS".equals(r.getStatus()) ? 1 : 0)
                .sum();

        long failedCount = results.stream()
                .mapToLong(r -> "FAILED".equals(r.getStatus()) ? 1 : 0)
                .sum();

        long skippedCount = results.stream()
                .mapToLong(r -> "SKIPPED".equals(r.getStatus()) ? 1 : 0)
                .sum();

        summary.put("totalSteps", results.size());
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("skippedCount", skippedCount);
        summary.put("overallStatus", failedCount > 0 ? "FAILED" : "SUCCESS");

        return summary;
    }


    
}
