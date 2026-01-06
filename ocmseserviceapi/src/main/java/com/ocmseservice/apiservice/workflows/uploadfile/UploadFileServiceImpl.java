package com.ocmseservice.apiservice.workflows.uploadfile;

import com.ocmseservice.apiservice.utilities.AzureBlobStorageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for file upload operations
 */
@Service
@Slf4j
public class UploadFileServiceImpl implements UploadFileService {

    @Value("${upload.temp.directory:/temp/ocms/upload/hht}")
    private String tempUploadDirectory;
    
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    
    public UploadFileServiceImpl(AzureBlobStorageUtil azureBlobStorageUtil) {
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    /**
     * Upload HHT file to temporary storage location
     * 
     * @param request HTTP request
     * @param file File to be uploaded
     * @return Response with upload status and file details
     */
    @Override
    public ResponseEntity<Map<String, Object>> uploadHhtFileToTemp(HttpServletRequest request, MultipartFile file) {
        log.info("Received request to upload file to temp storage: {}", file.getOriginalFilename());
        
        try {
            // Generate timestamp for unique filenames (yyyyMMdd_HHmmss)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Get original filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown_file";
            }
            
            // Extract file extension and name
            String fileExtension = "";
            String fileNameWithoutExt = originalFilename;
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                fileExtension = originalFilename.substring(lastDotIndex);
                fileNameWithoutExt = originalFilename.substring(0, lastDotIndex);
            }
            
            // Create timestamped filename: filename_timestamp.ext
            String timestampedFilename = fileNameWithoutExt + "_" + timestamp + fileExtension;
            log.info("Original filename: {}, Timestamped filename: {}", originalFilename, timestampedFilename);
            
            // Upload file to Azure Blob Storage with the generated timestamp
            AzureBlobStorageUtil.FileUploadResponse response;
            try {
                // Use the Azure Blob Storage utility to upload the file with our timestamp
                response = azureBlobStorageUtil.uploadToTemp(file, timestamp);
                
                if (!response.isSuccess()) {
                    log.error("Failed to upload file to Azure Blob Storage: {}", response.getErrorMessage());
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", response.getErrorMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                }
            } catch (Exception e) {
                log.error("Error uploading file to Azure Blob Storage: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Error uploading file: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
            // Return response with file details
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("filename", timestampedFilename);
            result.put("timestamp", timestamp);
            result.put("fileSize", response.getFileSize());
            
            log.info("File uploaded successfully to temp storage. Returning filename: {}", timestampedFilename);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Failed to upload file: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }


}
