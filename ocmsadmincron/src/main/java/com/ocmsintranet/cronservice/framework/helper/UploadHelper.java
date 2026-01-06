package com.ocmsintranet.cronservice.framework.helper;

import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadHelper {
    private  final Logger logger = LoggerFactory.getLogger(UploadHelper.class);

    @Autowired
    private SftpUtil sftpUtil;

    @Value("${ces.download.server-name}")
    private String sftpServerName;

    public  SftpUploadResult uploadToCes(byte[] fileContent, String fileName, String sftpPath, String processName) {
        logger.info("Starting SFTP upload for {} process: {}", processName, fileName);

        SftpUploadResult result = new SftpUploadResult();
        result.setFileName(fileName);
        result.setProcessName(processName);

        try {
            String fullSftpPath = sftpPath.endsWith("/") ? sftpPath + fileName : sftpPath + "/" + fileName;
            logger.info("Uploading {} file to SFTP server '{}' at path: {}", processName, sftpServerName, fullSftpPath);

            boolean sftpUploadSuccess = sftpUtil.uploadFile(sftpServerName, fileContent, fullSftpPath);

            if (!sftpUploadSuccess) {
                String errorMsg = "Failed to upload " + processName + " file to SFTP server: " + fullSftpPath;
                logger.error(errorMsg);
                result.setSuccess(false);
                result.setError(errorMsg);
                return result;
            }

            result.setSuccess(true);
            result.setSftpPath(fullSftpPath);
            logger.info("Successfully uploaded {} file to SFTP: {}", processName, fullSftpPath);

            return result;

        } catch (Exception e) {
            String errorMsg = "Unexpected error during SFTP upload for " + processName + ": " + e.getMessage();
            logger.error(errorMsg, e);
            result.setSuccess(false);
            result.setError(errorMsg);
            return result;
        }
    }
    public static class SftpUploadResult {
        private String fileName;
        private String processName;
        private boolean success;
        private String sftpPath;
        private String error;

        // Constructors
        public SftpUploadResult() {}

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getSftpPath() { return sftpPath; }
        public void setSftpPath(String sftpPath) { this.sftpPath = sftpPath; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
