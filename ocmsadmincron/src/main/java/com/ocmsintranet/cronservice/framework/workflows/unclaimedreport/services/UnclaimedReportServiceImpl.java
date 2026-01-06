package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.services;

import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.helpers.UnclaimedReportHelper;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of UnclaimedReportService
 * Handles Unclaimed Batch Data report generation and upload to Azure Blob Storage
 */
@Slf4j
@Service
public class UnclaimedReportServiceImpl implements UnclaimedReportService {

    private final UnclaimedReportHelper reportHelper;
    private final AzureBlobStorageUtil blobStorageUtil;

    @Value("${blob.folder.unclaimed.report:/offence/reports/unclaimed/}")
    private String blobFolderPath;

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    public UnclaimedReportServiceImpl(
            UnclaimedReportHelper reportHelper,
            AzureBlobStorageUtil blobStorageUtil) {
        this.reportHelper = reportHelper;
        this.blobStorageUtil = blobStorageUtil;
    }

    @Override
    @Async
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Starting Unclaimed Batch Data report generation job");

        try {
            // Generate report for current date
            String reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            ReportResult result = generateReport(reportDate);

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error executing Unclaimed Batch Data report generation job", e);
            return CompletableFuture.completedFuture(
                ReportResult.error("Job execution failed: " + e.getMessage())
            );
        }
    }

    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Generating Unclaimed Batch Data report for date: {}", reportDate);

        try {
            // Step 1: Fetch unclaimed batch data
            log.info("Step 1: Fetching unclaimed batch data");
            Map<String, Object> reportData = reportHelper.fetchUnclaimedBatchData();

            if (reportData.isEmpty()) {
                log.warn("No unclaimed data found for report date: {}", reportDate);
                return ReportResult.error("No unclaimed data found");
            }

            int recordCount = (int) reportData.getOrDefault("totalCount", 0);
            log.info("Found {} unclaimed records", recordCount);

            // Step 2: Generate Excel report
            log.info("Step 2: Generating Excel report");
            byte[] excelBytes = reportHelper.generateExcelReport(reportData, reportDate);
            log.info("Excel report generated: {} bytes", excelBytes.length);

            // Step 3: Upload to Azure Blob Storage
            log.info("Step 3: Uploading report to Azure Blob Storage");
            String fileName = generateFileName(reportDate);
            String fullBlobPath = blobFolderPath + fileName;

            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                    blobStorageUtil.uploadBytesToBlob(excelBytes, fullBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload report to blob storage: {}", uploadResponse.getErrorMessage());
                return ReportResult.error("Failed to upload report: " + uploadResponse.getErrorMessage());
            }

            String reportUrl = uploadResponse.getFileUrl();
            log.info("Report uploaded successfully to: {}", reportUrl);

            // Return success result
            return ReportResult.success(reportUrl, recordCount);

        } catch (Exception e) {
            log.error("Error generating Unclaimed Batch Data report", e);
            return ReportResult.error(e.getMessage());
        }
    }

    @Override
    public ReportResult checkAndGenerateBatchDataReport() {
        log.info("Checking for new MHA/DataHive UNC results for Batch Data Report");

        try {
            // Check if there are unprocessed UNC results in ocms_temp_unc_hst_addr
            boolean hasNewResults = reportHelper.hasUnprocessedUncResults();

            if (!hasNewResults) {
                log.info("No new MHA/DataHive UNC results available for Batch Data Report");
                return null;
            }

            log.info("Found new MHA/DataHive UNC results. Generating Batch Data Report...");
            String reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Fetch UNC data from temp table
            Map<String, Object> reportData = reportHelper.fetchUncBatchDataFromTemp();

            if (reportData.isEmpty()) {
                log.warn("No UNC data found in temp table");
                return ReportResult.error("No UNC data available");
            }

            int recordCount = (int) reportData.getOrDefault("totalCount", 0);
            log.info("Found {} UNC records from MHA/DataHive", recordCount);

            // Generate Excel report
            byte[] excelBytes = reportHelper.generateUncBatchDataReport(reportData, reportDate);
            log.info("Unclaimed Batch Data Report generated: {} bytes", excelBytes.length);

            // Upload to Azure Blob Storage
            String fileName = String.format("UnclaimedBatchDataReport_%s.xlsx",
                LocalDate.parse(reportDate).format(FILENAME_FORMATTER));
            String fullBlobPath = blobFolderPath + fileName;

            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                blobStorageUtil.uploadBytesToBlob(excelBytes, fullBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload Batch Data Report: {}", uploadResponse.getErrorMessage());
                return ReportResult.error("Failed to upload report: " + uploadResponse.getErrorMessage());
            }

            log.info("Unclaimed Batch Data Report uploaded to: {}", uploadResponse.getFileUrl());

            // Mark UNC results as processed
            reportHelper.markUncResultsAsProcessed();

            return ReportResult.success(uploadResponse.getFileUrl(), recordCount);

        } catch (Exception e) {
            log.error("Error generating Unclaimed Batch Data Report", e);
            return ReportResult.error("Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Generate filename for the report
     *
     * @param reportDate Report date in yyyy-MM-dd format
     * @return Filename in format: UnclaimedBatchData_yyyyMMdd.xlsx
     */
    private String generateFileName(String reportDate) {
        try {
            LocalDate date = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            String dateFormatted = date.format(FILENAME_FORMATTER);
            return String.format("UnclaimedBatchData_%s.xlsx", dateFormatted);
        } catch (Exception e) {
            log.warn("Failed to parse report date, using current timestamp", e);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            return String.format("UnclaimedBatchData_%s.xlsx", timestamp);
        }
    }
}
