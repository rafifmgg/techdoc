package com.ocmsintranet.cronservice.framework.services.comcrypt;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper.ComcryptUploadProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for manual COMCRYPT framework testing
 * Enable with: ocms.comcrypt.test=true
 */
@Slf4j
@RestController
@RequestMapping("/api/comcrypt/test")
@RequiredArgsConstructor
public class ComcryptTestController {

    private final ComcryptTokenHelper comcryptTokenHelper;
    private final ComcryptUploadProcessor uploadProcessor;

    /**
     * Test encryption endpoint using LtaUploadJob method (fire-and-forget)
     * POST /api/comcrypt/test/encrypt
     *
     * Uses ComcryptTokenHelper.requestToken() - same pattern as LtaUploadJob
     * 1. Upload file to Azure Blob Storage FIRST (for callback to download later)
     * 2. Request token from COMCRYPT (fire-and-forget)
     * 3. Callback will handle: download from blob -> encrypt -> upload encrypted to SFTP
     *
     * Required payload fields: appCode, blobPath (e.g., "offence/lta/vrls/input")
     */
    @PostMapping("/encrypt")
    public ResponseEntity<Map<String, Object>> testEncryption2(@RequestBody EncryptionTestRequest request) {
        log.info("Testing encryption via HTTP using LtaUploadJob method (fire-and-forget)");

        try {
            String appCode = request.getAppCode();
            String blobFolderPath = request.getBlobPath();
            String fileName = request.getFileName() != null ? request.getFileName() : "test-fire-and-forget.txt";
            String context = request.getContext() != null ? request.getContext() : "test-fire-and-forget";
            String content = request.getContent() != null ? request.getContent() : "Default test content";

            // Validate required fields
            if (appCode == null || appCode.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "appCode is required"
                ));
            }
            if (blobFolderPath == null || blobFolderPath.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "blobPath is required (e.g., 'offence/lta/vrls/input')"
                ));
            }

            String metadata = String.format("Test encryption - context: %s, content length: %d",
                                           context, content.length());

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("appCode", appCode);
            response.put("contentLength", content.length());
            response.put("metadata", metadata);

            // Step 1: Upload file to Azure Blob Storage FIRST (for callback to download later)
            // Remove leading/trailing slashes before parsing
            String cleanBlobPath = blobFolderPath.trim().replaceAll("^/+|/+$", "");
            String[] parts = cleanBlobPath.split("/", 2);
            String container = parts[0];
            String directory = parts.length > 1 ? parts[1] : "";

            String blobPath = null;
            try {
                blobPath = uploadProcessor.uploadToBlob(container, directory, content.getBytes(), fileName);
                log.info("Successfully uploaded file to Azure Blob Storage: {}", blobPath);
                response.put("blobPath", blobPath);
            } catch (Exception e) {
                log.error("Error uploading file to blob: {}", e.getMessage(), e);
                response.put("success", false);
                response.put("error", "Failed to upload to blob: " + e.getMessage());
                return ResponseEntity.ok(response);
            }

            // Step 2: Request token from COMCRYPT (fire-and-forget pattern like LtaUploadJob)
            String requestId = comcryptTokenHelper.requestToken(
                    appCode,
                    SystemConstant.CryptOperation.ENCRYPTION,
                    fileName,
                    metadata);

            if (requestId != null) {
                response.put("success", true);
                response.put("requestId", requestId);
                response.put("message", "File uploaded to blob and token requested successfully (fire-and-forget). Callback will handle encryption.");
            } else {
                response.put("success", false);
                response.put("requestId", null);
                response.put("message", "File uploaded to blob but failed to request COMCRYPT token");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in encryption2 test", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Test decryption endpoint using fire-and-forget method
     * POST /api/comcrypt/test/decrypt
     *
     * Uses ComcryptTokenHelper.requestToken() - fire-and-forget pattern
     * 1. Request decryption token from COMCRYPT (fire-and-forget)
     * 2. Callback will handle: find file on SFTP -> download -> decrypt -> upload
     *
     * Required payload fields: appCode, fileName
     */
    @PostMapping("/decrypt")
    public ResponseEntity<Map<String, Object>> testDecryption2(@RequestBody DecryptionTestRequest request) {
        log.info("Testing decryption via HTTP using fire-and-forget method");

        try {
            String appCode = request.getAppCode();
            String fileName = request.getFileName();
            String context = request.getContext() != null ? request.getContext() : "test-decrypt";

            // Validate required fields
            if (appCode == null || appCode.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "appCode is required"
                ));
            }
            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "fileName is required"
                ));
            }

            String metadata = String.format("Test decryption - context: %s", context);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("appCode", appCode);
            response.put("metadata", metadata);

            // Request decryption token from COMCRYPT (fire-and-forget)
            // Callback will handle: find file on SFTP -> download -> decrypt -> upload
            String requestId = comcryptTokenHelper.requestToken(
                    appCode,
                    SystemConstant.CryptOperation.DECRYPTION,
                    fileName,
                    metadata);

            if (requestId != null) {
                response.put("success", true);
                response.put("requestId", requestId);
                response.put("message", "Decryption token requested successfully (fire-and-forget). Callback will handle: find file on SFTP -> download -> decrypt -> upload.");
            } else {
                response.put("success", false);
                response.put("requestId", null);
                response.put("message", "Failed to request COMCRYPT decryption token");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in decryption2 test", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // ================================
    // Request DTOs
    // ================================

    public static class EncryptionTestRequest {
        private String cryptType;
        private String appCode;
        private String content;
        private String fileName;
        private String context;
        private String blobPath;  // For encrypt: blob folder path (e.g., "offence/lta/vrls/input")

        public String getCryptType() { return cryptType; }
        public void setCryptType(String cryptType) { this.cryptType = cryptType; }
        public String getAppCode() { return appCode; }
        public void setAppCode(String appCode) { this.appCode = appCode; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getBlobPath() { return blobPath; }
        public void setBlobPath(String blobPath) { this.blobPath = blobPath; }
    }

    public static class DecryptionTestRequest {
        private String cryptType;
        private String appCode;
        private String encryptedContent;
        private String context;
        private String fileName;      // For decrypt: encrypted file name on SFTP
        private String sftpServer;    // For decrypt: SFTP server name (e.g., "lta")
        private String sftpPath;      // For decrypt: full SFTP path to encrypted file
        private String blobPath;      // For decrypt: blob folder path for callback

        public String getCryptType() { return cryptType; }
        public void setCryptType(String cryptType) { this.cryptType = cryptType; }
        public String getAppCode() { return appCode; }
        public void setAppCode(String appCode) { this.appCode = appCode; }
        public String getEncryptedContent() { return encryptedContent; }
        public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getSftpServer() { return sftpServer; }
        public void setSftpServer(String sftpServer) { this.sftpServer = sftpServer; }
        public String getSftpPath() { return sftpPath; }
        public void setSftpPath(String sftpPath) { this.sftpPath = sftpPath; }
        public String getBlobPath() { return blobPath; }
        public void setBlobPath(String blobPath) { this.blobPath = blobPath; }
    }

    public static class FullCycleTestRequest {
        private String cryptType;
        private String appCode;
        private String content;
        private String fileName;
        
        public String getCryptType() { return cryptType; }
        public void setCryptType(String cryptType) { this.cryptType = cryptType; }
        public String getAppCode() { return appCode; }
        public void setAppCode(String appCode) { this.appCode = appCode; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}
