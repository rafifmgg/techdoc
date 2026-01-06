package com.ocmsintranet.apiservice.testing.furnish_submission.service;

import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestResponse;

/**
 * Service interface for Furnish Submission Integration Testing
 */
public interface FurnishSubmissionTestService {

    /**
     * Execute a furnish submission test scenario
     *
     * @param request Test request with scenario name
     * @return Test execution results
     */
    FurnishSubmissionTestResponse executeTest(FurnishSubmissionTestRequest request);
}
