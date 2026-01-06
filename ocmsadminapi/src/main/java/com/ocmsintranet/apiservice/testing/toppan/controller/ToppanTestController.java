package com.ocmsintranet.apiservice.testing.toppan.controller;

import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadResponse;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadResponse;
import com.ocmsintranet.apiservice.testing.toppan.service.ToppanTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Toppan testing endpoints
 */
@RestController
@RequestMapping("/v1")
@Slf4j
public class ToppanTestController {

    private final ToppanTestService toppanTestService;

    public ToppanTestController(ToppanTestService toppanTestService) {
        this.toppanTestService = toppanTestService;
    }

    /**
     * POST endpoint for Toppan upload testing
     * Executes multiple steps and returns results for all steps
     *
     * @param request ToppanUploadRequest containing preload flag (default: true)
     * @return ResponseEntity with ToppanUploadResponse containing step execution results
     */
    @PostMapping("/toppan-upload")
    public ResponseEntity<ToppanUploadResponse> handleToppanUpload(@RequestBody ToppanUploadRequest request) {
        log.info("Received POST /toppan-upload with preload={}", request.getPreload());

        ToppanUploadResponse response = toppanTestService.executeToppanUpload(request);

        log.info("Toppan upload test completed with {} steps", response.getSteps().size());
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint for Toppan download testing
     * Executes 4 steps and returns results for all steps
     *
     * @param request ToppanDownloadRequest containing triggerCron and details flags (default: true)
     * @return ResponseEntity with ToppanDownloadResponse containing step execution results
     */
    @PostMapping("/toppan-download")
    public ResponseEntity<ToppanDownloadResponse> handleToppanDownload(@RequestBody ToppanDownloadRequest request) {
        log.info("Received POST /toppan-download with triggerCron={}", request.getTriggerCron());

        ToppanDownloadResponse response = toppanTestService.executeToppanDownload(request);

        log.info("Toppan download test completed with {} steps", response.getSteps().size());
        return ResponseEntity.ok(response);
    }
}
