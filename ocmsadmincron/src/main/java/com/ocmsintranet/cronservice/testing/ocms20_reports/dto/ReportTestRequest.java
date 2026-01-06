package com.ocmsintranet.cronservice.testing.ocms20_reports.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OCMS 20 report testing
 */
@Data
@NoArgsConstructor
public class ReportTestRequest {

    @JsonProperty("generateReport")
    private Boolean generateReport;     // Actually generate the report

    @JsonProperty("uploadToBlob")
    private Boolean uploadToBlob;       // Upload to Azure Blob Storage

    @JsonProperty("sendEmail")
    private Boolean sendEmail;          // Send email notification (HST only)

    @JsonProperty("details")
    private Boolean details;            // Show detailed verification results

    /**
     * Default all flags to true for full test execution
     */
    public ReportTestRequest(Boolean generateReport, Boolean uploadToBlob, Boolean sendEmail, Boolean details) {
        this.generateReport = generateReport != null ? generateReport : true;
        this.uploadToBlob = uploadToBlob != null ? uploadToBlob : true;
        this.sendEmail = sendEmail != null ? sendEmail : true;
        this.details = details != null ? details : true;
    }

    // Getters return true if null (default behavior)
    public Boolean getGenerateReport() {
        return generateReport != null ? generateReport : true;
    }

    public Boolean getUploadToBlob() {
        return uploadToBlob != null ? uploadToBlob : true;
    }

    public Boolean getSendEmail() {
        return sendEmail != null ? sendEmail : true;
    }

    public Boolean getDetails() {
        return details != null ? details : true;
    }
}
