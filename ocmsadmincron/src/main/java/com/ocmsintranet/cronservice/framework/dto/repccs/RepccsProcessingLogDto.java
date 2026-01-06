package com.ocmsintranet.cronservice.framework.dto.repccs;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for structured batch job log text.
 * Used by REPCCS processing services: ANS Vehicle, Listed Vehicle, NOPO Archival, Offence Rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepccsProcessingLogDto {

    public enum Status {
        SUCCESS, FAILED
    }

    private String prefix;
    private String fileName;
    private Integer records;
    private Status generateFileStatus;
    private Status blobUploadStatus;
    private Status sftpUploadStatus;
    private Status encryptionStatus;
    private Status overallStatus;

    public String toLogText() {
        calculateOverallStatus();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(prefix).append(" file created and uploaded: ");

        if (records == null || records == 0) {
            sb.append("skip no data");
        } else if (overallStatus == Status.SUCCESS) {
            sb.append("success");
        } else {
            sb.append("failed");
        }

        return sb.toString();
    }

    public void calculateOverallStatus() {
        if (blobUploadStatus == Status.FAILED ||
            sftpUploadStatus == Status.FAILED ||
            encryptionStatus == Status.FAILED) {
            this.overallStatus = Status.FAILED;
        } else {
            this.overallStatus = Status.SUCCESS;
        }
    }

    public boolean isFailed() {
        calculateOverallStatus();
        return this.overallStatus == Status.FAILED;
    }
}