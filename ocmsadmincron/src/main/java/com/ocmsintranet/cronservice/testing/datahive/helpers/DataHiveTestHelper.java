package com.ocmsintranet.cronservice.testing.datahive.helpers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * DataHive Test Helper
 * Provides utility methods for DataHive testing operations
 */
@Slf4j
@Component
public class DataHiveTestHelper {

    /**
     * Validate JWT token format
     *
     * @param jwtToken JWT token to validate
     * @return true if format is valid (3 parts separated by dots)
     */
    public boolean isValidJwtFormat(String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            return false;
        }

        String[] parts = jwtToken.split("\\.");
        return parts.length == 3;
    }

    /**
     * Create test data map with common fields
     *
     * @param executionTime Execution time in milliseconds
     * @return Map with common test data fields
     */
    public Map<String, Object> createBaseTestData(long executionTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionTimeMs", executionTime);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * Mask sensitive data for logging purposes
     *
     * @param sensitiveData The sensitive data to mask
     * @return Masked version of the data
     */
    public String maskSensitiveData(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.length() <= 8) {
            return "***";
        }
        return sensitiveData.substring(0, 4) + "***" + sensitiveData.substring(sensitiveData.length() - 4);
    }

    /**
     * Calculate percentage of successful operations
     *
     * @param successful Number of successful operations
     * @param total Total number of operations
     * @return Success percentage
     */
    public double calculateSuccessPercentage(long successful, long total) {
        if (total == 0) {
            return 0.0;
        }
        return (double) successful / total * 100.0;
    }

    /**
     * Create test result with consistent format
     *
     * @param status Status test (SUCCESS, FAILED, SKIPPED)
     * @param title Judul step test
     * @param details Detail hasil test
     * @param jsonData Data tambahan dalam format JSON
     * @return TestStepResult object
     */
    public TestStepResult createTestResult(String status, String title, String details, Map<String, Object> jsonData) {
        TestStepResult result = new TestStepResult(title, status);
        result.addDetail(details);
        result.setJsonData(jsonData);
        return result;
    }

    /**
     * Create success test result dengan execution time
     *
     * @param title Judul step test
     * @param details Detail hasil test
     * @param executionTimeMs Waktu eksekusi dalam milliseconds
     * @return TestStepResult object dengan status SUCCESS
     */
    public TestStepResult createSuccessResult(String title, String details, long executionTimeMs) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("executionTimeMs", executionTimeMs);
        jsonData.put("timestamp", System.currentTimeMillis());
        
        return createTestResult("SUCCESS", title, details, jsonData);
    }

    /**
     * Create failed test result dengan error message
     *
     * @param title Judul step test
     * @param errorMessage Error message
     * @return TestStepResult object dengan status FAILED
     */
    public TestStepResult createFailedResult(String title, String errorMessage) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("error", errorMessage);
        jsonData.put("timestamp", System.currentTimeMillis());
        
        return createTestResult("FAILED", title, "Test failed: " + errorMessage, jsonData);
    }

    /**
     * Create skipped test result
     *
     * @param title Judul step test
     * @param reason Alasan skip
     * @return TestStepResult object dengan status SKIPPED
     */
    public TestStepResult createSkippedResult(String title, String reason) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("skipReason", reason);
        jsonData.put("timestamp", System.currentTimeMillis());
        
        return createTestResult("SKIPPED", title, "Skipped: " + reason, jsonData);
    }
}
