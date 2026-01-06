package com.ocmsintranet.apiservice.workflows.furnish.rejection.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the result of a furnish rejection operation.
 * Based on OCMS 41 User Stories 41.24-41.32.
 */
public sealed interface FurnishRejectionResult {

    /**
     * Successful rejection
     *
     * @param txnNo Transaction number
     * @param noticeNo Notice number
     * @param emailSentToOwner Whether email was sent to owner
     * @param noticeResentToPortal Whether notice was resent to eService portal
     * @param message Success message
     */
    @Builder
    record Success(
            String txnNo,
            String noticeNo,
            boolean emailSentToOwner,
            boolean noticeResentToPortal,
            String message
    ) implements FurnishRejectionResult {
    }

    /**
     * Validation error (request validation failed)
     */
    @Builder
    record ValidationError(
            String field,
            String message,
            List<String> violations
    ) implements FurnishRejectionResult {
    }

    /**
     * Business rule error (rejection not allowed)
     *
     * @param reason Reason why rejection is not allowed
     * @param message Error message
     */
    @Builder
    record BusinessError(
            String reason,
            String message
    ) implements FurnishRejectionResult {
    }

    /**
     * Technical error (database, external system, etc.)
     */
    @Builder
    record TechnicalError(
            String operation,
            String message,
            String cause,
            Map<String, Object> details
    ) implements FurnishRejectionResult {
    }
}
