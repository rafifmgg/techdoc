package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive parser for all types of Toppan response files.
 * 
 * This parser handles the complex task of extracting processing results from various
 * types of response files that Toppan sends after completing print and dispatch operations.
 * It supports multiple file formats and handles the specific data structures used by Toppan.
 * 
 * SUPPORTED FILE TYPES:
 * 1. Data Acknowledgement Files (DPT-URA-LOG-D2-*)
 *    - Confirms printing status for data files across all stages
 *    - Contains successful/failed notice counts and error details
 *    - Includes stage information extracted from data file references
 * 
 * 2. PDF Acknowledgement Files (DPT-URA-LOG-PDF-*)
 *    - Similar to data acknowledgements but for PDF letter files
 *    - Same structure with PDF-specific processing information
 * 
 * 3. Registered Mail Return Files (DPT-URA-RD2-D2-*, DPT-URA-DN2-D2-*)
 *    - Contains postal registration numbers for registered mail
 *    - Only available for RD2 and DN2 stages (second reminders)
 *    - Uses Header-Detail-Trailer format with fixed-width records
 * 
 * 4. PDF Registered Mail Return Files (DPT-URA-PDF-D2-*)
 *    - Similar to registered mail returns but for PDF letters
 *    - Contains registration numbers for PDF-based registered mail
 * 
 * PARSING FEATURES:
 * - Automatic file type detection based on filename patterns
 * - Stage extraction from embedded data file references
 * - Notice number extraction with validation
 * - Error section parsing to identify failed notices
 * - Overseas address handling (marked as successful)
 * - Postal registration number extraction for registered mail
 * - Comprehensive error handling and logging
 * 
 * FILE FORMAT HANDLING:
 * - Processes byte arrays from decrypted SLIFT files
 * - Supports UTF-8 encoding for international characters
 * - Handles various line endings and formatting variations
 * - Robust parsing with fallback mechanisms for unexpected formats
 * 
 * BUSINESS LOGIC:
 * - Distinguishes between successful and failed notices
 * - Preserves processing statistics for monitoring
 * - Handles partial success scenarios appropriately
 * - Maintains traceability with detailed logging
 */
@Slf4j
@Component
public class ToppanResponseParser {
    
    /**
     * Regular expression patterns for parsing various elements from response files.
     * These patterns are designed to handle Toppan's specific file formats and
     * extract key information reliably.
     */
    
    // Extracts stage information from data file references in acknowledgement files
    // Example: "Data File : URA-DPT-RD2-D1-20241201120000" -> extracts "RD2"
    private static final Pattern DATA_FILE_PATTERN = Pattern.compile("Data File\\s*:\\s*(URA-DPT-(\\w+)-D1-\\d+)");
    
    // Extracts processing status from status lines
    // Example: "Status : Successfully Processed" -> extracts "Successfully Processed"
    private static final Pattern STATUS_PATTERN = Pattern.compile("Status\\s*:\\s*(.+)");
    
    // Extracts total printed count from summary lines
    // Example: "Total accounts printed : 150" -> extracts "150"
    private static final Pattern TOTAL_PRINTED_PATTERN = Pattern.compile("Total accounts printed\\s*:\\s*(\\d+)");
    
    // Matches notice numbers in detail lines (9 digits + 1 letter format)
    // Example: "123456789A    Additional details..." -> extracts "123456789A"
    private static final Pattern NOTICE_PATTERN = Pattern.compile("^(\\d{9}[A-Z])\\s+");
    
    // Matches notice numbers in error lines with additional error details
    // Used for failed notice identification
    private static final Pattern ERROR_NOTICE_PATTERN = Pattern.compile("^(\\d{9}[A-Z])\\s+.+");
    
    /**
     * Main entry point for parsing any type of Toppan response file.
     * 
     * This method automatically detects the file type based on filename patterns
     * and routes to the appropriate parsing method. It handles all supported
     * file formats and provides a consistent return structure.
     * 
     * FILE TYPE DETECTION:
     * - DPT-URA-LOG-D2-* : Data acknowledgement files
     * - DPT-URA-LOG-PDF-* : PDF acknowledgement files  
     * - DPT-URA-RD2-D2-* : RD2 registered mail return files
     * - DPT-URA-DN2-D2-* : DN2 registered mail return files
     * - DPT-URA-PDF-D2-* : PDF registered mail return files
     * 
     * RETURN STRUCTURE:
     * - stage: The workflow stage this response relates to
     * - status: Processing status from Toppan
     * - isSuccessful: Boolean indicating overall success
     * - successfulNotices: List of successfully processed notice numbers
     * - failedNotices: List of failed notice numbers
     * - successfulCount: Total count of successful notices
     * - postalRegistrationNumbers: Map of notice to registration number (RD2/DN2 only)
     * - fileType: Type of file processed for debugging
     * - error: Error message if parsing failed
     * 
     * @param fileContent The decrypted file content as byte array
     * @param fileName The original filename without .p7 extension for type detection
     * @return Map containing comprehensive parsing results
     */
    public Map<String, Object> parseResponseFileContent(byte[] fileContent, String fileName) {
        log.info("Parsing response file: {}", fileName);

        // Initialize result containers for all parsing methods
        Map<String, Object> result = new HashMap<>();
        List<String> successfulNotices = new ArrayList<>();
        List<String> failedNotices = new ArrayList<>();

        // TP-FMT-08: Detect empty files early
        if (fileContent == null || fileContent.length == 0) {
            result.put("error", "EMPTY_FILE");
            log.error("Empty file detected: {}", fileName);
            return result;
        }

        try {
            // Convert filename to uppercase for consistent pattern matching
            String upperFileName = fileName.toUpperCase();
            
            // Route to appropriate parser based on Toppan's filename conventions
            if (upperFileName.startsWith("DPT-URA-LOG-D2-")) {
                // Data acknowledgement files - contain processing results for all stages
                parseAcknowledgementFileContent(fileContent, result, successfulNotices, failedNotices);
                
            } else if (upperFileName.startsWith("DPT-URA-LOG-PDF-")) {
                // PDF acknowledgement files - similar to data ack but for PDF letters
                parsePdfAcknowledgementFileContent(fileContent, result, successfulNotices, failedNotices);
                
            } else if (upperFileName.startsWith("DPT-URA-RD2-D2-")) {
                // RD2 registered mail return files - contain postal registration numbers
                parseRegisteredMailReturnFileContent(fileContent, SystemConstant.SuspensionReason.RD2, result, successfulNotices);
                
            } else if (upperFileName.startsWith("DPT-URA-DN2-D2-")) {
                // DN2 registered mail return files - contain postal registration numbers
                parseRegisteredMailReturnFileContent(fileContent, SystemConstant.SuspensionReason.DN2, result, successfulNotices);
                
            } else if (upperFileName.startsWith("DPT-URA-PDF-D2-")) {
                // PDF registered mail return files - registration numbers for PDF letters
                parsePdfRegisteredMailReturnFileContent(fileContent, result, successfulNotices);

            } else {
                // TP-FMT-10: Return error for unsupported file types
                result.put("error", "UNSUPPORTED_TYPE");
                log.warn("Unsupported file type, skipping: {}. Allowed types: DPT-URA-LOG-D2-*, DPT-URA-LOG-PDF-*, DPT-URA-RD2-D2-*, DPT-URA-DN2-D2-*, DPT-URA-PDF-D2-*", fileName);
                return result;
            }
            
            // Log parsing results for monitoring and debugging
            log.info("Parsed response file: Stage={}, Status={}, Successful={}, Failed={}", 
                result.get("stage"), result.get("status"), 
                successfulNotices.size(), failedNotices.size());
            
            return result;
            
        } catch (Exception e) {
            // Handle parsing errors gracefully - return error information for debugging
            log.error("Error parsing response file {}: {}", fileName, e.getMessage(), e);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Parses data acknowledgement files that confirm printing and dispatch status.
     * 
     * These files are the primary response from Toppan for all workflow stages and contain
     * comprehensive information about the processing results including successful prints,
     * failed prints, and various processing statistics.
     * 
     * FILE STRUCTURE:
     * - Header with data file reference (contains stage information)
     * - Processing status and summary statistics
     * - Successful records section (may be implicit)
     * - Error report section with failed notice details
     * - Overseas address report section (still successful)
     * - Summary totals and counts
     * 
     * STAGE EXTRACTION:
     * Stage information is embedded in data file references like:
     * "Data File : URA-DPT-RD2-D1-20241201120000" where RD2 is the stage
     * 
     * ERROR HANDLING:
     * - Failed notices are explicitly listed in error sections
     * - Overseas addresses are flagged but considered successful
     * - Processing continues even if some sections cannot be parsed
     * 
     * @param fileContent Decrypted file content to parse
     * @param result Result map to populate with parsing results
     * @param successfulNotices List to populate with successful notice numbers
     * @param failedNotices List to populate with failed notice numbers
     * @throws Exception if file cannot be read or parsed
     */
    private void parseAcknowledgementFileContent(byte[] fileContent, Map<String, Object> result,
                                                List<String> successfulNotices, List<String> failedNotices) throws Exception {
        // Create buffered reader for line-by-line processing with UTF-8 encoding
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(new java.io.ByteArrayInputStream(fileContent), "UTF-8"))) {
            String line;
            
            // State variables for tracking parsing context
            String currentStage = null;          // Extracted from data file reference
            String currentStatus = null;         // Overall processing status
            int totalPrinted = 0;               // Total successfully printed notices
            int totalErrors = 0;                // Total failed notices
            boolean inErrorSection = false;     // Currently parsing error report section
            boolean inOverseasSection = false;  // Currently parsing overseas address section
            
            // Process file line by line to extract all relevant information
            while ((line = reader.readLine()) != null) {
                // Extract stage information from data file references
                // This is critical for determining which workflow stage this response relates to
                // Format: "Data File : URA-DPT-{STAGE}-D1-YYYYMMDDHHMMSS"
                // where STAGE can be RD1, RD2, RR3, DN1, DN2, DR3
                Matcher dataFileMatcher = DATA_FILE_PATTERN.matcher(line);
                if (dataFileMatcher.find()) {
                    currentStage = dataFileMatcher.group(2); // Extract stage identifier
                    log.debug("Found stage: {}", currentStage);
                }
                
                // Track which section of the file we're currently parsing
                // This determines how to interpret notice numbers found in each section
                
                // Error section contains notices that failed to print or dispatch
                if (line.contains("Error Report") || line.contains("accounts with error")) {
                    inErrorSection = true;
                    inOverseasSection = false;
                    continue;
                }
                
                // Overseas section contains notices with overseas addresses
                // These are flagged for attention but considered successfully processed
                if (line.contains("Overseas Address Report")) {
                    inOverseasSection = true;
                    inErrorSection = false;
                    continue;
                }
                
                // End of special sections - return to normal processing
                if (line.contains("Successfully Processed") || line.contains("Summary Report")) {
                    inErrorSection = false;
                    inOverseasSection = false;
                }
                
                // Extract error count from summary lines
                // This provides the total number of notices that failed processing
                if (line.contains("Total accounts with error")) {
                    Pattern errorCountPattern = Pattern.compile("Total accounts with error.*:\s*(\\d+)");
                    Matcher errorMatcher = errorCountPattern.matcher(line);
                    if (errorMatcher.find()) {
                        totalErrors = Integer.parseInt(errorMatcher.group(1));
                    }
                }
                
                // Extract individual notice numbers and categorize based on current section
                Matcher noticeMatcher = NOTICE_PATTERN.matcher(line);
                if (noticeMatcher.find()) {
                    String noticeNo = noticeMatcher.group(1);
                    
                    if (inErrorSection) {
                        // Notices in error section failed to print or dispatch
                        failedNotices.add(noticeNo);
                        log.debug("Found failed notice: {}", noticeNo);
                        
                    } else if (inOverseasSection) {
                        // Overseas addresses require special handling but are still successful
                        // They may need manual processing but printing was completed
                        successfulNotices.add(noticeNo);
                        log.debug("Found overseas notice (successful): {}", noticeNo);
                    }
                    // Note: Regular successful notices may not be individually listed
                    // They are typically calculated as totalPrinted - totalErrors
                }
                
                // Extract overall processing status from Toppan
                Matcher statusMatcher = STATUS_PATTERN.matcher(line);
                if (statusMatcher.find()) {
                    currentStatus = statusMatcher.group(1).trim();
                    log.debug("Found status: {}", currentStatus);
                }
                
                // Extract total count of printed notices
                // This is the key metric for determining overall success
                Matcher totalMatcher = TOTAL_PRINTED_PATTERN.matcher(line);
                if (totalMatcher.find()) {
                    totalPrinted = Integer.parseInt(totalMatcher.group(1));
                    log.debug("Found total printed: {}", totalPrinted);
                }
            }
            
            // Populate result map with extracted information
            
            // Stage information is critical for workflow routing
            if (currentStage != null) {
                result.put("stage", currentStage);
                log.info("Processing acknowledgement for stage: {}", currentStage);
            } else {
                log.warn("Could not determine stage from acknowledgement file");
            }
            
            // Determine overall success based on status and error indicators
            if (currentStatus != null) {
                result.put("status", currentStatus);
                // Multiple indicators of failure: explicit error status, error count, or failed notices
                boolean hasErrors = currentStatus.toLowerCase().contains("error") || totalErrors > 0 || !failedNotices.isEmpty();
                result.put("isSuccessful", !hasErrors);
            } else {
                // Fallback success determination when status is not explicitly stated
                result.put("isSuccessful", totalErrors == 0 && failedNotices.isEmpty());
            }
            
            // Store all extracted statistics and notice lists
            result.put("totalPrinted", totalPrinted);
            result.put("totalErrors", totalErrors);
            result.put("successfulNotices", successfulNotices);
            result.put("failedNotices", failedNotices);
            
            // Handle cases where successful count is available but individual notices aren't listed
            // This is common in summary-only acknowledgement files
            int successfulCount = totalPrinted - totalErrors;
            if (successfulCount > 0 && successfulNotices.isEmpty()) {
                // We know the count but don't have individual notice numbers
                // This may require additional processing or database queries to identify specific notices
                log.info("Successfully printed {} notices for stage {} (individual notice details not available)", 
                        successfulCount, currentStage);
                result.put("successfulCount", successfulCount);
            } else {
                // We have detailed information about individual notices
                log.info("Parsed {} successful and {} failed notices for stage {}", 
                    successfulNotices.size(), failedNotices.size(), currentStage);
            }
        }
    }
    
    /**
     * Parses PDF acknowledgement files with the same logic as data acknowledgements.
     * 
     * PDF acknowledgement files have the same structure and content as data acknowledgement
     * files but relate to PDF-format letters instead of data files. The parsing logic
     * is identical, but we flag the file type for proper categorization.
     * 
     * @param fileContent Decrypted PDF acknowledgement file content
     * @param result Result map to populate with parsing results
     * @param successfulNotices List to populate with successful notice numbers
     * @param failedNotices List to populate with failed notice numbers
     * @throws Exception if file cannot be read or parsed
     */
    private void parsePdfAcknowledgementFileContent(byte[] fileContent, Map<String, Object> result,
                                                   List<String> successfulNotices, List<String> failedNotices) throws Exception {
        // Reuse data acknowledgement parsing logic - same format and structure
        parseAcknowledgementFileContent(fileContent, result, successfulNotices, failedNotices);
        
        // Flag this as a PDF file for proper categorization and tracking
        result.put("fileType", "PDF");
    }
    
    /**
     * Parses registered mail return files containing postal registration numbers.
     * 
     * These files are only generated for RD2 and DN2 stages, which use registered mail
     * for enhanced delivery tracking. The files contain postal registration numbers
     * that can be used to track delivery status with the postal service.
     * 
     * FILE FORMAT:
     * - Header record with file information
     * - Detail records with notice number and postal registration number pairs
     * - Trailer record with record count validation
     * 
     * RECORD STRUCTURE:
     * - Header: H + file metadata
     * - Detail: D + Notice Number (10 chars) + Postal Registration Number (20 chars) + other data
     * - Trailer: T + record count + other summary data
     * 
     * BUSINESS IMPORTANCE:
     * Postal registration numbers are critical for:
     * - Tracking delivery confirmation
     * - Legal proof of service for court proceedings
     * - Compliance with registered mail requirements
     * 
     * @param fileContent Decrypted registered mail return file content
     * @param stage The stage this file relates to (RD2 or DN2)
     * @param result Result map to populate with parsing results
     * @param successfulNotices List to populate with notice numbers that have registration numbers
     * @throws Exception if file cannot be read or parsed
     */
    private void parseRegisteredMailReturnFileContent(byte[] fileContent, String stage, Map<String, Object> result,
                                                     List<String> successfulNotices) throws Exception {
        // Map to store notice number to postal registration number mappings
        Map<String, String> postalRegistrationNumbers = new HashMap<>();
        
        // TP-FMT-07: Process file using UTF-8 encoding with explicit error handling
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(new java.io.ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {
            String line;
            int recordCount = 0;        // Count of detail records processed
            boolean sawTrailer = false; // Track if trailer was found

            // TP-FMT-01: Validate header record (first line must be header)
            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.startsWith("H")) {
                result.put("error", "HEADER_INVALID");
                log.error("Header record missing or invalid for stage {}. First line: {}", stage,
                    headerLine != null ? headerLine.substring(0, Math.min(20, headerLine.length())) : "null");
                return;
            }

            // Validate header format per Toppan specs
            if (!validateHeaderFormat(headerLine, stage)) {
                result.put("error", "HEADER_INVALID");
                log.error("Header record format validation failed for stage {}", stage);
                return;
            }

            log.debug("Valid header found for stage {}", stage);

            // Process each line according to the Header-Detail-Trailer format
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // TP-FMT-05 & TP-FMT-06: Process and validate trailer record
                if (line.startsWith("T")) {
                    sawTrailer = true;

                    // Per toppanSpecs.txt, trailer format:
                    // T + Printed(N7) + Rejected(N7) + Total(N7) + Filler
                    // Position 1-7: Total Printed, Position 8-14: Total Rejected, Position 15-21: Total from URA
                    if (line.length() >= 22) {
                        try {
                            String content = line.substring(1);
                            int totalPrinted = Integer.parseInt(content.substring(0, 7).trim());
                            int totalRejected = Integer.parseInt(content.substring(7, 14).trim());
                            int totalFromURA = Integer.parseInt(content.substring(14, 21).trim());

                            log.debug("Trailer: Printed={}, Rejected={}, Total={}, Parsed={}",
                                totalPrinted, totalRejected, totalFromURA, recordCount);

                            // TP-FMT-05: Validate trailer count matches parsed detail count
                            // Note: totalPrinted in trailer may not match actual detail records in file
                            // because Toppan may indicate multiple copies printed for same notice
                            // We validate that we parsed at least some records if trailer says printed > 0
                            if (totalPrinted > 0 && recordCount == 0) {
                                result.put("error", "TRAILER_COUNT_MISMATCH");
                                log.error("Trailer printed count {} but no records parsed for stage {}",
                                    totalPrinted, stage);
                                return;
                            }

                            // Log warning if counts don't match but don't fail
                            if (totalPrinted != recordCount) {
                                log.warn("Trailer printed count {} does not match parsed record count {} for stage {} - this may be expected if multiple copies printed",
                                    totalPrinted, recordCount, stage);
                            }

                            // TP-FMT-06: Validate trailer totals consistency (Printed + Rejected should not exceed Total)
                            // Note: Total is the original batch size sent to Toppan, not Printed + Rejected
                            // This validation ensures counts are reasonable
                            if (totalPrinted + totalRejected > totalFromURA) {
                                result.put("error", "TRAILER_COUNT_MISMATCH");
                                log.error("Trailer totals inconsistent: Printed({}) + Rejected({}) > Total({}) for stage {}",
                                    totalPrinted, totalRejected, totalFromURA, stage);
                                return;
                            }

                            // TP-FMT-04b: Position 22 onwards: Filler (A216) - should be all spaces
                            if (line.length() > 22) {
                                String filler = line.substring(22);
                                // Check if filler contains non-space characters
                                if (!filler.matches("^\\s*$")) {
                                    result.put("error", "FIELD_INVALID_FILLER");
                                    log.error("Trailer filler field contains non-space characters at position 22+ for stage {}", stage);
                                    return;
                                }
                            }

                            log.debug("Trailer validation passed for stage {}", stage);

                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            result.put("error", "TRAILER_INVALID");
                            log.error("Invalid trailer format for stage {}: {}", stage, e.getMessage());
                            return;
                        }
                    } else {
                        result.put("error", "TRAILER_INVALID");
                        log.error("Trailer too short: {} chars (expected at least 22)", line.length());
                        return;
                    }

                    break; // End of file
                }
                
                // Process detail records containing notice and registration number pairs
                if (line.startsWith("D")) {
                    // Parse the fixed-width detail record to extract key information
                    Map<String, String> recordData = parseRegisteredMailRecord(line);

                    // Skip records with validation errors (missing data, invalid format, invalid symbols)
                    if (recordData.containsKey("error")) {
                        log.warn("Skipping invalid detail record: {}", recordData.get("error"));
                        continue;
                    }

                    String noticeNo = recordData.get("noticeNo");
                    String registrationNo = recordData.get("registrationNo");

                    if (noticeNo != null) {
                        // All notices in this file are successful (they have registration numbers)
                        successfulNotices.add(noticeNo);

                        if (registrationNo != null) {
                            // Store the postal registration number for delivery tracking
                            postalRegistrationNumbers.put(noticeNo, registrationNo);
                            log.debug("Found registered mail for notice: {} with registration: {} (stage: {})",
                                    noticeNo, registrationNo, stage);
                        }
                        recordCount++;
                    }
                }
            }

            // TP-FMT-09: Check if trailer was found
            if (!sawTrailer) {
                result.put("error", "TRAILER_MISSING");
                log.error("Trailer record missing in registered mail file for stage {}", stage);
                return;
            }

            // Populate result with parsed information
            result.put("stage", stage);
            result.put("isSuccessful", true);  // All notices in return files are successful
            result.put("successfulNotices", successfulNotices);
            result.put("postalRegistrationNumbers", postalRegistrationNumbers);
            result.put("recordCount", recordCount);
            result.put("fileType", "REGISTERED_MAIL_RETURN");
            
            log.info("Parsed {} registered mail records for stage {} with {} registration numbers",
                    recordCount, stage, postalRegistrationNumbers.size());
        } catch (CharacterCodingException encEx) {
            // TP-FMT-07: Handle encoding errors explicitly
            result.put("error", "ENCODING_INVALID");
            log.error("Invalid encoding in registered mail file for stage {}: {}", stage, encEx.getMessage());
        }
    }
    
    /**
     * Parses an individual detail record from a registered mail return file.
     * 
     * Detail records use a fixed-width format with specific field positions:
     * - Position 0: Record type indicator ('D')
     * - Positions 1-10: Notice number (10 characters)
     * - Positions 11-30: Postal registration number (20 characters)
     * - Additional positions may contain other metadata
     * 
     * This method extracts the critical notice number and postal registration number
     * that are needed for tracking and database updates.
     * 
     * @param line The detail record line to parse
     * @return Map containing 'noticeNo' and 'registrationNo' if successfully parsed
     */
    private Map<String, String> parseRegisteredMailRecord(String line) {
        Map<String, String> data = new HashMap<>();

        // Parse fixed-width record format per toppanSpecs.txt
        // Total: 238 chars = D(1) + Date(10) + Notice(10) + Name(66) + NRIC(20) +
        //        Block(10) + Street(32) + Floor(2) + Unit(5) + Building(30) +
        //        Postal(6) + Vehicle(12) + DateTime(19) + PostalReg(15)

        if (line == null || line.length() <= 1 || !line.startsWith("D")) {
            return data;
        }

        // Skip the 'D' record type indicator
        String content = line.substring(1);

        // Minimum length check - need at least Date(10) + Notice(10) = 20 chars
        if (content.length() < 20) {
            data.put("error", "FIELD_MISSING_DATA");
            log.warn("Detail record too short: {} chars (expected at least 20)", content.length());
            return data;
        }

        // TP-FMT-04: Field 2 - Position 1-10: Date of Driver Notice (A10, DD-MM-YYYY)
        String dateOfNotice = content.substring(0, 10).trim();
        if (!dateOfNotice.isEmpty()) {
            if (!dateOfNotice.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
                data.put("error", "FIELD_INVALID_TYPE_DATE");
                log.warn("Invalid date format: {} (expected DD-MM-YYYY)", dateOfNotice);
                return data;
            }
            // Validate date range
            String[] dateParts = dateOfNotice.split("-");
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);
            if (day < 1 || day > 31 || month < 1 || month > 12 || year < 2000 || year > 2099) {
                data.put("error", "FIELD_INVALID_TYPE_DATE");
                log.warn("Invalid date values: {}-{}-{}", day, month, year);
                return data;
            }
            data.put("dateOfNotice", dateOfNotice);
        }

        // TP-FMT-04: Field 3 - Position 11-20: Notice No (A10) - CRITICAL FIELD
        String noticeNo = content.substring(10, 20).trim();

            // Critical Fields For Missing Data Check
            if (noticeNo.isEmpty()) {
                data.put("error", "FIELD_MISSING_NOTICE");
                log.warn("Missing notice number in detail record");
                return data;
            }

            // Critical Fields for Symbols Check - notice number should be alphanumeric only
            if (!noticeNo.matches("^[0-9A-Z]+$")) {
                data.put("error", "FIELD_INVALID_SYMBOLS_NOTICE");
                log.warn("Invalid symbols in notice number: {}", noticeNo);
                return data;
            }

            // The format must be appropriate - notice number should be exactly 10 chars (9 digits + 1 letter typical)
            if (noticeNo.length() != 10 || !noticeNo.matches("^[0-9]{9}[A-Z]$")) {
                data.put("error", "FIELD_INVALID_FORMAT_NOTICE");
                log.warn("Invalid notice number format: {} (expected 9 digits + 1 letter)", noticeNo);
                return data;
            }

        data.put("noticeNo", noticeNo);

        // TP-RD2-NRIC-01 & TP-DN2-NRIC-01: Field 5 - Position 87-106: NRIC (A20) - CRITICAL FIELD
        if (content.length() >= 106) {
            String nric = content.substring(86, 106).trim();

            // Critical Fields For Missing Data Check - NRIC is required
            if (nric.isEmpty()) {
                data.put("error", "FIELD_MISSING_NRIC");
                log.warn("Missing NRIC in detail record");
                return data;
            }

            // The format must be appropriate - NRIC up to 20 chars, alphanumeric
            if (nric.length() > 20) {
                data.put("error", "FIELD_INVALID_LENGTH_NRIC");
                log.warn("NRIC exceeds 20 characters: {} chars", nric.length());
                return data;
            }

            // Critical Fields for Symbols Check - NRIC should be alphanumeric only
            if (!nric.matches("^[0-9A-Z]+$")) {
                data.put("error", "FIELD_INVALID_SYMBOLS_NRIC");
                log.warn("Invalid symbols in NRIC: {}", nric);
                return data;
            }

            data.put("nric", nric);
        } else {
            // If record is too short to contain NRIC field, treat as missing
            data.put("error", "FIELD_MISSING_NRIC");
            log.warn("Detail record too short to contain NRIC field: {} chars (expected at least 106)", content.length());
            return data;
        }

        // TP-FMT-04: Field 11 - Position 186-191: Postal Code (A6) - Numeric validation
        if (content.length() >= 191) {
            String postalCode = content.substring(185, 191).trim();
            if (!postalCode.isEmpty() && !postalCode.matches("^\\d{6}$")) {
                data.put("error", "FIELD_INVALID_TYPE_POSTAL");
                log.warn("Invalid postal code: {} (expected 6 digits)", postalCode);
                return data;
            }
            if (!postalCode.isEmpty()) {
                data.put("postalCode", postalCode);
            }
        }

        // TP-FMT-04: Field 13 - Position 204-222: Notice Date & Time (A19, DD-MM-YYYY HH:MI AM)
        if (content.length() >= 222) {
            String noticeDateTime = content.substring(203, 222).trim();
            if (!noticeDateTime.isEmpty()) {
                if (!noticeDateTime.matches("^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2} (AM|PM)$")) {
                    data.put("error", "FIELD_INVALID_TYPE_DATETIME");
                    log.warn("Invalid notice date/time: {} (expected DD-MM-YYYY HH:MI AM/PM)", noticeDateTime);
                    return data;
                }
                data.put("noticeDateTime", noticeDateTime);
            }
        }

        // Field 14 - Position 223-237: Postal Registration Numbers (A15)
        if (content.length() >= 237) {
            String regNo = content.substring(222, 237).trim();

            if (!regNo.isEmpty()) {
                // Critical Fields for Symbols Check - registration number should be alphanumeric only (no special chars)
                if (!regNo.matches("^[0-9A-Z]+$")) {
                    data.put("error", "FIELD_INVALID_SYMBOLS_REG");
                    log.warn("Invalid symbols in registration number: {}", regNo);
                    return data;
                }

                // The format must be appropriate - per specs: A15 max length
                // Typical format: 2 letters + 9 digits + 2 letters (e.g., RR862063526SG = 13 chars)
                if (regNo.length() > 15) {
                    data.put("error", "FIELD_INVALID_FORMAT_REG");
                    log.warn("Invalid registration number length: {} (max 15 chars per spec)", regNo);
                    return data;
                }

                // Validate typical postal registration format: 2 letters + 9 digits + 2 letters
                if (regNo.length() == 13 && !regNo.matches("^[A-Z]{2}[0-9]{9}[A-Z]{2}$")) {
                    data.put("error", "FIELD_INVALID_FORMAT_REG");
                    log.warn("Invalid registration number format: {} (expected format: RR862063526SG)", regNo);
                    return data;
                }

                data.put("registrationNo", regNo);
            }
            // Note: Registration number is optional, so no error if empty
        }

        return data;
    }

    /**
     * Validates header record format according to Toppan specifications.
     *
     * Per Toppan specs, header record structure:
     * - Position 0: Record Type (A1) = 'H'
     * - Position 1-8: Date Of Run (N8) = YYYYMMDD
     * - Position 9-12: Time of Run (N4) = HHMM (24-hour format)
     * - Position 13-17: File Type (A5) = 'DN2' or 'RD2' (left justified)
     * - Position 18-237: Filler (A220) = Spaces
     *
     * @param headerLine The header line to validate
     * @param expectedStage The expected stage (RD2 or DN2)
     * @return true if header format is valid, false otherwise
     */
    private boolean validateHeaderFormat(String headerLine, String expectedStage) {
        try {
            // Minimum required length for header validation
            if (headerLine.length() < 18) {
                log.warn("Header line too short: {} chars (expected at least 18)", headerLine.length());
                return false;
            }

            // Position 1-8: Date Of Run (YYYYMMDD format)
            String dateOfRun = headerLine.substring(1, Math.min(9, headerLine.length()));
            if (!dateOfRun.matches("^\\d{8}$")) {
                log.warn("Invalid date format in header: {} (expected YYYYMMDD)", dateOfRun);
                return false;
            }

            // Validate date is reasonable (year 2000-2099, month 01-12, day 01-31)
            int year = Integer.parseInt(dateOfRun.substring(0, 4));
            int month = Integer.parseInt(dateOfRun.substring(4, 6));
            int day = Integer.parseInt(dateOfRun.substring(6, 8));
            if (year < 2000 || year > 2099 || month < 1 || month > 12 || day < 1 || day > 31) {
                log.warn("Invalid date values in header: {}-{}-{}", year, month, day);
                return false;
            }

            // Position 9-12: Time of Run (HHMM format in 24-hour)
            if (headerLine.length() >= 13) {
                String timeOfRun = headerLine.substring(9, 13);
                if (!timeOfRun.matches("^\\d{4}$")) {
                    log.warn("Invalid time format in header: {} (expected HHMM)", timeOfRun);
                    return false;
                }

                // Validate time is reasonable (HH: 00-23, MM: 00-59)
                int hour = Integer.parseInt(timeOfRun.substring(0, 2));
                int minute = Integer.parseInt(timeOfRun.substring(2, 4));
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    log.warn("Invalid time values in header: {}:{}", hour, minute);
                    return false;
                }
            }

            // Position 13-17: File Type (A5) - should match expected stage
            if (headerLine.length() >= 18) {
                String fileType = headerLine.substring(13, 18).trim();
                if (!fileType.equals(expectedStage)) {
                    log.warn("File type mismatch in header: {} (expected {})", fileType, expectedStage);
                    // Warning but not failure - stage might be 'PDF' or other valid types
                }
            }

            // TP-FMT-04b: Position 18 onwards: Filler (A220) - should be all spaces
            if (headerLine.length() > 18) {
                String filler = headerLine.substring(18);
                // Check if filler contains non-space characters
                if (!filler.matches("^\\s*$")) {
                    log.warn("Header filler field contains non-space characters at position 18+");
                    return false;
                }
            }

            log.debug("Header validation passed for stage {}", expectedStage);
            return true;

        } catch (Exception e) {
            log.error("Error validating header format: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parses PDF registered mail return files using the same logic as regular return files.
     * 
     * PDF registered mail return files have the same structure and format as regular
     * registered mail return files but relate to PDF-format letters. The parsing
     * logic is identical.
     * 
     * @param fileContent Decrypted PDF registered mail return file content
     * @param result Result map to populate with parsing results
     * @param successfulNotices List to populate with successful notice numbers
     * @throws Exception if file cannot be read or parsed
     */
    private void parsePdfRegisteredMailReturnFileContent(byte[] fileContent, Map<String, Object> result,
                                                        List<String> successfulNotices) throws Exception {
        // Reuse registered mail parsing logic - same format for PDF files
        parseRegisteredMailReturnFileContent(fileContent, "PDF", result, successfulNotices);
        
        // Override file type to distinguish PDF registered mail returns
        result.put("fileType", "PDF_REGISTERED_MAIL_RETURN");
    }
    
    /**
     * Extracts notice numbers from detail lines using pattern matching.
     * 
     * Notice numbers follow a standard format of 9 digits followed by 1 uppercase letter.
     * This method uses regex pattern matching to reliably extract notice numbers
     * from various line formats.
     * 
     * @param line The line to extract notice number from
     * @return The notice number if found, null otherwise
     */
    private String extractNoticeNumber(String line) {
        // Standard notice number format: 9 digits + 1 uppercase letter (e.g., 123456789A)
        Pattern pattern = Pattern.compile("(\\d{9}[A-Z])");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    
    /**
     * Fallback parser for response files that don't match known formats.
     * 
     * This method attempts to extract basic information from response files using
     * generic parsing patterns. It's used when the filename doesn't match any of
     * the expected Toppan file naming conventions.
     * 
     * The parser makes best-effort attempts to:
     * - Extract stage information from data file references
     * - Identify processing status
     * - Distinguish between successful and failed notices
     * - Handle error sections appropriately
     * 
     * This provides resilience against unexpected file formats while maintaining
     * basic functionality.
     * 
     * @param fileContent Decrypted file content to parse
     * @param result Result map to populate with parsing results
     * @param successfulNotices List to populate with successful notice numbers
     * @param failedNotices List to populate with failed notice numbers
     * @throws Exception if file cannot be read
     */
    private void parseGeneralResponseFileContent(byte[] fileContent, Map<String, Object> result,
                                                List<String> successfulNotices, List<String> failedNotices) throws Exception {
        
        // Read entire file into memory for multi-pass processing
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(new java.io.ByteArrayInputStream(fileContent), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        
        // Initialize parsing state variables
        String currentStage = null;
        String currentStatus = "Unknown";
        boolean processingErrors = false;  // Track whether we're in an error section
        
        // Process all lines using generic patterns
        for (String line : lines) {
            // Attempt to extract stage information using standard pattern
            Matcher dataFileMatcher = DATA_FILE_PATTERN.matcher(line);
            if (dataFileMatcher.find()) {
                currentStage = dataFileMatcher.group(2);
                processingErrors = false;  // Reset error state for new section
            }
            
            // Extract processing status information
            Matcher statusMatcher = STATUS_PATTERN.matcher(line);
            if (statusMatcher.find()) {
                currentStatus = statusMatcher.group(1).trim();
            }
            
            // Detect error sections to properly categorize notices
            if (line.contains("Error Report") || line.contains("accounts with error")) {
                processingErrors = true;
            } else if (line.contains("Successfully Processed") || line.contains("Total accounts printed")) {
                processingErrors = false;
            }
            
            // Extract notice numbers and categorize based on section
            Matcher noticeMatcher = NOTICE_PATTERN.matcher(line);
            if (noticeMatcher.find()) {
                String noticeNo = noticeMatcher.group(1);
                if (processingErrors) {
                    failedNotices.add(noticeNo);
                } else {
                    // Default assumption: notices are successful unless in error section
                    successfulNotices.add(noticeNo);
                }
            }
        }
        
        // Populate result with best-effort parsing results
        result.put("stage", currentStage);
        result.put("status", currentStatus);
        result.put("isSuccessful", currentStatus.toLowerCase().contains("success"));
        result.put("successfulNotices", successfulNotices);
        result.put("failedNotices", failedNotices);
        result.put("fileType", "GENERIC");  // Flag as generic parsing for debugging
    }
}