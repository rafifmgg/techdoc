package com.ocmsintranet.cronservice.dummyintegrations.smsdummyserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/dummy-sms")
@Slf4j
public class DummySmsController {
    // Hardcoded credentials - will be used to call the real SMS service
    private final String USERNAME = "56633";
    private final String PASSWORD = "fVtGNJ8ao93AuU";
    private final String FROM = "MGGTEST";
    private final String SMS_HOST = "https://mx.fortdigital.net/http/send-message";

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @PostConstruct
    public void init() {
        log.info("==========================================");
        log.info("DUMMY SMS SERVICE INITIALIZED");
        log.info("Using credentials: Username={}, From={}", USERNAME, FROM);
        log.info("SMS Service URL: {}", SMS_HOST);
        log.info("==========================================");
    }

    /**
     * Sends an SMS message using the FortDigital API
     * 
     * @param requestBody JSON payload with recipient, language, and values.body
     * @return ResponseEntity containing the result of the SMS operation
     */
    @PostMapping("/send")
    public ResponseEntity<ObjectNode> sendSms(@RequestBody JsonNode requestBody) {
        ObjectNode smsResult = objectMapper.createObjectNode();
        
        try {
            // Extract fields from the PLUS format payload
            String recipient = requestBody.get("recipient").asText();
            String language = requestBody.has("language") ? requestBody.get("language").asText() : "english";
            String message = requestBody.get("values").get("body").asText();
            
            // Ensure message is properly encoded for SMS gateway
            // This simulates how the PLUS system would handle the message
            
            log.info("Sending SMS to {}: {} (language: {})", recipient, message, language);
            
            // Build URL with query parameters - using a different approach to avoid duplicate parameters
            String encodedMessage = UriComponentsBuilder.fromPath("").queryParam("temp", message).build().toString();
            encodedMessage = encodedMessage.substring(6); // Remove the "temp=" part
            
            String smsUrl = SMS_HOST + 
                    "?username=" + USERNAME + 
                    "&password=" + PASSWORD + 
                    "&from=" + FROM + 
                    "&to=" + recipient + 
                    "&message=" + encodedMessage;
            
            log.info("Calling SMS API: {}", smsUrl);
            
            // For testing purposes, let's log the exact URL being called
            log.info("Exact SMS URL being called: {}", smsUrl);
            
            // Make the HTTP request - using getForEntity which returns the response body
            ResponseEntity<String> response = restTemplate.getForEntity(smsUrl, String.class);
            
            // Log the response for debugging
            log.info("SMS API response: {}", response.getBody());
            
            // Process response
            smsResult.put("status", "success");
            smsResult.put("statusCode", response.getStatusCodeValue());
            smsResult.put("response", response.getBody());
            
            log.info("SMS sent successfully to {} (language: {}). Response code: {}", 
                    recipient, language, response.getStatusCodeValue());
            return ResponseEntity.ok(smsResult);
            
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            smsResult.put("status", "failed");
            smsResult.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(smsResult);
        }
    }
    
    /**
     * Batch SMS endpoint that accepts CSV file and sends multiple SMS messages
     * 
     * @param file CSV file with format: recipient,language,message (or with template params)
     * @return ResponseEntity containing batch result with batchId
     */
    @PostMapping("/batch")
    public ResponseEntity<ObjectNode> sendBatchSms(@RequestParam("file") MultipartFile file) {
        ObjectNode batchResult = objectMapper.createObjectNode();
        String batchId = "BATCH_" + UUID.randomUUID().toString();
        
        try {
            log.info("==========================================");
            log.info("BATCH SMS REQUEST RECEIVED");
            log.info("Batch ID: {}", batchId);
            log.info("File name: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());
            log.info("==========================================");
            
            // Parse CSV file
            List<SmsRecord> smsRecords = parseCsvFile(file);
            log.info("Parsed {} SMS records from CSV", smsRecords.size());
            
            // Track results
            int successCount = 0;
            int failedCount = 0;
            List<ObjectNode> individualResults = new ArrayList<>();
            
            // Send SMS for each record
            for (int i = 0; i < smsRecords.size(); i++) {
                SmsRecord record = smsRecords.get(i);
                ObjectNode recordResult = objectMapper.createObjectNode();
                recordResult.put("row", i + 1);
                recordResult.put("recipient", record.recipient);
                
                try {
                    // Build message from template parameters or use direct message
                    String message = record.message;
                    if (message == null || message.isEmpty()) {
                        // If no direct message, combine template parameters
                        StringBuilder msgBuilder = new StringBuilder();
                        if (record.templateParams != null && !record.templateParams.isEmpty()) {
                            record.templateParams.forEach((key, value) -> {
                                if (!key.equals("recipient") && !key.equals("language")) {
                                    msgBuilder.append(key).append(": ").append(value).append(" ");
                                }
                            });
                            message = msgBuilder.toString().trim();
                        }
                    }
                    
                    if (message == null || message.isEmpty()) {
                        message = "Test SMS from batch";
                    }
                    
                    log.info("Processing row {}: Sending SMS to {} - Message: {}", 
                            i + 1, record.recipient, message);
                    
                    // Encode message for URL
                    String encodedMessage = UriComponentsBuilder.fromPath("")
                            .queryParam("temp", message).build().toString();
                    encodedMessage = encodedMessage.substring(6); // Remove "temp="
                    
                    String smsUrl = SMS_HOST + 
                            "?username=" + USERNAME + 
                            "&password=" + PASSWORD + 
                            "&from=" + FROM + 
                            "&to=" + record.recipient + 
                            "&message=" + encodedMessage;
                    
                    // Send SMS
                    ResponseEntity<String> response = restTemplate.getForEntity(smsUrl, String.class);
                    
                    if (response.getStatusCodeValue() == 200) {
                        successCount++;
                        recordResult.put("status", "success");
                        recordResult.put("response", response.getBody());
                        log.info("Row {} sent successfully", i + 1);
                    } else {
                        failedCount++;
                        recordResult.put("status", "failed");
                        recordResult.put("error", "HTTP " + response.getStatusCodeValue());
                        log.warn("Row {} failed with status: {}", i + 1, response.getStatusCodeValue());
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    recordResult.put("status", "failed");
                    recordResult.put("error", e.getMessage());
                    log.error("Failed to send SMS for row {}: {}", i + 1, e.getMessage());
                }
                
                individualResults.add(recordResult);
                
                // Add small delay between messages to avoid overwhelming the gateway
                if (i < smsRecords.size() - 1) {
                    Thread.sleep(100); // 100ms delay between messages
                }
            }
            
            // Build response
            batchResult.put("isValid", true);
            batchResult.put("batchId", batchId);
            batchResult.put("totalRecords", smsRecords.size());
            batchResult.put("successCount", successCount);
            batchResult.put("failedCount", failedCount);
            batchResult.set("results", objectMapper.valueToTree(individualResults));
            
            log.info("==========================================");
            log.info("BATCH SMS COMPLETED");
            log.info("Batch ID: {}", batchId);
            log.info("Total: {}, Success: {}, Failed: {}", 
                    smsRecords.size(), successCount, failedCount);
            log.info("==========================================");
            
            return ResponseEntity.ok(batchResult);
            
        } catch (Exception e) {
            log.error("Error processing batch SMS: {}", e.getMessage(), e);
            batchResult.put("isValid", false);
            batchResult.put("batchId", batchId);
            batchResult.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(batchResult);
        }
    }
    
    /**
     * Parse CSV file into SMS records
     */
    private List<SmsRecord> parseCsvFile(MultipartFile file) throws Exception {
        List<SmsRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            
            String line;
            String[] headers = null;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] values = line.split(",");
                
                // First line should be headers
                if (headers == null) {
                    headers = values;
                    log.debug("CSV Headers: {}", String.join(", ", headers));
                    continue;
                }
                
                // Parse data row
                SmsRecord record = new SmsRecord();
                
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    String header = headers[i].trim().toLowerCase();
                    String value = values[i].trim();
                    
                    switch (header) {
                        case "recipient":
                        case "phone":
                        case "mobile":
                        case "phonenumber":
                            record.recipient = value;
                            break;
                        case "language":
                        case "lang":
                            record.language = value;
                            break;
                        case "message":
                        case "body":
                        case "text":
                            record.message = value;
                            break;
                        default:
                            // Store as template parameter
                            if (record.templateParams == null) {
                                record.templateParams = new java.util.HashMap<>();
                            }
                            record.templateParams.put(header, value);
                            break;
                    }
                }
                
                // Validate record has at least a recipient
                if (record.recipient == null || record.recipient.isEmpty()) {
                    log.warn("Skipping row {} - no recipient found", lineNumber);
                    continue;
                }
                
                // Set default language if not specified
                if (record.language == null || record.language.isEmpty()) {
                    record.language = "english";
                }
                
                records.add(record);
                log.debug("Parsed record {}: recipient={}, language={}", 
                        lineNumber, record.recipient, record.language);
            }
        }
        
        return records;
    }
    
    /**
     * Inner class to represent SMS record from CSV
     */
    private static class SmsRecord {
        String recipient;
        String language;
        String message;
        java.util.Map<String, String> templateParams;
    }
}
