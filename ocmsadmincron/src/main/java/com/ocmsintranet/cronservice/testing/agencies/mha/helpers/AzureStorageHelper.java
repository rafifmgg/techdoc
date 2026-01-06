package com.ocmsintranet.cronservice.testing.agencies.mha.helpers;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Azure Storage operations.
 * Provides functionality for uploading, downloading, and listing files in Azure Blob Storage.
 */
@Slf4j
@Component
public class AzureStorageHelper {

    private final String connectionString;
    private final String containerName;
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    /**
     * Constructor with Azure Storage connection details.
     *
     * @param connectionString Azure Storage connection string
     * @param containerName Azure Storage container name
     */
    public AzureStorageHelper(
            @Value("${ocms.azure.storage.connection-string:}") String connectionString,
            @Value("${ocms.azure.storage.container-name:mha-test}") String containerName) {
        this.connectionString = connectionString;
        this.containerName = containerName;
        
        if (connectionString != null && !connectionString.isEmpty()) {
            try {
                initializeClients();
                log.info("AzureStorageHelper initialized with container: {}", containerName);
            } catch (Exception e) {
                log.error("Failed to initialize Azure Storage clients: {}", e.getMessage());
            }
        } else {
            log.warn("Azure Storage connection string not provided, helper will operate in mock mode");
        }
    }

    /**
     * Initialize Azure Storage clients.
     */
    private void initializeClients() {
        try {
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            
            // Check if container exists, create if not
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                log.info("Creating container: {}", containerName);
                containerClient.create();
            }
        } catch (Exception e) {
            log.error("Error initializing Azure Storage clients: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Upload a file to Azure Blob Storage.
     *
     * @param blobName Name of the blob to create
     * @param content Content to upload as a string
     * @return true if upload successful
     */
    public boolean uploadFile(String blobName, String content) {
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            log.info("Mock upload of blob: {} (size: {} bytes)", blobName, content.length());
            return true;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            
            try (ByteArrayInputStream dataStream = new ByteArrayInputStream(contentBytes)) {
                blobClient.upload(dataStream, contentBytes.length, true);
            }
            
            log.info("Successfully uploaded blob: {} (size: {} bytes)", blobName, contentBytes.length);
            return true;
        } catch (Exception e) {
            log.error("Error uploading blob {}: {}", blobName, e.getMessage());
            return false;
        }
    }

    /**
     * Download a file from Azure Blob Storage.
     *
     * @param blobName Name of the blob to download
     * @return Content as a string, or null if download fails
     */
    public String downloadFile(String blobName) {
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            log.info("Mock download of blob: {}", blobName);
            return "MOCK_CONTENT_FOR_" + blobName;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (!blobClient.exists()) {
                log.warn("Blob does not exist: {}", blobName);
                return null;
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.download(outputStream);
            String content = outputStream.toString(StandardCharsets.UTF_8.name());
            
            log.info("Successfully downloaded blob: {} (size: {} bytes)", blobName, content.length());
            return content;
        } catch (IOException e) {
            log.error("Error downloading blob {}: {}", blobName, e.getMessage());
            return null;
        }
    }

    /**
     * List files in Azure Blob Storage with the given prefix.
     *
     * @param prefix Prefix to filter blobs
     * @return List of blob names
     */
    public List<String> listFiles(String prefix) {
        List<String> fileNames = new ArrayList<>();
        
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            log.info("Mock listing of blobs with prefix: {}", prefix);
            fileNames.add("MOCK_FILE_1_" + prefix);
            fileNames.add("MOCK_FILE_2_" + prefix);
            return fileNames;
        }

        try {
            ListBlobsOptions options = new ListBlobsOptions()
                    .setPrefix(prefix)
                    .setDetails(new BlobListDetails().setRetrieveMetadata(true));
            
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                fileNames.add(blobItem.getName());
            }
            
            log.info("Found {} blobs with prefix: {}", fileNames.size(), prefix);
            return fileNames;
        } catch (Exception e) {
            log.error("Error listing blobs with prefix {}: {}", prefix, e.getMessage());
            return fileNames;
        }
    }

    /**
     * Delete a file from Azure Blob Storage.
     *
     * @param blobName Name of the blob to delete
     * @return true if deletion successful
     */
    public boolean deleteFile(String blobName) {
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            log.info("Mock deletion of blob: {}", blobName);
            return true;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (!blobClient.exists()) {
                log.warn("Blob does not exist: {}", blobName);
                return false;
            }
            
            blobClient.delete();
            log.info("Successfully deleted blob: {}", blobName);
            return true;
        } catch (Exception e) {
            log.error("Error deleting blob {}: {}", blobName, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a file exists in Azure Blob Storage.
     *
     * @param blobName Name of the blob to check
     * @return true if blob exists
     */
    public boolean fileExists(String blobName) {
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            log.info("Mock check if blob exists: {}", blobName);
            return true;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            boolean exists = blobClient.exists();
            log.info("Blob {} exists: {}", blobName, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking if blob {} exists: {}", blobName, e.getMessage());
            return false;
        }
    }

    /**
     * Get the URL for a blob in Azure Storage.
     *
     * @param blobName Name of the blob
     * @return URL of the blob
     */
    public String getBlobUrl(String blobName) {
        if (containerClient == null) {
            log.warn("Azure Storage not initialized, operating in mock mode");
            return "https://mock-storage.blob.core.windows.net/" + containerName + "/" + blobName;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.getBlobUrl();
        } catch (Exception e) {
            log.error("Error getting URL for blob {}: {}", blobName, e.getMessage());
            return null;
        }
    }
}
