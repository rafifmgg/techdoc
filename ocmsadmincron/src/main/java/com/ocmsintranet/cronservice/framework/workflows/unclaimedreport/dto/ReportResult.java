package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of Unclaimed Batch Data report generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResult {
    private boolean success;
    private String message;
    private String detailedMessage;
    private String reportUrl;
    private int recordCount;

    public static ReportResult success(String reportUrl, int recordCount) {
        return new ReportResult(
            true,
            "Report generated successfully",
            String.format("Unclaimed Batch Data report generated with %d records. URL: %s", recordCount, reportUrl),
            reportUrl,
            recordCount
        );
    }

    public static ReportResult error(String message) {
        return new ReportResult(
            false,
            message,
            "Failed to generate Unclaimed Batch Data report: " + message,
            null,
            0
        );
    }
}
