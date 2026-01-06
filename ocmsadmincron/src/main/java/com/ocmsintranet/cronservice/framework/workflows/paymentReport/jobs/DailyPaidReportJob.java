package com.ocmsintranet.cronservice.framework.workflows.paymentReport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers.DailyPaidReportHelper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily Paid Report Generation Job.
 * This job performs the following steps:
 * 1. Query database for paid notices (PRA, FP reasons) for the specified date
 * 2. Generate Excel report with 6 sheets categorized by payment method
 * 3. Send the report via email with Excel attachment
 *
 * The job extends TrackedCronJobTemplate to automatically track job execution history
 * in the database. This allows for monitoring and auditing of job runs.
 *
 * Technical Details:
 * - Queries for notices with crs_reason_of_suspension IN ('PRA', 'FP')
 * - Generates Excel with 6 sheets: Summary, eService, AXS, Offline, JTC Collections, Refund Records
 * - Sends email with Excel attachment to OIC on success
 * - Sends failure notification to support team on errors
 *
 * Dependencies:
 * - DailyPaidReportHelper: For data fetching and Excel generation
 * - EmailService: For sending reports via email
 */
@Slf4j
@Component
public class DailyPaidReportJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String JOB_NAME = "generate_daily_paid_report";

    @Value("${email.report.paymentreport.recipients:}")
    private String oicRecipients;

    @Value("${email.report.paymentreport.support.recipients:}")
    private String supportRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    // Dependencies
    private final DailyPaidReportHelper reportHelper;
    private final EmailService emailService;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    // Report date for the current execution
    private String reportDate;

    public DailyPaidReportJob(
            DailyPaidReportHelper reportHelper,
            EmailService emailService) {
        this.reportHelper = reportHelper;
        this.emailService = emailService;
    }

    /**
     * Set the report date for this job execution
     * This allows overriding the default yesterday date
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
        log.info("Starting daily paid report generation job for date: {}", reportDate);

        // Reset metadata for this execution
        jobMetadata = new HashMap<>();
        recordMetadata("reportDate", reportDate);
        recordMetadata("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Query database for paid notices
            log.info("Querying database for paid notices");
            Map<String, Object> reportData = reportHelper.fetchPaidRecords(reportDate);

            if (reportData == null || reportData.isEmpty()) {
                log.error("Failed to fetch paid records - reportData is null or empty");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Failed to fetch paid records");
                sendFailureEmail(reportDate, "Failed to fetch paid records from database");
                return false;
            }

            int totalCount = (Integer) reportData.getOrDefault("totalCount", 0);
            int eServiceCount = (Integer) reportData.getOrDefault("eServiceCount", 0);
            int axsCount = (Integer) reportData.getOrDefault("axsCount", 0);
            int offlineCount = (Integer) reportData.getOrDefault("offlineCount", 0);
            int jtcCollectionCount = (Integer) reportData.getOrDefault("jtcCollectionCount", 0);
            int refundCount = (Integer) reportData.getOrDefault("refundCount", 0);

            recordMetadata("totalCount", String.valueOf(totalCount));
            recordMetadata("eServiceCount", String.valueOf(eServiceCount));
            recordMetadata("axsCount", String.valueOf(axsCount));
            recordMetadata("offlineCount", String.valueOf(offlineCount));
            recordMetadata("jtcCollectionCount", String.valueOf(jtcCollectionCount));
            recordMetadata("refundCount", String.valueOf(refundCount));

            if (totalCount == 0) {
                log.info("No paid records found for date: {}", reportDate);
                recordMetadata("status", "NO_DATA");
                // Still send email with "no data" message
                sendNoDataEmail(reportDate);
                return true;
            }

            // Step 2: Generate Excel report
            log.info("Generating Excel report");
            byte[] reportBytes = reportHelper.generateExcelReport(reportData, reportDate);

            if (reportBytes == null || reportBytes.length == 0) {
                log.error("Failed to generate Excel report");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Failed to generate Excel report");
                sendFailureEmail(reportDate, "Failed to generate Excel report");
                return false;
            }

            // Step 3: Send the report via email to OIC with Excel attachment
            String fileName = "OCMS_Daily_Paid_Report_" + reportDate.replace("-", "") + ".xlsx";

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
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Daily Paid Report - " + reportDate);
            emailRequest.setHtmlContent(createEmailBody(reportDate, totalCount, eServiceCount, axsCount,
                                                         offlineCount, jtcCollectionCount, refundCount));

            // Add Excel attachment
            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(reportBytes);

            List<EmailRequest.Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            emailRequest.setAttachments(attachments);

            // Send email
            boolean emailSent = emailService.sendEmail(emailRequest);

            if (!emailSent) {
                log.error("Failed to send report via email to OIC");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Email sending failed");
                sendFailureEmail(reportDate, "Failed to send email to OIC");
                return false;
            }

            recordMetadata("fileName", fileName);
            recordMetadata("emailRecipients", oicRecipients);
            recordMetadata("status", "COMPLETED");
            recordMetadata("endTime", LocalDateTime.now().toString());

            log.info("Daily paid report generation job completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error executing daily paid report generation job", e);
            recordMetadata("status", "FAILED");
            recordMetadata("error", e.getMessage());
            sendFailureEmail(reportDate, "Exception occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send email when no data is found
     */
    private void sendNoDataEmail(String reportDate) {
        try {
            if (oicRecipients == null || oicRecipients.isEmpty()) {
                log.warn("No OIC email recipients configured. No data notification not sent.");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2>OCMS Daily Paid Report</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
            html.append("<hr/>");
            html.append("<p style='color: green;'>No paid records found for this date.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated report.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Daily Paid Report - " + reportDate + " (No Data)");
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("No data notification email sent to OIC: {}", oicRecipients);

        } catch (Exception e) {
            log.error("Failed to send no data notification email", e);
        }
    }

    /**
     * Send failure notification email to support team
     */
    private void sendFailureEmail(String reportDate, String errorMessage) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("No support email recipients configured. Failure notification not sent.");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2 style='color: red;'>OCMS Daily Paid Report - FAILED</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
            html.append("<p><strong>Error:</strong> ").append(errorMessage).append("</p>");
            html.append("<hr/>");
            html.append("<p>Please investigate and resolve the issue.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated notification.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Daily Paid Report - FAILED - " + reportDate);
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("Failure notification email sent to support team: {}", supportRecipients);

        } catch (Exception e) {
            log.error("Failed to send failure notification email", e);
        }
    }

    /**
     * Create HTML email body with summary statistics
     */
    private String createEmailBody(String reportDate, int totalCount, int eServiceCount, int axsCount,
                                     int offlineCount, int jtcCollectionCount, int refundCount) {
        LocalDate date = LocalDate.parse(reportDate);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"));

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>OCMS Daily Paid Report</h2>");
        html.append("<p><strong>Report Date:</strong> ").append(formattedDate).append("</p>");
        html.append("<hr/>");
        html.append("<h3>Summary</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><td><strong>Payment Channel</strong></td><td><strong>Count</strong></td></tr>");
        html.append("<tr><td>eService</td><td>").append(eServiceCount).append("</td></tr>");
        html.append("<tr><td>AXS</td><td>").append(axsCount).append("</td></tr>");
        html.append("<tr><td>Offline</td><td>").append(offlineCount).append("</td></tr>");
        html.append("<tr><td>JTC Collections</td><td>").append(jtcCollectionCount).append("</td></tr>");
        html.append("<tr><td>Refund Records</td><td>").append(refundCount).append("</td></tr>");
        html.append("<tr><td><strong>Total</strong></td><td><strong>").append(totalCount).append("</strong></td></tr>");
        html.append("</table>");
        html.append("<br/>");
        html.append("<p>Please find the detailed report in the attached Excel file.</p>");
        html.append("<p style='font-size: 11px; color: #666;'>This is an automated report.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for daily paid report generation job");
        try {
            if (reportHelper == null) {
                log.error("DailyPaidReportHelper is not initialized");
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
        log.info("Initializing daily paid report generation job");

        // Set report date to yesterday by default if not already set
        if (reportDate == null || reportDate.isEmpty()) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            reportDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("Report date set to default (yesterday): {}", reportDate);
        } else {
            log.info("Using manually set report date: {}", reportDate);
        }

        // Call super.initialize() to record job start in history
        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("Executing daily paid report generation job");

        try {
            boolean result = execute(reportDate);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "Daily paid report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "Daily paid report generation failed");
            }
        } catch (Exception e) {
            log.error("Error executing daily paid report generation job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up daily paid report generation job");
        // No specific cleanup needed
    }
}
