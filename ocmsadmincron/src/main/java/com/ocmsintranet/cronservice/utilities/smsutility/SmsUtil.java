package com.ocmsintranet.cronservice.utilities.smsutility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ocmsintranet.cronservice.utilities.ApimUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class SmsUtil {

    @Autowired
    private ApimUtil apimUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${ocms.APIM.sms.endpoint}")
    private String smsEndpoint;
    
    @Value("${ocms.APIM.sms.secretName:${ocms.APIM.secretName}}")
    private String smsSecretName;
    
    @Value("${sms.enable:true}")
    private boolean enableSms;

    @Value("${ocms.APIM.batch.sms.endpoint:/postman2-sms/batch}")
    private String batchSmsBaseEndpoint;

    // SMS body length constants
    public static final int SMS_BODY_RECOMMENDED_LENGTH = 320;
    public static final int SMS_BODY_MAX_LENGTH = 1000;

    /**
     * Inner class to represent SMS record for batch sending
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsRecord {
        private String recipient;
        private String language;
        private Map<String, String> templateParams;
    }
    
    /**
     * Sanitize and validate SMS content for both Postman2 and GSM-7 compliance.
     *
     * <h3>Postman2 Excluded Characters (from documentation):</h3>
     * <pre>` | { } [ ] ~ \ ' ' " " « » € ¬</pre>
     * Use safe replacements (e.g., EUR for €). Avoid backslash.
     *
     * <h3>GSM-7 Character Set:</h3>
     * <ul>
     *   <li>Basic set (160 chars/SMS): A-Z a-z 0-9, common punctuation, some accented chars</li>
     *   <li>Extended set (counted as 2 chars): | ^ € { } [ ] ~ \</li>
     *   <li>NOT in GSM-7: curly quotes, em/en dash, ellipsis, most Unicode</li>
     * </ul>
     *
     * <h3>This method:</h3>
     * <ol>
     *   <li>Trims leading/trailing whitespace</li>
     *   <li>Normalizes multiple spaces to single space</li>
     *   <li>Replaces non-GSM-7 characters with GSM-7 equivalents</li>
     *   <li>Removes Postman2 excluded characters that have no safe replacement</li>
     * </ol>
     *
     * @param content SMS content to validate
     * @return Sanitized content safe for both Postman2 and GSM-7
     */
    public String validateSmsBody(String content) {
        if (content == null) return null;

        // 1. Trim leading/trailing spaces
        content = content.trim();

        // 2. Normalize multiple spaces/whitespace to single space
        content = content.replaceAll("\\s+", " ");

        // 3. Replace Postman2 excluded characters with safe alternatives
        // (These are also NOT in GSM-7 basic set or need special handling)

        // Currency - Postman2 requires replacement
        content = content.replace("\u20AC", "EUR");     // € Euro sign → EUR

        // Curly/Smart quotes → straight quotes (NOT in GSM-7)
        content = content.replace("\u2018", "'");       // ' Left single quote
        content = content.replace("\u2019", "'");       // ' Right single quote
        content = content.replace("\u201A", "'");       // ‚ Single low-9 quote
        content = content.replace("\u201B", "'");       // ‛ Single high-reversed-9 quote
        content = content.replace("\u201C", "\"");      // " Left double quote
        content = content.replace("\u201D", "\"");      // " Right double quote
        content = content.replace("\u201E", "\"");      // „ Double low-9 quote
        content = content.replace("\u201F", "\"");      // ‟ Double high-reversed-9 quote

        // Guillemets → angle brackets (NOT in GSM-7)
        content = content.replace("\u00AB", "<<");      // « Left guillemet
        content = content.replace("\u00BB", ">>");      // » Right guillemet
        content = content.replace("\u2039", "<");       // ‹ Single left guillemet
        content = content.replace("\u203A", ">");       // › Single right guillemet

        // Dashes → hyphen (NOT in GSM-7)
        content = content.replace("\u2013", "-");       // – En dash
        content = content.replace("\u2014", "-");       // — Em dash
        content = content.replace("\u2015", "-");       // ― Horizontal bar
        content = content.replace("\u2212", "-");       // − Minus sign

        // Other punctuation replacements (NOT in GSM-7)
        content = content.replace("\u2026", "...");     // … Ellipsis
        content = content.replace("\u00AC", "-");       // ¬ Not sign
        content = content.replace("\u00B4", "'");       // ´ Acute accent
        content = content.replace("\u0060", "'");       // ` Grave accent (backtick)
        content = content.replace("\u00A0", " ");       // Non-breaking space
        content = content.replace("\u2002", " ");       // En space
        content = content.replace("\u2003", " ");       // Em space
        content = content.replace("\u2009", " ");       // Thin space

        // Bullet points → hyphen (NOT in GSM-7)
        content = content.replace("\u2022", "-");       // • Bullet
        content = content.replace("\u2023", "-");       // ‣ Triangular bullet
        content = content.replace("\u25E6", "-");       // ◦ White bullet

        // 4. Remove Postman2 excluded characters that cannot be safely replaced
        // These are in GSM-7 extended but Postman2 does not support them:
        // | { } [ ] ~ \
        // Plus backtick which is not in GSM-7 either
        content = content.replaceAll("[|{}\\[\\]~\\\\]", "");

        // 5. Remove any remaining non-GSM-7 characters
        // Keep only: GSM-7 basic character set
        // A-Z, a-z, 0-9, and GSM-7 punctuation/symbols
        content = removeNonGsm7Characters(content);

        // 6. Validate and truncate body length
        content = validateAndTruncateSmsBody(content);

        return content;
    }

    /**
     * Validate SMS body length and truncate if exceeds max length.
     * Logs warning if exceeds recommended length (320 chars).
     * Truncates with "..." if exceeds max length (1000 chars).
     *
     * @param body SMS body content
     * @return Validated/truncated body
     */
    public String validateAndTruncateSmsBody(String body) {
        if (body == null) return null;

        if (body.length() > SMS_BODY_MAX_LENGTH) {
            log.warn("SMS body exceeds max length ({}), truncating to {}",
                    body.length(), SMS_BODY_MAX_LENGTH);
            body = body.substring(0, SMS_BODY_MAX_LENGTH - 3) + "...";
        } else if (body.length() > SMS_BODY_RECOMMENDED_LENGTH) {
            log.info("SMS body exceeds recommended length ({} > {})",
                    body.length(), SMS_BODY_RECOMMENDED_LENGTH);
        }

        return body;
    }

    /**
     * Remove characters that are not in the GSM-7 basic character set.
     * This ensures the SMS will use 7-bit encoding (160 chars/SMS).
     *
     * GSM-7 Basic Character Set:
     * - Space, newline, carriage return
     * - A-Z, a-z, 0-9
     * - @ £ $ ¥ è é ù ì ò Ç Ø ø Å å Δ _ Φ Γ Λ Ω Π Ψ Σ Θ Ξ
     * - Æ æ ß É Ä Ö Ñ Ü § ¿ ä ö ñ ü à
     * - ! " # ¤ % & ' ( ) * + , - . / : ; < = > ?
     *
     * @param content Content to filter
     * @return Content with only GSM-7 characters
     */
    private String removeNonGsm7Characters(String content) {
        if (content == null) return null;

        // GSM-7 basic character set (excluding extended chars which Postman2 doesn't support)
        // Using regex to keep only allowed characters
        String gsm7BasicChars =
            " \n\r" +                                    // Whitespace
            "A-Za-z0-9" +                                // Alphanumeric
            "@£$¥èéùìòÇØøÅåΔ_ΦΓΛΩΠΨΣΘΞ" +               // GSM-7 special chars
            "ÆæßÉÄÖÑÜ§¿äöñüà" +                          // GSM-7 accented chars
            "!\"#¤%&'()*+,\\-./:;<=>?" +                 // GSM-7 punctuation
            "\u000C";                                    // Form feed (GSM-7)

        // Build regex pattern for allowed characters
        StringBuilder result = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (isGsm7BasicChar(c)) {
                result.append(c);
            }
            // Non-GSM-7 characters are silently removed
        }

        return result.toString();
    }

    /**
     * Check if a character is in the GSM-7 basic character set.
     *
     * @param c Character to check
     * @return true if character is GSM-7 compatible
     */
    private boolean isGsm7BasicChar(char c) {
        // GSM-7 Basic Character Set (03.38)
        // Organized by Unicode code points

        // Control characters
        if (c == '\n' || c == '\r' || c == '\f') return true;  // LF, CR, FF

        // Space
        if (c == ' ') return true;

        // Basic Latin
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= 'a' && c <= 'z') return true;
        if (c >= '0' && c <= '9') return true;

        // GSM-7 punctuation and symbols
        String gsm7Punctuation = "!\"#%&'()*+,-./:;<=>?@_";
        if (gsm7Punctuation.indexOf(c) >= 0) return true;

        // GSM-7 currency and special symbols
        // £ (00A3), ¤ (00A4), ¥ (00A5), § (00A7)
        if (c == '\u00A3' || c == '\u00A4' || c == '\u00A5' || c == '\u00A7') return true;

        // GSM-7 accented characters (Latin-1 Supplement)
        // ¿ (00BF), À-Å (00C0-00C5), Æ (00C6), Ç (00C7), È-Ë (00C8-00CB)
        // Ì-Ñ (00CC-00D1), Ò-Ö (00D2-00D6), Ø (00D8), Ù-Ü (00D9-00DC)
        // ß (00DF), à-å (00E0-00E5), æ (00E6), ç (00E7), è-ë (00E8-00EB)
        // ì-ñ (00EC-00F1), ò-ö (00F2-00F6), ø (00F8), ù-ü (00F9-00FC)
        String gsm7Accented = "\u00BF\u00C4\u00C5\u00C6\u00C7\u00C9\u00D1\u00D6\u00D8\u00DC" +
                              "\u00DF\u00E0\u00E4\u00E5\u00E6\u00E8\u00E9\u00EC\u00F1\u00F2" +
                              "\u00F6\u00F8\u00F9\u00FC";
        if (gsm7Accented.indexOf(c) >= 0) return true;

        // Greek letters used in GSM-7
        // Δ (0394), Φ (03A6), Γ (0393), Λ (039B), Ω (03A9)
        // Π (03A0), Ψ (03A8), Σ (03A3), Θ (0398), Ξ (039E)
        String gsm7Greek = "\u0394\u03A6\u0393\u039B\u03A9\u03A0\u03A8\u03A3\u0398\u039E";
        if (gsm7Greek.indexOf(c) >= 0) return true;

        return false;
    }

    /**
     * Trim a string value, returning empty string if null
     *
     * @param value String to trim
     * @return Trimmed string or empty string if null
     */
    public String trimValue(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * Send SMS message using APIM
     *
     * @param phoneNumber Recipient phone number
     * @param message SMS message body
     * @param language Language (default: english)
     * @return ObjectNode with status and response
     */
    public ObjectNode sendSms(String phoneNumber, String message, String language) {
        ObjectNode smsResult = objectMapper.createObjectNode();
        
        // Check if SMS is enabled
        if (!enableSms) {
            log.warn("SMS is disabled. SMS was not sent");
            smsResult.put("status", "disabled");
            smsResult.put("message", "SMS sending is disabled by configuration");
            return smsResult;
        }
        
        try {
            // Log the actual endpoint being used
            log.info("Using SMS endpoint: {}", smsEndpoint);
            
            // Create request body using the original PLUS format
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("recipient", phoneNumber);
            requestBody.put("language", language != null ? language : "english");

            // Create nested values object with body field
            // Validate and trim the message content
            ObjectNode values = objectMapper.createObjectNode();
            values.put("body", validateSmsBody(message));
            requestBody.set("values", values);
            
            log.info("Sending SMS via APIM");
            
            // Call APIM endpoint with SMS-specific secret name
            JsonNode response = apimUtil.callApimPostCustomSecret(
                    smsEndpoint, 
                    requestBody, 
                    JsonNode.class,
                    smsSecretName);
            
            smsResult.put("status", "success");
            smsResult.set("response", response);
            
        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage(), e);
            smsResult.put("status", "failed");
            smsResult.put("message", e.getMessage());
        }
        
        return smsResult;
    }
    
    /**
     * Send batch SMS messages using CSV file through APIM
     * CSV format: recipient,language,<template_params>
     * 
     * @param csvFile MultipartFile containing the CSV data
     * @return ObjectNode with batchId and status
     */
    public ObjectNode sendBatchSms(MultipartFile csvFile) {
        ObjectNode batchResult = objectMapper.createObjectNode();
        
        if (!enableSms) {
            log.warn("SMS is disabled. Batch SMS was not sent");
            batchResult.put("status", "disabled");
            batchResult.put("message", "SMS sending is disabled by configuration");
            return batchResult;
        }
        
        try {
            // Build endpoint: /postman2-sms/batch/messages
            String endpoint = batchSmsBaseEndpoint + "/messages";
            log.info("Sending batch SMS via APIM. Endpoint: {}", endpoint);

            // Call APIM with multipart file - APIM handles campaign ID routing
            JsonNode response = apimUtil.callApimPostMultipart(
                    endpoint,
                    csvFile,
                    smsSecretName);
            
            batchResult.put("status", "success");
            batchResult.set("response", response);
            
            // Extract batchId if present
            if (response.has("batchId")) {
                batchResult.put("batchId", response.get("batchId").asText());
            }
            
        } catch (Exception e) {
            log.error("Error sending batch SMS: {}", e.getMessage(), e);
            batchResult.put("status", "failed");
            batchResult.put("message", e.getMessage());
        }
        
        return batchResult;
    }
    
    /**
     * Convert a list of SMS records to CSV format for batch sending
     * 
     * @param smsRecords List of SMS records to convert
     * @param includeHeaders Whether to include CSV headers
     * @return MultipartFile containing the CSV data
     */
    public MultipartFile convertToCSV(List<SmsRecord> smsRecords, boolean includeHeaders) {
        try {
            StringBuilder csvBuilder = new StringBuilder();
            
            // Add headers if needed
            if (includeHeaders && !smsRecords.isEmpty()) {
                // Basic headers
                csvBuilder.append("recipient,language");
                
                // Add any template parameter headers from first record
                SmsRecord firstRecord = smsRecords.get(0);
                if (firstRecord.getTemplateParams() != null) {
                    for (String key : firstRecord.getTemplateParams().keySet()) {
                        csvBuilder.append(",").append(key);
                    }
                }
                csvBuilder.append("\n");
            }
            
            // Add data rows
            for (SmsRecord record : smsRecords) {
                csvBuilder.append(record.getRecipient())
                          .append(",")
                          .append(record.getLanguage() != null ? record.getLanguage() : "english");

                // Add template parameters if present
                if (record.getTemplateParams() != null) {
                    for (Map.Entry<String, String> entry : record.getTemplateParams().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();

                        // Wrap body field in double quotes and escape internal quotes
                        if ("body".equals(key)) {
                            // Escape double quotes by doubling them (RFC 4180)
                            String escapedValue = value.replace("\"", "\"\"");
                            csvBuilder.append(",\"").append(escapedValue).append("\"");
                        } else {
                            // Other fields: no quotes
                            csvBuilder.append(",").append(value);
                        }
                    }
                }
                csvBuilder.append("\n");
            }
            
            // Convert to MultipartFile
            byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
            String fileName = "batch_sms_" + System.currentTimeMillis() + ".csv";
            
            // Create a simple MultipartFile implementation
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }
                
                @Override
                public String getOriginalFilename() {
                    return fileName;
                }
                
                @Override
                public String getContentType() {
                    return "text/csv";
                }
                
                @Override
                public boolean isEmpty() {
                    return csvBytes.length == 0;
                }
                
                @Override
                public long getSize() {
                    return csvBytes.length;
                }
                
                @Override
                public byte[] getBytes() throws IOException {
                    return csvBytes;
                }
                
                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(csvBytes);
                }
                
                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    try (FileOutputStream fos = new FileOutputStream(dest)) {
                        fos.write(csvBytes);
                    }
                }
                
                @Override
                public Resource getResource() {
                    return new ByteArrayResource(csvBytes) {
                        @Override
                        public String getFilename() {
                            return fileName;
                        }
                    };
                }
            };
            
        } catch (Exception e) {
            log.error("Error converting SMS records to CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create CSV file", e);
        }
    }
    
    /**
     * Send batch SMS with simple messages (no template parameters)
     *
     * @param messages List of phone numbers and messages
     * @return ObjectNode with batchId, status, and csvContent (the CSV sent to Postman)
     */
    public ObjectNode sendBatchSmsSimple(List<Map<String, String>> messages) {
        ObjectNode result = objectMapper.createObjectNode();
        String csvContent = null;

        try {
            // Convert simple messages to SmsRecord format
            List<SmsRecord> smsRecords = new ArrayList<>();

            for (Map<String, String> msg : messages) {
                SmsRecord record = new SmsRecord();
                record.setRecipient(msg.get("phoneNumber"));
                record.setLanguage(msg.getOrDefault("language", "english"));

                // Add message as template parameter with 'body' key
                // Validate and trim the message content
                Map<String, String> params = new HashMap<>();
                params.put("body", validateSmsBody(msg.get("message")));
                record.setTemplateParams(params);

                smsRecords.add(record);
            }

            // Convert to CSV
            MultipartFile csvFile = convertToCSV(smsRecords, true);

            // Capture CSV content for error reporting
            csvContent = new String(csvFile.getBytes(), StandardCharsets.UTF_8);

            // Send batch
            result = sendBatchSms(csvFile);

            // Include CSV content in result for error reporting
            result.put("csvContent", csvContent);

            return result;

        } catch (Exception e) {
            log.error("Error sending simple batch SMS: {}", e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", e.getMessage());
            // Include CSV content if available (for error email attachment)
            if (csvContent != null) {
                result.put("csvContent", csvContent);
            }
            return result;
        }
    }
    
    /**
     * Send batch SMS from database query results
     * 
     * @param records List of database records with phone numbers and template data
     * @param phoneField Field name for phone number
     * @param languageField Field name for language (optional)
     * @param templateFields Map of template parameter names to field names
     * @return ObjectNode with batchId and status
     */
    public ObjectNode sendBatchSmsFromRecords(
            List<Map<String, Object>> records,
            String phoneField,
            String languageField,
            Map<String, String> templateFields) {
        
        try {
            List<SmsRecord> smsRecords = new ArrayList<>();
            
            for (Map<String, Object> record : records) {
                SmsRecord smsRecord = new SmsRecord();
                
                // Set recipient
                Object phone = record.get(phoneField);
                if (phone == null || phone.toString().isEmpty()) {
                    log.warn("Skipping record with missing phone number");
                    continue;
                }
                smsRecord.setRecipient(phone.toString());
                
                // Set language
                if (languageField != null && record.containsKey(languageField)) {
                    smsRecord.setLanguage(record.get(languageField).toString());
                } else {
                    smsRecord.setLanguage("english");
                }
                
                // Set template parameters
                // Validate and trim the body content if present
                if (templateFields != null && !templateFields.isEmpty()) {
                    Map<String, String> params = new HashMap<>();
                    for (Map.Entry<String, String> entry : templateFields.entrySet()) {
                        String paramName = entry.getKey();
                        String fieldName = entry.getValue();
                        if (record.containsKey(fieldName)) {
                            String value = record.get(fieldName).toString();
                            // Validate body content specifically
                            if ("body".equals(paramName)) {
                                value = validateSmsBody(value);
                            } else {
                                value = trimValue(value);
                            }
                            params.put(paramName, value);
                        }
                    }
                    smsRecord.setTemplateParams(params);
                }
                
                smsRecords.add(smsRecord);
            }
            
            if (smsRecords.isEmpty()) {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("status", "failed");
                result.put("message", "No valid records to send");
                return result;
            }

            // Convert to CSV and send
            MultipartFile csvFile = convertToCSV(smsRecords, true);
            return sendBatchSms(csvFile);

        } catch (Exception e) {
            log.error("Error sending batch SMS from records: {}", e.getMessage(), e);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Retrieve batch message statuses from Postman2
     * GET /postman2-sms/batch/{batchId}/messages
     *
     * @param batchId The batch ID returned from sendBatchSms response
     * @param limit Number of messages to retrieve (optional, default 100)
     * @return JsonNode with batch message statuses
     */
    public JsonNode retrieveBatchMessages(String batchId, Integer limit) {
        ObjectNode result = objectMapper.createObjectNode();

        if (batchId == null || batchId.isEmpty()) {
            log.error("BatchId is required for retrieving batch messages");
            result.put("status", "failed");
            result.put("message", "BatchId is required");
            return result;
        }

        try {
            // Build endpoint: /postman2-sms/batch/{batchId}/messages
            String endpoint = batchSmsBaseEndpoint + "/" + batchId + "/messages";

            // Build query parameters
            Map<String, String> queryParams = new HashMap<>();
            if (limit != null && limit > 0) {
                queryParams.put("limit", String.valueOf(limit));
            }

            log.info("Retrieving batch messages. Endpoint: {}, BatchId: {}, Limit: {}",
                    endpoint, batchId, limit);

            // Call APIM GET endpoint with custom secret
            JsonNode response = apimUtil.callApimGetCustomSecret(
                    endpoint,
                    queryParams.isEmpty() ? null : queryParams,
                    JsonNode.class,
                    smsSecretName);

            log.info("Successfully retrieved batch messages for batchId: {}", batchId);
            return response;

        } catch (Exception e) {
            log.error("Error retrieving batch messages for batchId {}: {}", batchId, e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Retry all failed messages in a batch
     * POST /postman2-sms/batch/{batchId}/retry
     *
     * @param batchId The batch ID to retry
     * @return ObjectNode with retry result
     */
    public ObjectNode retryBatch(String batchId) {
        ObjectNode result = objectMapper.createObjectNode();

        if (batchId == null || batchId.isEmpty()) {
            log.error("BatchId is required for retrying batch");
            result.put("status", "failed");
            result.put("message", "BatchId is required");
            return result;
        }

        try {
            // Build endpoint: /postman2-sms/batch/{batchId}/retry
            String endpoint = batchSmsBaseEndpoint + "/" + batchId + "/retry";

            log.info("Retrying batch. Endpoint: {}, BatchId: {}", endpoint, batchId);

            // Call APIM POST endpoint with custom secret (empty body)
            JsonNode response = apimUtil.callApimPostCustomSecret(
                    endpoint,
                    null,
                    JsonNode.class,
                    smsSecretName);

            result.put("status", "success");
            result.set("response", response);
            result.put("batchId", batchId);

            log.info("Successfully initiated retry for batchId: {}", batchId);

        } catch (Exception e) {
            log.error("Error retrying batch for batchId {}: {}", batchId, e.getMessage(), e);
            result.put("status", "failed");
            result.put("message", e.getMessage());
        }

        return result;
    }
}
