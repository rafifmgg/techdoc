package com.ocmsintranet.apiservice.workflows.notice_management.ownerdriver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling PLUS API owner driver endpoints
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class OwnerDriverControllerPlus {

    private final OwnerDriverServicePlus ownerDriverService;

    /**
     * Get list of offence notice owner drivers (PLUS Interface Specification)
     *
     * @param requestBody Request body containing noticeNo and/or idNo with optional filters
     * @return Response with offence notice owner driver data
     */
    @PostMapping("/plus-offence-notice-owner-driver")
    public ResponseEntity<?> getPlusOffenceNoticeOwnerDriver(@RequestBody Map<String, Object> requestBody) {
        log.info("Received request on /plus-offence-notice-owner-driver: {}", requestBody);
        return ownerDriverService.processPlusOffenceNoticeOwnerDriver(requestBody);
    }

    /**
     * POST endpoint for updating offence notice owner driver (PLUS Interface)
     *
     * @param payload Request body containing noticeNo, ownerDriverIndicator and fields to update
     * @return Response indicating success or failure
     */
    @PostMapping("/plus-update-offence-notice-owner-driver")
    public ResponseEntity<?> plusUpdateOffenceNoticeOwnerDriver(@RequestBody Map<String, Object> payload) {
        log.info("Received request on /plus-update-offence-notice-owner-driver: {}", payload);
        return ownerDriverService.processPlusUpdateOffenceNoticeOwnerDriver(payload);
    }
}
