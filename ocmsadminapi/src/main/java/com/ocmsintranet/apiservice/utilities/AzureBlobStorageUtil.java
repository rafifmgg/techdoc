package com.ocmsintranet.apiservice.utilities;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for Azure Blob Storage operations
 */
@Component
@org.springframework.context.annotation.Lazy
public class AzureBlobStorageUtil {
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageUtil.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private BlobServiceClient blobServiceClient;
    private final String accountEndpoint;
    private final String containerName;
    private final String tempFolder;
    private final String noticeAttachmentsFolder;
    private final long maxFileSize;
    private boolean initialized = false;
    
    public AzureBlobStorageUtil(
            @Value("${azure.storage.account-endpoint}") String accountEndpoint,
            @Value("${azure.blob.container-name}") String containerName,
            @Value("${azure.blob.temp-folder}") String tempFolder,
            @Value("${azure.blob.notice-attachments-folder}") String noticeAttachmentsFolder,
            @Value("${azure.blob.max-file-size-bytes:10485760}") long maxFileSize) {
        
        this.accountEndpoint = accountEndpoint;
        this.containerName = containerName;
        this.tempFolder = tempFolder;
        this.noticeAttachmentsFolder = noticeAttachmentsFolder;
        this.maxFileSize = maxFileSize;
        
        logger.info("AzureBlobStorageUtil initialized with: accountEndpoint={}, containerName={}, tempFolder={}, noticeAttachmentsFolder={}, maxFileSize={}",
                accountEndpoint, containerName, tempFolder, noticeAttachmentsFolder, maxFileSize);
        
        // Don't initialize Azure connections at startup
        logger.debug("AzureBlobStorageUtil constructor called - lazy initialization will be used");
    }
    
    /**
     * Initialize Azure Blob Storage client only when needed
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            logger.info("Initializing Azure Blob Storage client with account endpoint: {}", accountEndpoint);
            
            try {
                // Create BlobServiceClient using DefaultAzureCredential (Managed Identity)
                DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
                this.blobServiceClient = new BlobServiceClientBuilder()
                        .endpoint(accountEndpoint)
                        .credential(credential)
                        .buildClient();
                
                // Ensure container exists
                ensureContainerExists(containerName);
                
                initialized = true;
                logger.info("Azure Blob Storage client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Azure Blob Storage client", e);
                throw new RuntimeException("Failed to initialize Azure Blob Storage client", e);
            }
        }
    }
    
    /**
     * Ensure the specified container exists, create if it doesn't
     */
    private void ensureContainerExists(String containerName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                logger.info("Creating container: {}", containerName);
                containerClient.create();
            }
        } catch (Exception e) {
            logger.error("Failed to ensure container exists: {}", containerName, e);
            throw new RuntimeException("Failed to ensure container exists: " + containerName, e);
        }
    }
    
    /**
     * Upload file to temporary blob storage
     * @param file The file to upload
     * @return FileUploadResponse containing upload details
     * @throws IllegalArgumentException if file size exceeds maximum allowed
     */
    public FileUploadResponse uploadToTemp(MultipartFile file) {
        return uploadToTemp(file, null);
    }
    
    /**
     * Upload file to temporary blob storage with a specific timestamp
     * @param file The file to upload
     * @param providedTimestamp Optional timestamp to use in the filename (if null, a new timestamp will be generated)
     * @return FileUploadResponse containing upload details
     * @throws IllegalArgumentException if file size exceeds maximum allowed
     */
    public FileUploadResponse uploadToTemp(MultipartFile file, String providedTimestamp) {
        initializeIfNeeded();
        
        try {
            if (file.isEmpty()) {
                return new FileUploadResponse(false, "File is empty");
            }
            
            if (file.getSize() > maxFileSize) {
                String errorMsg = String.format("File size %d exceeds maximum allowed size %d", file.getSize(), maxFileSize);
                logger.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            
            // Get original filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown_file";
            }
            
            // Use provided timestamp or generate a new one
            String timestamp = providedTimestamp;
            if (timestamp == null || timestamp.isEmpty()) {
                timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            }
            
            // Extract file extension
            String fileExtension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                fileExtension = originalFilename.substring(lastDotIndex);
                originalFilename = originalFilename.substring(0, lastDotIndex);
            }

            // Create blob name with timestamp folder: temp/offencenoticefiles/{timestamp}/{originalFilename}.{extension}
            String blobName = tempFolder + "/" + timestamp + "/" + originalFilename + fileExtension;

            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Upload the file
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            
            // Verify the upload
            boolean exists = blobClient.exists();
            String blobUrl = blobClient.getBlobUrl();
            
            if (exists) {
                logger.info("File uploaded successfully to temp storage: {}", blobName);
                
                // Return response with blob details
                return new FileUploadResponse(
                        true,
                        originalFilename + fileExtension,
                        containerName + "/" + blobName,
                        blobUrl,
                        file.getSize()
                );
            } else {
                logger.error("File upload failed verification - file does not exist after upload: {}", blobName);
                return new FileUploadResponse(false, "File not found after upload");
            }
            
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw to be handled by controller
        } catch (Exception e) {
            logger.error("Failed to upload file to temp storage", e);
            return new FileUploadResponse(false, "Failed to upload file: " + e.getMessage());
        }
    }
    
    /**
     * Move file from temporary to permanent storage
     * @param tempFilePath Path of the file in temporary storage
     * @param noticeNo Notice number to organize the file in permanent storage
     * @return FileUploadResponse containing details about the moved file
     */
    public FileUploadResponse moveToPermStorage(String tempFilePath, String noticeNo) {
        initializeIfNeeded();
        
        try {
            logger.info("Moving file from temp to permanent storage. Temp path: {}, Notice: {}", tempFilePath, noticeNo);
            
            // Log the configuration values
            logger.info("Azure Blob Storage configuration - Container: {}, TempFolder: {}, NoticeAttachmentsFolder: {}", 
                    containerName, tempFolder, noticeAttachmentsFolder);
            
            // Parse the temp file path
            String expectedPrefix = containerName + "/" + tempFolder + "/";
            if (!tempFilePath.startsWith(expectedPrefix)) {
                return new FileUploadResponse(false, "Invalid temp file path format");
            }
            
            // Extract blob name from temp path (remove container and prefix)
            String tempBlobName = tempFilePath.substring(containerName.length() + 1);
            
            // Extract filename from blob name
            int lastSlashIndex = tempBlobName.lastIndexOf('/');
            if (lastSlashIndex == -1) {
                return new FileUploadResponse(false, "Invalid temp blob name format");
            }
            
            String filename = tempBlobName.substring(lastSlashIndex + 1);
        
            // Remove timestamp from filename if present (format: filename_timestamp.ext)
            String originalFilename = filename;
            int lastUnderscoreIndex = filename.lastIndexOf("_");
            if (lastUnderscoreIndex > 0) {
                int lastDotIndex = filename.lastIndexOf(".");
                if (lastDotIndex > lastUnderscoreIndex) {
                    // Check if the part between underscore and dot matches our timestamp format
                    String possibleTimestamp = filename.substring(lastUnderscoreIndex + 1, lastDotIndex);
                    if (possibleTimestamp.matches("\\d{8}_\\d{6}")) { // Format: yyyyMMdd_HHmmss
                        // Restore original filename without timestamp
                        originalFilename = filename.substring(0, lastUnderscoreIndex) + filename.substring(lastDotIndex);
                    }
                }
            }
            
            // Create destination blob name: {noticeAttachmentsFolder}/{noticeNo}/{originalFilename}
            String destBlobName = noticeAttachmentsFolder + "/" + noticeNo + "/" + originalFilename;
            logger.info("Using new folder structure for permanent storage: {}", destBlobName);
            
            // Get source blob client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient sourceBlobClient = containerClient.getBlobClient(tempBlobName);
            
            // Check if source blob exists
            boolean sourceExists = sourceBlobClient.exists();
            logger.info("Checking for source blob: {} - Exists: {}", tempBlobName, sourceExists);
            logger.info("Full source blob URL being checked: {}", sourceBlobClient.getBlobUrl());
            
            if (!sourceExists) {
                logger.error("Source file not found in temp storage. Temp blob name: {}", tempBlobName);
                return new FileUploadResponse(false, "Source file not found in temp storage");
            }
            
            // Get destination blob client
            BlobClient destBlobClient = containerClient.getBlobClient(destBlobName);
            
            // Log source and destination details
            logger.info("Source blob URL: {}", sourceBlobClient.getBlobUrl());
            logger.info("Destination blob path: {}", destBlobName);
            
            // Copy blob from source to destination
            destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl());
            
            // Verify the copy operation
            boolean destExists = destBlobClient.exists();
            logger.info("Destination blob exists after copy: {}", destExists);
            
            if (!destExists) {
                return new FileUploadResponse(false, "Failed to verify file copy");
            }
            
            // Get blob URL and properties (properties needed for getBlobSize)
            String blobUrl = destBlobClient.getBlobUrl();
            BlobProperties properties = destBlobClient.getProperties(); // Get properties to access blob size
            
            logger.info("File moved successfully to permanent storage: {}", blobUrl);
            
            // Return response with blob details
            return new FileUploadResponse(
                    true,
                    originalFilename,
                    containerName + "/" + destBlobName,
                    blobUrl,
                    properties.getBlobSize()
            );
        } catch (Exception e) {
            logger.error("Failed to move file to permanent storage", e);
            return new FileUploadResponse(false, "Failed to move file: " + e.getMessage());
        }
    }
    
    /**
     * Delete a file from temporary storage
     * @param tempFilePath Path of the file in temporary storage
     * @return boolean indicating whether the file was deleted successfully
     */
    public boolean deleteTempFile(String tempFilePath) {
        initializeIfNeeded();
        
        try {
            logger.info("Deleting file from temp storage. Temp path: {}", tempFilePath);
            
            // Parse the temp file path
            String expectedPrefix = containerName + "/" + tempFolder + "/";
            if (!tempFilePath.startsWith(expectedPrefix)) {
                logger.error("Invalid temp file path format");
                return false;
            }
            
            // Extract blob name from temp path (remove container and prefix)
            String tempBlobName = tempFilePath.substring(containerName.length() + 1);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(tempBlobName);
            
            // Delete the blob
            blobClient.delete();
            
            // Verify the deletion
            if (blobClient.exists()) {
                logger.error("Failed to verify file deletion");
                return false;
            }
            
            logger.info("File deleted successfully from temp storage");
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete file from temp storage", e);
            return false;
        }
    }
    
    /**
     * Download file from blob storage
     * @param blobPath The path to the blob in the container
     * @param containerName Optional container name, defaults to the configured container if not specified
     * @return byte array containing the file content or null if file doesn't exist
     */
    public byte[] downloadFromBlob(String blobPath, String containerName) {
        initializeIfNeeded();
        
        try {
            String container = containerName != null ? containerName : this.containerName;
            logger.info("Downloading blob from storage. Path: {}, Container: {}", blobPath, container);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            
            // Check if blob exists
            if (!blobClient.exists()) {
                logger.warn("Blob does not exist: {} in container {}", blobPath, container);
                return null;
            }
            
            // Download content
            byte[] content = blobClient.downloadContent().toBytes();
            logger.info("Successfully downloaded {} bytes from {}", content.length, blobPath);
            
            return content;
        } catch (Exception e) {
            logger.error("Failed to download blob: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download blob: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download file from the configured container
     * @param blobPath The path to the blob in the container
     * @return byte array containing the file content or null if file doesn't exist
     */
    public byte[] downloadFromBlob(String blobPath) {
        return downloadFromBlob(blobPath, this.containerName);
    }
    
    /**
     * Upload byte array directly to permanent blob storage
     * @param content The byte array content to upload
     * @param targetPath The target path in the blob container
     * @return FileUploadResponse containing upload details
     */
    public FileUploadResponse uploadBytesToBlob(byte[] content, String targetPath) {
        initializeIfNeeded();
        try {
            if (content == null || content.length == 0) {
                return new FileUploadResponse(false, "Content is empty");
            }
            
            logger.info("Uploading byte content to blob storage. Path: {}, Size: {} bytes", 
                    targetPath, content.length);
            
            // Extract filename from path
            String fileName = targetPath.contains("/") ? 
                    targetPath.substring(targetPath.lastIndexOf('/') + 1) : targetPath;
            
            // Get container client for permanent storage
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(targetPath);
            
            // Upload the content directly
            try (InputStream dataStream = new ByteArrayInputStream(content)) {
                blobClient.upload(dataStream, content.length, true);
            }
            
            // Enhanced verification logging
            boolean exists = blobClient.exists();
            String blobUrl = blobClient.getBlobUrl();
            
            logger.info("==== AZURE BLOB UPLOAD DETAILS ====");
            logger.info("Upload completed to path: {}", targetPath);
            logger.info("Container: {}", containerName);
            logger.info("Blob URL: {}", blobUrl);
            logger.info("File exists in blob storage: {}", exists);
            logger.info("Content length uploaded: {} bytes", content.length);
            logger.info("==== END AZURE BLOB UPLOAD DETAILS ====");
            
            if (exists) {
                logger.info("Successfully uploaded {} bytes to {}", content.length, targetPath);
                
                // Return response with blob details
                return new FileUploadResponse(
                        true,
                        fileName,
                        targetPath,
                        blobUrl,
                        (long) content.length
                );
            } else {
                logger.error("Blob upload failed verification - file does not exist after upload: {}", targetPath);
                return new FileUploadResponse(false, "File not found after upload");
            }
            
        } catch (Exception e) {
            logger.error("Failed to upload bytes to blob storage: {}", e.getMessage(), e);
            return new FileUploadResponse(false, "Failed to upload to blob: " + e.getMessage());
        }
    }

    /**
     * Delete a file from blob storage
     * @param blobPath Path of the file in blob storage
     * @return boolean indicating whether the file was deleted successfully
     */
    public boolean deleteFromBlob(String blobPath) {
        initializeIfNeeded();

        try {
            logger.info("Deleting file from blob storage. Path: {}", blobPath);

            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(noticeAttachmentsFolder + "/" + blobPath);

            // Check if blob exists
            if (!blobClient.exists()) {
                logger.warn("Blob does not exist: {}", blobPath);
                return false;
            }

            // Delete the blob
            blobClient.delete();

            // Verify the deletion
            boolean stillExists = blobClient.exists();
            if (stillExists) {
                logger.error("Failed to verify file deletion");
                return false;
            }

            logger.info("File deleted successfully from blob storage");
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete file from blob storage", e);
            return false;
        }
    }

    /**
     * List blobs in a container with optional prefix filter
     * @param containerName Container name to list blobs from
     * @param prefix Optional prefix to filter blobs (e.g., "folder/subfolder/")
     * @return List of BlobFileInfo containing file information
     */
    public java.util.List<BlobFileInfo> listBlobsInContainer(String containerName, String prefix) {
        initializeIfNeeded();

        java.util.List<BlobFileInfo> blobFiles = new java.util.ArrayList<>();

        try {
            logger.info("Listing blobs in container: {}, prefix: {}", containerName, prefix);

            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // List blobs with prefix
            com.azure.storage.blob.models.ListBlobsOptions options = new com.azure.storage.blob.models.ListBlobsOptions();
            if (prefix != null && !prefix.isEmpty()) {
                options.setPrefix(prefix);
            }

            // Iterate through blobs
            containerClient.listBlobs(options, null).forEach(blobItem -> {
                if (!blobItem.isPrefix()) { // Skip directory markers
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());

                    // Extract filename from path
                    String fileName = blobItem.getName();
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    }

                    BlobFileInfo fileInfo = new BlobFileInfo(
                            fileName,
                            blobItem.getName(),
                            blobClient.getBlobUrl(),
                            blobItem.getProperties().getContentLength()
                    );

                    blobFiles.add(fileInfo);
                }
            });

            logger.info("Found {} blobs in container: {}", blobFiles.size(), containerName);

        } catch (Exception e) {
            logger.error("Failed to list blobs in container: {}", containerName, e);
            throw new RuntimeException("Failed to list blobs: " + e.getMessage(), e);
        }

        return blobFiles;
    }
    
    /**
     * Response class for file operations
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileUploadResponse {
        private boolean success;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String errorMessage;
        
        public FileUploadResponse(boolean success, String fileName, String filePath, String fileUrl, Long fileSize) {
            this.success = success;
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileUrl = fileUrl;
            this.fileSize = fileSize;
        }
        
        public FileUploadResponse(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        // Getters and setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getFileUrl() {
            return fileUrl;
        }
        
        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Information class for blob files
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BlobFileInfo {
        private String fileName;
        private String blobPath;
        private String downloadUrl;
        private Long fileSize;

        public BlobFileInfo(String fileName, String blobPath, String downloadUrl, Long fileSize) {
            this.fileName = fileName;
            this.blobPath = blobPath;
            this.downloadUrl = downloadUrl;
            this.fileSize = fileSize;
        }

        // Getters and setters
        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getBlobPath() {
            return blobPath;
        }

        public void setBlobPath(String blobPath) {
            this.blobPath = blobPath;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
