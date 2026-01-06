package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraCompanyDetail;

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
 * Controller for OcmsDhAcraCompanyDetail entity
 * This controller exposes four specific endpoints:
 * 1. POST /dhacracompanydetail - for creating ACRA company detail records
 * 2. POST /dhacracompanydetaillist - for listing ACRA company detail records
 * 3. POST /dhacracompanydetailpatch/{uen}/{noticeNo} - for updating ACRA company detail records
 * 4. POST /dhacracompanydetailexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsDhAcraCompanyDetailController {
    
    private final OcmsDhAcraCompanyDetailService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsDhAcraCompanyDetailController(OcmsDhAcraCompanyDetailService service) {
        this.service = service;
        log.info("ACRA company detail controller initialized");
    }
    
    /**
     * POST endpoint for listing ACRA company detail records
     */
    @PostMapping("/dhacracompanydetaillist")
    public ResponseEntity<FindAllResponse<OcmsDhAcraCompanyDetail>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsDhAcraCompanyDetail> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an ACRA company detail record exists by composite key
     * @param request Map containing uen and noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/dhacracompanydetailexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String uen = request.get("uen");
            String noticeNo = request.get("noticeNo");
            
            if (uen == null || noticeNo == null) {
                throw new IllegalArgumentException("Both uen and noticeNo are required");
            }
            
            // Create composite key
            OcmsDhAcraCompanyDetailId compositeId = new OcmsDhAcraCompanyDetailId(uen, noticeNo);
            
            // Check if record exists by trying to get it
            Optional<OcmsDhAcraCompanyDetail> record = service.getById(compositeId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking ACRA company detail record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
