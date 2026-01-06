package com.ocmsintranet.apiservice.workflows.furnish.approval.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the result of a furnish approval operation.
 * Based on OCMS 41 User Stories 41.15-41.23.
 */
public sealed interface FurnishApprovalResult {

    /**
     * Successful approval
     *
     * @param txnNo Transaction number
     * @param noticeNo Notice number
     * @param hirerDriverRecordUpdated Whether hirer/driver record was updated
     * @param suspensionRevived Whether TS-PDP suspension was revived
     * @param emailSentToOwner Whether email was sent to owner
     * @param emailSentToFurnished Whether email was sent to furnished person
     * @param smsSentToFurnished Whether SMS was sent to furnished person
     * @param message Success message
     */
    @Builder
    record Success(
            String txnNo,
            String noticeNo,
            boolean hirerDriverRecordUpdated,
            boolean suspensionRevived,
            boolean emailSentToOwner,
            boolean emailSentToFurnished,
            boolean smsSentToFurnished,
            String message
    ) implements FurnishApprovalResult {
    }

    /**
     * Validation error (request validation failed)
     */
    @Builder
    record ValidationError(
            String field,
            String message,
            List<String> violations
    ) implements FurnishApprovalResult {
    }

    /**
     * Business rule error (approval not allowed)
     *
     * @param reason Reason why approval is not allowed
     * @param message Error message
     */
    @Builder
    record BusinessError(
            String reason,
            String message
    ) implements FurnishApprovalResult {
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
    ) implements FurnishApprovalResult {
    }
}
