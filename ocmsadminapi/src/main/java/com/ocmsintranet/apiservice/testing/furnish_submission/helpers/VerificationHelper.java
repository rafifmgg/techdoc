package com.ocmsintranet.apiservice.testing.furnish_submission.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDoc;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Helper for verifying database state after furnish submission
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationHelper {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsFurnishApplicationDocRepository furnishApplicationDocRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * Verify furnish application record exists and has correct status
     *
     * @param context Test context
     * @param txnNo Transaction number
     * @param expectedStatus Expected status ('A' for auto-approved, 'P' for pending)
     */
    public void verifyFurnishApplication(TestContext context, String txnNo, String expectedStatus) {
        log.info("Verifying furnish application - txnNo: {}, expectedStatus: {}", txnNo, expectedStatus);

        Optional<OcmsFurnishApplication> appOpt = furnishApplicationRepository.findById(txnNo);

        if (appOpt.isEmpty()) {
            context.addVerification(
                    "Furnish Application Exists",
                    false,
                    "Application record found",
                    "No record found",
                    "Missing furnish application for txnNo: " + txnNo
            );
            return;
        }

        OcmsFurnishApplication app = appOpt.get();

        // Verify status
        boolean statusMatches = expectedStatus.equals(app.getApplicationStatus());
        context.addVerification(
                "Application Status",
                statusMatches,
                expectedStatus,
                app.getApplicationStatus(),
                statusMatches ? "Status matches expected" : "Status mismatch"
        );

        // Store in runtime data for other verifications
        context.getRuntimeData().put("furnishApplication", app);
    }

    /**
     * Verify furnish application documents were attached
     *
     * @param context Test context
     * @param txnNo Transaction number
     * @param expectedDocCount Expected number of documents
     */
    public void verifyFurnishDocuments(TestContext context, String txnNo, int expectedDocCount) {
        log.info("Verifying furnish documents - txnNo: {}, expectedCount: {}", txnNo, expectedDocCount);

        List<OcmsFurnishApplicationDoc> docs = furnishApplicationDocRepository.findByTxnNo(txnNo);

        boolean countMatches = docs.size() == expectedDocCount;
        context.addVerification(
                "Document Count",
                countMatches,
                String.valueOf(expectedDocCount),
                String.valueOf(docs.size()),
                countMatches ? "Document count matches" : "Document count mismatch"
        );
    }

    /**
     * Verify owner/driver record was created with current_offender='Y'
     *
     * @param context Test context
     * @param noticeNo Notice number
     * @param expectedOffenderType Expected offender type ('D' for driver, 'H' for hirer)
     */
    public void verifyOwnerDriverRecord(TestContext context, String noticeNo, String expectedOffenderType) {
        log.info("Verifying owner/driver record - noticeNo: {}, expectedOffenderType: {}",
                noticeNo, expectedOffenderType);

        List<OcmsOffenceNoticeOwnerDriver> records = ownerDriverRepository.findAll().stream()
                .filter(od -> noticeNo.equals(od.getNoticeNo()) && "Y".equals(od.getOffenderIndicator()))
                .collect(java.util.stream.Collectors.toList());

        if (records.isEmpty()) {
            context.addVerification(
                    "Owner/Driver Record Exists",
                    false,
                    "Record with offender_indicator='Y'",
                    "No record found",
                    "Missing owner/driver record for notice: " + noticeNo
            );
            return;
        }

        OcmsOffenceNoticeOwnerDriver record = records.get(0);

        // Verify offender type
        boolean typeMatches = expectedOffenderType.equals(record.getOwnerDriverIndicator());
        context.addVerification(
                "Offender Type",
                typeMatches,
                expectedOffenderType,
                record.getOwnerDriverIndicator(),
                typeMatches ? "Offender type matches" : "Offender type mismatch"
        );

        // Store in runtime data
        context.getRuntimeData().put("ownerDriverRecord", record);
    }

    /**
     * Verify owner/driver address record was created
     *
     * @param context Test context
     * @param noticeNo Notice number
     * @param srNo Serial number of owner/driver record
     */
    public void verifyOwnerDriverAddress(TestContext context, String noticeNo, String ownerDriverIndicator) {
        log.info("Verifying owner/driver address - noticeNo: {}, ownerDriverIndicator: {}", noticeNo, ownerDriverIndicator);

        List<OcmsOffenceNoticeOwnerDriverAddr> addresses = ownerDriverAddrRepository.findAll().stream()
                .filter(addr -> noticeNo.equals(addr.getNoticeNo())
                        && ownerDriverIndicator.equals(addr.getOwnerDriverIndicator())
                        && "registered_mail".equals(addr.getTypeOfAddress()))
                .collect(java.util.stream.Collectors.toList());

        boolean addressExists = !addresses.isEmpty();
        context.addVerification(
                "Owner/Driver Address Exists",
                addressExists,
                "Address record found",
                addressExists ? "Found" : "Not found",
                addressExists ? "Address record created" : "Missing address record"
        );
    }

    /**
     * Verify TS-PDP suspension was created (for auto-approved cases)
     *
     * @param context Test context
     * @param noticeNo Notice number
     */
    public void verifyTsPdpSuspension(TestContext context, String noticeNo) {
        log.info("Verifying TS-PDP suspension - noticeNo: {}", noticeNo);

        List<SuspendedNotice> suspensions = suspendedNoticeRepository.findAll().stream()
                .filter(s -> noticeNo.equals(s.getNoticeNo())
                        && "TS".equals(s.getSuspensionType())
                        && "PDP".equals(s.getReasonOfSuspension()))
                .collect(java.util.stream.Collectors.toList());

        if (suspensions.isEmpty()) {
            context.addVerification(
                    "TS-PDP Suspension Created",
                    false,
                    "TS-PDP suspension record",
                    "No suspension found",
                    "Missing TS-PDP suspension for notice: " + noticeNo
            );
            return;
        }

        SuspendedNotice suspension = suspensions.get(0);

        // Verify suspension was created
        context.addVerification(
                "TS-PDP Suspension Created",
                true,
                "TS-PDP suspension exists",
                "Found",
                "TS-PDP suspension created successfully"
        );
    }

    /**
     * Verify NO TS-PDP suspension exists (for manual review cases)
     *
     * @param context Test context
     * @param noticeNo Notice number
     */
    public void verifyNoTsPdpSuspension(TestContext context, String noticeNo) {
        log.info("Verifying NO TS-PDP suspension - noticeNo: {}", noticeNo);

        List<SuspendedNotice> suspensions = suspendedNoticeRepository.findAll().stream()
                .filter(s -> noticeNo.equals(s.getNoticeNo())
                        && "TS".equals(s.getSuspensionType())
                        && "PDP".equals(s.getReasonOfSuspension()))
                .collect(java.util.stream.Collectors.toList());

        boolean noSuspension = suspensions.isEmpty();
        context.addVerification(
                "No TS-PDP Suspension",
                noSuspension,
                "No suspension",
                noSuspension ? "No suspension" : "Suspension found",
                noSuspension ? "Correctly no suspension created" : "Unexpected suspension created"
        );
    }
}
