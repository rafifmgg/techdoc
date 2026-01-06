package com.ocmsintranet.cronservice.framework.workflows.paymentReport.controllers;

import com.ocmsintranet.cronservice.framework.workflows.paymentReport.dto.MonthlyPaidReportResult;
import com.ocmsintranet.cronservice.framework.workflows.paymentReport.services.MonthlyPaidReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Monthly Paid Report operations.
 * Provides REST API endpoints to trigger report generation manually.
 */
@Slf4j
@RestController
@RequestMapping("/${api.version}/payment/monthly-paid-report")
public class MonthlyPaidReportController {

    private final MonthlyPaidReportService reportService;

    public MonthlyPaidReportController(MonthlyPaidReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpoint to trigger the Monthly Paid Report generation job.
     * Uses previous month as the report month.
     *
     * @return Response entity with job execution status
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeJob() {
        log.info("Received request to execute Monthly Paid Report generation job");

        return reportService.executeJob()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", result.isSuccess());
                response.put("message", result.getDetailedMessage());
                response.put("jobName", "MonthlyPaidReportJob");
                response.put("totalCount", result.getTotalCount());
                response.put("eServiceCount", result.getEServiceCount());
                response.put("axsCount", result.getAxsCount());
                response.put("offlineCount", result.getOfflineCount());
                response.put("jtcCollectionCount", result.getJtcCollectionCount());
                response.put("refundCount", result.getRefundCount());
                response.put("reportMonth", result.getReportMonth());
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Error executing Monthly Paid Report generation job: {}", e.getMessage(), e);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error executing Monthly Paid Report generation job: " + e.getMessage());
                response.put("jobName", "MonthlyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.internalServerError().body(response);
            });
    }

    /**
     * Endpoint to generate a Monthly Paid Report for a specific month.
     *
     * @param yearMonth The month for which to generate the report (format: yyyy-MM)
     * @return Response entity with report generation status
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {

        Map<String, Object> response = new HashMap<>();

        // If no month is provided, use previous month
        if (yearMonth == null || yearMonth.isEmpty()) {
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            yearMonth = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            log.info("No report month provided, using previous month: {}", yearMonth);
        } else {
            // Validate month format
            try {
                YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException e) {
                log.error("Invalid month format: {}", yearMonth, e);
                response.put("success", false);
                response.put("message", "Invalid month format. Please use yyyy-MM format (e.g., 2024-01).");
                response.put("jobName", "MonthlyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }

            // Validate that the month is not in the future
            YearMonth requestedMonth = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            if (requestedMonth.isAfter(YearMonth.now())) {
                log.error("Future month requested: {}", yearMonth);
                response.put("success", false);
                response.put("message", "Cannot generate report for future months. Please provide a past or current month.");
                response.put("jobName", "MonthlyPaidReportJob");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(response);
            }
        }

        log.info("Received request to generate Monthly Paid Report for month: {}", yearMonth);

        MonthlyPaidReportResult result = reportService.generateReport(yearMonth);

        response.put("success", result.isSuccess());
        response.put("message", result.getDetailedMessage());
        response.put("jobName", "MonthlyPaidReportJob");
        response.put("totalCount", result.getTotalCount());
        response.put("eServiceCount", result.getEServiceCount());
        response.put("axsCount", result.getAxsCount());
        response.put("offlineCount", result.getOfflineCount());
        response.put("jtcCollectionCount", result.getJtcCollectionCount());
        response.put("refundCount", result.getRefundCount());
        response.put("reportMonth", yearMonth);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
