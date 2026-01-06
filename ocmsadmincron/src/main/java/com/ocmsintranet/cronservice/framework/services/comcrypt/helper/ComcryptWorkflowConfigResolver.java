package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves workflow configuration from requestId/appCode.
 * No additional properties needed - uses existing properties and derives
 * encrypt type (SLIFT/PGP) from appCode pattern.
 *
 * Mapping:
 * - SLIFT: LTA, MHA, TOPPAN
 * - PGP: CES, REPCCS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptWorkflowConfigResolver {

    // LTA upload config (existing properties)
    @Value("${sftp.folders.lta.upload}")
    private String ltaSftpFolder;
    @Value("${blob.folder.lta.upload}")
    private String ltaBlobFolder;

    // LTA download config (existing properties)
    @Value("${sftp.folders.lta.download}")
    private String ltaDownloadSftpFolder;
    @Value("${blob.folder.lta.download}")
    private String ltaDownloadBlobFolder;

    // MHA config (existing properties)
    @Value("${sftp.folders.mha.upload}")
    private String mhaSftpFolder;
    @Value("${blob.folder.mha.upload}")
    private String mhaBlobFolder;

    // TOPPAN config (existing properties)
    @Value("${sftp.folders.toppan.upload}")
    private String toppanSftpFolder;
    @Value("${blob.folder.toppan.upload}")
    private String toppanBlobFolder;

    // CES config (existing properties)
    @Value("${sftp.folders.ces.in}")
    private String cesSftpFolder;
    @Value("${blob.folder.ces.upload}")
    private String cesBlobFolder;

    // REPCCS config (existing properties)
    @Value("${sftp.folders.repccs.in}")
    private String repccsSftpFolder;
    @Value("${blob.folder.repccs.in}")
    private String repccsBlobFolder;

    /**
     * Resolve workflow configuration from requestId and operation type.
     * Extracts appCode from requestId and determines workflow config.
     *
     * @param requestId The request ID (format: {appCode}_REQ_{timestamp})
     * @param operationType The operation type (e.g., "ENCRYPTION", "DECRYPTION", "SLIFT")
     * @return WorkflowConfig with sftp server, folders, and encrypt type
     */
    public WorkflowConfig resolveFromRequestId(String requestId, String operationType) {
        if (requestId == null || requestId.isEmpty()) {
            throw new IllegalArgumentException("RequestId cannot be null or empty");
        }

        // Extract appCode from requestId (format: OCMSLTA001_REQ_timestamp)
        String appCode = requestId.split("_")[0].toUpperCase();
        boolean isDecryption = "DECRYPTION".equalsIgnoreCase(operationType);
        log.debug("Resolving workflow config for appCode: {}, operationType: {} (from requestId: {})", appCode, operationType, requestId);

        // SLIFT workflows
        if (appCode.contains("LTA")) {
            if (isDecryption) {
                return new WorkflowConfig("lta", ltaDownloadBlobFolder, ltaDownloadSftpFolder, "SLIFT");
            }
            return new WorkflowConfig("lta", ltaBlobFolder, ltaSftpFolder, "SLIFT");
        }
        if (appCode.contains("MHA")) {
            return new WorkflowConfig("mha", mhaBlobFolder, mhaSftpFolder, "SLIFT");
        }
        if (appCode.contains("TOPPAN")) {
            return new WorkflowConfig("toppan", toppanBlobFolder, toppanSftpFolder, "SLIFT");
        }

        // PGP workflows
        if (appCode.contains("CES")) {
            return new WorkflowConfig("ces", cesBlobFolder, cesSftpFolder, "PGP");
        }
        if (appCode.contains("REPCCS") || appCode.contains("REP")) {
            return new WorkflowConfig("repccs", repccsBlobFolder, repccsSftpFolder, "PGP");
        }

        log.warn("Unknown workflow for appCode: {}, defaulting to SLIFT", appCode);
        return new WorkflowConfig("unknown", "", "", "SLIFT");
    }

    /**
     * Workflow configuration data class.
     */
    @Data
    @AllArgsConstructor
    public static class WorkflowConfig {
        private String sftpServer;
        private String blobFolder;
        private String sftpFolder;
        private String encryptType;  // "SLIFT" or "PGP"
    }
}
