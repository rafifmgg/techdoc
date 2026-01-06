package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.mha.report.helpers.MhaReportHelper;
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
 * MHA Suspended Notices Report Generation Job.
 * This job performs the following steps:
 * 1. Query database for suspended notices with TS/PS status and NRO/RP2 reason
 * 2. Generate Excel report with multiple sheets (Summary, Error, Success)
 * 3. Send the report via email as an attachment
 *
 * The job extends TrackedCronJobTemplate to automatically track job execution history
 * in the database. This allows for monitoring and auditing of job runs.
 *
 * Technical Details:
 * - Queries the OcmsSuspendedNotice table for records with suspension_type in ('TS', 'PS') and reason_of_suspension in ('NRO', 'RP2')
 * - Joins with related tables to get complete notice information
 * - Generates an Excel report with three sheets:
 *   a. Summary: Contains overall statistics (total records, success count, error count)
 *   b. Success: Lists all successfully processed records with complete details
 *   c. Error: Lists records with issues or missing required fields
 * - Sends the report via email with naming convention: SuspendedNotices_Report_YYYYMMDD.xlsx
 * - Records metadata about the job execution for tracking and troubleshooting
 *
 * Dependencies:
 * - TableQueryService: For database queries
 * - MhaReportHelper: For data processing and Excel generation
 * - EmailService: For sending reports via email
 */
@Slf4j
@Component
public class MhaReportJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String JOB_NAME = "generate_mha_daily_summary_rpt";
    
    @Value("${email.report.recipients:}")
    private String emailRecipients;
    
    @Value("${email.report.cc:}")
    private String emailCc;
    
    @Value("${spring.profiles.active:unknown}")
    private String environment;
    
    // Dependencies
    private final TableQueryService tableQueryService;
    private final MhaReportHelper mhaReportHelper;
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

    public MhaReportJob(
            TableQueryService tableQueryService,
            MhaReportHelper mhaReportHelper,
            EmailService emailService) {
        this.tableQueryService = tableQueryService;
        this.mhaReportHelper = mhaReportHelper;
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
        log.info("Starting MHA suspended notices report generation job for date: {}", reportDate);
        
        // Reset metadata for this execution
        jobMetadata = new HashMap<>();
        recordMetadata("reportDate", reportDate);
        recordMetadata("startTime", LocalDateTime.now().toString());
        
        try {
            // Step 1: Query database for suspended notices with TS/PS and NRO/RP2
            log.info("Querying database for suspended notices with TS/PS and NRO/RP2");
            Map<String, Object> reportData = mhaReportHelper.fetchSuspendedNoticesData(reportDate);
            
            if (reportData == null || reportData.isEmpty()) {
                log.info("No MHA data found for date: {}", reportDate);
                recordMetadata("status", "NO_DATA");
                return true;
            }
            
            recordMetadata("recordCount", String.valueOf(reportData.size()));
            
            // Step 2: Generate Excel report
            log.info("Generating Excel report");
            byte[] reportBytes = mhaReportHelper.generateExcelReport(reportData, reportDate);
            
            if (reportBytes == null || reportBytes.length == 0) {
                log.error("Failed to generate Excel report");
                recordMetadata("status", "FAILED");
                return false;
            }
            
            // Step 3: Send the report via email
            String fileName = "MHA_Report_" + reportDate.replace("-", "") + ".xlsx";
            
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
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS MHA Report - " + reportDate);
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
            
            log.info("MHA suspended notices report generation job completed successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Error executing MHA suspended notices report generation job", e);
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
        html.append("<h2>MHA Report</h2>");
        html.append("<p>Please find attached the MHA Report for ").append(reportDate).append(".</p>");
        html.append("<p>This report includes notices with suspension_type in ('TS', 'PS') and reason_of_suspension in ('NRO', 'RP2', 'RIP').</p>");
        html.append("<p>The report contains the following sheets:</p>");
        html.append("<ul>");
        html.append("<li><strong>Summary</strong>: Contains overall statistics (total records, success count, error count)</li>");
        html.append("<li><strong>Success</strong>: Lists all successfully processed records with complete details</li>");
        html.append("<li><strong>Error</strong>: Lists records with issues or missing required fields</li>");
        html.append("</ul>");
        html.append("<p>This is an automated email. Please do not reply.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for MHA suspended notices report generation job");
        try {
            // Check if required dependencies are initialized
            if (tableQueryService == null) {
                log.error("TableQueryService is not initialized");
                return false;
            }
            
            if (mhaReportHelper == null) {
                log.error("MhaReportHelper is not initialized");
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
        log.info("Initializing MHA suspended notices report generation job");

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
        log.info("Executing MHA suspended notices report generation job");
        
        try {
            boolean result = execute(reportDate);
            
            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "MHA suspended notices report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "MHA suspended notices report generation failed");
            }
        } catch (Exception e) {
            log.error("Error executing MHA suspended notices report generation job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up MHA suspended notices report generation job");
        // No specific cleanup needed
    }
}
