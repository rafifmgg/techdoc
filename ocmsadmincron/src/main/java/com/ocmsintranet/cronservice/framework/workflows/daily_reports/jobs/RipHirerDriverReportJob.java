package com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers.RipHirerDriverReportHelper;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.models.RipHirerDriverReportRecord;
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
 * OCMS 14: RIP Hirer/Driver Furnished Report Job
 * Daily report for notices suspended under PS-RP2 where offender is Hirer/Driver
 *
 * Scheduled execution: Daily at 02:00 AM
 * Report date: Current date (notices suspended today)
 */
@Slf4j
@Component
public class RipHirerDriverReportJob extends TrackedCronJobTemplate {

    private static final String JOB_NAME = "generate_rip_hirer_driver_report";

    @Value("${email.report.daily.reports.rip.recipients:}")
    private String oicRecipients;

    @Value("${email.report.daily.reports.support.recipients:}")
    private String supportRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    private final RipHirerDriverReportHelper reportHelper;
    private final EmailService emailService;

    private Map<String, String> jobMetadata = new HashMap<>();
    private String reportDate;

    public RipHirerDriverReportJob(
            RipHirerDriverReportHelper reportHelper,
            EmailService emailService) {
        this.reportHelper = reportHelper;
        this.emailService = emailService;
    }

    /**
     * Set the report date for this job execution
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
        log.info("[RIP Hirer/Driver Report Job] Report date manually set to: {}", reportDate);
    }

    /**
     * Execute the report generation job
     */
    public boolean execute(String reportDate) {
        log.info("[RIP Hirer/Driver Report Job] Starting report generation for date: {}", reportDate);

        jobMetadata = new HashMap<>();
        jobMetadata.put("reportDate", reportDate);
        jobMetadata.put("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Fetch PS-RP2 Hirer/Driver records
            log.info("[RIP Hirer/Driver Report Job] Fetching PS-RP2 Hirer/Driver records");
            List<RipHirerDriverReportRecord> records = reportHelper.fetchRipHirerDriverRecords(reportDate);

            jobMetadata.put("totalRecords", String.valueOf(records.size()));

            if (records.isEmpty()) {
                log.info("[RIP Hirer/Driver Report Job] No records found for date: {}", reportDate);
                jobMetadata.put("status", "NO_DATA");
                sendNoDataEmail(reportDate);
                return true;
            }

            // Step 2: Generate Excel report
            log.info("[RIP Hirer/Driver Report Job] Generating Excel report for {} records", records.size());
            byte[] reportBytes = reportHelper.generateExcelReport(records, reportDate);

            if (reportBytes == null || reportBytes.length == 0) {
                log.error("[RIP Hirer/Driver Report Job] Failed to generate Excel report");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Failed to generate Excel report");
                sendFailureEmail(reportDate, "Failed to generate Excel report");
                return false;
            }

            // Step 3: Send email with Excel attachment
            String fileName = "OCMS_RIP_Hirer_Driver_Report_" + reportDate.replace("-", "") + ".xlsx";

            log.info("[RIP Hirer/Driver Report Job] Sending report via email to: {}", oicRecipients);

            if (oicRecipients == null || oicRecipients.isEmpty()) {
                log.error("[RIP Hirer/Driver Report Job] No OIC email recipients configured");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "No OIC email recipients configured");
                sendFailureEmail(reportDate, "No OIC email recipients configured");
                return false;
            }

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS RIP Hirer/Driver Furnished Report - " + reportDate);
            emailRequest.setHtmlContent(createEmailBody(reportDate, records.size()));

            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(reportBytes);

            List<EmailRequest.Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            emailRequest.setAttachments(attachments);

            boolean emailSent = emailService.sendEmail(emailRequest);

            if (!emailSent) {
                log.error("[RIP Hirer/Driver Report Job] Failed to send email");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Email sending failed");
                sendFailureEmail(reportDate, "Failed to send email to OIC");
                return false;
            }

            jobMetadata.put("fileName", fileName);
            jobMetadata.put("emailRecipients", oicRecipients);
            jobMetadata.put("status", "COMPLETED");
            jobMetadata.put("endTime", LocalDateTime.now().toString());

            log.info("[RIP Hirer/Driver Report Job] Report generation completed successfully");
            return true;

        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report Job] Error executing job", e);
            jobMetadata.put("status", "FAILED");
            jobMetadata.put("error", e.getMessage());
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
                log.warn("[RIP Hirer/Driver Report Job] No OIC email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2>OCMS RIP Hirer/Driver Furnished Report</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(formatDate(reportDate)).append("</p>");
            html.append("<hr/>");
            html.append("<p style='color: green;'>No PS-RP2 Hirer/Driver records found for this date.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated report.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS RIP Hirer/Driver Report - " + reportDate + " (No Data)");
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[RIP Hirer/Driver Report Job] No data notification email sent");

        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report Job] Failed to send no data notification", e);
        }
    }

    /**
     * Send failure notification email
     */
    private void sendFailureEmail(String reportDate, String errorMessage) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("[RIP Hirer/Driver Report Job] No support email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2 style='color: red;'>OCMS RIP Hirer/Driver Report - FAILED</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
            html.append("<p><strong>Error:</strong> ").append(errorMessage).append("</p>");
            html.append("<hr/>");
            html.append("<p>Please investigate and resolve the issue.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated notification.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS RIP Hirer/Driver Report - FAILED - " + reportDate);
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[RIP Hirer/Driver Report Job] Failure notification sent to support");

        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report Job] Failed to send failure notification", e);
        }
    }

    /**
     * Create HTML email body
     */
    private String createEmailBody(String reportDate, int recordCount) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>OCMS RIP Hirer/Driver Furnished Report</h2>");
        html.append("<p><strong>Report Date:</strong> ").append(formatDate(reportDate)).append("</p>");
        html.append("<hr/>");
        html.append("<h3>Summary</h3>");
        html.append("<p>This report contains <strong>").append(recordCount).append("</strong> notice(s) suspended under PS-RP2 where the offender is a Hirer or Driver.</p>");
        html.append("<br/>");
        html.append("<p>Please find the detailed report in the attached Excel file.</p>");
        html.append("<p style='font-size: 11px; color: #666;'>This is an automated report generated by OCMS.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Format date for display
     */
    private String formatDate(String reportDate) {
        LocalDate date = LocalDate.parse(reportDate);
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("[RIP Hirer/Driver Report Job] Validating pre-conditions");
        if (reportHelper == null) {
            log.error("[RIP Hirer/Driver Report Job] RipHirerDriverReportHelper is not initialized");
            return false;
        }
        if (emailService == null) {
            log.error("[RIP Hirer/Driver Report Job] EmailService is not initialized");
            return false;
        }
        return true;
    }

    @Override
    protected void initialize() {
        log.info("[RIP Hirer/Driver Report Job] Initializing job");

        // Set report date to today by default (notices suspended today)
        if (reportDate == null || reportDate.isEmpty()) {
            LocalDate today = LocalDate.now();
            reportDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("[RIP Hirer/Driver Report Job] Report date set to today: {}", reportDate);
        }

        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("[RIP Hirer/Driver Report Job] Executing job");

        try {
            boolean result = execute(reportDate);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "RIP Hirer/Driver report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "RIP Hirer/Driver report generation failed");
            }
        } catch (Exception e) {
            log.error("[RIP Hirer/Driver Report Job] Error executing job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("[RIP Hirer/Driver Report Job] Cleanup completed");
    }
}
