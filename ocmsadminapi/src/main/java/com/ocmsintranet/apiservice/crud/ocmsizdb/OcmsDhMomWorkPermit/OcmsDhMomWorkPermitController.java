package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMomWorkPermit;

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
 * Controller for OcmsDhMomWorkPermit entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhmomworkpermit - for creating MOM work permit records
 * 2. POST /dhmomworkpermitlist - for listing MOM work permit records
 * 3. POST /dhmomworkpermitpatch/{idNo}/{noticeNo} - for updating MOM work permit records
 * 4. POST /dhmomworkpermitexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhMomWorkPermitController {
    
    private final OcmsDhMomWorkPermitService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhMomWorkPermitController(OcmsDhMomWorkPermitService service) {
        this.service = service;
        log.info("MOM work permit controller initialized");
    }
    
    /**
     * POST endpoint for listing MOM work permit records
     */
    @PostMapping("/dhmomworkpermitlist")
    public ResponseEntity<FindAllResponse<OcmsDhMomWorkPermit>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhMomWorkPermit> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if a MOM work permit record exists by composite key
     * @param request Map containing idNo and noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhmomworkpermitexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String idNo = request.get("idNo");
            String noticeNo = request.get("noticeNo");
            
            if (idNo == null || noticeNo == null) {
                throw new IllegalArgumentException("idNo and noticeNo are required");
            }
            
            // Create a composite key object
            OcmsDhMomWorkPermitId recordId = new OcmsDhMomWorkPermitId(idNo, noticeNo);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhMomWorkPermit> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking MOM work permit record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
