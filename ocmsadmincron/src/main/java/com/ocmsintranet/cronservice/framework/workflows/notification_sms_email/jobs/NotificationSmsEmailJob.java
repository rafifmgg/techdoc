package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.jobs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.helpers.NotificationSmsEmailHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification SMS and Email Job.
 * This job performs the following steps in a consolidated workflow:
 * 
 * Main Flow (Initial Send):
 * 1. Query records that need initial SMS/email notifications using NotificationSmsEmailHelper
 * 2. For each record:
 *    a. Generate SMS & email messages
 *    b. Send SMS if enabled
 *    c. Send email if enabled
 *    d. Save send status (success or error)
 *    e. Check if notice number is in HST list and apply TS-HST if found
 * 
 * Retry Flow:
 * 1. Query records that need retry using NotificationSmsEmailHelper
 * 2. For each record:
 *    a. Check if SMS and/or email retry is needed
 *    b. Generate messages
 *    c. Retry sending SMS if needed and enabled
 *    d. Retry sending email if needed and enabled
 *    e. Save send status
 *    f. If retry fails, move to RD1 stage with proper error recording
 *    g. Check if notice number is in HST list and apply TS-HST if found
 * 
 * The job can be run in two modes:
 * - Main mode: Processes records that are ready for initial notification
 * - Retry mode: Retries sending failed notifications and moves to RD1 stage if retry fails
 * 
 * All workflow logic is consolidated in the doExecute method for better readability and maintenance.
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("send_ena_reminder")
public class NotificationSmsEmailJob extends TrackedCronJobTemplate {
    
    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;
    
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    private final NotificationSmsEmailHelper notificationHelper;
    private final TableQueryService tableQueryService;
    
    // Flag to indicate if this is a retry operation
    private boolean isRetry = false;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();
    
    @Value("${cron.enareminder.shedlock.name:send_ena_reminder}")
    private String jobName;

    @Value("${cron.enareminder.retry.shedlock.name:send_ena_reminder_retry}")
    private String jobNameRetry;

    public NotificationSmsEmailJob(
            NotificationSmsEmailHelper notificationHelper,
            TableQueryService tableQueryService) {
        this.notificationHelper = notificationHelper;
        this.tableQueryService = tableQueryService;
    }

    /**
     * Set the job to run in retry mode
     * 
     * @param retry true to run in retry mode, false for main mode
     */
    public void setRetryMode(boolean retry) {
        this.isRetry = retry;
    }

    /**
     * Records job metadata for tracking and reporting
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    private void recordJobMetadata(String key, String value) {
        // Store in the metadata map
        jobMetadata.put(key, value);
        // Also log for visibility
        log.info("Job Metadata: {} = {}", key, value);
    }

    @Override
    protected String getJobName() {
        return isRetry ? jobNameRetry : jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for {} job", isRetry ? "notification retry" : "notification");
        
        try {
            // Check if required dependencies are initialized
            if (notificationHelper == null) {
                log.error("NotificationSmsEmailHelper is not initialized");
                return false;
            }
            
            if (tableQueryService == null) {
                log.error("TableQueryService is not initialized");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating pre-conditions: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    protected void initialize() {
        // Call parent initialize which records job start in history
        super.initialize();
        
        // Initialize job metadata
        jobMetadata.clear();
        jobMetadata.put("jobStartTime", LocalDateTime.now().toString());
        jobMetadata.put("isRetryMode", String.valueOf(isRetry));
    }
    
    @Override
    protected void cleanup() {
        // Clean up any temporary resources
        jobMetadata.clear();
        super.cleanup();
    }

    @Override
    protected com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("Starting {} job execution", isRetry ? "notification retry" : "notification");
        
        // List to collect preparation errors
        List<Map<String, String>> preparationErrors = new ArrayList<>();

        try {
            // Get records - dual notice handling
            List<Map<String, Object>> records;
            
            // Main flow: Combine ENA and Reminder notices
            List<Map<String, Object>> enaNotices = notificationHelper.queryRecordsForEna();
            List<Map<String, Object>> reminderNotices = notificationHelper.queryRecordsForReminder();
            
            records = new ArrayList<>();
            records.addAll(enaNotices);
            records.addAll(reminderNotices);
            
            // Add notice type markers
            for (Map<String, Object> record : enaNotices) {
                record.put("noticeType", SystemConstant.SuspensionReason.ENA);
                // Ensure processing stage is available on the record for inserts
                record.put("nextProcessingStage", SystemConstant.SuspensionReason.ENA);
            }
            for (Map<String, Object> record : reminderNotices) {
                record.put("noticeType", SystemConstant.SuspensionReason.RD1);
                // Ensure processing stage is available on the record for inserts
                Object stage = record.get("lastProcessingStage");
                if (stage == null) stage = record.get("last_processing_stage");
                record.put("nextProcessingStage", stage);
            }
            
            // Refresh contact info from DataHive
            for (Map<String, Object> record : records) {
                notificationHelper.refreshContactInfo(record);
            }
            
            // Combine notices for same recipient
            records = notificationHelper.combineNotices(records);

            // Prepare SMS records - each notice gets its own SMS (no deduplication by phone)
            List<Map<String, Object>> smsRecords = new ArrayList<>();

            for (Map<String, Object> record : records) {
                // Skip invalid contacts (apply required decision for missing mobile)
                if (!notificationHelper.validateContactInfo(record)) {
                    // Determine stage using both camelCase and snake_case keys
                    String nextStageForPrep = (String) record.get("nextProcessingStage");
                    if (nextStageForPrep == null) {
                        nextStageForPrep = (String) record.get("next_processing_stage");
                    }
                    if ("ENA".equals(nextStageForPrep)) {
                        log.warn("No mobile for ENA notice {} during Prepare Send SMS. Patching to RD1 next day.", record.get("noticeNo"));
                        notificationHelper.patchNextStageToRd1NextDay(record);
                    } else {
                        log.warn("Skipping record with invalid contact (non-ENA): {}", record.get("noticeNo"));
                    }
                    continue;
                }

                // Skip companies from SMS batch - companies only get email
                String idType = (String) record.get("idType");
                if (idType == null) {
                    idType = (String) record.get("id_type");
                }
                if ("B".equals(idType)) {
                    log.debug("Skipping company record {} from SMS batch", record.get("noticeNo"));
                    continue;
                }

                // Add ALL valid individual records - each notice gets its own SMS
                String phoneKey = (String) record.get("normalizedPhone");
                if (phoneKey != null && !phoneKey.isEmpty()) {
                    log.info("Adding record {} to SMS batch with phone {}", record.get("noticeNo"), phoneKey);
                    smsRecords.add(record);
                }
            }

            // Send batch SMS if enabled
            boolean batchSmsStatus = false;
            if (smsEnabled && !smsRecords.isEmpty()) {
                log.info("Sending batch SMS for {} notices", smsRecords.size());
                batchSmsStatus = notificationHelper.sendBatchSms(smsRecords);
                recordJobMetadata("batchSmsStatus", String.valueOf(batchSmsStatus));
            }
            
            if (records.isEmpty()) {
                log.info("No records found for {} processing. Job completed successfully with no action.", 
                        isRetry ? "retry" : "initial");
                return new com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult(true, "No records found for processing");
            }
            
            log.info("Found {} records for {} processing", 
                    records.size(), isRetry ? "retry" : "initial");
            
            // Log the first record for debugging purposes
            if (!records.isEmpty()) {
                log.debug("Sample record: {}", records.get(0));
            }
            
            // Record job metadata
            recordJobMetadata("recordCount", String.valueOf(records.size()));
            
            // Process all notices
            int successCount = 0;
            int failureCount = 0;
            int hstCount = 0;
            
            for (Map<String, Object> record : records) {
                try {
                    // Pre-checks per flow: exclusion and passport handling when stage is ENA
                    String idNo = (String) record.get("id_no");
                    if (idNo == null) {
                        idNo = (String) record.get("idNo");
                    }

                    String nextStage = (String) record.get("nextProcessingStage");
                    if (nextStage == null) {
                        nextStage = (String) record.get("next_processing_stage");
                        if (nextStage == null) {
                            String noticeTypeMarker = (String) record.get("noticeType");
                            if ("ENA".equals(noticeTypeMarker) || "RD1".equals(noticeTypeMarker)) {
                                nextStage = noticeTypeMarker;
                            }
                        }
                    }

                    String idType = (String) record.get("idType");
                    if (idType == null) {
                        idType = (String) record.get("id_type");
                    }

                    // 1) Exclusion check
                    if (idNo != null && notificationHelper.isExcluded(idNo)) {
                        log.info("ID {} is in exclusion list", idNo);
                        // If current next stage is ENA, patch to RD1 next day; otherwise skip
                        if ("ENA".equals(nextStage)) {
                            notificationHelper.patchNextStageToRd1NextDay(record);
                        }
                        continue;
                    }

                    // 2) Passport handling only if next stage is ENA
                    if ("ENA".equals(nextStage)) {
                        if ("P".equalsIgnoreCase(idType) || "PASSPORT".equalsIgnoreCase(idType)) {
                            log.info("Passport ID detected for notice {}. Patching to RD1 next day and skipping.", record.get("noticeNo"));
                            notificationHelper.patchNextStageToRd1NextDay(record);
                            continue;
                        }
                    }

                    // 3) Do not skip non-ENA records here so reminders can proceed

                    // Extract necessary information from the record
                    String noticeNo = (String) record.get("noticeNo");
                    boolean success = false;
                    
                    // Determine notification type based on ID type
                    boolean sendSms = (idType == null || !"B".equals(idType));
                    boolean sendEmail = "B".equals(idType);
                    
                    // Check for missing contact info in ENA stage
                    if ("ENA".equals(nextStage)) {
                        if (sendSms && (record.get("phoneNo") == null || ((String) record.get("phoneNo")).isEmpty())) {
                            log.warn("No phone number for ENA notice: {}. Moving to RD1", noticeNo);
                            notificationHelper.updateStageToRd1(record);
                            continue;
                        }
                        if (sendEmail && (record.get("email") == null || ((String) record.get("email")).isEmpty())) {
                            log.warn("No email for ENA notice: {}. Moving to RD1", noticeNo);
                            notificationHelper.updateStageToRd1(record);
                            continue;
                        }
                    }
                    
                    // ===== INITIAL SEND FLOW =====
                    log.info("Processing record for initial notification: {}", noticeNo);
                    
                    // For initial flow, SMS is already sent in batch
                    boolean smsSent = sendSms && batchSmsStatus; // SMS sent if enabled and batch was successful
                    
                    // Only send email individually
                    boolean emailSent = false;
                    if (emailEnabled && sendEmail) {
                        Map<String, String> messages = notificationHelper.generateMessages(record);
                        record.put("smsContent", messages.get("smsContent"));
                        record.put("emailContent", messages.get("emailContent"));
                        emailSent = notificationHelper.sendEmail(record, messages.get("emailSubject"), messages.get("emailContent"));
                        log.info("Email for notice {}: {}", noticeNo, emailSent ? "SENT" : "FAILED");
                    } else if (sendEmail) {
                        log.info("Email sending is disabled");
                    }
                    
                    // 4. Save send status
                    boolean statusSaved = notificationHelper.saveSendStatus(record, smsSent, emailSent);

                    // 5. OCMS 10: Trigger PS-ANS suspension for Advisory Notices after successful send
                    String anFlag = (String) record.get("anFlag");
                    if ("Y".equalsIgnoreCase(anFlag) && (smsSent || emailSent)) {
                        log.info("AN detected (an_flag='Y'), triggering PS-ANS suspension for notice: {}", noticeNo);
                        boolean suspensionApplied = notificationHelper.triggerPsAnsSuspension(noticeNo, "eAN sent successfully");
                        if (suspensionApplied) {
                            log.info("PS-ANS suspension successfully applied for AN notice: {}", noticeNo);
                        } else {
                            log.warn("Failed to apply PS-ANS suspension for AN notice: {} - but continuing", noticeNo);
                        }
                    }

                    // 6. Update stage to RD1 if original stage was ENA
                    // if ("ENA".equals(record.get("nextProcessingStage"))) {
                    //     notificationHelper.updateStageToRd1(record);
                    // }

                    // Return true if both SMS and email were handled properly
                    success = (!sendSms || smsSent) &&
                                (!sendEmail || (emailEnabled && emailSent) || !emailEnabled) &&
                                statusSaved;
                
                    
                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    
                    // Check if notice is in HST list and apply TS-HST if found
                    // if (noticeNo != null && notificationHelper.checkAndApplyHst(noticeNo)) {
                    //     hstCount++;
                    //     log.info("Applied TS-HST suspension for notice {} due to HST record", noticeNo);
                    // }
                    
                } catch (Exception e) {
                    String errorMsg = "Error processing record: " + e.getMessage();
                    log.error(errorMsg, e);
                    
                    // Collect error details
                    Map<String, String> errorDetail = new HashMap<>();
                    errorDetail.put("noticeNo", (String) record.get("noticeNo"));
                    errorDetail.put("error", errorMsg);
                    preparationErrors.add(errorDetail);
                    
                    failureCount++;
                }
            }
            
            // Send error notification if there were preparation errors
            if (!preparationErrors.isEmpty()) {
                notificationHelper.sendErrorNotification(preparationErrors);
            }
            
            // Record final statistics
            recordJobMetadata("successCount", String.valueOf(successCount));
            recordJobMetadata("failureCount", String.valueOf(failureCount));
            recordJobMetadata("hstCount", String.valueOf(hstCount));
            
            // Return success if at least one record was processed successfully
            boolean overallSuccess = successCount > 0 || records.isEmpty();
            String message = String.format(
                    "%s job completed. Processed %d records: %d successful, %d failed, %d HST applied",
                    isRetry ? "Retry" : "Main", records.size(), successCount, failureCount, hstCount);
            
            return new com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult(overallSuccess, message);
            
        } catch (Exception e) {
            log.error("Error executing job: {}", e.getMessage(), e);
            return new com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }
    
    // The processRecord and processRetryRecord methods have been inlined into doExecute
    // for better readability and to keep all workflow logic in one place
}