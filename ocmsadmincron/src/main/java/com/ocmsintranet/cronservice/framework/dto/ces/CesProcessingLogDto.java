package com.ocmsintranet.cronservice.framework.dto.ces;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for structured batch job log text.
 * Used by CES processing services: ANS Vehicle, Offence Rule, Wanted Vehicle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesProcessingLogDto {

    public enum Status {
        SUCCESS, FAILED
    }

    private String prefix;            // e.g., "ANS VEHICLE PROCESSING"
    private String fileName;
    private Integer records;
    private Status generateFileStatus;
    private Status blobUploadStatus;
    private Status sftpNonEncryptedStatus;
    private Status sftpEncryptedStatus;
    private Status overallStatus;

    /**
     * Generates formatted log text for batch job.
     * Format: simple single line per process.
     */
    public String toLogText() {
        // Calculate overall status before generating log
        calculateOverallStatus();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(prefix).append(" file created and uploaded: ");

        // Check if no data
        if (records == null || records == 0) {
            sb.append("skip no data");
        } else if (overallStatus == Status.SUCCESS) {
            sb.append("success");
        } else {
            sb.append("failed");
        }

        return sb.toString();
    }

    private String getStatusText(Status status) {
        return status != null ? status.name() : "-";
    }

    /**
     * Calculate overall status based on individual statuses.
     * If any of blobUploadStatus, sftpNonEncryptedStatus, or sftpEncryptedStatus is FAILED,
     * then overallStatus is FAILED.
     */
    public void calculateOverallStatus() {
        if (blobUploadStatus == Status.FAILED ||
            sftpNonEncryptedStatus == Status.FAILED ||
            sftpEncryptedStatus == Status.FAILED) {
            this.overallStatus = Status.FAILED;
        } else {
            this.overallStatus = Status.SUCCESS;
        }
    }

    /**
     * Check if overall status is FAILED
     */
    public boolean isFailed() {
        calculateOverallStatus();
        return this.overallStatus == Status.FAILED;
    }
}