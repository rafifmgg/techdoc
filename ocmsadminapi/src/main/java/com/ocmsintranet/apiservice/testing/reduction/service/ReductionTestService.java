package com.ocmsintranet.apiservice.testing.reduction.service;

import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestRequest;
import com.ocmsintranet.apiservice.testing.reduction.dto.ReductionTestResponse;

/**
 * Service interface for Reduction testing operations
 */
public interface ReductionTestService {

    /**
     * Execute reduction test workflow with 4 steps
     *
     * @param request ReductionTestRequest containing configuration flags
     * @return ReductionTestResponse with results from all steps (never throws exception)
     */
    ReductionTestResponse executeReductionTest(ReductionTestRequest request);
}
