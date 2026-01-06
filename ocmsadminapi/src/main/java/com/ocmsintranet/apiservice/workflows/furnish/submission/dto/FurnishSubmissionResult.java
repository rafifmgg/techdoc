package com.ocmsintranet.apiservice.workflows.furnish.submission.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the result of a furnish submission operation.
 * Based on OCMS 41 requirements and reduction workflow pattern.
 */
public sealed interface FurnishSubmissionResult {

    /**
     * Successful furnish submission with optional auto-approval
     *
     * @param furnishApplication The created furnish application record
     * @param autoApproved Whether the submission was auto-approved
     * @param hirerDriverRecordCreated Whether hirer/driver record was auto-created
     * @param suspensionApplied Whether TS-PDP suspension was applied
     * @param message Success message
     */
    @Builder
    record Success(
            OcmsFurnishApplication furnishApplication,
            boolean autoApproved,
            boolean hirerDriverRecordCreated,
            boolean suspensionApplied,
            String message
    ) implements FurnishSubmissionResult {
    }

    /**
     * Validation error (request format/business validation failed)
     *
     * @param field The field that failed validation (if applicable)
     * @param message Error message
     * @param violations List of all validation violations
     */
    @Builder
    record ValidationError(
            String field,
            String message,
            List<String> violations
    ) implements FurnishSubmissionResult {
    }

    /**
     * Business rule error (auto-approval check failed, manual review required)
     *
     * @param checkType The auto-approval check that failed
     * @param message Error message explaining why manual review is required
     * @param requiresManualReview Always true for this variant
     * @param furnishApplication The created furnish application (pending status)
     */
    @Builder
    record BusinessError(
            String checkType,
            String message,
            boolean requiresManualReview,
            OcmsFurnishApplication furnishApplication
    ) implements FurnishSubmissionResult {
    }

    /**
     * Technical error (database, external system, etc.)
     *
     * @param operation The operation that failed
     * @param message Error message
     * @param cause Root cause exception class name (if available)
     * @param details Additional error details
     */
    @Builder
    record TechnicalError(
            String operation,
            String message,
            String cause,
            Map<String, Object> details
    ) implements FurnishSubmissionResult {
    }
}
