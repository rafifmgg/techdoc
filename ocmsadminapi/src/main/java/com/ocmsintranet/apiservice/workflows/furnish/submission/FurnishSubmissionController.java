package com.ocmsintranet.apiservice.workflows.furnish.submission;

import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionResult;
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
 * REST controller for furnish submission workflow.
 * Exposes endpoints for eService to submit furnish hirer/driver applications.
 *
 * Based on OCMS 41 User Story 41.4-41.7.
 */
@RestController
@RequestMapping("/${api.version}/furnish")
@Slf4j
@RequiredArgsConstructor
public class FurnishSubmissionController {

    private final FurnishSubmissionService furnishSubmissionService;

    /**
     * Submit furnish hirer/driver application from eService.
     *
     * @param request The furnish submission request with @Valid annotations
     * @return ResponseEntity with result details
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitFurnish(@Valid @RequestBody FurnishSubmissionRequest request) {
        log.info("Received furnish submission request - TxnNo: {}, NoticeNo: {}",
                request.getTxnNo(), request.getNoticeNo());

        FurnishSubmissionResult result = furnishSubmissionService.handleFurnishSubmission(request);

        if (result instanceof FurnishSubmissionResult.Success success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("txnNo", success.furnishApplication().getTxnNo());
            response.put("noticeNo", success.furnishApplication().getNoticeNo());
            response.put("status", success.furnishApplication().getStatus());
            response.put("autoApproved", success.autoApproved());
            response.put("hirerDriverRecordCreated", success.hirerDriverRecordCreated());
            response.put("suspensionApplied", success.suspensionApplied());
            response.put("message", success.message());

            return ResponseEntity.ok(response);
        } else if (result instanceof FurnishSubmissionResult.ValidationError validationError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "VALIDATION_ERROR");
            response.put("field", validationError.field());
            response.put("message", validationError.message());
            if (validationError.violations() != null) {
                response.put("violations", validationError.violations());
            }

            return ResponseEntity.badRequest().body(response);
        } else if (result instanceof FurnishSubmissionResult.BusinessError businessError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "BUSINESS_ERROR");
            response.put("checkType", businessError.checkType());
            response.put("message", businessError.message());
            response.put("requiresManualReview", businessError.requiresManualReview());
            if (businessError.furnishApplication() != null) {
                response.put("txnNo", businessError.furnishApplication().getTxnNo());
                response.put("noticeNo", businessError.furnishApplication().getNoticeNo());
                response.put("status", businessError.furnishApplication().getStatus());
            }

            // Return 200 OK since submission was accepted but requires manual review
            return ResponseEntity.ok(response);
        } else if (result instanceof FurnishSubmissionResult.TechnicalError technicalError) {
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
     * Exception handler for bean validation errors.
     * Converts @Valid annotation violations to structured error response.
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
     * Exception handler for generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorType", "TECHNICAL_ERROR");
        response.put("message", "Unexpected error: " + ex.getMessage());

        log.error("Unexpected error in furnish submission controller", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
