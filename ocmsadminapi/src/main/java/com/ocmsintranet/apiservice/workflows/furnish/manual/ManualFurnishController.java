package com.ocmsintranet.apiservice.workflows.furnish.manual;

import com.ocmsintranet.apiservice.workflows.furnish.dto.*;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.*;
import com.ocmsintranet.apiservice.workflows.furnish.manual.helpers.ManualFurnishValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for officer manual furnish operations.
 * Based on OCMS 41 User Stories 41.41-41.51.
 */
@RestController
@RequestMapping("/${api.version}/furnish/officer/manual")
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishController {

    private final ManualFurnishService manualFurnishService;
    private final ManualFurnishValidationService validationService;

    /**
     * Check if notice is furnishable (OCMS41.43)
     * GET /furnish/officer/manual/notice/{noticeNo}/furnishable
     *
     * @param noticeNo Notice number
     * @return Furnishable check result
     */
    @GetMapping("/notice/{noticeNo}/furnishable")
    public ResponseEntity<Map<String, Object>> checkFurnishable(@PathVariable String noticeNo) {
        log.info("Checking if notice is furnishable: {}", noticeNo);

        try {
            FurnishableCheckResponse response = validationService.checkFurnishable(noticeNo);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error checking furnishable status for notice: {}", noticeNo, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", "TECHNICAL_ERROR");
            errorResponse.put("message", "Error checking furnishable status: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Check existing owner/hirer/driver particulars (OCMS41.44-41.45)
     * GET /furnish/officer/manual/notice/{noticeNo}/check-existing
     *
     * @param noticeNo Notice number
     * @return Existing particulars check result
     */
    @GetMapping("/notice/{noticeNo}/check-existing")
    public ResponseEntity<Map<String, Object>> checkExistingParticulars(@PathVariable String noticeNo) {
        log.info("Checking existing particulars for notice: {}", noticeNo);

        try {
            ExistingParticularsResponse response = validationService.checkExistingParticulars(noticeNo);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error checking existing particulars for notice: {}", noticeNo, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", "TECHNICAL_ERROR");
            errorResponse.put("message", "Error checking existing particulars: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Manual furnish single notice (OCMS41.46-41.48)
     * POST /furnish/officer/manual/single
     *
     * @param request Manual furnish request
     * @return Manual furnish result
     */
    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> manualFurnishSingle(@Valid @RequestBody ManualFurnishRequest request) {
        log.info("Received manual furnish request - NoticeNo: {}, Type: {}, Officer: {}",
                request.getNoticeNo(), request.getOwnerDriverIndicator(), request.getOfficerId());

        ManualFurnishResult result = manualFurnishService.handleManualFurnish(request);

        if (result instanceof ManualFurnishResult.Success success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("noticeNo", success.noticeNo());
            response.put("ownerDriverIndicator", success.ownerDriverIndicator());
            response.put("idNo", success.idNo());
            response.put("name", success.name());
            response.put("recordUpdated", success.recordUpdated());
            response.put("message", success.message());

            return ResponseEntity.ok(response);
        } else if (result instanceof ManualFurnishResult.ValidationError validationError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "VALIDATION_ERROR");
            response.put("field", validationError.field());
            response.put("message", validationError.message());
            if (validationError.violations() != null) {
                response.put("violations", validationError.violations());
            }

            return ResponseEntity.badRequest().body(response);
        } else if (result instanceof ManualFurnishResult.BusinessError businessError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "BUSINESS_ERROR");
            response.put("reason", businessError.reason());
            response.put("message", businessError.message());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (result instanceof ManualFurnishResult.TechnicalError technicalError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "TECHNICAL_ERROR");
            response.put("operation", technicalError.operation());
            response.put("message", technicalError.message());
            response.put("cause", technicalError.cause());
            if (technicalError.details() != null) {
                response.put("details", technicalError.details());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Should never reach here
        throw new IllegalStateException("Unexpected result type: " + result.getClass());
    }

    /**
     * Bulk furnish multiple notices (OCMS41.50-41.51)
     * POST /furnish/officer/manual/bulk
     *
     * @param request Bulk furnish request
     * @return Bulk furnish result with success/failure counts
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> manualFurnishBulk(@Valid @RequestBody BulkFurnishRequest request) {
        log.info("Received bulk furnish request - {} notices, Type: {}, Officer: {}",
                request.getNoticeNos().size(), request.getOwnerDriverIndicator(), request.getOfficerId());

        BulkFurnishResult result = manualFurnishService.handleBulkFurnish(request);

        if (result instanceof BulkFurnishResult.Success success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalNotices", success.totalNotices());
            response.put("successCount", success.successCount());
            response.put("failedCount", success.failedCount());
            response.put("successNotices", success.successNotices());
            response.put("failedNotices", success.failedNotices());
            response.put("message", success.message());

            return ResponseEntity.ok(response);
        } else if (result instanceof BulkFurnishResult.ValidationError validationError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "VALIDATION_ERROR");
            response.put("field", validationError.field());
            response.put("message", validationError.message());
            if (validationError.violations() != null) {
                response.put("violations", validationError.violations());
            }

            return ResponseEntity.badRequest().body(response);
        } else if (result instanceof BulkFurnishResult.BusinessError businessError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "BUSINESS_ERROR");
            response.put("reason", businessError.reason());
            response.put("message", businessError.message());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (result instanceof BulkFurnishResult.TechnicalError technicalError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "TECHNICAL_ERROR");
            response.put("operation", technicalError.operation());
            response.put("message", technicalError.message());
            response.put("cause", technicalError.cause());
            if (technicalError.details() != null) {
                response.put("details", technicalError.details());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Should never reach here
        throw new IllegalStateException("Unexpected result type: " + result.getClass());
    }

    /**
     * Exception handler for bean validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
                    String message = error.getDefaultMessage();
                    return fieldName + ": " + message;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorType", "VALIDATION_ERROR");
        response.put("message", "Request validation failed");
        response.put("violations", violations);

        log.warn("Bean validation failed: {}", violations);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Exception handler for generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorType", "TECHNICAL_ERROR");
        response.put("message", "Unexpected error: " + ex.getMessage());

        log.error("Unexpected error in manual furnish controller", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
