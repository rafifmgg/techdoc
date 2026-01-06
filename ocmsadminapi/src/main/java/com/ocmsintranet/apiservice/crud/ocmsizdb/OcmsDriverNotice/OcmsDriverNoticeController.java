package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsDriverNotice entity
 * This controller exposes two specific endpoints:
 * 1. POST /drivernoticelist - for listing Driver Notice records
 * 2. POST /drivernoticeexists - for checking if a driver notice exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDriverNoticeController {

    private final OcmsDriverNoticeService service;

    public OcmsDriverNoticeController(OcmsDriverNoticeService service) {
        this.service = service;
        log.info("Driver Notice controller initialized");
    }

    /**
     * POST endpoint for listing Driver Notice records
     */
    @PostMapping("/drivernoticelist")
    public ResponseEntity<FindAllResponse<OcmsDriverNotice>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsDriverNotice> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to check if a driver notice exists by ID components
     * @param request Map containing dateOfProcessing and noticeNo
     * @return ResponseEntity with boolean indicating if driver notice exists
     */
    @PostMapping("/drivernoticeexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String dateOfProcessing = request.get("dateOfProcessing");
            String noticeNo = request.get("noticeNo");

            if (dateOfProcessing == null || noticeNo == null) {
                throw new IllegalArgumentException("dateOfProcessing and noticeNo are required");
            }

            // Create a OcmsDriverNoticeId object for the composite key
            OcmsDriverNoticeId driverNoticeId = new OcmsDriverNoticeId(
                java.time.LocalDateTime.parse(dateOfProcessing),
                noticeNo
            );

            // Check if driver notice exists by trying to get it
            Optional<OcmsDriverNotice> driverNotice = service.getById(driverNoticeId);
            boolean exists = driverNotice.isPresent();

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking driver notice existence: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}