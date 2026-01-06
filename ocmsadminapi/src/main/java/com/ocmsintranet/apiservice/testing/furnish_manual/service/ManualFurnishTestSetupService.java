package com.ocmsintranet.apiservice.testing.furnish_manual.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishTestSetupService {

    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> setupTestData() {
        log.info("Starting manual furnish test data setup");

        int noticesCreated = 0;
        final int[] noticesResetArr = {0};

        try {
            List<String> testNotices = getTestNoticeNumbers();
            log.info("Found {} test notices to setup", testNotices.size());

            cleanupExistingData(testNotices);

            for (String noticeNo : testNotices) {
                // Create valid offence notice
                OcmsValidOffenceNotice notice = validOffenceNoticeRepository.findById(noticeNo)
                        .orElse(new OcmsValidOffenceNotice());
                boolean isNewNotice = notice.getNoticeNo() == null;

                notice.setNoticeNo(noticeNo);
                notice.setVehicleNo("TEST" + noticeNo);
                notice.setCurrentProcessingStage("PRE"); // Furnishable stage
                notice.setNoticeDateAndTime(LocalDateTime.now().minusDays(10));
                notice.setCreDate(LocalDateTime.now());
                notice.setCreUserId("TEST_SETUP");
                validOffenceNoticeRepository.save(notice);

                if (isNewNotice) noticesCreated++;
                else noticesResetArr[0]++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("notices_reset", noticesResetArr[0]);
            result.put("test_notices", testNotices);

            log.info("Setup completed: {} created, {} reset", noticesCreated, noticesResetArr[0]);

            return result;

        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Setup failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> cleanupTestData() {
        log.info("Starting manual furnish test data cleanup");

        int ownerDriversDeleted = 0;
        int addressesDeleted = 0;
        final int[] noticesResetArr = {0};

        try {
            List<String> testNotices = getTestNoticeNumbers();

            for (String noticeNo : testNotices) {
                // Delete owner/driver addresses first (foreign key)
                ownerDriverAddrRepository.findAll().stream()
                        .filter(addr -> noticeNo.equals(addr.getNoticeNo()))
                        .forEach(addr -> {
                            ownerDriverAddrRepository.delete(addr);
                        });
                addressesDeleted++;

                // Delete owner/driver records
                ownerDriverRepository.findAll().stream()
                        .filter(od -> noticeNo.equals(od.getNoticeNo()))
                        .forEach(od -> {
                            ownerDriverRepository.delete(od);
                        });
                ownerDriversDeleted++;

                // Reset notice
                validOffenceNoticeRepository.findById(noticeNo).ifPresent(notice -> {
                    notice.setCurrentProcessingStage("PRE");
                    validOffenceNoticeRepository.save(notice);
                    noticesResetArr[0]++;
                });
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data cleanup completed");
            result.put("owner_drivers_deleted", ownerDriversDeleted);
            result.put("addresses_deleted", addressesDeleted);
            result.put("notices_reset", noticesResetArr[0]);

            log.info("Cleanup completed: {} owner/drivers, {} addresses, {} notices",
                    ownerDriversDeleted, addressesDeleted, noticesResetArr[0]);

            return result;

        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }

    private List<String> getTestNoticeNumbers() {
        try {
            ClassPathResource resource = new ClassPathResource("testing/furnish_manual/data/scenario.json");
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
            return Arrays.asList("MF001", "MF002", "MF003", "MF004", "MF005", "MF006");
        }
    }

    private void cleanupExistingData(List<String> testNotices) {
        for (String noticeNo : testNotices) {
            ownerDriverAddrRepository.findAll().stream()
                    .filter(addr -> noticeNo.equals(addr.getNoticeNo()))
                    .forEach(addr -> ownerDriverAddrRepository.delete(addr));

            ownerDriverRepository.findAll().stream()
                    .filter(od -> noticeNo.equals(od.getNoticeNo()))
                    .forEach(od -> ownerDriverRepository.delete(od));
        }
    }
}
