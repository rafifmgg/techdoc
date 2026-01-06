package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validator component for LTA records
 * Handles validation logic for different record types and error scenarios
 */
@Slf4j
@Component
public class LtaRecordValidator {

    /**
     * Validate an LTA record before processing
     * For error codes 1-4: Only requires offenceNoticeNumber and vehicleNumber
     * For success cases: Also requires ownerId
     *
     * @param record The LTA record to validate
     * @return true if record is valid for processing
     */
    public boolean validateRecord(Map<String, Object> record) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        String vehicleNumber = (String) record.get("vehicleNumber");
        String ownerId = (String) record.get("ownerId");
        String errorCode = (String) record.get("errorCode");
        
        if (StringUtils.isEmpty(offenceNoticeNumber)) {
            log.error("Missing offence notice number in record");
            return false;
        }
        
        if (StringUtils.isEmpty(vehicleNumber)) {
            log.error("Missing vehicle number in record for notice {}", offenceNoticeNumber);
            return false;
        }
        
        // Only require ownerId for success cases (error code 0 or empty)
        // For error codes 1-4, ownerId is expected to be empty per LTA specification
        if (isSuccessCase(errorCode) && StringUtils.isEmpty(ownerId)) {
            log.error("Missing owner ID in record for notice {} (success case)", offenceNoticeNumber);
            return false;
        }

        log.debug("Record validation passed for notice {}", offenceNoticeNumber);
        return true;
    }
    
    /**
     * Determine if this is a success case (error code 0 or empty)
     *
     * @param errorCode The error code from LTA response
     * @return true if this represents a successful vehicle lookup
     */
    public boolean isSuccessCase(String errorCode) {
        if (StringUtils.isEmpty(errorCode)) {
            return true; // Assume success if no error code provided
        }
        return "0".equals(errorCode.trim());
    }
    
    /**
     * Check if record has a valid error code (1-4)
     * Note: Error codes A, B, C are file-level errors handled separately
     *
     * @param errorCode The error code to check
     * @return true if error code is 1, 2, 3, or 4
     */
    public boolean hasValidErrorCode(String errorCode) {
        if (StringUtils.isEmpty(errorCode) || "0".equals(errorCode.trim())) {
            return false;
        }
        
        // Skip alphabetic error codes (A, B, C) - these are file integrity errors
        if ("A".equals(errorCode) || "B".equals(errorCode) || "C".equals(errorCode)) {
            return false;
        }
        
        try {
            int errorCodeInt = Integer.parseInt(errorCode.trim());
            return errorCodeInt >= 1 && errorCodeInt <= 4;
        } catch (NumberFormatException e) {
            log.warn("Invalid error code format: {}", errorCode);
            return false;
        }
    }
}