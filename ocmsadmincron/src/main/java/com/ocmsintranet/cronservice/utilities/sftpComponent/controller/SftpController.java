package com.ocmsintranet.cronservice.utilities.sftpComponent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ocmsintranet.cronservice.utilities.sftpComponent.ServerConfig;
import org.springframework.web.multipart.MultipartFile;


import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/sftp")
@org.springframework.context.annotation.Lazy
public class SftpController {

    private final SftpUtil sftpUtil;
    private final Logger logger = LoggerFactory.getLogger(SftpController.class);
    
    public SftpController(SftpUtil sftpUtil) {
        this.sftpUtil = sftpUtil;
    }
    
    /**
     * Upload a file to the SFTP server
     * @param file The file to upload
     * @param remoteFilename Optional remote filename, if not provided the original filename is used
     * @param server Optional server name parameter, defaults to the default server if not provided
     * @return Response with upload status
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "remoteFilename", required = false) String remoteFilename,
            @RequestParam(value = "server", required = false) String server) {
        
        try {
            logger.info("Received file upload request: {} for server: {}", file.getOriginalFilename(), server != null ? server : "default");
            
            // Use original filename if remote filename is not provided
            String filename = remoteFilename != null ? remoteFilename : file.getOriginalFilename();
            
            boolean result;
            if (server != null) {
                result = sftpUtil.uploadFile(server, file.getBytes(), filename);
            } else {
                result = sftpUtil.uploadFile(file.getBytes(), filename);
            }
            
            if (result) {
                logger.info("File uploaded successfully: {}", filename);
                return ResponseEntity.ok("File uploaded successfully: " + filename);
            } else {
                logger.error("Failed to upload file: {}", filename);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload file: " + filename);
            }
        } catch (Exception e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }
    
    /**
     * Upload text content as a file to the SFTP server
     * @param request Map containing content, filename, and optional server
     * @return Response with upload status
     */
    @PostMapping("/upload/text")
    public ResponseEntity<String> uploadTextContent(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String filename = request.get("filename");
            String server = request.get("server");
            
            if (content == null || filename == null) {
                return ResponseEntity.badRequest().body("Content and filename are required");
            }
            
            logger.info("Received text upload request for file: {} for server: {}", filename, server != null ? server : "default");
            
            boolean result;
            if (server != null) {
                result = sftpUtil.uploadFile(server, content.getBytes(), filename);
            } else {
                result = sftpUtil.uploadFile(content.getBytes(), filename);
            }
            
            if (result) {
                logger.info("Text file uploaded successfully: {}", filename);
                return ResponseEntity.ok("Text file uploaded successfully: " + filename);
            } else {
                logger.error("Failed to upload text file: {}", filename);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload text file: " + filename);
            }
        } catch (Exception e) {
            logger.error("Error uploading text file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading text file: " + e.getMessage());
        }
    }
    
    /**
     * Download a file from the SFTP server
     * @param request The HTTP request
     * @param server Optional server name parameter, defaults to the default server if not provided
     * @return The file as a resource
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(
            HttpServletRequest request,
            @RequestParam(value = "server", required = false) String server) {
        try {
            // Extract the full path after "/download/"
            String uri = request.getRequestURI();
            String baseUri = "/ocms/v1/sftp/download";
            
            // Extract the path after the base URI
            String fullPath;
            if (uri.startsWith(baseUri)) {
                fullPath = uri.substring(baseUri.length());
                if (fullPath.startsWith("/")) {
                    fullPath = fullPath.substring(1); // Remove leading slash
                }
            } else {
                logger.error("Unexpected URI format: {}", uri);
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Received download request for file: {} from server: {}", fullPath, server != null ? server : "default");
            
            byte[] fileContent;
            if (server != null) {
                fileContent = sftpUtil.downloadFile(server, fullPath);
            } else {
                fileContent = sftpUtil.downloadFile(fullPath);
            }
            
            // Extract just the filename part for the Content-Disposition header
            String filename = fullPath.contains("/") ? 
                            fullPath.substring(fullPath.lastIndexOf('/') + 1) : 
                            fullPath;
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            
            logger.info("File downloaded successfully: {} from server: {}", fullPath, server != null ? server : "default");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileContent.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
        
    /**
     * List all files in the SFTP directory
     * @param request The HTTP request
     * @param server Optional server name parameter, defaults to the default server if not provided
     * @return List of files in the directory
     */
    @GetMapping({"/list", "/list/**"})
    public ResponseEntity<List<String>> listFiles(
            HttpServletRequest request,
            @RequestParam(value = "server", required = false) String server) {
        try {
            // Use the provided server name or default if not provided
            String serverName = server != null ? server : "default";
            
            String uri = request.getRequestURI();
            String baseUri = "/ocms/v1/sftp/list";
            
            // Check if we're listing the root or a subdirectory
            if (uri.equals(baseUri) || uri.equals(baseUri + "/")) {
                // List root directory
                logger.info("Received request to list files in root directory using server: {}", serverName);
                List<String> files;
                if ("default".equals(serverName)) {
                    files = sftpUtil.listFiles();
                } else {
                    files = sftpUtil.listFiles(serverName, "/");
                }
                logger.info("Retrieved file list from root directory using server {}, count: {}", serverName, files.size());
                return ResponseEntity.ok(files);
            } else {
                // List subdirectory
                String subdirectory = uri.substring(baseUri.length());
                // Remove leading slash if present
                if (subdirectory.startsWith("/")) {
                    subdirectory = subdirectory.substring(1);
                }
                logger.info("Received request to list files in subdirectory: {} using server: {}", subdirectory, serverName);
                List<String> files;
                if ("default".equals(serverName)) {
                    files = sftpUtil.listFiles(subdirectory);
                } else {
                    files = sftpUtil.listFiles(serverName, subdirectory);
                }
                logger.info("Retrieved file list for subdirectory {} using server {}, count: {}", subdirectory, serverName, files.size());
                return ResponseEntity.ok(files);
            }
        } catch (Exception e) {
            logger.error("Error listing files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a file from the SFTP server
     * @param request The HTTP request
     * @param server Optional server name parameter, defaults to the default server if not provided
     * @return Response with delete status
     */
    @DeleteMapping("/delete/**")
    public ResponseEntity<String> deleteFile(
            HttpServletRequest request,
            @RequestParam(value = "server", required = false) String server) {
        try {
            // Extract the full path after "/delete/"
            String uri = request.getRequestURI();
            String baseUri = "/ocms/v1/sftp/delete";
            
            // Extract the path after the base URI
            String fullPath;
            if (uri.startsWith(baseUri)) {
                fullPath = uri.substring(baseUri.length());
                if (fullPath.startsWith("/")) {
                    fullPath = fullPath.substring(1); // Remove leading slash
                }
            } else {
                logger.error("Unexpected URI format: {}", uri);
                return ResponseEntity.badRequest().body("Invalid request URI format");
            }
            
            logger.info("Received request to delete file with path: {} from server: {}", fullPath, server != null ? server : "default");
            
            boolean result;
            if (server != null) {
                result = sftpUtil.deleteFile(server, fullPath);
            } else {
                result = sftpUtil.deleteFile(fullPath);
            }
            
            if (result) {
                logger.info("File deleted successfully: {} from server: {}", fullPath, server != null ? server : "default");
                return ResponseEntity.ok("File deleted successfully: " + fullPath);
            } else {
                logger.warn("Failed to delete file (possibly not found): {} on server: {}", fullPath, server != null ? server : "default");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Failed to delete file: " + fullPath);
            }
        } catch (Exception e) {
            logger.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting file: " + e.getMessage());
        }
    }
    
    /**
     * Check if a file exists on the SFTP server
     * @param request The HTTP request
     * @param server Optional server name parameter, defaults to the default server if not provided
     * @return True if file exists, false otherwise
     */
    @GetMapping("/exists/**")
    public ResponseEntity<Boolean> fileExists(
            HttpServletRequest request,
            @RequestParam(value = "server", required = false) String server) {
        try {
            // Extract the full path after "/exists/"
            String uri = request.getRequestURI();
            String baseUri = "/ocms/v1/sftp/exists";
            
            // Extract the path after the base URI
            String fullPath;
            if (uri.startsWith(baseUri)) {
                fullPath = uri.substring(baseUri.length());
                if (fullPath.startsWith("/")) {
                    fullPath = fullPath.substring(1); // Remove leading slash
                }
            } else {
                logger.error("Unexpected URI format: {}", uri);
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Checking if file exists: {} on server: {}", fullPath, server != null ? server : "default");
            
            boolean exists;
            if (server != null) {
                exists = sftpUtil.fileExists(server, fullPath);
            } else {
                exists = sftpUtil.fileExists(fullPath);
            }
            
            logger.info("File {} exists: {} on server: {}", fullPath, exists, server != null ? server : "default");
            
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Error checking if file exists", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Move/rename a file on the SFTP server
     * @param request Map containing sourceFilename, targetFilename, and optional server
     * @return Response with move status
     */
    @PostMapping("/move")
    public ResponseEntity<String> moveFile(@RequestBody Map<String, String> request) {
        try {
            String sourceFilename = request.get("sourceFilename");
            String targetFilename = request.get("targetFilename");
            String server = request.get("server");
            
            if (sourceFilename == null || targetFilename == null) {
                return ResponseEntity.badRequest().body("Source and target filenames are required");
            }
            
            logger.info("Moving file from {} to {} on server: {}", sourceFilename, targetFilename, server != null ? server : "default");
            
            boolean result;
            if (server != null) {
                result = sftpUtil.moveFile(server, sourceFilename, targetFilename);
            } else {
                result = sftpUtil.moveFile(sourceFilename, targetFilename);
            }
            
            if (result) {
                logger.info("File moved successfully from {} to {} on server: {}", sourceFilename, targetFilename, server != null ? server : "default");
                return ResponseEntity.ok("File moved successfully from " + sourceFilename + " to " + targetFilename);
            } else {
                logger.error("Failed to move file from {} to {} on server: {}", sourceFilename, targetFilename, server != null ? server : "default");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to move file from " + sourceFilename + " to " + targetFilename);
            }
        } catch (Exception e) {
            logger.error("Error moving file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error moving file: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for SFTP connections.
     * @return Status of all configured SFTP servers
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        logger.info("Performing SFTP health check for all configured servers");
        
        // Get all server names from the SftpUtil
        Set<String> serverNames = sftpUtil.getServerNames();
        
        if (serverNames.isEmpty()) {
            logger.warn("No SFTP servers are configured");
            Map<String, String> response = new HashMap<>();
            response.put("status", "No SFTP servers configured");
            return ResponseEntity.ok(response);
        }
        
        // Test each server and collect results
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> serversStatus = new HashMap<>();
        boolean allHealthy = true;
        
        for (String serverName : serverNames) {
            Map<String, Object> serverStatus = new HashMap<>();
            
            // Add server configuration details
            try {
                // Get server configuration using reflection
                java.lang.reflect.Field serverConfigsField = sftpUtil.getClass().getDeclaredField("serverConfigs");
                serverConfigsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, ServerConfig> serverConfigs = (Map<String, ServerConfig>) serverConfigsField.get(sftpUtil);
                
                ServerConfig config = serverConfigs.get(serverName);
                if (config != null) {
                    Map<String, Object> serverConfig = new HashMap<>();
                    serverConfig.put("host", config.getHost());
                    serverConfig.put("port", config.getPort());
                    serverConfig.put("username", config.getUsername());
                    serverConfig.put("secretname", config.getSecretName());
                    
                    // You can uncomment this if you want to include secret values
                    try {
                        String secretValue = sftpUtil.getAzureKeyVaultUtil().getSecret(config.getSecretName());
                        if (secretValue != null && !secretValue.isEmpty()) {
                            serverConfig.put("secretname_value", secretValue);
                        }
                    } catch (Exception ex) {
                        logger.warn("Could not retrieve secret for {}: {}", serverName, ex.getMessage());
                    }
                    
                    serverStatus.put("server", serverConfig);
                }
            } catch (Exception e) {
                logger.warn("Could not retrieve server configuration for {}: {}", serverName, e.getMessage());
            }
            
            try {
                // Test connection
                boolean connectionSuccess = sftpUtil.testConnection(serverName);
                
                if (connectionSuccess) {
                    // If connection successful, try to list files
                    try {
                        List<String> files = sftpUtil.listFiles(serverName, "/");
                        serverStatus.put("status", "healthy");
                        serverStatus.put("fileCount", files.size());
                        serverStatus.put("files", files.size() > 10 ? files.subList(0, 10) : files);
                        logger.info("SFTP server '{}' health check successful, found {} files", serverName, files.size());
                    } catch (Exception e) {
                        serverStatus.put("status", "connected but cannot list files");
                        serverStatus.put("error", e.getMessage());
                        allHealthy = false;
                        logger.warn("SFTP server '{}' connected but failed to list files: {}", serverName, e.getMessage());
                    }
                } else {
                    serverStatus.put("status", "connection failed");
                    allHealthy = false;
                    logger.error("SFTP server '{}' connection test failed", serverName);
                }
            } catch (Exception e) {
                serverStatus.put("status", "error");
                serverStatus.put("error", e.getMessage());
                allHealthy = false;
                logger.error("SFTP server '{}' health check error: {}", serverName, e.getMessage());
            }
            
            serversStatus.put(serverName, serverStatus);
        }
        
        // Add Azure Key Vault connectivity test
        try {
            boolean kvConnected = sftpUtil.getAzureKeyVaultUtil().testConnection();
            Map<String, Object> kvStatus = new HashMap<>();
            kvStatus.put("status", kvConnected ? "healthy" : "connection failed");
            serversStatus.put("azureKeyVault", kvStatus);
            
            if (!kvConnected) {
                allHealthy = false;
                logger.error("Azure Key Vault connection test failed");
            } else {
                logger.info("Azure Key Vault connection test successful");
            }
        } catch (Exception e) {
            Map<String, Object> kvStatus = new HashMap<>();
            kvStatus.put("status", "error");
            kvStatus.put("error", e.getMessage());
            serversStatus.put("azureKeyVault", kvStatus);
            allHealthy = false;
            logger.error("Azure Key Vault health check error: {}", e.getMessage());
        }
        
        // Build the final response
        response.put("overallStatus", allHealthy ? "All connections healthy" : "Some connections failed");
        response.put("healthy", allHealthy);
        response.put("servers", serversStatus);
        
        HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(httpStatus).body(response);
    }

    
}