package com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers.ClassifiedVehicleReportHelper;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.models.ClassifiedVehicleReportSummary;
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
 * OCMS 14: Classified Vehicle (VIP) Report Job
 * Daily report for Type V (VIP) vehicle notices
 *
 * Scheduled execution: Daily at configurable time (default: 08:00 AM)
 * Report includes: Type V notices, amended notices (V→S), summary statistics
 */
@Slf4j
@Component
public class ClassifiedVehicleReportJob extends TrackedCronJobTemplate {

    private static final String JOB_NAME = "generate_classified_vehicle_report";

    @Value("${email.report.daily.reports.classified.recipients:}")
    private String oicRecipients;

    @Value("${email.report.daily.reports.support.recipients:}")
    private String supportRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    private final ClassifiedVehicleReportHelper reportHelper;
    private final EmailService emailService;

    private Map<String, String> jobMetadata = new HashMap<>();
    private String reportDate;

    public ClassifiedVehicleReportJob(
            ClassifiedVehicleReportHelper reportHelper,
            EmailService emailService) {
        this.reportHelper = reportHelper;
        this.emailService = emailService;
    }

    /**
     * Set the report date for this job execution
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
        log.info("[Classified Vehicle Report Job] Report date manually set to: {}", reportDate);
    }

    /**
     * Execute the report generation job
     */
    public boolean execute(String reportDate) {
        log.info("[Classified Vehicle Report Job] Starting report generation for date: {}", reportDate);

        jobMetadata = new HashMap<>();
        jobMetadata.put("reportDate", reportDate);
        jobMetadata.put("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Fetch Type V and amended notice records
            log.info("[Classified Vehicle Report Job] Fetching Classified Vehicle records");
            Map<String, Object> reportData = reportHelper.fetchClassifiedVehicleRecords();

            if (reportData == null || reportData.isEmpty()) {
                log.error("[Classified Vehicle Report Job] Failed to fetch records - reportData is null or empty");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Failed to fetch records");
                sendFailureEmail(reportDate, "Failed to fetch records from database");
                return false;
            }

            ClassifiedVehicleReportSummary summary = (ClassifiedVehicleReportSummary) reportData.get("summary");

            jobMetadata.put("totalNotices", String.valueOf(summary.getTotalNoticesIssued()));
            jobMetadata.put("outstandingNotices", String.valueOf(summary.getOutstandingNotices()));
            jobMetadata.put("settledNotices", String.valueOf(summary.getSettledNotices()));
            jobMetadata.put("amendedNotices", String.valueOf(summary.getAmendedNotices()));

            if (summary.getTotalNoticesIssued() == 0 && summary.getAmendedNotices() == 0) {
                log.info("[Classified Vehicle Report Job] No records found for date: {}", reportDate);
                jobMetadata.put("status", "NO_DATA");
                sendNoDataEmail(reportDate);
                return true;
            }

            // Step 2: Generate Excel report (3 sheets)
            log.info("[Classified Vehicle Report Job] Generating Excel report with summary statistics");
            byte[] reportBytes = reportHelper.generateExcelReport(reportData, reportDate);

            if (reportBytes == null || reportBytes.length == 0) {
                log.error("[Classified Vehicle Report Job] Failed to generate Excel report");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Failed to generate Excel report");
                sendFailureEmail(reportDate, "Failed to generate Excel report");
                return false;
            }

            // Step 3: Send email with summary in body and Excel attachment
            String fileName = "OCMS_Classified_Vehicle_Report_" + reportDate.replace("-", "") + ".xlsx";

            log.info("[Classified Vehicle Report Job] Sending report via email to: {}", oicRecipients);

            if (oicRecipients == null || oicRecipients.isEmpty()) {
                log.error("[Classified Vehicle Report Job] No OIC email recipients configured");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "No OIC email recipients configured");
                sendFailureEmail(reportDate, "No OIC email recipients configured");
                return false;
            }

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Classified Vehicle Notices Report - " + reportDate);
            emailRequest.setHtmlContent(createEmailBody(reportDate, summary));

            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(reportBytes);

            List<EmailRequest.Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            emailRequest.setAttachments(attachments);

            boolean emailSent = emailService.sendEmail(emailRequest);

            if (!emailSent) {
                log.error("[Classified Vehicle Report Job] Failed to send email");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Email sending failed");
                sendFailureEmail(reportDate, "Failed to send email to OIC");
                return false;
            }

            jobMetadata.put("fileName", fileName);
            jobMetadata.put("emailRecipients", oicRecipients);
            jobMetadata.put("status", "COMPLETED");
            jobMetadata.put("endTime", LocalDateTime.now().toString());

            log.info("[Classified Vehicle Report Job] Report generation completed successfully");
            return true;

        } catch (Exception e) {
            log.error("[Classified Vehicle Report Job] Error executing job", e);
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
                log.warn("[Classified Vehicle Report Job] No OIC email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2>OCMS Classified Vehicle Notices Report</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(formatDate(reportDate)).append("</p>");
            html.append("<hr/>");
            html.append("<p style='color: green;'>No Type V (VIP) vehicle notices found for this date.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated report.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Classified Vehicle Report - " + reportDate + " (No Data)");
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[Classified Vehicle Report Job] No data notification email sent");

        } catch (Exception e) {
            log.error("[Classified Vehicle Report Job] Failed to send no data notification", e);
        }
    }

    /**
     * Send failure notification email
     */
    private void sendFailureEmail(String reportDate, String errorMessage) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("[Classified Vehicle Report Job] No support email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2 style='color: red;'>OCMS Classified Vehicle Report - FAILED</h2>");
            html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
            html.append("<p><strong>Error:</strong> ").append(errorMessage).append("</p>");
            html.append("<hr/>");
            html.append("<p>Please investigate and resolve the issue.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated notification.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Classified Vehicle Report - FAILED - " + reportDate);
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[Classified Vehicle Report Job] Failure notification sent to support");

        } catch (Exception e) {
            log.error("[Classified Vehicle Report Job] Failed to send failure notification", e);
        }
    }

    /**
     * Create HTML email body with summary statistics
     */
    private String createEmailBody(String reportDate, ClassifiedVehicleReportSummary summary) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>OCMS Classified Vehicle Notices Report</h2>");
        html.append("<p><strong>Report Date:</strong> ").append(formatDate(reportDate)).append("</p>");
        html.append("<hr/>");
        html.append("<h3>Summary</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><td><strong>Category</strong></td><td><strong>Count</strong></td></tr>");
        html.append("<tr><td>Total Notices Issued (Type V)</td><td>").append(summary.getTotalNoticesIssued()).append("</td></tr>");
        html.append("<tr><td>Outstanding Notices (Unpaid)</td><td>").append(summary.getOutstandingNotices()).append("</td></tr>");
        html.append("<tr><td>Settled Notices (Paid)</td><td>").append(summary.getSettledNotices()).append("</td></tr>");
        html.append("<tr><td>Notices Amended (V→S)</td><td>").append(summary.getAmendedNotices()).append("</td></tr>");
        html.append("</table>");
        html.append("<br/>");
        html.append("<p>Detailed information is available in the attached Excel report (3 sheets: Summary, Type V Notices Detail, Amended Notices).</p>");
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
        log.info("[Classified Vehicle Report Job] Validating pre-conditions");
        if (reportHelper == null) {
            log.error("[Classified Vehicle Report Job] ClassifiedVehicleReportHelper is not initialized");
            return false;
        }
        if (emailService == null) {
            log.error("[Classified Vehicle Report Job] EmailService is not initialized");
            return false;
        }
        return true;
    }

    @Override
    protected void initialize() {
        log.info("[Classified Vehicle Report Job] Initializing job");

        // Set report date to today by default
        if (reportDate == null || reportDate.isEmpty()) {
            LocalDate today = LocalDate.now();
            reportDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("[Classified Vehicle Report Job] Report date set to today: {}", reportDate);
        }

        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("[Classified Vehicle Report Job] Executing job");

        try {
            boolean result = execute(reportDate);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(true, "Classified Vehicle report generation completed successfully");
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(false, "Classified Vehicle report generation failed");
            }
        } catch (Exception e) {
            log.error("[Classified Vehicle Report Job] Error executing job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("[Classified Vehicle Report Job] Cleanup completed");
    }
}
