package com.ocmsintranet.apiservice.workflows.notice_creation.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents the file upload information that should be included
 * in the payload when calling the existing createnotice endpoints.
 * 
 * After uploading a file using the /upload/hht/temp endpoint, the response will contain
 * these fields which should be included in the createnotice payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoticeRequest {
    
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
