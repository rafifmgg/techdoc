package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment.OcmsOffenceAttachment;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment.OcmsOffenceAttachmentService;
import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for processing files during notice creation
 */
@Component
@Slf4j
public class FileProcessingHelper {

    @Autowired
    private OcmsOffenceAttachmentService offenceAttachmentService;
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private AzureBlobStorageUtil azureBlobStorageUtil;
    
    @Value("${azure.blob.container-name}")
    private String containerName;
    
    @Value("${azure.blob.temp-folder}")
    private String tempFolder;
    
    // Log the configuration values when the bean is initialized
    public FileProcessingHelper() {
        log.info("FileProcessingHelper initialized");
    }
    
    @Autowired
    public void logConfig() {
        log.info("Azure Blob Storage configuration - Container: {}, TempFolder: {}", containerName, tempFolder);
    }

    /**
     * Process uploaded files for a notice
     * - Move files from temporary location to notice directory
     * - Save attachment information in the database
     *
     * @param noticeNumber Notice number
     * @param data Map containing notice data
     * @param userId User ID who created the notice
     * @return true if all file processing was successful, false if any file failed
     */
    public boolean processUploadedFile(String noticeNumber, Map<String, Object> data, String userId) {
        log.info("Processing uploaded files for notice {}", noticeNumber);
        
        if (data == null) {
            log.warn("No data provided for notice {}", noticeNumber);
            return true; // Not an error, just no data to process
        }
        
        try {
            // Get the DTO from the data map
            Object dtoObj = data.get("dto");
            if (dtoObj != null && dtoObj instanceof com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto) {
                com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto dto = 
                    (com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto) dtoObj;
                
                // Get photos and videos from the DTO and combine them
                List<String> photos = dto.getPhotos();
                List<String> videos = dto.getVideos();
                List<String> allFiles = new ArrayList<>();
                if (photos != null) allFiles.addAll(photos);
                if (videos != null) allFiles.addAll(videos);
                
                if (!allFiles.isEmpty()) {
                    log.info("Found {} file entries in DTO for notice {}", allFiles.size(), noticeNumber);
                    
                    // Convert string paths to file info maps
                    List<Map<String, Object>> files = new ArrayList<>();
                    for (String filePath : allFiles) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        
                        // Normalize path and extract components
                        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
                        
                        // Extract timestamp from path (second path component)
                        String[] pathParts = normalizedPath.split("/");
                        String timestamp = pathParts.length > 1 ? pathParts[1] : "";

                        String filename = pathParts.length > 2 ? pathParts[2] : normalizedPath;
                        log.info("Processing file: {} with timestamp: {}", normalizedPath, timestamp);
                        
                        fileInfo.put("filename", filename);
                        fileInfo.put("timestamp", timestamp);
                        files.add(fileInfo);
                    }
                    
                    // Process the files
                    return processFiles(files, noticeNumber, userId);
                }
            }
            
            // If we get here, either there's no DTO or no files in the DTO
            // Try the old way as fallback
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) data.get("files");
            
            if (files == null || files.isEmpty()) {
                log.info("No file information found in the payload for notice {}", noticeNumber);
                return true; // Not an error, just no files to process
            }
            
            return processFiles(files, noticeNumber, userId);
        } catch (Exception e) {
            log.error("Error processing files for notice {}: {}", noticeNumber, e.getMessage(), e);
            return false;
        }
    }
    
    private boolean processFiles(List<Map<String, Object>> files, String noticeNumber, String userId) {
        try {
            // Debug the files list to see what we're getting
            log.info("Found {} file entries in payload for notice {}", files.size(), noticeNumber);
            for (int i = 0; i < files.size(); i++) {
                log.info("File {}: {}", i, files.get(i));
            }
            
            log.info("Processing {} files for notice {}", files.size(), noticeNumber);
            boolean allSuccessful = true;
            
            // Process each file in the list
            for (Map<String, Object> fileInfo : files) {
                // In the new secure approach, we only expect filename
                // We don't expect or use tempFilePath from the frontend
                String filename = (String) fileInfo.get("filename");
                
                if (filename == null) {
                    log.warn("Missing filename for notice {}, skipping file", noticeNumber);
                    continue;
                }
                
                // Extract file information from the DTO
                String timestamp = (String) fileInfo.get("timestamp");
                
                log.info("Processing file: {} with timestamp: {}", filename, timestamp);
                
                // Use the filename directly from the payload
                // The file is already saved in Azure with this name
                String blobName = tempFolder + "/" + timestamp + "/" + filename;
                String tempFilePath = containerName + "/" + blobName;
                
                log.info("Using filename directly from payload: {}", filename);
                
                // Log the components for debugging
                log.info("Path components - Container: {}, TempFolder: {}, Filename: {}", containerName, tempFolder, filename);
                log.info("Blob name: {}", blobName);
                log.info("Full temp file path being used: {}", tempFilePath);
                
                // Log the exact file we're looking for in Azure Blob Storage
                log.info("SEARCHING FOR FILE: {} in Azure Blob Storage", filename);
                log.info("SEARCHING IN PATH: {} in Azure Blob Storage", blobName);
                
                try {
                    // Step 1: Copy file from temporary to permanent storage in Azure Blob Storage
                    // This doesn't delete the temporary file yet
                    AzureBlobStorageUtil.FileUploadResponse response = 
                            azureBlobStorageUtil.moveToPermStorage(tempFilePath, noticeNumber);
                    
                    if (!response.isSuccess()) {
                        log.error("Failed to copy file to permanent storage for notice {}: {}", 
                                noticeNumber, response.getErrorMessage());
                        allSuccessful = false;
                        continue;
                    }
                    
                    try {
                        // Save attachment information in the database
                        saveAttachmentInformation(noticeNumber, response.getFileName(), response.getFilePath(), response.getFileUrl(), response.getFileSize(), userId);
                        
                        // Step 3: Only delete the temporary file after database operations are successful
                        boolean deleteSuccess = azureBlobStorageUtil.deleteTempFile(tempFilePath);
                        if (!deleteSuccess) {
                            log.warn("Failed to delete temporary file after successful processing: {}", tempFilePath);
                            // This is not considered a critical failure - the cleanup scheduler will handle it later
                        }
                        
                        log.info("File processed successfully for notice {}: {}", noticeNumber, response.getFileName());
                    } catch (Exception dbException) {
                        // If database operations fail, we don't delete the temporary file
                        // This ensures we don't lose the uploaded file if database operations fail
                        log.error("Failed to save attachment information in database for notice {}: {}", 
                                noticeNumber, dbException.getMessage(), dbException);
                        allSuccessful = false;
                    }
                } catch (Exception e) {
                    log.error("Error processing file for notice {}: {}", noticeNumber, e.getMessage(), e);
                    allSuccessful = false;
                }
            }

            return allSuccessful;
        } catch (Exception e) {
            log.error("Error processing files for notice {}: {}", noticeNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save attachment information in the database
     *
     * @param noticeNumber Notice number
     * @param fileName Original filename
     * @param filePath Path to the saved file
     * @param fileUrl URL to access the file (not stored in database, used for logging only)
     * @param fileSize File size
     * @param userId User ID who created the notice
     */
    private void saveAttachmentInformation(String noticeNumber, String fileName, String filePath, String fileUrl, Long fileSize, String userId) {
        try {
            // Determine mime type based on file extension
            String mimeType = determineMimeType(fileName);

            // Create attachment entity
            OcmsOffenceAttachment attachment = new OcmsOffenceAttachment();
            
            // Get next sequence for attachment ID
            // Integer attachmentId = getNextAttachmentId();
            // attachment.setAttachmentId(attachmentId);
            
            attachment.setNoticeNo(noticeNumber);
            
            // Store both file name and path in the fileName field as per the database schema
            // The file_name column in the database stores both the name and path to Azure Blob
            attachment.setFileName(fileName); // Use the full path instead of just the filename
            
            attachment.setMime(mimeType);
            attachment.setSize(fileSize);
            attachment.setCreUserId(userId);
            // attachment.setCreDate(LocalDateTime.now());
            // attachment.setUpdUserId(userId);
            // attachment.setUpdDate(LocalDateTime.now());
            
            // Log the file information for debugging
            log.info("Saving attachment with ID: , Notice: {}, Path: {}, URL: {}", 
                    noticeNumber, filePath, fileUrl);
            
            // Save to database
            offenceAttachmentService.save(attachment);
            
            log.info("Saved attachment information for notice {}: {}", noticeNumber, filePath);
        } catch (Exception e) {
            log.error("Failed to save attachment information for notice {}: {}", noticeNumber, e.getMessage(), e);
            throw e; // Re-throw to be handled by caller
        }
    }
    
    /**
     * Determine MIME type based on file extension
     * 
     * @param fileName The file name to analyze
     * @return The MIME type string
     */
    private String determineMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String lowerCaseFileName = fileName.toLowerCase();
        
        // Common document types
        if (lowerCaseFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerCaseFileName.endsWith(".doc") || lowerCaseFileName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerCaseFileName.endsWith(".xls") || lowerCaseFileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerCaseFileName.endsWith(".ppt") || lowerCaseFileName.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        }
        
        // Image types
        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerCaseFileName.endsWith(".bmp")) {
            return "image/bmp";
        }
        
        // Text types
        if (lowerCaseFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerCaseFileName.endsWith(".html") || lowerCaseFileName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerCaseFileName.endsWith(".xml")) {
            return "text/xml";
        } else if (lowerCaseFileName.endsWith(".json")) {
            return "application/json";
        }
        
        // Compressed types
        if (lowerCaseFileName.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerCaseFileName.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else if (lowerCaseFileName.endsWith(".7z")) {
            return "application/x-7z-compressed";
        }
        
        // Default type if no match
        return "application/octet-stream";
    }
    
    /**
     * Get the next attachment ID from the database
     * This method uses a direct SQL query to get the next value from the sequence
     *
     * @return Next attachment ID
     */
    private Integer getNextAttachmentId() {
        Integer nextId = 1; // Default starting value if sequence doesn't exist
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT NEXT VALUE FOR ocmsizmgr.seq_ocms_offence_attachment")) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nextId = rs.getInt(1);
            }
            rs.close();
            
            log.info("Generated next attachment ID from sequence: {}", nextId);
            return nextId;
            
        } catch (Exception e) {
            log.error("Error getting next attachment ID from sequence: {}", e.getMessage(), e);
            
            // Fallback to MAX+1 approach if sequence is not available
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT MAX(attachment_id) + 1 FROM ocmsizmgr.ocms_offence_attachment")) {
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getObject(1) != null) {
                    nextId = rs.getInt(1);
                }
                rs.close();
                
                log.info("Generated next attachment ID using MAX+1: {}", nextId);
                return nextId;
                
            } catch (Exception ex) {
                log.error("Error in fallback ID generation: {}", ex.getMessage(), ex);
                // Last resort fallback - use timestamp-based ID
                return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            }
        }
    }

    /**
     * Delete an attachment from both database and blob storage
     * 
     * @param noticeNumber The notice ID
     * @param fileName The file name to delete
     * @param userId The user ID performing the deletion
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteAttachment(String noticeNumber, String fileName, String userId) {
        try {
            // 1. Find the attachment in the database
            OcmsOffenceAttachment attachment = findAttachmentByNoticeAndFileName(noticeNumber, fileName);
            
            if (attachment == null) {
                log.warn("Attachment not found for deletion: {} for notice {}", fileName, noticeNumber);
                return false;
            }
            
            // 2. Delete from Azure Blob Storage
            String blobPath = noticeNumber + "/" + fileName;
            boolean blobDeleted = azureBlobStorageUtil.deleteFromBlob(blobPath);
            
            if (!blobDeleted) {
                log.warn("Failed to delete blob for attachment: {}", blobPath);
                // Continue with database deletion even if blob deletion fails
            }
            
            // 3. Delete from database (OPTION 1)
            offenceAttachmentService.delete(attachment.getAttachmentId());
            log.info("Successfully deleted attachment {} for notice {}", fileName, noticeNumber);
            
            return true;
        } catch (Exception e) {
            log.error("Error deleting attachment {} for notice {}: {}", fileName, noticeNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Find an attachment by notice number and file name
     * 
     * @param noticeNumber The notice ID
     * @param fileName The file name to find
     * @return The attachment entity or null if not found
     */
    private OcmsOffenceAttachment findAttachmentByNoticeAndFileName(String noticeNumber, String fileName) {
        try {
            // Create criteria to find the attachment
            Map<String, String[]> criteria = new HashMap<>();
            criteria.put("noticeNo", new String[]{noticeNumber});
            criteria.put("fileName", new String[]{fileName});
            
            // Find matching attachments
            FindAllResponse<OcmsOffenceAttachment> attachments = offenceAttachmentService.getAll(criteria);
            
            // Return the first match if found
            return attachments.getData().isEmpty() ? null : attachments.getData().get(0);
        } catch (Exception e) {
            log.error("Error finding attachment {} for notice {}: {}", fileName, noticeNumber, e.getMessage(), e);
            return null;
        }
    }
}
