package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraShareholderInfo;

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
 * Controller for OcmsDhAcraShareholderInfo entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhacrashareholderinfo - for creating ACRA shareholder info records
 * 2. POST /dhacrashareholderinfolist - for listing ACRA shareholder info records
 * 3. POST /dhacrashareholderinfopatch/{companyUen}/{personIdNo}/{noticeNo} - for updating ACRA shareholder info records
 * 4. POST /dhacrashareholderinfoexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhAcraShareholderInfoController {
    
    private final OcmsDhAcraShareholderInfoService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhAcraShareholderInfoController(OcmsDhAcraShareholderInfoService service) {
        this.service = service;
        log.info("ACRA shareholder info controller initialized");
    }
    
    /**
     * POST endpoint for listing ACRA shareholder info records
     */
    @PostMapping("/dhacrashareholderinfolist")
    public ResponseEntity<FindAllResponse<OcmsDhAcraShareholderInfo>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhAcraShareholderInfo> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an ACRA shareholder info record exists by composite key
     * @param request Map containing companyUen, personIdNo, and noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhacrashareholderinfoexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String companyUen = request.get("companyUen");
            String personIdNo = request.get("personIdNo");
            String noticeNo = request.get("noticeNo");
            
            if (companyUen == null || personIdNo == null || noticeNo == null) {
                throw new IllegalArgumentException("companyUen, personIdNo, and noticeNo are required");
            }
            
            // Create a composite key object
            OcmsDhAcraShareholderInfoId recordId = new OcmsDhAcraShareholderInfoId(companyUen, personIdNo, noticeNo);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhAcraShareholderInfo> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking ACRA shareholder info record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
