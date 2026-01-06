package com.ocmsintranet.cronservice.testing.suspension_revival.ts.rov.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

/**
 * TS-ROV Test Service Interface
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 */
public interface TsRovTestService {

    /**
     * Execute TS-ROV test with 4-step workflow
     *
     * @param request Test request with details and triggerApi flags
     * @return Test response with all 4 steps
     */
    SuspensionRevivalTestResponse executeTsRovTest(SuspensionRevivalTestRequest request);
}
