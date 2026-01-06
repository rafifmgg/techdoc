package com.ocmsintranet.apiservice.utilities.emailutility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Helper class for API error email notification operations
 * that leverages EmailService's built-in retry capability
 */
@Slf4j
@Component
public class ApiErrorEmailHelper {
    
    private final EmailService emailService;
    private final ResourceLoader resourceLoader;
    private final ExecutorService executorService;
    
    // Template path - this should be in src/main/resources/templates/
    private static final String TEMPLATE_PATH = "templates/api-error-template.html";
    
    @Value("${email.admin}")
    private String adminEmail;
    
    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;
    
    @Value("${server.hostname:unknown}")
    private String serverHostname;
    
    public ApiErrorEmailHelper(EmailService emailService, ResourceLoader resourceLoader, ExecutorService executorService) {
        this.emailService = emailService;
        this.resourceLoader = resourceLoader;
        this.executorService = executorService;
    }
    
    /**
     * Sends an error notification email to administrators asynchronously
     * EmailService will handle retry logic internally
     * 
     * @param transactionId The transaction ID or request ID
     * @param requestType The HTTP method (GET, POST, etc.)
     * @param requestPath The API endpoint path
     * @param statusCode The HTTP status code
     * @param errorMessage The error message to include in the email
     * @return CompletableFuture<Boolean> indicating if email was sent successfully
     */
    public CompletableFuture<Boolean> sendErrorNotificationEmailAsync(String transactionId, 
                                                                    String requestType,
                                                                    String requestPath,
                                                                    int statusCode,
                                                                    String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            return sendErrorNotificationEmail(transactionId, requestType, requestPath, statusCode, errorMessage);
        }, executorService);
    }
    
    /**
     * Sends an error notification email to administrators
     * EmailService will handle retry logic internally
     * 
     * @param transactionId The transaction ID or request ID
     * @param requestType The HTTP method (GET, POST, etc.)
     * @param requestPath The API endpoint path
     * @param statusCode The HTTP status code
     * @param errorMessage The error message to include in the email
     * @return true if email was sent successfully
     */
    public boolean sendErrorNotificationEmail(String transactionId, 
                                            String requestType,
                                            String requestPath,
                                            int statusCode,
                                            String errorMessage) {
        try {
            log.info("Sending error notification email to: {}", adminEmail);
            
            // Create email request
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(adminEmail);
            emailRequest.setSubject("OCMS API Service Error - " + transactionId);
            
            // Prepare template data
            Map<String, Object> templateData = prepareTemplateData(transactionId, requestType, requestPath, statusCode, errorMessage);
            
            // Apply template using Mustache
            String htmlContent = applyMustacheTemplate(templateData);
            emailRequest.setHtmlContent(htmlContent);
            
            // Send email - EmailService handles retry internally
            boolean sent = emailService.sendEmail(emailRequest);
            if (sent) {
                log.info("Error notification email sent successfully");
            } else {
                log.error("Failed to send error notification email after retries");
            }
            
            return sent;
        } catch (Exception e) {
            // Log but don't throw - email sending should not affect main processing
            log.error("Error sending notification email: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Prepare data for the Mustache template
     */
    private Map<String, Object> prepareTemplateData(String transactionId, 
                                                  String requestType,
                                                  String requestPath,
                                                  int statusCode,
                                                  String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        
        // Basic request information
        data.put("transactionId", transactionId);
        data.put("requestType", requestType);
        data.put("requestPath", requestPath);
        data.put("statusCode", statusCode);
        
        // Error information - don't escape HTML to allow formatting
        data.put("errorMessage", errorMessage);
        
        // System information
        data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        data.put("environment", activeProfile);
        data.put("serverName", serverHostname);
        
        return data;
    }
    
    /**
     * Apply template to data using a simplified approach
     */
    private String applyMustacheTemplate(Map<String, Object> data) {
        try {
            // Load template from resources
            Resource resource = resourceLoader.getResource("classpath:" + TEMPLATE_PATH);
            
            if (!resource.exists()) {
                log.error("Template not found: {}", TEMPLATE_PATH);
                return createFallbackHtml(data);
            }
            
            // Read template content
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                String template = FileCopyUtils.copyToString(reader);
                
                // Simple template processing - replace {{placeholders}} with values
                String result = template;
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    
                    // Special handling for errorMessage - don't escape HTML to allow formatting
                    if ("errorMessage".equals(entry.getKey())) {
                        result = result.replace(placeholder, value);
                    } else {
                        // Escape HTML for other fields
                        result = result.replace(placeholder, escapeHtml(value));
                    }
                }
                
                // Sanitize the final HTML to ensure it's well-formed
                return sanitizeHtml(result);
            }
        } catch (Exception e) {
            log.error("Error applying template: {}", e.getMessage(), e);
            return createFallbackHtml(data);
        }
    }
    
    /**
     * Sanitize HTML to ensure clean, well-formed email content
     * This helps prevent MIME parsing issues in email clients
     * 
     * @param html The HTML content to sanitize
     * @return Clean, well-formed HTML
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }
        
        // Ensure there's a basic HTML structure
        if (!html.contains("<html")) {
            html = "<html><body>" + html + "</body></html>";
        }
        
        // Ensure there are no unclosed tags
        if (html.contains("<body>") && !html.contains("</body>")) {
            html = html + "</body></html>";
        }
        
        // Fix common HTML issues in tables
        if (html.contains("<table>") && !html.contains("</table>")) {
            int tableIndex = html.lastIndexOf("<table>");
            int bodyCloseIndex = html.lastIndexOf("</body>");
            
            if (tableIndex > -1 && bodyCloseIndex > -1 && tableIndex < bodyCloseIndex) {
                html = html.substring(0, bodyCloseIndex) + "</table>" + html.substring(bodyCloseIndex);
            }
        }
        
        // Remove HTML comments
        html = html.replaceAll("<!--[\\s\\S]*?-->", "");
        
        // Fix common HTML issues
        html = html.replace("</tr><td>", "</tr>\n<tr><td>");
        html = html.replace("</td><tr>", "</td></tr>\n<tr>");
        html = html.replace("</div><div", "</div>\n<div");
        
        // Remove excessive whitespace
        html = html.replaceAll("\\s{2,}", " ");
        
        // Ensure all tags are properly closed
        html = ensureProperTagClosure(html);
        
        return html;
    }
    
    /**
     * Properly escape HTML content to prevent issues with JSON parsing and email rendering
     * More comprehensive than simple < and > replacement
     * 
     * @param input The string to escape
     * @return HTML escaped string
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        // Replace special characters with their HTML entities
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   // Replace JSON special characters
                   .replace("\\", "\\\\")
                   .replace("/", "/");
    }
    
    private String ensureProperTagClosure(String html) {
        // Common self-closing tags that might be missing the closing slash
        String[] selfClosingTags = {"img", "br", "hr", "input", "meta", "link"};
        
        for (String tag : selfClosingTags) {
            // Find tags without proper closure and fix them
            // Pattern: <tag ...> (without /> at the end)
            String openPattern = "<" + tag + "([^>]*[^/])>";
            String replacement = "<" + tag + "$1 />";
            html = html.replaceAll(openPattern, replacement);
        }
        
        return html;
    }
    
    /**
     * Create fallback HTML in case template loading fails
     */
    private String createFallbackHtml(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>OCMS API Service Error</h2>");
        
        // Transaction details
        html.append("<h3>Transaction Details</h3>");
        html.append("<table border='1' cellpadding='5'>");
        html.append("<tr><th>Field</th><th>Value</th></tr>");
        html.append("<tr><td>Transaction ID</td><td>").append(data.get("transactionId")).append("</td></tr>");
        html.append("<tr><td>Request Type</td><td>").append(data.get("requestType")).append("</td></tr>");
        html.append("<tr><td>Request Path</td><td>").append(data.get("requestPath")).append("</td></tr>");
        html.append("<tr><td>Status Code</td><td>").append(data.get("statusCode")).append("</td></tr>");
        html.append("</table>");
        
        // Error details
        html.append("<h3>Error Details</h3>");
        html.append("<p style='color: red;'>").append(data.get("errorMessage")).append("</p>");
        
        // System information
        html.append("<h3>System Information</h3>");
        html.append("<p>Timestamp: ").append(data.get("timestamp")).append("</p>");
        html.append("<p>Environment: ").append(data.get("environment")).append("</p>");
        html.append("<p>Server: ").append(data.get("serverName")).append("</p>");
        
        return html.toString();
    }
}
