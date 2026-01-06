package com.ocmsintranet.apiservice.workflows.furnish.rejection;

import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionResult;

/**
 * Service interface for officer rejection workflow.
 * Based on OCMS 41 User Stories 41.24-41.33.
 *
 * Rejection workflow steps:
 * 1. Validate request and check application status
 * 2. Send email notification to owner (if selected)
 * 3. Update status to 'R' (Rejected)
 * 4. Update remarks with officer ID, rejection date, and reason
 * 5. Resend notice to eService portal (OCMS41.33)
 *
 * Note: TS-PDP suspension remains active (no revival on rejection)
 */
public interface FurnishRejectionService {

    /**
     * Handle officer rejection of furnish submission.
     *
     * @param request The rejection request with notification preferences
     * @return Result indicating success, validation error, business error, or technical error
     */
    FurnishRejectionResult handleRejection(FurnishRejectionRequest request);
}
