package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.dto;

import lombok.Data;
import java.util.Map;

/**
 * Result wrapper for Toppan report file retrieval operations.
 * Contains execution status and download URLs for 5 report files.
 *
 * File types:
 * - PDF_D2: DPT-URA-PDF-D2-YYYYMMDDHHMISS
 * - LOG_PDF: DPT-URA-LOG-PDF-YYYYMMDDHHMISS
 * - LOG_D2: DPT-URA-LOG-D2-YYYYMMDDHHMISS
 * - RD2_D2: DPT-URA-RD2-D2-YYYYMMDD
 * - DN2_D2: DPT-URA-DN2-D2-YYYYMMDD
 */
@Data
public class ReportResult {
    private boolean success;
    private String message;
    private String reportDate;
    private Map<String, String> downloadUrls;  // Map of file type to download URL
    private Map<String, String> fileNames;     // Map of file type to file name
    private int filesFound;                     // Number of files found (0-5)

    public ReportResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.filesFound = 0;
    }

    public ReportResult(boolean success, String message, String reportDate, Map<String, String> downloadUrls, Map<String, String> fileNames) {
        this.success = success;
        this.message = message;
        this.reportDate = reportDate;
        this.downloadUrls = downloadUrls;
        this.fileNames = fileNames;
        this.filesFound = downloadUrls != null ? downloadUrls.size() : 0;
    }

    /**
     * Generate a detailed message with download information
     */
    public String getDetailedMessage() {
        if (!success) {
            return message;
        }

        if (downloadUrls == null || downloadUrls.isEmpty()) {
            return "Toppan report files not found for " + reportDate + " - no downloads available";
        }

        if (filesFound < 5) {
            return String.format("Toppan report: only %d out of 5 files found for %s", filesFound, reportDate);
        }

        return String.format("Toppan report: all 5 files ready for download for %s", reportDate);
    }
}