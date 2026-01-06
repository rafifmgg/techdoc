package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.SuspensionProcessingHelper;
// import com.ocmsintranet.apiservice.workflows.notice_management.suspension.revivenotice.helpers.ReviveProcessingHelper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for SuspendedNotice entity
 * This controller exposes three specific endpoints:
 * 1. POST /suspendednotice - for creating suspended notices
 * 2. POST /suspendednoticelist - for listing suspended notices
 * 3. POST /suspendednoticepatch/{id1}/{id2}/{id3} - for updating suspended notices
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class SuspendedNoticeController {
    
    private final SuspendedNoticeService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SuspensionProcessingHelper suspensionProcessingHelper;

    @Autowired
    // private ReviveProcessingHelper reviveProcessingHelper;

    public SuspendedNoticeController(SuspendedNoticeService service) {
        this.service = service;
        log.info("suspended notice controller initialized");
    }
    
    /**
     * POST endpoint for listing suspended notices
     */
    @PostMapping("/suspendednoticelist")
    public ResponseEntity<FindAllResponse<SuspendedNotice>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<SuspendedNotice> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint to check if a suspended notice exists by ID components
     * @param request Map containing noticeNo, dateOfSuspension, and srNo
     * @return ResponseEntity with boolean indicating if suspended notice exists
     */
    @PostMapping("/suspendednoticeexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String id1 = request.get("noticeNo");
            String id2 = request.get("dateOfSuspension");
            String id3 = request.get("srNo");
            
            if (id1 == null || id2 == null || id3 == null) {
                throw new IllegalArgumentException("noticeNo, dateOfSuspension, and srNo are required");
            }
            
            // Create a SuspendedNoticeId object for the composite key
            SuspendedNoticeId suspendedNoticeId = new SuspendedNoticeId(
                id1, 
                java.time.LocalDateTime.parse(id2), 
                Integer.parseInt(id3)
            );
            
            // Check if suspended notice exists by trying to get it
            Optional<SuspendedNotice> suspendedNotice = service.getById(suspendedNoticeId);
            boolean exists = suspendedNotice.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking suspended notice existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

}
