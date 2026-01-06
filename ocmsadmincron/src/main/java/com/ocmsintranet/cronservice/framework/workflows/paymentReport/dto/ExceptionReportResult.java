package com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto;

import lombok.Data;

/**
 * Result wrapper for Payment Exception Report generation operations.
 * Contains execution status and processing statistics for detailed response messages.
 */
@Data
public class ExceptionReportResult {
    private boolean success;
    private String message;
    private int totalCount;
    private String reportDate;

    public ExceptionReportResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.totalCount = 0;
    }

    public ExceptionReportResult(boolean success, String message, int totalCount, String reportDate) {
        this.success = success;
        this.message = message;
        this.totalCount = totalCount;
        this.reportDate = reportDate;
    }

    /**
     * Generate a detailed message with processing statistics
     */
    public String getDetailedMessage() {
        if (!success) {
            return message;
        }

        if (totalCount == 0) {
            return "No exception records found for " + reportDate + " - report sent with empty results";
        }

        return "Processed " + totalCount + " exception records for " + reportDate + " - report sent successfully";
    }
}
