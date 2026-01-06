package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for LTA fixed-width response files according to LTA Interface Specification v37.0 - Appendix F
 * VRLS Enquiry File (Results) from LTA to URA
 * Record Length: 691 characters
 * 
 * ENHANCED VERSION: Added detailed logging for diplomatic flag and other key fields
 * ENHANCED VERSION 2: Added file integrity checks for missing header, missing trailer, and record count mismatch
 */
@Slf4j
@Component
public class LtaResponseFileParser {

    // Constants for record types
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    
    // Expected record length for Appendix F format
    private static final int EXPECTED_RECORD_LENGTH = 691;

    /**
     * Parse the LTA response file content
     *
     * @param content The file content as a string
     * @return ParseResult containing header data, records, and trailer data
     */
    public ParseResult parseFile(String content) {
        log.info("Parsing LTA response file");
        
        try {
            String[] lines = content.split("\\r?\\n");
            
            Map<String, Object> headerData = null;
            List<Map<String, Object>> records = new ArrayList<>();
            Map<String, Object> trailerData = null;
            
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                
                // Validate record length
                if (line.length() != EXPECTED_RECORD_LENGTH) {
                    log.warn("Record length mismatch: expected {}, actual {} for line: {}", 
                            EXPECTED_RECORD_LENGTH, line.length(), line.substring(0, Math.min(50, line.length())));
                }
                
                String recordType = line.substring(0, 1);
                
                switch (recordType) {
                    case HEADER_RECORD_TYPE:
                        headerData = parseHeaderRecord(line);
                        break;
                    case DETAIL_RECORD_TYPE:
                        Map<String, Object> record = parseDetailRecord(line);
                        if (record != null) {
                            records.add(record);
                        }
                        break;
                    case TRAILER_RECORD_TYPE:
                        trailerData = parseTrailerRecord(line);
                        break;
                    default:
                        log.warn("Unknown record type: {}", recordType);
                }
            }
            
            // Check for special error codes A, B, C in the detail records
            String specialErrorCode = null;
            for (Map<String, Object> record : records) {
                if (record.containsKey("specialErrorCode")) {
                    specialErrorCode = (String) record.get("specialErrorCode");
                    log.warn("Special error code detected in detail record: {}", specialErrorCode);
                    break;
                }
            }
            
            // If a special error code was found, return it as an integrity error
            if (specialErrorCode != null) {
                FileIntegrityError error = FileIntegrityError.getByCode(specialErrorCode);
                if (error != null) {
                    log.error("File integrity error detected: {} - {}", error.getCode(), error.getDescription());
                    return new ParseResult(headerData, records, trailerData, error, 
                            "Error code " + specialErrorCode + " detected in detail record");
                }
            }
            
            log.info("Successfully parsed LTA response file: {} records", records.size());
            return new ParseResult(headerData, records, trailerData, null, null);
            
        } catch (Exception e) {
            log.error("Error parsing LTA response file", e);
            return new ParseResult(null, new ArrayList<>(), null, FileIntegrityError.PARSE_ERROR, e.getMessage());
        }
    }
    
    /**
     * Parse a header record from the LTA response file
     * Format: H + Date (8) + 682 spaces
     *
     * @param line The header record line
     * @return Map of header data
     */
    private Map<String, Object> parseHeaderRecord(String line) {
        Map<String, Object> headerData = new HashMap<>();
        
        try {
            // Appendix F Header Format: H + Date of Run (8) + Filler (682 spaces)
            headerData.put("recordType", line.substring(0, 1));
            headerData.put("dateOfRun", line.substring(1, 9));
            
            log.debug("Parsed header: recordType={}, dateOfRun={}", 
                    headerData.get("recordType"), headerData.get("dateOfRun"));
            
            return headerData;
        } catch (Exception e) {
            log.error("Error parsing header record", e);
            return headerData;
        }
    }
    
    /**
     * ENHANCED VERSION - Parse a detail record from the LTA response file according to Appendix F
     * VRLS Enquiry File (Results) from LTA to URA - 691 characters
     * Added detailed logging for diplomatic flag and key fields
     *
     * @param line The detail record line
     * @return Map of record data
     */
    private Map<String, Object> parseDetailRecord(String line) {
        Map<String, Object> record = new HashMap<>();
        
        try {
            // Ensure line is long enough
            if (line.length() < EXPECTED_RECORD_LENGTH) {
                log.error("Detail record too short: {} characters, expected {}", line.length(), EXPECTED_RECORD_LENGTH);
                return null;
            }
            
            // Parse according to Appendix F specification
            
            // Position 1: Record Type (A1)
            record.put("recordType", line.substring(0, 1));
            
            // Position 2-13: Vehicle Registration Number (A12)
            String vehicleNumber = line.substring(1, 13).trim();
            record.put("vehicleNumber", vehicleNumber);
            
            // Position 14-38: Chassis Number (A25)
            String chassisNumber = line.substring(13, 38).trim();
            record.put("chassisNumber", chassisNumber);
            
            // Position 39: Diplomatic Flag (A1) ⭐ KEY FIELD - LOG RAW VALUE
            String diplomaticFlag = line.substring(38, 39);
            record.put("diplomaticFlag", diplomaticFlag);
            
            // Position 40-49: Notice Number (A10) ⭐ KEY FIELD
            String offenceNoticeNumber = line.substring(39, 49).trim();
            record.put("offenceNoticeNumber", offenceNoticeNumber);
            
            // Position 50: Owner ID Type (A1)
            record.put("ownerIdType", line.substring(49, 50));
            
            // Position 51-53: Owner's Passport Place of Issue (A3)
            record.put("passportPlaceOfIssue", line.substring(50, 53));
            
            // Position 54-73: Owner ID (A20)
            String ownerId = line.substring(53, 73).trim();
            record.put("ownerId", ownerId);
            
            // Position 74-139: Owner Name (A66)
            String ownerName = line.substring(73, 139).trim();
            record.put("ownerName", ownerName);
            
            // Position 140: Address Type (A1)
            record.put("addressType", line.substring(139, 140));
            
            // Position 141-150: Block/House Number (A10)
            record.put("blockHouseNo", line.substring(140, 150).trim());
            
            // Position 151-182: Street Name (A32)
            record.put("streetName", line.substring(150, 182).trim());
            
            // Position 183-184: Floor Number (A2)
            record.put("floorNumber", line.substring(182, 184).trim());
            
            // Position 185-189: Unit Number (A5)
            record.put("unitNumber", line.substring(184, 189).trim());
            
            // Position 190-219: Building Name (A30)
            record.put("buildingName", line.substring(189, 219).trim());
            
            // Position 220-227: Postal Code (A8)
            record.put("postalCode", line.substring(219, 227).trim());
            
            // Position 228-327: Make Description (A100)
            String vehicleMake = line.substring(227, 327).trim();
            record.put("vehicleMake", vehicleMake);
            
            // Position 328-427: Primary Colour (A100)
            String primaryColour = line.substring(327, 427).trim();
            record.put("primaryColour", primaryColour);
            
            // Position 428-527: Secondary Colour (A100)
            record.put("secondaryColour", line.substring(427, 527).trim());
            
            // Position 528-535: Road Tax Expiry Date (N8)
            record.put("roadTaxExpiryDate", line.substring(527, 535).trim());
            
            // Position 536-542: Unladen Weight (N7)
            record.put("unladenWeight", line.substring(535, 542).trim());
            
            // Position 543-549: Maximum Laden Weight (N7)
            record.put("maxLadenWeight", line.substring(542, 549).trim());
            
            // Position 550-557: Effective Ownership Date (N8)
            record.put("effectiveOwnershipDate", line.substring(549, 557).trim());
            
            // Position 558-565: Deregistration Date (N8)
            record.put("deregistrationDate", line.substring(557, 565).trim());
            
            // Position 566: Return Error Code (A1) ⭐ KEY FIELD
            String errorCode = line.substring(565, 566);
            record.put("errorCode", errorCode);
            
            // Position 567-574: Processing Date (N8)
            record.put("processingDate", line.substring(566, 574).trim());
            
            // Position 575-578: Processing Time (N4)
            record.put("processingTime", line.substring(574, 578).trim());
            
            // Position 579-588: IU/OBU Label Number (A10)
            record.put("iuObuLabel", line.substring(578, 588).trim());
            
            // Position 589-596: Registered Address Effective Date (N8)
            record.put("regAddressEffectiveDate", line.substring(588, 596).trim());
            
            // Position 597-606: Mailing Block/House Number (A10)
            record.put("mailingBlockHouse", line.substring(596, 606).trim());
            
            // Position 607-638: Mailing Street Name (A32)
            record.put("mailingStreetName", line.substring(606, 638).trim());
            
            // Position 639-640: Mailing Floor Number (A2)
            record.put("mailingFloorNumber", line.substring(638, 640).trim());
            
            // Position 641-645: Mailing Unit Number (A5)
            record.put("mailingUnitNumber", line.substring(640, 645).trim());
            
            // Position 646-675: Mailing Building Name (A30)
            record.put("mailingBuildingName", line.substring(645, 675).trim());
            
            // Position 676-683: Mailing Postal Code (A8)
            record.put("mailingPostalCode", line.substring(675, 683).trim());
            
            // Position 684-691: Mailing Address Effective Date (N8)
            record.put("mailingAddressEffectiveDate", line.substring(683, 691).trim());
            
            // Add some legacy field mappings for compatibility
            record.put("vehicleColor", record.get("primaryColour")); // For backwards compatibility
            
            // Check if error code at position 566 is A, B, or C (file integrity errors)
            // These are different from numeric error codes 1-4
            String specialErrorCode = null;
            if ("A".equals(errorCode) || "B".equals(errorCode) || "C".equals(errorCode)) {
                specialErrorCode = errorCode;
                log.info("File integrity error code {} detected at position 566", specialErrorCode);
                record.put("specialErrorCode", specialErrorCode);
            }
            
            // ⭐ ENHANCED LOGGING: Log raw diplomatic flag value from LTA file
            log.info("📄 RAW FROM LTA FILE - Vehicle: {}, Notice: {}, Diplomatic Flag: '{}' (Position 39), Error Code: '{}' (Position 568), Special Error Code: '{}'", 
                    vehicleNumber, offenceNoticeNumber, diplomaticFlag, errorCode, specialErrorCode != null ? specialErrorCode : "None");
            
            // Additional detailed logging for key vehicle data
            log.info("📄 RAW VEHICLE DATA - Chassis: '{}', Make: '{}', Color: '{}', Owner: '{}'", 
                    chassisNumber, vehicleMake, primaryColour, ownerName);
            
            // Log key fields for debugging
            log.debug("Parsed detail record: vehicle={}, notice={}, ownerId={}, diplomaticFlag={}, errorCode={}", 
                    vehicleNumber, offenceNoticeNumber, ownerId, diplomaticFlag, errorCode);
            
            return record;
        } catch (Exception e) {
            log.error("Error parsing detail record: {}", line.substring(0, Math.min(100, line.length())), e);
            return null;
        }
    }
    
    /**
     * Parse a trailer record from the LTA response file
     * Format: T + Record Count (6) + 684 spaces
     *
     * @param line The trailer record line
     * @return Map of trailer data
     */
    private Map<String, Object> parseTrailerRecord(String line) {
        Map<String, Object> trailerData = new HashMap<>();
        
        try {
            // Appendix F Trailer Format: T + Number of Detail Record (6) + Filler (684 spaces)
            trailerData.put("recordType", line.substring(0, 1));
            trailerData.put("recordCount", Integer.parseInt(line.substring(1, 7).trim()));
            
            log.debug("Parsed trailer: recordType={}, recordCount={}", 
                    trailerData.get("recordType"), trailerData.get("recordCount"));
            
            return trailerData;
        } catch (Exception e) {
            log.error("Error parsing trailer record", e);
            return trailerData;
        }
    }
    
    /**
     * Enum for file integrity error codes
     * Error codes A, B, C are found at position 568 (Return Error Code field)
     * These are file-level errors that stop processing of the entire file
     */
    @Getter
    public enum FileIntegrityError {
        ERROR_A("A", "File integrity error A - Record count mismatch"),
        ERROR_B("B", "File integrity error B - Missing header record"),
        ERROR_C("C", "File integrity error C - Missing trailer record"),
        PARSE_ERROR("E", "General parsing error");
        
        private final String code;
        private final String description;
        
        FileIntegrityError(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        /**
         * Get FileIntegrityError by code
         * 
         * @param code Error code (A, B, C, E)
         * @return FileIntegrityError or null if not found
         */
        public static FileIntegrityError getByCode(String code) {
            if (code == null) {
                return null;
            }
            
            for (FileIntegrityError error : values()) {
                if (error.getCode().equals(code)) {
                    return error;
                }
            }
            
            return null;
        }
    }
    
    /**
     * Result class for parsed file data
     * UPDATED: Added constructor to create result with error code from filename
     */
    @Data
    @AllArgsConstructor
    public static class ParseResult {
        private Map<String, Object> headerData;
        private List<Map<String, Object>> records;
        private Map<String, Object> trailerData;
        private FileIntegrityError integrityError;
        private String errorMessage;
        
        /**
         * Constructor for creating a result with an error code from filename
         * 
         * @param errorCode The error code (A, B, C) extracted from filename
         */
        public ParseResult(String errorCode) {
            this.headerData = null;
            this.records = new ArrayList<>();
            this.trailerData = null;
            this.integrityError = FileIntegrityError.getByCode(errorCode);
            this.errorMessage = "Error code " + errorCode + " detected in filename";
        }
        
        /**
         * Check if the parse result has an integrity error
         * @return true if there is an integrity error, false otherwise
         */
        public boolean hasIntegrityError() {
            return integrityError != null;
        }
        
        /**
         * Get the error code (A, B, C, etc.) if there is an integrity error
         * @return the error code or null if no error
         */
        public String getErrorCode() {
            return integrityError != null ? integrityError.getCode() : null;
        }
    }
}