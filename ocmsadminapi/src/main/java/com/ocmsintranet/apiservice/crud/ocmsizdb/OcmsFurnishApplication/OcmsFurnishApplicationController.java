package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsFurnishApplication entity
 * This controller exposes endpoints for:
 * 1. POST /furnishapplicationlist - for listing furnish application records
 * 2. POST /furnishapplicationexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsFurnishApplicationController {

    private final OcmsFurnishApplicationService service;

    @Autowired
    private ObjectMapper objectMapper;

    public OcmsFurnishApplicationController(OcmsFurnishApplicationService service) {
        this.service = service;
        log.info("Furnish application controller initialized");
    }

    /**
     * POST endpoint for listing furnish application records
     */
    @PostMapping("/furnishapplicationlist")
    public ResponseEntity<FindAllResponse<OcmsFurnishApplication>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsFurnishApplication> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to check if a furnish application record exists by txn_no
     * @param request Map containing txnNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/furnishapplicationexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String txnNo = request.get("txnNo");

            if (txnNo == null) {
                throw new IllegalArgumentException("txnNo is required");
            }

            // Check if record exists by trying to get it
            Optional<OcmsFurnishApplication> record = service.getById(txnNo);
            boolean exists = record.isPresent();

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking furnish application record existence: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
