package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

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
 * Controller for OcmsFurnishApplicationDoc entity
 * This controller exposes endpoints for:
 * 1. POST /furnishapplicationdoclist - for listing furnish application document records
 * 2. POST /furnishapplicationdocexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsFurnishApplicationDocController {

    private final OcmsFurnishApplicationDocService service;

    @Autowired
    private ObjectMapper objectMapper;

    public OcmsFurnishApplicationDocController(OcmsFurnishApplicationDocService service) {
        this.service = service;
        log.info("Furnish application doc controller initialized");
    }

    /**
     * POST endpoint for listing furnish application document records
     */
    @PostMapping("/furnishapplicationdoclist")
    public ResponseEntity<FindAllResponse<OcmsFurnishApplicationDoc>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsFurnishApplicationDoc> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to check if a furnish application document record exists by composite key
     * @param request Map containing txnNo and attachmentId
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/furnishapplicationdocexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, Object> request) {
        try {
            String txnNo = (String) request.get("txnNo");
            Integer attachmentId = (Integer) request.get("attachmentId");

            if (txnNo == null || attachmentId == null) {
                throw new IllegalArgumentException("txnNo and attachmentId are required");
            }

            // Create a composite key object
            OcmsFurnishApplicationDocId recordId = new OcmsFurnishApplicationDocId(txnNo, attachmentId);

            // Check if record exists by trying to get it
            Optional<OcmsFurnishApplicationDoc> record = service.getById(recordId);
            boolean exists = record.isPresent();

            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking furnish application doc record existence: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
