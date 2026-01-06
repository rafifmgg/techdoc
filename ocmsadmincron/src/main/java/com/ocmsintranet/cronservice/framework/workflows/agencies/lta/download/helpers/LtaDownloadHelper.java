package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ocmsintranet.cronservice.framework.common.FieldValidationHelper;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.regex.Pattern;


/**
 * Main orchestrator for LTA Download workflow.
 * Handles file parsing, record logging, and delegates processing to specialized components.
 * 
 * REFACTORED VERSION - Focused on orchestration only
 */
@Slf4j
@Component
public class LtaDownloadHelper {

    private final LtaResponseFileParser responseFileParser;
    private final LtaRecordValidator recordValidator;
    private final LtaProcessingOrchestrator processingOrchestrator;

    @Value("${notification.email.lta.error:ocms-support@ura.gov.sg}")
    private String errorNotificationEmail;
    
    // Pattern for VIP vehicle detection
    private static final Pattern VIP_VEHICLE_PATTERN = Pattern.compile("^V[A-Z0-9]+$");

    public LtaDownloadHelper(
            LtaResponseFileParser responseFileParser,
            LtaRecordValidator recordValidator,
            LtaProcessingOrchestrator processingOrchestrator) {
        this.responseFileParser = responseFileParser;
        this.recordValidator = recordValidator;
        this.processingOrchestrator = processingOrchestrator;
    }

    /**
     * Process an LTA response file
     *
     * @param fileContent The decrypted file content
     * @return true if parsing was successful, false if there were errors
     */
    public boolean processLtaResponseFile(byte[] fileContent) {
        log.info("Processing LTA response file");
        
        try {
            // Convert byte array to string
            String content = new String(fileContent, StandardCharsets.UTF_8);
            
            // Parse the file content
            LtaResponseFileParser.ParseResult parseResult = responseFileParser.parseFile(content);
            
            // Check for file integrity errors
            if (parseResult == null) {
                log.error("Failed to parse LTA response file - parse result is null");
                return false;
            }
            
            // Check for file integrity errors (A, B, C)
            if (parseResult.hasIntegrityError()) {
                String errorCode = parseResult.getErrorCode();
                String errorMessage = parseResult.getErrorMessage();
                LtaResponseFileParser.FileIntegrityError errorType = parseResult.getIntegrityError();
                
                log.error("File integrity error detected: Code '{}' - {} ({})", 
                        errorCode, errorType.getDescription(), errorMessage);
                
                // Return false to indicate processing failure due to integrity error
                return false;
            }
            
            // If we get here, the file has passed integrity checks
            if (parseResult.getRecords().isEmpty()) {
                log.warn("LTA response file has no records");
                return true; // File is valid but empty
            }
            
            log.info("Successfully parsed LTA response file: {} records", parseResult.getRecords().size());
            log.info("Header data: {}", parseResult.getHeaderData());
            log.info("Trailer data: {}", parseResult.getTrailerData());
            
            // Log each record
            for (Map<String, Object> record : parseResult.getRecords()) {
                logRecord(record);
            }
            
            // Call main processing method
            processLtaResponseRecords(parseResult);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error processing LTA response file", e);
            return false;
        }
    }
    
    /**
     * Log a single record from the LTA response file
     *
     * @param record The parsed record data
     */
    private void logRecord(Map record) {         
        try {             
            // Extract key fields             
            String vehicleNumber = (String) record.get("vehicleNumber");             
            String errorCode = (String) record.get("errorCode");             
            String ownerIdType = (String) record.get("ownerIdType");             
            String ownerId = (String) record.get("ownerId");             
            String ownerName = (String) record.get("ownerName");             
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");             
            String chassisNumber = (String) record.get("chassisNumber");             
            String vehicleMake = (String) record.get("vehicleMake");             
            String vehicleColor = (String) record.get("vehicleColor");             
            String roadTaxExpiryDate = (String) record.get("roadTaxExpiryDate");
            
            // ⭐ ADD DIPLOMATIC FLAG EXTRACTION
            String diplomaticFlag = (String) record.get("diplomaticFlag");
                              
            // Log detailed information             
            log.info("LTA Record Details:");             
            log.info("  - Vehicle: {}", vehicleNumber);             
            log.info("  - Chassis: {}", chassisNumber);             
            log.info("  - Make: {}", vehicleMake);             
            log.info("  - Color: {}", vehicleColor);             
            log.info("  - Road Tax Expiry: {}", roadTaxExpiryDate);
            
            // ⭐ ADD DIPLOMATIC FLAG TO LOGGING
            log.info("  - Diplomatic Flag: '{}'", diplomaticFlag != null ? diplomaticFlag : "None");
                              
            // Safe error code parsing             
            String errorDescription = "N/A";             
            if (errorCode != null && !errorCode.trim().isEmpty()) {
                // Handle alphabetic error codes (A, B, C)
                if ("A".equals(errorCode) || "B".equals(errorCode) || "C".equals(errorCode)) {
                    errorDescription = "File integrity error " + errorCode;
                } else {
                    try {                     
                        int errorCodeInt = Integer.parseInt(errorCode.trim());                     
                        errorDescription = getErrorDescription(errorCodeInt);                 
                    } catch (NumberFormatException e) {                     
                        log.warn("Invalid error code format: {}", errorCode);                     
                        errorDescription = "Invalid format";                 
                    }
                }             
            }                          
            
            log.info("  - Error Code: {} ({})",                      
                    errorCode != null && !errorCode.trim().isEmpty() ? errorCode : "None",                     
                    errorDescription);             
            log.info("  - Owner ID Type: {}", ownerIdType);             
            log.info("  - Owner ID: {}", ownerId);             
            log.info("  - Owner Name: {}", ownerName != null ? ownerName : "Not provided");             
            log.info("  - Offence Notice: {}", offenceNoticeNumber);                      
                          
        } catch (Exception e) {             
            log.error("Error logging LTA record", e);         
        }     
    }
    
    /**
     * Process the parsed LTA response file and update database records
     *
     * @param parseResult The parsed file result
     */
    private void processLtaResponseRecords(LtaResponseFileParser.ParseResult parseResult) {
        log.info("Processing LTA response file with {} records", parseResult.getRecords().size());
        
        for (Map<String, Object> record : parseResult.getRecords()) {
            try {
                processRecord(record);
            } catch (Exception e) {
                log.error("Error processing LTA record: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Process a single LTA record - Main entry point for processing logic
     * 
     * PROCESSING FLOW SUMMARY:
     * ========================
     * 1. Validate record (offence notice, vehicle number, owner ID for success cases)
     * 2. Check for LTA error codes 1-4 → If found, apply TS-ROV status and END
     * 3. Update notice with owner data from LTA response (ocms_offence_notice_detail and ocms_offence_notice_owner_driver)
     * 4. Detect vehicle type (Diplomat, VIP, Standard)
     * 5. For Diplomat/VIP vehicles → Update database and set to RD1, then END
     * 6. For Standard vehicles:
     *    a. Check if notice is suspended
     *    b. If suspended, check if under TS-HST → If not under TS-HST, END
     *    c. Check if offender is in exclusion list → If yes, set to RD1 and END
     *    d. Check if ID type is passport → If yes, query passport data, set to RD1 and END
     *    e. If not passport, call Snowflake function:
     *       - Check for mobile/email contact info
     *       - If found → Update stage to ENA
     *       - If not found → Set to RD1
     * 
     * @param record The LTA record to process
     */
    private void processRecord(Map<String, Object> record) {
        try {
            // Step 1: Validate record before processing
            if (!recordValidator.validateRecord(record)) {
                log.error("Record validation failed, skipping processing");
                return;
            }

            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            String errorCode = (String) record.get("errorCode");
            String vehicleNumber = (String) record.get("vehicleNumber");
            
            log.info("Processing record for vehicle {} with notice {}", vehicleNumber, offenceNoticeNumber);
            
            // Step 2: Check for error codes 1-4 → Apply TS-ROV and END
            if (hasErrorCode(errorCode)) {
                try {
                    int errorCodeInt = Integer.parseInt(errorCode.trim());
                    log.info("Error code {} found for vehicle {}: {}", errorCode, vehicleNumber, getErrorDescription(errorCodeInt));
                    
                    // Special handling for Error Code 3: Patch tables first, then TS-ROV
                    if (errorCode.equals("3")) {
                        log.info("Error code 3 (Deregistered Vehicle) - patching owner data before applying TS-ROV");
                        processingOrchestrator.updateNoticeWithOwnerData(record, offenceNoticeNumber);
                    }
                    // Special handling for Error Codes 1, 2, and 4: Update ownerdriver table with LTA processing date and error code
                    else if (errorCode.equals("1") || errorCode.equals("2") || errorCode.equals("4")) {
                        log.info("Error code {} - updating ownerdriver table with LTA processing date and error code", errorCode);
                        processingOrchestrator.updateOffenceNoticeLtaDatetimeErrorCode(offenceNoticeNumber, errorCode);
                    }
                    
                    // Apply TS-ROV for all error codes (1, 2, 3, 4)
                    processingOrchestrator.applyTsRovStatus(record);
                    
                    // END processing for all error codes (1, 2, 3, 4)
                    return;
                    
                } catch (NumberFormatException e) {
                    log.error("Failed to parse numeric error code: {}", errorCode);
                    return;
                }
            }
            
            // Step 3: Update notice with owner data from LTA response
            log.info("Updating notice with owner data for vehicle {} with notice {}", vehicleNumber, offenceNoticeNumber);
            processingOrchestrator.updateNoticeWithOwnerData(record, offenceNoticeNumber);
            
            // Step 4: Detect vehicle type using diplomaticFlag from LTA response
            String diplomaticFlag = (String) record.get("diplomaticFlag");
            log.info("Vehicle {} with notice {} has diplomatic flag: {}", vehicleNumber, offenceNoticeNumber, diplomaticFlag);
            String vehicleType = detectVehicleType(vehicleNumber, diplomaticFlag);
            
            // Step 5: Process Diplomat/VIP vehicles → RD1 and END
            if ("D".equals(vehicleType)) {
                log.info("Diplomat vehicle detected: {} (diplomatic flag: {})", vehicleNumber, diplomaticFlag);
                processingOrchestrator.processDiplomatVehicle(record);
                return;
            } else if ("V".equals(vehicleType)) {
                log.info("VIP vehicle detected: {} (diplomatic flag: {})", vehicleNumber, diplomaticFlag);
                processingOrchestrator.processVipVehicle(record);
                return;
            }
            
            // Step 6: Standard vehicle processing
            log.info("Standard vehicle processing for {}", vehicleNumber);
            processingOrchestrator.processStandardVehicle(record);
            
        } catch (Exception e) {
            log.error("Error processing record: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if record has error code 1-4
     * Note: Error codes A, B, C are file-level errors and should not reach this point
     */
    private boolean hasErrorCode(String errorCode) {
        if (errorCode == null || errorCode.trim().isEmpty() || "0".equals(errorCode.trim())) {
            return false;
        }
        
        // Skip alphabetic error codes (A, B, C) - these are file integrity errors
        if ("A".equals(errorCode) || "B".equals(errorCode) || "C".equals(errorCode)) {
            log.warn("File integrity error code {} should have been handled at file level", errorCode);
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
    
    /**
     * Detect vehicle registration type based on diplomaticFlag from LTA response and vehicle number pattern for VIP
     *
     * @param vehicleNumber The vehicle registration number (used for VIP detection)
     * @param diplomaticFlag The diplomatic flag from LTA response ('Y' or 'N')
     * @return "D" for Diplomat, "V" for VIP, "S" for Standard
     */
    private String detectVehicleType(String vehicleNumber, String diplomaticFlag) {
        // Check for diplomatic vehicles using diplomaticFlag from LTA response
        if (diplomaticFlag != null && "Y".equals(diplomaticFlag.trim())) {
            return "D";
        }
        
        // Check for VIP vehicles using vehicle number pattern
        if (vehicleNumber != null && !vehicleNumber.isEmpty() && VIP_VEHICLE_PATTERN.matcher(vehicleNumber).matches()) {
            return "V";
        }
        
        // Default to standard vehicle
        return "S";
    }
    
    /**
     * Get a description for an LTA error code
     * 
     * @param errorCode The LTA error code (1-4)
     * @return A human-readable description of the error
     */
    private String getErrorDescription(int errorCode) {
        switch (errorCode) {
            case 1:
                return "Reserved";
            case 2:
                return "Record Not Found";
            case 3:
                return "Deregistered Vehicle";
            case 4:
                return "Invalid Offence Date";
            default:
                return "Unknown error code";
        }
    }
}
