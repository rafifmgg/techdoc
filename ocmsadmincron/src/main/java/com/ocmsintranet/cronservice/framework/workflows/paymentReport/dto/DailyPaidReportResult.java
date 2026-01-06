package com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto;

import lombok.Data;

/**
 * Result wrapper for Daily Paid Report generation operations.
 * Contains execution status and processing statistics for detailed response messages.
 */
@Data
public class DailyPaidReportResult {
    private boolean success;
    private String message;
    private int totalCount;
    private int eServiceCount;
    private int axsCount;
    private int offlineCount;
    private int jtcCollectionCount;
    private int refundCount;
    private String reportDate;

    public DailyPaidReportResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.totalCount = 0;
        this.eServiceCount = 0;
        this.axsCount = 0;
        this.offlineCount = 0;
        this.jtcCollectionCount = 0;
        this.refundCount = 0;
    }

    public DailyPaidReportResult(boolean success, String message, int totalCount,
                                  int eServiceCount, int axsCount, int offlineCount,
                                  int jtcCollectionCount, int refundCount, String reportDate) {
        this.success = success;
        this.message = message;
        this.totalCount = totalCount;
        this.eServiceCount = eServiceCount;
        this.axsCount = axsCount;
        this.offlineCount = offlineCount;
        this.jtcCollectionCount = jtcCollectionCount;
        this.refundCount = refundCount;
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
            return "No paid records found for " + reportDate + " - report generation skipped";
        }

        return String.format("Processed %d paid records for %s - eService: %d, AXS: %d, Offline: %d, JTC: %d, Refund: %d",
                totalCount, reportDate, eServiceCount, axsCount, offlineCount, jtcCollectionCount, refundCount);
    }
}
