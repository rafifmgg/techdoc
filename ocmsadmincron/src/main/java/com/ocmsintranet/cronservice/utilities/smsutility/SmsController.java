package com.ocmsintranet.cronservice.utilities.smsutility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller for handling SMS-related requests
 */
@RestController
@RequestMapping("/v1/sms")
@Slf4j
public class SmsController {
    
    @Autowired
    private SmsUtil smsUtil;
        
    /**
     * Endpoint for sending SMS messages
     */
    @PostMapping("/send")
    public ResponseEntity<ObjectNode> sendSms(@RequestBody SmsRequest request) {
        log.info("Received request to send SMS to: {}", request.getPhoneNumber());
        
        ObjectNode result = smsUtil.sendSms(
            request.getPhoneNumber(), 
            request.getMessage(), 
            request.getLanguage() != null ? request.getLanguage() : "english"
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Endpoint for testing batch SMS functionality with CSV file upload
     * 
     * @param file CSV file containing batch SMS data
     * @return Response with batch processing result
     */
    @PostMapping("/batch/send")
    public ResponseEntity<ObjectNode> sendBatchSms(@RequestParam("file") MultipartFile file) {
        log.info("Received request to send batch SMS with file: {}", file.getOriginalFilename());
        
        ObjectNode result = smsUtil.sendBatchSms(file);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Endpoint for testing batch SMS with simple messages
     *
     * @param request BatchSmsRequest containing list of messages
     * @return Response with batch processing result
     */
    @PostMapping("/batch/send-simple")
    public ResponseEntity<ObjectNode> sendBatchSmsSimple(@RequestBody BatchSmsRequest request) {
        log.info("Received request to send simple batch SMS with {} messages",
                request.getMessages() != null ? request.getMessages().size() : 0);

        ObjectNode result = smsUtil.sendBatchSmsSimple(request.getMessages());

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint for retrieving batch message statuses
     * GET /v1/sms/batch/{batchId}/messages
     *
     * @param batchId The batch ID from sendBatchSms response
     * @param limit Number of messages to retrieve (optional, default 100)
     * @return Response with batch message statuses
     */
    @GetMapping("/batch/{batchId}/messages")
    public ResponseEntity<JsonNode> retrieveBatchMessages(
            @PathVariable String batchId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        log.info("Received request to retrieve batch messages. BatchId: {}, Limit: {}", batchId, limit);

        JsonNode result = smsUtil.retrieveBatchMessages(batchId, limit);

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint for retrying failed batch messages
     * POST /v1/sms/batch/{batchId}/retry
     *
     * @param batchId The batch ID to retry
     * @return Response with retry result
     */
    @PostMapping("/batch/{batchId}/retry")
    public ResponseEntity<ObjectNode> retryBatch(@PathVariable String batchId) {
        log.info("Received request to retry batch. BatchId: {}", batchId);

        ObjectNode result = smsUtil.retryBatch(batchId);

        return ResponseEntity.ok(result);
    }
}

/**
 * Simple request class for SMS
 */
class SmsRequest {
    private String phoneNumber;
    private String message;
    private String language;
    
    // Getters and setters
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
}

/**
 * Request class for batch SMS
 */
class BatchSmsRequest {
    private List<Map<String, String>> messages;
    
    public List<Map<String, String>> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Map<String, String>> messages) {
        this.messages = messages;
    }
}