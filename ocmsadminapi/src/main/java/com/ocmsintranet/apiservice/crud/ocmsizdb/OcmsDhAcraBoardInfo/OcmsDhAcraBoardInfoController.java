package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraBoardInfo;

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
 * Controller for OcmsDhAcraBoardInfo entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhacraboardinfo - for creating ACRA board info records
 * 2. POST /dhacraboardinfolist - for listing ACRA board info records
 * 3. POST /dhacraboardinfopatch/{entityUen}/{personIdNo}/{noticeNo} - for updating ACRA board info records
 * 4. POST /dhacraboardinfoexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhAcraBoardInfoController {
    
    private final OcmsDhAcraBoardInfoService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhAcraBoardInfoController(OcmsDhAcraBoardInfoService service) {
        this.service = service;
        log.info("ACRA board info controller initialized");
    }
    
    /**
     * POST endpoint for listing ACRA board info records
     */
    @PostMapping("/dhacraboardinfolist")
    public ResponseEntity<FindAllResponse<OcmsDhAcraBoardInfo>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhAcraBoardInfo> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an ACRA board info record exists by composite key
     * @param request Map containing entityUen, personIdNo, and noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhacraboardinfoexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String entityUen = request.get("entityUen");
            String personIdNo = request.get("personIdNo");
            String noticeNo = request.get("noticeNo");
            
            if (entityUen == null || personIdNo == null || noticeNo == null) {
                throw new IllegalArgumentException("entityUen, personIdNo, and noticeNo are required");
            }
            
            // Create a composite key object
            OcmsDhAcraBoardInfoId recordId = new OcmsDhAcraBoardInfoId(entityUen, personIdNo, noticeNo);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhAcraBoardInfo> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking ACRA board info record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
