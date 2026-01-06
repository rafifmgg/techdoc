package com.ocmsintranet.apiservice.testing.furnish_rejection.service;

import com.ocmsintranet.apiservice.testing.furnish_rejection.dto.FurnishRejectionTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_rejection.dto.FurnishRejectionTestResponse;

/**
 * Service interface for Furnish Rejection Integration Testing
 */
public interface FurnishRejectionTestService {

    /**
     * Execute a furnish rejection test scenario
     *
     * @param request Test request with scenario name
     * @return Test execution results
     */
    FurnishRejectionTestResponse executeTest(FurnishRejectionTestRequest request);
}
