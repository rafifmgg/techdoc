package com.ocmsintranet.apiservice.workflows.furnish.manual.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the result of a bulk furnish operation.
 * Based on OCMS 41 User Stories 41.50-41.51.
 */
public sealed interface BulkFurnishResult {

    /**
     * Successful bulk furnish
     *
     * @param totalNotices Total number of notices in request
     * @param successCount Number of successfully processed notices
     * @param failedCount Number of failed notices
     * @param successNotices List of successfully processed notice numbers
     * @param failedNotices List of failed notice numbers with reasons
     * @param message Success message
     */
    @Builder
    record Success(
            int totalNotices,
            int successCount,
            int failedCount,
            List<String> successNotices,
            List<FailedNotice> failedNotices,
            String message
    ) implements BulkFurnishResult {
    }

    /**
     * Validation error
     */
    @Builder
    record ValidationError(
            String field,
            String message,
            List<String> violations
    ) implements BulkFurnishResult {
    }

    /**
     * Business rule error
     */
    @Builder
    record BusinessError(
            String reason,
            String message
    ) implements BulkFurnishResult {
    }

    /**
     * Technical error
     */
    @Builder
    record TechnicalError(
            String operation,
            String message,
            String cause,
            Map<String, Object> details
    ) implements BulkFurnishResult {
    }

    /**
     * Individual failed notice detail
     */
    @Builder
    record FailedNotice(
            String noticeNo,
            String reason,
            String message
    ) {
    }
}
