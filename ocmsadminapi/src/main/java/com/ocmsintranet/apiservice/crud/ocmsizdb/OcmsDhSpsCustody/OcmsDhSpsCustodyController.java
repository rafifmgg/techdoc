package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsCustody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsDhSpsCustody entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhspscustody - for creating SPS Custody records
 * 2. POST /dhspscustodylist - for listing SPS Custody records
 * 3. POST /dhspscustodypatch/{idNo}/{noticeNo}/{admDate} - for updating SPS Custody records
 * 4. POST /dhspscustodyexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhSpsCustodyController {
    
    private final OcmsDhSpsCustodyService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhSpsCustodyController(OcmsDhSpsCustodyService service) {
        this.service = service;
        log.info("SPS Custody controller initialized");
    }
    
    /**
     * POST endpoint for listing SPS Custody records
     */
    @PostMapping("/dhspscustodylist")
    public ResponseEntity<FindAllResponse<OcmsDhSpsCustody>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhSpsCustody> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an SPS Custody record exists by composite key
     * @param request Map containing idNo, noticeNo, and admDate
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhspscustodyexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, Object> request) {
        try {
            String idNo = (String) request.get("idNo");
            String noticeNo = (String) request.get("noticeNo");
            
            // The admDate needs to be converted from the request format to LocalDateTime
            LocalDateTime admDate = null;
            Object admDateObj = request.get("admDate");
            if (admDateObj instanceof String) {
                // Parse the date string to LocalDateTime
                admDate = LocalDateTime.parse((String) admDateObj);
            }
            
            if (idNo == null || noticeNo == null || admDate == null) {
                throw new IllegalArgumentException("idNo, noticeNo, and admDate are required");
            }
            
            // Create a composite key object
            OcmsDhSpsCustodyId recordId = new OcmsDhSpsCustodyId(idNo, noticeNo, admDate);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhSpsCustody> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking SPS Custody record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
