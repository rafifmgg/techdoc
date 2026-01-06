package com.ocmsintranet.cronservice.framework.workflows.daily_reports.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.daily_reports.helpers.AnsLetterReconciliationHelper;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OCMS 10: ANS Letter Reconciliation Report Job
 * Reconciles AN letters sent to Toppan vs acknowledgement file
 *
 * Triggered by: ToppanDownloadJob when acknowledgement file is received
 * Report: Compares Control Summary Report (sent) vs Acknowledgement File (printed)
 */
@Slf4j
@Component
public class AnsLetterReconciliationJob extends TrackedCronJobTemplate {

    private static final String JOB_NAME = "generate_ans_letter_reconciliation_report";
    private static final String BLOB_CONTAINER = "ocms-reports";
    private static final String BLOB_FOLDER = "ans-reconciliation";

    @Value("${email.report.daily.reports.support.recipients:}")
    private String supportRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    private final AnsLetterReconciliationHelper reconciliationHelper;
    private final EmailService emailService;
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    private Map<String, String> jobMetadata = new HashMap<>();
    private String processDate;
    private String ackFileContent;

    public AnsLetterReconciliationJob(
            AnsLetterReconciliationHelper reconciliationHelper,
            EmailService emailService,
            AzureBlobStorageUtil azureBlobStorageUtil) {
        this.reconciliationHelper = reconciliationHelper;
        this.emailService = emailService;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    /**
     * Set the process date and acknowledgement file content for this job execution
     *
     * @param processDate Date of the CSR/ACK files being reconciled
     * @param ackFileContent Content of the acknowledgement file from Toppan
     */
    public void setReconciliationData(String processDate, String ackFileContent) {
        this.processDate = processDate;
        this.ackFileContent = ackFileContent;
        log.info("[ANS Letter Reconciliation Job] Reconciliation data set - Process date: {}", processDate);
    }

    /**
     * Execute the reconciliation job
     */
    public boolean execute(String processDate, String ackFileContent) {
        log.info("[ANS Letter Reconciliation Job] Starting reconciliation for date: {}", processDate);

        jobMetadata = new HashMap<>();
        jobMetadata.put("processDate", processDate);
        jobMetadata.put("startTime", LocalDateTime.now().toString());

        try {
            // Step 1: Fetch letters sent to Toppan
            log.info("[ANS Letter Reconciliation Job] Fetching letters sent to Toppan");
            List<Map<String, Object>> sentLetters = reconciliationHelper.fetchLettersSentToToppan(processDate);
            jobMetadata.put("totalSentToToppan", String.valueOf(sentLetters.size()));

            if (sentLetters.isEmpty()) {
                log.info("[ANS Letter Reconciliation Job] No letters found sent to Toppan for date: {}", processDate);
                jobMetadata.put("status", "NO_DATA");
                sendNoDataEmail(processDate);
                return true;
            }

            // Step 2: Parse acknowledgement file
            log.info("[ANS Letter Reconciliation Job] Parsing acknowledgement file");
            Map<String, String> ackMap = reconciliationHelper.parseAcknowledgementFile(ackFileContent);
            jobMetadata.put("totalAcknowledged", String.valueOf(ackMap.size()));

            // Step 3: Perform reconciliation
            log.info("[ANS Letter Reconciliation Job] Performing reconciliation");
            List<AnsLetterReconciliationHelper.ReconciliationRecord> reconciliationRecords =
                    reconciliationHelper.performReconciliation(sentLetters, ackMap);

            // Step 4: Calculate summary statistics
            AnsLetterReconciliationHelper.ReconciliationSummary summary =
                    reconciliationHelper.calculateSummary(reconciliationRecords);

            jobMetadata.put("totalPrinted", String.valueOf(summary.getTotalPrintedSuccessfully()));
            jobMetadata.put("totalMissing", String.valueOf(summary.getTotalMissingInAcknowledgement()));
            jobMetadata.put("totalErrors", String.valueOf(summary.getTotalErrorsInPrinting()));
            jobMetadata.put("matchRate", String.format("%.2f%%", summary.getMatchRate()));

            // Step 5: Generate Excel report
            log.info("[ANS Letter Reconciliation Job] Generating Excel report");
            byte[] reportBytes = reconciliationHelper.generateExcelReport(reconciliationRecords, summary, processDate);

            if (reportBytes == null || reportBytes.length == 0) {
                log.error("[ANS Letter Reconciliation Job] Failed to generate Excel report");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Failed to generate Excel report");
                sendFailureEmail(processDate, "Failed to generate Excel report");
                return false;
            }

            String fileName = "OCMS_ANS_Letter_Reconciliation_" + processDate.replace("-", "") + ".xlsx";

            // Step 6: Upload to blob storage
            log.info("[ANS Letter Reconciliation Job] Uploading report to blob storage");
            String blobPath = BLOB_FOLDER + "/" + fileName;

            try {
                azureBlobStorageUtil.uploadBytesToBlob(reportBytes, blobPath);
                jobMetadata.put("blobPath", blobPath);
                log.info("[ANS Letter Reconciliation Job] Report uploaded to blob storage: {}", blobPath);
            } catch (Exception e) {
                log.error("[ANS Letter Reconciliation Job] Failed to upload to blob storage", e);
                // Continue anyway - we can still email the report
            }

            // Step 7: Send email with Excel attachment
            log.info("[ANS Letter Reconciliation Job] Sending report via email to: {}", supportRecipients);

            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.error("[ANS Letter Reconciliation Job] No support email recipients configured");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "No support email recipients configured");
                return false;
            }

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS ANS Letter Reconciliation Report - " + processDate);
            emailRequest.setHtmlContent(createEmailBody(processDate, summary));

            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(reportBytes);

            List<EmailRequest.Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            emailRequest.setAttachments(attachments);

            boolean emailSent = emailService.sendEmail(emailRequest);

            if (!emailSent) {
                log.error("[ANS Letter Reconciliation Job] Failed to send email");
                jobMetadata.put("status", "FAILED");
                jobMetadata.put("error", "Email sending failed");
                return false;
            }

            jobMetadata.put("fileName", fileName);
            jobMetadata.put("emailRecipients", supportRecipients);
            jobMetadata.put("status", "COMPLETED");
            jobMetadata.put("endTime", LocalDateTime.now().toString());

            log.info("[ANS Letter Reconciliation Job] Reconciliation report generation completed successfully");
            return true;

        } catch (Exception e) {
            log.error("[ANS Letter Reconciliation Job] Error executing job", e);
            jobMetadata.put("status", "FAILED");
            jobMetadata.put("error", e.getMessage());
            sendFailureEmail(processDate, "Exception occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send email when no data is found
     */
    private void sendNoDataEmail(String processDate) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("[ANS Letter Reconciliation Job] No support email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2>OCMS ANS Letter Reconciliation Report</h2>");
            html.append("<p><strong>Process Date:</strong> ").append(processDate).append("</p>");
            html.append("<hr/>");
            html.append("<p style='color: green;'>No AN letters found sent to Toppan for this date.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated report.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS ANS Letter Reconciliation - " + processDate + " (No Data)");
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[ANS Letter Reconciliation Job] No data notification email sent");

        } catch (Exception e) {
            log.error("[ANS Letter Reconciliation Job] Failed to send no data notification", e);
        }
    }

    /**
     * Send failure notification email
     */
    private void sendFailureEmail(String processDate, String errorMessage) {
        try {
            if (supportRecipients == null || supportRecipients.isEmpty()) {
                log.warn("[ANS Letter Reconciliation Job] No support email recipients configured");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2 style='color: red;'>OCMS ANS Letter Reconciliation - FAILED</h2>");
            html.append("<p><strong>Process Date:</strong> ").append(processDate).append("</p>");
            html.append("<p><strong>Error:</strong> ").append(errorMessage).append("</p>");
            html.append("<hr/>");
            html.append("<p>Please investigate and resolve the issue.</p>");
            html.append("<p style='font-size: 11px; color: #666;'>This is an automated notification.</p>");
            html.append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(supportRecipients);
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS ANS Letter Reconciliation - FAILED - " + processDate);
            emailRequest.setHtmlContent(html.toString());

            emailService.sendEmail(emailRequest);
            log.info("[ANS Letter Reconciliation Job] Failure notification sent to support");

        } catch (Exception e) {
            log.error("[ANS Letter Reconciliation Job] Failed to send failure notification", e);
        }
    }

    /**
     * Create HTML email body with summary statistics
     */
    private String createEmailBody(String processDate, AnsLetterReconciliationHelper.ReconciliationSummary summary) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>OCMS ANS Letter Reconciliation Report</h2>");
        html.append("<p><strong>Process Date:</strong> ").append(processDate).append("</p>");
        html.append("<hr/>");

        html.append("<h3>Reconciliation Summary</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><td><strong>Total Letters Sent to Toppan</strong></td><td>").append(summary.getTotalSentToToppan()).append("</td></tr>");
        html.append("<tr style='background-color: #d4edda;'><td><strong>Successfully Printed</strong></td><td>").append(summary.getTotalPrintedSuccessfully()).append("</td></tr>");
        html.append("<tr style='background-color: #f8d7da;'><td><strong>Missing in Acknowledgement</strong></td><td>").append(summary.getTotalMissingInAcknowledgement()).append("</td></tr>");
        html.append("<tr style='background-color: #fff3cd;'><td><strong>Printing Errors</strong></td><td>").append(summary.getTotalErrorsInPrinting()).append("</td></tr>");
        html.append("<tr><td><strong>Match Rate</strong></td><td>").append(String.format("%.2f%%", summary.getMatchRate())).append("</td></tr>");
        html.append("</table>");

        html.append("<br/>");

        if (summary.getTotalMissingInAcknowledgement() > 0 || summary.getTotalErrorsInPrinting() > 0) {
            html.append("<p style='color: red;'><strong>⚠️ Action Required:</strong> There are discrepancies in the reconciliation. Please review the attached report for details.</p>");
        } else {
            html.append("<p style='color: green;'>✅ All letters sent to Toppan were successfully printed.</p>");
        }

        html.append("<p>Please find the detailed reconciliation report in the attached Excel file.</p>");
        html.append("<p style='font-size: 11px; color: #666;'>This is an automated report generated by OCMS.</p>");
        html.append("</body></html>");

        return html.toString();
    }

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("[ANS Letter Reconciliation Job] Validating pre-conditions");

        if (reconciliationHelper == null) {
            log.error("[ANS Letter Reconciliation Job] AnsLetterReconciliationHelper is not initialized");
            return false;
        }
        if (emailService == null) {
            log.error("[ANS Letter Reconciliation Job] EmailService is not initialized");
            return false;
        }
        if (azureBlobStorageUtil == null) {
            log.error("[ANS Letter Reconciliation Job] AzureBlobStorageUtil is not initialized");
            return false;
        }
        if (processDate == null || processDate.isEmpty()) {
            log.error("[ANS Letter Reconciliation Job] Process date is not set");
            return false;
        }
        if (ackFileContent == null || ackFileContent.isEmpty()) {
            log.error("[ANS Letter Reconciliation Job] Acknowledgement file content is not set");
            return false;
        }

        return true;
    }

    @Override
    protected void initialize() {
        log.info("[ANS Letter Reconciliation Job] Initializing job for process date: {}", processDate);
        super.initialize();
    }

    @Override
    protected CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("[ANS Letter Reconciliation Job] Executing job");

        try {
            boolean result = execute(processDate, ackFileContent);

            if (result) {
                return new CronJobFramework.CronJobTemplate.JobResult(
                        true,
                        "ANS Letter Reconciliation report generation completed successfully"
                );
            } else {
                return new CronJobFramework.CronJobTemplate.JobResult(
                        false,
                        "ANS Letter Reconciliation report generation failed"
                );
            }
        } catch (Exception e) {
            log.error("[ANS Letter Reconciliation Job] Error executing job", e);
            return new CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void cleanup() {
        log.info("[ANS Letter Reconciliation Job] Cleanup completed");
    }
}
