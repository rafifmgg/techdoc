package com.ocmsintranet.cronservice.framework.workflows.paymentReport.controllers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.ExceptionReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.PaymentExceptionReportService;
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
 * Controller for Payment Exception Report operations.
 * Provides REST API endpoints to trigger report generation manually.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/payment/exception-report")
public class PaymentExceptionReportController {

    private final PaymentExceptionReportService reportService;

    public PaymentExceptionReportController(PaymentExceptionReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpoint to trigger the Payment Exception Report generation job.
     * Uses yesterday's date as the report date.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute Payment Exception Report generation job");

        return reportService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getDetailedMessage());
                response.put("jobName", "PaymentExceptionReportJob");
                response.put("totalCount", result.getTotalCount());
                response.put("reportDate", result.getReportDate());
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing Payment Exception Report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing Payment Exception Report generation job: " + e.getMessage());
                response.put("jobName", "PaymentExceptionReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to generate a Payment Exception Report for a specific date.
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
                response.put("jobName", "PaymentExceptionReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }
        }

        log.info("Received request to generate Payment Exception Report for date: {}", reportDate);

        ExceptionReportResult result = reportService.generateReport(reportDate);

        response.put("success", result.isSuccess());
        response.put("message", result.getDetailedMessage());
        response.put("jobName", "PaymentExceptionReportJob");
        response.put("totalCount", result.getTotalCount());
        response.put("reportDate", reportDate);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
