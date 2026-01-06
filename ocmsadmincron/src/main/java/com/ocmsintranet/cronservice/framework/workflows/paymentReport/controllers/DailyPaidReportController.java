package com.ocmsintranet.cronservice.framework.workflows.paymentReport.controllers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.DailyPaidReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.DailyPaidReportService;
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
 * Controller for Daily Paid Report operations.
 * Provides REST API endpoints to trigger report generation manually.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/payment/daily-paid-report")
public class DailyPaidReportController {

    private final DailyPaidReportService reportService;

    public DailyPaidReportController(DailyPaidReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpoint to trigger the Daily Paid Report generation job.
     * Uses yesterday as the report date.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute Daily Paid Report generation job");

        return reportService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getDetailedMessage());
                response.put("jobName", "DailyPaidReportJob");
                response.put("totalCount", result.getTotalCount());
                response.put("eServiceCount", result.getEServiceCount());
                response.put("axsCount", result.getAxsCount());
                response.put("offlineCount", result.getOfflineCount());
                response.put("jtcCollectionCount", result.getJtcCollectionCount());
                response.put("refundCount", result.getRefundCount());
                response.put("reportDate", result.getReportDate());
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing Daily Paid Report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing Daily Paid Report generation job: " + e.getMessage());
                response.put("jobName", "DailyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to generate a Daily Paid Report for a specific date.
     *
     * @param reportDate The date for which to generate the report (format: yyyy-MM-dd)
     * @return Response entity with report generation status
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam(value = "reportDate", required = false) String reportDate) {

        Map<String, Object> response = new HashMap<>();

        // If no date is provided, use yesterday
        if (reportDate == null || reportDate.isEmpty()) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            reportDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("No report date provided, using yesterday: {}", reportDate);
        } else {
            // Validate date format
            try {
                LocalDate.parse(reportDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", reportDate, e);
                response.put("success", false);
                response.put("message", "Invalid date format. Please use yyyy-MM-dd format (e.g., 2024-01-15).");
                response.put("jobName", "DailyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }

            // Validate that the date is not in the future
            LocalDate requestedDate = LocalDate.parse(reportDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (requestedDate.isAfter(LocalDate.now())) {
                log.error("Future date requested: {}", reportDate);
                response.put("success", false);
                response.put("message", "Cannot generate report for future dates. Please provide a past or current date.");
                response.put("jobName", "DailyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }
        }

        log.info("Received request to generate Daily Paid Report for date: {}", reportDate);

        DailyPaidReportResult result = reportService.generateReport(reportDate);

        response.put("success", result.isSuccess());
        response.put("message", result.getDetailedMessage());
        response.put("jobName", "DailyPaidReportJob");
        response.put("totalCount", result.getTotalCount());
        response.put("eServiceCount", result.getEServiceCount());
        response.put("axsCount", result.getAxsCount());
        response.put("offlineCount", result.getOfflineCount());
        response.put("jtcCollectionCount", result.getJtcCollectionCount());
        response.put("refundCount", result.getRefundCount());
        response.put("reportDate", reportDate);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
