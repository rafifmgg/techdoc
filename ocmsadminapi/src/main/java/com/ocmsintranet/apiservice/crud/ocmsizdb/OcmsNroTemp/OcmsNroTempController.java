package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsNroTemp entity
 * This controller exposes endpoints for managing NRO (Notice Reminder Offender) temp records:
 * 1. POST /nrotemplist - for listing NRO temp records
 * 2. POST /nrotempunprocessed - for getting unprocessed records by query reason
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsNroTempController {

    private final OcmsNroTempService service;

    @Autowired
    private ObjectMapper objectMapper;

    public OcmsNroTempController(OcmsNroTempService service) {
        this.service = service;
        log.info("NRO Temp controller initialized");
    }

    /**
     * POST endpoint for listing NRO temp records
     */
    @PostMapping("/nrotemplist")
    public ResponseEntity<FindAllResponse<OcmsNroTemp>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsNroTemp> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to get unprocessed records by query reason
     * @param request Map containing queryReason ('UNC' or 'HST')
     * @return ResponseEntity with list of unprocessed records
     */
    @PostMapping("/nrotempunprocessed")
    public ResponseEntity<?> getUnprocessedByQueryReason(@RequestBody Map<String, String> request) {
        try {
            String queryReason = request.get("queryReason");

            if (queryReason == null) {
                throw new IllegalArgumentException("queryReason is required");
            }

            if (!queryReason.equals("UNC") && !queryReason.equals("HST")) {
                throw new IllegalArgumentException("queryReason must be 'UNC' or 'HST'");
            }

            // Get unprocessed records
            List<OcmsNroTemp> records = service.findUnprocessedByQueryReason(queryReason);

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("total", records.size());
            response.put("data", records);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving unprocessed NRO temp records: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
