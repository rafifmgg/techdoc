package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Context - Stores shared data between test steps
 *
 * This context persists data across all 5 steps:
 * - Step 0: Stores created notice IDs
 * - Step 3: Stores fetched verification data
 * - Step 4: Uses stored data for verification
 */
@Component
public class TestContext {

    // Step 0: Created test notices
    private List<String> createdNoticeNumbers;

    // Step 3: Fetched data for verification
    private List<Map<String, Object>> vonData;
    private List<Map<String, Object>> onodData;
    private List<Map<String, Object>> changeProcessingData;
    private List<Map<String, Object>> parameterData;

    public TestContext() {
        this.createdNoticeNumbers = new ArrayList<>();
    }

    // Created Notices
    public List<String> getCreatedNoticeNumbers() {
        return createdNoticeNumbers;
    }

    public void setCreatedNoticeNumbers(List<String> createdNoticeNumbers) {
        this.createdNoticeNumbers = createdNoticeNumbers;
    }

    public void addCreatedNoticeNumber(String noticeNo) {
        if (!this.createdNoticeNumbers.contains(noticeNo)) {
            this.createdNoticeNumbers.add(noticeNo);
        }
    }

    // VON Data
    public List<Map<String, Object>> getVonData() {
        return vonData;
    }

    public void setVonData(List<Map<String, Object>> vonData) {
        this.vonData = vonData;
    }

    // ONOD Data
    public List<Map<String, Object>> getOnodData() {
        return onodData;
    }

    public void setOnodData(List<Map<String, Object>> onodData) {
        this.onodData = onodData;
    }

    // Change Processing Data
    public List<Map<String, Object>> getChangeProcessingData() {
        return changeProcessingData;
    }

    public void setChangeProcessingData(List<Map<String, Object>> changeProcessingData) {
        this.changeProcessingData = changeProcessingData;
    }

    // Parameter Data
    public List<Map<String, Object>> getParameterData() {
        return parameterData;
    }

    public void setParameterData(List<Map<String, Object>> parameterData) {
        this.parameterData = parameterData;
    }

    /**
     * Clear all context data
     */
    public void clear() {
        this.createdNoticeNumbers = new ArrayList<>();
        this.vonData = null;
        this.onodData = null;
        this.changeProcessingData = null;
        this.parameterData = null;
    }

    /**
     * Get VON record by notice number
     */
    public Map<String, Object> getVonByNoticeNo(String noticeNo) {
        if (vonData == null) return null;
        return vonData.stream()
            .filter(von -> noticeNo.equals(von.get("noticeNo")))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get ONOD record by notice number
     */
    public Map<String, Object> getOnodByNoticeNo(String noticeNo) {
        if (onodData == null) return null;
        return onodData.stream()
            .filter(onod -> noticeNo.equals(onod.get("noticeNo")))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get change processing records by notice number
     */
    public List<Map<String, Object>> getChangeProcessingByNoticeNo(String noticeNo) {
        if (changeProcessingData == null) return new ArrayList<>();
        return changeProcessingData.stream()
            .filter(cp -> noticeNo.equals(cp.get("noticeNo")))
            .toList();
    }
}
