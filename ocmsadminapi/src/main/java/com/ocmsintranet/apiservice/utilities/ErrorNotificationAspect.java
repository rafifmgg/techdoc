package com.ocmsintranet.apiservice.utilities;

import com.ocmsintranet.apiservice.utilities.emailutility.ApiErrorEmailHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Aspect that monitors for exceptions and sends email notifications for critical errors.
 * This works alongside the existing GlobalExceptionHandler without replacing it.
 */
@Aspect
@Component
@Slf4j
public class ErrorNotificationAspect {

    private final ApiErrorEmailHelper emailHelper;
    
    @Value("${email.error.notification.enabled:true}")
    private boolean errorNotificationEnabled;
    
    @Value("${email.error.notification.threshold:500}")
    private int errorNotificationThreshold;
    
    public ErrorNotificationAspect(ApiErrorEmailHelper emailHelper) {
        this.emailHelper = emailHelper;
    }
    
    /**
     * Captures exceptions thrown by controllers and sends email notifications
     * for critical errors
     */
    @AfterThrowing(
        pointcut = "execution(* com.ocmsintranet.apiservice..*Controller.*(..))",
        throwing = "ex"
    )
    public void notifyOnControllerException(JoinPoint joinPoint, Exception ex) {
        // Only proceed if email notification is enabled
        if (!errorNotificationEnabled) {
            return;
        }
        
        try {
            // Generate a unique transaction ID for this error
            String transactionId = UUID.randomUUID().toString();
            
            // Get the current request if available
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("No request attributes available, cannot send detailed error notification");
                return;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String requestType = request.getMethod();
            String requestPath = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null) {
                requestPath += "?" + queryString;
            }
            
            // Determine if this exception should trigger an email notification
            // We'll consider all exceptions from controllers as potentially severe
            int statusCode = determineStatusCode(ex);
            
            if (statusCode >= errorNotificationThreshold) {
                // Format detailed error message for email
                StringBuilder errorDetails = new StringBuilder();
                errorDetails.append("<p><strong>Error Type:</strong> ")
                          .append(ex.getClass().getName())
                          .append("</p>");
                errorDetails.append("<p><strong>Error Message:</strong> ")
                          .append(ex.getMessage() != null ? ex.getMessage() : "No message")
                          .append("</p>");
                errorDetails.append("<p><strong>Controller:</strong> ")
                          .append(joinPoint.getSignature().getDeclaringTypeName())
                          .append("</p>");
                errorDetails.append("<p><strong>Method:</strong> ")
                          .append(joinPoint.getSignature().getName())
                          .append("</p>");
                
                // Add stack trace with formatting
                errorDetails.append("<p><strong>Stack Trace:</strong></p>");
                errorDetails.append("<pre style='background-color: #f8f8f8; padding: 10px; overflow-x: auto;'>");
                for (StackTraceElement element : ex.getStackTrace()) {
                    // Only include relevant stack trace elements (our application code)
                    if (element.getClassName().contains("com.ocmsintranet")) {
                        errorDetails.append(element.toString()).append("\n");
                    }
                }
                errorDetails.append("</pre>");
                
                // Log the error with transaction ID for correlation
                log.error("Sending error notification email for exception [{}] {}: {} - {}", 
                        transactionId, requestType, requestPath, ex.getMessage(), ex);
                
                // Send email notification asynchronously
                emailHelper.sendErrorNotificationEmailAsync(
                    transactionId, 
                    requestType, 
                    requestPath, 
                    statusCode, 
                    errorDetails.toString()
                );
            }
        } catch (Exception e) {
            // Never let the aspect itself throw exceptions
            log.error("Error in exception notification aspect: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Determine an appropriate HTTP status code based on the exception type
     */
    private int determineStatusCode(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST.value();
        } else {
            // Default to internal server error for most exceptions
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }
}
