package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.services;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.CronJobTemplate.JobResult;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.helpers.LtaReportHelper;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.jobs.LtaReportJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of LTA VRLS Report Service.
 * Handles Excel report generation execution and file management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LtaReportServiceImpl implements LtaReportService {

    private final LtaReportHelper ltaReportHelper;
    private final LtaReportJob ltaReportJob;

    @Value("${lta.report.output.directory:/tmp/lta-reports}")
    private String reportOutputDirectory;

    @Override
    public CompletableFuture<JobResult> executeLtaReportGeneration() {
        log.info("Executing LTA VRLS report generation via service using job framework");

        try {
            // Execute using the job framework for proper batchjob tracking
            return ltaReportJob.execute();
        } catch (Exception e) {
            log.error("Error executing LTA VRLS report generation", e);
            JobResult errorResult = new JobResult(false, "Error executing LTA VRLS report generation: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    @Override
    public Map<String, Object> getReportStatus(String reportType, String reportPeriod) {
        log.info("Getting LTA VRLS report status for type: {}, period: {}", reportType, reportPeriod);

        Map<String, Object> status = new HashMap<>();

        try {
            Path outputDir = Paths.get(reportOutputDirectory);

            if (!Files.exists(outputDir)) {
                status.put("directoryExists", false);
                status.put("availableReports", List.of());
                status.put("totalReports", 0);
                return status;
            }

            // List Excel report files
            List<Map<String, Object>> availableReports = Files.list(outputDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("ReportNoticeLTA_"))
                    .filter(path -> path.getFileName().toString().endsWith(".xlsx"))
                    .map(this::parseExcelReportFileInfo)
                    .filter(info -> info != null)
                    .collect(Collectors.toList());

            status.put("directoryExists", true);
            status.put("availableReports", availableReports);
            status.put("totalReports", availableReports.size());
            status.put("outputDirectory", reportOutputDirectory);
            status.put("lastUpdated", LocalDateTime.now().toString());

        } catch (IOException e) {
            log.error("Error getting LTA VRLS report status: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
        }

        return status;
    }

    @Override
    public String getReportContent(String reportType, String reportPeriod) {
        log.info("Getting LTA VRLS Excel report for type: {}, period: {}", reportType, reportPeriod);

        // For Excel files, we can't return as string content
        // This method could return the file path or base64 encoded content
        log.warn("getReportContent called for Excel report - use downloadExcelReport instead");
        return "Excel report content cannot be returned as string. Use downloadExcelReport endpoint.";
    }

    @Override
    public Map<String, Object> getAvailableReportOptions() {
        log.info("Getting available LTA VRLS report options");

        Map<String, Object> options = new HashMap<>();

        try {
            Path outputDir = Paths.get(reportOutputDirectory);

            options.put("reportTypes", Arrays.asList("excel"));
            options.put("reportPeriods", Arrays.asList("daily"));
            options.put("supportedFormats", Arrays.asList("XLSX"));
            options.put("outputDirectory", reportOutputDirectory);
            options.put("fileNamingPattern", "LTA_REPORT_YYYYMMDD.xlsx");
            options.put("sheets", Arrays.asList("Summary", "Error", "Success", "TS-ROV Revived"));

            if (Files.exists(outputDir)) {
                long fileCount = Files.list(outputDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("ReportNoticeLTA_"))
                        .count();
                options.put("availableReportCount", fileCount);
            } else {
                options.put("availableReportCount", 0);
            }

        } catch (IOException e) {
            log.error("Error getting available LTA VRLS report options: {}", e.getMessage(), e);
            options.put("error", e.getMessage());
        }

        return options;
    }

    /**
     * Get Excel report file as byte array for download.
     *
     * @param reportDate The date of the report
     * @return Excel file as byte array, or null if not found
     */
    public byte[] downloadExcelReport(LocalDate reportDate) {
        try {
            Path outputDir = Paths.get(reportOutputDirectory);
            String fileName = String.format("LTA_REPORT_%s.xlsx",
                    reportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Path filePath = outputDir.resolve(fileName);

            if (!Files.exists(filePath)) {
                log.warn("Excel report file not found: {}", filePath);
                return null;
            }

            log.info("Reading Excel report from: {}", filePath);
            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            log.error("Error reading Excel report file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the latest Excel report file.
     *
     * @return Latest Excel report as byte array, or null if none found
     */
    public byte[] downloadLatestExcelReport() {
        try {
            Path outputDir = Paths.get(reportOutputDirectory);

            if (!Files.exists(outputDir)) {
                return null;
            }

            Path latestFile = Files.list(outputDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("ReportNoticeLTA_"))
                    .filter(path -> path.getFileName().toString().endsWith(".xlsx"))
                    .max((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .orElse(null);

            if (latestFile == null) {
                log.info("No Excel report files found");
                return null;
            }

            log.info("Reading latest Excel report from: {}", latestFile);
            return Files.readAllBytes(latestFile);

        } catch (IOException e) {
            log.error("Error reading latest Excel report: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse Excel report file information.
     *
     * @param filePath Path to the Excel report file
     * @return Map containing file information
     */
    private Map<String, Object> parseExcelReportFileInfo(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            Map<String, Object> info = new HashMap<>();

            info.put("fileName", fileName);
            info.put("filePath", filePath.toString());
            info.put("size", Files.size(filePath));
            info.put("lastModified", Files.getLastModifiedTime(filePath).toString());
            info.put("type", "excel");
            info.put("format", "xlsx");

            // Extract date from filename (ReportNoticeLTA_YYYYMMDD.xlsx)
            if (fileName.matches("ReportNoticeLTA_\\d{8}\\.xlsx")) {
                String datePart = fileName.substring(16, 24); // Extract YYYYMMDD
                info.put("reportDate", datePart);
                info.put("period", "daily");
            }

            return info;

        } catch (IOException e) {
            log.error("Error parsing Excel report file info for {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * Trigger manual Excel report generation for a specific date.
     *
     * @param reportDate The date to generate report for
     * @return Job result
     */
    public CompletableFuture<JobResult> generateExcelReportForDate(LocalDate reportDate) {
        log.info("Manually generating LTA VRLS Excel report for date: {}", reportDate);

        try {
            // Set the specific report date before execution
            ltaReportJob.setReportDate(reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

            // Execute using the job framework for proper batchjob tracking
            return ltaReportJob.execute();
        } catch (Exception e) {
            log.error("Error in manual Excel report generation: {}", e.getMessage(), e);
            JobResult errorResult = new JobResult(false, "Error in manual Excel report generation: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }
}