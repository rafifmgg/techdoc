package com.ocmsintranet.cronservice.framework.dto.ces;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for structured batch job log text.
 * Used by CesDownloadAttachmentService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesDownloadAttachmentLogDto {

    public enum Status {
        SUCCESS, FAILED
    }

    private Integer zipFileProcess;
    private Integer imageCountProcess;
    private Integer imageUploaded;
    private Integer imageNotMatchValidation;
    private Status overallStatus;

    /**
     * Generates formatted log text for batch job.
     */
    public String toLogText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Zip File Process: ").append(zipFileProcess != null ? zipFileProcess : 0);
        sb.append(", Image Count Process: ").append(imageCountProcess != null ? imageCountProcess : 0);
        sb.append(", Image Uploaded: ").append(imageUploaded != null ? imageUploaded : 0);
        sb.append(", Image Not Match Validation: ").append(imageNotMatchValidation != null ? imageNotMatchValidation : 0);
        sb.append(", Status: ").append(overallStatus != null ? overallStatus.name() : "-");
        return sb.toString();
    }

    /**
     * Calculate imageCountProcess from imageUploaded and imageNotMatchValidation
     */
    public void calculateImageCountProcess() {
        int uploaded = imageUploaded != null ? imageUploaded : 0;
        int failed = imageNotMatchValidation != null ? imageNotMatchValidation : 0;
        this.imageCountProcess = uploaded + failed;
    }
}