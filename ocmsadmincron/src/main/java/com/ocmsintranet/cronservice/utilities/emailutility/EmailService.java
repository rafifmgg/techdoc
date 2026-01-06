package com.ocmsintranet.cronservice.utilities.emailutility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
@Slf4j
@org.springframework.context.annotation.Lazy
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${email.from.address:DoNotReply@ura.gov.sg}")
    private String defaultFromEmail;
    
    @Value("${email.from.name:URA}")
    private String defaultFromName;
    
    @Value("${email.enableSmtp:true}")
    private boolean enableSmtp;
    

    
    /**
     * Sends an HTML email with optional attachments using JavaMailSender
     * 
     * @param request The email request containing all necessary information
     * @return true if email was sent successfully
     */
    public boolean sendEmail(EmailRequest request) {
        if (!enableSmtp) {
            log.warn("SMTP is disabled. Email was not sent");
            return false;
        }
        
        // Set default from address if not provided
        if (request.getFrom() == null || request.getFrom().isEmpty()) {
            request.setFrom(defaultFromEmail);
            log.debug("Using default from address");
        }
        
        // Validate recipient
        if (request.getTo() == null || request.getTo().isEmpty()) {
            log.error("Email recipient is required");
            return false;
        }
        
        log.info("Preparing to send email");
        
        try {
            // Create a new MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            
            // Set email details
            helper.setText(applyDefaultStyle(request.getHtmlContent()), true);
            helper.setTo(InternetAddress.parse(request.getTo()));
            helper.setSubject(request.getSubject());
            helper.setFrom(request.getFrom(), defaultFromName);
            
            // Set optional CC field
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(InternetAddress.parse(request.getCc()));
            }

            // Add attachments if present
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                for (EmailRequest.Attachment attachment : request.getAttachments()) {
                    // Determine content type based on file extension or default to octet-stream
                    String contentType = determineContentType(attachment.getFileName());
                    ByteArrayDataSource dataSource = new ByteArrayDataSource(
                            attachment.getFileContent(), 
                            contentType
                    );
                    helper.addAttachment(attachment.getFileName(), dataSource);
                }
            }
            
            log.info("Sending email via JavaMailSender");
            
            // Send the email
            mailSender.send(mimeMessage);
            
            log.info("Email sent successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return false;
        }
    }
    

    
    /**
     * Applies default styling to HTML content
     */
    private String applyDefaultStyle(String htmlContent) {
        String defaultStyle = "figure.table {width: 100vw;border-spacing: 0px;border-collapse: collapse;}" +
                             "table {width: 100vw;border: 1px solid black;}" +
                             "td {border: 1px solid black;padding: 1.75rem;}" +
                             "p {margin:0;line-height:1.2;}";
        
        if (htmlContent.contains("<style>")) {
            // Append new styles to existing <style> tag
            return htmlContent.replaceFirst("<style>", "<style>" + defaultStyle);
        } else {
            // Prepend new styles if no <style> tag is present
            return "<style>" + defaultStyle + "</style>" + htmlContent;
        }
    }
    
    /**
     * Determines the MIME content type based on file extension
     * 
     * @param fileName The name of the file
     * @return The appropriate MIME content type
     */
    private String determineContentType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerFileName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }
}