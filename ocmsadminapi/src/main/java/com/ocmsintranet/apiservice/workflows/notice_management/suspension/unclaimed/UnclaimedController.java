package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTemp;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTempService;

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
 * Controller for OCMS 20 Unclaimed Reminders functionality
 * Endpoints:
 * 1. POST /v1/check-unclaimed - Validate notice numbers and retrieve details
 * 2. POST /v1/submit-unclaimed - Submit unclaimed reminders, apply TS-UNC, generate report
 */
@RestController
@RequestMapping("/${api.version}")
@RequiredArgsConstructor
@Slf4j
public class UnclaimedController {

    private final UnclaimedService unclaimedService;
    private final UnclaimedReportService reportService;
    private final OcmsNroTempService nroTempService;
    private final ObjectMapper objectMapper;

    /**
     * POST /v1/check-unclaimed
     * Validate notice numbers and retrieve Notice & Reminder Letter details
     *
     * Request Body:
     * {
     *   "noticeNumbers": ["NOTICE1", "NOTICE2"]
     * }
     *
     * Response:
     * {
     *   "data": [
     *     {
     *       "noticeNo": "NOTICE1",
     *       "dateOfLetter": "2025-11-22 00:00:00",
     *       "lastProcessingStage": "RD1",
     *       "idNumber": "S1234567A",
     *       "idType": "N",
     *       "ownerHirerIndicator": "O",
     *       "validationStatus": "OK"
     *     }
     *   ]
     * }
     */
    @PostMapping("/check-unclaimed")
    public ResponseEntity<?> checkUnclaimed(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received check-unclaimed request");

            // Validate request
            if (!request.containsKey("noticeNumbers")) {
                return createErrorResponse("OCMS-4000", "noticeNumbers is required");
            }

            @SuppressWarnings("unchecked")
            List<String> noticeNumbers = (List<String>) request.get("noticeNumbers");

            if (noticeNumbers == null || noticeNumbers.isEmpty()) {
                return createErrorResponse("OCMS-4000", "noticeNumbers cannot be empty");
            }

            log.info("Checking {} notice numbers", noticeNumbers.size());

            // Call service to validate and retrieve notice details
            List<UnclaimedReminderDto> noticeDetails = unclaimedService.checkUnclaimedNotices(noticeNumbers);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("data", noticeDetails);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error checking unclaimed notices", e);
            return createErrorResponse("OCMS-5000", "Error checking unclaimed notices: " + e.getMessage());
        }
    }

    /**
     * POST /v1/submit-unclaimed
     * Process unclaimed reminder submissions, apply TS-UNC suspension, generate report
     *
     * Request Body:
     * [{
     *   "noticeNo": "NOTICE1",
     *   "dateOfLetter": "2025-11-22 00:00:00",
     *   "lastProcessingStage": "RD1",
     *   "idNumber": "S1234567A",
     *   "idType": "N",
     *   "ownerHirerIndicator": "O",
     *   "dateOfReturn": "2025-11-23 00:00:00",
     *   "reasonForUnclaim": "NSP",
     *   "unclaimRemarks": "-",
     *   "reasonOfSuspension": "UNC",
     *   "daysOfRevival": 10,
     *   "suspensionRemarks": "-"
     * }]
     *
     * Response:
     * {
     *   "data": {
     *     "name": "unclaimed/2025112300481928-Unclaimed-Report.xlsx"
     *   }
     * }
     */
    @PostMapping("/submit-unclaimed")
    public ResponseEntity<?> submitUnclaimed(@RequestBody List<UnclaimedReminderDto> unclaimedRecords) {
        try {
            log.info("Received submit-unclaimed request for {} records", unclaimedRecords.size());

            // Validate request
            if (unclaimedRecords == null || unclaimedRecords.isEmpty()) {
                return createErrorResponse("OCMS-4000", "Request body cannot be empty");
            }

            // Validate all records have required fields
            for (UnclaimedReminderDto record : unclaimedRecords) {
                if (record.getNoticeNo() == null || record.getNoticeNo().isEmpty()) {
                    return createErrorResponse("OCMS-4000", "noticeNo is required for all records");
                }
                if (record.getReasonOfSuspension() == null || !record.getReasonOfSuspension().equals("UNC")) {
                    return createErrorResponse("OCMS-4000", "reasonOfSuspension must be 'UNC'");
                }
            }

            // Process each unclaimed record
            List<UnclaimedProcessingResult> processingResults =
                unclaimedService.processUnclaimedReminders(unclaimedRecords);

            // Check for processing errors
            boolean hasErrors = processingResults.stream().anyMatch(r -> !r.isSuccess());
            if (hasErrors) {
                log.warn("Some unclaimed records failed processing");
                // Log errors but continue
                processingResults.stream()
                    .filter(r -> !r.isSuccess())
                    .forEach(r -> log.error("Failed to process notice {}: {}", r.getNoticeNo(), r.getErrorMessage()));

                // TODO: Send email to OIC if there are errors
            }

            // Insert records to ocms_nro_temp for MHA/DataHive query
            for (UnclaimedReminderDto record : unclaimedRecords) {
                try {
                    OcmsNroTemp nroTemp = new OcmsNroTemp();
                    nroTemp.setNoticeNo(record.getNoticeNo());
                    nroTemp.setIdNo(record.getIdNumber());
                    nroTemp.setIdType(record.getIdType());
                    nroTemp.setQueryReason("UNC");
                    nroTemp.setProcessed(false);
                    nroTemp.setCreUserId("SYSTEM"); // TODO: Get from security context

                    nroTempService.save(nroTemp);
                    log.info("Inserted notice {} to ocms_nro_temp for MHA/DataHive query", record.getNoticeNo());
                } catch (Exception e) {
                    log.error("Failed to insert notice {} to ocms_nro_temp", record.getNoticeNo(), e);
                }
            }

            // Generate Unclaimed Reminder Report
            String userId = "SYSTEM"; // TODO: Get from security context
            String reportPath = reportService.generateUnclaimedReminderReport(unclaimedRecords, userId);

            log.info("Unclaimed submission processed successfully. Report: {}", reportPath);

            // Create response
            Map<String, Object> data = new HashMap<>();
            data.put("name", reportPath);

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (UnclaimedReportService.ReportGenerationException e) {
            log.error("Error generating unclaimed report", e);
            return createErrorResponse("OCMS-5001", "Error generating report: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing unclaimed submission", e);
            return createErrorResponse("OCMS-5000", "Error processing unclaimed submission: " + e.getMessage());
        }
    }

    /**
     * GET /v1/unclaimed/reports
     * List all unclaimed reports (both reminder reports and batch data reports)
     *
     * Query Parameters:
     * - startDate: Filter reports from this date (optional, ISO date format)
     * - endDate: Filter reports to this date (optional, ISO date format)
     *
     * Response:
     * {
     *   "data": [
     *     {
     *       "reportDate": "2025-11-23T00:48:19",
     *       "reportType": "REMINDER",
     *       "generatedBy": "SYSTEM",
     *       "reportUrl": "unclaimed/2025112300481928-Unclaimed-Report.xlsx"
     *     }
     *   ],
     *   "totalReports": 1
     * }
     */
    @GetMapping("/unclaimed/reports")
    public ResponseEntity<?> getUnclaimedReports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            log.info("Retrieving unclaimed reports. startDate={}, endDate={}", startDate, endDate);

            // TODO: Query from ocms_unclaim_report table once it's created by DBA
            // For now, return example structure
            List<Map<String, Object>> reportList = new java.util.ArrayList<>();

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportList);
            response.put("totalReports", reportList.size());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error retrieving unclaimed reports", e);
            return createErrorResponse("OCMS-5000", "Error retrieving reports: " + e.getMessage());
        }
    }

    /**
     * GET /v1/unclaimed/reports/batch-data
     * Get latest unclaimed batch data report (with MHA/DataHive results)
     *
     * Response:
     * {
     *   "data": {
     *     "reportDate": "2025-11-23T00:48:19",
     *     "reportUrl": "unclaimed/20251123004819-Unclaimed-Batch-Data-Report.xlsx",
     *     "recordCount": 15
     *   }
     * }
     */
    @GetMapping("/unclaimed/reports/batch-data")
    public ResponseEntity<?> getLatestBatchDataReport() {
        try {
            log.info("Retrieving latest unclaimed batch data report");

            // TODO: Query from ocms_unclaim_report table once it's created by DBA
            // For now, return example structure
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now().toString());
            reportData.put("reportUrl", "");
            reportData.put("recordCount", 0);

            Map<String, Object> response = new HashMap<>();
            response.put("data", reportData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error retrieving latest batch data report", e);
            return createErrorResponse("OCMS-5000", "Error retrieving report: " + e.getMessage());
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
