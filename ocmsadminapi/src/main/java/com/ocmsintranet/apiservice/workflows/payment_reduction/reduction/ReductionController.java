package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling reduction requests from external systems (e.g., PLUS).
 *
 * Endpoint: POST /v1/reduction
 */
@RestController
@RequestMapping("/v1")
@Slf4j
@RequiredArgsConstructor
public class ReductionController {

    private final ReductionService reductionService;

    @PostMapping("/plus-apply-reduction")
    public ResponseEntity<CrudResponse<?>> handleReduction(
            HttpServletRequest request,
            @Valid @RequestBody ReductionRequest reductionRequest) {

        log.info("Received reduction request for notice: {}", reductionRequest.getNoticeNo());

        ReductionResult result = reductionService.handleReductionRequest(reductionRequest);
        CrudResponse<?> response = mapResultToResponse(result);
        HttpStatus httpStatus = determineHttpStatus(result);

        log.info("Reduction request processed for notice: {} - Result: {} - HTTP Status: {}",
                reductionRequest.getNoticeNo(),
                result.getClass().getSimpleName(),
                httpStatus.value());

        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * Map ReductionResult to CrudResponse with spec-compliant messages.
     *
     * Spec messages:
     * - "Reduction Success" (200)
     * - "Invalid format" (400)
     * - "Missing data" (400)
     * - "Notice not found" (404)
     * - "Notice has been paid" (409)
     * - "Notice is not eligible" (409)
     * - "Reduction fail" (500)
     * - "System unavailable" (503)
     */
    private CrudResponse<?> mapResultToResponse(ReductionResult result) {
        if (result instanceof ReductionResult.Success) {
            return CrudResponse.success("Reduction Success");
        } else if (result instanceof ReductionResult.ValidationError validation) {
            // Map validation errors to spec messages
            return mapValidationError(validation);
        } else if (result instanceof ReductionResult.BusinessError business) {
            // Map business errors to spec messages
            return mapBusinessError(business);
        } else if (result instanceof ReductionResult.TechnicalError technical) {
            // Map technical errors to spec messages
            return mapTechnicalError(technical);
        }
        return CrudResponse.error(CrudResponse.AppCodes.INTERNAL_SERVER_ERROR, "Reduction fail");
    }

    private CrudResponse<?> mapValidationError(ReductionResult.ValidationError validation) {
        return switch (validation.code()) {
            case "INVALID_FORMAT" -> CrudResponse.error(CrudResponse.AppCodes.BAD_REQUEST, "Invalid format");
            case "MISSING_DATA" -> CrudResponse.error(CrudResponse.AppCodes.BAD_REQUEST, "Missing data");
            case "INVALID_REDUCTION_AMOUNT", "INCONSISTENT_AMOUNTS", "NEGATIVE_AMOUNT", "INVALID_DATES" ->
                    CrudResponse.error(CrudResponse.AppCodes.BAD_REQUEST, "Invalid format");
            default -> CrudResponse.error(CrudResponse.AppCodes.BAD_REQUEST, "Invalid format");
        };
    }

    private CrudResponse<?> mapBusinessError(ReductionResult.BusinessError business) {
        return switch (business.code()) {
            case "NOTICE_NOT_FOUND" -> CrudResponse.error(CrudResponse.AppCodes.NOT_FOUND, "Notice not found");
            case "NOTICE_PAID" -> CrudResponse.error(CrudResponse.AppCodes.CONFLICT, "Notice has been paid");
            case "NOT_ELIGIBLE" -> CrudResponse.error(CrudResponse.AppCodes.CONFLICT, "Notice is not eligible");
            default -> CrudResponse.error(CrudResponse.AppCodes.CONFLICT, business.message());
        };
    }

    private CrudResponse<?> mapTechnicalError(ReductionResult.TechnicalError technical) {
        return switch (technical.code()) {
            case "SYSTEM_UNAVAILABLE" -> CrudResponse.error(CrudResponse.AppCodes.SERVICE_UNAVAILABLE, "System unavailable");
            case "ROLLBACK_FAILURE", "DATABASE_ERROR" ->
                    CrudResponse.error(CrudResponse.AppCodes.INTERNAL_SERVER_ERROR, "Reduction fail");
            default -> CrudResponse.error(CrudResponse.AppCodes.INTERNAL_SERVER_ERROR, "Reduction fail");
        };
    }

    private HttpStatus determineHttpStatus(ReductionResult result) {
        if (result instanceof ReductionResult.Success) {
            return HttpStatus.OK;
        } else if (result instanceof ReductionResult.ValidationError) {
            return HttpStatus.BAD_REQUEST;
        } else if (result instanceof ReductionResult.BusinessError business) {
            // Map business errors to correct HTTP status
            return switch (business.code()) {
                case "NOTICE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "NOTICE_PAID", "NOT_ELIGIBLE" -> HttpStatus.CONFLICT;
                default -> HttpStatus.CONFLICT;
            };
        } else if (result instanceof ReductionResult.TechnicalError technical) {
            // Map technical errors to correct HTTP status
            return switch (technical.code()) {
                case "SYSTEM_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Global exception handler for validation errors.
     * This catches bean validation failures before the request reaches the service.
     *
     * Per spec: Returns "Missing data" for mandatory field violations, "Invalid format" for others.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<CrudResponse<?>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        log.warn("Validation error in reduction request: {}", ex.getMessage());

        // Check if error is due to missing mandatory fields
        boolean hasMissingFields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .anyMatch(error -> error.getDefaultMessage() != null &&
                        (error.getDefaultMessage().contains("must not be null") ||
                         error.getDefaultMessage().contains("must not be blank")));

        String message = hasMissingFields ? "Missing data" : "Invalid format";
        CrudResponse<?> response = CrudResponse.error(CrudResponse.AppCodes.BAD_REQUEST, message);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Global exception handler for unexpected errors.
     *
     * Per spec: Returns "Reduction fail" for unexpected system errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CrudResponse<?>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error in reduction controller: {}", ex.getMessage(), ex);

        CrudResponse<?> response = CrudResponse.error(CrudResponse.AppCodes.INTERNAL_SERVER_ERROR,
                "Reduction fail");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
