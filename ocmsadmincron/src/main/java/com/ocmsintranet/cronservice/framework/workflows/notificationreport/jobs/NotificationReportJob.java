package com.ocmsintranet.cronservice.framework.workflows.notificationreport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.notificationreport.helpers.NotificationReportHelper;
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
 * eNotification Report Generation Job.
 * This job performs the following steps:
 * 1. Query database for notification records from SMS and email tables
 * 2. Generate Excel report with multiple sheets (Summary, Error, Success)
 * 3. Send the report via email as an attachment
 *
 * The job extends TrackedCronJobTemplate to automatically track job execution history
 * in the database. This allows for monitoring and auditing of job runs.
 *
 * Technical Details:
 * - Queries SMS and email notification tables using UNION to combine records
 * - Joins with owner/driver information for complete details
 * - Generates an Excel report with three sheets:
 *   a. Summary: Contains statistics by processing stage (ENA, RD1, RD2, etc.)
 *   b. Success: Lists all successful notifications (11 columns)
 *   c. Error: Lists failed notifications with error messages (12 columns)
 * - Sends the report via email with naming convention: ReportENotificationSent_YYYYMMDD.xlsx
 * - Records metadata about the job execution for tracking and troubleshooting
 *
 * Dependencies:
 * - NotificationReportHelper: For data processing and Excel generation
 * - EmailService: For sending reports via email
 */
@Slf4j
@Component
public class NotificationReportJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String JOB_NAME = "generate_ena_daily_summary_rpt";
    
    @Value("${email.report.recipients:}")
    private String emailRecipients;
    
    @Value("${email.report.cc:}")
    private String emailCc;
    
    @Value("${spring.profiles.active:unknown}")
    private String environment;
    
    // Dependencies
    private final NotificationReportHelper notificationReportHelper;
    private final EmailService emailService;
    
    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();
    
    // Report date for the current execution
    private String reportDate;

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

    public NotificationReportJob(
            NotificationReportHelper notificationReportHelper,
            EmailService emailService) {
        this.notificationReportHelper = notificationReportHelper;
        this.emailService = emailService;
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
        log.info("Starting eNotification report generation job for date: {}", reportDate);

        // Reset metadata for this execution
        jobMetadata = new HashMap<>();
        recordMetadata("reportDate", reportDate);
        recordMetadata("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Query database for notification records
            log.info("Querying database for notification records");
            Map<String, Object> reportData = notificationReportHelper.fetchNotificationReportsData(reportDate);
            
            if (reportData == null || reportData.isEmpty()) {
                log.info("No notification data found for date: {}", reportDate);
                recordMetadata("status", "NO_DATA");
                return true;
            }

            recordMetadata("recordCount", String.valueOf(reportData.size()));

            // Check if there are error records - only proceed if errors exist
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errorRecords =
                (List<Map<String, Object>>) reportData.getOrDefault("errorRecords", new ArrayList<>());

            if (errorRecords.isEmpty()) {
                log.info("No error records found for date: {}. Excel generation and email sending skipped.", reportDate);
                recordMetadata("status", "NO_ERRORS_NO_REPORT");
                recordMetadata("errorCount", "0");
                return true; // Job successful but no report generated
            }

            log.info("Found {} error records. Proceeding with Excel generation and email sending.", errorRecords.size());
            recordMetadata("errorCount", String.valueOf(errorRecords.size()));

            // Step 2: Generate Excel report (only when errors exist)
            log.info("Generating Excel report");
            byte[] reportBytes = notificationReportHelper.generateExcelReport(reportData, reportDate);

            if (reportBytes == null || reportBytes.length == 0) {
                log.error("Failed to generate Excel report");
                recordMetadata("status", "FAILED");
                return false;
            }
            
            // Step 3: Send the report via email
            String fileName = "ReportENotification_" + reportDate.replace("-", "") + ".xlsx";
            
            log.info("Sending report via email to: {}", emailRecipients);
            
            if (emailRecipients == null || emailRecipients.isEmpty()) {
                log.error("No email recipients configured. Report not sent.");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "No email recipients configured");
                return false;
            }
            
            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setCc(emailCc);
            // Add environment prefix to email subject
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS eNotification Report - " + reportDate);
            emailRequest.setHtmlContent(createEmailBody(reportDate));
            
            // Add attachment
            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(reportBytes);
            
            List<EmailRequest.Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            emailRequest.setAttachments(attachments);
            
            // Send email
            boolean emailSent = emailService.sendEmail(emailRequest);
            
            if (!emailSent) {
                log.error("Failed to send report via email");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Email sending failed");
                return false;
            }
            
            recordMetadata("fileName", fileName);
            recordMetadata("emailRecipients", emailRecipients);
            recordMetadata("status", "COMPLETED");
            recordMetadata("endTime", LocalDateTime.now().toString());
            
            log.info("eNotification report generation job completed successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Error executing eNotification report generation job", e);
            recordMetadata("status", "FAILED");
            return false;
        }
    }
        
    @Override
    protected String getJobName() {
        return JOB_NAME;
    }
    
    /**
     * Creates the HTML body for the email
     * 
     * @param reportDate The date for which the report was generated
     * @return HTML content for the email body
     */
    private String createEmailBody(String reportDate) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>eNotification Report</h2>");
        html.append("<p>Please find attached the eNotification Report for ").append(reportDate).append(".</p>");
        html.append("<p>This report includes SMS and email notifications sent across all processing stages.</p>");
        html.append("<p>The report contains the following sheets:</p>");
        html.append("<ul>");
        html.append("<li><strong>Summary</strong>: Contains statistics by processing stage (ENA, RD1, RD2, RR3, DN1, DN2, DR3)</li>");
        html.append("<li><strong>Success</strong>: Lists all successful notifications (11 columns)</li>");
        html.append("<li><strong>Error</strong>: Lists failed notifications with error messages (12 columns)</li>");
        html.append("</ul>");
        html.append("<p>This is an automated email. Please do not reply.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for eNotification report generation job");
        try {
            // Check if required dependencies are initialized
            if (notificationReportHelper == null) {
                log.error("NotificationReportHelper is not initialized");
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
        log.info("Initializing eNotification report generation job");

        // Set report date to yesterday by default if not already set
        if (reportDate == null || reportDate.isEmpty()) {
            reportDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("Report date set to default (yesterday): {}", reportDate);
        } else {
            log.info("Using manually set report date: {}", reportDate);
        }

        // Call parent initialize to trigger batchjob tracking
        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("Executing eNotification report generation job");
        
        try {
            boolean result = execute(reportDate);
            
            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "eNotification report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "eNotification report generation failed");
            }
        } catch (Exception e) {
            log.error("Error executing eNotification report generation job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up eNotification report generation job");
        // No specific cleanup needed
    }
}
