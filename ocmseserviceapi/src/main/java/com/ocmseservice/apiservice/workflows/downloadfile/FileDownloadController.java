package com.ocmseservice.apiservice.workflows.downloadfile;

import com.ocmseservice.apiservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

/**
 * Fixed controller for downloading files from Azure Blob Storage
 */
@RestController
@RequestMapping("${api.version:/v1}")
@Slf4j
public class FileDownloadController {

    private final AzureBlobStorageUtil azureBlobStorageUtil;

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
    @GetMapping("/download/blob/**")
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
        log.info("Download request received for path: {}, container: {}", blobPath, container);

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
            byte[] fileContent = container != null ?
                    azureBlobStorageUtil.downloadFromBlob(blobPath, container) :
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
}
