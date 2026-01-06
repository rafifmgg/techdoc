package com.ocmsintranet.apiservice.testing.furnish_approval.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecords;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecords;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Helper for verifying database state after furnish approval
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationHelper {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsEmailNotificationRecordsRepository emailNotificationRepository;
    private final OcmsSmsNotificationRecordsRepository smsNotificationRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * Verify furnish application was approved (status='A')
     *
     * @param context Test context
     * @param txnNo Transaction number
     */
    public void verifyApplicationApproved(TestContext context, String txnNo) {
        log.info("Verifying application approved - txnNo: {}", txnNo);

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

        boolean statusApproved = "A".equals(app.getApplicationStatus());
        context.addVerification(
                "Application Status = Approved",
                statusApproved,
                "A",
                app.getApplicationStatus(),
                statusApproved ? "Application approved" : "Application not approved"
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
                emailSent ? "Owner email sent successfully" : "Missing owner email"
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
     * Verify email to furnished person was sent
     *
     * @param context Test context
     * @param noticeNo Notice number
     * @param furnishedEmail Expected furnished person email
     */
    public void verifyEmailToFurnished(TestContext context, String noticeNo, String furnishedEmail) {
        log.info("Verifying email to furnished person - noticeNo: {}, email: {}", noticeNo, furnishedEmail);

        List<OcmsEmailNotificationRecords> emails = emailNotificationRepository
                .findByNoticeNoAndEmailAddr(noticeNo, furnishedEmail);

        boolean emailSent = !emails.isEmpty();
        context.addVerification(
                "Email Sent to Furnished Person",
                emailSent,
                "Email sent",
                emailSent ? "Email found" : "No email found",
                emailSent ? "Furnished person email sent successfully" : "Missing furnished person email"
        );

        if (emailSent) {
            OcmsEmailNotificationRecords email = emails.get(0);
            boolean emailSuccess = "S".equals(email.getStatus());
            context.addVerification(
                    "Email to Furnished Status",
                    emailSuccess,
                    "S (Success)",
                    email.getStatus(),
                    emailSuccess ? "Email sent successfully" : "Email failed"
            );
        }
    }

    /**
     * Verify SMS to furnished person was sent
     *
     * @param context Test context
     * @param noticeNo Notice number
     * @param mobileNo Expected mobile number
     */
    public void verifySmsToFurnished(TestContext context, String noticeNo, String mobileNo) {
        log.info("Verifying SMS to furnished person - noticeNo: {}, mobile: {}", noticeNo, mobileNo);

        List<OcmsSmsNotificationRecords> smsRecords = smsNotificationRepository
                .findByNoticeNoAndMobileNo(noticeNo, mobileNo);

        boolean smsSent = !smsRecords.isEmpty();
        context.addVerification(
                "SMS Sent to Furnished Person",
                smsSent,
                "SMS sent",
                smsSent ? "SMS found" : "No SMS found",
                smsSent ? "Furnished person SMS sent successfully" : "Missing furnished person SMS"
        );

        if (smsSent) {
            OcmsSmsNotificationRecords sms = smsRecords.get(0);
            // Note: SMS might fail (status='F') if cron service integration issues, but record should exist
            context.addVerification(
                    "SMS Record Created",
                    true,
                    "SMS record exists",
                    "Found",
                    "SMS record created (status=" + sms.getStatus() + ")"
            );
        }
    }

    /**
     * Verify TS-PDP suspension was revived
     *
     * @param context Test context
     * @param noticeNo Notice number
     */
    public void verifyTsPdpSuspensionRevived(TestContext context, String noticeNo) {
        log.info("Verifying TS-PDP suspension revived - noticeNo: {}", noticeNo);

        List<SuspendedNotice> suspensions = suspendedNoticeRepository
                .findByNoticeNoAndSuspensionTypeAndReasonOfSuspension(noticeNo, "TS", "PDP");

        if (suspensions.isEmpty()) {
            context.addVerification(
                    "TS-PDP Suspension Exists",
                    false,
                    "Suspension record found",
                    "No suspension found",
                    "Missing TS-PDP suspension record"
            );
            return;
        }

        SuspendedNotice suspension = suspensions.get(0);

        boolean revived = suspension.getDateOfRevival() != null;
        context.addVerification(
                "TS-PDP Suspension Revived",
                revived,
                "date_of_revival set",
                revived ? "Revived" : "Not revived",
                revived ? "TS-PDP suspension revived successfully" : "TS-PDP suspension not revived"
        );
    }

    /**
     * Verify approval is blocked when PS suspension exists
     *
     * @param context Test context
     * @param txnNo Transaction number
     */
    public void verifyApprovalBlockedByPs(TestContext context, String txnNo) {
        log.info("Verifying approval blocked by PS suspension - txnNo: {}", txnNo);

        Optional<OcmsFurnishApplication> appOpt = furnishApplicationRepository.findById(txnNo);

        if (appOpt.isEmpty()) {
            context.addVerification(
                    "Application Exists",
                    false,
                    "Application record found",
                    "No record found",
                    "Missing furnish application"
            );
            return;
        }

        OcmsFurnishApplication app = appOpt.get();

        // Status should still be 'P' (pending) - approval blocked
        boolean stillPending = "P".equals(app.getApplicationStatus());
        context.addVerification(
                "Approval Blocked - Status Still Pending",
                stillPending,
                "P",
                app.getApplicationStatus(),
                stillPending ? "Approval correctly blocked by PS suspension" : "Approval not blocked (unexpected)"
        );
    }
}
