package com.ocmsintranet.apiservice.workflows.notice_management.filedownload;

import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for downloading files from Azure Blob Storage
 * Provides endpoints for:
 * 1. GET /download/blob/** - Generic file download
 * 2. POST /downloads - Standard download with JSON
 * 3. POST /plus-download - PLUS Interface download
 */
@RestController
@RequestMapping("${api.version:/v1}")
@Slf4j
public class FileDownloadController {

    private final AzureBlobStorageUtil azureBlobStorageUtil;

    @Value("${azure.blob.container-name}")
    private String containerName;

    @Value("${azure.blob.notice-attachments-folder}")
    private String noticeAttachmentsFolder;

    public FileDownloadController(AzureBlobStorageUtil azureBlobStorageUtil) {
        this.azureBlobStorageUtil = azureBlobStorageUtil;
        log.info("FileDownloadController initialized");
        
        // Force initialization of Azure Blob Storage client
        try {
            Method initMethod = azureBlobStorageUtil.getClass().getDeclaredMethod("initializeIfNeeded");
            initMethod.setAccessible(true);
            initMethod.invoke(azureBlobStorageUtil);
            log.info("Successfully initialized Azure Blob Storage client");
        } catch (Exception e) {
            log.error("Failed to initialize Azure Blob Storage client", e);
        }
    }

    /**
     * Download a file from Azure Blob Storage with explicit initialization
     */
    @GetMapping("download/blob/**")
    public ResponseEntity<?> downloadFile(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(value = "container", required = false) String container) {

        // Extract the blob path from the URL
        String requestURI = request.getRequestURI();
        int blobPrefixPos = requestURI.indexOf("/download/blob/");
        if (blobPrefixPos == -1) {
            return ResponseEntity.badRequest().body("Invalid URL format");
        }
        
        // Extract everything after '/download/blob/'
        String blobPath = requestURI.substring(blobPrefixPos + "/download/blob/".length());
        log.info("Download request received for path: {}, container: {}, configured container: {}", 
                blobPath, container, containerName);

        try {
            // Ensure Azure Blob Storage client is initialized
            try {
                Method initMethod = azureBlobStorageUtil.getClass().getDeclaredMethod("initializeIfNeeded");
                initMethod.setAccessible(true);
                initMethod.invoke(azureBlobStorageUtil);
                log.info("Azure Blob Storage client initialized");
            } catch (Exception e) {
                log.error("Failed to initialize Azure Blob Storage client", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to initialize Azure Blob Storage client: " + e.getMessage());
            }

            // Download the file from Azure Blob Storage
            log.info("Attempting to download blob: {}", blobPath);
            byte[] fileContent = null;
            
            // First try with the provided container parameter if it exists
            if (container != null) {
                try {
                    log.info("Trying to download from specified container: {}", container);
                    fileContent = azureBlobStorageUtil.downloadFromBlob(blobPath, container);
                } catch (Exception e) {
                    log.warn("Failed to download from specified container: {}, error: {}", container, e.getMessage());
                }
            }
            
            // If file content is still null, try with the configured container name
            if (fileContent == null) {
                log.info("Trying to download from configured container: {}", containerName);
                fileContent = azureBlobStorageUtil.downloadFromBlob(blobPath, containerName);
            }
            
            // If still null, try the default method (which uses the configured container name)
            if (fileContent == null) {
                log.info("Trying to download using default method");
                fileContent = azureBlobStorageUtil.downloadFromBlob(blobPath);
            }

            if (fileContent == null) {
                log.warn("File not found: {}", blobPath);
                return ResponseEntity.notFound().build();
            }

            // Determine filename for the download
            String downloadFilename = blobPath.contains("/") ?
                    blobPath.substring(blobPath.lastIndexOf('/') + 1) :
                    blobPath;

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

    /**
     * Download a file from Azure Blob Storage using POST with JSON payload
     * @param request The download request containing notice number and filename
     * @return ResponseEntity containing the file or an error message
     */
    @PostMapping("downloads")
    public ResponseEntity<?> downloadFileByPost(
            @RequestBody Map<String, String> payload) {

        String noticeNo = payload.get("noticeNo");
        String fileName = payload.get("fileName");
        
        log.info("POST download request received for notice: {}, filename: {}", 
        noticeNo, fileName);

        try {
            // Ensure Azure Blob Storage client is initialized
            try {
                Method initMethod = azureBlobStorageUtil.getClass().getDeclaredMethod("initializeIfNeeded");
                initMethod.setAccessible(true);
                initMethod.invoke(azureBlobStorageUtil);
                log.info("Azure Blob Storage client initialized");
            } catch (Exception e) {
                log.error("Failed to initialize Azure Blob Storage client", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to initialize Azure Blob Storage client: " + e.getMessage());
            }

            // Validate request parameters
            if (noticeNo == null || noticeNo.isEmpty() ||
                fileName == null || fileName.isEmpty()) {
                return ResponseEntity.badRequest().body("Notice number and filename are required");
            }

            // Construct the blob path using the main folder from properties and notice number
            String blobPath = noticeAttachmentsFolder + "/" + noticeNo + "/" + fileName;
            log.info("Attempting to download blob: {}", blobPath);

            // Download the file from Azure Blob Storage
            log.info("Attempting to download blob: {}", blobPath);
            byte[] fileContent = containerName != null ?
                    azureBlobStorageUtil.downloadFromBlob(blobPath, containerName) :
                    azureBlobStorageUtil.downloadFromBlob(blobPath);

            if (fileContent == null) {
                log.warn("File not found: {}", blobPath);
                return ResponseEntity.notFound().build();
            }

            // Determine filename for the download
            String downloadFilename = blobPath.contains("/") ?
                    blobPath.substring(blobPath.lastIndexOf('/') + 1) :
                    blobPath;

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

    /**
     * Download a file from Azure Blob Storage (PLUS Interface)
     * API Spec: Download attachment by noticeNo and fileName
     *
     * Request Body Example:
     * {
     *   "noticeNo": "541000009T",
     *   "fileName": "evidence.jpg"
     * }
     *
     * Success Response (200):
     * Binary file with headers:
     * - Content-Type: application/octet-stream
     * - Content-Disposition: attachment; filename="evidence.jpg"
     * - Content-Length: {fileSize}
     *
     * Error Response (400):
     * {
     *   "data": {
     *     "appCode": "OCMS-4000",
     *     "message": "noticeNo is required"
     *   }
     * }
     *
     * Error Response (404):
     * {
     *   "data": {
     *     "appCode": "OCMS-4001",
     *     "message": "File not found"
     *   }
     * }
     */
    @PostMapping("plus-download")
    public ResponseEntity<?> downloadFileForPlus(
            @RequestBody(required = false) Map<String, String> payload) {

        log.info("Received PLUS download request with body: {}", payload);

        try {
            // Validate request body
            if (payload == null || payload.isEmpty()) {
                log.warn("Request body is empty");
                return createPlusErrorResponse("OCMS-4000", "Request body is required");
            }

            // Validate noticeNo is present
            if (!payload.containsKey("noticeNo") || payload.get("noticeNo") == null) {
                log.warn("noticeNo is missing in request body");
                return createPlusErrorResponse("OCMS-4000", "noticeNo is required");
            }

            // Validate noticeNo is not empty string
            String noticeNo = payload.get("noticeNo").trim();
            if (noticeNo.isEmpty()) {
                log.warn("noticeNo is empty string");
                return createPlusErrorResponse("OCMS-4000", "noticeNo is required");
            }

            // Validate fileName is present
            if (!payload.containsKey("fileName") || payload.get("fileName") == null) {
                log.warn("fileName is missing in request body");
                return createPlusErrorResponse("OCMS-4000", "fileName is required");
            }

            // Validate fileName is not empty string
            String fileName = payload.get("fileName").trim();
            if (fileName.isEmpty()) {
                log.warn("fileName is empty string");
                return createPlusErrorResponse("OCMS-4000", "fileName is required");
            }

            // Ensure Azure Blob Storage client is initialized
            try {
                Method initMethod = azureBlobStorageUtil.getClass().getDeclaredMethod("initializeIfNeeded");
                initMethod.setAccessible(true);
                initMethod.invoke(azureBlobStorageUtil);
                log.info("Azure Blob Storage client initialized");
            } catch (Exception e) {
                log.error("Failed to initialize Azure Blob Storage client", e);
                return createPlusErrorResponse("OCMS-5000",
                    "Something went wrong on our end. Please try again later.");
            }

            // Construct the blob path using the main folder from properties and notice number
            String blobPath = noticeAttachmentsFolder + "/" + noticeNo + "/" + fileName;
            log.info("Attempting to download blob: {}", blobPath);

            // Download the file from Azure Blob Storage
            byte[] fileContent = containerName != null ?
                    azureBlobStorageUtil.downloadFromBlob(blobPath, containerName) :
                    azureBlobStorageUtil.downloadFromBlob(blobPath);

            if (fileContent == null) {
                log.warn("File not found: {}", blobPath);
                return createPlusErrorResponse("OCMS-4001", "File not found");
            }

            // Determine filename for the download
            String downloadFilename = blobPath.contains("/") ?
                    blobPath.substring(blobPath.lastIndexOf('/') + 1) :
                    blobPath;

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", downloadFilename);
            headers.setContentLength(fileContent.length);

            log.info("Successfully downloaded file for PLUS: {}, size: {} bytes", downloadFilename, fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing PLUS download request: {}", e.getMessage(), e);
            return createPlusErrorResponse("OCMS-5000",
                "Something went wrong on our end. Please try again later.");
        }
    }

    /**
     * Create error response in PLUS API format
     */
    private ResponseEntity<?> createPlusErrorResponse(String appCode, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("appCode", appCode);
        errorData.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", errorData);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
