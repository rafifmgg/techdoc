package com.ocmseservice.apiservice.workflows.uploadfile;

import com.ocmseservice.apiservice.utilities.AzureBlobStorageUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for handling file uploads directly to Azure Blob Storage
 */
@RestController
@RequestMapping("${api.version:/v1}/upload")
@Slf4j
public class FileUploadController {

    private final UploadFileService uploadFileService;
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    public FileUploadController(UploadFileService uploadFileService, AzureBlobStorageUtil azureBlobStorageUtil) {
        this.uploadFileService = uploadFileService;
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    /**
     * Upload a file to temporary Azure Blob Storage
     * 
     * @param file The file to upload
     * @return Response containing upload details
     */
    @PostMapping(value = "/hht/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadTempFile(@RequestParam("file") MultipartFile file) {
        log.info("Received request to upload file to temp storage: {}", file.getOriginalFilename());
        
        // Delegate to the service implementation
        return uploadFileService.uploadHhtFileToTemp(null, file);
    }
    
    /**
     * Download a file from Azure Blob Storage
     * 
     * @param blobPath Path to the blob in the container
     * @param container Optional container name (defaults to the configured container)
     * @param filename Optional filename for the downloaded file (defaults to the last part of the blob path)
     * @return The file content as a byte array
     */
    @GetMapping("/blob/{blobPath}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String blobPath,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "filename", required = false) String filename) {
        
        log.info("Received request to download blob: {}, container: {}", blobPath, container);
        
        try {
            // Download the file from Azure Blob Storage
            byte[] fileContent = container != null ? 
                azureBlobStorageUtil.downloadFromBlob(blobPath, container) : 
                azureBlobStorageUtil.downloadFromBlob(blobPath);
            
            if (fileContent == null) {
                log.warn("File not found: {}", blobPath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine filename for the download
            String downloadFilename = filename;
            if (downloadFilename == null || downloadFilename.isEmpty()) {
                downloadFilename = blobPath.contains("/") ? 
                    blobPath.substring(blobPath.lastIndexOf('/') + 1) : 
                    blobPath;
            }
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", downloadFilename);
            headers.setContentLength(fileContent.length);
            
            log.info("Successfully downloaded file: {}, size: {} bytes", downloadFilename, fileContent.length);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error downloading file: " + e.getMessage());
        }
    }
}
