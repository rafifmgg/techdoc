package com.ocmsintranet.cronservice.dummyintegrations.emailsmtpserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/v1/dummy-email")
@Slf4j
public class DummyBrevoController {

    @Autowired
    private JavaMailSender javaMailSender;
    
    /**
     * Dummy endpoint for sending emails via API with SMTP backend
     * This is a development/testing endpoint only
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody Map<String, Object> request) {
        log.info("Received email request via DUMMY email endpoint");
        
        // Log the exact JSON payload received
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.info("Raw JSON payload received:\n{}", jsonPayload);
        } catch (Exception e) {
            log.error("Error serializing JSON payload: {}", e.getMessage());
        }
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if the request is in the new format (with 'from' instead of 'sender')
            if (request.containsKey("from") && !request.containsKey("sender")) {
                // Transform the request to the old format
                request = transformToOldFormat(request);
                log.info("Transformed request from new format to old format");
            }
            
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            
            // Extract sender
            Map<String, String> sender = (Map<String, String>) request.get("sender");
            String fromEmail = sender.get("email");
            String fromName = sender.get("name");
            
            log.debug("Setting email from: {} <{}>", fromName, fromEmail);
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            
            // Extract recipients
            List<Map<String, String>> recipients = (List<Map<String, String>>) request.get("to");
            if (recipients == null || recipients.isEmpty()) {
                throw new IllegalArgumentException("At least one recipient is required");
            }
            
            InternetAddress[] toAddresses = new InternetAddress[recipients.size()];
            for (int i = 0; i < recipients.size(); i++) {
                Map<String, String> recipient = recipients.get(i);
                toAddresses[i] = new InternetAddress(recipient.get("email"), recipient.get("name"));
            }
            
            log.debug("Setting email recipients: {}", recipients.size());
            helper.setTo(toAddresses);
            
            // Extract CC recipients if present
            if (request.containsKey("cc") && request.get("cc") != null) {
                List<Map<String, String>> ccRecipients = (List<Map<String, String>>) request.get("cc");
                if (!ccRecipients.isEmpty()) {
                    InternetAddress[] ccAddresses = new InternetAddress[ccRecipients.size()];
                    for (int i = 0; i < ccRecipients.size(); i++) {
                        Map<String, String> ccRecipient = ccRecipients.get(i);
                        ccAddresses[i] = new InternetAddress(ccRecipient.get("email"), ccRecipient.get("name"));
                    }
                    
                    log.debug("Setting CC recipients: {}", ccRecipients.size());
                    helper.setCc(ccAddresses);
                }
            }
            
            // Extract subject and content
            String subject = (String) request.get("subject");
            String htmlContent = (String) request.get("htmlContent");
            
            log.debug("Setting email subject: {}", subject);
            helper.setSubject(subject);
            
            log.debug("Setting HTML content with length: {}", htmlContent.length());
            
            // Clean up HTML content to avoid MIME parsing issues
            htmlContent = cleanHtmlContent(htmlContent);
            
            helper.setText(htmlContent, true);
            
            // Handle attachments if present
            if (request.containsKey("attachments") && request.get("attachments") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attachments = (List<Map<String, Object>>) request.get("attachments");
                for (Map<String, Object> attachment : attachments) {
                    // Check if using the new format (fileName) or old format (name)
                    String fileName = attachment.containsKey("fileName") ? 
                                    (String) attachment.get("fileName") : 
                                    (String) attachment.get("name");
                    
                    // Check if using the new format (fileContent) or old format (content)
                    String contentBase64 = attachment.containsKey("fileContent") ? 
                                         (String) attachment.get("fileContent") : 
                                         (String) attachment.get("content");
                    
                    byte[] fileContent = Base64.getDecoder().decode(contentBase64);
                    
                    ByteArrayDataSource dataSource = new ByteArrayDataSource(fileContent, "application/octet-stream");
                    helper.addAttachment(fileName, dataSource);
                    log.debug("Added attachment: {}", fileName);
                }
            }
            
            // Send the email
            log.info("Sending email via DUMMY SMTP server");
            javaMailSender.send(mimeMessage);
            
            log.info("Email sent successfully via DUMMY service");
            response.put("status", "success");
            response.put("message", "Email sent successfully via dummy SMTP server");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending email via DUMMY service: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Transform the new simplified email format to the old format
     */
    private Map<String, Object> transformToOldFormat(Map<String, Object> simpleRequest) {
        Map<String, Object> oldFormatRequest = new HashMap<>();
        
        // Copy subject and htmlContent as is
        oldFormatRequest.put("subject", simpleRequest.get("subject"));
        oldFormatRequest.put("htmlContent", simpleRequest.get("htmlContent"));
        
        // Transform 'from' to 'sender'
        String fromEmail = (String) simpleRequest.get("from");
        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", extractNameFromEmail(fromEmail));
        oldFormatRequest.put("sender", sender);
        
        // Transform 'to' to list of recipients
        String toEmail = (String) simpleRequest.get("to");
        List<Map<String, String>> toList = new java.util.ArrayList<>();
        Map<String, String> toRecipient = new HashMap<>();
        toRecipient.put("email", toEmail);
        toRecipient.put("name", extractNameFromEmail(toEmail));
        toList.add(toRecipient);
        oldFormatRequest.put("to", toList);
        
        // Transform 'cc' if present
        if (simpleRequest.containsKey("cc") && simpleRequest.get("cc") != null) {
            String ccEmail = (String) simpleRequest.get("cc");
            List<Map<String, String>> ccList = new java.util.ArrayList<>();
            Map<String, String> ccRecipient = new HashMap<>();
            ccRecipient.put("email", ccEmail);
            ccRecipient.put("name", extractNameFromEmail(ccEmail));
            ccList.add(ccRecipient);
            oldFormatRequest.put("cc", ccList);
        }
        
        // Transform attachments if present
        if (simpleRequest.containsKey("attachments") && simpleRequest.get("attachments") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attachments = (List<Map<String, Object>>) simpleRequest.get("attachments");
            List<Map<String, String>> oldFormatAttachments = new java.util.ArrayList<>();
            
            for (Map<String, Object> attachment : attachments) {
                Map<String, String> oldFormatAttachment = new HashMap<>();
                oldFormatAttachment.put("name", (String) attachment.get("fileName"));
                oldFormatAttachment.put("content", (String) attachment.get("fileContent"));
                oldFormatAttachments.add(oldFormatAttachment);
            }
            
            oldFormatRequest.put("attachments", oldFormatAttachments);
        }
        
        return oldFormatRequest;
    }
    
    /**
     * Clean HTML content to prevent MIME parsing issues
     * This method simplifies complex HTML structures that might cause problems
     * when being processed by email clients
     * 
     * @param html The original HTML content
     * @return Cleaned HTML content
     */
    private String cleanHtmlContent(String html) {
        if (html == null) {
            return "";
        }
        
        // Remove HTML comments
        html = html.replaceAll("<!--[\\s\\S]*?-->", "");
        
        // Normalize whitespace
        html = html.replaceAll("\\s+", " ");
        
        // Replace problematic characters in attributes
        html = html.replace("=\"UTF-8\"", "=\"utf-8\"");
        
        // Ensure proper DOCTYPE
        if (html.contains("<!DOCTYPE")) {
            // If it has a doctype, make sure it's simple
            html = html.replaceAll("<!DOCTYPE[^>]*>", "<!DOCTYPE html>");
        } else {
            // Add a simple doctype if missing
            html = "<!DOCTYPE html>\n" + html;
        }
        
        // Make sure there's only one <html> tag
        if (!html.contains("<html>") && !html.contains("<html ")) {
            html = "<html>" + html + "</html>";
        }
        
        // Make sure there's a <head> section
        if (!html.contains("<head>") && !html.contains("<head ")) {
            int htmlTagEnd = html.indexOf(">", html.indexOf("<html")) + 1;
            html = html.substring(0, htmlTagEnd) + "\n<head><meta charset=\"utf-8\"></head>\n" + html.substring(htmlTagEnd);
        }
        
        // Make sure there's a <body> section
        if (!html.contains("<body>") && !html.contains("<body ")) {
            int headEndIndex = html.indexOf("</head>") + 7;
            if (headEndIndex > 6) { // Make sure </head> was found
                html = html.substring(0, headEndIndex) + "\n<body>" + html.substring(headEndIndex) + "</body>\n";
            }
        }
        
        // Ensure proper nesting of elements
        html = ensureProperNesting(html);
        
        return html;
    }
    
    /**
     * Ensure proper nesting of HTML elements
     * This is a simplified approach to fix common nesting issues
     * 
     * @param html The HTML content to fix
     * @return HTML with improved nesting
     */
    private String ensureProperNesting(String html) {
        // Fix common nesting issues with tables
        html = html.replace("</tr><td>", "</tr>\n<tr><td>");
        html = html.replace("</td><tr>", "</td></tr>\n<tr>");
        
        // Fix issues with list items
        html = html.replace("</li><li>", "</li>\n<li>");
        
        // Fix div nesting
        html = html.replace("</div><div", "</div>\n<div");
        
        return html;
    }
    
    /**
     * Brevo API endpoint for sending emails
     * This mimics the actual Brevo API for testing purposes
     */
    @PostMapping(value = "/dummy-brevo-api/v3/smtp/email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBrevoEmail(@RequestBody Map<String, Object> emailRequest) {
        log.info("Received email request via Brevo API endpoint");
        
        // Log the exact JSON payload received
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(emailRequest);
            log.info("Raw Brevo API JSON payload received:\n{}", jsonPayload);
        } catch (Exception e) {
            log.error("Error serializing JSON payload: {}", e.getMessage());
        }
        
        try {
            // Check if the request is in the new format (with 'from' instead of 'sender')
            if (emailRequest.containsKey("from") && !emailRequest.containsKey("sender")) {
                // Transform the request to Brevo format
                Map<String, Object> transformedRequest = transformToBrevoFormat(emailRequest);
                
                // Log the transformed request
                ObjectMapper objectMapper = new ObjectMapper();
                String transformedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformedRequest);
                log.info("Transformed to Brevo format:\n{}", transformedJson);
                
                // Use the transformed request
                emailRequest = transformedRequest;
            }
            
            // Extract email details
            String subject = (String) emailRequest.get("subject");
            String htmlContent = (String) emailRequest.get("htmlContent");
            List<Map<String, Object>> to = (List<Map<String, Object>>) emailRequest.get("to");
            Map<String, Object> sender = (Map<String, Object>) emailRequest.get("sender");
            
            // Log email details
            log.info("Subject: {}", subject);
            log.info("From: {} <{}>", sender.get("name"), sender.get("email"));
            
            StringBuilder toBuilder = new StringBuilder();
            for (Map<String, Object> recipient : to) {
                toBuilder.append(recipient.get("name")).append(" <").append(recipient.get("email")).append(">, ");
            }
            log.info("To: {}", toBuilder.toString().replaceAll(", $", ""));
            
            // Process CC recipients if present
            List<Map<String, Object>> cc = null;
            if (emailRequest.containsKey("cc") && emailRequest.get("cc") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ccList = (List<Map<String, Object>>) emailRequest.get("cc");
                if (ccList != null && !ccList.isEmpty()) {
                    cc = ccList;
                    log.info("Found {} CC recipients", ccList.size());
                }
            }
            
            // Send email via SMTP
            sendEmailViaSmtp(subject, htmlContent, to, sender, cc);
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("messageId", "<" + System.currentTimeMillis() + ".brevo@dummy.local>");
            response.put("code", 201);
            response.put("message", "Email sent successfully via Brevo API");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending email via Brevo API: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Transform the simplified email format to Brevo API format
     */
    private Map<String, Object> transformToBrevoFormat(Map<String, Object> simpleRequest) {
        log.info("Transforming simple email format to Brevo format");
        Map<String, Object> brevoRequest = new HashMap<>();
        
        // Copy subject and htmlContent as is
        brevoRequest.put("subject", simpleRequest.get("subject"));
        brevoRequest.put("htmlContent", simpleRequest.get("htmlContent"));
        
        // Transform 'from' to 'sender'
        String fromEmail = (String) simpleRequest.get("from");
        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", extractNameFromEmail(fromEmail));
        brevoRequest.put("sender", sender);
        
        // Transform 'to' to list of recipients
        String toEmail = (String) simpleRequest.get("to");
        List<Map<String, String>> toList = new java.util.ArrayList<>();
        Map<String, String> toRecipient = new HashMap<>();
        toRecipient.put("email", toEmail);
        toRecipient.put("name", extractNameFromEmail(toEmail));
        toList.add(toRecipient);
        brevoRequest.put("to", toList);
        
        // Transform 'cc' if present
        if (simpleRequest.containsKey("cc") && simpleRequest.get("cc") != null) {
            String ccEmail = (String) simpleRequest.get("cc");
            List<Map<String, String>> ccList = new java.util.ArrayList<>();
            Map<String, String> ccRecipient = new HashMap<>();
            ccRecipient.put("email", ccEmail);
            ccRecipient.put("name", extractNameFromEmail(ccEmail));
            ccList.add(ccRecipient);
            brevoRequest.put("cc", ccList);
        }
        
        // Transform attachments if present
        if (simpleRequest.containsKey("attachments") && simpleRequest.get("attachments") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attachments = (List<Map<String, Object>>) simpleRequest.get("attachments");
            List<Map<String, String>> brevoAttachments = new java.util.ArrayList<>();
            
            for (Map<String, Object> attachment : attachments) {
                Map<String, String> brevoAttachment = new HashMap<>();
                brevoAttachment.put("name", (String) attachment.get("fileName"));
                brevoAttachment.put("content", (String) attachment.get("fileContent"));
                brevoAttachments.add(brevoAttachment);
            }
            
            brevoRequest.put("attachment", brevoAttachments);
        }
        
        return brevoRequest;
    }
    
    /**
     * Extract a name from an email address
     * For example, "john.doe@example.com" becomes "John Doe"
     */
    private String extractNameFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        
        // Extract the part before the @ symbol
        String localPart = email.split("@")[0];
        
        // Replace dots and underscores with spaces
        String nameWithSpaces = localPart.replace('.', ' ').replace('_', ' ');
        
        // Capitalize each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : nameWithSpaces.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Send email via SMTP without CC recipients
     */
    private void sendEmailViaSmtp(String subject, String htmlContent, List<Map<String, Object>> to, Map<String, Object> sender) throws Exception {
        // Call the overloaded method with null CC recipients
        sendEmailViaSmtp(subject, htmlContent, to, sender, null);
    }
    
    /**
     * Send email via SMTP with CC recipients
     */
    private void sendEmailViaSmtp(String subject, String htmlContent, List<Map<String, Object>> to, Map<String, Object> sender, List<Map<String, Object>> cc) throws Exception {
        log.info("Sending email via SMTP with subject: {}", subject);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
        
        // Set sender
        helper.setFrom(new InternetAddress((String) sender.get("email"), (String) sender.get("name")));
        
        // Set recipients
        InternetAddress[] toAddresses = new InternetAddress[to.size()];
        for (int i = 0; i < to.size(); i++) {
            Map<String, Object> recipient = to.get(i);
            toAddresses[i] = new InternetAddress((String) recipient.get("email"), (String) recipient.get("name"));
        }
        helper.setTo(toAddresses);
        
        // Set CC recipients if present
        if (cc != null && !cc.isEmpty()) {
            InternetAddress[] ccAddresses = new InternetAddress[cc.size()];
            for (int i = 0; i < cc.size(); i++) {
                Map<String, Object> ccRecipient = cc.get(i);
                ccAddresses[i] = new InternetAddress((String) ccRecipient.get("email"), (String) ccRecipient.get("name"));
            }
            helper.setCc(ccAddresses);
            log.info("Added {} CC recipients", cc.size());
        }
        
        // Set subject and content
        helper.setSubject(subject);
        
        // Clean up HTML content to avoid MIME parsing issues
        htmlContent = cleanHtmlContent(htmlContent);
        
        helper.setText(htmlContent, true);
        
        // Handle attachments if present
        if (sender.containsKey("attachment")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> attachments = (List<Map<String, String>>) sender.get("attachment");
            for (Map<String, String> attachment : attachments) {
                String name = attachment.get("name");
                String content = attachment.get("content");
                byte[] decodedContent = Base64.getDecoder().decode(content);
                helper.addAttachment(name, new ByteArrayDataSource(decodedContent, "application/octet-stream"));
                log.info("Added attachment: {}", name);
            }
        }
        
        // Send the email
        log.info("Sending email via DUMMY SMTP server from Brevo API endpoint");
        javaMailSender.send(mimeMessage);
        log.info("Email sent successfully via Brevo API endpoint");
    }
}
