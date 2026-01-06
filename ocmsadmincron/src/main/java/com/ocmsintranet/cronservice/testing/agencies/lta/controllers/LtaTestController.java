package com.ocmsintranet.cronservice.testing.agencies.lta.controllers;

import com.ocmsintranet.cronservice.testing.agencies.lta.models.TestStepResult;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaCallbackService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaErrorCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller to expose LTA test endpoints.
 * This controller is only active in non-production environments.
 */
@RestController
@RequestMapping("/test/lta")
@Profile({"!prod", "!production"}) // Disable in production environments
public class LtaTestController {

    private final LtaCallbackService ltaCallbackService;
    private final LtaErrorCodeService ltaErrorCodeService;

    @Value("${lta.test.enabled:true}")
    private boolean testEndpointsEnabled;

    @Autowired
    public LtaTestController(
            LtaCallbackService ltaCallbackService,
            LtaErrorCodeService ltaErrorCodeService) {
        this.ltaCallbackService = ltaCallbackService;
        this.ltaErrorCodeService = ltaErrorCodeService;
    }

    /**
     * Process result test flow.
     * This endpoint triggers the process result test flow which lists files in
     * SFTP container, downloads the latest file, and uploads it to the output
     * directory with a new name.
     *
     * @return List of test step results
     */
    @PostMapping("/lta-callback")
    public ResponseEntity<?> processCallbackTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = ltaCallbackService.processCallbackTest();
        return ResponseEntity.ok(results);
    }
    
    /**
     * Process error code A test (Total count does not match).
     * Creates a file with error code A and incorrect count in trailer.
     *
     * @return List of test step results
     */
    @PostMapping("/lta-error-a")
    public ResponseEntity<?> processErrorCodeA() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = ltaErrorCodeService.processErrorCodeA();
        return ResponseEntity.ok(results);
    }
    
    /**
     * Process error code B test (Missing Trailer).
     * Creates a file with error code B and includes trailer (with comment for easy modification).
     *
     * @return List of test step results
     */
    @PostMapping("/lta-error-b")
    public ResponseEntity<?> processErrorCodeB() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = ltaErrorCodeService.processErrorCodeB();
        return ResponseEntity.ok(results);
    }
    
    /**
     * Process error code C test (Missing Header).
     * Creates a file with error code C and includes header (with comment for easy modification).
     *
     * @return List of test step results
     */
    @PostMapping("/lta-error-c")
    public ResponseEntity<?> processErrorCodeC() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = ltaErrorCodeService.processErrorCodeC();
        return ResponseEntity.ok(results);
    }
}
