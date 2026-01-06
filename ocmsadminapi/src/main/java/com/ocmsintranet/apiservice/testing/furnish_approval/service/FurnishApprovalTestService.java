package com.ocmsintranet.apiservice.testing.furnish_approval.service;

import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestResponse;

/**
 * Service interface for Furnish Approval Integration Testing
 */
public interface FurnishApprovalTestService {

    /**
     * Execute a furnish approval test scenario
     *
     * @param request Test request with scenario name
     * @return Test execution results
     */
    FurnishApprovalTestResponse executeTest(FurnishApprovalTestRequest request);
}
