package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper class for sending COMCRYPT-related notifications.
 * Handles email notifications for errors during COMCRYPT operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptNotificationHelper {

    private final EmailService emailService;

    @Value("${email.report.recipients:}")
    private String comcryptSupportRecipients;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * COMCRYPT response codes
     */
    public static final String RESPONSE_CODE_SUCCESS = "HC200";
    public static final String RESPONSE_CODE_INTERNAL_ERROR = "HC500";
    public static final String RESPONSE_CODE_TIMEOUT = "HC408";

    /**
     * Send notification for COMCRYPT callback error.
     *
     * @param requestId    The request ID of the failed operation
     * @param responseCode The error response code (HC500 or HC408)
     * @param description  Description of the error
     * @param operationType The type of operation (ENCRYPTION or DECRYPTION)
     * @param fileName     The file name being processed
     * @param retryAttempt The retry attempt number
     */
    public void sendComcryptErrorNotification(String requestId, String responseCode, String description,
                                               String operationType, String fileName, int retryAttempt) {
        log.info("[COMCRYPT-NOTIFICATION] Sending error notification for requestId: {}, responseCode: {}",
                requestId, responseCode);

        if (comcryptSupportRecipients == null || comcryptSupportRecipients.isEmpty()) {
            log.warn("[COMCRYPT-NOTIFICATION] No support recipients configured. Skipping email notification.");
            return;
        }

        try {
            String subject = buildSubject(responseCode, operationType, activeProfile);
            String htmlContent = buildHtmlContent(requestId, responseCode, description, operationType, fileName, retryAttempt);

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(comcryptSupportRecipients);
            emailRequest.setSubject(subject);
            emailRequest.setHtmlContent(htmlContent);

            boolean sent = emailService.sendEmail(emailRequest);
            if (sent) {
                log.info("[COMCRYPT-NOTIFICATION] Error notification sent successfully for requestId: {}", requestId);
            } else {
                log.error("[COMCRYPT-NOTIFICATION] Failed to send error notification for requestId: {}", requestId);
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-NOTIFICATION] Error sending notification for requestId: {}: {}", requestId, e.getMessage(), e);
        }
    }

    /**
     * Build email subject based on response code
     */
    private String buildSubject(String responseCode, String operationType, String profile) {
        String errorType;
        switch (responseCode) {
            case RESPONSE_CODE_INTERNAL_ERROR:
                errorType = "Internal Server Error (HC500)";
                break;
            case RESPONSE_CODE_TIMEOUT:
                errorType = "Timeout Error (HC408)";
                break;
            default:
                errorType = "Error (" + responseCode + ")";
        }
        return String.format("[%s] OCMS COMCRYPT %s - %s", profile.toUpperCase(), operationType, errorType);
    }

    /**
     * Build HTML content for the notification email
     */
    private String buildHtmlContent(String requestId, String responseCode, String description,
                                    String operationType, String fileName, int retryAttempt) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String errorTypeDescription;
        String recommendedAction;

        switch (responseCode) {
            case RESPONSE_CODE_INTERNAL_ERROR:
                errorTypeDescription = "COMCRYPT service returned an internal server error.";
                recommendedAction = "Please check the COMCRYPT service status and logs. The operation has been retried but continues to fail.";
                break;
            case RESPONSE_CODE_TIMEOUT:
                errorTypeDescription = "COMCRYPT service request timed out.";
                recommendedAction = "Please check network connectivity and COMCRYPT service availability. The operation has been retried but continues to fail.";
                break;
            default:
                errorTypeDescription = "COMCRYPT service returned an unexpected error.";
                recommendedAction = "Please investigate the COMCRYPT service logs for more details.";
        }

        return new StringBuilder()
                .append("<html><body>")
                .append("<h2 style='color: #d32f2f;'>COMCRYPT Operation Error</h2>")
                .append("<p>").append(errorTypeDescription).append("</p>")
                .append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Timestamp</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(timestamp).append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Request ID</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(requestId != null ? requestId : "N/A").append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Response Code</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px; color: #d32f2f;'>").append(responseCode).append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Description</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(description != null ? description : "N/A").append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Operation Type</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(operationType != null ? operationType : "N/A").append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>File Name</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(fileName != null ? fileName : "N/A").append("</td></tr>")
                .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Retry Attempts</strong></td>")
                .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(retryAttempt).append("</td></tr>")
                .append("</table>")
                .append("<p style='margin-top: 20px;'><strong>Recommended Action:</strong></p>")
                .append("<p>").append(recommendedAction).append("</p>")
                .append("<hr style='margin-top: 20px;'>")
                .append("<p style='color: #666; font-size: 12px;'>This is an automated message from OCMS Intranet Cron Service.</p>")
                .append("</body></html>")
                .toString();
    }

    /**
     * Check if the response code indicates an error that requires notification
     */
    public boolean isErrorResponseCode(String responseCode) {
        return RESPONSE_CODE_INTERNAL_ERROR.equals(responseCode) || RESPONSE_CODE_TIMEOUT.equals(responseCode);
    }

    /**
     * Check if the response code indicates success
     */
    public boolean isSuccessResponseCode(String responseCode) {
        return RESPONSE_CODE_SUCCESS.equals(responseCode);
    }
}
