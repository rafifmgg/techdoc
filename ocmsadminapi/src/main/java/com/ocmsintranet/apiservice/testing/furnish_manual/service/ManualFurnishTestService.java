package com.ocmsintranet.apiservice.testing.furnish_manual.service;

import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestResponse;

public interface ManualFurnishTestService {

    /**
     * Execute manual furnish test for a specific scenario
     *
     * @param request Test request containing scenario name and options
     * @return Test response with execution results
     */
    ManualFurnishTestResponse executeTest(ManualFurnishTestRequest request);
}
