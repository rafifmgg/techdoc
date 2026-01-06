package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.helpers.ToppanReportHelper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Toppan Report File Retrieval Job.
 * This job performs the following steps:
 * 1. Check if Toppan report file exists in blob storage for the specified date
 * 2. Generate secure download URL for the file
 * 3. Send email notification with download link
 *
 * The job extends TrackedCronJobTemplate to automatically track job execution history
 * in the database. This allows for monitoring and auditing of job runs.
 *
 * Technical Details:
 * - Checks blob storage for pre-generated Toppan report files with naming convention: Toppan_Report_YYYYMMDD.xlsx
 * - Generates secure download URLs with configurable expiry time
 * - Sends email notification with download link instead of file attachment
 * - Records metadata about the job execution for tracking and troubleshooting
 *
 * Dependencies:
 * - ToppanReportHelper: For blob storage file operations and URL generation
 * - EmailService: For sending email notifications with download links
 */
@Slf4j
@Component
public class ToppanReportJob extends TrackedCronJobTemplate {

    // Constants for configuration
    private static final String JOB_NAME = "generate_toppan_daily_summary_rpt";
    
    @Value("${email.report.recipients:}")
    private String emailRecipients;
    
    @Value("${email.report.cc:}")
    private String emailCc;
    
    @Value("${spring.profiles.active:unknown}")
    private String environment;
    
    // Dependencies
    private final ToppanReportHelper toppanReportHelper;
    private final EmailService emailService;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    // Report date for the current execution
    private String reportDate;

    /**
     * Set the report date for this job execution
     * This allows overriding the default yesterday's date
     *
     * @param reportDate The date for which to check file availability (format: yyyy-MM-dd)
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
        log.info("Report date manually set to: {}", reportDate);
    }

    public ToppanReportJob(
            ToppanReportHelper toppanReportHelper,
            EmailService emailService) {
        this.toppanReportHelper = toppanReportHelper;
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
     * Execute the report file retrieval job for a specific date
     *
     * @param reportDate The date for which to retrieve the report file (format: yyyy-MM-dd)
     * @return true if the job executed successfully
     */
    public boolean execute(String reportDate) {
        log.info("Starting Toppan report files retrieval job for date: {}", reportDate);

        // Reset metadata for this execution
        jobMetadata = new HashMap<>();
        recordMetadata("reportDate", reportDate);
        recordMetadata("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Check if all 3 report files exist in blob storage
            log.info("Checking if Toppan report files exist for date: {}", reportDate);
            Map<String, Object> fileCheckResult = toppanReportHelper.checkReportFileExists(reportDate);

            boolean allFilesFound = (Boolean) fileCheckResult.getOrDefault("allFilesFound", false);
            int filesFound = (Integer) fileCheckResult.getOrDefault("foundCount", 0);

            @SuppressWarnings("unchecked")
            Map<String, String> foundFiles = (Map<String, String>) fileCheckResult.get("foundFiles");

            if (filesFound == 0) {
                log.warn("No Toppan report files found for date: {}", reportDate);
                recordMetadata("status", "FILES_NOT_FOUND");
                recordMetadata("filesFound", "0");
                // Still send email notification about missing files
                sendEmailNotification(reportDate, null, null);
                return true; // Job completed successfully even if files not found
            }

            recordMetadata("filesFound", String.valueOf(filesFound));
            recordMetadata("allFilesFound", String.valueOf(allFilesFound));

            // Step 2: Generate download URLs for all found files
            log.info("Generating download URLs for {} Toppan report files", filesFound);
            Map<String, String> downloadUrls = toppanReportHelper.generateDownloadUrls(reportDate);

            if (downloadUrls == null || downloadUrls.isEmpty()) {
                log.error("Failed to generate download URLs for files");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Download URL generation failed");
                return false;
            }

            recordMetadata("downloadUrlsGenerated", String.valueOf(downloadUrls.size()));

            // Log all found files and download URLs for verification
            logFileDetails(foundFiles, downloadUrls);

            // Step 3: Send email notification with download links
            log.info("Sending email notification with {} download links to: {}", downloadUrls.size(), emailRecipients);

            if (emailRecipients == null || emailRecipients.isEmpty()) {
                log.error("No email recipients configured. Notification not sent.");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "No email recipients configured");
                return false;
            }

            boolean emailSent = sendEmailNotification(reportDate, downloadUrls, foundFiles);

            if (!emailSent) {
                log.error("Failed to send email notification");
                recordMetadata("status", "FAILED");
                recordMetadata("error", "Email sending failed");
                return false;
            }

            recordMetadata("emailRecipients", emailRecipients);
            recordMetadata("status", "COMPLETED");
            recordMetadata("endTime", LocalDateTime.now().toString());

            log.info("Toppan report files retrieval job completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error executing Toppan report files retrieval job", e);
            recordMetadata("status", "FAILED");
            recordMetadata("error", e.getMessage());
            return false;
        }
    }

    /**
     * Log detailed information about found files and download URLs
     * This is useful for local testing when email sending is not available
     *
     * @param foundFiles Map of file type to file name
     * @param downloadUrls Map of file type to download URL
     */
    private void logFileDetails(Map<String, String> foundFiles, Map<String, String> downloadUrls) {
        log.info("========================================");
        log.info("TOPPAN REPORT - FILE DETAILS");
        log.info("========================================");
        log.info("Total files found: {}", foundFiles != null ? foundFiles.size() : 0);
        log.info("");

        if (foundFiles != null && !foundFiles.isEmpty()) {
            // Log PDF-D2 file
            if (foundFiles.containsKey("PDF_D2")) {
                log.info("1. PDF D2 Report:");
                log.info("   File Name: {}", foundFiles.get("PDF_D2"));
                log.info("   Download URL: {}", downloadUrls.get("PDF_D2"));
                log.info("");
            }

            // Log LOG-PDF file
            if (foundFiles.containsKey("LOG_PDF")) {
                log.info("2. LOG PDF Report:");
                log.info("   File Name: {}", foundFiles.get("LOG_PDF"));
                log.info("   Download URL: {}", downloadUrls.get("LOG_PDF"));
                log.info("");
            }

            // Log LOG-D2 file
            if (foundFiles.containsKey("LOG_D2")) {
                log.info("3. LOG D2 Report:");
                log.info("   File Name: {}", foundFiles.get("LOG_D2"));
                log.info("   Download URL: {}", downloadUrls.get("LOG_D2"));
                log.info("");
            }

            // Log RD2-D2 file
            if (foundFiles.containsKey("RD2_D2")) {
                log.info("4. RD2 D2 Report:");
                log.info("   File Name: {}", foundFiles.get("RD2_D2"));
                log.info("   Download URL: {}", downloadUrls.get("RD2_D2"));
                log.info("");
            }

            // Log DN2-D2 file
            if (foundFiles.containsKey("DN2_D2")) {
                log.info("5. DN2 D2 Report:");
                log.info("   File Name: {}", foundFiles.get("DN2_D2"));
                log.info("   Download URL: {}", downloadUrls.get("DN2_D2"));
                log.info("");
            }
        } else {
            log.info("No files found!");
        }

        log.info("========================================");
    }

    /**
     * Send email notification with download links or files not found message
     *
     * @param reportDate The report date
     * @param downloadUrls Map of file type to download URL (null if no files found)
     * @param fileNames Map of file type to file name (null if no files found)
     * @return true if email was sent successfully
     */
    private boolean sendEmailNotification(String reportDate, Map<String, String> downloadUrls, Map<String, String> fileNames) {
        try {
            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setCc(emailCc);

            // Add environment prefix to email subject
            String subject = "[" + environment.toUpperCase() + "] - OCMS Toppan Report - " + reportDate;
            emailRequest.setSubject(subject);

            // Create email body with download links or file not found message
            emailRequest.setHtmlContent(createEmailBody(reportDate, downloadUrls, fileNames));

            // Send email (no attachments needed)
            return emailService.sendEmail(emailRequest);

        } catch (Exception e) {
            log.error("Error sending email notification", e);
            return false;
        }
    }

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }
    
    /**
     * Creates the HTML body for the email with multiple download links
     *
     * @param reportDate The date for which the report was requested
     * @param downloadUrls Map of file type to download URL (null if no files found)
     * @param fileNames Map of file type to file name (null if no files found)
     * @return HTML content for the email body
     */
    private String createEmailBody(String reportDate, Map<String, String> downloadUrls, Map<String, String> fileNames) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>Toppan Report</h2>");

        if (downloadUrls != null && !downloadUrls.isEmpty()) {
            // Files found - provide download links
            html.append("<p>Please find Download URL for Toppan Report for ").append(reportDate).append(".</p>");

            html.append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>");
            html.append("<tr><th>File Name</th><th>Download Link</th></tr>");

            // Add row for PDF-D2 file
            addFileRowSimple(html, downloadUrls, fileNames, "PDF_D2");

            // Add row for LOG-PDF file
            addFileRowSimple(html, downloadUrls, fileNames, "LOG_PDF");

            // Add row for LOG-D2 file
            addFileRowSimple(html, downloadUrls, fileNames, "LOG_D2");

            // Add row for RD2-D2 file
            addFileRowSimple(html, downloadUrls, fileNames, "RD2_D2");

            // Add row for DN2-D2 file
            addFileRowSimple(html, downloadUrls, fileNames, "DN2_D2");

            html.append("</table>");
        } else {
            // No files found
            html.append("<p>No Toppan Report files were found for ").append(reportDate).append(".</p>");
            html.append("<p>Please verify that the reports have been generated and uploaded to the blob storage.</p>");
        }

        html.append("<br>");
        html.append("<p>This is an automated email. Please do not reply.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Helper method to add a file row to the email HTML table (simplified version)
     */
    private void addFileRowSimple(StringBuilder html, Map<String, String> downloadUrls,
                                   Map<String, String> fileNames, String fileTypeKey) {
        if (downloadUrls.containsKey(fileTypeKey)) {
            String fileName = fileNames.get(fileTypeKey);
            String downloadUrl = downloadUrls.get(fileTypeKey);

            html.append("<tr>");
            html.append("<td>").append(fileName).append("</td>");
            html.append("<td><a href=\"").append(downloadUrl).append("\" target=\"_blank\">Download</a></td>");
            html.append("</tr>");
        }
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for Toppan report file retrieval job");
        try {
            // Check if required dependencies are initialized
            if (toppanReportHelper == null) {
                log.error("ToppanReportHelper is not initialized");
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
        log.info("Initializing Toppan report file retrieval job");

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
        log.info("Executing Toppan report file retrieval job");

        try {
            boolean result = execute(reportDate);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "Toppan report file retrieval completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "Toppan report file retrieval failed");
            }
        } catch (Exception e) {
            log.error("Error executing Toppan report file retrieval job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("Cleaning up Toppan report file retrieval job");
        // No specific cleanup needed
    }
}
