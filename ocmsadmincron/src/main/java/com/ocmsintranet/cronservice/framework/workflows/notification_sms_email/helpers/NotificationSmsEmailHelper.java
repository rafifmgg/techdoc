package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecords;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecordsService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecords;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsService;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveService;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import com.ocmsintranet.cronservice.utilities.smsutility.SmsUtil;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services.NotificationTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for the Notification SMS and Email workflow.
 * This class contains methods for:
 * - Querying records that need SMS/email notifications
 * - Generating SMS and email messages
 * - Sending SMS and email through notification services
 * - Saving send status for each message
 * - Checking if notice number is in HST list and applying TS-HST if found
 */
@Slf4j
@Component
public class NotificationSmsEmailHelper {

    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final TableQueryService tableQueryService;
    private final OcmsSmsNotificationRecordsService smsNotificationRecordsService;
    private final OcmsEmailNotificationRecordsService emailNotificationRecordsService;
    private final SmsUtil smsUtil;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final DataHiveService dataHiveService;
    private final SuspensionApiClient suspensionApiClient;
    private final NotificationTemplateService templateService;

    @Value("${notification.sms.retry.max:3}")
    private int maxSmsRetries;
    
    @Value("${notification.email.retry.max:3}")
    private int maxEmailRetries;
    
    @Value("${notification.processing.stage.ena:ENA}")
    private String enaStage;
    
    @Value("${notification.processing.stage.rd1:RD1}")
    private String rd1Stage;
    
    @Value("${notification.processing.stage.success:NPA}")
    private String successStage;

    @Value("${datahive.database}")
    private String datahiveDatabase;

    @Value("${datahive.schema}")
    private String datahiveSchema;

    @Value("${datahive.warehouse}")
    private String datahiveWarehouse;

    @Value("${datahive.role}")
    private String datahiveRole;

    @Value("${email.report.recipients}")
    private String emailRecipients;

    @Value("${cron.enareminder.datahive}")
    private String statusGetDatahive;

    // SMS Batch Polling Configuration
    @Value("${sms.batch.poll.initial.delay.ms:300000}")
    private long pollInitialDelayMs;

    @Value("${sms.batch.poll.interval.ms:30000}")
    private long pollIntervalMs;

    @Value("${sms.batch.poll.timeout.ms:600000}")
    private long pollTimeoutMs;

    @Value("${sms.batch.size.limit:1000}")
    private int batchSizeLimit;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public NotificationSmsEmailHelper(
            OcmsValidOffenceNoticeRepository validOffenceNoticeRepository,
            TableQueryService tableQueryService,
            OcmsSmsNotificationRecordsService smsNotificationRecordsService,
            OcmsEmailNotificationRecordsService emailNotificationRecordsService,
            SmsUtil smsUtil,
            EmailService emailService,
            JdbcTemplate jdbcTemplate,
            DataHiveService dataHiveService,
            SuspensionApiClient suspensionApiClient,
            NotificationTemplateService templateService) {
        this.validOffenceNoticeRepository = validOffenceNoticeRepository;
        this.tableQueryService = tableQueryService;
        this.smsNotificationRecordsService = smsNotificationRecordsService;
        this.emailNotificationRecordsService = emailNotificationRecordsService;
        this.smsUtil = smsUtil;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
        this.dataHiveService = dataHiveService;
        this.suspensionApiClient = suspensionApiClient;
        this.templateService = templateService;
    }

    /**
     * Query records that need initial SMS/email notifications
     * Uses the corrected repository query that properly JOINs tables
     * 
     * @return List of records to process
     */
    public List<Map<String, Object>> queryRecordsForInitialSend() {
        try {
            log.info("Querying records for initial notification");
            
            // Use the corrected repository query that handles the JOIN properly
            List<Object[]> results = validOffenceNoticeRepository.findRecordsForInitialNotification();
            
            // Convert Object[] results to Map<String, Object>
            List<Map<String, Object>> records = new ArrayList<>();
            
            for (Object[] row : results) {
                Map<String, Object> record = new HashMap<>();
                
                // Map the query results to field names (based on SELECT order)
                // CORRECTED: Maps id_no from database to nricNo in application
                record.put("noticeNo", row[0]);
                record.put("vehicleNo", row[1]);
                record.put("lastProcessingStage", row[2]);
                record.put("lastProcessingDate", row[3]);
                record.put("nextProcessingStage", row[4]);
                record.put("nextProcessingDate", row[5]);
                record.put("nricNo", row[6]);  // This maps onod.id_no as nricNo from query
                record.put("name", row[7]);
                record.put("phoneNo", row[8]);
                record.put("email", row[9]);
                
                records.add(record);
            }
            
            log.info("Found {} records for initial notification", records.size());
            return records;
            
        } catch (Exception e) {
            log.error("Error querying records for initial notification: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Query records that need retry SMS/email notifications
     * Queries both SMS and email notification tables for error records from today
     * 
     * @return List of records to retry
     */
    public List<Map<String, Object>> queryRecordsForRetry() {
        try {
            log.info("Querying records for retry notification");

            List<Map<String, Object>> allRecords = new ArrayList<>();

            // Get today's date in the format expected by the database
            String today = java.time.LocalDate.now().toString(); // Format: YYYY-MM-DD

            // Query SMS notification records with non-success status from today (latest batch per notice)
            // This includes status = 'P' (pending/created) and status = 'E' (error/failed)
            String smsQuery = """
                SELECT * FROM ocmsizmgr.ocms_sms_notification_records r1
                WHERE r1.status != 'S'
                AND CAST(r1.date_sent AS DATE) = '%s'
                AND r1.date_sent = (
                    SELECT MAX(r2.date_sent)
                    FROM ocmsizmgr.ocms_sms_notification_records r2
                    WHERE r2.notice_no = r1.notice_no
                    AND r2.processing_stage = r1.processing_stage
                )
                """.formatted(today);
            List<Map<String, Object>> smsRecords = jdbcTemplate.queryForList(smsQuery);

            // Add notification type to distinguish records
            for (Map<String, Object> record : smsRecords) {
                record.put("notification_type", "SMS");
            }
            allRecords.addAll(smsRecords);

            // Query Email notification records with non-success status from today (latest batch per notice)
            String emailQuery = """
                SELECT * FROM ocmsizmgr.ocms_email_notification_records r1
                WHERE r1.status != 'S'
                AND CAST(r1.date_sent AS DATE) = '%s'
                AND r1.date_sent = (
                    SELECT MAX(r2.date_sent)
                    FROM ocmsizmgr.ocms_email_notification_records r2
                    WHERE r2.notice_no = r1.notice_no
                    AND r2.processing_stage = r1.processing_stage
                )
                """.formatted(today);
            List<Map<String, Object>> emailRecords = jdbcTemplate.queryForList(emailQuery);

            // Add notification type to distinguish records
            for (Map<String, Object> record : emailRecords) {
                record.put("notification_type", "EMAIL");
            }
            allRecords.addAll(emailRecords);

            log.info("Found {} SMS records and {} email records for retry notification",
                    smsRecords.size(), emailRecords.size());
            return allRecords;

        } catch (Exception e) {
            log.error("Error querying records for retry notification", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate SMS and email messages for a record using database templates
     *
     * @param record The record to generate messages for
     * @return Map containing smsContent, emailSubject, and emailContent
     */
    public Map<String, String> generateMessages(Map<String, Object> record) {
        try {
            String noticeNo = trimValue((String) record.get("noticeNo"));
            log.debug("Generating messages for record: {}", noticeNo);

            // Check if this is an Advisory Notice (OCMS 10) - handle separately
            String anFlag = trimValue((String) record.get("anFlag"));
            if ("Y".equalsIgnoreCase(anFlag)) {
                log.info("Generating AN-specific messages for notice: {}", noticeNo);
                return generateAnMessages(record);
            }

            // Extract processing stage and case type to determine template
            String processingStage = trimValue((String) record.get("nextProcessingStage"));
            String caseType = trimValue((String) record.get("caseType")); // compoundable or non-compoundable
            String deliveryChannel = determineDeliveryChannel(record);

            // Check if this is a combined notice
            String combinedNotices = trimValue((String) record.get("combinedNotices"));
            boolean isCombined = combinedNotices != null && !combinedNotices.isEmpty() && combinedNotices.contains(".");

            // Prepare data map for placeholder replacement
            Map<String, Object> templateData = prepareTemplateData(record);

            String smsContent = "";
            String emailContent = "";
            String emailSubject = "";

            // Determine template name based on stage, channel, and case type
            String templateName = determineTemplateName(processingStage, deliveryChannel, caseType, isCombined);

            if (deliveryChannel.equals("SMS")) {
                // Fetch and process SMS template
                smsContent = templateService.getAndProcessTemplate(templateName, templateData);

                if (smsContent == null || smsContent.isEmpty()) {
                    log.warn("SMS template {} not found, using fallback", templateName);
                    smsContent = generateFallbackSMS(record, isCombined);
                }

            } else if (deliveryChannel.equals("EMAIL")) {
                // Fetch and process email template
                String emailTemplate = templateService.getAndProcessTemplate(templateName, templateData);

                if (emailTemplate == null || emailTemplate.isEmpty()) {
                    log.warn("Email template {} not found, using fallback", templateName);
                    Map<String, String> fallback = generateFallbackEmail(record, isCombined);
                    emailSubject = fallback.get("subject");
                    emailContent = fallback.get("body");
                } else {
                    // Split template into subject and body
                    Map<String, String> emailParts = templateService.splitEmailTemplate(emailTemplate);
                    emailSubject = emailParts.get("subject");
                    emailContent = emailParts.get("body");
                }
            }

            // Return all generated content
            Map<String, String> messages = new HashMap<>();
            messages.put("smsContent", smsContent);
            messages.put("emailSubject", emailSubject);
            messages.put("emailContent", emailContent);

            return messages;

        } catch (Exception e) {
            log.error("Error generating messages: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Generate AN-specific messages for Advisory Notice (OCMS 10)
     *
     * @param record The record containing AN notice data
     * @return Map containing smsContent, emailSubject, and emailContent for AN
     */
    private Map<String, String> generateAnMessages(Map<String, Object> record) {
        try {
            // Extract necessary data
            String noticeNo = trimValue((String) record.get("noticeNo"));
            String name = trimValue((String) record.get("name"));
            String vehicleNo = trimValue((String) record.get("vehicleNo"));

            // Generate AN-specific SMS content
            // TODO: Replace with actual approved template from BA/Product Owner
            String smsContent = String.format(
                "Advisory Notice: Your vehicle %s has been issued an Advisory Notice (%s) for repeat parking offences. " +
                "Repeated offences may result in penalties. For enquiries, call URA 6329 5400 during office hours.",
                vehicleNo, noticeNo
            );

            // Generate AN-specific email content
            String emailContent = String.format(
                "<html><body>" +
                "<h2>Advisory Notice - Action Required</h2>" +
                "<p>Dear %s,</p>" +
                "<p>This is to inform you that an <strong>Advisory Notice</strong> has been issued for repeat parking offences:</p>" +
                "<ul>" +
                "<li><strong>Notice Number:</strong> %s</li>" +
                "<li><strong>Vehicle Number:</strong> %s</li>" +
                "<li><strong>Notice Type:</strong> Advisory Notice</li>" +
                "</ul>" +
                "<p><strong>Important:</strong> This is an advisory notice to inform you of repeat parking offences. " +
                "Continued offences may result in enforcement action and penalties.</p>" +
                "<p>For enquiries, please contact URA at 6329 5400 during office hours.</p>" +
                "<p>Regards,<br>Urban Redevelopment Authority<br>Parking Operations</p>" +
                "</body></html>",
                name, noticeNo, vehicleNo
            );

            String emailSubject = String.format("Advisory Notice %s - Repeat Parking Offences", noticeNo);

            // Return all generated content
            Map<String, String> messages = new HashMap<>();
            messages.put("smsContent", smsContent);
            messages.put("emailSubject", emailSubject);
            messages.put("emailContent", emailContent);

            log.info("Generated AN messages for notice {}: SMS length={}, Email length={}",
                     noticeNo, smsContent.length(), emailContent.length());

            return messages;

        } catch (Exception e) {
            log.error("Error generating AN messages: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Prepare data map for template placeholder replacement
     */
    private Map<String, Object> prepareTemplateData(Map<String, Object> record) {
        Map<String, Object> data = new HashMap<>();

        // Add all fields from record with normalized keys
        data.put("Name", trimValue((String) record.get("name")));
        data.put("noticeno", trimValue((String) record.get("noticeNo")));
        data.put("vehicle no.", trimValue((String) record.get("vehicleNo")));
        data.put("amount", record.get("amountPayable"));

        // Handle payment due date
        Object dueDateObj = record.get("paymentDueDate");
        data.put("payment due date", dueDateObj != null ? dueDateObj.toString().trim() : "");

        // Handle combined notices
        String combinedNotices = trimValue((String) record.get("combinedNotices"));
        if (combinedNotices != null && !combinedNotices.isEmpty()) {
            String[] noticeNumbers = combinedNotices.split("\\.");
            data.put("notice count", noticeNumbers.length);
            data.put("combined notices", combinedNotices);
        }

        // Add other fields that may be needed
        data.put("processing stage", trimValue((String) record.get("nextProcessingStage")));
        data.put("email", trimValue((String) record.get("email")));
        data.put("phone", trimValue((String) record.get("phoneNo")));

        return data;
    }

    /**
     * Determine delivery channel based on record data (SMS for individuals, EMAIL for companies)
     */
    private String determineDeliveryChannel(Map<String, Object> record) {
        // UEN → Company → Email
        // NRIC/FIN/Passport → Individual → SMS
        String idType = trimValue((String) record.get("idType"));

        if (idType != null && idType.equalsIgnoreCase("UEN")) {
            return "EMAIL";
        }
        return "SMS"; // Default to SMS for NRIC/FIN/Passport
    }

    /**
     * Determine template name based on processing stage, delivery channel, case type
     * Format: {stage}_{channel}_{caseType}
     * Example: eAN_SMS_COMPOUNDABLE, eNOPO_EMAIL_NONCOMPOUNDABLE
     */
    private String determineTemplateName(String stage, String channel, String caseType, boolean isCombined) {
        // Normalize inputs
        stage = stage != null ? stage.toUpperCase() : "DEFAULT";
        channel = channel != null ? channel.toUpperCase() : "SMS";
        caseType = caseType != null ? caseType.toUpperCase() : "COMPOUNDABLE";

        // Build template name
        String templateName = stage + "_" + channel + "_" + caseType;

        // Add combined suffix if applicable
        if (isCombined) {
            templateName += "_COMBINED";
        }

        log.debug("Determined template name: {}", templateName);
        return templateName;
    }

    /**
     * Generate fallback SMS content when template is not found
     */
    private String generateFallbackSMS(Map<String, Object> record, boolean isCombined) {
        String name = trimValue((String) record.get("name"));
        String noticeNo = trimValue((String) record.get("noticeNo"));
        String vehicleNo = trimValue((String) record.get("vehicleNo"));
        BigDecimal amount = (BigDecimal) record.get("amountPayable");

        if (isCombined) {
            String combinedNotices = trimValue((String) record.get("combinedNotices"));
            String[] noticeNumbers = combinedNotices.split("\\.");
            int noticeCount = noticeNumbers.length;

            return String.format(
                "Dear %s. you have received %d offence notices (%s) for vehicle %s. " +
                "Total amount payable: %s. Please make payment by 31 Dec 2025.",
                name, noticeCount, combinedNotices, vehicleNo, amount
            );
        } else {
            return String.format(
                "Dear %s you have an offence notice %s for vehicle %s. " +
                "Amount payable: %s. Please make payment by 31 Dec 2025.",
                name, noticeNo, vehicleNo, amount
            );
        }
    }

    /**
     * Generate fallback email content when template is not found
     */
    private Map<String, String> generateFallbackEmail(Map<String, Object> record, boolean isCombined) {
        Map<String, String> result = new HashMap<>();

        String name = trimValue((String) record.get("name"));
        String noticeNo = trimValue((String) record.get("noticeNo"));
        String vehicleNo = trimValue((String) record.get("vehicleNo"));
        BigDecimal amount = (BigDecimal) record.get("amountPayable");
        Object dueDateObj = record.get("paymentDueDate");
        String dueDate = dueDateObj != null ? dueDateObj.toString().trim() : "";

        if (isCombined) {
            String combinedNotices = trimValue((String) record.get("combinedNotices"));
            String[] noticeNumbers = combinedNotices.split("\\.");
            int noticeCount = noticeNumbers.length;

            result.put("subject", String.format("%d Offence Notices - Action Required", noticeCount));
            result.put("body", String.format(
                "<html><body>" +
                "<h2>Offence Notices Summary</h2>" +
                "<p>Dear %s,</p>" +
                "<p>This is to inform you that %d offence notices have been issued:</p>" +
                "<ul>" +
                "<li>Notices: %s</li>" +
                "<li>Vehicle Number: %s</li>" +
                "<li>Total Amount Payable: %s</li>" +
                "<li>Payment Due Date: %s</li>" +
                "</ul>" +
                "<p>Please take necessary action within the stipulated timeframe.</p>" +
                "<p>Regards,<br>Traffic Department</p>" +
                "</body></html>",
                name, noticeCount, combinedNotices, vehicleNo, amount, dueDate
            ));
        } else {
            result.put("subject", String.format("Offence Notice %s - Action Required", noticeNo));
            result.put("body", String.format(
                "<html><body>" +
                "<h2>Offence Notice Details</h2>" +
                "<p>Dear %s,</p>" +
                "<p>This is to inform you that an offence notice has been issued:</p>" +
                "<ul>" +
                "<li>Notice Number: %s</li>" +
                "<li>Vehicle Number: %s</li>" +
                "<li>Amount Payable: %s</li>" +
                "<li>Payment Due Date: %s</li>" +
                "</ul>" +
                "<p>Please take necessary action within the stipulated timeframe.</p>" +
                "<p>Regards,<br>Traffic Department</p>" +
                "</body></html>",
                name, noticeNo, vehicleNo, amount, dueDate
            ));
        }

        return result;
    }

    private String resolveTemplateName(String stage) {
        // Map stage to template name
        switch (stage) {
            case "ENA": return "SMS_ENA";
            case "RD1": return "SMS_RD1";
            case "RD2": return "SMS_RD2";
            default: return "SMS_DEFAULT";
        }
    }

    private String replacePlaceholders(String template, Map<String, Object> data) {
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                result = result.replace("{" + key + "}", value.toString());
            }
        }
        return result;
    }
    
    /**
     * Send SMS notification using SmsUtil
     * Validates content for Postman2 and GSM-7 compliance before sending.
     *
     * @param record The record to send SMS for
     * @param content The SMS content
     * @return true if SMS was sent successfully
     */
    public boolean sendSms(Map<String, Object> record, String content) {
        try {
            log.info("Sending SMS notification for notice: {}", record.get("noticeNo"));

            // Extract phone number from record
            String phoneNumber = (String) record.get("phoneNo");

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("No phone number available for SMS notification for notice: {}", record.get("noticeNo"));
                return false;
            }

            // Validate content for Postman2 and GSM-7 compliance
            // This trims, replaces unsupported chars, and removes non-GSM-7 chars
            content = smsUtil.validateSmsBody(content);

            // Persist validated SMS content so recordSuccessful/Failed can store it in DB
            record.put("smsContent", content);

            // Use SmsUtil to send the SMS with default language (english)
            // Note: smsUtil.sendSms also calls validateSmsBody internally for defense-in-depth
            ObjectNode result = smsUtil.sendSms(phoneNumber, content, "english");
            
            // Check if SMS was sent successfully
            boolean success = "success".equals(result.get("status").asText());
            
            if (success) {
                log.info("SMS sent successfully to {} for notice: {}", phoneNumber, record.get("noticeNo"));
            } else {
                log.warn("Failed to send SMS to {} for notice: {}, reason: {}", 
                        phoneNumber, record.get("noticeNo"), 
                        result.has("message") ? result.get("message").asText() : "Unknown error");
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Error sending SMS for notice {}: {}", record.get("noticeNo"), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send email notification using EmailService
     * 
     * @param record The record to send email for
     * @param subject The email subject
     * @param content The email content
     * @return true if email was sent successfully
     */
    public boolean sendEmail(Map<String, Object> record, String subject, String content) {
        try {
            log.info("Sending email notification for notice: {}", record.get("noticeNo"));
            
            // Extract email address from record
            String email = (String) record.get("email");
            
            if (email == null || email.isEmpty()) {
                log.warn("No email address available for notice: {}", record.get("noticeNo"));
                return false;
            }
            
            // Persist email content so recordSuccessful/Failed can store it in DB
            record.put("emailContent", content);
            
            // Create email request using EmailRequest class
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(email);
            emailRequest.setSubject(subject);
            emailRequest.setHtmlContent(content);
            // From address will be set by EmailService using default value if not provided
            
            // Use EmailService to send the email
            boolean success = emailService.sendEmail(emailRequest);
            
            if (success) {
                log.info("Email sent successfully for notice: {}", record.get("noticeNo"));
            } else {
                log.warn("Failed to send email for notice: {}", record.get("noticeNo"));
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Error sending email for notice {}: {}", record.get("noticeNo"), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Save send status for SMS and email
     * FIXED: Handle combined notices by saving separate records for each individual notice
     * 
     * @param record The record to save status for
     * @param smsSent Whether SMS was sent successfully
     * @param emailSent Whether email was sent successfully
     * @return true if status was saved successfully
     */
    public boolean saveSendStatus(Map<String, Object> record, boolean smsSent, boolean emailSent) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            String combinedNotices = (String) record.get("combinedNotices");
            
            // Check if this is a combined notice
            if (combinedNotices != null && combinedNotices.contains(".")) {
                // Handle combined notices - save separate records for each notice
                String[] individualNotices = combinedNotices.split("\\.");
                log.info("Saving send status for {} combined notices: SMS={}, Email={}", 
                        individualNotices.length, smsSent, emailSent);
                
                boolean allSaved = true;
                for (String individualNotice : individualNotices) {
                    if (individualNotice != null && !individualNotice.trim().isEmpty()) {
                        boolean saved = saveSendStatusForSingleNotice(individualNotice.trim(), record, smsSent, emailSent);
                        if (!saved) {
                            allSaved = false;
                        }
                    }
                }
                return allSaved;
            } else {
                // Handle single notice
                log.info("Saving send status for single notice {}: SMS={}, Email={}", noticeNo, smsSent, emailSent);
                return saveSendStatusForSingleNotice(noticeNo, record, smsSent, emailSent);
            }
            
        } catch (Exception e) {
            log.error("Error saving send status for record {}: {}", record.get("noticeNo"), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Save send status for a single notice
     * 
     * @param noticeNo The individual notice number
     * @param record The original record containing contact info
     * @param smsSent Whether SMS was sent successfully
     * @param emailSent Whether email was sent successfully
     * @return true if status was saved successfully
     */
    private boolean saveSendStatusForSingleNotice(String noticeNo, Map<String, Object> record, boolean smsSent, boolean emailSent) {
        try {
            // Extract record ID and other needed fields from the original record
            String nricNo = (String) record.get("nricNo");
            if (nricNo == null) {
                nricNo = (String) record.get("idNo");
            }
            String phoneNo = (String) record.get("phoneNo");
            String email = (String) record.get("email");

            // Get idType to determine which notification type applies
            String idType = (String) record.get("idType");
            if (idType == null) {
                idType = (String) record.get("id_type");
            }
            boolean isCompany = "B".equals(idType);

            // Determine success based on idType:
            // - Companies (idType = "B"): Only check email status (ignore SMS)
            // - Individuals (idType != "B"): Only check SMS status (ignore email)
            boolean isSuccess;
            if (isCompany) {
                // Company: success if email was sent or no email address
                isSuccess = emailSent || email == null || email.isEmpty();
            } else {
                // Individual: success if SMS was sent or no phone number
                isSuccess = smsSent || phoneNo == null || phoneNo.isEmpty();
            }

            if (isSuccess) {
                log.info("Recording successful notification for notice {}", noticeNo);
                recordSuccessfulNotificationForSingleNotice(noticeNo, nricNo, phoneNo, email, smsSent, emailSent, record);
            } else {
                // Notification failed
                log.warn("Notification failed for notice {} (isCompany={})", noticeNo, isCompany);
                recordFailedNotificationForSingleNotice(noticeNo, nricNo, phoneNo, email, smsSent, emailSent, record);
            }

            return true;

        } catch (Exception e) {
            log.error("Error saving send status for single notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Record successful notification status
     * SIMPLIFIED: Just insert with status S (success)
     * NOTE: SMS is only for individuals (idType != "B"), Email is only for companies (idType = "B")
     */
    private void recordSuccessfulNotificationForSingleNotice(String noticeNo, String nricNo, String phoneNo, String email, boolean smsSent, boolean emailSent, Map<String, Object> record) {
        try {
            // Get idType to determine notification type
            String idType = (String) record.get("idType");
            if (idType == null) {
                idType = (String) record.get("id_type");
            }
            boolean isCompany = "B".equals(idType);

            // Record SMS success - only for individuals (not companies)
            if (!isCompany && smsSent && phoneNo != null && !phoneNo.isEmpty()) {
                OcmsSmsNotificationRecords smsRecord = new OcmsSmsNotificationRecords();
                smsRecord.setNoticeNo(noticeNo);
                smsRecord.setMobileNo(extractLocalNumber(phoneNo));
                smsRecord.setStatus((String) record.getOrDefault("smsStatus", SystemConstant.Notification.STATUS_SENT));
                smsRecord.setDateSent(LocalDateTime.now());
                smsRecord.setCreDate(LocalDateTime.now());
                smsRecord.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                smsRecord.setProcessingStage(getNextProcessingStageFromRecord(record));
                // Set batchId from record (set during batch SMS processing)
                String batchId = (String) record.get("batchId");
                if (batchId != null && !batchId.isEmpty()) {
                    smsRecord.setBatchId(batchId);
                } else {
                    smsRecord.setBatchId("single_" + System.currentTimeMillis());
                }
                // Set msgStatus from record if available
                String msgStatus = (String) record.get("smsMsgStatus");
                if (msgStatus != null) {
                    smsRecord.setMsgStatus(msgStatus);
                }
                String smsContent = (String) record.get("smsContent");
                if (smsContent == null) {
                    Map<String, String> msgs = generateMessages(record);
                    smsContent = msgs.get("smsContent");
                }
                if (smsContent == null) smsContent = "";
                smsRecord.setContent(smsContent.getBytes(StandardCharsets.UTF_8));
                smsRecord.setMobileCode("65");
                smsNotificationRecordsService.save(smsRecord);
                log.info("Recorded SMS success in ocms_sms_notification_records for notice {} with batchId {}", noticeNo, smsRecord.getBatchId());
            }

            // Record email success - only for companies
            if (isCompany && emailSent && email != null && !email.isEmpty()) {
                OcmsEmailNotificationRecords emailRecord = new OcmsEmailNotificationRecords();
                emailRecord.setNoticeNo(noticeNo);
                emailRecord.setEmailAddr(email);
                emailRecord.setStatus(SystemConstant.Notification.STATUS_SENT);  // Success
                emailRecord.setDateSent(LocalDateTime.now());
                emailRecord.setCreDate(LocalDateTime.now());
                emailRecord.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                emailRecord.setSubject("Offence Notice " + noticeNo);
                String emailContent = (String) record.get("emailContent");
                if (emailContent == null) {
                    Map<String, String> msgs = generateMessages(record);
                    emailContent = msgs.get("emailContent");
                }
                if (emailContent == null) emailContent = "";
                emailRecord.setContent(emailContent.getBytes(StandardCharsets.UTF_8));
                emailRecord.setProcessingStage(getNextProcessingStageFromRecord(record));                      // Required field
                emailNotificationRecordsService.save(emailRecord);
                log.info("Recorded email success in ocms_email_notification_records for notice {}", noticeNo);
            }
        } catch (Exception e) {
            log.error("Error recording successful notification for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Record failed notification status
     * SIMPLIFIED: Just insert with status E (error)
     * NOTE: SMS is only for individuals (idType != "B"), Email is only for companies (idType = "B")
     */
    private void recordFailedNotificationForSingleNotice(String noticeNo, String nricNo, String phoneNo, String email,
            boolean smsSent, boolean emailSent, Map<String, Object> record) {
        try {
            // Get idType to determine notification type
            String idType = (String) record.get("idType");
            if (idType == null) {
                idType = (String) record.get("id_type");
            }
            boolean isCompany = "B".equals(idType);

            // Record SMS failure - only for individuals (not companies)
            if (!isCompany && !smsSent && phoneNo != null && !phoneNo.isEmpty()) {
                OcmsSmsNotificationRecords smsErrorRecord = new OcmsSmsNotificationRecords();
                smsErrorRecord.setNoticeNo(noticeNo);
                smsErrorRecord.setMobileNo(extractLocalNumber(phoneNo));
                smsErrorRecord.setStatus((String) record.getOrDefault("smsStatus", SystemConstant.Notification.STATUS_FAILED));
                smsErrorRecord.setDateSent(LocalDateTime.now());
                smsErrorRecord.setCreDate(LocalDateTime.now());
                smsErrorRecord.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                // Set msgStatus from record if available, otherwise use default message
                String msgStatus = (String) record.get("smsMsgStatus");
                smsErrorRecord.setMsgStatus(msgStatus != null ? msgStatus : "Failed to send SMS notification");
                smsErrorRecord.setProcessingStage(getNextProcessingStageFromRecord(record));
                // Set batchId from record (set during batch SMS processing)
                String batchId = (String) record.get("batchId");
                if (batchId != null && !batchId.isEmpty()) {
                    smsErrorRecord.setBatchId(batchId);
                } else {
                    smsErrorRecord.setBatchId("single_" + System.currentTimeMillis());
                }
                String smsContent = (String) record.get("smsContent");
                if (smsContent == null) {
                    Map<String, String> msgs = generateMessages(record);
                    smsContent = msgs.get("smsContent");
                }
                if (smsContent == null) smsContent = "";
                smsErrorRecord.setContent(smsContent.getBytes(StandardCharsets.UTF_8));
                smsErrorRecord.setMobileCode("65");
                smsNotificationRecordsService.save(smsErrorRecord);
                log.info("Recorded SMS error in ocms_sms_notification_records for notice {} with batchId {}", noticeNo, smsErrorRecord.getBatchId());
            }

            // Record email failure - only for companies
            if (isCompany && !emailSent && email != null && !email.isEmpty()) {
                OcmsEmailNotificationRecords emailErrorRecord = new OcmsEmailNotificationRecords();
                emailErrorRecord.setNoticeNo(noticeNo);
                emailErrorRecord.setEmailAddr(email);
                emailErrorRecord.setStatus(SystemConstant.Notification.STATUS_FAILED);  // Error
                emailErrorRecord.setDateSent(LocalDateTime.now());
                emailErrorRecord.setCreDate(LocalDateTime.now());
                emailErrorRecord.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                emailErrorRecord.setSubject("Offence Notice " + noticeNo + " - Failed");
                emailErrorRecord.setMsgError("Failed to send email notification");
                String emailContent = (String) record.get("emailContent");
                if (emailContent == null) {
                    Map<String, String> msgs = generateMessages(record);
                    emailContent = msgs.get("emailContent");
                }
                if (emailContent == null) emailContent = "";
                emailErrorRecord.setContent(emailContent.getBytes(StandardCharsets.UTF_8));
                emailErrorRecord.setProcessingStage(getNextProcessingStageFromRecord(record));                          // Required field
                emailNotificationRecordsService.save(emailErrorRecord);
                log.info("Recorded email error in ocms_email_notification_records for notice {}", noticeNo);
            }
        } catch (Exception e) {
            log.error("Error recording failed notification for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Check if SMS retry is needed for a record
     * 
     * @param record The record to check
     * @return true if SMS retry is needed
     */
    public boolean isSmsRetryNeeded(Map<String, Object> record) {
        // Check if SMS was not sent and retry count is less than max
        Boolean smsSent = (Boolean) record.get("smsSent");
        Integer smsRetryCount = (Integer) record.get("smsRetryCount");
        
        return (smsSent == null || !smsSent) && 
               (smsRetryCount == null || smsRetryCount < maxSmsRetries);
    }
    
    /**
     * Check if email retry is needed for a record
     * 
     * @param record The record to check
     * @return true if email retry is needed
     */
    public boolean isEmailRetryNeeded(Map<String, Object> record) {
        // Check if email was not sent and retry count is less than max
        Boolean emailSent = (Boolean) record.get("emailSent");
        Integer emailRetryCount = (Integer) record.get("emailRetryCount");
        
        return (emailSent == null || !emailSent) && 
               (emailRetryCount == null || emailRetryCount < maxEmailRetries);
    }
    
    /**
     * Move a record to RD1 stage after retry failure
     * 
     * @param record The record to move
     * @return true if record was moved successfully
     */
    public boolean moveToRd1Stage(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            log.info("Moving record to RD1 stage after retry failure for notice: {}", noticeNo);
            
            // Extract record ID and other needed fields
            String nricNo = (String) record.get("nricNo");
            String phoneNo = (String) record.get("phoneNo");
            String email = (String) record.get("email");
            
            // Prepare update data for next processing stage
            Map<String, Object> updateData = new HashMap<>();
            
            // Update processing stages as per flowchart
            updateData.put("prevProcessingStage", SystemConstant.SuspensionReason.ROV); // As shown in flowchart
            updateData.put("prevProcessingDate", record.get("lastProcessingDate")); // old value last_process
            updateData.put("lastProcessingStage", enaStage); // ENA
            updateData.put("lastProcessingDate", LocalDateTime.now()); // date enter ENA
            updateData.put("nextProcessingStage", rd1Stage); // RD1
            updateData.put("nextProcessingDate", LocalDateTime.now().plusDays(1)); // next day

            // PROCESS 5: Set is_sync to N to trigger cron batch sync to internet DB
            updateData.put("isSync", "N");

            // Update the database
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);

            tableQueryService.patch("ocms_valid_offence_notice", filter, updateData);
            log.info("Updated processing stage to RD1 for notice {} (isSync=N)", noticeNo);
            
            // Record final failure status
            recordFailedNotificationForSingleNotice(noticeNo, nricNo, phoneNo, email, false, false, record);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error moving record to RD1 stage for notice {}: {}", record.get("noticeNo"), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if notice number is in HST list and apply TS-HST if found
     * CORRECTED: Check by notice_no in ocms_hst table, not by id_no
     * 
     * @param noticeNo The notice number to check
     * @return true if HST was applied
     */
    public boolean checkAndApplyHst(String noticeNo) {
        try {
            log.info("Checking if notice {} is in HST list", noticeNo);
            
            // Query ocms_hst table to check if notice number exists
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            
            List<Map<String, Object>> hstRecords = tableQueryService.query("ocms_hst", filter);
            
            if (hstRecords == null || hstRecords.isEmpty()) {
                log.info("Notice number {} not found in HST list", noticeNo);
                return false;
            }
            
            log.info("Notice number {} found in HST list. Applying TS-HST.", noticeNo);
            
            // Apply TS-HST by updating both tables
            return applyTsHstSuspension(noticeNo);
            
        } catch (Exception e) {
            log.error("Error checking HST list for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Apply TS-HST suspension by updating VON table and creating suspended notice record
     * 
     * @param noticeNo The notice number to apply suspension to
     * @return true if suspension was applied successfully
     */
    private boolean applyTsHstSuspension(String noticeNo) {
        try {
            LocalDateTime suspensionDate = LocalDateTime.now();
            
            // 1. Update ocms_valid_offence_notice table
            Map<String, Object> vonFilter = new HashMap<>();
            vonFilter.put("noticeNo", noticeNo);
            
            Map<String, Object> vonUpdateData = new HashMap<>();
            vonUpdateData.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            vonUpdateData.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.HST);
            vonUpdateData.put("eprDateOfSuspension", suspensionDate);

            // PROCESS 5: Set is_sync to N to trigger cron batch sync to internet DB
            vonUpdateData.put("isSync", "N");

            tableQueryService.patch("ocms_valid_offence_notice", vonFilter, vonUpdateData);
            log.info("Updated VON table with TS-HST suspension for notice {} (isSync=N)", noticeNo);

            // 2. Apply suspension via API (replaces direct suspended_notice table creation)
            log.info("[TS-HST] Calling suspension API for notice: {}", noticeNo);
            Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                noticeNo,
                SystemConstant.SuspensionType.TEMPORARY, // TS
                SystemConstant.SuspensionReason.HST,
                "Auto-applied TS-HST due to HST record",
                SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                SystemConstant.Subsystem.OCMS_CODE,
                null, // caseNo
                null  // daysToRevive - NULL for TS-HST (no revival date)
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("[TS-HST] Successfully applied TS-HST suspension via API for notice {}", noticeNo);
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                log.error("[TS-HST] API returned error for notice {}: {}", noticeNo, errorMsg);
                return false;
            }

            return true;
            
        } catch (Exception e) {
            log.error("Error applying TS-HST suspension for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    public List<Map<String, Object>> queryRecordsForEna() {
        // First, query the parameter value for ENA from ocms_parameter
        Integer retryDays = getParameterValueForEna("ENA");
        if (retryDays == null) {
            log.warn("Could not find parameter value for ENA in ocms_parameter, using default -2");
            retryDays = 2;
        }

        String sql =
            "SELECT " +
            "von.notice_no AS noticeNo, " +
            "von.vehicle_no AS vehicleNo, " +
            "von.payment_due_date AS paymentDueDate, " +
            "von.amount_payable AS amountPayable, " +
            "von.last_processing_stage AS lastProcessingStage, " +
            "von.next_processing_stage AS nextProcessingStage, " +
            "onod.name AS name, " +
            "onod.offender_tel_no AS phoneNo, " +
            "onod.email_addr AS email, " +
            "onod.id_no AS idNo, " +
            "onod.id_type AS idType, " +
            "von.vehicle_registration_type AS vehicleRegistrationType, " +
            "von.an_flag AS anFlag, " +
            "von.suspension_type AS suspensionType, " +
            "von.epr_reason_of_suspension AS eprReasonOfSuspension " +
            "FROM ocmsizmgr.ocms_valid_offence_notice AS von " +
            "JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod " +
            "ON onod.notice_no = von.notice_no " +
            "AND onod.offender_indicator = 'Y' " +
            "WHERE " +
            "von.last_processing_stage = 'ENA' " +
            "AND von.last_processing_date <= CAST(SYSDATETIME() AS datetime2(7)) " +
            "AND ( " +
            "von.suspension_type IS NULL " +
            "OR (von.suspension_type = 'TS' AND von.epr_reason_of_suspension = 'HST') " +
            // TODO: Uncomment below when ocms_suspended_notice table is available
            // "OR EXISTS ( " +
            // "SELECT 1 " +
            // "FROM ocmsizmgr.ocms_suspended_notice sn " +
            // "WHERE sn.notice_no = von.notice_no " +
            // "AND sn.suspension_type = 'TS' " +
            // "AND sn.reason_of_suspension = 'HST' " +
            // "AND sn.date_of_revival IS NULL " +
            // ") " +
            ") " +
            "AND NOT EXISTS ( " +
            "SELECT 1 " +
            "FROM ocmsizmgr.ocms_sms_notification_records snr " +
            "WHERE snr.notice_no = von.notice_no " +
            "AND snr.processing_stage = von.next_processing_stage " +
            "AND snr.cre_date >= DATEADD(DAY, -" + retryDays + ", CAST(SYSDATETIME() AS datetime2(7))) " +
            ") " +
            "AND NOT EXISTS ( " +
            "SELECT 1 " +
            "FROM ocmsizmgr.ocms_email_notification_records enr " +
            "WHERE enr.notice_no = von.notice_no " +
            "AND enr.processing_stage = von.next_processing_stage " +
            "AND enr.cre_date >= DATEADD(DAY, -" + retryDays + ", CAST(SYSDATETIME() AS datetime2(7))) " +
            ") " +
            "AND (von.vehicle_registration_type IS NULL OR von.vehicle_registration_type NOT IN ('I', 'D', 'V')) " +
            "AND NOT (von.suspension_type = 'PS' AND von.epr_reason_of_suspension = 'ANS')";
            // TODO: Uncomment below when ocms_suspended_notice table is available
            // " + "AND NOT EXISTS ( " +
            // "SELECT 1 " +
            // "FROM ocmsizmgr.ocms_suspended_notice sn " +
            // "WHERE sn.notice_no = von.notice_no " +
            // "AND sn.date_of_revival IS NOT NULL " +
            // "AND sn.date_of_revival >= von.last_processing_date " +
            // "AND sn.date_of_revival <= von.next_processing_date " +
            // ")";
        
        return jdbcTemplate.queryForList(sql);
    }
    
    public List<Map<String, Object>> queryRecordsForReminder() {
        // First, query the parameter value for ENA from ocms_parameter
        Integer retryDays = getParameterValueForEna(SystemConstant.SuspensionReason.RD1);
        if (retryDays == null) {
            log.warn("Could not find parameter value for RD1 in ocms_parameter, using default -2");
            retryDays = 2;
        }

        String sql =
            "SELECT " +
            "von.notice_no AS noticeNo, " +
            "von.vehicle_no AS vehicleNo, " +
            "von.payment_due_date AS paymentDueDate, " +
            "von.amount_payable AS amountPayable, " +
            "von.last_processing_stage AS lastProcessingStage, " +
            "von.next_processing_stage AS nextProcessingStage, " +
            "onod.name AS name, " +
            "onod.offender_tel_no AS phoneNo, " +
            "onod.email_addr AS email, " +
            "onod.id_no AS idNo, " +
            "onod.id_type AS idType, " +
            "von.vehicle_registration_type AS vehicleRegistrationType " +
            "FROM ocmsizmgr.ocms_valid_offence_notice AS von " +
            "JOIN ocmsizmgr.ocms_parameter AS notif_day_param " +
            "ON notif_day_param.code = von.last_processing_stage " +
            "AND notif_day_param.parameter_id = 'ENOTEDAYS' " +
            "JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod " +
            "ON onod.notice_no = von.notice_no " +
            "AND onod.offender_indicator = 'Y' " +
            "WHERE " +
            "von.last_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN1', 'DN2', 'DR3') " +
            "AND CAST(von.payment_due_date AS DATE) " +
            "<= CAST(DATEADD(DAY, CAST(notif_day_param.value AS INT), GETDATE()) AS DATE) " +
            "AND ( " +
            "von.suspension_type IS NULL " +
            "OR (von.suspension_type = 'TS' AND von.epr_reason_of_suspension = 'HST') " +
            // TODO: Uncomment below when ocms_suspended_notice table is available
            // "OR EXISTS ( " +
            // "SELECT 1 " +
            // "FROM ocmsizmgr.ocms_suspended_notice osn " +
            // "WHERE osn.notice_no = von.notice_no " +
            // "AND osn.suspension_type = 'TS' " +
            // "AND osn.reason_of_suspension = 'HST' " +
            // "AND osn.date_of_revival IS NULL " +
            // ") " +
            ") " +
            "AND onod.id_no NOT IN ( " +
            "SELECT id_no " +
            "FROM ocmsizmgr.ocms_enotification_exclusion_list " +
            ") " +
            "AND NOT EXISTS ( " +
            "SELECT 1 " +
            "FROM ocmsizmgr.ocms_sms_notification_records snr " +
            "WHERE snr.notice_no = von.notice_no " +
            "AND snr.processing_stage = von.last_processing_stage " +
            "AND snr.cre_date >= DATEADD(DAY, -" + retryDays + ", GETDATE()) " +
            ") " +
            "AND NOT EXISTS ( " +
            "SELECT 1 " +
            "FROM ocmsizmgr.ocms_email_notification_records enr " +
            "WHERE enr.notice_no = von.notice_no " +
            "AND enr.processing_stage = von.last_processing_stage " +
            "AND enr.cre_date >= DATEADD(DAY, -" + retryDays + ", GETDATE()) " +
            ") " +
            "AND (von.vehicle_registration_type IS NULL OR von.vehicle_registration_type NOT IN ('I', 'D', 'V'))";
            // TODO: Uncomment below when ocms_suspended_notice table is available
            // " + "AND NOT EXISTS ( " +
            // "SELECT 1 " +
            // "FROM ocmsizmgr.ocms_suspended_notice osn2 " +
            // "WHERE osn2.notice_no = von.notice_no " +
            // "AND osn2.date_of_revival IS NOT NULL " +
            // "AND osn2.date_of_revival >= von.last_processing_date " +
            // "AND osn2.date_of_revival <= von.next_processing_date " +
            // ")";
        
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get parameter value for ENA from ocms_parameter table
     * 
     * @return The parameter value as Integer, or null if not found
     */
    private Integer getParameterValueForEna(String code) {
        try {
            String paramSql = "SELECT CAST(value AS INT) AS paramValue " +
                             "FROM ocmsizmgr.ocms_parameter " +
                             "WHERE parameter_id = 'ENOTEDAYS' " +
                             "AND code = '" + code + "'";
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(paramSql);
            if (result != null && !result.isEmpty()) {
                return (Integer) result.get(0).get("paramValue");
            }
            return null;
        } catch (Exception e) {
            log.error("Error querying parameter value for ENA: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get parameter value for ENA from ocms_parameter table
     * 
     * @return The parameter value as Integer, or null if not found
     */
    private Integer getParameterValueForRD2() {
        try {
            String paramSql = "SELECT CAST(value AS INT) AS paramValue " +
                             "FROM ocmsizmgr.ocms_parameter " +
                             "WHERE parameter_id = 'STAGEDAYS' " +
                             "AND code = 'RD2'";
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(paramSql);
            if (result != null && !result.isEmpty()) {
                return (Integer) result.get(0).get("paramValue");
            }
            return null;
        } catch (Exception e) {
            log.error("Error querying parameter value for ENA: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public boolean isExcluded(String idNo) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ocms_enotification_exclusion_list WHERE id_no = ?",
                Integer.class, idNo
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Error checking exclusion list for {}: {}", idNo, e.getMessage());
            return false;
        }
    }

    /**
     * Update last processing fields for Check Notice flow:
     * - lastProcessingStage = RD1
     * - lastProcessingDate = next day
     * Does not alter nextProcessing fields.
     *
     * @param record The record context, expected to include noticeNo or notice_no
     */
    public void patchNextStageToRd1NextDay(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            if (noticeNo == null) {
                noticeNo = (String) record.get("notice_no");
            }
            if (noticeNo == null) {
                log.warn("Cannot patch next stage to RD1: noticeNo missing in record");
                return;
            }

            log.info("Setting lastProcessingStage=RD1 and lastProcessingDate=next day for notice: {}", noticeNo);

            Integer rd2Days = getParameterValueForRD2();
            if (rd2Days == null) {
                log.warn("Could not find parameter value for RD2 in ocms_parameter");
            }

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("lastProcessingStage", SystemConstant.SuspensionReason.RD1);
            updateData.put("lastProcessingDate", LocalDateTime.now().plusDays(1));
            updateData.put("nextProcessingStage", SystemConstant.SuspensionReason.RD2);
            updateData.put("nextProcessingDate", LocalDateTime.now().plusDays(rd2Days));

            // PROCESS 5: Set is_sync to N to trigger cron batch sync to internet DB
            updateData.put("isSync", "N");

            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);

            tableQueryService.patch("ocms_valid_offence_notice", filter, updateData);

            // Reflect in local record map as well
            record.put("lastProcessingStage", SystemConstant.SuspensionReason.RD1);
            record.put("lastProcessingDate", LocalDateTime.now().plusDays(1));
        } catch (Exception e) {
            log.error("Error patching next stage to RD1 for notice {}: {}", record.get("noticeNo"), e.getMessage(), e);
        }
    }

    /**
     * Update stage to RD1 for records that were originally in ENA stage
     * 
     * @param record The record to update
     */
    public void updateStageToRd1(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            log.info("Updating stage to RD1 for notice: {}", noticeNo);
            
            // Prepare update data for next processing stage
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("prevProcessingStage", record.get("nextProcessingStage"));
            updateData.put("prevProcessingDate", record.get("nextProcessingDate"));
            updateData.put("lastProcessingStage", SystemConstant.SuspensionReason.RD1);
            updateData.put("lastProcessingDate", LocalDateTime.now());
            updateData.put("nextProcessingStage", SystemConstant.SuspensionReason.RD2); // Clear next stage
            updateData.put("nextProcessingDate", null);

            // PROCESS 5: Set is_sync to N to trigger cron batch sync to internet DB
            updateData.put("isSync", "N");

            // Update the database
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);

            tableQueryService.patch("ocms_valid_offence_notice", filter, updateData);
            log.info("Updated stage to RD1 for notice {} (isSync=N)", noticeNo);
            
        } catch (Exception e) {
            log.error("Error updating stage to RD1 for notice {}: {}", record.get("noticeNo"), e.getMessage(), e);
        }
    }

    /**
     * Send error notification email to MGG, ISG, and OCMS
     * 
     * @param errors List of error details (noticeNo and error message)
     */
    public void sendErrorNotification(List<Map<String, String>> errors) {
        try {
            if (errors.isEmpty()) {
                return;
            }
            
            log.warn("Sending error notification for {} failed records", errors.size());
            
            // Build error details
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append("<h2>Notification Preparation Errors</h2>");
            errorDetails.append("<p>The following errors occurred during notification preparation:</p>");
            errorDetails.append("<ul>");
            
            for (Map<String, String> error : errors) {
                errorDetails.append(String.format(
                    "<li>Notice: %s - Error: %s</li>", 
                    error.get("noticeNo"), 
                    error.get("error")
                ));
            }
            
            errorDetails.append("</ul>");
            
            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setSubject("OCMS Notification Preparation Errors");
            emailRequest.setHtmlContent(errorDetails.toString());
            
            // Send email
            boolean sent = emailService.sendEmail(emailRequest);
            
            if (sent) {
                log.info("Error notification email sent successfully");
            } else {
                log.error("Failed to send error notification email");
            }
            
        } catch (Exception e) {
            log.error("Error sending error notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Trigger PS-ANS suspension for Advisory Notice (OCMS 10)
     * Calls the internal Suspension API to apply PS-ANS suspension
     *
     * @param noticeNo Notice number to suspend
     * @param reason Suspension reason/remarks
     * @return true if suspension was successfully applied, false otherwise
     */
    public boolean triggerPsAnsSuspension(String noticeNo, String reason) {
        try {
            log.info("Triggering PS-ANS suspension for notice: {}", noticeNo);

            // Call suspension API to apply PS-ANS
            Map<String, Object> response = suspensionApiClient.applySuspensionSingle(
                noticeNo,
                "PS",                           // Suspension Type: Permanent
                "ANS",                          // Reason: Advisory Notice System
                reason,                         // Remarks
                "SYSTEM",                       // Officer: System-generated
                "004",                          // Source: 004=OCMS
                null,                           // Case No: N/A for ANS
                null                            // Days to Revive: N/A for PS
            );

            // Check if successful
            if (suspensionApiClient.isSuccess(response)) {
                log.info("Successfully triggered PS-ANS suspension for notice: {}", noticeNo);
                return true;
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(response);
                log.error("Failed to trigger PS-ANS suspension for notice {}: {}", noticeNo, errorMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("Error triggering PS-ANS suspension for notice {}: {}", noticeNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Call DataHive API to get company information for email recipients
     *
     * @param uen Unique Entity Number
     * @return Company information map or null on failure
     */
    public Map<String, Object> getCompanyInfoFromDataHive(String uen) {
        try {
            log.info("Fetching company info from DataHive for UEN: {}", uen);
            
            // Build SQL query
            String sql = "SELECT CP_ACCOUNT_EMAIL " +
                         "FROM V_DH_GOVTECH_CORPPASS_DELTA " +
                         "WHERE ENTITY_ID = '" + uen + "'";
            
            // Execute query
            JsonNode result = dataHiveService.executeQueryAsyncCustom(
                sql, 
                datahiveDatabase, 
                datahiveSchema, 
                datahiveWarehouse, 
                datahiveRole
            );
            
            // Process result and convert to Map
            return convertJsonNodeToMap(result);
        } catch (Exception e) {
            log.error("Error fetching company info from DataHive: {}", e.getMessage(), e);
            return null;
        }
    }
    
    
    private void addIfValid(List<String> emails, String email) {
        if (email != null && !email.isEmpty() && email.contains("@")) {
            emails.add(email);
        }
    }

    /**
     * Call DataHive API to get contact information for SMS recipients
     * 
     * @param idNo National ID number
     * @return Contact information map or null on failure
     */
    public Map<String, Object> getContactInfoFromDataHive(String idNo) {
        try {
            log.info("Fetching contact info from DataHive for ID: {}", idNo);
            
            // Build SQL query
            String sql = "SELECT CONTACT " +
                         "FROM V_DH_SNDGO_SINGPASSCONTACT_MASTER " +
                         "WHERE UIN = '" + idNo + "'";
            
            // Execute query
            JsonNode result = dataHiveService.executeQueryAsyncCustom(
                sql, 
                datahiveDatabase, 
                datahiveSchema, 
                datahiveWarehouse, 
                datahiveRole
            );
            
            // Process result and convert to Map
            return convertJsonNodeToMap(result);
        } catch (Exception e) {
            log.error("Error fetching contact info from DataHive: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Call DataHive Singpass dataset to get contact information for SMS recipients
     * Required source: V_DH_SNDGO_SINGPASSCONTACT_MASTER
     * 
     * @param idNo National ID number
     * @return Contact information map or null on failure
     */
    public Map<String, Object> getContactInfoFromSingpass(String idNo) {
        try {
            log.info("Fetching Singpass contact info from DataHive for ID: {}", idNo);
            
            String sql = "SELECT CONTACT " +
                         "FROM V_DH_SNDGO_SINGPASSCONTACT_MASTER " +
                         "WHERE UIN = '" + idNo + "'";
            
            JsonNode result = dataHiveService.executeQueryAsyncCustom(
                sql,
                datahiveDatabase,
                datahiveSchema,
                datahiveWarehouse,
                datahiveRole
            );
            
            // Handle multiple possible shapes of DataHive result
            if (result == null) {
                log.warn("DataHive returned null for Singpass contact");
                return null;
            }
            if (result.isArray() && result.size() > 0) {
                JsonNode first = result.get(0);
                // Case 1: Object with field CONTACT
                if (first.isObject()) {
                    return convertJsonNodeToMap(result);
                }
                // Case 2: Scalar value (e.g., ["84517544"])
                if (first.isValueNode()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("CONTACT", first.asText());
                    log.info("Parsed CONTACT from scalar DataHive row: {}", first.asText());
                    return m;
                }
                // Case 3: Row as array (e.g., [["84517544"]])
                if (first.isArray() && first.size() > 0) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("CONTACT", first.get(0).asText());
                    log.info("Parsed CONTACT from array row: {}", first.get(0).asText());
                    return m;
                }
            }
            
            // Case 4: Direct object (not in array)
            if (result.isObject()) {
                Map<String, Object> companyMap = new HashMap<>();
                result.fields().forEachRemaining(entry -> {
                    companyMap.put(entry.getKey(), entry.getValue().asText());
                });
                log.info("Parsed company data from direct object: {}", companyMap);
                return companyMap;
            }
            
            log.warn("Unknown DataHive result format for company data: {}", result.getNodeType());
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching Singpass contact info from DataHive: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        Map<String, Object> resultMap = new HashMap<>();
        if (jsonNode != null && jsonNode.isArray() && jsonNode.size() > 0) {
            JsonNode firstRow = jsonNode.get(0);
            firstRow.fields().forEachRemaining(entry -> {
                resultMap.put(entry.getKey(), entry.getValue().asText());
            });
        }
        log.info("Converted JSON node to map: {}", resultMap);
        return resultMap;
    }

    /**
     * Send batch SMS notifications with polling and retry support.
     * Flow:
     * 1. Split records into batches of 1000
     * 2. For each batch: Send, poll until terminal status, retry if whole batch failed
     * 3. Map status from Postman2 response and save to DB
     *
     * @param records List of records to send
     * @return true if all batches were sent successfully
     */
    public boolean sendBatchSms(List<Map<String, Object>> records) {
        try {
            log.info("Preparing batch SMS for {} recipients", records.size());

            // Prepare batch records with phone-to-record mapping
            List<Map<String, Object>> validRecords = new ArrayList<>();
            List<Map<String, String>> batchRecords = new ArrayList<>();

            for (Map<String, Object> record : records) {
                String noticeNo = (String) record.get("noticeNo");

                // DEFENSIVE CHECK: Skip companies - they should only receive email, not SMS
                String idType = (String) record.get("idType");
                if (idType == null) {
                    idType = (String) record.get("id_type");
                }
                if ("B".equals(idType)) {
                    log.warn("Skipping company record {} in sendBatchSms - companies should only receive email", noticeNo);
                    continue;
                }

                String phoneNo = (String) record.get("phoneNo");

                if (phoneNo == null || phoneNo.isEmpty()) {
                    log.warn("Skipping record with missing phone number: {}", noticeNo);
                    continue;
                }

                // Normalize mobile number
                String normalizedPhone = normalizeMobile(phoneNo);

                // Generate SMS content
                Map<String, String> messages = generateMessages(record);
                String content = messages.get("smsContent");

                // Validate content for Postman2 and GSM-7 compliance
                content = smsUtil.validateSmsBody(content);

                // Store validated content and normalized phone back to record
                record.put("smsContent", content);
                record.put("normalizedPhone", normalizedPhone);

                // Create batch record for sendBatchSmsSimple
                Map<String, String> batchRecord = new HashMap<>();
                batchRecord.put("phoneNumber", normalizedPhone);
                batchRecord.put("language", "english");
                batchRecord.put("message", content);

                batchRecords.add(batchRecord);
                validRecords.add(record);
            }

            if (batchRecords.isEmpty()) {
                log.info("No valid records for batch SMS");
                return true;
            }

            // Split into batches of batchSizeLimit (1000)
            List<List<Map<String, String>>> batches = splitIntoBatches(batchRecords, batchSizeLimit);
            List<List<Map<String, Object>>> recordBatches = splitIntoBatches(validRecords, batchSizeLimit);

            log.info("Split {} records into {} batches of max {} each",
                    batchRecords.size(), batches.size(), batchSizeLimit);

            // Track overall results
            int totalSuccess = 0;
            int totalFailure = 0;
            List<String> batchIds = new ArrayList<>();

            // Process each batch
            for (int i = 0; i < batches.size(); i++) {
                List<Map<String, String>> batch = batches.get(i);
                List<Map<String, Object>> recordBatch = recordBatches.get(i);

                log.info("Processing batch {}/{} with {} records", i + 1, batches.size(), batch.size());

                // Send batch SMS
                ObjectNode result = smsUtil.sendBatchSmsSimple(batch);

                // Capture CSV content sent to Postman for error reporting
                String csvContent = result.path("csvContent").asText(null);

                if (!"success".equals(result.path("status").asText())) {
                    String errorMessage = result.path("message").asText();
                    log.error("Batch {}/{} send failed: {}", i + 1, batches.size(), errorMessage);

                    // Generate a consistent batchId for all failed records in this batch
                    String failedBatchId = "failed_" + System.currentTimeMillis();

                    // Mark all records in this batch as failed with same batchId
                    for (Map<String, Object> record : recordBatch) {
                        record.put("smsStatus", "E");
                        record.put("smsMsgStatus", "batch_send_failed: " + errorMessage);
                        record.put("batchId", failedBatchId);
                    }

                    // Send error email with actual CSV that was sent to Postman
                    sendBatchFailureEmail(recordBatch, failedBatchId, errorMessage, csvContent);

                    totalFailure += recordBatch.size();
                    continue;
                }

                // Get batchId from response
                String batchId = result.path("batchId").asText();
                if (batchId == null || batchId.isEmpty()) {
                    batchId = result.path("response").path("batchId").asText();
                }
                batchIds.add(batchId);
                log.info("Batch {}/{} sent successfully. BatchId: {}", i + 1, batches.size(), batchId);

                // Poll until terminal status
                BatchPollResult pollResult = pollBatchUntilTerminal(batchId);

                // Check if whole batch failed and retry once
                if (pollResult.isWholeBatchFailed() && !pollResult.isAlreadyRetried()) {
                    log.info("Whole batch {} failed, retrying once...", batchId);
                    ObjectNode retryResult = smsUtil.retryBatch(batchId);
                    if ("success".equals(retryResult.path("status").asText())) {
                        // Poll again after retry
                        pollResult = pollBatchUntilTerminal(batchId);
                        pollResult.setAlreadyRetried(true);
                    }
                }

                // Map status from poll result to records
                mapBatchStatusToRecords(recordBatch, pollResult);

                totalSuccess += pollResult.getSuccessCount();
                totalFailure += pollResult.getFailureCount();
            }

            // Log summary
            log.info("Batch SMS complete. Total: {}, Success: {}, Failure: {}",
                    validRecords.size(), totalSuccess, totalFailure);

            // Send summary email
            sendSmsBatchSummaryEmail(validRecords.size(), totalSuccess, totalFailure, batchIds);

            return totalFailure == 0;

        } catch (Exception e) {
            log.error("Error sending batch SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Split a list into batches of specified size
     */
    @SuppressWarnings("unchecked")
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return batches;
    }

    /**
     * Poll batch status until all messages reach terminal state (success or failure)
     *
     * @param batchId The batch ID to poll
     * @return BatchPollResult with status counts and message details
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
     * Map batch status from poll result to records for DB persistence
     */
    private void mapBatchStatusToRecords(List<Map<String, Object>> records, BatchPollResult pollResult) {
        Map<String, Map<String, Object>> phoneToStatus = new HashMap<>();

        // Build phone -> status map from poll result
        if (pollResult.getMessageStatuses() != null) {
            for (Map<String, Object> msgStatus : pollResult.getMessageStatuses()) {
                String recipient = (String) msgStatus.get("recipient");
                phoneToStatus.put(recipient, msgStatus);
            }
        }

        // Map status to records
        for (Map<String, Object> record : records) {
            String normalizedPhone = (String) record.get("normalizedPhone");
            Map<String, Object> msgStatus = phoneToStatus.get(normalizedPhone);

            if (msgStatus != null) {
                String latestStatus = (String) msgStatus.get("latestStatus");
                String status;
                String detailedStatus;

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
                        // Build detailed status from messageAttempts
                        detailedStatus = buildDetailedStatus(latestStatus, msgStatus.get("messageAttempts"));
                        break;
                }

                record.put("smsStatus", status);
                record.put("smsMsgStatus", detailedStatus);
                record.put("batchId", pollResult.getBatchId());
            } else {
                // No status found - mark as error
                record.put("smsStatus", "E");
                record.put("smsMsgStatus", "status_not_found");
            }
        }
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
     * Send summary email after batch SMS processing
     */
    private void sendSmsBatchSummaryEmail(int totalRecords, int successCount, int failureCount, List<String> batchIds) {
        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String htmlContent = new StringBuilder()
                    .append("<html><body>")
                    .append("<h2 style='color: #1976d2;'>SMS Batch Processing Summary</h2>")
                    .append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Timestamp</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(timestamp).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Total Records</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(totalRecords).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Successful</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(successCount).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Failed</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(failureCount).append("</td></tr>")
                    .append("<tr><td style='border: 1px solid #ddd; padding: 8px; background-color: #f5f5f5;'><strong>Batch IDs</strong></td>")
                    .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(String.join(", ", batchIds)).append("</td></tr>")
                    .append("</table>")
                    .append("<hr style='margin-top: 20px;'>")
                    .append("<p style='color: #666; font-size: 12px;'>This is an automated message from OCMS Intranet Cron Service.</p>")
                    .append("</body></html>")
                    .toString();

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(emailRecipients);
            emailRequest.setSubject("[" + activeProfile.toUpperCase() + "] OCMS SMS Batch Processing Summary - " + java.time.LocalDate.now());
            emailRequest.setHtmlContent(htmlContent);

            emailService.sendEmail(emailRequest);
            log.info("Sent SMS batch summary email to {}", emailRecipients);
        } catch (Exception e) {
            log.error("Error sending SMS batch summary email: {}", e.getMessage(), e);
        }
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
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            StringBuilder htmlBuilder = new StringBuilder()
                    .append("<html><body>")
                    .append("<h2 style='color: #d32f2f;'>SMS Batch Send Failed</h2>")
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
            emailRequest.setSubject("[" + activeProfile.toUpperCase() + "] OCMS SMS Batch Send Failed - " + java.time.LocalDate.now());
            emailRequest.setHtmlContent(htmlContent);

            // Add the actual Postman CSV as attachment
            if (postmanCsvContent != null && !postmanCsvContent.isEmpty()) {
                EmailRequest.Attachment attachment = new EmailRequest.Attachment();
                attachment.setFileName("postman_batch_sms_" + java.time.LocalDate.now() + ".csv");
                attachment.setFileContent(postmanCsvContent.getBytes(StandardCharsets.UTF_8));
                emailRequest.setAttachments(java.util.Collections.singletonList(attachment));
            } else {
                log.warn("No CSV content available to attach to failure email");
            }

            emailService.sendEmail(emailRequest);
            log.info("Sent batch failure email with Postman CSV attachment to {}", emailRecipients);
        } catch (Exception e) {
            log.error("Error sending batch failure email: {}", e.getMessage(), e);
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
    
    /**
     * Normalize mobile number to E.164 format
     * 
     * @param phoneNo Raw phone number
     * @return Normalized number
     */
    public String normalizeMobile(String phoneNo) {
        // Remove all non-digit characters
        String normalized = phoneNo.replaceAll("[^0-9]", "");
        
        // Handle SG numbers: remove leading 0, add country code
        if (normalized.startsWith("0")) {
            normalized = "65" + normalized.substring(1);
        } else if (!normalized.startsWith("65")) {
            normalized = "65" + normalized;
        }
        log.info("Normalized mobile number: {}", normalized);
        return normalized;
    }

    /**
     * Trim a string value, returning empty string if null.
     * Used to clean up database values before using in SMS/Email content.
     *
     * @param value String to trim
     * @return Trimmed string or empty string if null
     */
    public String trimValue(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * Extract local SG mobile number (without country code) from an input.
     * - Removes all non-digits
     * - Strips leading 0065 or 65 if present
     * - Returns the remaining digits (typically 8-digit SG number)
     */
    private String extractLocalNumber(String phone) {
        if (phone == null) return null;
        // Keep digits only
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0065")) {
            digits = digits.substring(4);
        } else if (digits.startsWith("65") && digits.length() > 8) {
            digits = digits.substring(2);
        }
        return digits;
    }
    
    /**
     * Get processing stage from a record for saving to notification records.
     * Priority: nextProcessingStage > next_processing_stage > lastProcessingStage > last_processing_stage
     * For ENA records, nextProcessingStage may be null, so we fallback to lastProcessingStage (which is 'ENA').
     */
    private String getNextProcessingStageFromRecord(Map<String, Object> record) {
        String stage = (String) record.get("nextProcessingStage");
        if (stage == null) {
            stage = (String) record.get("next_processing_stage");
        }
        // Fallback to lastProcessingStage if nextProcessingStage is null (e.g., ENA records)
        if (stage == null) {
            stage = (String) record.get("lastProcessingStage");
        }
        if (stage == null) {
            stage = (String) record.get("last_processing_stage");
        }
        return stage;
    }

    /**
     * Combine notices for the same recipient with same stage and due date
     * UPDATED: ENA notices for individuals (idType != "B") are NOT combined (separate SMS for each)
     * ENA notices for companies (idType = "B") CAN be combined (email notifications)
     * 
     * @param records List of notices to process
     * @return Combined notice records
     */
    public List<Map<String, Object>> combineNotices(List<Map<String, Object>> records) {
        try {
            log.info("Combining notices for same recipient (ENA individual notices will NOT be combined)");
            
            // Group notices by recipient and processing stage
            Map<String, Map<String, Object>> combinedMap = new HashMap<>();
            
            for (Map<String, Object> record : records) {
                String recipientKey = getRecipientKey(record);
                String stage = (String) record.get("nextProcessingStage");

                Object dueDateObj = record.get("paymentDueDate");
                String dueDate = dueDateObj != null ? dueDateObj.toString() : "";
                
                // Get idType to determine if this is a company or individual
                String idType = (String) record.get("idType");
                if (idType == null) {
                    idType = (String) record.get("id_type");
                }
                
                // ENA notices for individuals (idType != "B") should NOT be combined - each gets separate SMS
                // ENA notices for companies (idType = "B") CAN be combined since they use email
                if ("ENA".equals(stage) && !"B".equals(idType)) {
                    String uniqueKey = recipientKey + "_ENA_" + record.get("noticeNo");
                    combinedMap.put(uniqueKey, new HashMap<>(record));
                    continue;
                }
                
                // All other cases can be combined (ENA companies, non-ENA notices)
                String compositeKey = recipientKey + "_" + (stage != null ? stage : "NA") + "_" + dueDate;
                
                if (combinedMap.containsKey(compositeKey)) {
                    // Add notice to existing combined record
                    Map<String, Object> combined = combinedMap.get(compositeKey);
                    
                    // Append notice number
                    String existingNotices = (String) combined.get("combinedNotices");
                    String newNotice = (String) record.get("noticeNo");
                    combined.put("combinedNotices", existingNotices + "." + newNotice);
                    
                    // Update amount with null safety
                    BigDecimal existingAmount = (BigDecimal) combined.get("amountPayable");
                    BigDecimal newAmount = (BigDecimal) record.get("amountPayable");
                    
                    if (existingAmount == null) existingAmount = BigDecimal.ZERO;
                    if (newAmount == null) newAmount = BigDecimal.ZERO;
                    
                    combined.put("amountPayable", existingAmount.add(newAmount));
                    
                    // Handle vehicle number - use first non-null vehicle
                    if (combined.get("vehicleNo") == null && record.get("vehicleNo") != null) {
                        combined.put("vehicleNo", record.get("vehicleNo"));
                    }
                    
                    // Handle name - use first non-null name
                    if (combined.get("name") == null && record.get("name") != null) {
                        combined.put("name", record.get("name"));
                    }
                    
                } else {
                    // Create new combined record with null safety
                    Map<String, Object> combined = new HashMap<>(record);
                    combined.put("combinedNotices", record.get("noticeNo"));
                    
                    // Ensure amountPayable is not null
                    if (combined.get("amountPayable") == null) {
                        combined.put("amountPayable", BigDecimal.ZERO);
                    }
                    
                    combinedMap.put(compositeKey, combined);
                }
            }
            
            return new ArrayList<>(combinedMap.values());
            
        } catch (Exception e) {
            log.error("Error combining notices: {}", e.getMessage(), e);
            return records;
        }
    }

    private String getRecipientKey(Map<String, Object> record) {
        String idType = (String) record.get("idType");
        if (idType == null) {
            idType = (String) record.get("id_type");
        }
        if ("B".equals(idType)) {
            // Business - use UEN
            Object uen = record.get("idNo");
            if (uen == null) {
                uen = record.get("id_no");
            }
            return "B_" + (uen != null ? uen : "");
        } else {
            // Individual - use ID number
            Object idNo = record.get("idNo");
            if (idNo == null) {
                idNo = record.get("id_no");
            }
            return "I_" + (idNo != null ? idNo : "");
        }
    }

    /**
     * Validate contact info before sending notifications
     * FIXED: Handle company records (idType B) by validating email, individuals by validating phone
     * 
     * @param record The record to validate
     * @return true if contact info is valid
     */
    public boolean validateContactInfo(Map<String, Object> record) {
        // Check for DataHive errors
        if (record.containsKey("contactError")) {
            log.warn("Contact validation failed: {}", record.get("contactError"));
            return false;
        }
        
        // Check ID type to determine validation logic
        String idType = (String) record.get("idType");
        if (idType == null) {
            idType = (String) record.get("id_type");
        }
        
        if ("B".equals(idType)) {
            // Company record - validate email address
            String email = (String) record.get("email");
            if (email == null || email.isEmpty()) {
                log.warn("No email address available for company notice: {}", record.get("noticeNo"));
                return false;
            }
            log.debug("Company record {} has valid email: {}", record.get("noticeNo"), email);
            return true;
        } else {
            // Individual record - validate phone number
            String phone = (String) record.get("phoneNo");
            if (phone == null || phone.isEmpty()) {
                log.warn("No phone number available for individual notice: {}", record.get("noticeNo"));
                return false;
            }
            log.debug("Individual record {} has valid phone: {}", record.get("noticeNo"), phone);
            return true;
        }
    }

    /**
     * Refresh contact info from DataHive
     * 
     * @param record The record to refresh
     */
    // public void refreshContactInfo(Map<String, Object> record) {
    //     try {
    //         String idType = (String) record.get("idType");
    //         if (idType == null) {
    //             idType = (String) record.get("id_type");
    //         }
            
    //         if ("B".equals(idType)) {
    //             // Company record - refresh from CorpPass
    //             refreshCompanyContactInfo(record);
    //         } else {
    //             // Individual record - refresh from Singpass (existing logic)
    //             refreshIndividualContactInfo(record);
    //         }
    //     } catch (Exception e) {
    //         log.error("Error refreshing contact info: {}", e.getMessage(), e);
    //         record.put("contactError", "DataHive error: " + e.getMessage());
    //     }
    // }

    public void refreshContactInfo(Map<String, Object> record) {
        // Get idType early - needed for both DataHive disabled and enabled paths
        String idType = (String) record.get("idType");
        if (idType == null) {
            idType = (String) record.get("id_type");
        }

        if (!"true".equalsIgnoreCase(statusGetDatahive)) {
            log.debug("Skipping DataHive query, using existing contact info from record");
            // Only set normalizedPhone for individuals (not companies)
            // Companies (idType = "B") should only receive email, not SMS
            if (!"B".equals(idType) && record.get("phoneNo") != null) {
                record.put("normalizedPhone", normalizeMobile((String) record.get("phoneNo")));
            }
            return;
        }

        try {
            if ("B".equals(idType)) {
                refreshCompanyContactInfo(record);
            } else {
                refreshIndividualContactInfo(record);
            }
        } catch (Exception e) {
            log.error("Error refreshing contact info: {}", e.getMessage(), e);
            record.put("contactError", "DataHive error: " + e.getMessage());
        }
    }
    
    /**
     * Refresh company contact info from CorpPass DataHive
     * 
     * @param record The company record to refresh
     */
    private void refreshCompanyContactInfo(Map<String, Object> record) {
        try {
            String uen = (String) record.get("idNo");
            
            if (uen == null) {
                log.warn("No UEN (idNo) found for company record: {}", record.get("noticeNo"));
                record.put("contactError", "No UEN available");
                return;
            }
            
            // Query CorpPass DataHive for company email
            String sql = "SELECT NAME, CP_ACCOUNT_EMAIL, ACCOUNT_TYPE, ACCOUNT_STATUS " +
                        "FROM V_DH_GOVTECH_CORPPASS_DELTA " +
                        "WHERE ENTITY_ID = '" + uen + "'";
            
            JsonNode result = dataHiveService.executeQueryAsyncCustom(
                sql, 
                datahiveDatabase, 
                datahiveSchema, 
                datahiveWarehouse, 
                datahiveRole
            );
            
            if (result == null || result.isEmpty()) {
                log.warn("No company info found in DataHive for UEN: {}", uen);
                record.put("contactError", "Company not found in DataHive");
                return;
            }
            
            // Parse company info with enhanced format handling
            Map<String, Object> companyInfo = parseCompanyDataFromDataHive(result);
            if (companyInfo == null || companyInfo.isEmpty()) {
                log.warn("Failed to parse company info from DataHive for UEN: {}", uen);
                record.put("contactError", "Failed to parse company data");
                return;
            }
            
            // Extract admin email and account info
            String adminEmail = (String) companyInfo.get("CP_ACCOUNT_EMAIL");
            String accountType = (String) companyInfo.get("ACCOUNT_TYPE");
            String accountStatus = (String) companyInfo.get("ACCOUNT_STATUS");

            // Validate account type and status (as per diagram requirement)
            if (adminEmail != null && !adminEmail.trim().isEmpty()) {
                // Check if account_type = A01 and status = ACTIVE
                boolean isAdminAccount = "A01".equals(accountType);
                boolean isActiveAccount = "ACTIVE".equals(accountStatus);

                if (!isAdminAccount || !isActiveAccount) {
                    log.warn("No active admin account found for company UEN: {}. account_type={}, status={}",
                            uen, accountType, accountStatus);

                    // Check if this is ENA stage and patch to RD1 if needed
                    String nextStage = (String) record.get("nextProcessingStage");
                    if (nextStage == null) {
                        nextStage = (String) record.get("next_processing_stage");
                    }
                    if ("ENA".equals(nextStage)) {
                        log.info("No active admin for ENA company notice {}. Patching to RD1 next day.", record.get("noticeNo"));
                        patchNextStageToRd1NextDay(record);
                    }
                    record.put("contactError", "No active admin account available");
                    return;
                }
            }

            // Build email list (only for valid admin accounts)
            List<String> emails = new ArrayList<>();
            if (adminEmail != null && !adminEmail.trim().isEmpty()) {
                emails.add(adminEmail.trim());
            }

            if (emails.isEmpty()) {
                log.warn("No admin email found for company UEN: {}", uen);
                // Check if this is ENA stage and patch to RD1 if needed
                String nextStage = (String) record.get("nextProcessingStage");
                if (nextStage == null) {
                    nextStage = (String) record.get("next_processing_stage");
                }
                if ("ENA".equals(nextStage)) {
                    log.info("No admin email for ENA company notice {}. Patching to RD1 next day.", record.get("noticeNo"));
                    patchNextStageToRd1NextDay(record);
                }
                record.put("contactError", "No admin email available");
                return;
            }
            
            // Update record with company email info
            record.put("email", String.join(",", emails));
            record.put("adminEmails", emails);
            
            // Update OCMS company contact info in database
            updateOcmsCompanyContactInfo(record, emails);
            
            log.info("Refreshed company contact info for UEN {}: {} admin emails", uen, emails.size());
            
        } catch (Exception e) {
            log.error("Error refreshing company contact info: {}", e.getMessage(), e);
            record.put("contactError", "CorpPass DataHive error: " + e.getMessage());
        }
    }
    
    /**
     * Parse company data from DataHive JsonNode with multiple format support
     * 
     * @param result JsonNode from DataHive
     * @return Parsed company data map
     */
    private Map<String, Object> parseCompanyDataFromDataHive(JsonNode result) {
        try {
            log.debug("Parsing company DataHive result: {}", result.toString());
            
            // Handle multiple possible shapes of DataHive result
            if (result == null) {
                log.warn("DataHive returned null for company data");
                return null;
            }
            
            if (result.isArray() && result.size() > 0) {
                JsonNode first = result.get(0);
                
                // Case 1: Object with fields NAME and CP_ACCOUNT_EMAIL
                if (first.isObject()) {
                    Map<String, Object> companyMap = new HashMap<>();
                    first.fields().forEachRemaining(entry -> {
                        companyMap.put(entry.getKey(), entry.getValue().asText());
                    });
                    log.info("Parsed company data from object format: {}", companyMap);
                    return companyMap;
                }
                
                // Case 2: Array of values [name, email, account_type, account_status]
                if (first.isArray() && first.size() >= 4) {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("NAME", first.get(0).asText());
                    companyMap.put("CP_ACCOUNT_EMAIL", first.get(1).asText());
                    companyMap.put("ACCOUNT_TYPE", first.get(2).asText());
                    companyMap.put("ACCOUNT_STATUS", first.get(3).asText());
                    log.info("Parsed company data from array format: {}", companyMap);
                    return companyMap;
                }
                
                // Case 3: Single value (just email)
                if (first.isValueNode()) {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("CP_ACCOUNT_EMAIL", first.asText());
                    log.info("Parsed company data from scalar format: {}", companyMap);
                    return companyMap;
                }
            }
            
            // Case 4: Direct object (not in array)
            if (result.isObject()) {
                Map<String, Object> companyMap = new HashMap<>();
                result.fields().forEachRemaining(entry -> {
                    companyMap.put(entry.getKey(), entry.getValue().asText());
                });
                log.info("Parsed company data from direct object: {}", companyMap);
                return companyMap;
            }
            
            log.warn("Unknown DataHive result format for company data: {}", result.getNodeType());
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing company data from DataHive: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update OCMS company contact info in database
     * 
     * @param record The company record to update
     * @param emails List of admin emails
     */
    private void updateOcmsCompanyContactInfo(Map<String, Object> record, List<String> emails) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            String uen = (String) record.get("idNo");
            
            if (noticeNo == null || uen == null) {
                log.warn("Cannot update OCMS company contact: noticeNo or idNo missing");
                return;
            }
            
            // Prepare update data
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("emailAddr", String.join(",", emails));
            updateData.put("updDate", LocalDateTime.now());
            updateData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            // Filter by noticeNo and idNo
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            filter.put("idNo", uen);
            
            // Update the ocms_offence_notice_owner_driver table
            tableQueryService.patch("ocms_offence_notice_owner_driver", filter, updateData);
            log.info("Updated company contact info for notice {} and UEN {}", noticeNo, uen);
            
        } catch (Exception e) {
            log.error("Error updating OCMS company contact info: {}", e.getMessage(), e);
        }
    }

    /**
     * Refresh individual contact info from Singpass DataHive (renamed from original method)
     * 
     * @param record The individual record to refresh
     */
    private void refreshIndividualContactInfo(Map<String, Object> record) {
        try {
            String idNo = (String) record.get("idNo");
            
            if (idNo != null) {
                // Use Singpass-specific DataHive method
                Map<String, Object> contactInfo = getContactInfoFromSingpass(idNo);
                
                if (contactInfo == null) {
                    log.warn("No contact info found in DataHive for ID: {}", idNo);
                    record.put("contactError", "DataHive contact not found");
                    
                    // Check if this is ENA stage and patch to RD1 if needed
                    String nextStage = (String) record.get("nextProcessingStage");
                    if (nextStage == null) {
                        nextStage = (String) record.get("next_processing_stage");
                    }
                    if ("ENA".equals(nextStage)) {
                        log.info("No contact info for ENA notice {}. Patching to RD1 next day.", record.get("noticeNo"));
                        patchNextStageToRd1NextDay(record);
                    }
                    return;
                }
                
                // Extract mobile number with proper parsing
                String dhPhoneNo = null;
                Object contactObj = contactInfo.get("CONTACT");
                if (contactObj != null) {
                    String contactStr = contactObj.toString();
                    if (contactStr.length() >= 8) {
                        dhPhoneNo = contactStr;
                        log.info("Extracted mobile number from DataHive: {}", dhPhoneNo);
                    } else {
                        log.warn("Invalid mobile number format from DataHive: {}", contactStr);
                        record.put("contactError", "Invalid mobile format");
                        
                        // Check if this is ENA stage and patch to RD1 if needed
                        String nextStage = (String) record.get("nextProcessingStage");
                        if (nextStage == null) {
                            nextStage = (String) record.get("next_processing_stage");
                        }
                        if ("ENA".equals(nextStage)) {
                            log.info("Invalid mobile for ENA notice {}. Patching to RD1 next day.", record.get("noticeNo"));
                            patchNextStageToRd1NextDay(record);
                        }
                        return;
                    }
                }
                
                // Update record with fresh contact info
                record.put("contact", contactInfo.get("CONTACT"));
                record.put("phoneNo", dhPhoneNo);
                
                // Normalize mobile number
                record.put("normalizedPhone", normalizeMobile(dhPhoneNo));
                
                // Update OCMS contact info
                updateOcmsContactInfo(record);
            }
        } catch (Exception e) {
            log.error("Error refreshing individual contact info: {}", e.getMessage(), e);
            record.put("contactError", "Singpass DataHive error: " + e.getMessage());
        }
    }

    /**
     * Validate contact info before sending notifications
     * 
     * @param record The record to validate
     * @return true if contact info is valid
     */
    // public boolean validateContactInfo(Map<String, Object> record) {
    //     // Check for DataHive errors
    //     if (record.containsKey("contactError")) {
    //         log.warn("Contact validation failed: {}", record.get("contactError"));
    //         return false;
    //     }
        
    //     // Check required fields
    //     String phone = (String) record.get("phoneNo");
    //     if (phone == null || phone.isEmpty()) {
    //         log.warn("No phone number available for notice: {}", record.get("noticeNo"));
    //         return false;
    //     }
        
    //     return true;
    // }

    /**
     * Update OCMS contact info in the database
     * 
     * @param record The record containing fresh contact info
     */
    private void updateOcmsContactInfo(Map<String, Object> record) {
        try {
            String noticeNo = (String) record.get("noticeNo");
            String idNo = (String) record.get("idNo");
            
            if (noticeNo == null || idNo == null) {
                log.warn("Cannot update OCMS contact: noticeNo or idNo missing");
                return;
            }
            
            // Prepare update data
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("offenderTelNo", record.get("phoneNo"));
            updateData.put("emailAddr", record.get("email"));
            updateData.put("updDate", LocalDateTime.now());
            updateData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            // Filter by noticeNo and idNo
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            filter.put("idNo", idNo);
            
            // Update the ocms_offence_notice_owner_driver table
            tableQueryService.patch("ocms_offence_notice_owner_driver", filter, updateData);
            log.info("Updated contact info for notice {} and id {}", noticeNo, idNo);
            
        } catch (Exception e) {
            log.error("Error updating OCMS contact info: {}", e.getMessage(), e);
        }
    }
}