package com.ocmsintranet.cronservice.testing.agencies.mha.helpers;

import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Helper class for MHA test database operations.
 * Contains methods for creating and verifying test records in the database.
 */
@Component
public class MhaTestDatabaseHelper {

    private final JdbcTemplate jdbcTemplate;
    private String schema;
    private String testFilesPath;
    private boolean createPhysicalFiles = true;
    private String datFile;
    private String encryptedFile;

    /**
     * Constructor for MhaTestDatabaseHelper
     *
     * @param jdbcTemplate JdbcTemplate for database operations
     * @param schema Database schema name
     */
    @Autowired
    public MhaTestDatabaseHelper(
            JdbcTemplate jdbcTemplate,
            @Value("${mha.test.schema:ocmsizmgr}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    /**
     * Set the test files path
     *
     * @param testFilesPath Path to test files directory
     */
    public void setTestFilesPath(String testFilesPath) {
        this.testFilesPath = testFilesPath;
    }

    /**
     * Set the flag to create physical files
     *
     * @param createPhysicalFiles True if physical files should be created
     */
    public void setCreatePhysicalFiles(boolean createPhysicalFiles) {
        this.createPhysicalFiles = createPhysicalFiles;
    }

    /**
     * Set the DAT file path
     *
     * @param datFile Path to DAT file
     */
    public void setDatFile(String datFile) {
        this.datFile = datFile;
    }

    /**
     * Set the encrypted file path
     *
     * @param encryptedFile Path to encrypted file
     */
    public void setEncryptedFile(String encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    /**
     * Clean up existing test records from the database
     */
    public void cleanupExistingTestRecords() {
        try {
            // Delete from ocms_offence_notice_owner_driver
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_offence_notice_owner_driver " +
                "WHERE notice_no LIKE 'MHATEST%'"
            );
            
            // Delete from ocms_offence_notice_detail
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_offence_notice_detail " +
                "WHERE notice_no LIKE 'MHATEST%'"
            );
            
            // Delete from ocms_valid_offence_notice
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_valid_offence_notice " +
                "WHERE notice_no LIKE 'MHATEST%'"
            );
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Error cleaning up test records: " + e.getMessage());
            System.err.println(sw.toString());
            throw new RuntimeException("Error cleaning up test records", e);
        }
    }
    
    /**
     * Alias for cleanupExistingTestRecords for consistency with other methods
     */
    public void cleanupTestRecords() {
        cleanupExistingTestRecords();
    }

    /**
     * Cleans up all test files in the test directory if physical file creation is enabled
     */
    public void cleanupTestFiles() {
        if (!createPhysicalFiles || testFilesPath == null) {
            return;
        }

        try {
            File directory = new File(testFilesPath);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().startsWith("URA2NRO_") || 
                                             file.getName().startsWith("test_") ||
                                             file.getName().endsWith(".enc"))) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                System.err.println("Failed to delete file: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Error cleaning up test files: " + sw.toString());
        }
    }

    /**
     * Create test records for the "No NRIC Data Found" scenario
     */
    public void createNoNricDataFoundTestRecords() {
        try {
            // Insert into ocms_valid_offence_notice
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_valid_offence_notice " +
                "(notice_no, next_processing_stage, next_processing_date, last_processing_stage, " +
                "last_processing_date, suspension_type, offence_notice_type, notice_date_and_time, " +
                "composition_amount, cre_user_id, cre_date, pp_code, vehicle_no, vehicle_category, computer_rule_code) " +
                "VALUES (?, ?, DATEADD(day, 10, GETDATE()), ?, GETDATE(), NULL, 'O', GETDATE(), " +
                "100.00, 'SYSTEM', GETDATE(), 'C0517', ?, ?, 123)",
                "MHATEST001", "INVALID", "ROV", "SGX1234A", "A"
            );
            
            // No owner/driver record for this scenario
            // No offence detail record for this scenario
            
            // Create test files if enabled
            if (createPhysicalFiles) {
                // No files needed for this scenario
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            throw new RuntimeException("Failed to create test records: " + e.getMessage(), e);
        }
    }



    /**
     * Display joined table data for a specific notice number
     *
     * @param noticeNumber The notice number to query
     * @return A formatted string with the query results
     */
    public String displayJoinedTableData(String noticeNumber) {
        StringBuilder sb = new StringBuilder();
        try {
            // Query to join all three tables
            String joinQuery = "SELECT von.notice_no, von.next_processing_stage, von.next_processing_date, " +
                "von.vehicle_no, von.vehicle_category, " +
                "ond.comments, ond.rule_no, " +
                "onod.id_type, onod.id_no, onod.name " +
                "FROM " + schema + ".ocms_valid_offence_notice von " +
                "LEFT JOIN " + schema + ".ocms_offence_notice_detail ond ON von.notice_no = ond.notice_no " +
                "LEFT JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                "WHERE von.notice_no = ?";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(joinQuery, noticeNumber);

            sb.append("\n=== Joined Data for Notice " + noticeNumber + " ===\n");
            for (Map<String, Object> row : results) {
                sb.append("---------------------------------------------------\n");
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                sb.append("---------------------------------------------------\n");
            }

            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "Error querying data: " + e.getMessage() + "\n" + sw.toString();
        }
    }
    
    /**
     * Helper method to extract XML value from a tag
     * 
     * @param xml The XML string
     * @param tagName The tag name to extract value from
     * @return The value between the tags, or null if not found
     */
    public String extractXmlValue(String xml, String tagName) {
        if (xml == null || tagName == null) {
            return null;
        }
        
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += startTag.length();
        int endIndex = xml.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return null;
        }
        
        return xml.substring(startIndex, endIndex).trim();
    }

    /**
     * Verify records were created in all required tables
     *
     * @param result TestStepResult to update with verification results
     */
    public void verifyRecordsCreated(TestStepResult result) {
        verifyRecordsCreated(result, "MHATEST%");
    }
    
    /**
     * Verify records were created in all required tables for a specific notice prefix
     *
     * @param result TestStepResult to update with verification results
     * @param noticePrefix The prefix for test notice numbers (e.g., "MHATEST001")
     */
    public void verifyRecordsCreated(TestStepResult result, String noticePrefix) {
        // If noticePrefix is a specific notice number, add % for SQL LIKE pattern
        String likePattern = noticePrefix;
        if (!noticePrefix.endsWith("%")) {
            likePattern = noticePrefix + "%";
        }
        
        String verifyRecordsSql = 
            "SELECT 'CREATED RECORDS - ocms_valid_offence_notice' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_valid_offence_notice " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'CREATED RECORDS - ocms_offence_notice_detail' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_detail " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'CREATED RECORDS - ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_owner_driver " +
            "WHERE notice_no LIKE '" + likePattern + "'";

        List<Map<String, Object>> verificationResults = jdbcTemplate.queryForList(verifyRecordsSql);

        // Add verification results to the step result
        for (Map<String, Object> row : verificationResults) {
            result.addDetail(row.get("table_name") + ": " + row.get("count"));
        }
    }

    /**
     * Check if test records exist in database for a specific notice number
     *
     * @param noticeNumber Notice number to check
     * @return true if records exist, false otherwise
     */
    public boolean testRecordsExist(String noticeNumber) {
        String checkSql = "SELECT COUNT(*) FROM " + schema + ".ocms_valid_offence_notice " +
                          "WHERE notice_no = ?";

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, noticeNumber);
        return count != null && count > 0;
    }

    /**
     * Create or update test records based on environment
     *
     * @param cleanup true if environment is local or SIT, false for DEV
     * @param result TestStepResult to add details to
     */
    public void createOrUpdateTestRecords(boolean cleanup, TestStepResult result) {
        if (cleanup) {
            // For local and SIT environments, use the original flow (delete then insert)
            cleanupExistingTestRecords();
            result.addDetail("✅ Cleaned up existing test records (local/SIT environment)");

            createNoNricDataFoundTestRecords();
            result.addDetail("✅ Created new test records for 'No NRIC Data Found' scenario");
        } else {
            // For DEV environment, use select-update-insert flow
            result.addDetail("⚠️ Running in DEV environment - using select-update-insert flow");

            // Check and handle test records
            if (testRecordsExist("MHATEST001")) {
                // Delete and recreate for simplicity in this case
                cleanupExistingTestRecords();
                createNoNricDataFoundTestRecords();
                result.addDetail("✅ Recreated test records for 'No NRIC Data Found' scenario");
            } else {
                createNoNricDataFoundTestRecords();
                result.addDetail("✅ Created new test records for 'No NRIC Data Found' scenario");
            }
        }
    }
    
    /**
     * Create test records for the "Successful Upload with SLIFT Encryption" scenario
     * 
     * @param noticePrefix The prefix for test notice numbers
     */
    public void createSuccessfulUploadTestRecords(String noticePrefix) {
        try {
            // Validate input parameter
            if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
                throw new IllegalArgumentException("Notice prefix cannot be null or empty");
            }
            
            // Generate notice numbers like shell script: MHATEST001, MHATEST002, MHATEST003
            String noticeNo1 = "MHATEST001";
            String noticeNo2 = "MHATEST002";
            String noticeNo3 = "MHATEST003";
            
            // Clean up any existing test records first
            jdbcTemplate.update("DELETE FROM " + schema + ".ocms_offence_notice_owner_driver WHERE notice_no LIKE 'MHATEST%'");
            jdbcTemplate.update("DELETE FROM " + schema + ".ocms_offence_notice_detail WHERE notice_no LIKE 'MHATEST%'");
            jdbcTemplate.update("DELETE FROM " + schema + ".ocms_valid_offence_notice WHERE notice_no LIKE 'MHATEST%'");
            
            // Insert into ocms_valid_offence_notice - match shell script exactly
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_valid_offence_notice " +
                "(notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, " +
                "composition_amount, computer_rule_code, pp_code, prev_processing_stage, prev_processing_date, " +
                "last_processing_stage, next_processing_stage, last_processing_date, next_processing_date, " +
                "subsystem_label, suspension_type, cre_user_id, cre_date) " +
                "VALUES (?, ?, ?, 'O', GETDATE(), ?, ?, ?, " +
                "'ROV', DATEADD(day, -1, GETDATE()), 'ENA', 'RD1', GETDATE(), CAST(GETDATE() AS DATE), " +
                "'OCMS', NULL, 'SYSTEM', GETDATE())",
                noticeNo1, "SXX1234A", "A", 100.00, 101, "PP01"
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_valid_offence_notice " +
                "(notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, " +
                "composition_amount, computer_rule_code, pp_code, prev_processing_stage, prev_processing_date, " +
                "last_processing_stage, next_processing_stage, last_processing_date, next_processing_date, " +
                "subsystem_label, suspension_type, cre_user_id, cre_date) " +
                "VALUES (?, ?, ?, 'O', GETDATE(), ?, ?, ?, " +
                "'ROV', DATEADD(day, -1, GETDATE()), 'ENA', 'RD1', GETDATE(), CAST(GETDATE() AS DATE), " +
                "'OCMS', NULL, 'SYSTEM', GETDATE())",
                noticeNo2, "SXX5678B", "A", 150.00, 102, "PP02"
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_valid_offence_notice " +
                "(notice_no, vehicle_no, vehicle_category, offence_notice_type, notice_date_and_time, " +
                "composition_amount, computer_rule_code, pp_code, prev_processing_stage, prev_processing_date, " +
                "last_processing_stage, next_processing_stage, last_processing_date, next_processing_date, " +
                "subsystem_label, suspension_type, cre_user_id, cre_date) " +
                "VALUES (?, ?, ?, 'O', GETDATE(), ?, ?, ?, " +
                "'ROV', DATEADD(day, -1, GETDATE()), 'ENA', 'RD1', GETDATE(), CAST(GETDATE() AS DATE), " +
                "'OCMS', NULL, 'SYSTEM', GETDATE())",
                noticeNo3, "SXX9012C", "A", 120.00, 103, "PP03"
            );
            
            // Insert into ocms_offence_notice_owner_driver - match shell script exactly
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_owner_driver " +
                "(notice_no, owner_driver_indicator, nric_no, id_type, life_status, offender_indicator, " +
                "name, cre_user_id, cre_date, mha_processing_date_time, lta_processing_date_time) " +
                "VALUES (?, 'O', ?, 'F', 'A', 'Y', ?, 'SYSTEM', GETDATE(), GETDATE(), GETDATE())",
                noticeNo1, "S1234567A", "Test Driver 001"
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_owner_driver " +
                "(notice_no, owner_driver_indicator, nric_no, id_type, life_status, offender_indicator, " +
                "name, cre_user_id, cre_date, mha_processing_date_time, lta_processing_date_time) " +
                "VALUES (?, 'O', ?, 'F', 'A', 'Y', ?, 'SYSTEM', GETDATE(), GETDATE(), GETDATE())",
                noticeNo2, "G1234567A", "Test Driver 002"
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_owner_driver " +
                "(notice_no, owner_driver_indicator, nric_no, id_type, life_status, offender_indicator, " +
                "name, cre_user_id, cre_date, mha_processing_date_time, lta_processing_date_time) " +
                "VALUES (?, 'O', ?, 'B', 'A', 'Y', ?, 'SYSTEM', GETDATE(), GETDATE(), GETDATE())",
                noticeNo3, "F1234567A", "Test Driver 003"
            );
            
            // Insert offence detail records (keep existing logic)
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_detail " +
                "(notice_no, comments, rule_no, cre_user_id, cre_date) " +
                "VALUES (?, 'TEST LOCATION', 'TC01', 'SYSTEM', GETDATE())",
                noticeNo1
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_detail " +
                "(notice_no, comments, rule_no, cre_user_id, cre_date) " +
                "VALUES (?, 'TEST LOCATION', 'TC01', 'SYSTEM', GETDATE())",
                noticeNo2
            );
            
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".ocms_offence_notice_detail " +
                "(notice_no, comments, rule_no, cre_user_id, cre_date) " +
                "VALUES (?, 'TEST LOCATION', 'TC01', 'SYSTEM', GETDATE())",
                noticeNo3
            );
            
            // Clean up temp tables like shell script
            jdbcTemplate.update("DELETE FROM " + schema + ".ocms_nro_temp WHERE notice_no LIKE 'MHATEST%'");
            jdbcTemplate.update("DELETE FROM " + schema + ".ocms_hst WHERE id_no IN ('S1234567A', 'G1234567A', 'F1234567A')");
            
            // Create test files if enabled
            if (createPhysicalFiles) {
                // No files needed for this scenario
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println("Error creating test records: " + e.getMessage());
            System.err.println(sw.toString());
            throw new RuntimeException("Error creating test records", e);
        }
    }
    
    /**
     * Get the DAT file path.
     * @return The DAT file path
     */
    public String getDatFile() {
        return datFile;
    }

    /**
     * Get the encrypted file path.
     * @return The encrypted file path
     */
    public String getEncryptedFile() {
        return encryptedFile;
    }
    
    /**
     * Verify records were created successfully
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return A formatted string with verification results
     */
    public String verifyRecordsCreated(String noticePrefix) {
        StringBuilder sb = new StringBuilder();
        
        // If noticePrefix is a specific notice number, add % for SQL LIKE pattern
        String likePattern = noticePrefix;
        if (!noticePrefix.endsWith("%")) {
            likePattern = noticePrefix + "%";
        }
        
        String verifyRecordsSql = 
            "SELECT 'CREATED RECORDS - ocms_valid_offence_notice' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_valid_offence_notice " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'CREATED RECORDS - ocms_offence_notice_detail' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_detail " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'CREATED RECORDS - ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_owner_driver " +
            "WHERE notice_no LIKE '" + likePattern + "'";

        List<Map<String, Object>> verificationResults = jdbcTemplate.queryForList(verifyRecordsSql);

        sb.append("\n=== Verification Results ===\n");
        boolean allTablesHaveRecords = true;
        
        for (Map<String, Object> row : verificationResults) {
            String tableName = (String) row.get("table_name");
            Long count = (Long) row.get("count");
            
            sb.append(tableName).append(": ").append(count).append("\n");
            
            if (count == 0) {
                allTablesHaveRecords = false;
            }
        }
        
        if (allTablesHaveRecords) {
            sb.append("\nRecords created successfully.\n");
        } else {
            sb.append("\nSome tables have no records.\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Generate NRIC file content based on test data
     * Aligned with shell script format: H|timestamp|total|valid|invalid, D|nric|verification|, T|total|valid|invalid
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return The generated file content
     */
    public String generateNricFileContent(String noticePrefix) {
        StringBuilder content = new StringBuilder();
        
        // Query for NRIC data from owner_driver records (aligned with shell script)
        String query = "SELECT DISTINCT onod.nric_no " +
                      "FROM " + schema + ".ocms_valid_offence_notice von " +
                      "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                      "WHERE von.notice_no LIKE ? AND onod.id_type IN ('F', 'B') " +
                      "AND onod.offender_indicator = 'Y'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, noticePrefix + "%");
        
        // Generate timestamp for header (matching shell script format)
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        int totalRecords = results.size();
        int validRecords = totalRecords; // For test purposes, assume all are valid
        int invalidRecords = 0;
        
        // Header: H|timestamp|total_records|valid_records|invalid_records
        content.append("H|" + timestamp + "|" + totalRecords + "|" + validRecords + "|" + invalidRecords + "\n");
        
        // Data records: D|nric|verification_result|
        for (Map<String, Object> row : results) {
            String nric = (String) row.get("nric_no");
            String verificationResult = "Y"; // For test purposes, assume all are verified successfully
            
            content.append("D|" + nric + "|" + verificationResult + "|\n");
        }
        
        // Trailer: T|total_records|valid_records|invalid_records
        content.append("T|" + totalRecords + "|" + validRecords + "|" + invalidRecords + "\n");
        
        return content.toString();
    }
    
    /**
     * Save file content to a local file
     * 
     * @param filePath The path to save the file to
     * @param content The content to write to the file
     * @throws Exception If an error occurs while writing the file
     */
    public void saveFileLocally(String filePath, String content) throws Exception {
        java.io.File file = new java.io.File(filePath);
        
        // Create parent directories if they don't exist
        java.io.File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Write content to file
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
        }
    }
    
    /**
     * Verify database updates after processing
     * 
     * @return A formatted string with verification results
     */
    public String verifyDatabaseUpdates() {
        StringBuilder sb = new StringBuilder();
        
        try {
            // Query for updated records
            String query = "SELECT von.notice_no, von.next_processing_stage, von.last_processing_stage, " +
                          "von.upd_user_id, von.upd_date " +
                          "FROM " + schema + ".ocms_valid_offence_notice von " +
                          "WHERE von.notice_no LIKE 'MHATEST%' " +
                          "ORDER BY von.upd_date DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            
            sb.append("\n=== Database Updates ===\n");
            
            if (results.isEmpty()) {
                sb.append("No updated records found.\n");
                return sb.toString();
            }
            
            for (Map<String, Object> row : results) {
                sb.append("Notice No: ").append(row.get("notice_no")).append("\n");
                sb.append("Next Processing Stage: ").append(row.get("next_processing_stage")).append("\n");
                sb.append("Last Processing Stage: ").append(row.get("last_processing_stage")).append("\n");
                sb.append("Updated By: ").append(row.get("upd_user_id")).append("\n");
                sb.append("Updated Date: ").append(row.get("upd_date")).append("\n");
                sb.append("---------------------------------------------------\n");
            }
            
            sb.append("SUCCESS: Found " + results.size() + " updated records.\n");
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            sb.append("ERROR: Failed to verify database updates: ").append(e.getMessage()).append("\n");
            sb.append(sw.toString());
            
            return sb.toString();
        }
    }
    
    /**
     * List output files in the test directory
     * 
     * @return List of output file paths
     */
    public List<String> listOutputFiles() {
        List<String> outputFiles = new java.util.ArrayList<>();
        
        if (!createPhysicalFiles || testFilesPath == null) {
            return outputFiles;
        }
        
        try {
            java.io.File directory = new java.io.File(testFilesPath);
            if (directory.exists() && directory.isDirectory()) {
                java.io.File[] files = directory.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile()) {
                            outputFiles.add(file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error listing output files: " + e.getMessage());
        }
        
        return outputFiles;
    }
    
    /**
     * Verify output file contents
     * 
     * @return A formatted string with verification results
     */
    public String verifyOutputFileContents() {
        StringBuilder sb = new StringBuilder();
        
        List<String> outputFiles = listOutputFiles();
        if (outputFiles.isEmpty()) {
            return "No output files found.";
        }
        
        sb.append("\n=== Output File Contents ===\n");
        
        for (String filePath : outputFiles) {
            try {
                java.io.File file = new java.io.File(filePath);
                sb.append("File: ").append(file.getName()).append("\n");
                sb.append("Size: ").append(file.length()).append(" bytes\n");
                
                // Read first few lines of the file
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    int lineCount = 0;
                    sb.append("Content Preview:\n");
                    
                    while ((line = reader.readLine()) != null && lineCount < 5) {
                        sb.append(line).append("\n");
                        lineCount++;
                    }
                    
                    if (lineCount == 5) {
                        sb.append("... (more lines)\n");
                    }
                }
                
                sb.append("---------------------------------------------------\n");
            } catch (Exception e) {
                sb.append("Error reading file ").append(filePath).append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Verify records exist for a specific notice prefix
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return A formatted string with verification results
     */
    public String verifyRecordsExist(String noticePrefix) {
        StringBuilder sb = new StringBuilder();
        
        // If noticePrefix is a specific notice number, add % for SQL LIKE pattern
        String likePattern = noticePrefix;
        if (!noticePrefix.endsWith("%")) {
            likePattern = noticePrefix + "%";
        }
        
        String verifyRecordsSql = 
            "SELECT 'EXISTING RECORDS - ocms_valid_offence_notice' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_valid_offence_notice " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'EXISTING RECORDS - ocms_offence_notice_detail' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_detail " +
            "WHERE notice_no LIKE '" + likePattern + "' " +
            "UNION ALL " +
            "SELECT 'EXISTING RECORDS - ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count " +
            "FROM " + schema + ".ocms_offence_notice_owner_driver " +
            "WHERE notice_no LIKE '" + likePattern + "'";

        List<Map<String, Object>> verificationResults = jdbcTemplate.queryForList(verifyRecordsSql);

        sb.append("\n=== Record Existence Verification ===\n");
        boolean allTablesHaveRecords = true;
        
        for (Map<String, Object> row : verificationResults) {
            String tableName = (String) row.get("table_name");
            Long count = (Long) row.get("count");
            
            sb.append(tableName).append(": ").append(count).append("\n");
            
            if (count == 0) {
                allTablesHaveRecords = false;
            }
        }
        
        if (allTablesHaveRecords) {
            sb.append("\nRecords exist in all tables.\n");
        } else {
            sb.append("\nSome tables have no records.\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Generate NRIC response file content
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return The generated response file content
     */
    public String generateNricResponseFileContent(String noticePrefix) {
        StringBuilder content = new StringBuilder();
        
        // Query for NRIC data
        String query = "SELECT von.notice_no, von.vehicle_no, onod.id_no " +
                      "FROM " + schema + ".ocms_valid_offence_notice von " +
                      "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                      "WHERE von.notice_no LIKE ? AND onod.id_type = 'N'";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, noticePrefix + "%");
        
        // Header
        content.append("H|MHA|URA|NRIC|" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "\n");
        
        // Data records
        for (Map<String, Object> row : results) {
            String noticeNo = (String) row.get("notice_no");
            String vehicleNo = (String) row.get("vehicle_no");
            String nric = (String) row.get("id_no");
            String status = "SUCCESS"; // Default status
            
            content.append("D|" + noticeNo + "|" + vehicleNo + "|" + nric + "|" + status + "\n");
        }
        
        // Trailer
        content.append("T|" + results.size() + "\n");
        
        return content.toString();
    }
    
    /**
     * Verify download database updates
     * 
     * @param noticePrefix The prefix for test notice numbers
     * @return A formatted string with verification results
     */
    public String verifyDownloadDatabaseUpdates(String noticePrefix) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // Query for updated records
            String query = "SELECT von.notice_no, von.next_processing_stage, von.last_processing_stage, " +
                          "von.upd_user_id, von.upd_date " +
                          "FROM " + schema + ".ocms_valid_offence_notice von " +
                          "WHERE von.notice_no LIKE ? " +
                          "ORDER BY von.upd_date DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, noticePrefix + "%");
            
            sb.append("\n=== Download Database Updates ===\n");
            
            if (results.isEmpty()) {
                sb.append("No updated records found.\n");
                return sb.toString();
            }
            
            for (Map<String, Object> row : results) {
                sb.append("Notice No: ").append(row.get("notice_no")).append("\n");
                sb.append("Next Processing Stage: ").append(row.get("next_processing_stage")).append("\n");
                sb.append("Last Processing Stage: ").append(row.get("last_processing_stage")).append("\n");
                sb.append("Updated By: ").append(row.get("upd_user_id")).append("\n");
                sb.append("Updated Date: ").append(row.get("upd_date")).append("\n");
                sb.append("---------------------------------------------------\n");
            }
            
            sb.append("SUCCESS: Found " + results.size() + " updated records.\n");
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            sb.append("ERROR: Failed to verify download database updates: ").append(e.getMessage()).append("\n");
            sb.append(sw.toString());
            
            return sb.toString();
        }
    }
    
    /**
     * Verify test data for a specific notice prefix
     *
     * @param noticePrefix The prefix for test notice numbers
     * @return A formatted string with verification results
     */
    public String verifyTestData(String noticePrefix) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // Query for test data
            String query = "SELECT von.notice_no, von.next_processing_stage, von.last_processing_stage, " +
                          "onod.id_type, onod.id_no, onod.name " +
                          "FROM " + schema + ".ocms_valid_offence_notice von " +
                          "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                          "WHERE von.notice_no LIKE ? AND onod.id_type = 'N'";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, noticePrefix + "%");
            
            sb.append("\n=== Test Data Verification ===\n");
            
            if (results.isEmpty()) {
                sb.append("No test data found.\n");
                return sb.toString();
            }
            
            for (Map<String, Object> row : results) {
                sb.append("Notice No: ").append(row.get("notice_no")).append("\n");
                sb.append("Next Processing Stage: ").append(row.get("next_processing_stage")).append("\n");
                sb.append("Last Processing Stage: ").append(row.get("last_processing_stage")).append("\n");
                sb.append("ID Type: ").append(row.get("id_type")).append("\n");
                sb.append("ID No: ").append(row.get("id_no")).append("\n");
                sb.append("Name: ").append(row.get("name")).append("\n");
                sb.append("---------------------------------------------------\n");
            }
            
            sb.append("Test data verified successfully.\n");
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            sb.append("ERROR: Failed to verify test data: ").append(e.getMessage()).append("\n");
            sb.append(sw.toString());
            
            return sb.toString();
        }
    }
    
    /**
     * Upload file to SFTP server
     *
     * @param remoteFilePath Remote file path on SFTP server
     * @param fileContent File content as string
     * @return true if upload successful, false otherwise
     */
    public boolean uploadFileToSftp(String remoteFilePath, String fileContent) {
        try {
            // Create temporary file
            java.io.File tempFile = java.io.File.createTempFile("sftp", ".tmp");
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(fileContent);
            }
            
            // Upload file to SFTP
            return uploadFileToSftp(remoteFilePath, java.nio.file.Files.readAllBytes(tempFile.toPath()));
        } catch (Exception e) {
            System.err.println("Error uploading file to SFTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload file to SFTP server
     *
     * @param remoteFilePath Remote file path on SFTP server
     * @param fileContent File content as byte array
     * @return true if upload successful, false otherwise
     */
    public boolean uploadFileToSftp(String remoteFilePath, byte[] fileContent) {
        try {
            // Create temporary file
            java.io.File tempFile = java.io.File.createTempFile("sftp", ".tmp");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(fileContent);
            }
            
            // TODO: Implement actual SFTP upload using SftpUtil or similar
            // For now, just simulate successful upload
            System.out.println("Simulating SFTP upload to: " + remoteFilePath);
            System.out.println("File size: " + tempFile.length() + " bytes");
            
            // Clean up temp file
            tempFile.delete();
            
            return true;
        } catch (Exception e) {
            System.err.println("Error uploading file to SFTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Query job execution history
     * 
     * @param jobName The name of the job to query
     * @return A formatted string with job execution history
     */
    public String queryJobExecutionHistory(String jobName) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // Query for job execution history
            String query = "SELECT job_name, status, start_time, end_time, create_time " +
                          "FROM " + schema + ".batch_job_execution " +
                          "WHERE job_name LIKE ? " +
                          "ORDER BY start_time DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, "%" + jobName + "%");
            
            sb.append("\n=== Job Execution History for '" + jobName + "' ===\n");
            
            if (results.isEmpty()) {
                sb.append("No job execution history found.\n");
                return sb.toString();
            }
            
            for (Map<String, Object> row : results) {
                sb.append("Job Name: ").append(row.get("job_name")).append("\n");
                sb.append("Status: ").append(row.get("status")).append("\n");
                sb.append("Start Time: ").append(row.get("start_time")).append("\n");
                sb.append("End Time: ").append(row.get("end_time")).append("\n");
                sb.append("Create Time: ").append(row.get("create_time")).append("\n");
                sb.append("---------------------------------------------------\n");
            }
            
            sb.append("SUCCESS: Found " + results.size() + " job execution records.\n");
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            sb.append("ERROR: Failed to query job execution history: ").append(e.getMessage()).append("\n");
            sb.append(sw.toString());
            
            return sb.toString();
        }
    }
}
