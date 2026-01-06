package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.helpers.NotificationSmsEmailHelper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import com.ocmsintranet.cronservice.utilities.smsutility.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Notification SMS and Email Retry Job.
 * This job implements the retry flow as specified:
 * 
 * Retry Flow (send_ena_reminder_retry - Daily at 14:00):
 * 1. Get list of SMS notifications with status='E' and date_sent=today
 * 2. Get list of Email notifications with status='E' and date_sent=today
 * 3. Process Resend SMS - for each SMS error record, resend and handle provider response
 * 4. Process Resend Email - for each Email error record, resend and handle provider response
 * 5. End - both branches complete their work for today's error records
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("send_ena_reminder_retry")
public class NotificationSmsEmailRetryJob extends TrackedCronJobTemplate {
    
    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    // SMS Batch Polling Configuration
    @Value("${sms.batch.poll.initial.delay.ms:300000}")
    private long pollInitialDelayMs;

    @Value("${sms.batch.poll.interval.ms:30000}")
    private long pollIntervalMs;

    @Value("${sms.batch.poll.timeout.ms:600000}")
    private long pollTimeoutMs;

    @Value("${sms.batch.size.limit:1000}")
    private int batchSizeLimit;

    @Value("${email.report.recipients}")
    private String emailRecipients;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final NotificationSmsEmailHelper notificationHelper;
    private final TableQueryService tableQueryService;
    private final SmsUtil smsUtil;
    private final EmailService emailService;

    // Store metadata for the job execution
    private Map<String, String> jobMetadata = new HashMap<>();

    /**
     * Result object for notification retry operations
     */
    private static class NotificationRetryResult {
        private final int successCount;
        private final List<Map<String, Object>> failedRecords;

        public NotificationRetryResult(int successCount, List<Map<String, Object>> failedRecords) {
            this.successCount = successCount;
            this.failedRecords = failedRecords != null ? failedRecords : new ArrayList<>();
        }

        public int getSuccessCount() {
            return successCount;
        }

        public List<Map<String, Object>> getFailedRecords() {
            return failedRecords;
        }
    }

    /**
     * Inner class to hold batch polling result
     */
    private static class BatchPollResult {
        private String batchId;
        private boolean allTerminal;
        private boolean wholeBatchFailed;
        private boolean timedOut;
        private boolean alreadyRetried;
        private int successCount;
        private int failureCount;
        private int pendingCount;
        private List<Map<String, Object>> messageStatuses;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public boolean isAllTerminal() { return allTerminal; }
        public void setAllTerminal(boolean allTerminal) { this.allTerminal = allTerminal; }
        public boolean isWholeBatchFailed() { return wholeBatchFailed; }
        public void setWholeBatchFailed(boolean wholeBatchFailed) { this.wholeBatchFailed = wholeBatchFailed; }
        public boolean isTimedOut() { return timedOut; }
        public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }
        public boolean isAlreadyRetried() { return alreadyRetried; }
        public void setAlreadyRetried(boolean alreadyRetried) { this.alreadyRetried = alreadyRetried; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        public List<Map<String, Object>> getMessageStatuses() { return messageStatuses; }
        public void setMessageStatuses(List<Map<String, Object>> messageStatuses) { this.messageStatuses = messageStatuses; }
    }

    @Value("${cron.enareminder.retry.shedlock.name:send_ena_reminder_retry}")
    private String jobName;

    public NotificationSmsEmailRetryJob(
            NotificationSmsEmailHelper notificationHelper,
            TableQueryService tableQueryService,
            SmsUtil smsUtil,
            EmailService emailService) {
        this.notificationHelper = notificationHelper;
        this.tableQueryService = tableQueryService;
        this.smsUtil = smsUtil;
        this.emailService = emailService;
    }

    /**
     * Records job metadata for tracking and reporting
     */
    private void recordJobMetadata(String key, String value) {
        jobMetadata.put(key, value);
        log.info("Retry Job Metadata: {} = {}", key, value);
    }

    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for notification retry job");
        
        try {
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
        super.initialize();
        
        jobMetadata.clear();
        jobMetadata.put("jobStartTime", LocalDateTime.now().toString());
        jobMetadata.put("jobType", "retry");
    }
    
    @Override
    protected void cleanup() {
        jobMetadata.clear();
        super.cleanup();
    }

    @Override
    protected com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult doExecute() {
        log.info("Starting notification retry job execution (send_ena_reminder_retry)");
        
        try {
            // Define today (date-only)
            LocalDate today = LocalDate.now();
            log.info("Processing retry notifications for date: {}", today);
            
            // Get list of SMS notifications with status='E' and date_sent=today
            List<Map<String, Object>> smsErrorRecords = getSmsErrorRecords(today);
            log.info("Found {} SMS error records for retry", smsErrorRecords.size());
            
            // Get list of Email notifications with status='E' and date_sent=today  
            List<Map<String, Object>> emailErrorRecords = getEmailErrorRecords(today);
            log.info("Found {} Email error records for retry", emailErrorRecords.size());
            
            // Record metadata
            recordJobMetadata("smsErrorCount", String.valueOf(smsErrorRecords.size()));
            recordJobMetadata("emailErrorCount", String.valueOf(emailErrorRecords.size()));
            
            // Process Resend SMS
            NotificationRetryResult smsResult = processResendSms(smsErrorRecords);

            // Process Resend Email
            NotificationRetryResult emailResult = processResendEmail(emailErrorRecords);

            // Process failed notification results - update notice processing stages
            processFailedNotificationResults(smsResult.getFailedRecords(), emailResult.getFailedRecords());
            
            // Record final statistics
            recordJobMetadata("smsSuccessCount", String.valueOf(smsResult.getSuccessCount()));
            recordJobMetadata("emailSuccessCount", String.valueOf(emailResult.getSuccessCount()));
            recordJobMetadata("smsFailureCount", String.valueOf(smsResult.getFailedRecords().size()));
            recordJobMetadata("emailFailureCount", String.valueOf(emailResult.getFailedRecords().size()));

            // Job completes successfully if we processed all available records
            boolean overallSuccess = true;
            int totalProcessed = smsErrorRecords.size() + emailErrorRecords.size();

            String message = String.format(
                "Retry job completed. Processed %d error records: %d SMS (%d success), %d Email (%d success)",
                totalProcessed, smsErrorRecords.size(), smsResult.getSuccessCount(),
                emailErrorRecords.size(), emailResult.getSuccessCount());
            
            log.info(message);
            return new com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult(overallSuccess, message);
            
        } catch (Exception e) {
            log.error("Error executing retry job: {}", e.getMessage(), e);
            return new com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult(false, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Get SMS notifications with status='E' and date_sent=today
     * Uses helper method to get all retry records then filters for SMS only
     */
    private List<Map<String, Object>> getSmsErrorRecords(LocalDate today) {
        try {
            // Use helper method to get all retry records
            List<Map<String, Object>> allRetryRecords = notificationHelper.queryRecordsForRetry();
            
            // Filter for SMS records only
            List<Map<String, Object>> smsRecords = new ArrayList<>();
            for (Map<String, Object> record : allRetryRecords) {
                String notificationType = (String) record.get("notification_type");
                if ("SMS".equals(notificationType)) {
                    smsRecords.add(record);
                }
            }
            
            return smsRecords;
        } catch (Exception e) {
            log.error("Error querying SMS error records: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get Email notifications with status='E' and date_sent=today
     * Uses helper method to get all retry records then filters for Email only
     */
    private List<Map<String, Object>> getEmailErrorRecords(LocalDate today) {
        try {
            // Use helper method to get all retry records
            List<Map<String, Object>> allRetryRecords = notificationHelper.queryRecordsForRetry();
            
            // Filter for Email records only
            List<Map<String, Object>> emailRecords = new ArrayList<>();
            for (Map<String, Object> record : allRetryRecords) {
                String notificationType = (String) record.get("notification_type");
                if ("EMAIL".equals(notificationType)) {
                    emailRecords.add(record);
                }
            }
            
            return emailRecords;
        } catch (Exception e) {
            log.error("Error querying Email error records: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Process Resend SMS with corrected batch flow:
     * 1. Group records by batch_id
     * 2. For each batch_id, retrieve batch to get latest status
     * 3. If latest status = success → UPDATE existing record to S
     * 4. If latest status != success → Add to resend list (keep old record as-is)
     * 5. Send NEW batch for resend list → gets NEW batch_id
     * 6. INSERT new records with NEW batch_id
     * 7. Send summary email
     */
    private NotificationRetryResult processResendSms(List<Map<String, Object>> smsRecords) {
        log.info("Processing SMS resend for {} records", smsRecords.size());

        if (!smsEnabled) {
            log.info("SMS sending is disabled, skipping SMS retry");
            return new NotificationRetryResult(0, smsRecords);
        }

        if (smsRecords.isEmpty()) {
            log.info("No SMS records for retry");
            return new NotificationRetryResult(0, new ArrayList<>());
        }

        // Track results
        int alreadySuccessCount = 0;
        List<Map<String, Object>> recordsToResend = new ArrayList<>();
        List<Map<String, Object>> allFailedRecords = new ArrayList<>();

        // STEP 1: Group records by batch_id
        Map<String, List<Map<String, Object>>> batchGroups = groupRecordsByBatchId(smsRecords);
        log.info("Grouped {} records into {} batch groups", smsRecords.size(), batchGroups.size());

        // STEP 2-4: For each batch_id, retrieve latest status and partition records
        for (Map.Entry<String, List<Map<String, Object>>> entry : batchGroups.entrySet()) {
            String batchId = entry.getKey();
            List<Map<String, Object>> batchRecords = entry.getValue();

            if (batchId == null || batchId.isEmpty()) {
                // No batch_id - cannot retrieve, assume still failed
                log.warn("Records without batch_id found, adding to resend list");
                for (Map<String, Object> record : batchRecords) {
                    prepareRecordForResend(record);
                    recordsToResend.add(record);
                }
                continue;
            }

            log.info("Checking latest status for batch {} with {} records", batchId, batchRecords.size());

            // Retrieve batch to get latest status
            try {
                JsonNode response = smsUtil.retrieveBatchMessages(batchId, 1000);

                if (response.has("status") && "failed".equals(response.path("status").asText())) {
                    log.warn("Failed to retrieve batch {}, adding all records to resend", batchId);
                    for (Map<String, Object> record : batchRecords) {
                        prepareRecordForResend(record);
                        recordsToResend.add(record);
                    }
                    continue;
                }

                // Build recipient -> latestStatus map
                Map<String, Map<String, Object>> recipientStatusMap = new HashMap<>();
                JsonNode data = response.path("data");
                if (data.isArray()) {
                    for (JsonNode message : data) {
                        String recipient = message.path("recipient").asText();
                        Map<String, Object> statusInfo = new HashMap<>();
                        statusInfo.put("latestStatus", message.path("latestStatus").asText());
                        statusInfo.put("messageAttempts", message.path("messageAttempts"));
                        recipientStatusMap.put(recipient, statusInfo);
                    }
                }

                // Check each record's latest status
                for (Map<String, Object> record : batchRecords) {
                    String mobileNo = (String) record.get("mobile_no");
                    String normalizedPhone = notificationHelper.normalizeMobile(mobileNo);
                    Map<String, Object> statusInfo = recipientStatusMap.get(normalizedPhone);

                    if (statusInfo != null && "success".equals(statusInfo.get("latestStatus"))) {
                        // Already success - UPDATE existing record to S
                        log.info("Notice {} already delivered (batch {}), updating to S",
                                record.get("notice_no"), batchId);
                        updateSmsRecordStatus(record, "S", "success");
                        alreadySuccessCount++;
                    } else {
                        // Still failed or not found - add to resend list
                        log.info("Notice {} still not success (batch {}), adding to resend",
                                record.get("notice_no"), batchId);
                        prepareRecordForResend(record);
                        recordsToResend.add(record);
                    }
                }

            } catch (Exception e) {
                log.error("Error retrieving batch {}: {}", batchId, e.getMessage());
                // On error, add all to resend list
                for (Map<String, Object> record : batchRecords) {
                    prepareRecordForResend(record);
                    recordsToResend.add(record);
                }
            }
        }

        log.info("After checking batches: {} already success, {} need resend",
                alreadySuccessCount, recordsToResend.size());

        // STEP 5-6: Send new batch for records that still need resend
        int resendSuccessCount = 0;
        int resendFailureCount = 0;
        List<String> newBatchIds = new ArrayList<>();

        if (!recordsToResend.isEmpty()) {
            // Prepare batch records
            List<Map<String, String>> batchRecordsToSend = new ArrayList<>();
            List<Map<String, Object>> validRecordsToSend = new ArrayList<>();

            for (Map<String, Object> record : recordsToResend) {
                String smsContent = (String) record.get("smsContent");
                String normalizedPhone = (String) record.get("normalizedPhone");

                if (smsContent != null && normalizedPhone != null) {
                    Map<String, String> batchRecord = new HashMap<>();
                    batchRecord.put("phoneNumber", normalizedPhone);
                    batchRecord.put("language", "english");
                    batchRecord.put("message", smsContent);
                    batchRecordsToSend.add(batchRecord);
                    validRecordsToSend.add(record);
                }
            }

            if (!batchRecordsToSend.isEmpty()) {
                // Split into batches of batchSizeLimit
                List<List<Map<String, String>>> batches = splitIntoBatches(batchRecordsToSend, batchSizeLimit);
                List<List<Map<String, Object>>> recordBatches = splitIntoBatches(validRecordsToSend, batchSizeLimit);

                log.info("Sending {} records in {} new batches", batchRecordsToSend.size(), batches.size());

                for (int i = 0; i < batches.size(); i++) {
                    List<Map<String, String>> batch = batches.get(i);
                    List<Map<String, Object>> recordBatch = recordBatches.get(i);

                    log.info("Processing new resend batch {}/{} with {} records", i + 1, batches.size(), batch.size());

                    // Send as NEW batch
                    ObjectNode result = smsUtil.sendBatchSmsSimple(batch);

                    // Capture CSV content sent to Postman for error reporting
                    String csvContent = result.path("csvContent").asText(null);

                    if (!"success".equals(result.path("status").asText())) {
                        String errorMessage = result.path("message").asText();
                        log.error("New resend batch {}/{} send failed: {}", i + 1, batches.size(), errorMessage);

                        // INSERT new records with error status
                        String failedBatchId = "failed_" + System.currentTimeMillis();
                        for (Map<String, Object> record : recordBatch) {
                            insertNewSmsRecord(record, "E", "batch_send_failed: " + errorMessage, failedBatchId);
                            allFailedRecords.add(record);
                        }

                        // Send error email with actual CSV that was sent to Postman
                        sendBatchFailureEmail(recordBatch, failedBatchId, errorMessage, csvContent);
                        resendFailureCount += recordBatch.size();
                        continue;
                    }

                    // Get new batchId from response
                    String newBatchId = result.path("batchId").asText();
                    if (newBatchId == null || newBatchId.isEmpty()) {
                        newBatchId = result.path("response").path("batchId").asText();
                    }
                    newBatchIds.add(newBatchId);
                    log.info("New resend batch {}/{} sent. BatchId: {}", i + 1, batches.size(), newBatchId);

                    // Poll until terminal status
                    BatchPollResult pollResult = pollBatchUntilTerminal(newBatchId);

                    // Retry once if whole batch failed
                    if (pollResult.isWholeBatchFailed() && !pollResult.isAlreadyRetried()) {
                        log.info("Whole new batch {} failed, retrying once...", newBatchId);
                        ObjectNode retryResult = smsUtil.retryBatch(newBatchId);
                        if ("success".equals(retryResult.path("status").asText())) {
                            pollResult = pollBatchUntilTerminal(newBatchId);
                            pollResult.setAlreadyRetried(true);
                        }
                    }

                    // INSERT new records with new batch_id and status
                    mapBatchStatusAndInsertNewRecords(recordBatch, pollResult, newBatchId);

                    // Count results
                    for (Map<String, Object> record : recordBatch) {
                        String status = (String) record.get("smsStatus");
                        if ("S".equals(status)) {
                            resendSuccessCount++;
                        } else {
                            resendFailureCount++;
                            allFailedRecords.add(record);
                        }
                    }
                }
            }
        }

        // STEP 7: Send summary email
        int totalSuccess = alreadySuccessCount + resendSuccessCount;
        sendResendSummaryEmail(smsRecords.size(), alreadySuccessCount, recordsToResend.size(),
                resendSuccessCount, resendFailureCount, newBatchIds);

        log.info("SMS resend complete. Total: {}, Already Success: {}, Resent: {}, Resend Success: {}, Resend Failed: {}",
                smsRecords.size(), alreadySuccessCount, recordsToResend.size(), resendSuccessCount, resendFailureCount);

        return new NotificationRetryResult(totalSuccess, allFailedRecords);
    }

    /**
     * Group records by their batch_id
     */
    private Map<String, List<Map<String, Object>>> groupRecordsByBatchId(List<Map<String, Object>> records) {
        Map<String, List<Map<String, Object>>> groups = new HashMap<>();
        for (Map<String, Object> record : records) {
            String batchId = (String) record.get("batch_id");
            groups.computeIfAbsent(batchId, k -> new ArrayList<>()).add(record);
        }
        return groups;
    }

    /**
     * Prepare a record for resend by extracting and validating content
     */
    private void prepareRecordForResend(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("notice_no");
            record.put("noticeNo", noticeNo);
            record.put("phoneNo", record.get("mobile_no"));

            // Get SMS content from stored content
            String smsContent = null;
            Object contentObj = record.get("content");
            if (contentObj instanceof byte[]) {
                smsContent = new String((byte[]) contentObj, StandardCharsets.UTF_8);
            } else if (contentObj instanceof String) {
                smsContent = (String) contentObj;
            }

            if (smsContent != null && !smsContent.isEmpty()) {
                smsContent = smsUtil.validateSmsBody(smsContent);
                record.put("smsContent", smsContent);
            }

            // Normalize phone number
            String mobileNo = (String) record.get("mobile_no");
            if (mobileNo != null && !mobileNo.isEmpty()) {
                String normalizedPhone = notificationHelper.normalizeMobile(mobileNo);
                record.put("normalizedPhone", normalizedPhone);
            }
        } catch (Exception e) {
            log.error("Error preparing record for resend: {}", e.getMessage());
        }
    }

    /**
     * Map batch status from poll result and INSERT new records with new batch_id
     */
    private void mapBatchStatusAndInsertNewRecords(List<Map<String, Object>> records, BatchPollResult pollResult, String newBatchId) {
        Map<String, Map<String, Object>> phoneToStatus = new HashMap<>();

        // Build phone -> status map from poll result
        if (pollResult.getMessageStatuses() != null) {
            for (Map<String, Object> msgStatus : pollResult.getMessageStatuses()) {
                String recipient = (String) msgStatus.get("recipient");
                phoneToStatus.put(recipient, msgStatus);
            }
        }

        // Map status to records and INSERT new records
        for (Map<String, Object> record : records) {
            String normalizedPhone = (String) record.get("normalizedPhone");
            Map<String, Object> msgStatus = phoneToStatus.get(normalizedPhone);

            String status;
            String detailedStatus;

            if (msgStatus != null) {
                String latestStatus = (String) msgStatus.get("latestStatus");

                switch (latestStatus) {
                    case "success":
                        status = "S";
                        detailedStatus = "success";
                        break;
                    case "created":
                        status = "P";
                        detailedStatus = "created";
                        break;
                    default:
                        status = "E";
                        detailedStatus = buildDetailedStatus(latestStatus, msgStatus.get("messageAttempts"));
                        break;
                }
            } else {
                status = "E";
                detailedStatus = "status_not_found";
            }

            record.put("smsStatus", status);
            record.put("smsMsgStatus", detailedStatus);

            // INSERT new record with new batch_id (keeps old record, creates new one)
            insertNewSmsRecord(record, status, detailedStatus, newBatchId);
        }
    }

    /**
     * INSERT new SMS notification record with new batch_id
     * This creates a new row (new PK: notice_no + processing_stage + batch_id)
     */
    private void insertNewSmsRecord(Map<String, Object> record, String status, String msgStatus, String newBatchId) {
        try {
            Map<String, Object> newRecord = new HashMap<>();
            newRecord.put("noticeNo", record.get("notice_no"));
            newRecord.put("processingStage", record.get("processing_stage"));
            newRecord.put("batchId", newBatchId);  // NEW batch_id = NEW record
            newRecord.put("status", status);
            newRecord.put("msgStatus", msgStatus);
            newRecord.put("mobileNo", record.get("mobile_no"));
            newRecord.put("mobileCode", record.get("mobile_code") != null ? record.get("mobile_code") : "65");
            newRecord.put("dateSent", LocalDateTime.now());
            newRecord.put("creDate", LocalDateTime.now());
            newRecord.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // Copy content from original record
            Object content = record.get("content");
            if (content != null) {
                newRecord.put("content", content);
            } else {
                String smsContent = (String) record.get("smsContent");
                if (smsContent != null) {
                    newRecord.put("content", smsContent.getBytes(StandardCharsets.UTF_8));
                }
            }

            tableQueryService.post("ocms_sms_notification_records", newRecord);
            log.info("Inserted new SMS record for notice {} with new batchId {}", record.get("notice_no"), newBatchId);
        } catch (Exception e) {
            log.error("Error inserting new SMS record: {}", e.getMessage(), e);
        }
    }

    /**
     * Send resend summary email with detailed breakdown
     */
    private void sendResendSummaryEmail(int totalRecords, int alreadySuccessCount, int resendCount,
                                         int resendSuccessCount, int resendFailureCount, List<String> newBatchIds) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            StringBuilder htmlBuilder = new StringBuilder()
                    .append("<html><body>")
                    .append("<h2 style='color: #1976d2;'>SMS Batch Resend Summary</h2>")
                    .append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Timestamp</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(timestamp).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Total Non-Success Records</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(totalRecords).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Already Success (from retrieve)</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(alreadySuccessCount).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Resent</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(resendCount).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Resend Success</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(resendSuccessCount).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Resend Failed</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(resendFailureCount).append("</td></tr>");

            if (!newBatchIds.isEmpty()) {
                htmlBuilder.append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>New Batch IDs</strong></td>")
                        .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(String.join(", ", newBatchIds)).append("</td></tr>");
            }

            htmlBuilder.append("</table>")
                    .append("<hr style='margin-top: 20px;'>")
                    .append("<p style='color: #666; font-size: 12px;'>This is an automated message from OCMS Intranet Cron Service.</p>")
                    .append("</body></html>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setSubject("[" + activeProfile.toUpperCase() + "] OCMS SMS Batch Resend Summary - " + LocalDate.now());
            emailRequest.setHtmlContent(htmlBuilder.toString());

            emailService.sendEmail(emailRequest);
            log.info("Sent SMS batch resend summary email to {}", emailRecipients);
        } catch (Exception e) {
            log.error("Error sending resend summary email: {}", e.getMessage(), e);
        }
    }

    /**
     * Split a list into batches of specified size
     */
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return batches;
    }

    /**
     * Poll batch status until all messages reach terminal state (success or failure)
     */
    private BatchPollResult pollBatchUntilTerminal(String batchId) {
        BatchPollResult result = new BatchPollResult();
        result.setBatchId(batchId);

        // Initial wait before first poll to allow SMS provider time to process
        if (pollInitialDelayMs > 0) {
            log.info("Batch {} waiting {}ms before first status check...", batchId, pollInitialDelayMs);
            try {
                Thread.sleep(pollInitialDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Batch {} initial wait interrupted", batchId);
                result.setTimedOut(true);
                return result;
            }
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < pollTimeoutMs) {
            try {
                JsonNode response = smsUtil.retrieveBatchMessages(batchId, 1000);

                // Check for error response - retry instead of exiting
                if (response.has("status") && "failed".equals(response.path("status").asText())) {
                    log.warn("Failed to retrieve batch {}: {}. Retrying in {}ms...",
                            batchId, response.path("message").asText(), pollIntervalMs);
                    Thread.sleep(pollIntervalMs);
                    continue;
                }

                // Parse message statuses - retry if no data
                JsonNode data = response.path("data");
                if (data.isMissingNode() || !data.isArray()) {
                    log.warn("No data in batch response for {}. Retrying in {}ms...", batchId, pollIntervalMs);
                    Thread.sleep(pollIntervalMs);
                    continue;
                }

                int successCount = 0;
                int failureCount = 0;
                int pendingCount = 0;
                List<Map<String, Object>> messageStatuses = new ArrayList<>();

                for (JsonNode message : data) {
                    String latestStatus = message.path("latestStatus").asText();
                    String recipient = message.path("recipient").asText();

                    Map<String, Object> msgStatus = new HashMap<>();
                    msgStatus.put("recipient", recipient);
                    msgStatus.put("latestStatus", latestStatus);
                    msgStatus.put("messageAttempts", message.path("messageAttempts"));
                    messageStatuses.add(msgStatus);

                    if ("success".equals(latestStatus)) {
                        successCount++;
                    } else if ("failure".equals(latestStatus)) {
                        failureCount++;
                    } else {
                        pendingCount++;
                    }
                }

                result.setSuccessCount(successCount);
                result.setFailureCount(failureCount);
                result.setPendingCount(pendingCount);
                result.setMessageStatuses(messageStatuses);

                // Check if all terminal
                if (pendingCount == 0) {
                    log.info("Batch {} all terminal. Success: {}, Failure: {}",
                            batchId, successCount, failureCount);
                    result.setAllTerminal(true);
                    result.setWholeBatchFailed(successCount == 0 && failureCount > 0);
                    return result;
                }

                log.info("Batch {} polling: Success={}, Failure={}, Pending={}. Waiting {}ms...",
                        batchId, successCount, failureCount, pendingCount, pollIntervalMs);

                Thread.sleep(pollIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Batch polling interrupted for {}", batchId);
                result.setTimedOut(true);
                return result;
            } catch (Exception e) {
                log.error("Error polling batch {}: {}", batchId, e.getMessage());
                result.setTimedOut(true);
                return result;
            }
        }

        log.warn("Batch {} polling timed out after {}ms", batchId, pollTimeoutMs);
        result.setTimedOut(true);
        return result;
    }

    /**
     * Build detailed status string from message attempts.
     * Format: latestStatus|sentAt:xxx|deliveredAt:xxx|errorType:xxx|errorCode:xxx
     */
    private String buildDetailedStatus(String latestStatus, Object messageAttempts) {
        if (messageAttempts == null) {
            return latestStatus;
        }

        try {
            JsonNode attempts = (JsonNode) messageAttempts;
            if (attempts.isArray() && attempts.size() > 0) {
                JsonNode lastAttempt = attempts.get(attempts.size() - 1);
                return String.format("%s|sentAt:%s|deliveredAt:%s|errorType:%s|errorCode:%s",
                        latestStatus,
                        lastAttempt.path("sentAt").asText(""),
                        lastAttempt.path("deliveredAt").asText(""),
                        lastAttempt.path("errorType").asText(""),
                        lastAttempt.path("errorCode").asText(""));
            }
        } catch (Exception e) {
            log.warn("Error building detailed status: {}", e.getMessage());
        }

        return latestStatus;
    }

    /**
     * Send failure email with CSV attachment when batch SMS POST fails.
     * Attaches the actual CSV file that was sent to Postman API.
     *
     * @param failedRecords List of records that failed
     * @param batchId The batch ID (or "failed_xxx" if POST failed)
     * @param errorMessage The error message
     * @param postmanCsvContent The actual CSV content that was sent to Postman API
     */
    private void sendBatchFailureEmail(List<Map<String, Object>> failedRecords, String batchId,
            String errorMessage, String postmanCsvContent) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            StringBuilder htmlBuilder = new StringBuilder()
                    .append("<html><body>")
                    .append("<h2 style='color: #d32f2f;'>SMS Batch Resend Failed</h2>")
                    .append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Timestamp</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(timestamp).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Batch ID</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(batchId).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Failed Records</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(failedRecords.size()).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Error</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px; color: #d32f2f;'>").append(errorMessage).append("</td></tr>")
                    .append("</table>")
                    .append("<p style='margin-top: 20px;'>Please see attached CSV file that was sent to Postman API.</p>");

            // Add note if CSV not available
            if (postmanCsvContent == null || postmanCsvContent.isEmpty()) {
                htmlBuilder.append("<p><strong>Note:</strong> CSV content was not available for attachment.</p>");
            }

            htmlBuilder.append("<hr style='margin-top: 20px;'>")
                    .append("<p style='color: #666; font-size: 12px;'>This is an automated message from OCMS Intranet Cron Service.</p>")
                    .append("</body></html>");

            String htmlContent = htmlBuilder.toString();

            // Create email request with attachment
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setSubject("[" + activeProfile.toUpperCase() + "] OCMS SMS Batch Resend Failed - " + LocalDate.now());
            emailRequest.setHtmlContent(htmlContent);

            // Add the actual Postman CSV as attachment
            if (postmanCsvContent != null && !postmanCsvContent.isEmpty()) {
                EmailRequest.Attachment attachment = new EmailRequest.Attachment();
                attachment.setFileName("postman_batch_resend_sms_" + LocalDate.now() + ".csv");
                attachment.setFileContent(postmanCsvContent.getBytes(StandardCharsets.UTF_8));
                emailRequest.setAttachments(Collections.singletonList(attachment));
            } else {
                log.warn("No CSV content available to attach to failure email");
            }

            emailService.sendEmail(emailRequest);
            log.info("Sent batch resend failure email with Postman CSV attachment to {}", emailRecipients);
        } catch (Exception e) {
            log.error("Error sending batch failure email: {}", e.getMessage(), e);
        }
    }

    /**
     * Process Resend Email
     * For each item in the Email list: Send the email (subject/body as stored)
     */
    private NotificationRetryResult processResendEmail(List<Map<String, Object>> emailErrorRecords) {
        log.info("Processing Email resend for {} records", emailErrorRecords.size());
        
        if (!emailEnabled) {
            log.info("Email sending is disabled, skipping Email retry");
            return new NotificationRetryResult(0, emailErrorRecords);
        }

        int successCount = 0;
        List<Map<String, Object>> failedRecords = new ArrayList<>();
        
        for (Map<String, Object> record : emailErrorRecords) {
            try {
                String noticeNo = (String) record.get("notice_no");
                log.info("Retrying Email for notice: {}", noticeNo);
                
                // Map database fields to expected helper method fields
                record.put("noticeNo", noticeNo); // Map notice_no to noticeNo for helper method
                record.put("email", record.get("email_addr")); // Map email_addr to email for helper method
                
                // Use stored email subject/body from the record
                String emailSubject = (String) record.get("subject");
                String emailContent = (String) record.get("content");
                
                if (emailContent == null || emailContent.isEmpty()) {
                    // Try to get content from BLOB field if it exists
                    byte[] contentBytes = (byte[]) record.get("content");
                    if (contentBytes != null) {
                        emailContent = new String(contentBytes);
                    }
                }
                
                if (emailSubject == null || emailContent == null || 
                    emailSubject.isEmpty() || emailContent.isEmpty()) {
                    log.warn("No stored email content found for notice {}, skipping", noticeNo);
                    continue;
                }
                
                // Check if email address is available
                String emailAddr = (String) record.get("email_addr");
                if (emailAddr == null || emailAddr.isEmpty()) {
                    log.warn("No email address found for notice {}, skipping", noticeNo);
                    continue;
                }
                
                // Send the email using stored subject/body
                boolean emailSent = notificationHelper.sendEmail(record, emailSubject, emailContent);
                
                // Refer to subflow - handle provider response
                boolean subflowResult = handleEmailProviderResponse(record, emailSent);
                
                if (subflowResult) {
                    successCount++;
                    log.info("Email retry successful for notice: {}", noticeNo);
                } else {
                    failedRecords.add(record);
                    log.error("Email retry failed for notice: {}", noticeNo);
                }
                
            } catch (Exception e) {
                log.error("Error processing Email retry for record: {}", e.getMessage(), e);
                failedRecords.add(record);
            }
        }

        log.info("Email resend completed: {}/{} successful", successCount, emailErrorRecords.size());
        return new NotificationRetryResult(successCount, failedRecords);
    }

    /**
     * Email Subflow - Handle provider response (success/failure, mapping back to record state)
     * Returns control to the Email step, which then proceeds to End
     */
    private boolean handleEmailProviderResponse(Map<String, Object> record, boolean emailSent) {
        try {
            String noticeNo = (String) record.get("notice_no");
            
            if (emailSent) {
                // Update Email record status to success with empty remarks
                updateEmailRecordStatus(record, "S", "");
                log.debug("Email provider response: SUCCESS for notice {}", noticeNo);
                return true;
            } else {
                // Update Email record status to failed
                updateEmailRecordStatus(record, "E", "Email retry failed");
                log.debug("Email provider response: FAILURE for notice {}", noticeNo);
                return false;
            }
        } catch (Exception e) {
            log.error("Error handling Email provider response: {}", e.getMessage(), e);
            // Update status to failed with exception message as remarks
            updateEmailRecordStatus(record, "E", e.getMessage());
            return false;
        }
    }
    
    /**
     * Update SMS notification record status
     * PK: notice_no + processing_stage + batch_id
     */
    private void updateSmsRecordStatus(Map<String, Object> record, String status, String remarks) {
        try {
            // Prepare update data
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", status);
            updateData.put("msgStatus", remarks);
            updateData.put("updDate", LocalDateTime.now());
            updateData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // Filter by composite primary key: notice_no + processing_stage + batch_id
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", record.get("notice_no"));
            filter.put("processingStage", record.get("processing_stage"));
            filter.put("batchId", record.get("batch_id"));

            // Update the ocms_sms_notification_records table
            tableQueryService.patch("ocms_sms_notification_records", filter, updateData);
            log.info("Updated SMS record status for notice {}, stage {}, batch {}",
                    record.get("notice_no"), record.get("processing_stage"), record.get("batch_id"));
        } catch (Exception e) {
            log.error("Error updating SMS record status: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update Email notification record status
     */
    private void updateEmailRecordStatus(Map<String, Object> record, String status, String remarks) {
        try {
            // Prepare update data
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", status);
            updateData.put("msgStatus", remarks);
            updateData.put("updDate", LocalDateTime.now());
            updateData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            // Filter by notice_no and processing_stage (composite primary key)
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", record.get("notice_no"));
            filter.put("processingStage", record.get("processing_stage"));
            
            // Update the ocms_email_notification_records table
            tableQueryService.patch("ocms_email_notification_records", filter, updateData);
            log.info("Updated Email record status for notice {} and stage {}", record.get("notice_no"), record.get("processing_stage"));
        } catch (Exception e) {
            log.error("Error updating Email record status: {}", e.getMessage(), e);
        }
    }

    /**
     * Process failed notification results and update notice processing stages
     * For notices where both SMS and Email failed, update processing stage from ENA to RD1
     */
    private void processFailedNotificationResults(List<Map<String, Object>> smsFailedRecords,
                                                 List<Map<String, Object>> emailFailedRecords) {
        log.info("Processing failed notification results: {} SMS failures, {} Email failures",
                smsFailedRecords.size(), emailFailedRecords.size());

        try {
            // Get unique notice numbers from all failed records
            List<String> failedNoticeNumbers = getFailedNoticeNumbers(smsFailedRecords, emailFailedRecords);

            if (failedNoticeNumbers.isEmpty()) {
                log.info("No failed notices found for stage transition");
                return;
            }

            log.info("Found {} unique notices with notification failures", failedNoticeNumbers.size());
            recordJobMetadata("noticesForStageTransition", String.valueOf(failedNoticeNumbers.size()));

            int stageTransitionSuccessCount = 0;
            for (String noticeNo : failedNoticeNumbers) {
                try {
                    boolean success = updateNoticeProcessingStage(noticeNo);
                    if (success) {
                        stageTransitionSuccessCount++;
                        log.info("Successfully updated processing stage for notice: {}", noticeNo);
                    } else {
                        log.error("Failed to update processing stage for notice: {}", noticeNo);
                    }
                } catch (Exception e) {
                    log.error("Error updating processing stage for notice {}: {}", noticeNo, e.getMessage(), e);
                }
            }

            recordJobMetadata("stageTransitionSuccessCount", String.valueOf(stageTransitionSuccessCount));
            recordJobMetadata("stageTransitionFailureCount",
                            String.valueOf(failedNoticeNumbers.size() - stageTransitionSuccessCount));

            log.info("Stage transition processing completed: {}/{} notices successfully updated",
                    stageTransitionSuccessCount, failedNoticeNumbers.size());

        } catch (Exception e) {
            log.error("Error processing failed notification results: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract unique notice numbers from failed SMS and Email records
     */
    private List<String> getFailedNoticeNumbers(List<Map<String, Object>> smsFailedRecords,
                                               List<Map<String, Object>> emailFailedRecords) {
        Set<String> uniqueNoticeNumbers = new HashSet<>();

        // Add SMS failed notice numbers
        for (Map<String, Object> record : smsFailedRecords) {
            String noticeNo = (String) record.get("notice_no");
            if (noticeNo != null && !noticeNo.trim().isEmpty()) {
                uniqueNoticeNumbers.add(noticeNo.trim());
            }
        }

        // Add Email failed notice numbers
        for (Map<String, Object> record : emailFailedRecords) {
            String noticeNo = (String) record.get("notice_no");
            if (noticeNo != null && !noticeNo.trim().isEmpty()) {
                uniqueNoticeNumbers.add(noticeNo.trim());
            }
        }

        return new ArrayList<>(uniqueNoticeNumbers);
    }

    /**
     * Update notice processing stage from ENA to RD1 for failed notifications
     * Updates ocms_valid_offence_notice with new stage information
     */
    private boolean updateNoticeProcessingStage(String noticeNo) {
        try {
            log.debug("Updating processing stage for notice: {}", noticeNo);

            // First, query current notice state to validate it's in ENA stage
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            filter.put("lastProcessingStage", "ENA");

            List<Map<String, Object>> currentNotices = tableQueryService.query("ocms_valid_offence_notice", filter);

            if (currentNotices.isEmpty()) {
                log.warn("Notice {} not found or not in ENA stage, skipping stage transition", noticeNo);
                return false;
            }

            // Prepare update data for stage transition: ENA → RD1
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("prevProcessingStage", SystemConstant.SuspensionReason.ENA);
            updateData.put("prevProcessingDate", currentNotices.get(0).get("lastProcessingDate"));
            updateData.put("lastProcessingStage", SystemConstant.SuspensionReason.RD1);
            updateData.put("lastProcessingDate", LocalDate.now().plusDays(1)); // Next day
            updateData.put("nextProcessingStage", SystemConstant.SuspensionReason.RD2);
            updateData.put("nextProcessingDate", LocalDate.now().plusDays(3));
            updateData.put("updDate", LocalDateTime.now());
            updateData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // PROCESS 5: Set is_sync to N to trigger cron batch sync to internet DB
            updateData.put("isSync", "N");

            // Update the notice
            tableQueryService.patch("ocms_valid_offence_notice", filter, updateData);
            log.debug("Successfully updated notice {} processing stage from ENA to RD1 (isSync=N)", noticeNo);
            return true;

        } catch (Exception e) {
            log.error("Error updating processing stage for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }
}
