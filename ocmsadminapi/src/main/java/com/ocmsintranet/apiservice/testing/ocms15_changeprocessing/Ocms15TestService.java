package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing;

import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestRequest;
import com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.dto.Ocms15TestResponse;

/**
 * OCMS 15 Change Processing Stage Integration Test Service
 *
 * Provides REST API endpoint for testing OCMS 15 functionality:
 * - POST /v1/ocms15-test: Run full integration test (4-step pattern)
 *
 * Note: Setup and cleanup are handled by Ocms15TestSetupService
 */
public interface Ocms15TestService {

    /**
     * Execute OCMS 15 integration test
     *
     * Follows 4-step pattern:
     * Step 1: Load Test Scenarios (from scenario.json)
     * Step 2: Trigger Change Processing Stage API (OCMS/PLUS)
     * Step 3: Fetch Verification Data (VON, ONOD, change processing)
     * Step 4: Verify Business Logic (stage, amount, audit, DH/MHA, Excel report)
     *
     * IMPORTANT: Call /ocms15-test-setup BEFORE running this test
     *
     * @param request Test request with configuration options
     * @return Test response with step results
     */
    Ocms15TestResponse executeTest(Ocms15TestRequest request);
}
