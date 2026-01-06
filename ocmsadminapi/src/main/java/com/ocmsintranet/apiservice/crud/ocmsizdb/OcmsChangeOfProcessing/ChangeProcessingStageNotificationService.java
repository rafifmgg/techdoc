package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ChangeOfProcessingResponse;
import com.ocmsintranet.apiservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.apiservice.utilities.emailutility.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending email notifications for Change Processing Stage operations
 * Based on OCMS CPS Spec §2.5.1 Step 9
 *
 * Sends error notifications to MGG, ISG, and OCMS users when batch operations fail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeProcessingStageNotificationService {

    private final EmailService emailService;
    private final ExecutorService executorService;

    @Value("${email.cps.recipients.mgg:mgg@example.com}")
    private String mggEmail;

    @Value("${email.cps.recipients.isg:isg@example.com}")
    private String isgEmail;

    @Value("${email.cps.recipients.ocms:ocms@example.com}")
    private String ocmsEmail;

    @Value("${email.cps.subject.prefix:OCMS Change Processing Stage Error}")
    private String emailSubjectPrefix;

    /**
     * Send error notification email asynchronously
     * Based on OCMS CPS Spec §2.5.1 Step 9
     *
     * @param batchDate Date of the batch processing
     * @param userId User who submitted the batch
     * @param totalRequested Total notices requested
     * @param failed Number of failed notices
     * @param results List of all results (to extract error details)
     * @return CompletableFuture<Boolean> indicating if email was sent
     */
    public CompletableFuture<Boolean> sendErrorNotificationAsync(
            LocalDate batchDate,
            String userId,
            int totalRequested,
            int failed,
            List<ChangeOfProcessingResponse.NoticeResult> results) {

        return CompletableFuture.supplyAsync(() -> {
            return sendErrorNotification(batchDate, userId, totalRequested, failed, results);
        }, executorService);
    }

    /**
     * Send error notification email synchronously
     * Based on OCMS CPS Spec §2.5.1 Step 9
     *
     * @param batchDate Date of the batch processing
     * @param userId User who submitted the batch
     * @param totalRequested Total notices requested
     * @param failed Number of failed notices
     * @param results List of all results (to extract error details)
     * @return true if email was sent successfully
     */
    public boolean sendErrorNotification(
            LocalDate batchDate,
            String userId,
            int totalRequested,
            int failed,
            List<ChangeOfProcessingResponse.NoticeResult> results) {

        try {
            log.info("Preparing to send CPS error notification email for batch on {}", batchDate);

            // Build recipient list (MGG, ISG, OCMS)
            String recipients = buildRecipientList();

            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(recipients);
            emailRequest.setSubject(buildEmailSubject(batchDate, failed));

            // Build HTML content
            String htmlContent = buildErrorEmailContent(batchDate, userId, totalRequested, failed, results);
            emailRequest.setHtmlContent(htmlContent);

            // Send email
            boolean sent = emailService.sendEmail(emailRequest);

            if (sent) {
                log.info("CPS error notification email sent successfully to: {}", recipients);
            } else {
                log.error("Failed to send CPS error notification email");
            }

            return sent;

        } catch (Exception e) {
            log.error("Error sending CPS notification email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build recipient list (MGG, ISG, OCMS)
     * Combines all recipient emails into comma-separated list
     */
    private String buildRecipientList() {
        StringBuilder recipients = new StringBuilder();

        if (mggEmail != null && !mggEmail.isEmpty() && !mggEmail.equals("mgg@example.com")) {
            recipients.append(mggEmail);
        }

        if (isgEmail != null && !isgEmail.isEmpty() && !isgEmail.equals("isg@example.com")) {
            if (recipients.length() > 0) recipients.append(",");
            recipients.append(isgEmail);
        }

        if (ocmsEmail != null && !ocmsEmail.isEmpty() && !ocmsEmail.equals("ocms@example.com")) {
            if (recipients.length() > 0) recipients.append(",");
            recipients.append(ocmsEmail);
        }

        // Fallback to a default if no valid recipients configured
        if (recipients.length() == 0) {
            log.warn("No valid email recipients configured for CPS notifications, using admin email");
            return mggEmail; // Use first email as fallback
        }

        return recipients.toString();
    }

    /**
     * Build email subject line
     */
    private String buildEmailSubject(LocalDate batchDate, int failed) {
        return String.format("%s - %d Failed Notices on %s",
                emailSubjectPrefix,
                failed,
                batchDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    /**
     * Build HTML email content with error details
     * Based on OCMS CPS Spec §2.5.1 Step 9
     */
    private String buildErrorEmailContent(
            LocalDate batchDate,
            String userId,
            int totalRequested,
            int failed,
            List<ChangeOfProcessingResponse.NoticeResult> results) {

        StringBuilder html = new StringBuilder();

        // Start HTML
        html.append("<html><body style='font-family: Arial, sans-serif;'>");

        // Header
        html.append("<h2 style='color: #d32f2f;'>OCMS Change Processing Stage Error Notification</h2>");

        // Summary section
        html.append("<h3>Batch Processing Summary</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%; max-width: 600px;'>");
        html.append("<tr><th style='background-color: #f5f5f5; text-align: left;'>Field</th><th style='background-color: #f5f5f5; text-align: left;'>Value</th></tr>");
        html.append("<tr><td>Date of Processing</td><td>").append(batchDate).append("</td></tr>");
        html.append("<tr><td>Submitted By</td><td>").append(escapeHtml(userId)).append("</td></tr>");
        html.append("<tr><td>Total Notices Requested</td><td>").append(totalRequested).append("</td></tr>");
        html.append("<tr><td style='color: #d32f2f; font-weight: bold;'>Failed Notices</td><td style='color: #d32f2f; font-weight: bold;'>").append(failed).append("</td></tr>");
        html.append("<tr><td>Timestamp</td><td>").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</td></tr>");
        html.append("</table>");

        // Error details section
        html.append("<h3 style='margin-top: 30px;'>Failed Notice Details</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%;'>");
        html.append("<tr style='background-color: #f5f5f5;'>");
        html.append("<th>Notice No</th>");
        html.append("<th>Error Code</th>");
        html.append("<th>Error Message</th>");
        html.append("</tr>");

        // Extract failed notices
        int errorCount = 0;
        for (ChangeOfProcessingResponse.NoticeResult result : results) {
            if ("FAILED".equals(result.getOutcome()) || "WARNING".equals(result.getOutcome())) {
                errorCount++;
                html.append("<tr>");
                html.append("<td>").append(escapeHtml(result.getNoticeNo())).append("</td>");
                html.append("<td>").append(escapeHtml(result.getCode())).append("</td>");
                html.append("<td>").append(escapeHtml(result.getMessage())).append("</td>");
                html.append("</tr>");

                // Limit to 50 errors to prevent email size issues
                if (errorCount >= 50) {
                    int remaining = failed - 50;
                    if (remaining > 0) {
                        html.append("<tr><td colspan='3' style='text-align: center; font-style: italic; background-color: #fff9c4;'>");
                        html.append("... and ").append(remaining).append(" more errors (view full details in system logs)");
                        html.append("</td></tr>");
                    }
                    break;
                }
            }
        }

        html.append("</table>");

        // Footer
        html.append("<p style='margin-top: 30px; font-size: 12px; color: #666;'>");
        html.append("This is an automated notification from the OCMS Change Processing Stage system.<br>");
        html.append("Please review the errors and take appropriate action.<br>");
        html.append("For assistance, contact the OCMS development team.");
        html.append("</p>");

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Escape HTML special characters to prevent rendering issues
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
