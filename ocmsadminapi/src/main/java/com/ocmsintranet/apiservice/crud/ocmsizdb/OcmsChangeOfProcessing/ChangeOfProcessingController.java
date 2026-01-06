package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ChangeOfProcessingRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ChangeOfProcessingResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.PlusChangeStageRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.SearchChangeProcessingStageRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.SearchChangeProcessingStageResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ValidateChangeProcessingStageRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ValidateChangeProcessingStageResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap.StageMap;
import com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap.StageMapRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap.dto.StageMapResponse;
import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for Change Processing Stage operations
 * Based on OCMS CPS Spec §5
 */
@RestController
@RequestMapping("/${api.version}")
@RequiredArgsConstructor
@Slf4j
public class ChangeOfProcessingController {

    private final ChangeOfProcessingService changeProcessingStageService;
    private final StageMapRepository stageMapRepository;
    private final AzureBlobStorageUtil azureBlobStorageUtil;

    /**
     * Submit batch change processing stage request
     * Based on OCMS CPS Spec §5.1, §5.2
     *
     * Endpoint: POST /v1/change-processing-stage
     *
     * Request body:
     * {
     *   "items": [
     *     {
     *       "noticeNo": "N-001",
     *       "newStage": "DN2",  // optional, can be derived
     *       "reason": "Manual adjustment",
     *       "remark": "Verified by AO",
     *       "dhMhaCheck": false
     *     }
     *   ]
     * }
     *
     * Response:
     * {
     *   "status": "PARTIAL",
     *   "summary": { "requested": 5, "succeeded": 3, "failed": 2 },
     *   "results": [
     *     { "noticeNo": "N-001", "outcome": "UPDATED", "previousStage": "DN1", "newStage": "DN2" },
     *     { "noticeNo": "N-002", "outcome": "FAILED", "code": "OCMS.CPS.NOT_FOUND", "message": "VON not found" }
     *   ],
     *   "report": { "url": "https://signed-url.xlsx", "expiresAt": "2025-10-28T16:00:00+08:00" }
     * }
     *
     * @param request Batch change request
     * @param httpRequest HTTP servlet request
     * @return Batch response with per-notice results
     */
    @PostMapping("/change-processing-stage")
    public ResponseEntity<ChangeOfProcessingResponse> changeProcessingStage(
            @Valid @RequestBody ChangeOfProcessingRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received change processing stage request with {} items", request.getItems().size());

        try {
            // Get user ID from request (you may need to adjust this based on your auth mechanism)
            String userId = extractUserId(httpRequest);

            // Validate request format (§2 step 2)
            if (request.getItems() == null || request.getItems().isEmpty()) {
                log.warn("Empty items list in request");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("OCMS.CPS.INVALID_FORMAT", "Items list cannot be empty"));
            }

            // Validate mandatory data (§2 step 3)
            for (ChangeOfProcessingRequest.ChangeOfProcessingItem item : request.getItems()) {
                if (item.getNoticeNo() == null || item.getNoticeNo().trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(createErrorResponse("OCMS.CPS.MISSING_DATA", "noticeNo is required"));
                }
            }

            // Process the batch (§2 steps 4-8)
            ChangeOfProcessingResponse response = changeProcessingStageService.processBatch(request, userId);

            log.info("Completed change processing stage request: status={}, succeeded={}, failed={}",
                    response.getStatus(), response.getSummary().getSucceeded(), response.getSummary().getFailed());

            // Return 200 OK with status (§5.3)
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing change processing stage request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS.CPS.UNEXPECTED", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Extract user ID from HTTP request
     * Adjust this based on your authentication mechanism
     */
    private String extractUserId(HttpServletRequest request) {
        // Try to get from header
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Try to get from authenticated principal (if using Spring Security)
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null && auth.getPrincipal() != null) {
        //     return auth.getName();
        // }

        // Default to system user
        return "SYSTEM";
    }

    /**
     * Create error response for validation failures
     */
    private ChangeOfProcessingResponse createErrorResponse(String code, String message) {
        return ChangeOfProcessingResponse.builder()
            .status("FAILED")
            .summary(ChangeOfProcessingResponse.BatchSummary.builder()
                .requested(0)
                .succeeded(0)
                .failed(0)
                .build())
            .results(java.util.Collections.singletonList(
                ChangeOfProcessingResponse.NoticeResult.builder()
                    .noticeNo("")
                    .outcome("FAILED")
                    .code(code)
                    .message(message)
                    .build()
            ))
            .build();
    }

    /**
     * Validate notices for change processing stage eligibility
     * Based on OCMS CPS Spec §2.4.2
     *
     * Endpoint: POST /v1/change-processing-stage/validate
     *
     * This endpoint validates notices BEFORE actual submission to identify
     * which notices are eligible vs ineligible for the requested stage change.
     *
     * Request body:
     * {
     *   "notices": [
     *     {
     *       "noticeNo": "N-001",
     *       "currentStage": "DN1",
     *       "offenderType": "DRIVER",
     *       "entityType": null
     *     }
     *   ],
     *   "newProcessingStage": "DN2",
     *   "reasonOfChange": "SUP",
     *   "remarks": "Speed up processing"
     * }
     *
     * Response:
     * {
     *   "changeableNotices": [
     *     { "noticeNo": "N-001", "currentStage": "DN1", "offenderType": "DRIVER", "message": "Eligible for stage change" }
     *   ],
     *   "nonChangeableNotices": [
     *     { "noticeNo": "N-002", "code": "OCMS.CPS.COURT_STAGE", "message": "Notice is in court stage" }
     *   ],
     *   "summary": { "total": 2, "changeable": 1, "nonChangeable": 1 }
     * }
     *
     * @param request Validation request
     * @return Validation response with changeable/non-changeable lists
     */
    @PostMapping("/change-processing-stage/validate")
    public ResponseEntity<ValidateChangeProcessingStageResponse> validateChangeProcessingStage(
            @Valid @RequestBody ValidateChangeProcessingStageRequest request) {

        log.info("Received validation request for {} notices, newStage={}",
                request.getNotices().size(), request.getNewProcessingStage());

        try {
            // Validate request
            if (request.getNotices() == null || request.getNotices().isEmpty()) {
                log.warn("Empty notices list in validation request");
                return ResponseEntity.badRequest().body(
                    ValidateChangeProcessingStageResponse.builder()
                        .changeableNotices(java.util.Collections.emptyList())
                        .nonChangeableNotices(java.util.Collections.emptyList())
                        .summary(ValidateChangeProcessingStageResponse.ValidationSummary.builder()
                            .total(0)
                            .changeable(0)
                            .nonChangeable(0)
                            .build())
                        .build()
                );
            }

            // Validate remarks requirement if reason = OTH
            if ("OTH".equals(request.getReasonOfChange())) {
                if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                    log.warn("Remarks required when reasonOfChange=OTH");
                    // Return all notices as non-changeable with REMARKS_REQUIRED error
                    java.util.List<ValidateChangeProcessingStageResponse.NonChangeableNotice> allNonChangeable =
                        new java.util.ArrayList<>();
                    for (ValidateChangeProcessingStageRequest.NoticeToValidate notice : request.getNotices()) {
                        allNonChangeable.add(ValidateChangeProcessingStageResponse.NonChangeableNotice.builder()
                            .noticeNo(notice.getNoticeNo())
                            .code("OCMS.CPS.REMARKS_REQUIRED")
                            .message("Remarks are mandatory when reason for change is 'OTH' (Others)")
                            .build());
                    }
                    return ResponseEntity.ok(ValidateChangeProcessingStageResponse.builder()
                        .changeableNotices(java.util.Collections.emptyList())
                        .nonChangeableNotices(allNonChangeable)
                        .summary(ValidateChangeProcessingStageResponse.ValidationSummary.builder()
                            .total(request.getNotices().size())
                            .changeable(0)
                            .nonChangeable(request.getNotices().size())
                            .build())
                        .build());
                }
            }

            // Process validation using service
            ValidateChangeProcessingStageResponse response =
                changeProcessingStageService.validateNotices(request);

            log.info("Validation completed: total={}, changeable={}, nonChangeable={}",
                    response.getSummary().getTotal(),
                    response.getSummary().getChangeable(),
                    response.getSummary().getNonChangeable());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing validation request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ValidateChangeProcessingStageResponse.builder()
                    .changeableNotices(java.util.Collections.emptyList())
                    .nonChangeableNotices(java.util.Collections.emptyList())
                    .summary(ValidateChangeProcessingStageResponse.ValidationSummary.builder()
                        .total(0)
                        .changeable(0)
                        .nonChangeable(0)
                        .build())
                    .build()
            );
        }
    }

    /**
     * Search notices for change processing stage
     * Based on OCMS CPS Spec §2.3.1
     *
     * Endpoint: POST /v1/change-processing-stage/search
     *
     * This endpoint searches for notices based on search criteria and returns
     * segregated lists of eligible vs ineligible notices.
     *
     * Request body:
     * {
     *   "noticeNo": "N-001",           // Optional
     *   "idNo": "S1234567A",           // Optional
     *   "vehicleNo": "SBA1234A",       // Optional
     *   "currentProcessingStage": "DN1", // Optional
     *   "dateOfCurrentProcessingStage": "2025-12-20" // Optional
     * }
     *
     * At least one search criterion is required.
     *
     * Response:
     * {
     *   "eligibleNotices": [
     *     {
     *       "noticeNo": "N-001",
     *       "offenceType": "SP",
     *       "offenceDateTime": "2025-12-01T10:30:00",
     *       "offenderName": "John Doe",
     *       "offenderId": "S1234567A",
     *       "vehicleNo": "SBA1234A",
     *       "currentProcessingStage": "DN1",
     *       "currentProcessingStageDate": "2025-12-15T09:00:00",
     *       "suspensionType": null,
     *       "suspensionStatus": null,
     *       "ownerDriverIndicator": "D",
     *       "entityType": null
     *     }
     *   ],
     *   "ineligibleNotices": [
     *     {
     *       "noticeNo": "N-002",
     *       "offenceType": "SP",
     *       "offenceDateTime": "2025-12-01T11:00:00",
     *       "offenderName": "Jane Smith",
     *       "offenderId": "S9876543B",
     *       "vehicleNo": "SBA5678B",
     *       "currentProcessingStage": "CRT",
     *       "currentProcessingStageDate": "2025-12-18T14:00:00",
     *       "reasonCode": "OCMS.CPS.SEARCH.COURT_STAGE",
     *       "reasonMessage": "Notice is in court stage"
     *     }
     *   ],
     *   "summary": {
     *     "total": 2,
     *     "eligible": 1,
     *     "ineligible": 1
     *   }
     * }
     *
     * @param request Search request with criteria
     * @return Search response with segregated lists
     */
    @PostMapping("/change-processing-stage/search")
    public ResponseEntity<SearchChangeProcessingStageResponse> searchNotices(
            @Valid @RequestBody SearchChangeProcessingStageRequest request) {

        log.info("Received search request: noticeNo={}, idNo={}, vehicleNo={}, stage={}, date={}",
                request.getNoticeNo(), request.getIdNo(), request.getVehicleNo(),
                request.getLastProcessingStage(), request.getDateOfCurrentProcessingStage());

        try {
            // Validate at least one criterion is provided
            if (!request.hasValidCriteria()) {
                log.warn("No search criteria provided");
                return ResponseEntity.badRequest().body(
                    SearchChangeProcessingStageResponse.builder()
                        .eligibleNotices(java.util.Collections.emptyList())
                        .ineligibleNotices(java.util.Collections.emptyList())
                        .summary(SearchChangeProcessingStageResponse.SearchSummary.builder()
                            .total(0)
                            .eligible(0)
                            .ineligible(0)
                            .build())
                        .build()
                );
            }

            // Execute search using service
            SearchChangeProcessingStageResponse response =
                changeProcessingStageService.searchNotices(request);

            log.info("Search completed: total={}, eligible={}, ineligible={}",
                    response.getSummary().getTotal(),
                    response.getSummary().getEligible(),
                    response.getSummary().getIneligible());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing search request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                SearchChangeProcessingStageResponse.builder()
                    .eligibleNotices(java.util.Collections.emptyList())
                    .ineligibleNotices(java.util.Collections.emptyList())
                    .summary(SearchChangeProcessingStageResponse.SearchSummary.builder()
                        .total(0)
                        .eligible(0)
                        .ineligible(0)
                        .build())
                    .build()
            );
        }
    }

    /**
     * Retrieve Change Stage Reports by date range
     * Based on OCMS CPS Spec BE-007
     *
     * Endpoint: GET /v1/change-processing-stage/reports?startDate=2025-01-01&endDate=2025-01-31
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive - will query up to end of this day)
     * @return List of reports with download URLs
     */
    @GetMapping("/change-processing-stage/reports")
    public ResponseEntity<?> getReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {

        log.info("Retrieving change stage reports from {} to {}", startDate, endDate);

        try {
            // Validate date range (max 90 days)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 90) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of(
                        "error", "INVALID_DATE_RANGE",
                        "message", "Date range cannot exceed 90 days. Current range: " + daysBetween + " days"
                    ));
            }

            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of(
                        "error", "INVALID_DATE_RANGE",
                        "message", "End date must be after start date"
                    ));
            }

            // Convert to LocalDateTime for query
            java.time.LocalDateTime startDateTime = startDate.atStartOfDay();
            java.time.LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // Include entire end date

            // Query change records in date range
            java.util.List<OcmsChangeOfProcessing> records =
                changeProcessingStageService.getChangeRecordsByDateRange(startDateTime, endDateTime);

            // Group by date and generate report info
            java.util.Map<java.time.LocalDate, java.util.List<OcmsChangeOfProcessing>> groupedByDate =
                records.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        OcmsChangeOfProcessing::getDateOfChange
                    ));

            java.util.List<java.util.Map<String, Object>> reportInfoList = new java.util.ArrayList<>();

            for (java.util.Map.Entry<java.time.LocalDate, java.util.List<OcmsChangeOfProcessing>> entry :
                    groupedByDate.entrySet()) {

                java.time.LocalDate reportDate = entry.getKey();
                java.util.List<OcmsChangeOfProcessing> dateRecords = entry.getValue();

                // Get unique users who made changes on this date
                java.util.Set<String> users = dateRecords.stream()
                    .map(OcmsChangeOfProcessing::getCreUserId)
                    .collect(java.util.stream.Collectors.toSet());

                // For simplicity, generate report URL pattern
                // In production, you might store report URLs in a separate table
                String reportUrl = String.format(
                    "reports/change-stage/ChangeStageReport_%s_%s.xlsx",
                    reportDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    users.iterator().next()
                );

                java.util.Map<String, Object> reportInfo = new java.util.LinkedHashMap<>();
                reportInfo.put("reportDate", reportDate.toString());
                reportInfo.put("generatedBy", String.join(", ", users));
                reportInfo.put("noticeCount", dateRecords.size());
                reportInfo.put("reportUrl", reportUrl);

                reportInfoList.add(reportInfo);
            }

            // Sort by date descending
            reportInfoList.sort((a, b) ->
                ((String) b.get("reportDate")).compareTo((String) a.get("reportDate"))
            );

            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("reports", reportInfoList);
            response.put("totalReports", reportInfoList.size());
            response.put("totalNotices", records.size());

            log.info("Found {} reports containing {} total notices", reportInfoList.size(), records.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Failed to retrieve reports: " + e.getMessage()
                ));
        }
    }

    /**
     * Download individual change processing stage report
     * Based on OCMS CPS Spec §4 - Report Download
     *
     * Endpoint: GET /v1/change-processing-stage/report/{reportId}
     *
     * This endpoint streams the Excel report file from Azure Blob Storage.
     * The reportId should be the filename (e.g., "ChangeStageReport_20251220_143022_USER01.xlsx")
     *
     * @param reportId Report filename/ID
     * @return Excel file stream or 404 if not found
     */
    @GetMapping("/change-processing-stage/report/{reportId}")
    public ResponseEntity<?> downloadReport(@PathVariable String reportId) {

        log.info("Received report download request for reportId: {}", reportId);

        try {
            // Validate reportId format (basic sanitization)
            if (reportId == null || reportId.trim().isEmpty()) {
                log.warn("Invalid reportId: empty or null");
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of(
                        "error", "INVALID_REPORT_ID",
                        "message", "Report ID cannot be empty"
                    ));
            }

            // Sanitize reportId to prevent directory traversal
            String sanitizedReportId = reportId.replaceAll("[^a-zA-Z0-9._-]", "");
            if (!sanitizedReportId.equals(reportId)) {
                log.warn("Invalid characters in reportId: {}", reportId);
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of(
                        "error", "INVALID_REPORT_ID",
                        "message", "Report ID contains invalid characters"
                    ));
            }

            // Ensure .xlsx extension
            if (!reportId.toLowerCase().endsWith(".xlsx")) {
                reportId = reportId + ".xlsx";
            }

            // Construct blob path
            String blobPath = "reports/change-stage/" + reportId;

            log.info("Downloading report from blob: {}", blobPath);

            // Download report from Azure Blob Storage
            byte[] reportBytes = azureBlobStorageUtil.downloadFromBlob(blobPath);

            if (reportBytes == null) {
                log.warn("Report not found: {}", blobPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of(
                        "error", "REPORT_NOT_FOUND",
                        "message", "Report not found: " + reportId
                    ));
            }

            log.info("Successfully downloaded report {} ({} bytes)", reportId, reportBytes.length);

            // Stream the file as response
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"" + reportId + "\"")
                .header("Content-Length", String.valueOf(reportBytes.length))
                .body(reportBytes);

        } catch (Exception e) {
            log.error("Error downloading report {}: {}", reportId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "DOWNLOAD_FAILED",
                    "message", "Failed to download report: " + e.getMessage()
                ));
        }
    }

    /**
     * PLUS API endpoint for manual stage change
     * Based on OCMS CPS Spec §3 - Manual Change Processing Stage via PLUS Staff Portal
     *
     * Endpoint: POST /v1/external/plus/change-processing-stage
     *
     * Request body (example):
     * {
     *   "noticeNo": ["N-001", "N-002"],
     *   "lastStageName": "DN1",
     *   "nextStageName": "DN2",
     *   "lastStageDate": "2025-09-25T06:58:42",
     *   "newStageDate": "2025-09-30T06:58:42",
     *   "offenceType": "D",
     *   "source": "005"
     * }
     *
     * Response (success):
     * {
     *   "status": "SUCCESS",
     *   "message": "Stage change processed successfully",
     *   "noticeCount": 2
     * }
     *
     * Response (failure):
     * {
     *   "status": "FAILED",
     *   "code": "OCMS-4000",
     *   "message": "Stage transition not allowed: DN1 -> CFC"
     * }
     *
     * @param request PLUS change stage request
     * @return Response with status
     */
    @PostMapping("/external/plus/change-processing-stage")
    public ResponseEntity<?> plusChangeProcessingStage(
            @Valid @RequestBody PlusChangeStageRequest request) {

        log.info("Received PLUS change stage request for {} notices, lastStage={}, nextStage={}",
                request.getNoticeNo().size(), request.getLastStageName(), request.getNextStageName());

        try {
            // Process PLUS change request using existing service method
            changeProcessingStageService.processPlusChangeStage(request);

            log.info("PLUS change stage completed successfully for {} notices", request.getNoticeNo().size());

            // Return success response
            return ResponseEntity.ok(java.util.Map.of(
                "status", "SUCCESS",
                "message", "Stage change processed successfully",
                "noticeCount", request.getNoticeNo().size()
            ));

        } catch (ChangeOfProcessingService.PlusChangeStageException e) {
            // Expected business logic errors
            log.warn("PLUS change stage failed: {} - {}", e.getErrorCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(java.util.Map.of(
                    "status", "FAILED",
                    "code", e.getErrorCode(),
                    "message", e.getMessage()
                ));

        } catch (Exception e) {
            // Unexpected system errors
            log.error("Unexpected error processing PLUS change stage request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "status", "ERROR",
                    "code", "INTERNAL_ERROR",
                    "message", "Unexpected system error: " + e.getMessage()
                ));
        }
    }

    /**
     * Internal endpoint for Toppan cron to update VON processing stages
     * Based on OCMS 15 Spec §2.5.2 - Handling for Manual Stage Change Notice in Toppan Cron
     *
     * This endpoint is called by the generate_toppan_letters cron job after Toppan enquiry
     * files are generated. It updates VON processing stages and differentiates between
     * manual vs automatic stage changes.
     *
     * Endpoint: POST /v1/internal/toppan/update-stages
     *
     * Request body (example):
     * {
     *   "noticeNumbers": ["N-001", "N-002", "N-003"],
     *   "currentStage": "DN1",
     *   "processingDate": "2025-12-19T00:30:00"
     * }
     *
     * Response (example):
     * {
     *   "totalNotices": 3,
     *   "automaticUpdates": 2,
     *   "manualUpdates": 1,
     *   "skipped": 0,
     *   "errors": null,
     *   "success": true
     * }
     *
     * @param request Toppan stage update request
     * @return Update statistics
     */
    @PostMapping("/internal/toppan/update-stages")
    public ResponseEntity<com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse>
            updateToppanStages(
                @Valid @RequestBody com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateRequest request) {

        log.info("Received Toppan stage update request for {} notices, stage={}",
                request.getNoticeNumbers().size(), request.getCurrentStage());

        try {
            // Process Toppan stage updates
            com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse response =
                    changeProcessingStageService.processToppanStageUpdates(request);

            log.info("Toppan stage update completed: automatic={}, manual={}, skipped={}, errors={}",
                    response.getAutomaticUpdates(), response.getManualUpdates(),
                    response.getSkipped(), response.getErrors() != null ? response.getErrors().size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Unexpected error processing Toppan stage update", e);

            // Return error response
            com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse errorResponse =
                    com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse.builder()
                        .totalNotices(request.getNoticeNumbers() != null ? request.getNoticeNumbers().size() : 0)
                        .automaticUpdates(0)
                        .manualUpdates(0)
                        .skipped(0)
                        .errors(java.util.Collections.singletonList("Unexpected error: " + e.getMessage()))
                        .success(false)
                        .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
