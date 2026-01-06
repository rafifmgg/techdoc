package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.controllers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.services.LtaReportService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.services.LtaReportServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for LTA VRLS Vehicle Ownership Check Report operations.
 * Provides endpoints for Excel report generation, download, and management.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/lta/report")
@RequiredArgsConstructor
public class LtaReportController {

    private final LtaReportService ltaReportService;
    private final LtaReportServiceImpl ltaReportServiceImpl; // For Excel-specific methods

    /**
     * Endpoint to trigger the LTA VRLS report generation job for yesterday.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute LTA VRLS report generation job");

        return ltaReportService.executeLtaReportGeneration()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getMessage());
                response.put("jobName", "LtaReportJob");
                response.put("reportType", "Excel");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing LTA VRLS report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing LTA VRLS report generation job: " + e.getMessage());
                response.put("jobName", "LtaReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to generate a LTA VRLS report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Response entity with report generation status
     */
    @PostMapping("/generate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateReport(
            @RequestParam(value = "reportDate", required = false) String reportDate) {

        Map<String, Object> response = new HashMap<>();

        // If no date is provided, use yesterday's date
        LocalDate targetDate;
        if (reportDate == null || reportDate.isEmpty()) {
            targetDate = LocalDate.now().minusDays(1);
            reportDate = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("No report date provided, using yesterday's date: {}", reportDate);
        } else {
            // Validate date format
            try {
                targetDate = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", reportDate, e);
                response.put("success", false);
                response.put("message", "Invalid date format. Please use yyyy-MM-dd format.");
                response.put("jobName", "LtaReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
            }
        }

        log.info("Received request to generate LTA VRLS report for date: {}", reportDate);

        final String finalReportDate = reportDate;
        return ltaReportServiceImpl.generateExcelReportForDate(targetDate)
            .thenApply(result -> {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", result.isSuccess());
                resp.put("message", result.getMessage());
                resp.put("jobName", "LtaReportJob");
                resp.put("reportType", "Excel");
                resp.put("reportDate", finalReportDate);
                resp.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(resp);
            })
            .exceptionally(e -> {
                log.error("Error generating LTA VRLS report for {}: {}", finalReportDate, e.getMessage(), e);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("message", "Error generating LTA VRLS report: " + e.getMessage());
                resp.put("jobName", "LtaReportJob");
                resp.put("reportDate", finalReportDate);
                resp.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(resp);
            });
    }

}