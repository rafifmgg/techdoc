package com.ocmsintranet.cronservice.testing.suspension_revival.helpers;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Context for Suspension/Revival Tests
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Stores shared data between test steps:
 * - Step 1: Load scenarios → stores scenarios and identifiers
 * - Step 2: Trigger API → stores API results
 * - Step 3: Fetch data → stores verification data from database
 * - Step 4: Verify → uses all stored data for verification
 *
 * Scoped per request to ensure isolation between concurrent tests
 */
@Component
@RequestScope
public class SuspensionTestContext {

    // Step 1 data: Loaded scenarios
    private List<Map<String, Object>> scenarios;
    private List<String> noticeNumbers;

    // Step 2 data: API/Process execution results
    private Map<String, Object> executionResults;

    // Step 3 data: Verification data from database
    private List<Map<String, Object>> suspensionData;
    private List<Map<String, Object>> vonData;
    private List<Map<String, Object>> ltaFileData;
    private List<Map<String, Object>> mhaFileData;
    private List<Map<String, Object>> toppanFileData;
    private List<Map<String, Object>> dataHiveData;

    // General purpose data storage
    private Map<String, Object> customData;

    public SuspensionTestContext() {
        this.customData = new HashMap<>();
        this.executionResults = new HashMap<>();
    }

    // Step 1: Scenarios
    public List<Map<String, Object>> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<Map<String, Object>> scenarios) {
        this.scenarios = scenarios;
    }

    public List<String> getNoticeNumbers() {
        return noticeNumbers;
    }

    public void setNoticeNumbers(List<String> noticeNumbers) {
        this.noticeNumbers = noticeNumbers;
    }

    // Step 2: Execution results
    public Map<String, Object> getExecutionResults() {
        return executionResults;
    }

    public void setExecutionResults(Map<String, Object> executionResults) {
        this.executionResults = executionResults;
    }

    public void addExecutionResult(String key, Object value) {
        this.executionResults.put(key, value);
    }

    // Step 3: Verification data
    public List<Map<String, Object>> getSuspensionData() {
        return suspensionData;
    }

    public void setSuspensionData(List<Map<String, Object>> suspensionData) {
        this.suspensionData = suspensionData;
    }

    public List<Map<String, Object>> getVonData() {
        return vonData;
    }

    public void setVonData(List<Map<String, Object>> vonData) {
        this.vonData = vonData;
    }

    public List<Map<String, Object>> getLtaFileData() {
        return ltaFileData;
    }

    public void setLtaFileData(List<Map<String, Object>> ltaFileData) {
        this.ltaFileData = ltaFileData;
    }

    public List<Map<String, Object>> getMhaFileData() {
        return mhaFileData;
    }

    public void setMhaFileData(List<Map<String, Object>> mhaFileData) {
        this.mhaFileData = mhaFileData;
    }

    public List<Map<String, Object>> getToppanFileData() {
        return toppanFileData;
    }

    public void setToppanFileData(List<Map<String, Object>> toppanFileData) {
        this.toppanFileData = toppanFileData;
    }

    public List<Map<String, Object>> getDataHiveData() {
        return dataHiveData;
    }

    public void setDataHiveData(List<Map<String, Object>> dataHiveData) {
        this.dataHiveData = dataHiveData;
    }

    // Custom data storage
    public Object getCustomData(String key) {
        return customData.get(key);
    }

    public void setCustomData(String key, Object value) {
        this.customData.put(key, value);
    }

    /**
     * Clear all context data
     * Called after test execution completes
     */
    public void clear() {
        this.scenarios = null;
        this.noticeNumbers = null;
        this.executionResults.clear();
        this.suspensionData = null;
        this.vonData = null;
        this.ltaFileData = null;
        this.mhaFileData = null;
        this.toppanFileData = null;
        this.dataHiveData = null;
        this.customData.clear();
    }
}
