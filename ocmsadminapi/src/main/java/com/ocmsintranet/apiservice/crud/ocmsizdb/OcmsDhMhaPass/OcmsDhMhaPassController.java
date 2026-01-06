package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMhaPass;

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
 * Controller for OcmsDhMhaPass entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhmhapass - for creating MHA pass records
 * 2. POST /dhmhapasslist - for listing MHA pass records
 * 3. POST /dhmhapasspatch/{idNo}/{noticeNo} - for updating MHA pass records
 * 4. POST /dhmhapassexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhMhaPassController {
    
    private final OcmsDhMhaPassService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhMhaPassController(OcmsDhMhaPassService service) {
        this.service = service;
        log.info("MHA pass controller initialized");
    }
    
    /**
     * POST endpoint for listing MHA pass records
     */
    @PostMapping("/dhmhapasslist")
    public ResponseEntity<FindAllResponse<OcmsDhMhaPass>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhMhaPass> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an MHA pass record exists by composite key
     * @param request Map containing idNo and noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhmhapassexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String idNo = request.get("idNo");
            String noticeNo = request.get("noticeNo");
            
            if (idNo == null || noticeNo == null) {
                throw new IllegalArgumentException("Both idNo and noticeNo are required");
            }
            
            // Create composite key
            OcmsDhMhaPassId compositeId = new OcmsDhMhaPassId(idNo, noticeNo);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhMhaPass> record = service.getById(compositeId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking MHA pass record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
