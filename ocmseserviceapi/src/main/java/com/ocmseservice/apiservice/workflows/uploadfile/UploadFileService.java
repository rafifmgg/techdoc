package com.ocmseservice.apiservice.workflows.uploadfile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface UploadFileService {
    
    /**
     * Upload HHT file to temporary storage location
     * The response includes file information that should be included in the create notice payload
     * when calling the create-notice endpoint.
     * 
     * @param request HTTP request
     * @param file File to be uploaded
     * @return Response with upload status and file details (tempFilePath, timestamp, filename)
     */
    ResponseEntity<Map<String, Object>> uploadHhtFileToTemp(HttpServletRequest request, MultipartFile file);
}
