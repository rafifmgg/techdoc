package com.ocmsintranet.apiservice.workflows.notice_creation.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file upload operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    
    /**
     * Response code (0 for success, error code for failures)
     */
    private String code;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Path to the uploaded file in temporary storage
     */
    private String tempFilePath;
    
    /**
     * Timestamp directory name (yyyyMMddHHmmss)
     */
    private String timestamp;
    
    /**
     * Original filename
     */
    private String filename;
}
