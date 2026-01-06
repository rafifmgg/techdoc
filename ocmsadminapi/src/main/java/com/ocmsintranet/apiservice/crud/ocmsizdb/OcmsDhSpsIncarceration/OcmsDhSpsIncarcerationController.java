package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsIncarceration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsDhSpsIncarceration entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhspsincarceration - for creating SPS Incarceration records
 * 2. POST /dhspsincarcerationlist - for listing SPS Incarceration records
 * 3. POST /dhspsincarcerationpatch/{inmateNumber} - for updating SPS Incarceration records
 * 4. POST /dhspsincarcerationexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhSpsIncarcerationController {
    
    private final OcmsDhSpsIncarcerationService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhSpsIncarcerationController(OcmsDhSpsIncarcerationService service) {
        this.service = service;
        log.info("SPS Incarceration controller initialized");
    }
    
    /**
     * POST endpoint for listing SPS Incarceration records
     */
    @PostMapping("/dhspsincarcerationlist")
    public ResponseEntity<FindAllResponse<OcmsDhSpsIncarceration>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhSpsIncarceration> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an SPS Incarceration record exists by inmateNumber
     * @param request Map containing inmateNumber
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhspsincarcerationexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String inmateNumber = request.get("inmateNumber");
            
            if (inmateNumber == null) {
                throw new IllegalArgumentException("inmateNumber is required");
            }
            
            // Check if record exists by trying to get it
            Optional<OcmsDhSpsIncarceration> record = service.getById(inmateNumber);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking SPS Incarceration record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
