package com.ocmsintranet.cronservice.framework.workflows.paymentReport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers.PaymentExceptionReportHelper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payment Exception Report Generation Job.
 * This job performs the following steps:
 * 1. Query database for payment exceptions (PS-PRA, TS-PAM, refund notices)
 * 2. Generate HTML email body with the exception records
 * 3. Send the report via email (different recipients based on success/failure)
 *
 * The job extends TrackedCronJobTemplate to automatically track job execution history
 * in the database. This allows for monitoring and auditing of job runs.
 *
 * Technical Details:
 * - Queries for notices with:
 *   a. PS-PRA: Permanent Suspension - Payment Refund Approved
 *   b. TS-PAM: Temporary Suspension - Payment Amount Mismatch
 *   c. Refund notices created on the report date
 * - Sends HTML email with exception records in table format
 * - Success case: Email to OIC (whether records exist or not)
 * - Failure case: Email to support team (MGG, ISG, OCMS Users) only on errors
 *
 * Dependencies:
 * - PaymentExceptionReportHelper: For data fetching and HTML generation
 * - EmailService: For sending reports via email
 */
@Slf4j
@Component
public class PaymentExceptionReportJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String JOB_NAME = "generate_payment_exception_report";

    @Value("${email.report.paymentreport.recipients:}")
    private String oicRecipients;

    @Value("${email.report.paymentreport.support.recipients:}")
    private String supportRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;


    // Dependencies
    private final PaymentExceptionReportHelper reportHelper;
    private final EmailService emailService;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    // Report date for the current execution
    private String reportDate;

    public PaymentExceptionReportJob(
            PaymentExceptionReportHelper reportHelper,
            EmailService emailService) {
        this.reportHelper = reportHelper;
        this.emailService = emailService;
    }

    /**
     * Set the report date for this job execution
     * This allows overriding the default yesterday's date
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
        log.info("Report date manually set to: {}", reportDate);
    }

    /**
     * Records job metadata for tracking and reporting
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    private void recordMetadata(String key, String value) {
        jobMetadata.put(key, value);
        log.debug("Recorded metadata: {}={}", key, value);
    }

    /**
     * Execute the report generation job for a specific date
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return true if the job executed successfully
     */
    public boolean execute(String reportDate) {
        log.info("Starting payment exception report generation job for date: {}", reportDate);

        // Reset metadata for this execution
        jobMetadata = new HashMap<>();
        recordMetadata("reportDate", reportDate);
        recordMetadata("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Query database for payment exceptions
            log.info("Querying database for payment exceptions");
            Map<String, Object> reportData = reportHelper.fetchExceptionRecords(reportDate);

            if (reportData == null || reportData.isEmpty()) {
                log.error("Failed to fetch exception records - reportData is null or empty");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Failed to fetch exception records");
                sendFailureEmail(reportDate, "Failed to fetch exception records from database");
                return false;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) reportData.get("records");
            int totalCount = (Integer) reportData.getOrDefault("totalCount", 0);

            recordMetadata("recordCount", String.valueOf(totalCount));

            // Step 2: Generate HTML email body
            log.info("Generating HTML email body");
            String emailBody = reportHelper.generateEmailBody(records, reportDate);

            if (emailBody == null || emailBody.isEmpty()) {
                log.error("Failed to generate email body");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Failed to generate email body");
                sendFailureEmail(reportDate, "Failed to generate email body");
                return false;
            }

            // Step 3: Send the report via email to OIC (always)
            log.info("Sending report via email to OIC: {}", oicRecipients);

            if (oicRecipients == null || oicRecipients.isEmpty()) {
                log.error("No OIC email recipients configured. Report not sent.");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "No OIC email recipients configured");
                sendFailureEmail(reportDate, "No OIC email recipients configured");
                return false;
            }

            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            // Add environment prefix to email subject
            String subject = "[" + environment.toUpperCase() + "] - OCMS Payment Exception Report - " + reportDate;
            if (totalCount > 0) {
                subject += " (" + totalCount + " exceptions found)";
            }
            emailRequest.setSubject(subject);
            emailRequest.setHtmlContent(emailBody);

            // Send email
            boolean emailSent = emailService.sendEmail(emailRequest);

            if (!emailSent) {
                log.error("Failed to send report via email to OIC");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Email sending failed");
                sendFailureEmail(reportDate, "Failed to send email to OIC");
                return false;
            }

            recordMetadata("emailRecipients", oicRecipients);
            recordMetadata("status", "COMPLETED");
            recordMetadata("endTime", LocalDateTime.now().toString());

            log.info("Payment exception report generation job completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error executing payment exception report generation job", e);
            recordMetadata("status", "FAILED");
            recordMetadata("error", e.getMessage());
            sendFailureEmail(reportDate, "Exception occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send failure notification email to support team
     *
     * @param reportDate The report date
     * @param errorMessage The error message
     */
    private void sendFailureEmail(String reportDate, String errorMessage) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("No support email recipients configured. Failure notification not sent.");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2 style='color: red;'>OCMS Payment Exception Report - FAILED</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
            html.append("<p><strong>Error:</strong> ").append(errorMessage).append("</p>");
            html.append("<hr/>");
            html.append("<p>Please investigate and resolve the issue.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated notification. Please contact the development team if assistance is needed.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Payment Exception Report - FAILED - " + reportDate);
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("Failure notification email sent to support team: {}", supportRecipients);

        } catch (Exception e) {
            log.error("Failed to send failure notification email", e);
        }
    }

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for payment exception report generation job");
        try {
            // Check if required dependencies are initialized
            if (reportHelper == null) {
                log.error("PaymentExceptionReportHelper is not initialized");
                return false;
            }

            if (emailService == null) {
                log.error("EmailService is not initialized");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating pre-conditions", e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        log.info("Initializing payment exception report generation job");

        // Set report date to yesterday by default if not already set
        if (reportDate == null || reportDate.isEmpty()) {
            reportDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("Report date set to default (yesterday): {}", reportDate);
        } else {
            log.info("Using manually set report date: {}", reportDate);
        }

        // Call super.initialize() to record job start in history
        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("Executing payment exception report generation job");

        try {
            boolean result = execute(reportDate);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "Payment exception report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "Payment exception report generation failed");
            }
        } catch (Exception e) {
            log.error("Error executing payment exception report generation job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up payment exception report generation job");
        // No specific cleanup needed
    }
}
