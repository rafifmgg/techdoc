package com.ocmsintranet.cronservice.framework.workflows.hstreport.controllers;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.services.HstReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Monthly HST report operations.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/hst/report")
public class HstReportController {

    private final HstReportService hstReportService;

    public HstReportController(HstReportService hstReportService) {
        this.hstReportService = hstReportService;
    }

    /**
     * Endpoint to trigger the Monthly HST report generation job.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute Monthly HST report generation job");

        return hstReportService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getDetailedMessage());
                response.put("jobName", "MonthlyHSTReportJob");
                response.put("reportType", "Excel");
                response.put("reportUrl", result.getReportUrl());
                response.put("recordCount", result.getRecordCount());
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing Monthly HST report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing Monthly HST report generation job: " + e.getMessage());
                response.put("jobName", "MonthlyHSTReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to generate a Monthly HST report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Response entity with report generation status
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam(value = "reportDate", required = false) String reportDate) {

        Map<String, Object> response = new HashMap<>();

        // If no date is provided, use current date
        if (reportDate == null || reportDate.isEmpty()) {
            reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("No report date provided, using current date: {}", reportDate);
        } else {
            // Validate date format
            try {
                LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", reportDate, e);
                response.put("success", false);
                response.put("message", "Invalid date format. Please use yyyy-MM-dd format.");
                response.put("jobName", "MonthlyHSTReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }
        }

        log.info("Received request to generate Monthly HST report for date: {}", reportDate);

        ReportResult result = hstReportService.generateReport(reportDate);

        response.put("success", result.isSuccess());
        response.put("message", result.getDetailedMessage());
        response.put("jobName", "MonthlyHSTReportJob");
        response.put("reportType", "Excel");
        response.put("reportDate", reportDate);
        response.put("reportUrl", result.getReportUrl());
        response.put("recordCount", result.getRecordCount());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
