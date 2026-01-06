package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecords;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsEmailNotificationRecords entity
 * This controller exposes four specific endpoints:
 * 1. POST /emailnotificationrecord - for creating email notification records
 * 2. POST /emailnotificationrecordlist - for listing email notification records
 * 3. POST /emailnotificationrecordpatch/{noticeNo}/{emailAddr} - for updating email notification records
 * 4. POST /emailnotificationrecordexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsEmailNotificationRecordsController {
    
    private final OcmsEmailNotificationRecordsService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsEmailNotificationRecordsController(OcmsEmailNotificationRecordsService service) {
        this.service = service;
        log.info("email notification records controller initialized");
    }
    
    /**
     * POST endpoint for listing email notification records
     */
    @PostMapping("/emailnotificationrecordlist")
    public ResponseEntity<FindAllResponse<OcmsEmailNotificationRecords>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsEmailNotificationRecords> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if an email notification record exists by noticeNo and emailAddr
     * @param request Map containing noticeNo and emailAddr
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/emailnotificationrecordexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String noticeNo = request.get("noticeNo");
            String processingStage = request.get("processingStage");
            
            if (noticeNo == null || processingStage == null) {
                throw new IllegalArgumentException("noticeNo and processingStage are required");
            }
            
            // Create a composite key object
            OcmsEmailNotificationRecordsId recordId = new OcmsEmailNotificationRecordsId(noticeNo, processingStage);
            
            // Check if record exists by trying to get it
            Optional<OcmsEmailNotificationRecords> record = service.getById(recordId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking email notification record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
