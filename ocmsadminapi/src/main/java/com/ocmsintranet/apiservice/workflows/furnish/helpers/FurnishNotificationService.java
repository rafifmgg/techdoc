package com.ocmsintranet.apiservice.workflows.furnish.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecords;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecords;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecordsRepository;
import com.ocmsintranet.apiservice.utilities.SmsClient;
import com.ocmsintranet.apiservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.apiservice.utilities.emailutility.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Service for sending email and SMS notifications for OCMS 41 Furnish workflow.
 * Handles both sending and recording notifications in database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishNotificationService {

    private final EmailService emailService;
    private final SmsClient smsClient;
    private final OcmsEmailNotificationRecordsRepository emailNotificationRepository;
    private final OcmsSmsNotificationRecordsRepository smsNotificationRepository;

    /**
     * Send email and record in database
     *
     * @param noticeNo Notice number
     * @param processingStage Current processing stage
     * @param emailAddr Recipient email address
     * @param subject Email subject
     * @param htmlContent Email HTML content
     * @return true if email sent successfully
     */
    @Transactional
    public boolean sendAndRecordEmail(String noticeNo, String processingStage,
                                     String emailAddr, String subject, String htmlContent) {
        log.info("Sending email for notice: {}, stage: {}, to: {}", noticeNo, processingStage, emailAddr);

        // Create email request
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(emailAddr);
        emailRequest.setSubject(subject);
        emailRequest.setHtmlContent(htmlContent);

        // Send email
        boolean sent = emailService.sendEmail(emailRequest);

        // Record in database
        OcmsEmailNotificationRecords record = new OcmsEmailNotificationRecords();
        record.setNoticeNo(noticeNo);
        record.setProcessingStage(processingStage);
        record.setEmailAddr(emailAddr);
        record.setSubject(subject);
        record.setContent(htmlContent.getBytes(StandardCharsets.UTF_8));
        record.setStatus(sent ? "S" : "F"); // S=Success, F=Failed
        record.setDateSent(sent ? LocalDateTime.now() : null);

        if (!sent) {
            record.setMsgError("Failed to send email via EmailService");
        }

        emailNotificationRepository.save(record);

        log.info("Email notification recorded - Notice: {}, Status: {}", noticeNo, record.getStatus());
        return sent;
    }

    /**
     * Send SMS and record in database
     *
     * @param noticeNo Notice number
     * @param processingStage Current processing stage
     * @param mobileCode Mobile country code (e.g., "65")
     * @param mobileNo Mobile number
     * @param content SMS content
     * @return true if SMS sent successfully
     */
    @Transactional
    public boolean sendAndRecordSms(String noticeNo, String processingStage,
                                   String mobileCode, String mobileNo, String content) {
        log.info("Sending SMS for notice: {}, stage: {}, to: +{}{}",
                noticeNo, processingStage, mobileCode, mobileNo);

        // Send SMS via cron service
        boolean sent = smsClient.sendSms(mobileCode, mobileNo, content, "english");

        // Record in database
        OcmsSmsNotificationRecords record = new OcmsSmsNotificationRecords();
        record.setNoticeNo(noticeNo);
        record.setProcessingStage(processingStage);
        record.setMobileCode(mobileCode);
        record.setMobileNo(mobileNo);
        record.setContent(content.getBytes(StandardCharsets.UTF_8));
        record.setStatus(sent ? "S" : "F"); // S=Success, F=Failed
        record.setDateSent(sent ? LocalDateTime.now() : null);

        if (!sent) {
            record.setMsgError("Failed to send SMS via cron service");
        }

        smsNotificationRepository.save(record);

        log.info("SMS notification recorded - Notice: {}, Status: {}", noticeNo, record.getStatus());
        return sent;
    }

    /**
     * Send email to multiple recipients and record each
     *
     * @param noticeNo Notice number
     * @param processingStage Current processing stage
     * @param emailAddresses List of email addresses (comma-separated)
     * @param subject Email subject
     * @param htmlContent Email HTML content
     * @return Number of successfully sent emails
     */
    @Transactional
    public int sendAndRecordBulkEmail(String noticeNo, String processingStage,
                                     String emailAddresses, String subject, String htmlContent) {
        if (emailAddresses == null || emailAddresses.trim().isEmpty()) {
            log.warn("No email addresses provided for notice: {}", noticeNo);
            return 0;
        }

        String[] emails = emailAddresses.split("[,;]");
        int successCount = 0;

        for (String email : emails) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.isEmpty()) {
                boolean sent = sendAndRecordEmail(noticeNo, processingStage, trimmedEmail, subject, htmlContent);
                if (sent) {
                    successCount++;
                }
            }
        }

        log.info("Bulk email sent - Total: {}, Success: {}", emails.length, successCount);
        return successCount;
    }
}
