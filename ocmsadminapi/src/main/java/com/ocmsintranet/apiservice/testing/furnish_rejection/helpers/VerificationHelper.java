package com.ocmsintranet.apiservice.testing.furnish_rejection.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecords;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Helper for verifying database state after furnish rejection
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationHelper {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsEmailNotificationRecordsRepository emailNotificationRepository;

    /**
     * Verify furnish application was rejected (status='R')
     *
     * @param context Test context
     * @param txnNo Transaction number
     */
    public void verifyApplicationRejected(TestContext context, String txnNo) {
        log.info("Verifying application rejected - txnNo: {}", txnNo);

        Optional<OcmsFurnishApplication> appOpt = furnishApplicationRepository.findById(txnNo);

        if (appOpt.isEmpty()) {
            context.addVerification(
                    "Application Exists",
                    false,
                    "Application record found",
                    "No record found",
                    "Missing furnish application for txnNo: " + txnNo
            );
            return;
        }

        OcmsFurnishApplication app = appOpt.get();

        boolean statusRejected = "R".equals(app.getApplicationStatus());
        context.addVerification(
                "Application Status = Rejected",
                statusRejected,
                "R",
                app.getApplicationStatus(),
                statusRejected ? "Application rejected" : "Application not rejected"
        );

        context.getRuntimeData().put("furnishApplication", app);
    }

    /**
     * Verify email to owner was sent
     *
     * @param context Test context
     * @param noticeNo Notice number
     * @param ownerEmail Expected owner email
     */
    public void verifyEmailToOwner(TestContext context, String noticeNo, String ownerEmail) {
        log.info("Verifying email to owner - noticeNo: {}, email: {}", noticeNo, ownerEmail);

        List<OcmsEmailNotificationRecords> emails = emailNotificationRepository
                .findByNoticeNoAndEmailAddr(noticeNo, ownerEmail);

        boolean emailSent = !emails.isEmpty();
        context.addVerification(
                "Email Sent to Owner",
                emailSent,
                "Email sent",
                emailSent ? "Email found" : "No email found",
                emailSent ? "Owner rejection email sent successfully" : "Missing owner email"
        );

        if (emailSent) {
            OcmsEmailNotificationRecords email = emails.get(0);
            boolean emailSuccess = "S".equals(email.getStatus());
            context.addVerification(
                    "Email to Owner Status",
                    emailSuccess,
                    "S (Success)",
                    email.getStatus(),
                    emailSuccess ? "Email sent successfully" : "Email failed"
            );
        }
    }

    /**
     * Verify no email was sent (when sendEmailToOwner=false)
     *
     * @param context Test context
     * @param noticeNo Notice number
     */
    public void verifyNoEmailSent(TestContext context, String noticeNo) {
        log.info("Verifying no email sent - noticeNo: {}", noticeNo);

        List<OcmsEmailNotificationRecords> emails = emailNotificationRepository
                .findAll()
                .stream()
                .filter(email -> noticeNo.equals(email.getNoticeNo()))
                .toList();

        boolean noEmail = emails.isEmpty();
        context.addVerification(
                "No Email Sent",
                noEmail,
                "No email",
                noEmail ? "No email" : "Email found",
                noEmail ? "Correctly no email sent" : "Unexpected email sent"
        );
    }

    /**
     * Verify rejection reason in remarks
     *
     * @param context Test context
     * @param txnNo Transaction number
     * @param expectedReason Expected rejection reason
     */
    public void verifyRejectionReason(TestContext context, String txnNo, String expectedReason) {
        log.info("Verifying rejection reason - txnNo: {}", txnNo);

        Optional<OcmsFurnishApplication> appOpt = furnishApplicationRepository.findById(txnNo);

        if (appOpt.isEmpty()) {
            context.addVerification(
                    "Rejection Reason Check",
                    false,
                    "Remarks contains reason",
                    "No application found",
                    "Cannot verify rejection reason"
            );
            return;
        }

        OcmsFurnishApplication app = appOpt.get();
        String remarks = app.getRemarks();

        boolean hasReason = remarks != null && remarks.contains(expectedReason);
        context.addVerification(
                "Rejection Reason in Remarks",
                hasReason,
                "Contains: " + expectedReason,
                remarks != null ? remarks : "null",
                hasReason ? "Rejection reason recorded" : "Rejection reason missing"
        );
    }
}
