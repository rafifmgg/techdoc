package com.ocmsintranet.apiservice.workflows.furnish.submission;

import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionResult;

/**
 * Service interface for furnish submission workflow.
 * Based on OCMS 41 User Story 41.4-41.7.
 *
 * Main workflow steps:
 * 1. Validate submission request (format and business rules)
 * 2. Perform auto-approval checks (5 validations from OCMS41.5)
 * 3. Create furnish application record
 * 4. If auto-approved:
 *    a. Update current offender
 *    b. Create hirer/driver record
 *    c. Apply TS-PDP suspension (21 days)
 * 5. If manual review required:
 *    a. Create pending record with reason
 *    b. Apply TS-PDP suspension (21 days)
 */
public interface FurnishSubmissionService {

    /**
     * Handle furnish submission from eService.
     *
     * @param request The furnish submission request
     * @return Result indicating success, validation error, business error, or technical error
     */
    FurnishSubmissionResult handleFurnishSubmission(FurnishSubmissionRequest request);
}
