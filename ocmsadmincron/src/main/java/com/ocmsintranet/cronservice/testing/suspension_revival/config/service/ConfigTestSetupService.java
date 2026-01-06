package com.ocmsintranet.cronservice.testing.suspension_revival.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConfigTestSetupService {

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SCENARIO_FILE = "testing/suspension_revival/data/config_scenarios.json";

    public Map<String, Object> setupTestData() {
        log.info("Starting Configuration test data setup");

        try {
            List<Map<String, Object>> scenarios = loadScenarios();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Configuration test setup completed (no data setup required)");
            result.put("scenarios_loaded", scenarios.size());

            log.info("Configuration test setup completed: {} scenarios loaded", scenarios.size());

            return result;

        } catch (Exception e) {
            log.error("Configuration test setup failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Setup failed: " + e.getMessage());
            return errorResult;
        }
    }

    public Map<String, Object> cleanupTestData() {
        log.info("Starting Configuration test data cleanup");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Configuration test cleanup completed (no cleanup required)");

        log.info("Configuration test cleanup completed");
        return result;
    }

    private List<Map<String, Object>> loadScenarios() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCENARIO_FILE);
        if (inputStream == null) {
            throw new RuntimeException("Scenario file not found: " + SCENARIO_FILE);
        }
        return objectMapper.readValue(inputStream, List.class);
    }
}
