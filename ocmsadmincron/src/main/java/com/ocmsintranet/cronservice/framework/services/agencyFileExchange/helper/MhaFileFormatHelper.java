package com.ocmsintranet.cronservice.framework.services.agencyFileExchange.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class for MHA file format operations.
 * Contains constants and utility methods specific to MHA file formats.
 */
@Slf4j
@Component
public class MhaFileFormatHelper {

    // File naming constants
    public static final String AGENCY_TYPE = "MHA";
    public static final String REQUEST_FILE_PREFIX = "URA2NRO_";
    public static final String RESPONSE_FILE_PREFIX = "NRO2URA_";
    public static final String REPORT_FILE_PREFIX = "REPORT_";
    public static final String REPORT_TOT_SUFFIX = ".TOT";
    public static final String REPORT_EXP_SUFFIX = ".EXP";
    public static final String REQUEST_FILE_EXTENSION = "";
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // Request file format constants
    public static final String REQUEST_SEQUENCE_PREFIX = "X";  // Prefix for sequence numbers (e.g., X000081488)
    
    // Request file field definitions
    public static final LinkedHashMap<String, Integer> REQUEST_FIELD_SIZES = new LinkedHashMap<>();
    static {
        REQUEST_FIELD_SIZES.put("idNo", 9);
        REQUEST_FIELD_SIZES.put("timestamp", 23);
    }
    
    // Response file field definitions based on the official format
    public static final LinkedHashMap<String, String> RESPONSE_FIELD_DEFINITIONS = new LinkedHashMap<>();
    static {
        // Format based on the official MHA response file format
        // Format: [field name, start position, size, type, remarks]
        RESPONSE_FIELD_DEFINITIONS.put("uin", "1,9");                 // UIN (1-9)
        RESPONSE_FIELD_DEFINITIONS.put("name", "10,66");              // Name (10-75)
        RESPONSE_FIELD_DEFINITIONS.put("dateOfBirth", "76,8");        // Date of birth (76-83)
        RESPONSE_FIELD_DEFINITIONS.put("addressType", "84,1");        // MHA_Address_type (84)
        RESPONSE_FIELD_DEFINITIONS.put("blockHouseNo", "85,10");           // MHA_Block No (85-94)
        RESPONSE_FIELD_DEFINITIONS.put("streetName", "95,32");        // MHA_Street Name (95-126)
        RESPONSE_FIELD_DEFINITIONS.put("floorNo", "127,2");           // MHA_Floor No (127-128)
        RESPONSE_FIELD_DEFINITIONS.put("unitNo", "129,5");            // MHA_Unit No (129-133)
        RESPONSE_FIELD_DEFINITIONS.put("buildingName", "134,30");     // MHA_Building Name (134-163)
        RESPONSE_FIELD_DEFINITIONS.put("filler", "164,4");            // Filler (164-167)
        RESPONSE_FIELD_DEFINITIONS.put("postalCode", "168,6");        // MHA_New_Postal_Code (168-173)
        RESPONSE_FIELD_DEFINITIONS.put("dateOfDeath", "174,8");       // Date of Death (174-181)
        RESPONSE_FIELD_DEFINITIONS.put("lifeStatus", "182,1");        // Life Status (182)
        RESPONSE_FIELD_DEFINITIONS.put("invalidAddressTag", "183,1"); // Invalid Address Tag (183)
        RESPONSE_FIELD_DEFINITIONS.put("uraReferenceNo", "184,10");   // URA Reference No. (184-193)
        RESPONSE_FIELD_DEFINITIONS.put("batchDateTime", "194,14");    // Batch Date Time (194-207)
        RESPONSE_FIELD_DEFINITIONS.put("lastChangeAddressDate", "208,8"); // Date of Address Change (208-215)
        RESPONSE_FIELD_DEFINITIONS.put("timestamp", "216,23");       // Timestamp (216-238)
    }
    
    // Invalid address tag codes
    public static final Map<String, String> INVALID_ADDRESS_CODES = Map.of(
        "D", "Delisted Address",
        "M", "Demolished",
        "F", "Fail to Report",
        "G", "Gone Away",
        "I", "Invalid Address",
        "N", "No such numbers",
        "P", "Outdated Address",
        "S", "Overseas"
    );
    
    /**
     * Generates a filename for an MHA request file based on the provided timestamp.
     * 
     * @param timestamp Timestamp to use (or null to use current time)
     * @return Generated filename in the format URA2NRO_YYYYMMDDHHMMSS
     */
    public String generateRequestFilename(LocalDateTime timestamp) {
        LocalDateTime fileTimestamp = timestamp != null ? timestamp : LocalDateTime.now();
        return REQUEST_FILE_PREFIX + fileTimestamp.format(TIMESTAMP_FORMATTER);
    }
    
    /**
     * Formats a single record as a fixed-width string for MHA request file.
     * 
     * @param record The record to format
     * @param timestamp Timestamp to include in the record
     * @return Formatted fixed-width string
     */
    public String formatRequestRecord(Map<String, Object> record, LocalDateTime timestamp) {
        StringBuilder sb = new StringBuilder();
        
        // Format ID Number - 9 characters
        String idNo = record.getOrDefault("idNo", "").toString();
        sb.append(padRight(idNo, REQUEST_FIELD_SIZES.get("idNo")));
        
        // Format timestamp - 23 characters
        String timestampStr = timestamp != null ? 
                              timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) : 
                              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        sb.append(padRight(timestampStr, REQUEST_FIELD_SIZES.get("timestamp")));
        
        return sb.toString();
    }
    
    /**
     * Parses a single line from an MHA response file into a structured record.
     * 
     * @param line The line to parse
     * @return Map containing the parsed fields
     */
    public Map<String, Object> parseResponseRecord(String line) {
        Map<String, Object> record = new LinkedHashMap<>();
        
        // Ensure the line is long enough for parsing
        if (line.length() < 215) {
            log.warn("Record line too short (length: {}), expected at least 215 characters. Padding line.", line.length());
            line = padRight(line, 215);
        }
        
        // Extract each field based on its defined position and size
        for (Map.Entry<String, String> fieldDef : RESPONSE_FIELD_DEFINITIONS.entrySet()) {
            String fieldName = fieldDef.getKey();
            String[] positionSize = fieldDef.getValue().split(",");
            
            if (positionSize.length != 2) {
                log.warn("Invalid field definition for {}: {}", fieldName, fieldDef.getValue());
                continue;
            }
            
            try {
                int startPos = Integer.parseInt(positionSize[0]) - 1; // Convert 1-based to 0-based index
                int size = Integer.parseInt(positionSize[1]);
                int endPos = Math.min(startPos + size, line.length());
                
                if (startPos < line.length()) {
                    String value = line.substring(startPos, endPos).trim();
                    record.put(fieldName, value);
                } else {
                    record.put(fieldName, "");
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid position/size for field {}: {}", fieldName, fieldDef.getValue());
            }
        }
        
        // Add additional derived fields
        if (record.containsKey("dateOfBirth")) {
            String dobStr = (String) record.get("dateOfBirth");
            if (dobStr != null && !dobStr.isEmpty()) {
                try {
                    LocalDateTime dob = LocalDateTime.parse(dobStr + "000000", 
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    record.put("dateOfBirthFormatted", dob);
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
        }
        
        // Process date of death if present
        if (record.containsKey("dateOfDeath")) {
            String dodStr = (String) record.get("dateOfDeath");
            if (dodStr != null && !dodStr.isEmpty()) {
                try {
                    LocalDateTime dod = LocalDateTime.parse(dodStr + "000000", 
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    record.put("dateOfDeathFormatted", dod);
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
        }
        
        // Log the parsed record for debugging
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Parsed record fields: ");
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                sb.append(entry.getKey()).append("='")
                  .append(entry.getValue()).append("', ");
            }
            log.debug(sb.toString());
        }
        
        return record;
    }
    
    /**
     * Validates if a file appears to be a valid MHA response file based on its name.
     * 
     * @param filename The filename to check
     * @return true if the file appears to be a valid MHA response file
     */
    public boolean isValidResponseFile(String filename) {
        return filename.startsWith(RESPONSE_FILE_PREFIX) || 
               (filename.startsWith(REPORT_FILE_PREFIX) && 
                (filename.endsWith(REPORT_TOT_SUFFIX) || filename.endsWith(REPORT_EXP_SUFFIX)));
    }
    
    /**
     * Pads a string to the right with spaces to reach the specified length.
     */
    public String padRight(String s, int n) {
        if (s == null) {
            s = "";
        }
        if (s.length() > n) {
            return s.substring(0, n);
        }
        return String.format("%-" + n + "s", s);
    }
    
    /**
     * Pads a string to the left with spaces to reach the specified length.
     */
    public String padLeft(String s, int n) {
        if (s == null) {
            s = "";
        }
        if (s.length() > n) {
            return s.substring(0, n);
        }
        return String.format("%" + n + "s", s);
    }
}
