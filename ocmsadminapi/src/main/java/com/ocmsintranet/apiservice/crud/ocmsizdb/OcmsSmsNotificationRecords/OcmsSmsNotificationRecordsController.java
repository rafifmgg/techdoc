package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

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
 * Controller for OcmsSmsNotificationRecords entity
 * This controller exposes four specific endpoints:
 * 1. POST /smsnotificationrecord - for creating SMS notification records
 * 2. POST /smsnotificationrecordlist - for listing SMS notification records
 * 3. POST /smsnotificationrecordpatch/{noticeNo} - for updating SMS notification records
 * 4. POST /smsnotificationrecordexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsSmsNotificationRecordsController {
    
    private final OcmsSmsNotificationRecordsService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsSmsNotificationRecordsController(OcmsSmsNotificationRecordsService service) {
        this.service = service;
        log.info("SMS notification records controller initialized");
    }
    
    /**
     * POST endpoint for listing SMS notification records
     */
    @PostMapping("/smsnotificationrecordlist")
    public ResponseEntity<FindAllResponse<OcmsSmsNotificationRecords>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsSmsNotificationRecords> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an SMS notification record exists by noticeNo
     * @param request Map containing noticeNo
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/smsnotificationrecordexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String noticeNo = request.get("noticeNo");
            String processingStage = request.get("processingStage");
            
            if (noticeNo == null || processingStage == null) {
                throw new IllegalArgumentException("noticeNo and processingStage are required");
            }
            
            // Create a composite key object
            OcmsSmsNotificationRecordsId recordId = new OcmsSmsNotificationRecordsId(noticeNo, processingStage);
            
            // Check if record exists by trying to get it
            Optional<OcmsSmsNotificationRecords> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking SMS notification record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
}
