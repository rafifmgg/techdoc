package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers.DataSetupHelper;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers.EndpointHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCMS 15 Test Setup Service
 *
 * Provides API-based setup/cleanup operations so tests can be triggered from Staff Portal.
 *
 * Workflow:
 * 1. Setup: POST /v1/ocms15-test-setup
 *    - Creates test notices via /staff-create-notice API
 *    - Resets test data to known initial state
 *
 * 2. Test: POST /v1/ocms15-test
 *    - Runs 4-step integration test
 *
 * 3. Cleanup: DELETE /v1/ocms15-test-cleanup
 *    - Deletes test notices from VON, ONOD, change processing tables
 *    - Removes all test-related data
 */
@Slf4j
@Service
public class Ocms15TestSetupService {

    @Autowired
    private DataSetupHelper dataSetupHelper;

    @Autowired
    private EndpointHelper endpointHelper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Setup test data - Creates test notices via /staff-create-notice API
     *
     * What this does:
     * 1. Load test scenarios from scenario.json
     * 2. Create test notices via /staff-create-notice API
     * 3. Clear any existing change processing records for these notices
     *
     * @return Map with setup results
     */
    public Map<String, Object> setupTestData() {
        log.info("Starting OCMS 15 test data setup");

        try {
            // 1. Load scenarios from scenario.json
            ClassPathResource resource = new ClassPathResource("testing/ocms15_changeprocessing/data/scenario.json");
            InputStream inputStream = resource.getInputStream();

            List<Map<String, Object>> scenarios = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            log.info("Loaded {} scenarios from scenario.json", scenarios.size());

            // 2. First, cleanup any existing test data to ensure clean slate
            List<String> testNoticeNumbers = getTestNoticeNumbers();
            Map<String, Object> cleanupResult = dataSetupHelper.cleanupTestNotices(testNoticeNumbers);
            log.info("Pre-setup cleanup: deleted {} notices", cleanupResult.get("totalDeleted"));

            // 3. Create test notices via /staff-create-notice API
            Map<String, Object> createResult = dataSetupHelper.createTestNotices(scenarios);

            if (!Boolean.TRUE.equals(createResult.get("success"))) {
                log.error("Failed to create test notices: {}", createResult.get("errors"));
            }

            // 4. Build response
            Map<String, Object> result = new HashMap<>();
            result.put("status", Boolean.TRUE.equals(createResult.get("success")) ? "success" : "partial");
            result.put("message", "Test data setup completed");
            result.put("noticesCreated", createResult.get("totalCreated"));
            result.put("noticesFailed", createResult.get("totalFailed"));
            result.put("totalScenarios", scenarios.size());
            result.put("createdNotices", createResult.get("createdNotices"));
            result.put("failedNotices", createResult.get("failedNotices"));

            if (createResult.containsKey("errors")) {
                result.put("errors", createResult.get("errors"));
            }

            log.info("Test data setup completed: {} created, {} failed",
                createResult.get("totalCreated"), createResult.get("totalFailed"));

            return result;

        } catch (Exception e) {
            log.error("Test data setup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Setup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cleanup test data - Removes all test notices and related records
     *
     * What this does:
     * 1. Delete change processing records for test notices
     * 2. Delete ONOD records for test notices
     * 3. Delete VON records for test notices
     *
     * @return Map with cleanup results
     */
    public Map<String, Object> cleanupTestData() {
        log.info("Starting OCMS 15 test data cleanup");

        try {
            List<String> testNotices = getTestNoticeNumbers();

            // Delete all test notices
            Map<String, Object> cleanupResult = dataSetupHelper.cleanupTestNotices(testNotices);

            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("status", cleanupResult.get("success"));
            result.put("message", "Test data cleanup completed");
            result.put("noticesDeleted", cleanupResult.get("totalDeleted"));
            result.put("noticesFailed", cleanupResult.get("totalFailed"));
            result.put("deletedNotices", cleanupResult.get("deletedNotices"));
            result.put("failedNotices", cleanupResult.get("failedNotices"));

            log.info("Test data cleanup completed: {} deleted, {} failed",
                cleanupResult.get("totalDeleted"), cleanupResult.get("totalFailed"));

            return result;

        } catch (Exception e) {
            log.error("Test data cleanup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of test notice numbers from scenario.json
     *
     * This should match the notice numbers in scenario.json
     *
     * @return List of test notice numbers
     */
    private List<String> getTestNoticeNumbers() {
        return List.of(
            "T41000001X",  // OCMS: ROV → RD1
            "T41000002X",  // OCMS: RD1 → RD2
            "T41000003X",  // OCMS: RD2 → DN1
            "T41000004X",  // OCMS: DN1 → DN2
            "T41000005X",  // OCMS: DN2 → WOF
            "T41000006X",  // PLUS: ROV → RD1
            "T41000007X",  // PLUS: RD1 → RD2
            "T41000008X",  // OCMS: ENA → RD1 (rejection)
            "T41000009X",  // PLUS: ROV → DN1 (rejection)
            "T41000010X"   // OCMS: ROV → ROV (rejection)
        );
    }

    /**
     * Get count of existing test notices
     *
     * @return Count of test notices currently in database
     */
    public long getTestNoticeCount() {
        return dataSetupHelper.getTestNoticeCount();
    }
}
