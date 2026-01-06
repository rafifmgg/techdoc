package com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TS-ROV Test Setup/Cleanup Service
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Provides setup and cleanup operations for TS-ROV tests
 */
@Slf4j
@Service
public class TsRovTestSetupService {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/ts_rov_scenarios.json";

    /**
     * Setup test data - Creates/resets test notices to known state
     *
     * What this does:
     * 1. Load scenarios from JSON file
     * 2. Create test VON records for each scenario
     * 3. Create test LTA ROV file records
     * 4. Clear any existing suspensions for test notices
     *
     * @return Map with setup results
     */
    public Map<String, Object> setupTestData() {
        log.info("Starting TS-ROV test data setup");

        int noticesCreated = 0;
        int ltaFilesCreated = 0;
        int suspensionsDeleted = 0;

        try {
            // Load scenarios
            List<Map<String, Object>> scenarios = loadScenarios();

            // First, cleanup any existing data for these test notices
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                testDataHelper.cleanupByNoticeNo(noticeNo);
            }

            // Create test data for each scenario
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String vehicleNo = (String) scenario.get("vehicleNo");
                String nric = (String) scenario.get("nric");

                // Create test VON
                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);
                noticesCreated++;

                // Create test LTA ROV file
                testDataHelper.createTestLtaFile(vehicleNo, "ROV");
                ltaFilesCreated++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("lta_files_created", ltaFilesCreated);
            result.put("scenarios_loaded", scenarios.size());

            log.info("TS-ROV test data setup completed: {} notices, {} LTA files",
                    noticesCreated, ltaFilesCreated);

            return result;

        } catch (Exception e) {
            log.error("TS-ROV test data setup failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Setup failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Cleanup test data - Removes all test-related data
     *
     * What this does:
     * 1. Load scenario notice numbers
     * 2. Delete suspension records created by tests
     * 3. Delete test VON records
     * 4. Delete test LTA file records
     *
     * @return Map with cleanup results
     */
    public Map<String, Object> cleanupTestData() {
        log.info("Starting TS-ROV test data cleanup");

        try {
            // Cleanup all tracked test data
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data cleanup completed");
            result.putAll(deletedCounts);

            log.info("TS-ROV test data cleanup completed: {}", deletedCounts);

            return result;

        } catch (Exception e) {
            log.error("TS-ROV test data cleanup failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Cleanup failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Load scenarios from JSON file
     *
     * @return List of scenario maps
     */
    private List<Map<String, Object>> loadScenarios() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCENARIO_FILE);
        if (inputStream == null) {
            throw new RuntimeException("Scenario file not found: " + SCENARIO_FILE);
        }
        return objectMapper.readValue(inputStream, List.class);
    }
}
