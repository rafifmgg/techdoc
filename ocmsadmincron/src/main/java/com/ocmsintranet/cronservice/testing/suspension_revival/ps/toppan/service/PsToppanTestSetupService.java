package com.ocmsintranet.cronservice.testing.suspension_revival.ps.toppan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.cronservice.testing.suspension_revival.helpers.TestDataHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PsToppanTestSetupService {

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/ps_toppan_scenarios.json";

    public Map<String, Object> setupTestData() {
        log.info("Starting PS-Toppan test data setup");

        int noticesCreated = 0;

        try {
            List<Map<String, Object>> scenarios = loadScenarios();

            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                testDataHelper.cleanupByNoticeNo(noticeNo);
            }

            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String vehicleNo = (String) scenario.get("vehicleNo");
                String nric = (String) scenario.get("nric");

                testDataHelper.createTestValidOffenceNotice(noticeNo, vehicleNo, nric, null);
                noticesCreated++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("scenarios_loaded", scenarios.size());

            log.info("PS-Toppan test data setup completed: {} notices", noticesCreated);

            return result;

        } catch (Exception e) {
            log.error("PS-Toppan test data setup failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Setup failed: " + e.getMessage());
            return errorResult;
        }
    }

    public Map<String, Object> cleanupTestData() {
        log.info("Starting PS-Toppan test data cleanup");

        try {
            Map<String, Integer> deletedCounts = testDataHelper.cleanupTestData();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data cleanup completed");
            result.putAll(deletedCounts);

            log.info("PS-Toppan test data cleanup completed: {}", deletedCounts);
            return result;

        } catch (Exception e) {
            log.error("PS-Toppan test data cleanup failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Cleanup failed: " + e.getMessage());
            return errorResult;
        }
    }

    private List<Map<String, Object>> loadScenarios() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCENARIO_FILE);
        if (inputStream == null) {
            throw new RuntimeException("Scenario file not found: " + SCENARIO_FILE);
        }
        return objectMapper.readValue(inputStream, List.class);
    }
}
