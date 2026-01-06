package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

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
 * Controller for OcmsRequestDriverParticulars entity
 * This controller exposes two specific endpoints:
 * 1. POST /requestdriverparticularslist - for listing Request Driver Particulars records
 * 2. POST /requestdriverparticularsexists - for checking if a request driver particulars exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsRequestDriverParticularsController {

    private final OcmsRequestDriverParticularsService service;

    public OcmsRequestDriverParticularsController(OcmsRequestDriverParticularsService service) {
        this.service = service;
        log.info("Request Driver Particulars controller initialized");
    }

    /**
     * POST endpoint for listing Request Driver Particulars records
     */
    @PostMapping("/requestdriverparticularslist")
    public ResponseEntity<FindAllResponse<OcmsRequestDriverParticulars>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsRequestDriverParticulars> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to check if a request driver particulars exists by ID components
     * @param request Map containing dateOfProcessing and noticeNo
     * @return ResponseEntity with boolean indicating if request driver particulars exists
     */
    @PostMapping("/requestdriverparticularsexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String dateOfProcessing = request.get("dateOfProcessing");
            String noticeNo = request.get("noticeNo");

            if (dateOfProcessing == null || noticeNo == null) {
                throw new IllegalArgumentException("dateOfProcessing and noticeNo are required");
            }

            // Create a OcmsRequestDriverParticularsId object for the composite key
            OcmsRequestDriverParticularsId requestDriverParticularsId = new OcmsRequestDriverParticularsId(
                java.time.LocalDateTime.parse(dateOfProcessing),
                noticeNo
            );

            // Check if request driver particulars exists by trying to get it
            Optional<OcmsRequestDriverParticulars> requestDriverParticulars = service.getById(requestDriverParticularsId);
            boolean exists = requestDriverParticulars.isPresent();

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking request driver particulars existence: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}