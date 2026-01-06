package com.ocmsintranet.apiservice.workflows.furnish.approval;

import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalRequest;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalResult;

/**
 * Service interface for officer approval workflow.
 * Based on OCMS 41 User Stories 41.15-41.23.
 *
 * Approval workflow steps:
 * 1. Validate request and check application status
 * 2. Check if notice is still furnishable (no PS suspension)
 * 3. Update hirer/driver records (set current offender)
 * 4. Revive from TS-PDP suspension
 * 5. Send email/SMS notifications (if selected)
 * 6. Update status to 'A' (Approved)
 * 7. Update remarks with officer ID and approval date
 */
public interface FurnishApprovalService {

    /**
     * Handle officer approval of furnish submission.
     *
     * @param request The approval request with notification preferences
     * @return Result indicating success, validation error, business error, or technical error
     */
    FurnishApprovalResult handleApproval(FurnishApprovalRequest request);
}
