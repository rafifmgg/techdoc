package com.ocmsintranet.cronservice.testing.agencies.toppan.controllers;

import com.ocmsintranet.cronservice.testing.agencies.toppan.services.ToppanUploadTestService;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for Toppan test scenarios
 * Implements test endpoints for Toppan upload workflow
 */
@Slf4j
@RestController
@RequestMapping("/test/toppan")
public class ToppanScenarioController {

    @Value("${test.endpoints.enabled:true}")
    private boolean testEndpointsEnabled;

    @Autowired
    private ToppanUploadTestService toppanUploadTestService;

    /**
     * Test Flow: Toppan Upload Flow
     *
     * Input: Stage parameter (RD1, RD2, RR3, DN1, DN2, DR3)
     * Process: Insert test data → Call Toppan upload API → Verify results
     * Verification:
     *   - Status updated correctly based on stage
     *   - Records processed correctly
     *   - Exclusion logic works as expected
     */
    @PostMapping("/toppan-upload-flow-sh")
    public ResponseEntity<?> runToppanUploadFlowTest(@RequestParam String stage) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        log.info("Running Toppan upload flow test for stage: {}", stage);
        List<TestStepResult> results = toppanUploadTestService.runTest(stage);
        return ResponseEntity.ok(results);
    }
}
