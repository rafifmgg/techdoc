package com.ocmsintranet.apiservice.workflows.furnish.manual.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Sealed interface representing the result of a manual furnish operation.
 * Based on OCMS 41 User Stories 41.46-41.48.
 */
public sealed interface ManualFurnishResult {

    /**
     * Successful manual furnish
     *
     * @param noticeNo Notice number
     * @param ownerDriverIndicator H or D
     * @param idNo Furnished person ID
     * @param name Furnished person name
     * @param recordUpdated Whether record was updated (true) or created (false)
     * @param message Success message
     */
    @Builder
    record Success(
            String noticeNo,
            String ownerDriverIndicator,
            String idNo,
            String name,
            boolean recordUpdated,
            String message
    ) implements ManualFurnishResult {
    }

    /**
     * Validation error
     */
    @Builder
    record ValidationError(
            String field,
            String message,
            List<String> violations
    ) implements ManualFurnishResult {
    }

    /**
     * Business rule error
     *
     * @param reason Reason code
     * @param message Error message
     */
    @Builder
    record BusinessError(
            String reason,
            String message
    ) implements ManualFurnishResult {
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
    ) implements ManualFurnishResult {
    }
}
