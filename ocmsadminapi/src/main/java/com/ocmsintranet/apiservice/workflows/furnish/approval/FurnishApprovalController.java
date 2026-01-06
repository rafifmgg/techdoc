package com.ocmsintranet.apiservice.workflows.furnish.approval;

import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalRequest;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalResult;
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
 * REST controller for officer approval workflow.
 * Based on OCMS 41 User Stories 41.15-41.23.
 */
@RestController
@RequestMapping("/${api.version}/furnish/officer")
@Slf4j
@RequiredArgsConstructor
public class FurnishApprovalController {

    private final FurnishApprovalService approvalService;

    /**
     * Approve furnish submission (OCMS41.15-41.23)
     * POST /furnish/officer/approve
     *
     * @param request Approval request with notification preferences
     * @return ResponseEntity with approval result
     */
    @PostMapping("/approve")
    public ResponseEntity<Map<String, Object>> approveFurnish(@Valid @RequestBody FurnishApprovalRequest request) {
        log.info("Received approval request - TxnNo: {}, Officer: {}",
                request.getTxnNo(), request.getOfficerId());

        FurnishApprovalResult result = approvalService.handleApproval(request);

        if (result instanceof FurnishApprovalResult.Success success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("txnNo", success.txnNo());
            response.put("noticeNo", success.noticeNo());
            response.put("hirerDriverRecordUpdated", success.hirerDriverRecordUpdated());
            response.put("suspensionRevived", success.suspensionRevived());
            response.put("emailSentToOwner", success.emailSentToOwner());
            response.put("emailSentToFurnished", success.emailSentToFurnished());
            response.put("smsSentToFurnished", success.smsSentToFurnished());
            response.put("message", success.message());

            return ResponseEntity.ok(response);
        } else if (result instanceof FurnishApprovalResult.ValidationError validationError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "VALIDATION_ERROR");
            response.put("field", validationError.field());
            response.put("message", validationError.message());
            if (validationError.violations() != null) {
                response.put("violations", validationError.violations());
            }

            return ResponseEntity.badRequest().body(response);
        } else if (result instanceof FurnishApprovalResult.BusinessError businessError) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorType", "BUSINESS_ERROR");
            response.put("reason", businessError.reason());
            response.put("message", businessError.message());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (result instanceof FurnishApprovalResult.TechnicalError technicalError) {
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

        log.error("Unexpected error in furnish approval controller", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
