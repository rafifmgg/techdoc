package com.ocmsintranet.cronservice.framework.common;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * Helper class for field validation operations
 * Provides utility methods for validating field lengths against database column limits
 */
@Slf4j
public class FieldValidationHelper {

    /**
     * Validate field lengths against database column size limits
     * Enhanced version with more detailed error logging
     * 
     * @param record The record map to validate
     * @param recordIdentifier An identifier for the record (e.g., notice number) for logging purposes
     * @param fieldSizeLimits Map of field names to their maximum allowed lengths
     * @return true if all fields are within database limits
     */
    public static boolean validateFieldLengths(Map<String, Object> record, String recordIdentifier, Map<String, Integer> fieldSizeLimits) {
        // Early return if either map is empty
        if (record == null || record.isEmpty() || fieldSizeLimits == null || fieldSizeLimits.isEmpty()) {
            return true;
        }
        
        boolean isValid = true;
        StringBuilder errorSummary = new StringBuilder();
        int errorCount = 0;
        
        // Process only fields that have defined size limits to avoid unnecessary iterations
        for (Map.Entry<String, Integer> limitEntry : fieldSizeLimits.entrySet()) {
            String fieldName = limitEntry.getKey();
            int maxLength = limitEntry.getValue();
            
            // Get the field value from the record
            Object value = record.get(fieldName);
            
            // Skip null values and non-string fields
            if (value == null || !(value instanceof String)) {
                continue;
            }
            
            String stringValue = (String) value;
            
            // Check if value exceeds the limit
            if (stringValue.length() > maxLength) {
                errorCount++;
                
                // Prepare truncated value for logging with clearer indication of truncation
                String displayValue;
                if (stringValue.length() > 30) {
                    displayValue = stringValue.substring(0, 27) + "...";
                } else {
                    displayValue = stringValue;
                }
                
                // Log each field error with more detailed information
                log.error("[FIELD_LENGTH_EXCEEDED] Field '{}' exceeds database limit for record {}: '{}' (length: {}, max: {})", 
                        fieldName, recordIdentifier, displayValue, stringValue.length(), maxLength);
                
                // Add to error summary with more detailed information
                if (errorSummary.length() > 0) {
                    errorSummary.append(", ");
                }
                errorSummary.append(fieldName).append(" (").append(stringValue.length()).append("/").append(maxLength).append(")");
                
                isValid = false;
            }
        }
        
        // After all fields are checked, log a summary if there were errors
        if (!isValid) {
            log.error("[VALIDATION_FAILED] Record {} has {} field(s) exceeding database limits: {}", 
                    recordIdentifier, errorCount, errorSummary);
            
            // Add a clear action message to indicate what happens next
            log.error("[ACTION_REQUIRED] Record {} will be suspended with TS-ROV status due to field length validation failure", 
                    recordIdentifier);
        }
        
        return isValid;
    }
    
    /**
     * Validate a single field value against a maximum length
     * 
     * @param fieldName The name of the field
     * @param fieldValue The value to validate
     * @param maxLength The maximum allowed length
     * @param recordIdentifier An identifier for the record for logging purposes
     * @return true if the field is within the length limit
     */
    public static boolean validateFieldLength(String fieldName, String fieldValue, int maxLength, String recordIdentifier) {
        // Early return for null values or invalid parameters
        if (fieldValue == null || maxLength < 0 || fieldName == null) {
            return true; // Null values are considered valid
        }
        
        // Check if value exceeds the limit
        if (fieldValue.length() > maxLength) {
            // Prepare truncated value for logging
            String truncatedValue = fieldValue.length() > 20 ? 
                fieldValue.substring(0, 20) + "..." : fieldValue;
            
            // Log the error
            log.error("[FIELD_LENGTH_EXCEEDED] Field '{}' exceeds database limit of {} characters for record {}: '{}' (length: {})", 
                    fieldName, maxLength, recordIdentifier, truncatedValue, fieldValue.length());
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate LTA record field lengths against database column size limits
     * 
     * @param record The LTA record map to validate
     * @param recordIdentifier An identifier for the record (e.g., notice number) for logging purposes
     * @return true if all fields are within database limits
     */
    public static boolean validateLtaFieldLengths(Map<String, Object> record, String recordIdentifier) {
        return validateFieldLengths(record, recordIdentifier, DatabaseFieldSizeLimits.getLtaFieldSizeLimits());
    }
    
    /**
     * Validate MHA record field lengths against database column size limits
     * 
     * @param record The MHA record map to validate
     * @param recordIdentifier An identifier for the record (e.g., UIN) for logging purposes
     * @return true if all fields are within database limits
     */
    public static boolean validateMhaFieldLengths(Map<String, Object> record, String recordIdentifier) {
        return validateFieldLengths(record, recordIdentifier, DatabaseFieldSizeLimits.getMhaFieldSizeLimits());
    }
}
