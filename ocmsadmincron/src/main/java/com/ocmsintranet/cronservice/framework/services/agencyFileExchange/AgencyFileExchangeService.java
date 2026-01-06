package com.ocmsintranet.cronservice.framework.services.agencyFileExchange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for processing agency file exchanges - both creating request files and parsing response files.
 * This service handles the file format conversions but does not handle the actual file transfer.
 * The workflow is responsible for coordinating between this service and the SLIFT service.
 */
public interface AgencyFileExchangeService {

    /**
     * Creates a request file content for an agency from the provided data.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param records List of records from TableQueryService to include in the request
     * @param timestamp Timestamp to use in the filename (or null to generate one)
     * @return Byte array containing the file content
     */
    byte[] createRequestFileContent(String agencyType, List<Map<String, Object>> records, LocalDateTime timestamp);
    
    /**
     * Parses an agency response file content and returns the structured data.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param fileContent Byte array containing the response file content
     * @return List of parsed response records
     */
    List<Map<String, Object>> parseResponseFileContent(String agencyType, byte[] fileContent);
    
    /**
     * Parses an agency report file content (e.g., MHA .TOT file) and returns the summary statistics.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param fileContent Byte array containing the report file content
     * @return Map containing the summary statistics
     */
    Map<String, Object> parseReportFileContent(String agencyType, byte[] fileContent);
    
    /**
     * Parses an agency exceptions file content (e.g., MHA .EXP file) and returns the exception records.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param fileContent Byte array containing the exceptions file content
     * @return List of exception records
     */
    List<Map<String, Object>> parseExceptionsFileContent(String agencyType, byte[] fileContent);
    
    /**
     * Generates a filename for an agency request file based on the provided timestamp.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param timestamp Timestamp to use (or null to use current time)
     * @return Generated filename in the agency-specific format
     */
    String generateRequestFilename(String agencyType, LocalDateTime timestamp);
    
    /**
     * Validates if file content appears to be a valid agency response file.
     * 
     * @param agencyType The type of agency (e.g., "MHA", "LTA", "OPPAN")
     * @param filename The filename of the file
     * @param fileContent Byte array containing the file content
     * @return true if the file appears to be a valid response file for the specified agency
     */
    boolean isValidResponseFile(String agencyType, String filename, byte[] fileContent);
}
