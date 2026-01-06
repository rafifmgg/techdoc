package com.ocmsintranet.apiservice.testing.furnish_submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDoc;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for setting up and cleaning up test data for Furnish Submission Tests
 * Provides API-based setup/cleanup for Staff Portal integration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishSubmissionTestSetupService {

    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsFurnishApplicationDocRepository furnishApplicationDocRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;
    private final ObjectMapper objectMapper;

    /**
     * Setup test data - Creates/resets test notices to known state
     *
     * What this does:
     * 1. Read test scenarios from scenario.json
     * 2. Create or reset test notices in VON
     * 3. Set known initial values (stage, status, etc.)
     * 4. Clear any existing furnish application/suspension records
     *
     * @return Map with setup results
     */
    @Transactional
    public Map<String, Object> setupTestData() {
        log.info("Starting furnish submission test data setup");

        int noticesCreated = 0;
        final int[] noticesResetArr = {0};
        int recordsDeleted = 0;

        try {
            // 1. Load test scenarios
            List<String> testNotices = getTestNoticeNumbers();
            log.info("Found {} test notices to setup", testNotices.size());

            // 2. Delete existing test data first
            recordsDeleted += cleanupExistingData(testNotices);

            // 3. Create or reset test notices
            for (String noticeNo : testNotices) {
                OcmsValidOffenceNotice notice = validOffenceNoticeRepository.findById(noticeNo)
                        .orElse(new OcmsValidOffenceNotice());

                boolean isNewNotice = notice.getNoticeNo() == null;

                notice.setNoticeNo(noticeNo);
                notice.setVehicleNo("TEST" + noticeNo);
                notice.setCurrentProcessingStage("PRE"); // Pre-payment stage (furnishable)
                notice.setNoticeDateAndTime(LocalDateTime.now().minusDays(5));
                notice.setCreDate(LocalDateTime.now());
                notice.setCreUserId("TEST_SETUP");

                validOffenceNoticeRepository.save(notice);

                if (isNewNotice) {
                    noticesCreated++;
                } else {
                    noticesResetArr[0]++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("notices_reset", noticesResetArr[0]);
            result.put("records_deleted", recordsDeleted);
            result.put("test_notices", testNotices);

            log.info("Test data setup completed: {} created, {} reset, {} records deleted",
                    noticesCreated, noticesResetArr[0], recordsDeleted);

            return result;

        } catch (Exception e) {
            log.error("Test data setup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Setup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cleanup test data - Removes all test-related data
     *
     * What this does:
     * 1. Delete furnish application records
     * 2. Delete furnish application documents
     * 3. Delete owner/driver records
     * 4. Delete owner/driver addresses
     * 5. Delete suspension records
     * 6. Reset test notices to initial state
     *
     * @return Map with cleanup results
     */
    @Transactional
    public Map<String, Object> cleanupTestData() {
        log.info("Starting furnish submission test data cleanup");

        int applicationsDeleted = 0;
        int documentsDeleted = 0;
        int ownerDriversDeleted = 0;
        int addressesDeleted = 0;
        int suspensionsDeleted = 0;
        final int[] noticesResetArr = {0};

        try {
            List<String> testNotices = getTestNoticeNumbers();

            // 1. Delete furnish applications (by notice number)
            for (String noticeNo : testNotices) {
                List<String> txnNos = furnishApplicationRepository
                        .findAll()
                        .stream()
                        .filter(app -> noticeNo.equals(app.getNoticeNo()))
                        .map(app -> app.getTxnNo())
                        .collect(Collectors.toList());

                for (String txnNo : txnNos) {
                    // Delete documents first (foreign key - using composite key)
                    List<OcmsFurnishApplicationDoc> docs = furnishApplicationDocRepository
                            .findAll()
                            .stream()
                            .filter(doc -> txnNo.equals(doc.getTxnNo()))
                            .collect(Collectors.toList());
                    documentsDeleted += docs.size();
                    docs.forEach(doc -> furnishApplicationDocRepository.delete(doc));

                    // Delete application
                    furnishApplicationRepository.deleteById(txnNo);
                    applicationsDeleted++;
                }
            }

            // 2. Delete owner/driver records (composite key: noticeNo + ownerDriverIndicator)
            for (String noticeNo : testNotices) {
                List<OcmsOffenceNoticeOwnerDriver> ownerDrivers = ownerDriverRepository
                        .findAll()
                        .stream()
                        .filter(od -> noticeNo.equals(od.getNoticeNo()))
                        .collect(Collectors.toList());

                for (OcmsOffenceNoticeOwnerDriver od : ownerDrivers) {
                    String ownerDriverIndicator = od.getOwnerDriverIndicator();

                    // Delete addresses first (foreign key - composite key: noticeNo + ownerDriverIndicator + typeOfAddress)
                    List<OcmsOffenceNoticeOwnerDriverAddr> addrs = ownerDriverAddrRepository
                            .findAll()
                            .stream()
                            .filter(addr -> noticeNo.equals(addr.getNoticeNo())
                                    && ownerDriverIndicator.equals(addr.getOwnerDriverIndicator()))
                            .collect(Collectors.toList());
                    addressesDeleted += addrs.size();
                    addrs.forEach(addr -> ownerDriverAddrRepository.delete(addr));

                    // Delete owner/driver
                    ownerDriverRepository.delete(od);
                    ownerDriversDeleted++;
                }
            }

            // 3. Delete suspension records
            for (String noticeNo : testNotices) {
                List<SuspendedNotice> suspensions = suspendedNoticeRepository
                        .findAll()
                        .stream()
                        .filter(sus -> noticeNo.equals(sus.getNoticeNo()))
                        .collect(Collectors.toList());

                suspensionsDeleted += suspensions.size();
                suspensions.forEach(sus -> suspendedNoticeRepository.delete(sus));
            }

            // 4. Reset notices to initial state
            for (String noticeNo : testNotices) {
                validOffenceNoticeRepository.findById(noticeNo).ifPresent(notice -> {
                    notice.setCurrentProcessingStage("PRE");
                    validOffenceNoticeRepository.save(notice);
                    noticesResetArr[0]++;
                });
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data cleanup completed");
            result.put("applications_deleted", applicationsDeleted);
            result.put("documents_deleted", documentsDeleted);
            result.put("owner_drivers_deleted", ownerDriversDeleted);
            result.put("addresses_deleted", addressesDeleted);
            result.put("suspensions_deleted", suspensionsDeleted);
            result.put("notices_reset", noticesResetArr[0]);

            log.info("Test data cleanup completed: {} applications, {} documents, {} owner/drivers, {} addresses, {} suspensions deleted, {} notices reset",
                    applicationsDeleted, documentsDeleted, ownerDriversDeleted, addressesDeleted, suspensionsDeleted, noticesResetArr[0]);

            return result;

        } catch (Exception e) {
            log.error("Test data cleanup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of test notice numbers from scenario.json
     *
     * @return List of test notice numbers
     */
    private List<String> getTestNoticeNumbers() {
        try {
            ClassPathResource resource = new ClassPathResource("testing/furnish_submission/data/scenario.json");
            JsonNode scenarios = objectMapper.readTree(resource.getInputStream());

            List<String> noticeNumbers = new ArrayList<>();
            if (scenarios.isArray()) {
                for (JsonNode scenario : scenarios) {
                    if (scenario.has("noticeNo")) {
                        noticeNumbers.add(scenario.get("noticeNo").asText());
                    }
                }
            }

            return noticeNumbers;

        } catch (IOException e) {
            log.error("Failed to read scenario.json: {}", e.getMessage(), e);
            // Return default test notices if file not found
            return Arrays.asList("TEST001", "TEST002", "TEST003", "TEST004", "TEST005");
        }
    }

    /**
     * Cleanup existing test data before setup
     *
     * @param testNotices List of test notice numbers
     * @return Number of records deleted
     */
    private int cleanupExistingData(List<String> testNotices) {
        int recordsDeleted = 0;

        for (String noticeNo : testNotices) {
            // Delete existing furnish applications
            List<String> txnNos = furnishApplicationRepository
                    .findAll()
                    .stream()
                    .filter(app -> noticeNo.equals(app.getNoticeNo()))
                    .map(app -> app.getTxnNo())
                    .collect(Collectors.toList());

            for (String txnNo : txnNos) {
                // Delete documents first (composite key)
                List<OcmsFurnishApplicationDoc> docs = furnishApplicationDocRepository
                        .findAll()
                        .stream()
                        .filter(doc -> txnNo.equals(doc.getTxnNo()))
                        .collect(Collectors.toList());
                docs.forEach(doc -> furnishApplicationDocRepository.delete(doc));

                furnishApplicationRepository.deleteById(txnNo);
                recordsDeleted++;
            }

            // Delete existing suspensions
            List<SuspendedNotice> suspensions = suspendedNoticeRepository
                    .findAll()
                    .stream()
                    .filter(sus -> noticeNo.equals(sus.getNoticeNo()))
                    .collect(Collectors.toList());

            recordsDeleted += suspensions.size();
            suspensions.forEach(sus -> suspendedNoticeRepository.delete(sus));
        }

        return recordsDeleted;
    }
}
