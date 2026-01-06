package com.ocmsintranet.apiservice.workflows.furnish.rejection;

import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionResult;
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
 * REST controller for officer rejection workflow.
 * Based on OCMS 41 User Stories 41.24-41.33.
 */
@RestController
@RequestMapping("/${api.version}/furnish/officer")
@Slf4j
@RequiredArgsConstructor
public class FurnishRejectionController {

    private final FurnishRejectionService rejectionService;

    /**
     * Reject furnish submission (OCMS41.24-41.33)
     * POST /furnish/officer/reject
     *
     * @param request Rejection request with notification preferences
     * @return ResponseEntity with rejection result
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, Object>> rejectFurnish(@Valid @RequestBody FurnishRejectionRequest request) {
        log.info("Received rejection request - TxnNo: {}, Officer: {}",
                request.getTxnNo(), request.getOfficerId());

        FurnishRejectionResult result = rejectionService.handleRejection(request);

        if (result instanceof FurnishRejectionResult.Success success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("txnNo", success.txnNo());
            response.put("noticeNo", success.noticeNo());
            response.put("emailSentToOwner", success.emailSentToOwner());
            response.put("noticeResentToPortal", success.noticeResentToPortal());
            response.put("message", success.message());

            return ResponseEntity.ok(response);
        } else if (result instanceof FurnishRejectionResult.ValidationError validationError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "VALIDATION_ERROR");
            response.put("field", validationError.field());
            response.put("message", validationError.message());
            if (validationError.violations() != null) {
                response.put("violations", validationError.violations());
            }

            return ResponseEntity.badRequest().body(response);
        } else if (result instanceof FurnishRejectionResult.BusinessError businessError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "BUSINESS_ERROR");
            response.put("reason", businessError.reason());
            response.put("message", businessError.message());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (result instanceof FurnishRejectionResult.TechnicalError technicalError) {
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

        log.error("Unexpected error in furnish rejection controller", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
