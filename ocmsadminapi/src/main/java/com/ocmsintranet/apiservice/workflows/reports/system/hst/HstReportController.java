package com.ocmsintranet.apiservice.workflows.reports.system.hst;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for OCMS 20 HST Report endpoints
 * Provides access to various HST-related reports
 */
@RestController
@RequestMapping("/${api.version}/hst")
@RequiredArgsConstructor
@Slf4j
public class HstReportController {

    private final HstAdHocReportService hstAdHocReportService;

    /**
     * GET /v1/hst/reports/monthly-data
     * Get latest monthly HST data report
     *
     * Response:
     * {
     *   "data": {
     *     "reportDate": "2025-12-01T00:00:00",
     *     "reportUrl": "hst/20251201000000-Monthly-HST-Data-Report.xlsx",
     *     "hstRecordCount": 25
     *   }
     * }
     */
    @GetMapping("/reports/monthly-data")
    public ResponseEntity<?> getMonthlyHstDataReport() {
        try {
            log.info("Retrieving latest monthly HST data report");

            // TODO: Query from ocms_unclaim_report table once it's created by DBA
            // Will query WHERE unc_report_type = 3 (Monthly HST Data)
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now().toString());
            reportData.put("reportUrl", "");
            reportData.put("hstRecordCount", 0);

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error retrieving monthly HST data report", e);
            return createErrorResponse("OCMS-5000", "Error retrieving report: " + e.getMessage());
        }
    }

    /**
     * GET /v1/hst/reports/work-items
     * Get latest monthly HST work items report
     *
     * Response:
     * {
     *   "data": {
     *     "reportDate": "2025-12-01T00:00:00",
     *     "reportUrl": "hst/20251201000000-Monthly-HST-Work-Items-Report.xlsx",
     *     "statisticsCount": 11
     *   }
     * }
     */
    @GetMapping("/reports/work-items")
    public ResponseEntity<?> getWorkItemsReport() {
        try {
            log.info("Retrieving latest monthly HST work items report");

            // TODO: Query from ocms_unclaim_report table once it's created by DBA
            // Will query WHERE unc_report_type = 4 (Monthly HST Work Items)
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now().toString());
            reportData.put("reportUrl", "");
            reportData.put("statisticsCount", 11); // As per spec, 11 statistics

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error retrieving HST work items report", e);
            return createErrorResponse("OCMS-5000", "Error retrieving report: " + e.getMessage());
        }
    }

    /**
     * GET /v1/hst/reports/hirer-driver-furnished
     * Get HST hirer/driver furnished report (ad-hoc)
     *
     * Query Parameters:
     * - startDate: Filter from this date (optional, ISO date format)
     * - endDate: Filter to this date (optional, ISO date format)
     *
     * Response:
     * {
     *   "data": {
     *     "reportUrl": "hst/20251219-HST-Hirer-Driver-Furnished-Report.xlsx",
     *     "recordCount": 5
     *   }
     * }
     */
    @GetMapping("/reports/hirer-driver-furnished")
    public ResponseEntity<?> getHirerDriverFurnishedReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            log.info("Generating HST hirer/driver furnished report. startDate={}, endDate={}",
                    startDate, endDate);

            // OCMS 20 Section 3.6.3: Ad-hoc HST Hirer/Driver Furnished Report
            // Query ocms_furnish_application + ocms_hst for Hirer/Driver records
            Map<String, Object> reportData = hstAdHocReportService
                    .generateHirerDriverFurnishedReport(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating hirer/driver furnished report", e);
            return createErrorResponse("OCMS-5000", "Error generating report: " + e.getMessage());
        }
    }

    /**
     * GET /v1/hst/reports/alternative-address
     * Get alternative address furnished report (ad-hoc)
     *
     * Query Parameters:
     * - startDate: Filter from this date (optional, ISO date format)
     * - endDate: Filter to this date (optional, ISO date format)
     *
     * Response:
     * {
     *   "data": {
     *     "reportUrl": "hst/20251219-Alternative-Address-Furnished-Report.xlsx",
     *     "recordCount": 12
     *   }
     * }
     */
    @GetMapping("/reports/alternative-address")
    public ResponseEntity<?> getAlternativeAddressReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            log.info("Generating alternative address furnished report. startDate={}, endDate={}",
                    startDate, endDate);

            // OCMS 20 Section 3.6.4: Ad-hoc Alternative Address Furnished Report
            // Query ocms_furnish_application + ocms_hst for eService/LTA records
            Map<String, Object> reportData = hstAdHocReportService
                    .generateAlternativeAddressFurnishedReport(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating alternative address report", e);
            return createErrorResponse("OCMS-5000", "Error generating report: " + e.getMessage());
        }
    }

    /**
     * Create error response in OCMS API format
     */
    private ResponseEntity<?> createErrorResponse(String appCode, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("appCode", appCode);
        errorData.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", errorData);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
