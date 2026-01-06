package com.ocmsintranet.cronservice.utilities.sftpComponent;

import com.jcraft.jsch.*;
import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for SFTP operations using JSch.
 * Supports multiple server configurations and enhanced error handling.
 */
@Component
public class SftpUtil {
    private static final Logger logger = LoggerFactory.getLogger(SftpUtil.class);
    private final Map<String, ServerConfig> serverConfigs = new ConcurrentHashMap<>();
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final AzureKeyVaultUtil azureKeyVaultUtil;
    
    // Azure Key Vault URL for SFTP operations
    private final String sftpKeyVaultUrl;
    
    // Default server name when not specified
    private static final String DEFAULT_SERVER = "default";
    
    // Default timeout values
    private static final int DEFAULT_SESSION_TIMEOUT = 30000; // 30 seconds
    private static final int DEFAULT_CHANNEL_TIMEOUT = 15000; // 15 seconds
    
    // Default retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
    
    /**
     * Constructor with Azure Key Vault utility for SSH key retrieval
     * 
     * @param azureKeyVaultUtil The Azure Key Vault utility
     * @param sftpKeyVaultUrl The Azure Key Vault URL for SFTP operations, defaults to common URL if not specified
     */
    public SftpUtil(AzureKeyVaultUtil azureKeyVaultUtil, 
                   @org.springframework.beans.factory.annotation.Value("${ocms.azure.keyvault.sftp.url}") String sftpKeyVaultUrl) {
        this.azureKeyVaultUtil = azureKeyVaultUtil;
        this.sftpKeyVaultUrl = sftpKeyVaultUrl;
        logger.info("SftpUtil initialized with Key Vault URL: {}", sftpKeyVaultUrl);
    }
    
    /**
     * Add a server configuration
     * 
     * @param serverName Unique identifier for this server configuration
     * @param host SFTP server hostname
     * @param port SFTP server port
     * @param username SFTP username
     * @param secretName Azure Key Vault secret name for SSH private key
     */
    public void addServerConfig(String serverName, String host, int port, String username, String secretName) {
        logger.info("Adding SFTP server configuration: {} - {}@{}:{}", serverName, username, host, port);
        serverConfigs.put(serverName, new ServerConfig(host, port, username, secretName));
    }
    
    /**
     * Get a JSch session for the specified server
     * 
     * @param serverName Server configuration name
     * @return JSch session
     * @throws JSchException If session creation fails
     */
    private Session getSession(String serverName) throws JSchException {
        ServerConfig config = serverConfigs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("No server configuration found for: " + serverName);
        }
        
        // Check if we already have an active session
        Session existingSession = activeSessions.get(serverName);
        if (existingSession != null && existingSession.isConnected()) {
            return existingSession;
        }
        
        // Get private key from Azure Key Vault using explicit Key Vault URL
        String privateKeyContent;
        try {
            logger.debug("Retrieving SSH private key from Azure Key Vault for server: {} using Key Vault URL: {}", serverName, sftpKeyVaultUrl);
            privateKeyContent = azureKeyVaultUtil.getSecret(config.getSecretName(), sftpKeyVaultUrl);
        } catch (Exception e) {
            logger.error("Failed to retrieve SSH private key from Azure Key Vault for server: {}", serverName, e);
            throw new JSchException("Failed to retrieve SSH private key: " + e.getMessage());
        }
        
        if (privateKeyContent == null || privateKeyContent.isEmpty()) {
            throw new JSchException("Retrieved SSH private key is null or empty");
        }
        
        // Create JSch instance and add identity
        JSch jsch = new JSch();
        jsch.addIdentity(
            config.getUsername(),                                // Name
            privateKeyContent.getBytes(StandardCharsets.UTF_8),  // Private key
            null,                                                // Public key (null)
            null                                                 // Passphrase (null)
        );
        
        // Create session
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        
        // Skip host key checking (in production, you might want to implement proper host key verification)
        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        session.setConfig(properties);
        
        // Set timeout
        session.setTimeout(DEFAULT_SESSION_TIMEOUT);
        
        // Connect
        logger.info("Connecting to SFTP server: {}@{}:{}", config.getUsername(), config.getHost(), config.getPort());
        session.connect();
        logger.info("Successfully connected to SFTP server: {}", serverName);
        
        // Store in active sessions
        activeSessions.put(serverName, session);
        
        return session;
    }
    
    /**
     * Get a channel for SFTP operations
     * 
     * @param serverName Server configuration name
     * @return ChannelSftp instance
     * @throws JSchException If channel creation fails
     */
    private ChannelSftp getChannel(String serverName) throws JSchException {
        Session session = getSession(serverName);
        
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect(DEFAULT_CHANNEL_TIMEOUT);
            logger.debug("SFTP channel opened for server: {}", serverName);
            return (ChannelSftp) channel;
        } catch (JSchException e) {
            logger.error("Failed to open SFTP channel for server: {}", serverName, e);
            throw e;
        }
    }
    
    /**
     * Close a channel safely
     * 
     * @param channel Channel to close
     */
    private void closeChannel(ChannelSftp channel) {
        if (channel != null) {
            try {
                if (channel.isConnected()) {
                    channel.disconnect();
                    logger.debug("SFTP channel closed");
                }
            } catch (Exception e) {
                logger.warn("Error closing SFTP channel: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Close all active sessions
     */
    public void closeAllSessions() {
        logger.info("Closing all active SFTP sessions");
        for (Map.Entry<String, Session> entry : activeSessions.entrySet()) {
            try {
                Session session = entry.getValue();
                if (session != null && session.isConnected()) {
                    session.disconnect();
                    logger.debug("Closed SFTP session for server: {}", entry.getKey());
                }
            } catch (Exception e) {
                logger.warn("Error closing SFTP session for server {}: {}", entry.getKey(), e.getMessage());
            }
        }
        activeSessions.clear();
    }
    
    /**
     * Test connection to an SFTP server
     * 
     * @param serverName Server configuration name
     * @return true if connection successful
     */
    public boolean testConnection(String serverName) {
        logger.info("Testing connection to SFTP server: {}", serverName);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            boolean connected = channel.isConnected();
            logger.info("SFTP connection test result for server {}: {}", serverName, connected ? "SUCCESS" : "FAILED");
            return connected;
        } catch (Exception e) {
            logger.error("SFTP connection test failed for server {}: {}", serverName, e.getMessage());
            return false;
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Create directory recursively on SFTP server
     * 
     * @param serverName Server configuration name
     * @param directory Directory path to create
     * @throws JSchException If channel creation fails
     * @throws SftpException If directory creation fails
     */
    public void createDirectory(String serverName, String directory) throws JSchException, SftpException {
        logger.info("Creating directory on server {}: {}", serverName, directory);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            createDirectoryRecursive(channel, directory);
            logger.info("Successfully created directory: {}", directory);
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Helper method to create directories recursively
     * 
     * @param channel SFTP channel
     * @param directory Directory path
     * @throws SftpException If directory creation fails
     */
    private void createDirectoryRecursive(ChannelSftp channel, String directory) throws SftpException {
        if (directory == null || directory.isEmpty() || directory.equals("/")) {
            return;
        }
        
        String[] dirs = directory.split("/");
        String path = "";
        
        for (String dir : dirs) {
            if (dir.isEmpty()) {
                continue;
            }
            
            path += "/" + dir;
            
            try {
                channel.stat(path);
                logger.debug("Directory already exists: {}", path);
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    logger.debug("Creating directory: {}", path);
                    channel.mkdir(path);
                } else {
                    throw e;
                }
            }
        }
    }
    
    /**
     * List files in a directory on the SFTP server
     * 
     * @param serverName Server configuration name
     * @param directory Directory to list
     * @return List of filenames
     * @throws JSchException If channel creation fails
     * @throws SftpException If listing fails
     */
    public List<String> listFiles(String serverName, String directory) throws JSchException, SftpException {
        logger.info("Listing files on server {} in directory: {}", serverName, directory);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channel.ls(directory);
            List<String> files = new ArrayList<>();
            
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (!filename.equals(".") && !filename.equals("..") && !entry.getAttrs().isDir()) {
                    files.add(filename);
                }
            }
            
            logger.info("Found {} files in directory: {}", files.size(), directory);
            return files;
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Upload a file to the SFTP server
     * 
     * @param serverName Server configuration name
     * @param fileContent File content as byte array
     * @param remoteFilePath Remote file path
     * @return true if upload successful
     */
    public boolean uploadFile(String serverName, byte[] fileContent, String remoteFilePath) {
        logger.info("Uploading file to server {} at path: {}", serverName, remoteFilePath);
        ChannelSftp channel = null;
        
        // Retry configuration
        int retryCount = 0;
        boolean success = false;
        Exception lastException = null;
        
        while (retryCount < MAX_RETRIES && !success) {
            try {
                channel = getChannel(serverName);
                
                // Create parent directory if needed
                String parentDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
                createDirectoryRecursive(channel, parentDir);
                
                // Upload file (binary mode is default in JSch)
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
                    channel.put(inputStream, remoteFilePath);
                    logger.info("Successfully uploaded {} bytes to {}", fileContent.length, remoteFilePath);
                }
                
                // Verify file exists
                try {
                    channel.stat(remoteFilePath);
                    logger.info("Successfully uploaded file to: {}", remoteFilePath);
                    success = true;
                } catch (SftpException e) {
                    logger.error("File upload verification failed: {}", e.getMessage());
                    throw e;
                }
                
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                logger.warn("Upload attempt {} failed: {}", retryCount, e.getMessage());
                
                if (retryCount < MAX_RETRIES) {
                    try {
                        long delay = RETRY_DELAY_MS * (long) Math.pow(2, (double)(retryCount - 1));
                        logger.info("Retrying in {} ms...", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                closeChannel(channel);
                channel = null;
            }
        }
        
        if (!success && lastException != null) {
            logger.error("All upload attempts failed after {} retries", MAX_RETRIES, lastException);
        }
        
        return success;
    }
    
    /**
     * Download a file from the SFTP server
     * 
     * @param serverName Server configuration name
     * @param remoteFilePath Remote file path
     * @return File content as byte array
     * @throws JSchException If channel creation fails
     * @throws SftpException If download fails
     * @throws IOException If I/O error occurs
     */
    public byte[] downloadFile(String serverName, String remoteFilePath) throws JSchException, SftpException, IOException {
        logger.info("Downloading file from server {} at path: {}", serverName, remoteFilePath);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                channel.get(remoteFilePath, outputStream);
                byte[] content = outputStream.toByteArray();
                logger.info("Successfully downloaded file: {} (size: {} bytes)", remoteFilePath, content.length);
                return content;
            }
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Delete a file from the SFTP server
     * 
     * @param serverName Server configuration name
     * @param remoteFilePath Remote file path
     * @return true if deletion successful
     */
    public boolean deleteFile(String serverName, String remoteFilePath) {
        logger.info("Deleting file from server {} at path: {}", serverName, remoteFilePath);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            channel.rm(remoteFilePath);
            logger.info("Successfully deleted file: {}", remoteFilePath);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete file {}: {}", remoteFilePath, e.getMessage());
            return false;
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Check if a file exists on the SFTP server
     * 
     * @param serverName Server configuration name
     * @param remoteFilePath Remote file path
     * @return true if file exists
     */
    public boolean fileExists(String serverName, String remoteFilePath) {
        logger.info("Checking if file exists on server {} at path: {}", serverName, remoteFilePath);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            channel.stat(remoteFilePath);
            logger.info("File exists: {}", remoteFilePath);
            return true;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                logger.info("File does not exist: {}", remoteFilePath);
                return false;
            }
            logger.error("Error checking if file exists {}: {}", remoteFilePath, e.getMessage());
            return false;
        } catch (JSchException e) {
            logger.error("Connection error while checking if file exists {}: {}", remoteFilePath, e.getMessage());
            return false;
        } finally {
            closeChannel(channel);
        }
    }
    
    /**
     * Move a file on the SFTP server
     * 
     * @param serverName Server configuration name
     * @param sourceFilePath Source file path
     * @param targetFilePath Target file path
     * @return true if move successful
     */
    public boolean moveFile(String serverName, String sourceFilePath, String targetFilePath) {
        logger.info("Moving file on server {} from {} to {}", serverName, sourceFilePath, targetFilePath);
        ChannelSftp channel = null;
        
        try {
            channel = getChannel(serverName);
            
            // Create target directory if needed
            String targetDir = targetFilePath.substring(0, targetFilePath.lastIndexOf('/'));
            createDirectoryRecursive(channel, targetDir);
            
            // Move file
            channel.rename(sourceFilePath, targetFilePath);
            logger.info("Successfully moved file from {} to {}", sourceFilePath, targetFilePath);
            return true;
        } catch (Exception e) {
            logger.error("Failed to move file from {} to {}: {}", sourceFilePath, targetFilePath, e.getMessage());
            return false;
        } finally {
            closeChannel(channel);
        }
    }
    
    // Compatibility methods for backward compatibility with existing code
    
    /**
     * Upload a file to the default SFTP server
     * 
     * @param fileContent File content as byte array
     * @param remoteFileName Remote file name
     * @return true if upload successful
     */
    public boolean uploadFile(byte[] fileContent, String remoteFileName) {
        return uploadFile(DEFAULT_SERVER, fileContent, remoteFileName);
    }
    
    /**
     * Download a file from the default SFTP server
     * 
     * @param remoteFileName Remote file name
     * @return File content as byte array
     */
    public byte[] downloadFile(String remoteFileName) {
        try {
            return downloadFile(DEFAULT_SERVER, remoteFileName);
        } catch (Exception e) {
            logger.error("Failed to download file: {}", remoteFileName, e);
            throw new RuntimeException("Failed to download file: " + remoteFileName, e);
        }
    }
    
    /**
     * List files in the default SFTP server directory
     * 
     * @return List of filenames
     */
    public List<String> listFiles() {
        try {
            return listFiles(DEFAULT_SERVER, "/");
        } catch (Exception e) {
            logger.error("Failed to list files", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * List files in a specific subdirectory on the default SFTP server
     * 
     * @param subDirectory Subdirectory to list
     * @return List of filenames
     */
    public List<String> listFiles(String subDirectory) {
        try {
            return listFiles(DEFAULT_SERVER, subDirectory);
        } catch (Exception e) {
            logger.error("Failed to list files in directory: {}", subDirectory, e);
            return Collections.emptyList();
        }
    }
    public boolean isDirectory(String serverName, String remotePath) {
        ChannelSftp channel = null;
        try {
            channel = getChannel(serverName);  // May throw JSchException
            // Get the attributes of the remote path
            SftpATTRS attrs = channel.lstat(remotePath); // May throw SftpException
            return attrs.isDir();
        } catch (SftpException | JSchException e) {
            logger.error("Error checking if path is directory: {}", remotePath, e);
            return false;  // Return false if any exception occurs
        } finally {
            closeChannel(channel);
        }
    }

    public boolean deleteIfFile(String serverName, String remotePath) {
        if (isDirectory(serverName, remotePath)) {
            logger.info("deleteIfFile: path is a directory, skip: {}", remotePath);
            return false;
        }
        return deleteFile(serverName, remotePath);
    }


    /**
     * Delete a file from the default SFTP server
     * 
     * @param remoteFileName Remote file name
     * @return true if deletion successful
     */
    public boolean deleteFile(String remoteFileName) {
        return deleteFile(DEFAULT_SERVER, remoteFileName);
    }
    
    /**
     * Check if a file exists on the default SFTP server
     * 
     * @param remoteFileName Remote file name
     * @return true if file exists
     */
    public boolean fileExists(String remoteFileName) {
        return fileExists(DEFAULT_SERVER, remoteFileName);
    }
    
    /**
     * Move a file on the default SFTP server
     * 
     * @param sourceFileName Source file name
     * @param targetFileName Target file name
     * @return true if move successful
     */
    public boolean moveFile(String sourceFileName, String targetFileName) {
        return moveFile(DEFAULT_SERVER, sourceFileName, targetFileName);
    }
    
    /**
     * Get the names of all configured SFTP servers
     * 
     * @return Set of server names
     */
    public Set<String> getServerNames() {
        return new HashSet<>(serverConfigs.keySet());
    }
    
    /**
     * Get the Azure Key Vault utility instance
     * 
     * @return AzureKeyVaultUtil instance
     */
    public AzureKeyVaultUtil getAzureKeyVaultUtil() {
        return azureKeyVaultUtil;
    }
}
