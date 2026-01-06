package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.OcmsChangeOfProcessing;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.OcmsChangeOfProcessingRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Data Setup Helper - Handles creation and cleanup of test data
 *
 * Responsibilities:
 * - Step 0: Create test notices via /staff-create-notice API
 * - Manual Cleanup: Delete test notices (VON, ONOD, change processing records)
 */
@Slf4j
@Component
public class DataSetupHelper {

    @Autowired
    private EndpointHelper endpointHelper;

    @Autowired
    private OcmsValidOffenceNoticeRepository vonRepository;

    @Autowired
    private OcmsOffenceNoticeOwnerDriverRepository onodRepository;

    @Autowired
    private OcmsChangeOfProcessingRepository changeProcessingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Step 0: Create test notices via /staff-create-notice API
     *
     * @param scenarios List of scenario maps containing OffenceNoticeDto data
     * @return Map with success flag, created notices, and errors
     */
    public Map<String, Object> createTestNotices(List<Map<String, Object>> scenarios) {
        Map<String, Object> result = new HashMap<>();
        List<String> createdNotices = new ArrayList<>();
        List<String> failedNotices = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        try {
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");

                try {
                    // Convert scenario map to JSON string
                    String noticeJson = objectMapper.writeValueAsString(scenario.get("noticeData"));

                    // Call /staff-create-notice
                    Map<String, Object> apiResult = endpointHelper.createNotice(noticeJson);

                    if (Boolean.TRUE.equals(apiResult.get("success"))) {
                        createdNotices.add(noticeNo);
                        log.info("Created test notice: {}", noticeNo);
                    } else {
                        failedNotices.add(noticeNo);
                        errors.add(Map.of(
                            "noticeNo", noticeNo,
                            "error", apiResult.getOrDefault("error", "Unknown error")
                        ));
                        log.error("Failed to create notice {}: {}", noticeNo, apiResult.get("error"));
                    }

                } catch (Exception e) {
                    failedNotices.add(noticeNo);
                    errors.add(Map.of(
                        "noticeNo", noticeNo,
                        "error", e.getMessage()
                    ));
                    log.error("Exception creating notice {}: {}", noticeNo, e.getMessage(), e);
                }
            }

            result.put("success", failedNotices.isEmpty());
            result.put("createdNotices", createdNotices);
            result.put("failedNotices", failedNotices);
            result.put("totalCreated", createdNotices.size());
            result.put("totalFailed", failedNotices.size());
            result.put("errors", errors);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error in createTestNotices: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Manual Cleanup: Delete test notices from all tables
     *
     * @param noticeNumbers List of notice numbers to delete
     * @return Map with success flag, deleted count, and failed count
     */
    public Map<String, Object> cleanupTestNotices(List<String> noticeNumbers) {
        Map<String, Object> result = new HashMap<>();
        List<String> deletedNotices = new ArrayList<>();
        List<String> failedNotices = new ArrayList<>();

        try {
            for (String noticeNo : noticeNumbers) {
                try {
                    boolean deleted = deleteNotice(noticeNo);
                    if (deleted) {
                        deletedNotices.add(noticeNo);
                        log.info("Deleted test notice: {}", noticeNo);
                    } else {
                        failedNotices.add(noticeNo);
                        log.warn("Notice not found for deletion: {}", noticeNo);
                    }
                } catch (Exception e) {
                    failedNotices.add(noticeNo);
                    log.error("Error deleting notice {}: {}", noticeNo, e.getMessage(), e);
                }
            }

            result.put("success", failedNotices.isEmpty());
            result.put("deletedNotices", deletedNotices);
            result.put("failedNotices", failedNotices);
            result.put("totalDeleted", deletedNotices.size());
            result.put("totalFailed", failedNotices.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error in cleanupTestNotices: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Delete all test notices starting with 'T' prefix (DANGEROUS!)
     *
     * @return Map with success flag, deleted count, and notice numbers
     */
    public Map<String, Object> cleanupAllTestNotices() {
        Map<String, Object> result = new HashMap<>();
        List<String> deletedNotices = new ArrayList<>();

        try {
            // Find all VON records starting with 'T' (use getAll with filtering)
            List<OcmsValidOffenceNotice> testNotices = vonRepository.findAll().stream()
                    .filter(von -> von.getNoticeNo() != null && von.getNoticeNo().startsWith("T"))
                    .collect(java.util.stream.Collectors.toList());

            for (OcmsValidOffenceNotice notice : testNotices) {
                String noticeNo = notice.getNoticeNo();
                try {
                    boolean deleted = deleteNotice(noticeNo);
                    if (deleted) {
                        deletedNotices.add(noticeNo);
                        log.info("Deleted test notice: {}", noticeNo);
                    }
                } catch (Exception e) {
                    log.error("Error deleting notice {}: {}", noticeNo, e.getMessage(), e);
                }
            }

            result.put("success", true);
            result.put("deletedNotices", deletedNotices);
            result.put("totalDeleted", deletedNotices.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error in cleanupAllTestNotices: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Delete a single notice from all tables (VON, ONOD, change processing)
     *
     * @param noticeNo Notice number to delete
     * @return true if deleted, false if not found
     */
    private boolean deleteNotice(String noticeNo) {
        boolean found = false;

        // Delete from change processing table (composite key - use getAll with filtering)
        List<OcmsChangeOfProcessing> changeRecords = changeProcessingRepository.findAll().stream()
                .filter(c -> noticeNo.equals(c.getNoticeNo()))
                .collect(java.util.stream.Collectors.toList());
        if (!changeRecords.isEmpty()) {
            changeProcessingRepository.deleteAll(changeRecords);
            found = true;
            log.debug("Deleted {} change processing records for {}", changeRecords.size(), noticeNo);
        }

        // Delete from ONOD table (composite key - need to find all records for this notice first)
        List<OcmsOffenceNoticeOwnerDriver> onodRecords = onodRepository.findAll().stream()
                .filter(od -> noticeNo.equals(od.getNoticeNo()))
                .collect(java.util.stream.Collectors.toList());
        if (!onodRecords.isEmpty()) {
            onodRepository.deleteAll(onodRecords);
            found = true;
            log.debug("Deleted {} ONOD records for {}", onodRecords.size(), noticeNo);
        }

        // Skip the old single ONOD delete - handled above
        /*Optional<OcmsOffenceNoticeOwnerDriver> onod = onodRepository.findById(noticeNo);
        if (onod.isPresent()) {
            onodRepository.delete(onod.get());
            found = true;
            log.debug("Deleted ONOD record for {}", noticeNo);
        }*/

        // Delete from VON table
        Optional<OcmsValidOffenceNotice> von = vonRepository.findById(noticeNo);
        if (von.isPresent()) {
            vonRepository.delete(von.get());
            found = true;
            log.debug("Deleted VON record for {}", noticeNo);
        }

        return found;
    }

    /**
     * Check if a notice exists in the database
     *
     * @param noticeNo Notice number to check
     * @return true if exists, false otherwise
     */
    public boolean noticeExists(String noticeNo) {
        return vonRepository.findById(noticeNo).isPresent();
    }

    /**
     * Get count of test notices (starting with 'T')
     *
     * @return Count of test notices
     */
    public long getTestNoticeCount() {
        return vonRepository.findAll().stream()
                .filter(von -> von.getNoticeNo() != null && von.getNoticeNo().startsWith("T"))
                .count();
    }
}
