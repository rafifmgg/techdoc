package com.ocmsintranet.cronservice.framework.workflows.notificationreport.dto;

import lombok.Data;

/**
 * Result wrapper for notification report generation operations.
 * Contains execution status and processing statistics for detailed response messages.
 */
@Data
public class ReportResult {
    private boolean success;
    private String message;
    private int totalCount;
    private int successCount;
    private int errorCount;
    private String reportDate;

    public ReportResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.totalCount = 0;
        this.successCount = 0;
        this.errorCount = 0;
    }

    public ReportResult(boolean success, String message, int totalCount, int successCount, int errorCount, String reportDate) {
        this.success = success;
        this.message = message;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
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
            return "No notification records found for " + reportDate + " - report generation skipped";
        }

        if (errorCount == 0) {
            return "Processed " + totalCount + " records - all successful, no errors for " + reportDate + " - report generation skipped";
        }

        return "Processed " + totalCount + " records - " + successCount + " success, " + errorCount + " errors for " + reportDate;
    }
}