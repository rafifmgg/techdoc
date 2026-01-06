package com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.controllers;

import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.services.DatahiveContactInfoService;
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
 * Controller for Datahive Contact Info Daily report operations.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/dhcontactinfo/report")
public class DatahiveContactInfoController {

    private final DatahiveContactInfoService datahiveContactInfoService;

    public DatahiveContactInfoController(DatahiveContactInfoService datahiveContactInfoService) {
        this.datahiveContactInfoService = datahiveContactInfoService;
    }

    /**
     * Endpoint to trigger the Datahive Contact Info Daily report generation job.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute Datahive Contact Information Daily report generation job");

        return datahiveContactInfoService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getDetailedMessage());
                response.put("jobName", "DatahiveContactInfoReportJob");
                response.put("reportType", "Excel");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing Datahive Contact Information Daily report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing Datahive Contact Information Daily report generation job: " + e.getMessage());
                response.put("jobName", "DatahiveContactInfoReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    /**
     * Endpoint to generate a Daily report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Response entity with report generation status
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam(value = "reportDate", required = false) String reportDate) {

        Map<String, Object> response = new HashMap<>();

        // If no date is provided, use yesterday's date
        if (reportDate == null || reportDate.isEmpty()) {
            reportDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            log.info("No report date provided, using yesterday's date: {}", reportDate);
        } else {
            // Validate date format
            try {
                LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", reportDate, e);
                response.put("success", false);
                response.put("message", "Invalid date format. Please use yyyy-MM-dd format.");
                response.put("jobName", "DatahiveContactInfoReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }
        }

        log.info("Received request to generate Daily report for date: {}", reportDate);

        ReportResult result = datahiveContactInfoService.generateReport(reportDate);

        response.put("success", result.isSuccess());
        response.put("message", result.getDetailedMessage());
        response.put("jobName", "DatahiveContactInfoReportJob");
        response.put("reportType", "Excel");
        response.put("reportDate", reportDate);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
