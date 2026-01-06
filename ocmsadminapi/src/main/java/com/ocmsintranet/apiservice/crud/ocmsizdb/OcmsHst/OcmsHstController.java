package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst;

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
 * Controller for OcmsHst entity
 * This controller exposes endpoints for managing HST (House Tenant) suspension records:
 * 1. POST /hstlist - for listing HST records
 * 2. POST /hstexists - for checking if an HST record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsHstController {

    private final OcmsHstService service;

    @Autowired
    private ObjectMapper objectMapper;

    public OcmsHstController(OcmsHstService service) {
        this.service = service;
        log.info("HST controller initialized");
    }

    /**
     * POST endpoint for listing HST records
     */
    @PostMapping("/hstlist")
    public ResponseEntity<FindAllResponse<OcmsHst>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsHst> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to check if an HST record exists by ID
     * @param request Map containing idNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/hstexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String idNo = request.get("idNo");

            if (idNo == null) {
                throw new IllegalArgumentException("idNo is required");
            }

            // Check if record exists by trying to get it
            Optional<OcmsHst> record = service.getById(idNo);
            boolean exists = record.isPresent();

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            if (exists) {
                response.put("data", record.get());
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking HST record existence: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
