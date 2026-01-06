package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.ComcryptFileProcessingService;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Helper class for Toppan download operations
 * Handles file listing, downloading, archiving, and cleanup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToppanDownloadHelper {
    
    private static final String SFTP_SERVER = "toppan";
    
    @Value("${toppan.archive.directory:/tmp/toppan/archive}")
    private String archiveDirectory;
    
    @Value("${toppan.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${blob.folder.toppan.download:/toppan/download/}")
    private String blobFolderPath;
    
    // Service dependencies
    private final ComcryptFileProcessingService sliftFileProcessingService;
    private final SftpUtil sftpUtil;
    private final AzureBlobStorageUtil azureBlobStorageUtil;
    
    // Store decrypted files to avoid re-decryption
    private final Map<String, byte[]> decryptedFiles = new ConcurrentHashMap<>();
    
    /**
     * List response files from SFTP server
     * Looking for files with pattern: DPT-URA-* (encrypted with .p7 extension)
     */
    public List<String> listResponseFiles(String sftpServer, String remoteDirectory) {
        log.info("Listing response files from {}:{}", sftpServer, remoteDirectory);
        
        try {
            // List all files in the SFTP directory
            List<String> allFiles = sftpUtil.listFiles(sftpServer, remoteDirectory);

            // Debug: Log all files found
            log.info("All files in directory ({}): {}", allFiles.size(), allFiles);

            // Filter for Toppan response files
            List<String> responseFiles = allFiles.stream()
                // Temporarily commented out .p7 extension check
                // .filter(f -> {
                //     boolean hasP7 = f.endsWith(".p7");
                //     if (!hasP7) {
                //         log.debug("Skipping file (no .p7 extension): {}", f);
                //     }
                //     return hasP7;
                // })
                .filter(f -> {
                    String upperName = f.toUpperCase();
                    boolean isValid = isValidResponseFile(upperName);
                    if (!isValid) {
                        log.info("Skipping file (doesn't match expected pattern): {}", f);
                    }
                    return isValid;
                })
                .collect(Collectors.toList());

            // Log found files for debugging
            responseFiles.forEach(file -> log.debug("Found response file: {}", file));

            log.info("Found {} response files", responseFiles.size());
            return responseFiles;
            
        } catch (Exception e) {
            log.error("Error listing files from SFTP", e);
            throw new RuntimeException("Error listing files from SFTP", e);
        }
    }
    
    /**
     * Validate if a file is a valid Toppan response file
     *
     * Note: Only RD2 and DN2 files are processed for database updates.
     * Other files (LOG-D2, LOG-PDF) are only uploaded to blob storage.
     */
    private boolean isValidResponseFile(String fileName) {
        String upperName = fileName.toUpperCase();
        return upperName.startsWith("DPT-URA-LOG-D2-") ||      // Data acknowledgement (blob only)
               upperName.startsWith("DPT-URA-LOG-PDF-") ||     // PDF acknowledgement (blob only)
               upperName.startsWith("DPT-URA-RD2-D2-") ||      // RD2 return file (process + blob)
               upperName.startsWith("DPT-URA-DN2-D2-") ||
               upperName.startsWith("DPT-URA-PDF-D2-") ; 
    }
    
    /**
     * Download files from SFTP (with or without encryption)
     * Returns map of filename to file content
     *
     * @param sftpServer SFTP server name
     * @param remoteDirectory Remote directory path
     * @param fileNames List of filenames to download
     * @param useEncryption Whether to decrypt files using SLIFT
     * @return Map of filename to file content
     */
    public Map<String, byte[]> downloadAndDecryptFiles(String sftpServer, String remoteDirectory,
                                                       List<String> fileNames, boolean useEncryption) {
        log.info("Downloading {} files from SFTP (encryption: {})", fileNames.size(), useEncryption);

        Map<String, byte[]> processedFiles = new HashMap<>();

        for (String fileName : fileNames) {
            try {
                // Check if already processed
                if (decryptedFiles.containsKey(fileName)) {
                    log.info("Using cached content for file: {}", fileName);
                    processedFiles.put(fileName, decryptedFiles.get(fileName));
                    continue;
                }

                byte[] fileContent;
                if (useEncryption) {
                    // Download and decrypt the file using SLIFT
                    fileContent = downloadAndDecryptFile(sftpServer, remoteDirectory, fileName);
                } else {
                    // Download directly without decryption
                    fileContent = downloadDirectlyFromSftp(sftpServer, remoteDirectory, fileName);
                }

                if (fileContent != null && fileContent.length > 0) {
                    processedFiles.put(fileName, fileContent);
                    decryptedFiles.put(fileName, fileContent);  // Cache for future use
                    log.info("Successfully processed file: {} ({} bytes)", fileName, fileContent.length);
                } else {
                    log.error("Failed to download file: {}", fileName);
                }

            } catch (Exception e) {
                log.error("Error processing file {}: {}", fileName, e.getMessage(), e);
            }
        }

        log.info("Successfully processed {} out of {} files", processedFiles.size(), fileNames.size());
        return processedFiles;
    }

    /**
     * Download and decrypt files from SFTP (backward compatibility)
     * @deprecated Use {@link #downloadAndDecryptFiles(String, String, List, boolean)} instead
     */
    @Deprecated
    public Map<String, byte[]> downloadAndDecryptFiles(String sftpServer, String remoteDirectory,
                                                       List<String> fileNames) {
        return downloadAndDecryptFiles(sftpServer, remoteDirectory, fileNames, true);
    }
    
    /**
     * Download and decrypt a single file from SFTP using SLIFT
     */
    private byte[] downloadAndDecryptFile(String sftpServer, String remoteDirectory, String fileName) {
        log.info("Downloading and decrypting file: {}", fileName);
        
        try {
            // Generate unique request ID for SLIFT tracking
            String requestId = "toppan_download_" + System.currentTimeMillis() + "_" + fileName.hashCode();
            
            // Construct full SFTP path
            String fullSftpPath = remoteDirectory;
            if (!fullSftpPath.endsWith("/") && !fileName.startsWith("/")) {
                fullSftpPath += "/";
            }
            fullSftpPath += fileName;
            
            log.info("Initiating SLIFT download and decryption for: {}", fullSftpPath);
            
            // Use SliftFileProcessingService to download and decrypt
            CompletableFuture<byte[]> future = sliftFileProcessingService.executeDownloadAndDecryptionFlowWithServerName(
                    requestId,
                    sftpServer,
                    fullSftpPath,
                    blobFolderPath,
                    SystemConstant.CryptOperation.SLIFT);
            
            // Wait for result with timeout (5 minutes should be enough for most files)
            byte[] content = future.get(5, TimeUnit.MINUTES);
            
            if (content != null && content.length > 0) {
                log.info("Successfully downloaded and decrypted file: {} ({} bytes)", fileName, content.length);
                return content;
            } else {
                log.error("Downloaded file is empty or null: {}", fileName);
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error downloading and decrypting file {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Archive processed file with timestamp
     */
    public void archiveProcessedFile(String localFilePath, String stage, LocalDateTime processingDate) {
        if (!cleanupEnabled) {
            log.debug("Archiving is disabled, skipping archive for: {}", localFilePath);
            return;
        }
        
        try {
            Path sourcePath = Paths.get(localFilePath);
            String fileName = sourcePath.getFileName().toString();
            
            // Create archive directory structure: /archive/YYYYMMDD/stage/
            String dateStr = processingDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String archivePath = String.format("%s/%s/%s", archiveDirectory, dateStr, stage);
            Files.createDirectories(Paths.get(archivePath));
            
            // Add timestamp to archived file name
            String timestamp = processingDate.format(DateTimeFormatter.ofPattern("HHmmss"));
            String archivedFileName = timestamp + "_" + fileName;
            Path targetPath = Paths.get(archivePath, archivedFileName);
            
            // Move file to archive
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archived file to: {}", targetPath);
            
        } catch (IOException e) {
            log.error("Failed to archive file {}: {}", localFilePath, e.getMessage());
        }
    }
    
    /**
     * Delete file from SFTP after successful processing
     *
     * @param sftpServer SFTP server name
     * @param remoteDirectory Remote directory path
     * @param fileName File name to delete
     * @return true if deletion succeeded, false otherwise
     */
    public boolean deleteFromSftp(String sftpServer, String remoteDirectory, String fileName) {
        try {
            String remoteFilePath = remoteDirectory;
            if (!remoteFilePath.endsWith("/") && !fileName.startsWith("/")) {
                remoteFilePath += "/";
            }
            remoteFilePath += fileName;

            log.info("Deleting processed file from SFTP: {}", remoteFilePath);

            // Use SftpUtil to delete the file
            boolean deleted = sftpUtil.deleteFile(sftpServer, remoteFilePath);

            if (deleted) {
                log.info("Successfully deleted from SFTP: {}", fileName);
                return true;
            } else {
                log.warn("Failed to delete file from SFTP: {}", fileName);
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to delete file {} from SFTP: {}", fileName, e.getMessage());
            // Don't throw exception as this is cleanup operation
            return false;
        }
    }

    /**
     * Download file directly from SFTP without encryption/decryption
     *
     * @param sftpServer SFTP server name
     * @param remoteDirectory Remote directory path
     * @param fileName Filename to download
     * @return File content as byte array, or null if failed
     */
    private byte[] downloadDirectlyFromSftp(String sftpServer, String remoteDirectory, String fileName) {
        log.info("Downloading file directly from SFTP (no encryption): {}", fileName);

        try {
            String remoteFilePath = remoteDirectory;
            if (!remoteFilePath.endsWith("/") && !fileName.startsWith("/")) {
                remoteFilePath += "/";
            }
            remoteFilePath += fileName;

            log.info("Downloading file from SFTP path: {}", remoteFilePath);

            // Use SftpUtil to download the file directly
            byte[] content = sftpUtil.downloadFile(sftpServer, remoteFilePath);

            if (content == null || content.length == 0) {
                log.error("Downloaded file is empty or null: {}", fileName);
                return null;
            }

            log.info("Successfully downloaded file: {} ({} bytes)", fileName, content.length);
            return content;

        } catch (Exception e) {
            log.error("Error downloading file {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Download encrypted file from SFTP without decryption
     * Used for uploading encrypted files to blob storage
     */
    public byte[] downloadEncryptedFile(String sftpServer, String remoteDirectory, String fileName) {
        try {
            String remoteFilePath = remoteDirectory;
            if (!remoteFilePath.endsWith("/") && !fileName.startsWith("/")) {
                remoteFilePath += "/";
            }
            remoteFilePath += fileName;

            log.info("Downloading encrypted file from SFTP: {}", remoteFilePath);

            // Download file directly from SFTP without decryption
            byte[] encryptedContent = sftpUtil.downloadFile(sftpServer, remoteFilePath);

            if (encryptedContent != null && encryptedContent.length > 0) {
                log.info("Successfully downloaded encrypted file: {} ({} bytes)", fileName, encryptedContent.length);
                return encryptedContent;
            } else {
                log.error("Downloaded encrypted file is empty or null: {}", fileName);
                return null;
            }

        } catch (Exception e) {
            log.error("Error downloading encrypted file {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Clear decrypted files cache
     */
    public void clearCache() {
        decryptedFiles.clear();
        log.debug("Cleared decrypted files cache");
    }
    
    /**
     * Clean up local download directory
     */
    public void cleanupLocalFiles() {
        if (!cleanupEnabled) {
            log.debug("Cleanup is disabled");
            return;
        }
        
        try {
            Path downloadPath = Paths.get(archiveDirectory).getParent().resolve("download");
            if (Files.exists(downloadPath)) {
                Files.walk(downloadPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.debug("Deleted local file: {}", path);
                        } catch (IOException e) {
                            log.warn("Failed to delete file {}: {}", path, e.getMessage());
                        }
                    });
            }
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage());
        }
    }

}