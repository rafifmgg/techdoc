package com.ocmsintranet.apiservice.testing.reduction.service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction.OcmsReducedOffenceAmountRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for setting up and cleaning up reduction test data
 *
 * Provides API-based setup/cleanup so tests can be triggered from Staff Portal
 */
@Service
@Slf4j
public class ReductionTestSetupService {

    private final OcmsValidOffenceNoticeRepository vonRepository;
    private final EocmsValidOffenceNoticeRepository evonRepository;
    private final SuspendedNoticeRepository suspensionRepository;
    private final OcmsReducedOffenceAmountRepository reductionLogRepository;

    public ReductionTestSetupService(
            OcmsValidOffenceNoticeRepository vonRepository,
            EocmsValidOffenceNoticeRepository evonRepository,
            SuspendedNoticeRepository suspensionRepository,
            OcmsReducedOffenceAmountRepository reductionLogRepository) {
        this.vonRepository = vonRepository;
        this.evonRepository = evonRepository;
        this.suspensionRepository = suspensionRepository;
        this.reductionLogRepository = reductionLogRepository;
    }

    /**
     * Setup test data - Creates/resets test notices to known state
     *
     * What this does:
     * 1. Create or reset test notices in VON/eVON
     * 2. Set known initial values (amount_payable, stage, rule code)
     * 3. Clear any existing suspension/log records
     *
     * @return Map with setup results
     */
    @Transactional
    public Map<String, Object> setupTestData() {
        log.info("Starting reduction test data setup");

        int noticesCreated = 0;
        final int[] noticesResetArr = {0};

        try {
            List<String> testNotices = getTestNoticeNumbers();

            // 1. Clear existing test data first
            log.info("Clearing existing test data for {} notices", testNotices.size());
            // Delete suspensions for each notice (no bulk delete method available)
            for (String noticeNo : testNotices) {
                suspensionRepository.findAll().stream()
                        .filter(s -> noticeNo.equals(s.getNoticeNo()))
                        .forEach(s -> suspensionRepository.delete(s));
            }
            // Delete reduction logs for each notice (no bulk delete method available)
            for (String noticeNo : testNotices) {
                reductionLogRepository.findAll().stream()
                        .filter(r -> noticeNo.equals(r.getNoticeNo()))
                        .forEach(r -> reductionLogRepository.delete(r));
            }

            // 2. Create or reset test notices
            noticesCreated += createOrResetNotice("700405001A", "100.00", 30305, "RD1", null);
            noticesCreated += createOrResetNotice("700405002B", "200.00", 31302, "RD2", null);
            noticesCreated += createOrResetNotice("700405003C", "150.00", 99999, "RR3", null);
            noticesCreated += createOrResetNotice("700405004D", "180.00", 30302, "DN1", null);
            noticesCreated += createOrResetNotice("700405005E", "220.00", 88888, "DR3", null);
            noticesCreated += createOrResetNotice("700405006F", "100.00", 30305, "RD1", "FP");  // Paid (FP)
            noticesCreated += createOrResetNotice("700405007G", "150.00", 99999, "CPC", null);
            noticesCreated += createOrResetNotice("700405008H", "120.00", 77777, "ENA", null);
            noticesCreated += createOrResetNotice("700405009I", "90.00", 21300, "ENA", null);
            noticesCreated += createOrResetNotice("700405010J", "110.00", 30305, "NPA", null);
            noticesCreated += createOrResetNotice("700405011K", "130.00", 30305, "RD2", "PRA");  // Paid (PRA)
            noticesCreated += createOrResetNotice("700405012L", "100.00", 30305, "RD1", null);  // For amount validation test
            noticesCreated += createOrResetNotice("700405013M", "100.00", 30305, "RD1", null);  // For missing field test

            // Note: NOTFOUND999 should NOT exist in database (for not-found test)

            noticesResetArr[0] = testNotices.size() - 1;  // All except NOTFOUND999

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data setup completed");
            result.put("notices_created", noticesCreated);
            result.put("notices_reset", noticesResetArr[0]);

            log.info("Test data setup completed: {} created/reset", noticesCreated);

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
     * 1. Delete suspension records created by tests
     * 2. Delete reduction log records created by tests
     * 3. Reset test notices to initial state
     *
     * @return Map with cleanup results
     */
    @Transactional
    public Map<String, Object> cleanupTestData() {
        log.info("Starting reduction test data cleanup");

        int suspensionsDeleted = 0;
        int logsDeleted = 0;
        final int[] noticesResetArr = {0};

        try {
            List<String> testNotices = getTestNoticeNumbers();

            // 1. Delete suspension records (no bulk delete method available)
            for (String noticeNo : testNotices) {
                long deleted = suspensionRepository.findAll().stream()
                        .filter(s -> noticeNo.equals(s.getNoticeNo()))
                        .peek(s -> suspensionRepository.delete(s))
                        .count();
                suspensionsDeleted += deleted;
            }
            log.info("Deleted {} suspension records", suspensionsDeleted);

            // 2. Delete reduction log records (no bulk delete method available)
            for (String noticeNo : testNotices) {
                long deleted = reductionLogRepository.findAll().stream()
                        .filter(r -> noticeNo.equals(r.getNoticeNo()))
                        .peek(r -> reductionLogRepository.delete(r))
                        .count();
                logsDeleted += deleted;
            }
            log.info("Deleted {} reduction log records", logsDeleted);

            // 3. Reset notices to initial state
            for (String noticeNo : testNotices) {
                if ("NOTFOUND999".equals(noticeNo)) {
                    continue;  // Skip - this notice should not exist
                }

                vonRepository.findById(noticeNo).ifPresent(notice -> {
                    resetNoticeToInitialState(notice);
                    vonRepository.save(notice);
                    noticesResetArr[0]++;
                });

                evonRepository.findById(noticeNo).ifPresent(enotice -> {
                    resetEnoticeToInitialState(enotice);
                    evonRepository.save(enotice);
                });
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Test data cleanup completed");
            result.put("suspensions_deleted", suspensionsDeleted);
            result.put("logs_deleted", logsDeleted);
            result.put("notices_reset", noticesResetArr[0]);

            log.info("Test data cleanup completed: {} suspensions, {} logs deleted, {} notices reset",
                    suspensionsDeleted, logsDeleted, noticesResetArr[0]);

            return result;

        } catch (Exception e) {
            log.error("Test data cleanup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create or reset a test notice to known state
     *
     * @param noticeNo Notice number
     * @param amountPayable Initial amount payable
     * @param ruleCode Computer rule code
     * @param stage Last processing stage
     * @param crsReason CRS reason of suspension (null for unpaid, "FP" or "PRA" for paid)
     * @return 1 if created/reset successfully
     */
    private int createOrResetNotice(String noticeNo, String amountPayable, Integer ruleCode,
                                    String stage, String crsReason) {
        // Intranet VON
        OcmsValidOffenceNotice notice = vonRepository.findById(noticeNo)
                .orElse(new OcmsValidOffenceNotice());
        notice.setNoticeNo(noticeNo);
        notice.setAmountPayable(new BigDecimal(amountPayable));
        notice.setCompositionAmount(new BigDecimal(amountPayable));  // Set original amount
        notice.setComputerRuleCode(ruleCode);
        notice.setLastProcessingStage(stage);
        notice.setCrsReasonOfSuspension(crsReason);  // For paid status test
        notice.setSuspensionType(null);
        notice.setEprReasonOfSuspension(null);
        notice.setEprDateOfSuspension(null);
        notice.setDueDateOfRevival(null);
        notice.setVehicleNo("TEST" + noticeNo.substring(noticeNo.length() - 4));  // Dummy vehicle no
        notice.setCreDate(LocalDateTime.now());
        vonRepository.save(notice);

        // Internet eVON
        EocmsValidOffenceNotice enotice = evonRepository.findById(noticeNo)
                .orElse(new EocmsValidOffenceNotice());
        enotice.setNoticeNo(noticeNo);
        enotice.setAmountPayable(new BigDecimal(amountPayable));
        enotice.setLastProcessingStage(stage);
        enotice.setSuspensionType(null);
        enotice.setEprReasonOfSuspension(null);
        enotice.setEprDateOfSuspension(null);
        enotice.setVehicleNo("TEST" + noticeNo.substring(noticeNo.length() - 4));
        evonRepository.save(enotice);

        return 1;
    }

    /**
     * Reset notice to initial state (clear suspension fields)
     */
    private void resetNoticeToInitialState(OcmsValidOffenceNotice notice) {
        // Reset amount to original
        if (notice.getCompositionAmount() != null) {
            notice.setAmountPayable(notice.getCompositionAmount());
        }
        // Clear suspension fields
        notice.setSuspensionType(null);
        notice.setEprReasonOfSuspension(null);
        notice.setEprDateOfSuspension(null);
        notice.setDueDateOfRevival(null);
    }

    /**
     * Reset enotice to initial state (clear suspension fields)
     */
    private void resetEnoticeToInitialState(EocmsValidOffenceNotice enotice) {
        // Clear suspension fields
        enotice.setSuspensionType(null);
        enotice.setEprReasonOfSuspension(null);
        enotice.setEprDateOfSuspension(null);
    }

    /**
     * Get list of test notice numbers from scenario.json
     *
     * @return List of test notice numbers
     */
    private List<String> getTestNoticeNumbers() {
        return List.of(
                "700405001A", "700405002B", "700405003C", "700405004D",
                "700405005E", "700405006F", "700405007G", "700405008H",
                "700405009I", "700405010J", "700405011K", "700405012L",
                "700405013M", "NOTFOUND999"  // For not-found test (should NOT exist)
        );
    }
}
