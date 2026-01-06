package com.ocmsintranet.apiservice.utilities.emailutility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/email")
@Slf4j
public class EmailController {
    @Autowired
    private EmailService emailService;
    
    /**
     * Endpoint for sending HTML emails with optional attachments
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody EmailRequest request) {
        log.info("Received email request to: {}", request.getTo());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = emailService.sendEmail(request);
            
            if (success) {
                log.info("Email to {} sent successfully", request.getTo());
                response.put("status", "success");
                response.put("message", "Email sent successfully");
                return ResponseEntity.ok(response);
            } else {
                log.warn("Email to {} failed to send", request.getTo());
                response.put("status", "error");
                response.put("message", "Failed to send email");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Exception while sending email: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
