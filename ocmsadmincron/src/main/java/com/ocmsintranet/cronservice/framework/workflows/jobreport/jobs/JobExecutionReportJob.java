package com.ocmsintranet.cronservice.framework.workflows.jobreport.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.jobreport.helpers.EmailTemplateProcessor;
import com.ocmsintranet.cronservice.framework.workflows.jobreport.services.JobExecutionReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;

import java.util.*;

/**
 * Cron job that sends a daily email report of all job executions
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobExecutionReportJob extends TrackedCronJobTemplate {
    
    private static final String JOB_NAME = "generate_batch_summary_rpt";
    private static final String EMAIL_TEMPLATE_PATH = "templates/job-execution-report-template.html";
    
    private final JobExecutionReportService jobExecutionReportService;
    private final EmailTemplateProcessor emailTemplateProcessor;
    private final EmailService emailService;
    
    @Value("${email.report.recipients}")
    private String emailRecipients;
    
    @Value("${spring.profiles.active:unknown}")
    private String environment;
    
    @Value("${cron.job.execution.report.shedlock.name:generate_batch_summary_rpt}")
    private String jobName;
    
    @Override
    protected String getJobName() {
        return jobName;
    }
    
    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for job: {}", getJobName());
        if (emailRecipients == null || emailRecipients.trim().isEmpty()) {
            log.warn("No email recipients configured for job execution report");
            return false;
        }
        return true;
    }
    
    @Override
    protected void initialize() {
        try {
            super.initialize(); // This records the job start in history
            log.info("Initializing job execution report");
        } catch (Exception e) {
            log.error("Error initializing job execution report", e);
            throw e; // Re-throw to ensure the job fails if initialization fails
        }
    }
    
    @Override
    protected JobResult doExecute() {
        log.info("Starting job execution report generation");
        try {
            JobReportResult result = sendJobExecutionReport();
            boolean success = result != null && result.isSuccess();
            String message = result != null ? result.getMessage() : "No result returned from sendJobExecutionReport()";
            
            log.info("Job execution report generation completed: {}", success);
            if (!success) {
                log.warn("Job execution report completed with warnings: {}", message);
            }
            
            return new JobResult(success, message);
        } catch (Exception e) {
            String errorMsg = String.format("Error in job execution report generation: %s", e.getMessage());
            log.error(errorMsg, e);
            return new JobResult(false, errorMsg);
        }
    }
    
    /**
     * Custom JobResult that includes report data
     */
    public static class JobReportResult extends JobResult {
        private final Map<String, Object> reportData;
        
        public JobReportResult(boolean success, String message, Map<String, Object> reportData) {
            super(success, message);
            this.reportData = reportData;
        }
        
        public Map<String, Object> getReportData() {
            return reportData;
        }
    }
    
    /**
     * Generate and send the job execution report
     * @return JobReportResult indicating success or failure, including the report data
     */
    public JobReportResult sendJobExecutionReport() {
        try {
            if (!validatePreConditions()) {
                return new JobReportResult(false, "Pre-conditions validation failed", null);
            }
            
            // Exclude this job from the report to avoid self-reference
            List<String> excludedJobs = Collections.singletonList(JOB_NAME);
            
            // Generate the report data
            Map<String, Object> reportData = jobExecutionReportService.generateDailyJobReport(excludedJobs);
            
            // Load and process the email template
            String emailTemplate = emailTemplateProcessor.loadEmailTemplate(EMAIL_TEMPLATE_PATH);
            String emailContent = emailTemplateProcessor.processTemplate(emailTemplate, reportData);
            
            try {
                // Send the email
                sendEmail(emailContent);
                return new JobReportResult(true, "Job execution report sent successfully", reportData);
            } catch (Exception e) {
                log.error("Failed to send job execution report email", e);
                // Include the report data even if email fails, so it can still be returned via API
                return new JobReportResult(false, "Generated report but failed to send email: " + e.getMessage(), reportData);
            }
        } catch (Exception e) {
            log.error("Error generating or sending job execution report", e);
            return new JobReportResult(false, "Failed to generate or send job execution report: " + e.getMessage(), null);
        }
    }
    
    @Override
    protected void cleanup() {
        try {
            log.debug("Starting cleanup for job execution report");
            // Any resource cleanup logic would go here
            log.info("Successfully cleaned up resources for job execution report");
        } catch (Exception e) {
            log.error("Error during cleanup of job execution report: {}", e.getMessage(), e);
            // Don't rethrow to allow the job to complete
        } finally {
            // Ensure super.cleanup() is always called
            try {
                super.cleanup();
            } catch (Exception e) {
                log.error("Error in parent cleanup: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Send the email using the configured email service
     * 
     * @param emailContent The email content
     */
    /**
     * Send the email using the configured email service
     * 
     * @param emailContent The email content to send
     * @throws RuntimeException if there's an error sending the email
     */
    private void sendEmail(String emailContent) {
        try {
            if (emailRecipients == null || emailRecipients.trim().isEmpty()) {
                throw new IllegalArgumentException("No email recipients configured");
            }
            
            String[] recipients = emailRecipients.split("\\s*,\\s*");
            if (recipients.length == 0) {
                throw new IllegalArgumentException("No valid email recipients found");
            }
            
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(String.join(",", recipients));
            // Add environment prefix to email subject
            emailRequest.setSubject("[" + environment.toUpperCase() + "] - OCMS Summary Batch Job Report");
            emailRequest.setHtmlContent(emailContent);
            
            boolean sent = emailService.sendEmail(emailRequest);
            if (!sent) {
                throw new RuntimeException("Email service failed to send email (SMTP might be disabled)");
            }
            
            log.info("Job execution report email sent successfully to {} recipients", recipients.length);
            
        } catch (Exception e) {
            log.error("Failed to send job execution report email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
