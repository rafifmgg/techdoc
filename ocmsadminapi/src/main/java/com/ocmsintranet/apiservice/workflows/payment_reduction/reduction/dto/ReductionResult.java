package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto;

/**
 * Interface representing all possible outcomes of a reduction request.
 */
public interface ReductionResult {

    /**
     * Successful reduction processing
     */
    record Success(String noticeNo, String message) implements ReductionResult {}

    /**
     * Validation error (format, mandatory fields, etc.)
     */
    record ValidationError(String code, String message) implements ReductionResult {}

    /**
     * Business rule error (not eligible, already paid, etc.)
     */
    record BusinessError(String code, String message, String reason) implements ReductionResult {
        public BusinessError(String code, String message) {
            this(code, message, null);
        }
    }

    /**
     * Technical/system error (database failure, unexpected exception, etc.)
     */
    record TechnicalError(String code, String message, Throwable cause) implements ReductionResult {
        public TechnicalError(String code, String message) {
            this(code, message, null);
        }
    }
}
