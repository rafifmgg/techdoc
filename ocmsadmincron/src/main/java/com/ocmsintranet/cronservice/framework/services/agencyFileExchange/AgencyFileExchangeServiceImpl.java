package com.ocmsintranet.cronservice.framework.services.agencyFileExchange;

import com.ocmsintranet.cronservice.framework.services.agencyFileExchange.helper.MhaFileFormatHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of AgencyFileExchangeService that handles file format conversions
 * for different agencies. Currently focused on MHA implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyFileExchangeServiceImpl implements AgencyFileExchangeService {

    // Constants for MHA file formats
    private static final String MHA_AGENCY_TYPE = "MHA";
    private static final String MHA_REQUEST_FILE_PREFIX = "URA2NRO_";
    private static final String MHA_RESPONSE_FILE_PREFIX = "NRO2URA_";
    private static final String MHA_REPORT_FILE_PREFIX = "REPORT_";
    private static final String MHA_REPORT_TOT_SUFFIX = ".TOT";
    private static final String MHA_REPORT_EXP_SUFFIX = ".EXP";
    private static final DateTimeFormatter MHA_TIMESTAMP_FORMATTER = MhaFileFormatHelper.TIMESTAMP_FORMATTER;
    
    // Constants for LTA file formats
    private static final String LTA_AGENCY_TYPE = "LTA";
    private static final String LTA_REQUEST_FILE_PREFIX = "VRL-URA-OFFENQ-D1-";
    private static final String LTA_RESPONSE_FILE_PREFIX = "VRL-URA-OFFREPLY-D2-";
    private static final DateTimeFormatter LTA_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // Constants for Toppan file formats
    private static final String TOPPAN_AGENCY_TYPE_PREFIX = "TOPPAN_";
    private static final DateTimeFormatter TOPPAN_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    // MHA field sizes for request file
    private static final int MHA_REQUEST_UIN_SIZE = 9;
    private static final String MHA_REQUEST_SEQUENCE_PREFIX = MhaFileFormatHelper.REQUEST_SEQUENCE_PREFIX;
    
    // MHA field sizes for response file
    private static final LinkedHashMap<String, Integer> MHA_RESPONSE_FIELD_SIZES = new LinkedHashMap<>();
    static {
        MHA_RESPONSE_FIELD_SIZES.put("uin", 9);
        MHA_RESPONSE_FIELD_SIZES.put("name", 66);
        MHA_RESPONSE_FIELD_SIZES.put("lifeStatus", 1);
        MHA_RESPONSE_FIELD_SIZES.put("addressType", 1);
        MHA_RESPONSE_FIELD_SIZES.put("blockHouseNo", 10);
        MHA_RESPONSE_FIELD_SIZES.put("streetName", 32);
        MHA_RESPONSE_FIELD_SIZES.put("floorNo", 2);
        MHA_RESPONSE_FIELD_SIZES.put("unitNo", 5);
        MHA_RESPONSE_FIELD_SIZES.put("buildingName", 30);
        MHA_RESPONSE_FIELD_SIZES.put("postalCode", 6);
        MHA_RESPONSE_FIELD_SIZES.put("lastChangeAddressDate", 8);
        MHA_RESPONSE_FIELD_SIZES.put("userInput", 23);
        MHA_RESPONSE_FIELD_SIZES.put("dateOfBirth", 8);
        MHA_RESPONSE_FIELD_SIZES.put("dateOfDeath", 8);
        MHA_RESPONSE_FIELD_SIZES.put("invalidAddressTag", 1);
        MHA_RESPONSE_FIELD_SIZES.put("timestamp", 23);
    }

    @Override
    public byte[] createRequestFileContent(String agencyType, List<Map<String, Object>> records, LocalDateTime timestamp) {
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return createMhaRequestFileContent(records, timestamp);
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return createLtaRequestFileContent(records, timestamp);
        } else if (agencyType != null && agencyType.startsWith(TOPPAN_AGENCY_TYPE_PREFIX)) {
            return createToppanRequestFileContent(agencyType, records, timestamp);
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    @Override
    public List<Map<String, Object>> parseResponseFileContent(String agencyType, byte[] fileContent) {
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return parseMhaResponseFileContent(fileContent);
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return parseLtaResponseFileContent(fileContent);
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    @Override
    public Map<String, Object> parseReportFileContent(String agencyType, byte[] fileContent) {
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return parseMhaTotReportFileContent(fileContent);
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            // LTA doesn't have report files in the same format as MHA
            // Return empty map for now, can be implemented if needed
            return new HashMap<>();
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    @Override
    public List<Map<String, Object>> parseExceptionsFileContent(String agencyType, byte[] fileContent) {
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return parseMhaExpFileContent(fileContent);
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            // LTA doesn't have exception files in the same format as MHA
            // Return empty list for now, can be implemented if needed
            return new ArrayList<>();
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    @Override
    public String generateRequestFilename(String agencyType, LocalDateTime timestamp) {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return MHA_REQUEST_FILE_PREFIX + timestamp.format(MHA_TIMESTAMP_FORMATTER) + MhaFileFormatHelper.REQUEST_FILE_EXTENSION;
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return LTA_REQUEST_FILE_PREFIX + timestamp.format(LTA_TIMESTAMP_FORMATTER);
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    @Override
    public boolean isValidResponseFile(String agencyType, String filename, byte[] fileContent) {
        if (MHA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return filename != null && (
                filename.startsWith(MHA_RESPONSE_FILE_PREFIX) || 
                (filename.startsWith(MHA_REPORT_FILE_PREFIX) && 
                 (filename.endsWith(MHA_REPORT_TOT_SUFFIX) || filename.endsWith(MHA_REPORT_EXP_SUFFIX)))
            );
        } else if (LTA_AGENCY_TYPE.equalsIgnoreCase(agencyType)) {
            return filename != null && filename.startsWith(LTA_RESPONSE_FILE_PREFIX);
        } else {
            throw new UnsupportedOperationException("Agency type not supported: " + agencyType);
        }
    }

    /**
     * Creates MHA request file content from the provided data.
     * Format follows the example in URA2NRO_20250306102959 file.
     */
    private byte[] createMhaRequestFileContent(List<Map<String, Object>> records, LocalDateTime timestamp) {
        log.info("Creating MHA request file content with {} records", records.size());
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter printWriter = new PrintWriter(writer)) {
            
            // Generate header line with timestamp and record count with exactly 9 leading spaces
            // Format: 9 spaces + timestamp (14 chars) + record count (6 chars) + 4 spaces
            String timestampStr = timestamp.format(MHA_TIMESTAMP_FORMATTER);
            String headerLine = "         " + timestampStr + String.format("%06d", records.size()) + "    ";
            printWriter.println(headerLine);
            
            // Generate data lines with notice numbers


            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                String line = formatMhaRequestRecord(record, timestamp);
                printWriter.println(line);
            }
            
            printWriter.flush();
            byte[] content = outputStream.toByteArray();
            log.info("Successfully created MHA request file content with {} bytes", content.length);
            return content;
        } catch (IOException e) {
            log.error("Error creating MHA request file content", e);
            throw new RuntimeException("Failed to create MHA request file content", e);
        }
    }

    /**
     * Formats a single MHA request record as a fixed-width string.
     * Format follows the example in URA2NRO_20250306102959 file.
     * 
     * @param record The record data containing the ID number and notice number
     * @param timestamp The timestamp to use for the record
     * @param sequenceNumber The sequence number to use for this record
     * @return A formatted string representing the record
     */
    private String formatMhaRequestRecord(Map<String, Object> record, LocalDateTime timestamp) {
        StringBuilder sb = new StringBuilder();
        
        // Format UIN (ID Number) - 9 characters
        // Get NRIC number from the record - this should be provided by MhaNricDataHelper
        String nricNo = "";
        
        // Try all possible field names for NRIC number
        if (record.containsKey("nricNo")) {
            Object value = record.get("nricNo");
            if (value != null) {
                nricNo = value.toString();
            }
        } else if (record.containsKey("idNo")) {
            Object value = record.get("idNo");
            if (value != null) {
                nricNo = value.toString();
            }
        }
        
        sb.append(padRight(nricNo, MHA_REQUEST_UIN_SIZE));
        
        // Format notice number - 10 characters
        // Get notice number from the record
        String noticeNo = "";
        if (record.containsKey("noticeNo")) {
            Object value = record.get("noticeNo");
            if (value != null) {
                noticeNo = value.toString();
            }
        }
        // Ensure notice number is exactly 10 characters
        sb.append(padRight(noticeNo, 10));
        
        // Format timestamp - 14 characters in format yyyyMMddHHmmss
        String timestampStr = timestamp != null ? 
                              timestamp.format(MHA_TIMESTAMP_FORMATTER) : 
                              LocalDateTime.now().format(MHA_TIMESTAMP_FORMATTER);
        sb.append(timestampStr);
        
        // Add Timestamp field at position 34 with 23 characters (current datetime + padding)
        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // 14 chars
        String timestampField = padRight(currentTimestamp, 23); // Pad to 23 chars total
        sb.append(timestampField);
        
        return sb.toString();
    }



    /**
     * Parses MHA response file content and returns the structured data.
     */
    private List<Map<String, Object>> parseMhaResponseFileContent(byte[] fileContent) {
        log.info("Parsing MHA response file content of {} bytes", fileContent.length);
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                Map<String, Object> record = parseMhaResponseRecord(line);
                results.add(record);
            }
            log.info("Successfully parsed {} records from MHA response file content", results.size());
            return results;
        } catch (IOException e) {
            log.error("Error parsing MHA response file content", e);
            throw new RuntimeException("Failed to parse MHA response file content", e);
        }
    }

    /**
     * Parses a single line from an MHA response file into a structured record.
     */
    private Map<String, Object> parseMhaResponseRecord(String line) {
        Map<String, Object> record = new HashMap<>();
        int position = 0;
        
        // Extract each field based on its defined size
        for (Map.Entry<String, Integer> field : MHA_RESPONSE_FIELD_SIZES.entrySet()) {
            int endPos = Math.min(position + field.getValue(), line.length());
            String value = position < line.length() ? line.substring(position, endPos).trim() : "";
            record.put(field.getKey(), value);
            position += field.getValue();
        }
        
        return record;
    }

    /**
     * Parses an MHA TOT report file content and returns the summary statistics.
     */
    private Map<String, Object> parseMhaTotReportFileContent(byte[] fileContent) {
        log.info("Parsing MHA TOT report file content of {} bytes", fileContent.length);
        Map<String, Object> summary = new HashMap<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Parse lines like "TOTAL RECORDS READ: 123"
                if (line.contains("TOTAL RECORDS READ:")) {
                    summary.put("totalRecordsRead", extractNumberFromLine(line));
                } else if (line.contains("TOTAL RECORDS MATCHED:")) {
                    summary.put("totalRecordsMatched", extractNumberFromLine(line));
                } else if (line.contains("TOTAL INVALID UIN/FIN:")) {
                    summary.put("totalInvalidUinFin", extractNumberFromLine(line));
                } else if (line.contains("TOTAL VALID UIN/FIN NOT FOUND:")) {
                    summary.put("totalValidUinFinNotFound", extractNumberFromLine(line));
                }
            }
            log.info("Successfully parsed MHA TOT report file content");
            return summary;
        } catch (IOException e) {
            log.error("Error parsing MHA TOT report file content", e);
            throw new RuntimeException("Failed to parse MHA TOT report file content", e);
        }
    }

    /**
     * Parses an MHA EXP file content and returns the exception records.
     */
    private List<Map<String, Object>> parseMhaExpFileContent(byte[] fileContent) {
        log.info("Parsing MHA EXP file content of {} bytes", fileContent.length);
        List<Map<String, Object>> exceptions = new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Parse exception records
                // Format is typically: serial number, ID number, exception status
                String[] parts = line.trim().split("\\s+", 3);
                if (parts.length >= 3) {
                    Map<String, Object> exception = new HashMap<>();
                    exception.put("serialNumber", parts[0]);
                    exception.put("idNumber", parts[1]);
                    exception.put("exceptionStatus", parts[2]);
                    exceptions.add(exception);
                }
            }
            log.info("Successfully parsed {} exception records from MHA EXP file content", exceptions.size());
            return exceptions;
        } catch (IOException e) {
            log.error("Error parsing MHA EXP file content", e);
            throw new RuntimeException("Failed to parse MHA EXP file content", e);
        }
    }

    /**
     * Extracts a number from a line like "TOTAL RECORDS READ: 123"
     */
    private int extractNumberFromLine(String line) {
        String[] parts = line.split(":");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                log.warn("Could not parse number from line: {}", line);
            }
        }
        return 0;
    }

    /**
     * Pads a string to the right with spaces to reach the specified length.
     */
    private String padRight(String s, int n) {
        if (s == null) {
            s = "";
        }
        if (s.length() > n) {
            return s.substring(0, n);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Creates LTA request file content from the provided data according to
     * the VRLS Enquiry File format (Appendix E).
     * 
     * @param records The records to include in the file
     * @param timestamp The timestamp to use for the file
     * @return The generated file content as a byte array with UTF-8 encoding for human readability
     */
    private byte[] createLtaRequestFileContent(List<Map<String, Object>> records, LocalDateTime timestamp) {
        log.info("Creating LTA request file content with {} records", records.size());
        
        try {
            // Create the content as a string first
            StringBuilder fileContent = new StringBuilder();
            
            // Header Record - 47 characters total
            // Format: 'H' (1) + Date (8) + Filler spaces (38)
            String headerDate = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            StringBuilder headerBuilder = new StringBuilder("H");
            headerBuilder.append(headerDate);
            // Pad with spaces to exactly 47 characters
            headerBuilder = new StringBuilder(padRight(headerBuilder.toString(), 47));
            fileContent.append(headerBuilder.toString()).append("\n");
            
            // Data records - each 47 characters
            int recordCount = 0;
            
            for (Map<String, Object> record : records) {
                String line = formatLtaRequestRecord(record, timestamp);
                fileContent.append(line).append("\n");
                recordCount++;
                
                // Log preview of detail records
                log.debug("Detail record preview: {}...", line.substring(0, Math.min(line.length(), 47)));
            }
            
            // Trailer Record - 47 characters total
            // Format: 'T' (1) + Number of Detail Records (6) + Filler spaces (40)
            StringBuilder trailerBuilder = new StringBuilder("T");
            trailerBuilder.append(String.format("%06d", recordCount));
            // Pad with spaces to exactly 47 characters
            trailerBuilder = new StringBuilder(padRight(trailerBuilder.toString(), 47));
            fileContent.append(trailerBuilder.toString());
            
            log.info("Total records: {}", recordCount);
            log.debug("Generated LTA request file content in VRLS Enquiry format (47 chars per line)");
            
            // Convert to byte array with UTF-8 encoding for human readability
            return fileContent.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error creating LTA request file content", e);
            throw new RuntimeException("Error creating LTA request file content", e);
        }
    }
    
    /**
     * Formats a single LTA request record as a fixed-width string according to the
     * VRLS Enquiry File format (Appendix E).
     * 
     * @param record The record data
     * @param timestamp Current processing timestamp
     * @return The formatted record as a string with exactly 47 characters
     */
    private String formatLtaRequestRecord(Map<String, Object> record, LocalDateTime timestamp) {
        // Log the record information for debugging
        String recordNoticeNo = getStringValue(record, "noticeNo", "");
        String vehicleNo = getStringValue(record, "vehicleNo", "");
        log.info("===== LTA RECORD INFO FOR NOTICE {} =====", recordNoticeNo);
        log.info("Vehicle No: {}", vehicleNo);
        log.info("Notice Date and Time: {}", record.get("noticeDateAndTime"));
        log.info("=====================================");

        StringBuilder sb = new StringBuilder();
        
        // Record Type - A1 - Contains 'D'
        sb.append("D");
        
        // Vehicle Registration Number - A12 - Left justified
        sb.append(padRight(vehicleNo, 12));
        
        // Notice Number - A10 - Left justified
        sb.append(padRight(recordNoticeNo, 10));
        
        // Date and Time of Offence - N12 - YYYYMMDDHHMM format
        String noticeDateAndTime = "";
        if (record.get("noticeDateAndTime") != null) {
            if (record.get("noticeDateAndTime") instanceof LocalDateTime) {
                LocalDateTime dateTime = (LocalDateTime) record.get("noticeDateAndTime");
                noticeDateAndTime = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            } else {
                // Handle string or other formats if needed
                log.warn("Notice date and time is not a LocalDateTime: {}", record.get("noticeDateAndTime"));
                noticeDateAndTime = "000000000000"; // Default placeholder
            }
        } else {
            noticeDateAndTime = "000000000000"; // Default placeholder
        }
        sb.append(noticeDateAndTime);
        
        // Processing Date - N8 - YYYYMMDD format
        String processingDate = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        sb.append(processingDate);
        
        // Processing Time - N4 - HHMM format
        String processingTime = timestamp.format(DateTimeFormatter.ofPattern("HHmm"));
        sb.append(processingTime);
        
        // Ensure the total length is exactly 47 characters
        return padRight(sb.toString(), 47);
    }
    
    /**
     * Helper method to get string value from a record with a default value if not found
     */
    private String getStringValue(Map<String, Object> record, String key, String defaultValue) {
        if (record != null && record.containsKey(key)) {
            Object value = record.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }
    
    /**
     * Appends a field to a CSV line with proper escaping.
     */
    private void appendCsvField(StringBuilder sb, Object value) {
        appendCsvField(sb, value, true);
    }
    
    /**
     * Appends a field to a CSV line with proper escaping.
     */
    private void appendCsvField(StringBuilder sb, Object value, boolean addComma) {
        String strValue = value != null ? value.toString() : "";
        
        // Escape quotes and wrap in quotes if needed
        if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
            strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
        }
        
        sb.append(strValue);
        
        if (addComma) {
            sb.append(",");
        }
    }
    
    /**
     * Creates Toppan request file content from the provided data.
     * Generates fixed-width format file with 719 character records.
     *
     * @param agencyType The specific Toppan agency type (e.g., TOPPAN_RD1, TOPPAN_RD2)
     * @param records The records to include in the file
     * @param timestamp The timestamp to use for the file
     * @return The generated file content as a byte array
     */
    private byte[] createToppanRequestFileContent(String agencyType, List<Map<String, Object>> records, LocalDateTime timestamp) {
        log.info("Creating Toppan request file content for {} with {} records", agencyType, records.size());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter printWriter = new PrintWriter(writer)) {

            // Sort records by postal code (ascending) as per specification
            List<Map<String, Object>> sortedRecords = new ArrayList<>(records);
            sortedRecords.sort((r1, r2) -> {
                String pc1 = getStringValue(r1, "postalCode", "");
                String pc2 = getStringValue(r2, "postalCode", "");
                return pc1.compareTo(pc2);
            });

            // HEADER RECORD - Fixed width 719 characters
            StringBuilder header = new StringBuilder();

            // Record Type - A1 - "H"
            header.append("H");

            // Date of run - N8 - YYYYMMDD format
            header.append(timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // Time of run - N4 - HHMM format (24-hour)
            header.append(timestamp.format(DateTimeFormatter.ofPattern("HHmm")));

            // Filler - A705 - spaces
            header.append(padRightFixed("", 705));

            // Ensure exactly 719 characters and add line feed
            printWriter.println(header.toString());

            // DETAIL RECORDS - Fixed width 719 characters each
            for (Map<String, Object> record : sortedRecords) {
                StringBuilder detail = new StringBuilder();

                // Record Type - A1 - "D"
                detail.append("D");

                // Date of Request driver particulars - A10 - DD-MM-YYYY
                detail.append(formatDateField(record.get("letterDate"), "dd-MM-yyyy", 10));

                // Notice No - A10 - left justified
                detail.append(padRightFixed(getStringValue(record, "noticeNo", ""), 10));

                // Owner/Driver Name - A66 - left justified
                detail.append(padRightFixed(getStringValue(record, "ownerDriverName", ""), 66));

                // NRIC - A20 - left justified - PADDED WITH SPACES (Government policy: cannot use NRIC in public-facing letters)
                detail.append(padRightFixed("", 20));

                // Block/House No - A10 - left justified
                detail.append(padRightFixed(getStringValue(record, "blkHseNo", ""), 10));

                // Street Name - A32 - left justified
                detail.append(padRightFixed(getStringValue(record, "streetName", ""), 32));

                // Floor No - A2 - left justified
                detail.append(padRightFixed(getStringValue(record, "floorNo", ""), 2));

                // Unit No - A5 - left justified
                detail.append(padRightFixed(getStringValue(record, "unitNo", ""), 5));

                // Building Name - A30 - left justified
                detail.append(padRightFixed(getStringValue(record, "buildingName", ""), 30));

                // Postal Code - A6 - left justified (blank means foreign address)
                detail.append(padRightFixed(getStringValue(record, "postalCode", ""), 6));

                // Vehicle No - A12 - left justified
                detail.append(padRightFixed(getStringValue(record, "vehicleNo", ""), 12));

                // Notice Date & Time - A19 - DD-MM-YYYY HH:MI AM (12-hour format with AM/PM)
                detail.append(formatDateTimeField12Hour(record.get("noticeDateAndTime"), 19));

                // Parking place name - A45 - left justified
                detail.append(padRightFixed(getStringValue(record, "ppName", ""), 45));

                // Non-Coupon/Coupon Parking Rule infringed - A61 - left justified
                // Logic: If computer_rule_code <= 39999 → Parking Places Rules (with rule_no/desc translation for specific rules)
                //        If computer_rule_code > 39999 → Parking Places (Coupon Parking) Rules (no translation)
                String ruleNo = getStringValue(record, "ruleNo", "");
                String parkingRuleText = "";
                String ruleDescription = getStringValue(record, "ruleDesc", "");

                Object computerRuleCodeObj = record.get("computerRuleCode");
                if (computerRuleCodeObj != null) {
                    try {
                        long computerRuleCode = computerRuleCodeObj instanceof Number ?
                            ((Number) computerRuleCodeObj).longValue() :
                            Long.parseLong(computerRuleCodeObj.toString());

                        if (computerRuleCode <= 39999) {
                            // Parking Places Rules - translate rule number and description for specific rules
                            String translatedRuleNo = translateRuleNo(ruleNo);
                            parkingRuleText = "Rule " + translatedRuleNo + " of Parking Places Rules";
                            // Override description for specific rule numbers
                            String translatedDesc = translateRuleDesc(ruleNo);
                            if (translatedDesc != null) {
                                ruleDescription = translatedDesc;
                            }
                        } else {
                            // Coupon Parking Rule (> 39999) - no translation, use rule_no and description from DB
                            parkingRuleText = "Rule " + ruleNo + " of Parking Places (Coupon Parking) Rules";
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing computerRuleCode: {}", computerRuleCodeObj);
                        parkingRuleText = ruleNo;
                    }
                } else {
                    parkingRuleText = ruleNo;
                }

                detail.append(padRightFixed(parkingRuleText, 61));

                // Rule Description - A255 - left justified
                detail.append(padRightFixed(ruleDescription, 255));

                // Warrant Description - A100 - left justified - PADDED WITH SPACES (Court module not ready for MVP1)
                detail.append(padRightFixed("", 100));

                // Amount - N7 - in cents with leading zeros (always composition amount)
                detail.append(formatAmountInCents(record.get("compositionAmount"), 7));  // TO DO: OCMS 11 change to amount payable

                // Notice Expiry date - A10 - DD-MM-YYYY
                detail.append(formatDateField(record.get("paymentDueDate"), "dd-MM-yyyy", 10));

                // Fine Amount - N7 - in cents with leading zeros (composition amount + SUR parameter for all stages)
                // Formula: compositionAmount + Param(CPC, SUR)
                double compositionAmount = 0;
                double surAmount = 0;

                // Get composition amount
                Object compoObj = record.get("compositionAmount");
                if (compoObj != null) {
                    try {
                        compositionAmount = compoObj instanceof Number ?
                            ((Number) compoObj).doubleValue() :
                            Double.parseDouble(compoObj.toString());
                    } catch (Exception e) {
                        log.warn("Error parsing composition amount: {}", compoObj);
                        compositionAmount = 0;
                    }
                }

                // Get SUR parameter (Param code = CPC, Param ID = SUR)
                Object surObj = record.get("surAmount");
                if (surObj != null) {
                    try {
                        surAmount = surObj instanceof Number ?
                            ((Number) surObj).doubleValue() :
                            Double.parseDouble(surObj.toString());
                    } catch (Exception e) {
                        log.warn("Error parsing SUR amount: {}", surObj);
                        surAmount = 0;
                    }
                }

                // Calculate fine amount = compo + SUR
                double fineAmount = compositionAmount + surAmount;
                detail.append(formatAmountInCents(fineAmount, 7));

                // Payment Due date - A10 - DD-MM-YYYY (notice date + 7 days)
                LocalDate paymentDueDate = addDaysToDate(record.get("noticeDateAndTime"), 7);
                detail.append(formatDateField(paymentDueDate, "dd-MM-yyyy", 10));

                // Ensure exactly 719 characters and add line feed
                printWriter.println(detail.toString());
            }

            // TRAILER RECORD - Fixed width 719 characters
            StringBuilder trailer = new StringBuilder();

            // Record Type - A1 - "T"
            trailer.append("T");

            // Number of Detail Records - N7 - with leading zeros
            trailer.append(String.format("%07d", sortedRecords.size()));

            // Filler - A710 - spaces
            trailer.append(padRightFixed("", 710));

            // Ensure exactly 719 characters and add line feed
            printWriter.println(trailer.toString());

            printWriter.flush();
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error creating Toppan request file content", e);
            throw new RuntimeException("Failed to create Toppan request file content", e);
        }
    }

    /**
     * Formats a date field for fixed-width output.
     */
    private String formatDateField(Object value, String pattern, int width) {
        if (value == null) {
            return padRightFixed("", width);
        }

        try {
            LocalDateTime dateTime;
            if (value instanceof LocalDateTime) {
                dateTime = (LocalDateTime) value;
            } else if (value instanceof LocalDate) {
                dateTime = ((LocalDate) value).atStartOfDay();
            } else if (value instanceof java.sql.Timestamp) {
                // Handle SQL Timestamp (from native queries)
                dateTime = ((java.sql.Timestamp) value).toLocalDateTime();
            } else if (value instanceof java.util.Date) {
                // Handle java.util.Date
                dateTime = LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
            } else if (value instanceof String) {
                // Try to parse ISO format
                String strValue = value.toString();
                if (strValue.contains("T")) {
                    dateTime = LocalDateTime.parse(strValue.substring(0, Math.min(strValue.length(), 19)));
                } else if (strValue.length() == 10 && strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    // Parse ISO date format (YYYY-MM-DD)
                    dateTime = LocalDate.parse(strValue).atStartOfDay();
                } else {
                    return padRightFixed(strValue, width);
                }
            } else {
                return padRightFixed(value.toString(), width);
            }

            String formatted = dateTime.format(DateTimeFormatter.ofPattern(pattern));
            return padRightFixed(formatted, width);
        } catch (Exception e) {
            log.warn("Error formatting date field: {}", e.getMessage());
            return padRightFixed("", width);
        }
    }

    /**
     * Formats a datetime field in 12-hour format with AM/PM.
     */
    private String formatDateTimeField12Hour(Object value, int width) {
        if (value == null) {
            return padRightFixed("", width);
        }

        try {
            LocalDateTime dateTime;
            if (value instanceof LocalDateTime) {
                dateTime = (LocalDateTime) value;
            } else if (value instanceof java.sql.Timestamp) {
                // Handle SQL Timestamp (from native queries)
                dateTime = ((java.sql.Timestamp) value).toLocalDateTime();
            } else if (value instanceof java.util.Date) {
                // Handle java.util.Date
                dateTime = LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
            } else if (value instanceof String) {
                // Try to parse various datetime string formats
                String strValue = value.toString().trim();
                if (strValue.contains("T")) {
                    // ISO format with T separator (e.g., 2025-09-10T03:00:00)
                    dateTime = LocalDateTime.parse(strValue.substring(0, Math.min(strValue.length(), 19)));
                } else if (strValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                    // Format: YYYY-MM-DD HH:MM:SS (e.g., 2025-09-10 03:00:00)
                    dateTime = LocalDateTime.parse(strValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else if (strValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                    // Format: YYYY-MM-DD HH:MM (e.g., 2025-09-10 03:00)
                    dateTime = LocalDateTime.parse(strValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } else {
                    return padRightFixed(strValue, width);
                }
            } else {
                return padRightFixed(value.toString(), width);
            }

            // Format as DD-MM-YYYY HH:MI AM (12-hour format)
            String dateStr = dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            String timeStr = dateTime.format(DateTimeFormatter.ofPattern("hh:mm a")).toUpperCase();
            String formatted = dateStr + " " + timeStr;

            return padRightFixed(formatted, width);
        } catch (Exception e) {
            log.warn("Error formatting datetime field: {}", e.getMessage());
            return padRightFixed("", width);
        }
    }

    /**
     * Formats an amount in cents with leading zeros.
     */
    private String formatAmountInCents(Object value, int width) {
        if (value == null) {
            return String.format("%0" + width + "d", 0);
        }

        try {
            // Convert to cents
            double amount;
            if (value instanceof Number) {
                amount = ((Number) value).doubleValue();
            } else {
                amount = Double.parseDouble(value.toString());
            }

            // Convert to cents (multiply by 100)
            int cents = (int) Math.round(amount * 100);

            // Format with leading zeros
            return String.format("%0" + width + "d", cents);
        } catch (Exception e) {
            log.warn("Error formatting amount field: {}", e.getMessage());
            return String.format("%0" + width + "d", 0);
        }
    }

    /**
     * Calculates a date by adding days to a given date.
     *
     * @param dateValue The base date (can be LocalDateTime, LocalDate, Timestamp, Date, or String)
     * @param daysToAdd Number of days to add
     * @return The calculated date as LocalDate
     */
    private LocalDate addDaysToDate(Object dateValue, int daysToAdd) {
        if (dateValue == null) {
            return LocalDate.now().plusDays(daysToAdd);
        }

        LocalDate date;
        if (dateValue instanceof LocalDateTime) {
            date = ((LocalDateTime) dateValue).toLocalDate();
        } else if (dateValue instanceof LocalDate) {
            date = (LocalDate) dateValue;
        } else if (dateValue instanceof java.sql.Timestamp) {
            date = ((java.sql.Timestamp) dateValue).toLocalDateTime().toLocalDate();
        } else if (dateValue instanceof java.util.Date) {
            date = LocalDateTime.ofInstant(((java.util.Date) dateValue).toInstant(), java.time.ZoneId.systemDefault()).toLocalDate();
        } else if (dateValue instanceof String) {
            String strValue = dateValue.toString().trim();
            try {
                if (strValue.contains("T")) {
                    date = LocalDate.parse(strValue.substring(0, 10));
                } else if (strValue.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    date = LocalDate.parse(strValue.substring(0, 10));
                } else {
                    return LocalDate.now().plusDays(daysToAdd);
                }
            } catch (Exception e) {
                log.warn("Error parsing date string: {}", strValue);
                return LocalDate.now().plusDays(daysToAdd);
            }
        } else {
            return LocalDate.now().plusDays(daysToAdd);
        }

        return date.plusDays(daysToAdd);
    }

    /**
     * Fixed-width padding for Toppan format.
     * Ensures output is exactly the specified width.
     */
    private String padRightFixed(String str, int width) {
        if (str == null) str = "";
        if (str.length() >= width) {
            return str.substring(0, width);
        }
        return String.format("%-" + width + "s", str);
    }

    /**
     * Translates parking rule numbers for Toppan file.
     * Specific rule_no values need to be translated to their proper rule format.
     * Applied when computer_rule_code <= 39999 (Parking Places Rules).
     *
     * Translation rules:
     * - 13# → 13
     * - 13* → 13
     * - 13A# → 13A(1)(a)
     * - 13A* → 13A(1)(b)
     * - 4(1)# → 4(1)
     * - 4(1)* → 4(1)
     * - Other values → use as-is (no translation)
     */
    private String translateRuleNo(String ruleNo) {
        if (ruleNo == null || ruleNo.isEmpty()) {
            return ruleNo;
        }

        switch (ruleNo) {
            case "13#":
            case "13*":
                return "13";
            case "13A#":
                return "13A(1)(a)";
            case "13A*":
                return "13A(1)(b)";
            case "4(1)#":
            case "4(1)*":
                return "4(1)";
            default:
                // No translation needed, use rule_no as-is
                return ruleNo;
        }
    }

    /**
     * Translates parking rule descriptions for Toppan file.
     * Returns translated description for specific rule_no values, or null if no translation needed.
     * Applied when computer_rule_code <= 39999 (Parking Places Rules).
     * If rule_no doesn't match specific patterns, returns null to use description from DB.
     *
     * Translation rules:
     * - 13# → Parking beyond the boundaries of a parking lot
     * - 13* → Parking in such manner as to cause obstruction in/around a parking place
     * - 13A# → Entering a parking place other than through the access provided for that purpose
     * - 13A* → Leaving a parking place other than through the exit provided for that purpose
     * - 4(1)# → Parking in a coupon parking place without displaying valid parking coupon(s)
     * - 4(1)* → Parking in a coupon parking place without displaying valid parking coupon(s) - coupon(s) displayed not valid/insufficient to meet the parking charge
     * - Other values → null (use description from DB)
     */
    private String translateRuleDesc(String ruleNo) {
        if (ruleNo == null || ruleNo.isEmpty()) {
            return null;
        }

        switch (ruleNo) {
            case "13#":
                return "Parking beyond the boundaries of a parking lot";
            case "13*":
                return "Parking in such manner as to cause obstruction in/around a parking place";
            case "13A#":
                return "Entering a parking place other than through the access provided for that purpose";
            case "13A*":
                return "Leaving a parking place other than through the exit provided for that purpose";
            case "4(1)#":
                return "Parking in a coupon parking place without displaying valid parking coupon(s)";
            case "4(1)*":
                return "Parking in a coupon parking place without displaying valid parking coupon(s) - coupon(s) displayed not valid/insufficient to meet the parking charge";
            default:
                // No translation needed, use description from DB
                return null;
        }
    }

    /**
     * Parses LTA response file content and returns the structured data.
     */
    private List<Map<String, Object>> parseLtaResponseFileContent(byte[] fileContent) {
        log.info("Parsing LTA response file content of {} bytes", fileContent.length);
        List<Map<String, Object>> responseRecords = new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            String line;
            boolean isFirstLine = true;
            
            while ((line = bufferedReader.readLine()) != null) {
                if (isFirstLine) {
                    // Skip header row
                    isFirstLine = false;
                    continue;
                }
                
                if (!line.trim().isEmpty()) {
                    Map<String, Object> record = parseLtaResponseRecord(line);
                    responseRecords.add(record);
                }
            }
            
            log.info("Successfully parsed {} records from LTA response file content", responseRecords.size());
            return responseRecords;
            
        } catch (IOException e) {
            log.error("Error parsing LTA response file content", e);
            throw new RuntimeException("Failed to parse LTA response file content", e);
        }
    }
    
    /**
     * Parses a single line from an LTA response file into a structured record.
     */
    private Map<String, Object> parseLtaResponseRecord(String line) {
        Map<String, Object> record = new HashMap<>();
        
        // Parse CSV line, handling quoted fields
        String[] fields = parseCsvLine(line);
        
        // Map fields to record based on expected order
        if (fields.length >= 1) record.put("noticeNo", fields[0]);
        if (fields.length >= 2) record.put("status", fields[1]);
        if (fields.length >= 3) record.put("remarks", fields[2]);
        
        return record;
    }
    
    /**
     * Extracts the stage from the agency type string.
     * For example: "TOPPAN_RR3" -> "RR3", "TOPPAN_DR3" -> "DR3"
     *
     * @param agencyType The agency type string (e.g., "TOPPAN_RR3")
     * @return The stage code (e.g., "RR3") or empty string if not found
     */
    private String extractStageFromAgencyType(String agencyType) {
        if (agencyType != null && agencyType.startsWith(TOPPAN_AGENCY_TYPE_PREFIX)) {
            return agencyType.substring(TOPPAN_AGENCY_TYPE_PREFIX.length());
        }
        return "";
    }

    /**
     * Parses a CSV line, properly handling quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Check for escaped quotes
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // Skip the next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add the last field
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
}
