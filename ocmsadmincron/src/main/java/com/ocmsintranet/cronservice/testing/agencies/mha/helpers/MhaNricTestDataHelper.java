package com.ocmsintranet.cronservice.testing.agencies.mha.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for MHA NRIC test data operations.
 * Extends functionality of MhaTestDatabaseHelper with specific methods for NRIC upload/download testing.
 */
@Slf4j
@Component
public class MhaNricTestDataHelper {

    private final JdbcTemplate jdbcTemplate;
    private final MhaTestDatabaseHelper baseHelper;
    private final String schema;
    private final String uploadPath;
    private final String downloadPath;

    /**
     * Constructor for MhaNricTestDataHelper
     *
     * @param jdbcTemplate JdbcTemplate for database operations
     * @param baseHelper Base MhaTestDatabaseHelper
     * @param schema Database schema name
     */
    @Autowired
    public MhaNricTestDataHelper(
            JdbcTemplate jdbcTemplate,
            MhaTestDatabaseHelper baseHelper,
            @Value("${mha.test.schema:ocmsizmgr}") String schema,
            @Value("${mha.test.upload.path:logs/mha/upload-nric}") String uploadPath,
            @Value("${mha.test.download.path:logs/mha/download-nric}") String downloadPath) {
        this.jdbcTemplate = jdbcTemplate;
        this.baseHelper = baseHelper;
        this.schema = schema;
        this.uploadPath = System.getProperty("user.dir") + "/" + uploadPath;
        this.downloadPath = System.getProperty("user.dir") + "/" + downloadPath;
        
        // Ensure directories exist
        createDirectoryIfNotExists(this.uploadPath);
        createDirectoryIfNotExists(this.downloadPath);
    }

    /**
     * Create directory if it doesn't exist
     * 
     * @param directoryPath Path to create
     */
    private void createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created directory: {}", directoryPath);
            }
        } catch (Exception e) {
            log.error("Error creating directory {}: {}", directoryPath, e.getMessage());
        }
    }

    /**
     * Clean up test records from previous runs
     */
    public void cleanupTestRecords() {
        baseHelper.cleanupTestRecords();
    }

    /**
     * Clean up test files from previous runs
     */
    public void cleanupTestFiles() {
        baseHelper.cleanupTestFiles();
        
        // Additional cleanup for NRIC-specific files
        cleanupFilesInDirectory(uploadPath);
        cleanupFilesInDirectory(downloadPath);
    }

    /**
     * Clean up files in a specific directory
     * 
     * @param directoryPath Directory to clean
     */
    private void cleanupFilesInDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().startsWith("URA2NRO_") || 
                                             file.getName().startsWith("NRO2URA_"))) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                log.warn("Failed to delete file: {}", file.getAbsolutePath());
                            } else {
                                log.debug("Deleted file: {}", file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning up files in {}: {}", directoryPath, e.getMessage());
        }
    }

    /**
     * Create test records for NRIC upload test
     * 
     * @param noticePrefix Prefix for test notice numbers
     */
    public void createSuccessfulUploadTestRecords(String noticePrefix) {
        baseHelper.createSuccessfulUploadTestRecords(noticePrefix);
    }

    /**
     * Verify records were created successfully
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Verification result message
     */
    public String verifyRecordsCreated(String noticePrefix) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Query to check records in all three tables
            String checkSql = 
                "SELECT 'ocms_valid_offence_notice' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_valid_offence_notice " +
                "WHERE notice_no LIKE ? " +
                "UNION ALL " +
                "SELECT 'ocms_offence_notice_detail' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_offence_notice_detail " +
                "WHERE notice_no LIKE ? " +
                "UNION ALL " +
                "SELECT 'ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_offence_notice_owner_driver " +
                "WHERE notice_no LIKE ?";

            String likePattern = noticePrefix + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, likePattern, likePattern, likePattern);
            
            sb.append("Verification results:\n");
            boolean allTablesHaveRecords = true;
            
            for (Map<String, Object> row : results) {
                String tableName = (String) row.get("table_name");
                int count = ((Number) row.get("count")).intValue();
                
                sb.append("- ").append(tableName).append(": ").append(count).append(" records\n");
                
                if (count == 0) {
                    allTablesHaveRecords = false;
                }
            }
            
            if (allTablesHaveRecords) {
                sb.append("\nRecords created successfully for prefix: ").append(noticePrefix);
            } else {
                sb.append("\nWarning: Some tables have no records for prefix: ").append(noticePrefix);
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error verifying records: " + e.getMessage() + "\n" + sw.toString();
        }
    }

    /**
     * Check if test records exist in database
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Verification result message
     */
    public String verifyRecordsExist(String noticePrefix) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Query to check records in all three tables
            String checkSql = 
                "SELECT 'ocms_valid_offence_notice' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_valid_offence_notice " +
                "WHERE notice_no LIKE ? " +
                "UNION ALL " +
                "SELECT 'ocms_offence_notice_detail' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_offence_notice_detail " +
                "WHERE notice_no LIKE ? " +
                "UNION ALL " +
                "SELECT 'ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count " +
                "FROM " + schema + ".ocms_offence_notice_owner_driver " +
                "WHERE notice_no LIKE ?";

            String likePattern = noticePrefix + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, likePattern, likePattern, likePattern);
            
            sb.append("Verification results:\n");
            boolean recordsFound = false;
            
            for (Map<String, Object> row : results) {
                String tableName = (String) row.get("table_name");
                int count = ((Number) row.get("count")).intValue();
                
                sb.append("- ").append(tableName).append(": ").append(count).append(" records\n");
                
                if (count > 0) {
                    recordsFound = true;
                }
            }
            
            if (recordsFound) {
                sb.append("\nRecords found for prefix: ").append(noticePrefix);
            } else {
                sb.append("\nNo records found for prefix: ").append(noticePrefix);
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error verifying records: " + e.getMessage() + "\n" + sw.toString();
        }
    }

    /**
     * Generate NRIC file content for upload test
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Generated file content
     */
    public String generateNricFileContent(String noticePrefix) {
        StringBuilder content = new StringBuilder();
        
        // Generate header
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        content.append("H|").append(timestamp).append("|URA2NRO|001|\n");
        
        // Query database for test records
        String query = "SELECT von.notice_no, von.vehicle_no, onod.id_no " +
                      "FROM " + schema + ".ocms_valid_offence_notice von " +
                      "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                      "WHERE von.notice_no LIKE ?";
        
        List<Map<String, Object>> records = jdbcTemplate.queryForList(query, noticePrefix + "%");
        
        // Generate detail records
        for (Map<String, Object> record : records) {
            String noticeNo = (String) record.get("notice_no");
            String vehicleNo = (String) record.get("vehicle_no");
            String idNo = (String) record.get("id_no");
            
            content.append("D|").append(noticeNo).append("|").append(vehicleNo).append("|").append(idNo).append("|\n");
        }
        
        // Generate trailer
        content.append("T|").append(records.size()).append("|\n");
        
        return content.toString();
    }

    /**
     * Generate NRIC response file content for download test
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Generated file content
     */
    public String generateNricResponseFileContent(String noticePrefix) {
        StringBuilder content = new StringBuilder();
        
        // Generate header
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        content.append("H|").append(timestamp).append("|NRO2URA|001|\n");
        
        // Query database for test records
        String query = "SELECT von.notice_no, von.vehicle_no, onod.id_no " +
                      "FROM " + schema + ".ocms_valid_offence_notice von " +
                      "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                      "WHERE von.notice_no LIKE ?";
        
        List<Map<String, Object>> records = jdbcTemplate.queryForList(query, noticePrefix + "%");
        
        // Generate detail records with response data
        for (Map<String, Object> record : records) {
            String noticeNo = (String) record.get("notice_no");
            String vehicleNo = (String) record.get("vehicle_no");
            String idNo = (String) record.get("id_no");
            
            // Add response data (name, address, etc.)
            String name = "TEST USER " + noticeNo.substring(noticeNo.length() - 3);
            String address = "123 TEST STREET SINGAPORE 123456";
            String postalCode = "123456";
            
            content.append("D|").append(noticeNo).append("|").append(vehicleNo).append("|")
                   .append(idNo).append("|").append(name).append("|")
                   .append(address).append("|").append(postalCode).append("|SUCCESS|\n");
        }
        
        // Generate trailer
        content.append("T|").append(records.size()).append("|\n");
        
        return content.toString();
    }

    /**
     * Save file locally
     * 
     * @param filePath Path to save file
     * @param content File content
     * @throws Exception If file cannot be saved
     */
    public void saveFileLocally(String filePath, String content) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }

    /**
     * Verify database updates after upload
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Verification result message
     */
    public String verifyDatabaseUpdates(String noticePrefix) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Query to check processing stage updates
            String checkSql = 
                "SELECT notice_no, next_processing_stage, last_processing_stage, " +
                "upd_user_id, upd_date " +
                "FROM " + schema + ".ocms_valid_offence_notice " +
                "WHERE notice_no LIKE ?";

            String likePattern = noticePrefix + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, likePattern);
            
            sb.append("Database update verification results:\n");
            boolean allRecordsUpdated = true;
            
            for (Map<String, Object> row : results) {
                String noticeNo = (String) row.get("notice_no");
                String nextStage = (String) row.get("next_processing_stage");
                String lastStage = (String) row.get("last_processing_stage");
                String updUserId = (String) row.get("upd_user_id");
                Object updDate = row.get("upd_date");
                
                sb.append("- Notice: ").append(noticeNo)
                  .append(", Next Stage: ").append(nextStage)
                  .append(", Last Stage: ").append(lastStage)
                  .append(", Updated By: ").append(updUserId)
                  .append(", Update Date: ").append(updDate)
                  .append("\n");
                
                // Check if record was updated properly
                if (!"RD1".equals(nextStage) || updUserId == null || updDate == null) {
                    allRecordsUpdated = false;
                }
            }
            
            if (results.isEmpty()) {
                sb.append("\nNo records found for verification");
                return sb.toString();
            }
            
            if (allRecordsUpdated) {
                sb.append("\nDatabase updates successfully verified for prefix: ").append(noticePrefix);
            } else {
                sb.append("\nWarning: Some records were not updated correctly for prefix: ").append(noticePrefix);
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error verifying database updates: " + e.getMessage() + "\n" + sw.toString();
        }
    }

    /**
     * Verify database updates after download
     * 
     * @param noticePrefix Prefix for test notice numbers
     * @return Verification result message
     */
    public String verifyDownloadDatabaseUpdates(String noticePrefix) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Query to check owner/driver updates
            String checkSql = 
                "SELECT von.notice_no, onod.name, onod.address_line_1, onod.postal_code, " +
                "onod.upd_user_id, onod.upd_date " +
                "FROM " + schema + ".ocms_valid_offence_notice von " +
                "JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                "WHERE von.notice_no LIKE ?";

            String likePattern = noticePrefix + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, likePattern);
            
            sb.append("Download database update verification results:\n");
            boolean allRecordsUpdated = true;
            
            for (Map<String, Object> row : results) {
                String noticeNo = (String) row.get("notice_no");
                String name = (String) row.get("name");
                String address = (String) row.get("address_line_1");
                String postalCode = (String) row.get("postal_code");
                String updUserId = (String) row.get("upd_user_id");
                Object updDate = row.get("upd_date");
                
                sb.append("- Notice: ").append(noticeNo)
                  .append(", Name: ").append(name)
                  .append(", Address: ").append(address)
                  .append(", Postal Code: ").append(postalCode)
                  .append(", Updated By: ").append(updUserId)
                  .append(", Update Date: ").append(updDate)
                  .append("\n");
                
                // Check if record was updated properly
                if (name == null || address == null || updUserId == null || updDate == null) {
                    allRecordsUpdated = false;
                }
            }
            
            if (results.isEmpty()) {
                sb.append("\nNo records found for verification");
                return sb.toString();
            }
            
            if (allRecordsUpdated) {
                sb.append("\nDatabase updates successfully verified for prefix: ").append(noticePrefix);
            } else {
                sb.append("\nWarning: Some records were not updated correctly for prefix: ").append(noticePrefix);
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error verifying database updates: " + e.getMessage() + "\n" + sw.toString();
        }
    }

    /**
     * List output files generated by the process
     * 
     * @return List of output file paths
     */
    public List<String> listOutputFiles() {
        List<String> outputFiles = new ArrayList<>();
        
        try {
            // Check upload directory
            File uploadDir = new File(uploadPath);
            if (uploadDir.exists() && uploadDir.isDirectory()) {
                File[] files = uploadDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith("URA2NRO_")) {
                            outputFiles.add(file.getAbsolutePath());
                        }
                    }
                }
            }
            
            // Check download directory
            File downloadDir = new File(downloadPath);
            if (downloadDir.exists() && downloadDir.isDirectory()) {
                File[] files = downloadDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith("NRO2URA_")) {
                            outputFiles.add(file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error listing output files: {}", e.getMessage());
        }
        
        return outputFiles;
    }

    /**
     * Verify output file contents
     * 
     * @return Verification result message
     */
    public String verifyOutputFileContents() {
        StringBuilder sb = new StringBuilder();
        
        try {
            List<String> outputFiles = listOutputFiles();
            
            if (outputFiles.isEmpty()) {
                return "No output files found for verification";
            }
            
            for (String filePath : outputFiles) {
                File file = new File(filePath);
                String fileName = file.getName();
                
                sb.append("File: ").append(fileName).append("\n");
                
                // Read first few lines of file
                List<String> lines = Files.readAllLines(file.toPath());
                int linesToShow = Math.min(5, lines.size());
                
                sb.append("Content preview (").append(linesToShow).append(" lines):\n");
                
                for (int i = 0; i < linesToShow; i++) {
                    sb.append(lines.get(i)).append("\n");
                }
                
                sb.append("Total lines: ").append(lines.size()).append("\n\n");
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error verifying output file contents: " + e.getMessage() + "\n" + sw.toString();
        }
    }

    /**
     * Query job execution history
     * 
     * @param jobName Name of the job to query
     * @return Job execution history
     */
    public String queryJobExecutionHistory(String jobName) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // Query job execution history
            String query = 
                "SELECT TOP 5 job_name, status, start_time, end_time, " +
                "DATEDIFF(second, start_time, end_time) as duration_seconds " +
                "FROM " + schema + ".job_execution_history " +
                "WHERE job_name LIKE ? " +
                "ORDER BY start_time DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, "%" + jobName + "%");
            
            sb.append("Job execution history for ").append(jobName).append(":\n");
            
            if (results.isEmpty()) {
                sb.append("No job execution history found");
                return sb.toString();
            }
            
            for (Map<String, Object> row : results) {
                String status = (String) row.get("status");
                Object startTime = row.get("start_time");
                Object endTime = row.get("end_time");
                Object durationSeconds = row.get("duration_seconds");
                
                sb.append("- Status: ").append(status)
                  .append(", Start: ").append(startTime)
                  .append(", End: ").append(endTime)
                  .append(", Duration: ").append(durationSeconds).append(" seconds")
                  .append("\n");
            }
            
            return sb.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            return "Error querying job execution history: " + e.getMessage() + "\n" + sw.toString();
        }
    }
}
