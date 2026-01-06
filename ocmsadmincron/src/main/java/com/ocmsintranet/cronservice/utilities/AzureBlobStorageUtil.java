package com.ocmsintranet.cronservice.utilities;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Azure Blob Storage operations
 */
@Component
@org.springframework.context.annotation.Lazy
public class AzureBlobStorageUtil {
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageUtil.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    private BlobServiceClient blobServiceClient;
    private DefaultAzureCredential credential;
    private final String accountEndpoint;
    private final long maxFileSize;
    private final AzureKeyVaultUtil keyVaultUtil;
    private boolean initialized = false;
    
    // Container names from properties files
    private final String tempContainer;
    private final String permanentContainer;
    
    public AzureBlobStorageUtil(
            @Value("${azure.storage.account-endpoint}") String accountEndpoint,
            @Value("${azure.blob.max-file-size-bytes:10485760}") long maxFileSize,
            @Value("${azure.blob.temp-container:temp}") String tempContainer,
            @Value("${azure.blob.permanent-container:offence}") String permanentContainer,
            AzureKeyVaultUtil keyVaultUtil) {
        
        this.accountEndpoint = accountEndpoint;
        this.maxFileSize = maxFileSize;
        this.tempContainer = tempContainer;
        this.permanentContainer = permanentContainer;
        this.keyVaultUtil = keyVaultUtil;
        
        // Don't initialize Azure connections at startup
        logger.debug("AzureBlobStorageUtil constructor called - lazy initialization will be used with containers: temp={}, permanent={}", tempContainer, permanentContainer);
    }
    
    /**
     * Initialize Azure Blob Storage client only when needed
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            logger.info("Initializing Azure Blob Storage client with account endpoint: {}", accountEndpoint);
            
            try {
                // Create BlobServiceClient using DefaultAzureCredential (Managed Identity)
                // Store credential for later use with BlobClientBuilder (to avoid URL encoding issues)
                this.credential = new DefaultAzureCredentialBuilder().build();
                this.blobServiceClient = new BlobServiceClientBuilder()
                        .endpoint(accountEndpoint)
                        .credential(this.credential)
                        .buildClient();
                
                // Ensure default containers exist
                ensureContainerExists(tempContainer);
                ensureContainerExists(permanentContainer);
                
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
            logger.error("Failed to create container: {}", containerName, e);
            throw new RuntimeException("Failed to create container: " + containerName, e);
        }
    }
    
    /**
     * Parse blob folder path to extract container and directory
     * @param blobFolderPath Full blob folder path (e.g., "/offence/nro/input/")
     * @return String[] with container at index 0 and directory at index 1
     */
    private String[] parseBlobFolderPath(String blobFolderPath) {
        if (blobFolderPath == null || blobFolderPath.trim().isEmpty()) {
            return new String[] { permanentContainer, "" };
        }
        
        try {
            // Remove leading and trailing slashes and trim
            String normalizedPath = blobFolderPath.trim();
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            
            if (normalizedPath.isEmpty()) {
                return new String[] { permanentContainer, "" };
            }
            
            // Split by first slash
            int firstSlash = normalizedPath.indexOf('/');
            if (firstSlash == -1) {
                // No slash found, treat the whole string as container
                return new String[] { normalizedPath, "" };
            }
            
            String container = normalizedPath.substring(0, firstSlash);
            String directory = normalizedPath.substring(firstSlash + 1);
            
            return new String[] { container.trim(), directory.trim() };
        } catch (Exception e) {
            logger.warn("Failed to parse blob folder path: {}, using defaults", blobFolderPath, e);
            return new String[] { permanentContainer, "" };
        }
    }
    
    /**
     * Upload file to temporary blob storage
     * @param file The file to upload
     * @return FileUploadResponse containing upload details
     * @throws IllegalArgumentException if file size exceeds maximum allowed
     */
    public FileUploadResponse uploadToTemp(MultipartFile file) {
        initializeIfNeeded();
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size (%d bytes)", maxFileSize));
        }
        
        try {
            // Generate date-based path
            String datePath = LocalDateTime.now().format(DATE_FORMATTER);
            
            // Full path in temp storage: /temp/offencenoticefiles/<yyyy/mm/dd/hhmmss>/<filename>
            String blobPath = String.format("offencenoticefiles/%s/%s", datePath, file.getOriginalFilename());
            
            // Get reference to blob and upload
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(tempContainer);
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            
            logger.info("Uploading file to temporary storage: {}", blobPath);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            
            // Return response with blob details
            String blobUrl = blobClient.getBlobUrl();
            
            logger.info("File uploaded successfully to temporary storage. URL: {}", blobUrl);
            return new FileUploadResponse(
                    true,
                    file.getOriginalFilename(),
                    blobPath,
                    blobUrl,
                    file.getSize()
            );
            
        } catch (IOException e) {
            logger.error("Failed to read file content", e);
            throw new RuntimeException("Failed to read file content", e);
        } catch (Exception e) {
            logger.error("Failed to upload file to temporary storage", e);
            throw new RuntimeException("Failed to upload file to temporary storage: " + e.getMessage(), e);
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
            logger.info("Moving file from temporary to permanent storage. Path: {}, Notice: {}", 
                    tempFilePath, noticeNo);
            
           // Fix 1: Make sure path is correctly formatted with the container name
            // Check if tempFilePath already includes container name
            if (!tempFilePath.startsWith(tempContainer + "/") && !tempFilePath.startsWith("temp/")) {
                logger.debug("Adding container prefix to path: {}", tempFilePath);
            }
            
            // Get source blob (in temp storage)
            BlobContainerClient tempContainerClient = blobServiceClient.getBlobContainerClient(tempContainer);
            
            // Fix 2: Strip the container name from the path when getting blob
            String blobPath = tempFilePath;
            if (tempFilePath.startsWith(tempContainer + "/")) {
                blobPath = tempFilePath.substring((tempContainer + "/").length());
            }
            
            BlobClient sourceBlobClient = tempContainerClient.getBlobClient(blobPath);
            
            if (!sourceBlobClient.exists()) {
                logger.error("Source file not found in temporary storage: {}", tempFilePath);
                throw new IllegalArgumentException("Source file not found in temporary storage: " + tempFilePath);
            }
            
            // Get the filename from the full path
            String fileName = tempFilePath.substring(tempFilePath.lastIndexOf('/') + 1);
            
            // Define destination path: /offence/notice_no/<filename>
            String destBlobPath = String.format("offence/%s/%s", noticeNo, fileName);
            
            // Get target blob (in permanent storage)
            BlobContainerClient permContainerClient = blobServiceClient.getBlobContainerClient(permanentContainer);
            BlobClient targetBlobClient = permContainerClient.getBlobClient(destBlobPath);
            
            // Get properties of source blob
            BlobProperties sourceProperties = sourceBlobClient.getProperties();
            long contentLength = sourceProperties.getBlobSize();
            
            // Download content from source
            byte[] content = sourceBlobClient.downloadContent().toBytes();
            
            // Upload to destination
            try (InputStream dataStream = new ByteArrayInputStream(content)) {
                targetBlobClient.upload(dataStream, contentLength, true);
            }
            
            logger.info("File moved successfully to permanent storage. Path: {}", destBlobPath);
            
            // Return details of the moved file
            return new FileUploadResponse(
                    true,
                    fileName,
                    destBlobPath,
                    targetBlobClient.getBlobUrl(),
                    contentLength
            );
            
        } catch (Exception e) {
            logger.error("Failed to move file to permanent storage", e);
            throw new RuntimeException("Failed to move file to permanent storage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete files from temporary storage that are older than the specified number of days
     * @param daysOld Number of days old the files should be to be deleted
     * @return List of deleted file paths
     */
    public List<String> cleanupTempFiles(int daysOld) {
        initializeIfNeeded();
        List<String> deletedFiles = new ArrayList<>();
        
        try {
            logger.info("Starting cleanup of temporary files older than {} days", daysOld);
            
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(tempContainer);
            OffsetDateTime cutoffTime = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(daysOld);
            
            // List all blobs in the container
            containerClient.listBlobs().forEach(blobItem -> {
                if (blobItem.getProperties().getCreationTime().isBefore(cutoffTime)) {
                    String blobPath = blobItem.getName();
                    BlobClient blobClient = containerClient.getBlobClient(blobPath);
                    
                    try {
                        logger.info("Deleting old temporary file: {}", blobPath);
                        blobClient.delete();
                        deletedFiles.add(blobPath);
                    } catch (BlobStorageException ex) {
                        if (ex.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
                            logger.warn("Blob already deleted: {}", blobPath);
                        } else {
                            logger.error("Failed to delete blob: {}", blobPath, ex);
                        }
                    }
                }
            });
            
            logger.info("Temporary file cleanup completed. Deleted {} files", deletedFiles.size());
            return deletedFiles;
            
        } catch (Exception e) {
            logger.error("Failed to clean up temporary files", e);
            throw new RuntimeException("Failed to clean up temporary files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download file from blob storage
     * @param blobPath The full blob path including container (e.g., "offence/nro/input/file.dat")
     * @return byte array containing the file content or null if file doesn't exist
     */
    public byte[] downloadFromBlob(String blobPath) {
        initializeIfNeeded();
        
        try {
            // Parse container from blob path
            String[] pathParts = parseBlobFolderPath(blobPath);
            String container = pathParts[0];
            String actualBlobPath = pathParts[1];
            
            // If no directory part, treat the whole path as blob path in parsed container
            if (actualBlobPath.isEmpty()) {
                actualBlobPath = blobPath.substring(blobPath.indexOf('/') + 1);
            }
            
            logger.info("Downloading blob from storage. Path: {}, Container: {}", actualBlobPath, container);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(actualBlobPath);
            
            // Check if blob exists
            if (!blobClient.exists()) {
                logger.warn("Blob does not exist: {} in container {}", actualBlobPath, container);
                return null;
            }
            
            // Download content
            byte[] content = blobClient.downloadContent().toBytes();
            logger.info("Successfully downloaded {} bytes from {}", content.length, actualBlobPath);
            
            return content;
        } catch (Exception e) {
            logger.error("Failed to download blob: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download blob: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload byte array directly to blob storage using full blob path
     * @param content The byte array content to upload
     * @param fullBlobPath The full blob path including container (e.g., "offence/nro/input/filename.dat")
     * @return FileUploadResponse containing upload details
     */
    public FileUploadResponse uploadBytesToBlob(byte[] content, String fullBlobPath) {
        initializeIfNeeded();
        try {
            if (content == null || content.length == 0) {
                return new FileUploadResponse(false, "Content is empty");
            }

            logger.info("Uploading byte content to blob storage. Full path: {}, Size: {} bytes",
                    fullBlobPath, content.length);

            // Normalize path - remove leading slash and decode any URL-encoded characters
            String normalizedPath = fullBlobPath;
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }

            // Decode URL-encoded characters if present
            try {
                normalizedPath = java.net.URLDecoder.decode(normalizedPath, "UTF-8");
            } catch (Exception e) {
                // Path is not URL-encoded, use as-is
                logger.debug("Path is not URL-encoded, using as-is: {}", normalizedPath);
            }

            // Extract container name (first segment) and blob path (rest)
            String containerName;
            String actualBlobPath;

            int firstSlash = normalizedPath.indexOf('/');
            if (firstSlash > 0) {
                containerName = normalizedPath.substring(0, firstSlash);
                actualBlobPath = normalizedPath.substring(firstSlash + 1);
            } else {
                // No slash, use permanent container as default
                containerName = permanentContainer;
                actualBlobPath = normalizedPath;
            }

            // Extract filename from path
            String fileName = actualBlobPath.contains("/") ?
                    actualBlobPath.substring(actualBlobPath.lastIndexOf('/') + 1) : actualBlobPath;

            logger.info("Parsed path - Container: {}, BlobPath: {}, FileName: {}",
                    containerName, actualBlobPath, fileName);

            // Ensure container exists
            ensureContainerExists(containerName);

            // Build direct blob endpoint URL to avoid double URL encoding with Netty 4.2.x
            // containerClient.getBlobClient() URL-encodes the path, causing / to become %2F
            // which Netty 4.2.x strict URI validation rejects
            String blobEndpoint = accountEndpoint;
            if (!blobEndpoint.endsWith("/")) {
                blobEndpoint += "/";
            }
            blobEndpoint += containerName + "/" + actualBlobPath;

            logger.info("Using direct BlobClientBuilder with endpoint: {}", blobEndpoint);

            // Use BlobClientBuilder with OkHttp HttpClient to avoid URL encoding issues with Netty 4.2.x
            // OkHttp doesn't have the strict URI validation that Netty 4.2.x has
            BlobClient blobClient = new BlobClientBuilder()
                    .endpoint(blobEndpoint)
                    .credential(this.credential)
                    .httpClient(HttpClient.createDefault())  // Uses OkHttp when azure-core-http-okhttp is on classpath
                    .buildClient();

            // Upload the content directly using BinaryData
            blobClient.upload(com.azure.core.util.BinaryData.fromBytes(content), true);

            // Enhanced verification logging
            boolean exists = blobClient.exists();
            String blobUrl = blobClient.getBlobUrl();

            logger.info("==== AZURE BLOB UPLOAD DETAILS ====");
            logger.info("Upload completed to path: {}", actualBlobPath);
            logger.info("Container: {}", containerName);
            logger.info("Blob URL: {}", blobUrl);
            logger.info("File exists in blob storage: {}", exists);
            logger.info("Content length uploaded: {} bytes", content.length);
            logger.info("==== END AZURE BLOB UPLOAD DETAILS ====");

            if (exists) {
                logger.info("Successfully uploaded {} bytes to {}", content.length, actualBlobPath);

                // Return response with blob details
                return new FileUploadResponse(
                        true,
                        fileName,
                        actualBlobPath,
                        blobUrl,
                        (long) content.length
                );
            } else {
                logger.error("Blob upload failed verification - file does not exist after upload: {}", actualBlobPath);
                return new FileUploadResponse(false, "File not found after upload");
            }

        } catch (Exception e) {
            logger.error("Failed to upload bytes to blob storage: {}", e.getMessage(), e);
            return new FileUploadResponse(false, "Failed to upload to blob: " + e.getMessage());
        }
    }

    /**
     * List files in blob storage with path prefix filtering
     * @param pathPrefix The full path prefix including container (e.g., "offence/lta/vrls/input/")
     * @return List of file paths that match the prefix
     */
    public List<String> listFiles(String pathPrefix) {
        initializeIfNeeded();
        
        try {
            // Parse container from path prefix
            String[] pathParts = parseBlobFolderPath(pathPrefix);
            String container = pathParts[0];
            String actualPathPrefix = pathParts[1];
            
            if (!actualPathPrefix.isEmpty() && !actualPathPrefix.endsWith("/")) {
                actualPathPrefix += "/";
            }
            
            logger.info("Listing files from blob storage. Path prefix: {}, Container: {}", actualPathPrefix, container);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            
            // Create list options with prefix
            ListBlobsOptions options = new ListBlobsOptions().setPrefix(actualPathPrefix);
            
            // List blobs and collect file paths
            List<String> filePaths = new ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                filePaths.add(blobItem.getName());
            }
            
            logger.info("Successfully listed {} files from blob storage with prefix: {}", filePaths.size(), actualPathPrefix);
            return filePaths;
            
        } catch (Exception e) {
            logger.error("Failed to list files from blob storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list files from blob storage: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a specific file from blob storage
     * @param blobPath The full path to the blob including container (e.g., "offence/nro/input/file.dat")
     * @return true if file was deleted successfully, false if file didn't exist
     */
    public boolean deleteFile(String blobPath) {
        initializeIfNeeded();
        
        try {
            // Parse container from blob path
            String[] pathParts = parseBlobFolderPath(blobPath);
            String container = pathParts[0];
            String actualBlobPath = pathParts[1];
            
            // If no directory part, treat the whole path as blob path in parsed container
            if (actualBlobPath.isEmpty()) {
                actualBlobPath = blobPath.substring(blobPath.indexOf('/') + 1);
            }
            
            logger.info("Deleting file from blob storage. Path: {}, Container: {}", actualBlobPath, container);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(actualBlobPath);
            
            // Check if blob exists before deletion
            if (!blobClient.exists()) {
                logger.warn("Blob does not exist, cannot delete: {} in container {}", actualBlobPath, container);
                return false;
            }
            
            // Delete the blob
            blobClient.delete();
            
            logger.info("Successfully deleted file from blob storage: {}", actualBlobPath);
            return true;
            
        } catch (BlobStorageException ex) {
            if (ex.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
                logger.warn("Blob not found during deletion: {}", blobPath);
                return false;
            } else {
                logger.error("Failed to delete blob: {}", blobPath, ex);
                throw new RuntimeException("Failed to delete blob: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            logger.error("Failed to delete file from blob storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from blob storage: " + e.getMessage(), e);
        }
    }

    /**
     * Clean up files in specific path that are older than specified days
     * @param pathPrefix The full path prefix including container (e.g., "offence/temp/" or "temp/files/")
     * @param daysOld Number of days old the files should be to be deleted
     * @return List of deleted file paths
     */
    public List<String> cleanupFiles(String pathPrefix, int daysOld) {
        initializeIfNeeded();
        List<String> deletedFiles = new ArrayList<>();
        
        try {
            // Parse container from path prefix
            String[] pathParts = parseBlobFolderPath(pathPrefix);
            String container = pathParts[0];
            String actualPathPrefix = pathParts[1];
            
            if (!actualPathPrefix.isEmpty() && !actualPathPrefix.endsWith("/")) {
                actualPathPrefix += "/";
            }
            
            logger.info("Starting cleanup of files older than {} days in path: {}, container: {}", 
                    daysOld, actualPathPrefix, container);
            
            // Get container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            OffsetDateTime cutoffTime = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(daysOld);
            
            // Create list options with prefix
            ListBlobsOptions options = new ListBlobsOptions().setPrefix(actualPathPrefix);
            
            // List blobs with prefix and delete old ones
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getProperties().getCreationTime().isBefore(cutoffTime)) {
                    String blobPath = blobItem.getName();
                    BlobClient blobClient = containerClient.getBlobClient(blobPath);
                    
                    try {
                        logger.info("Deleting old file: {}", blobPath);
                        blobClient.delete();
                        deletedFiles.add(blobPath);
                    } catch (BlobStorageException ex) {
                        if (ex.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
                            logger.warn("Blob already deleted during cleanup: {}", blobPath);
                        } else {
                            logger.error("Failed to delete blob during cleanup: {}", blobPath, ex);
                        }
                    }
                }
            }
            
            logger.info("File cleanup completed. Deleted {} files", deletedFiles.size());
            return deletedFiles;
            
        } catch (Exception e) {
            logger.error("Failed to clean up files: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clean up files: " + e.getMessage(), e);
        }
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

        // Constructor for successful upload
        public FileUploadResponse(boolean success, String fileName, String filePath, String fileUrl, Long fileSize) {
            this.success = success;
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileUrl = fileUrl;
            this.fileSize = fileSize;
        }

        // Constructor for failed upload
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
}