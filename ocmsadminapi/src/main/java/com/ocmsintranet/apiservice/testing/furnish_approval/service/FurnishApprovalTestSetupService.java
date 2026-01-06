package com.ocmsintranet.apiservice.testing.furnish_approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecordsRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishApprovalTestSetupService {

    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsEmailNotificationRecordsRepository emailNotificationRepository;
    private final OcmsSmsNotificationRecordsRepository smsNotificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> setupTestData() {
        log.info("Starting furnish approval test data setup");

        int noticesCreated = 0;
        final int[] noticesResetArr = {0};
        int applicationsCreated = 0;
        int suspensionsCreated = 0;

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
                notice.setCurrentProcessingStage("PRE");
                notice.setNoticeDateAndTime(LocalDateTime.now().minusDays(10));
                notice.setOffenceTime(LocalDateTime.now().minusDays(10));
                notice.setCreDate(LocalDateTime.now());
                notice.setCreUserId("TEST_SETUP");
                validOffenceNoticeRepository.save(notice);

                if (isNewNotice) noticesCreated++;
                else noticesResetArr[0]++;

                // Create pending furnish application
                String txnNo = "TXN_" + noticeNo;
                OcmsFurnishApplication app = new OcmsFurnishApplication();
                app.setTxnNo(txnNo);
                app.setNoticeNo(noticeNo);
                app.setApplicationStatus("P"); // Pending
                app.setOwnerEmailAddr("owner." + noticeNo.toLowerCase() + "@test.com");
                app.setFurnishEmailAddr("furnished." + noticeNo.toLowerCase() + "@test.com");
                app.setFurnishTelCode("65");
                app.setFurnishTelNo("9" + noticeNo.substring(noticeNo.length() - 7));
                app.setOwnerDriverIndicator("D");
                app.setOwnerIdNo("S1234567A");
                app.setOwnerName("Test Owner " + noticeNo);
                app.setFurnishIdNo("S7654321B");
                app.setFurnishName("Test Furnished " + noticeNo);
                app.setVehicleNo("TEST" + noticeNo);
                app.setCreDate(LocalDateTime.now());
                app.setCreUserId("TEST_SETUP");
                furnishApplicationRepository.save(app);
                applicationsCreated++;

                // Create TS-PDP suspension
                SuspendedNotice tsPdp = new SuspendedNotice();
                tsPdp.setNoticeNo(noticeNo);
                tsPdp.setSuspensionType("TS");
                tsPdp.setReasonOfSuspension("PDP");
                tsPdp.setDateOfSuspension(LocalDateTime.now().minusDays(5));
                tsPdp.setSrNo(1);
                tsPdp.setSuspensionSource("TEST");
                tsPdp.setOfficerAuthorisingSupension("TEST_SETUP");
                tsPdp.setDateOfRevival(null);
                tsPdp.setCreDate(LocalDateTime.now());
                tsPdp.setCreUserId("TEST_SETUP");
                suspendedNoticeRepository.save(tsPdp);
                suspensionsCreated++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("notices_reset", noticesResetArr[0]);
            result.put("applications_created", applicationsCreated);
            result.put("suspensions_created", suspensionsCreated);
            result.put("test_notices", testNotices);

            log.info("Setup completed: {} created, {} reset, {} apps, {} suspensions",
                    noticesCreated, noticesResetArr[0], applicationsCreated, suspensionsCreated);

            return result;

        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Setup failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> cleanupTestData() {
        log.info("Starting furnish approval test data cleanup");

        int applicationsDeleted = 0;
        int suspensionsDeleted = 0;
        int emailsDeleted = 0;
        int smsDeleted = 0;
        final int[] noticesResetArr = {0}; // Use array to make it effectively final

        try {
            List<String> testNotices = getTestNoticeNumbers();

            for (String noticeNo : testNotices) {
                // Delete applications
                furnishApplicationRepository.findAll().stream()
                        .filter(app -> noticeNo.equals(app.getNoticeNo()))
                        .forEach(app -> {
                            furnishApplicationRepository.delete(app);
                        });
                applicationsDeleted++;

                // Delete suspensions
                suspendedNoticeRepository.findAll().stream()
                        .filter(sus -> noticeNo.equals(sus.getNoticeNo()))
                        .forEach(sus -> {
                            suspendedNoticeRepository.delete(sus);
                        });
                suspensionsDeleted++;

                // Delete email notifications
                emailNotificationRepository.findAll().stream()
                        .filter(email -> noticeNo.equals(email.getNoticeNo()))
                        .forEach(email -> {
                            emailNotificationRepository.delete(email);
                        });
                emailsDeleted++;

                // Delete SMS notifications
                smsNotificationRepository.findAll().stream()
                        .filter(sms -> noticeNo.equals(sms.getNoticeNo()))
                        .forEach(sms -> {
                            smsNotificationRepository.delete(sms);
                        });
                smsDeleted++;

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
            result.put("applications_deleted", applicationsDeleted);
            result.put("suspensions_deleted", suspensionsDeleted);
            result.put("emails_deleted", emailsDeleted);
            result.put("sms_deleted", smsDeleted);
            result.put("notices_reset", noticesResetArr[0]);

            log.info("Cleanup completed: {} apps, {} suspensions, {} emails, {} sms, {} notices",
                    applicationsDeleted, suspensionsDeleted, emailsDeleted, smsDeleted, noticesResetArr[0]);

            return result;

        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }

    private List<String> getTestNoticeNumbers() {
        try {
            ClassPathResource resource = new ClassPathResource("testing/furnish_approval/data/scenario.json");
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
            return Arrays.asList("APPR001", "APPR002", "APPR003", "APPR004", "APPR005");
        }
    }

    private void cleanupExistingData(List<String> testNotices) {
        for (String noticeNo : testNotices) {
            furnishApplicationRepository.findAll().stream()
                    .filter(app -> noticeNo.equals(app.getNoticeNo()))
                    .forEach(app -> furnishApplicationRepository.delete(app));

            suspendedNoticeRepository.findAll().stream()
                    .filter(sus -> noticeNo.equals(sus.getNoticeNo()))
                    .forEach(sus -> suspendedNoticeRepository.delete(sus));

            emailNotificationRepository.findAll().stream()
                    .filter(email -> noticeNo.equals(email.getNoticeNo()))
                    .forEach(email -> emailNotificationRepository.delete(email));

            smsNotificationRepository.findAll().stream()
                    .filter(sms -> noticeNo.equals(sms.getNoticeNo()))
                    .forEach(sms -> smsNotificationRepository.delete(sms));
        }
    }
}
