package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction;

import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionResult;

/**
 * Service interface for handling reduction requests.
 *
 * This is the main entry point for reduction processing, orchestrating
 * the entire workflow from validation through persistence.
 */
public interface ReductionService {

    /**
     * Handle a reduction request through the full workflow.
     *
     * Workflow steps:
     * 1. Receive and validate request format (handled by @Valid)
     * 2. Validate mandatory data (handled by @Valid)
     * 3. Load notice by notice number
     * 4. Check if notice has been paid
     * 5. Check eligibility (computer rule code + processing stage)
     * 6. Build reduction context
     * 7. Perform transactional database updates
     * 8. Return result
     *
     * @param request The reduction request from external system (e.g., PLUS)
     * @return ReductionResult indicating the outcome (Success, ValidationError, BusinessError, TechnicalError)
     */
    ReductionResult handleReductionRequest(ReductionRequest request);
}
