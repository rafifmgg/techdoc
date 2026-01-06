package com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for SuspensionReason entity
 * This controller only exposes five specific endpoints:
 * 1. POST /suspensionreason - for creating suspension reasons
 * 2. POST /suspensionreasonlist - for listing suspension reasons
 * 3. POST /suspensionreasonpatch/{id1}/{id2} - for updating suspension reasons
 * 4. POST /suspensionreasonexists - for checking if a suspension reason exists
 * 5. POST /suspensionreasonreference - for getting reference codes for dropdown
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class SuspensionReasonController {

    private final SuspensionReasonService service;
    private final SuspensionReasonRepository repository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public SuspensionReasonController(SuspensionReasonService service, SuspensionReasonRepository repository) {
        this.service = service;
        this.repository = repository;
    }
    
    /**
     * POST endpoint to check if a suspension reason exists by suspension type and reason of suspension
     */
    @PostMapping("/suspensionreasonexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, Object> payload) {
        try {
            // Extract suspension type and reason from payload
            String suspensionType = (String) payload.get("suspensionType");
            String reasonOfSuspension = (String) payload.get("reasonOfSuspension");
            
            if (suspensionType == null || reasonOfSuspension == null) {
                return new ResponseEntity<>("Missing required fields", HttpStatus.BAD_REQUEST);
            }
            
            SuspensionReasonId id = new SuspensionReasonId(suspensionType, reasonOfSuspension);
            Optional<SuspensionReason> result = service.getById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", result.isPresent());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * POST endpoint for creating a suspension reason
     */
    @PostMapping("/suspensionreason")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<SuspensionReason> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SuspensionReason.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                SuspensionReason entity = objectMapper.convertValue(payload, SuspensionReason.class);
                service.save(entity);
            }
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing suspension reasons
     */
    @PostMapping("/suspensionreasonlist")
    public ResponseEntity<FindAllResponse<SuspensionReason>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        // Log the normalized parameters for debugging
        log.debug("Normalized params for suspensionreasonlist: {}", normalizedParams);
        
        FindAllResponse<SuspensionReason> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a suspension reason by composite key
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/suspensionreasonpatch/{id1}/{id2}")
    public ResponseEntity<?> patch(
            @PathVariable("id1") String id1,
            @PathVariable("id2") String id2,
            @RequestBody SuspensionReason suspensionReason) {
        
        try {
            // Create composite key from path variables
            SuspensionReasonId suspensionReasonId = new SuspensionReasonId(id1, id2);
            
            // Get existing entity
            Optional<SuspensionReason> existingOpt = service.getById(suspensionReasonId);
            
            if (existingOpt.isPresent()) {
                SuspensionReason existing = existingOpt.get();
                
                // Update only non-null fields from the entity
                if (suspensionReason.getDescription() != null) {
                    existing.setDescription(suspensionReason.getDescription());
                }
                
                if (suspensionReason.getNoOfDaysForRevival() != null) {
                    existing.setNoOfDaysForRevival(suspensionReason.getNoOfDaysForRevival());
                }
                
                if (suspensionReason.getStatus() != null) {
                    existing.setStatus(suspensionReason.getStatus());
                }
                
                if (suspensionReason.getUpdUserId() != null) {
                    existing.setUpdUserId(suspensionReason.getUpdUserId());
                }
                
                // Save and return the updated entity
                service.save(existing);
                
                // Return success response
                CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                throw new RuntimeException("SuspensionReason not found with id: " + suspensionReasonId);
            }
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to get reference codes for dropdown
     */
    @PostMapping("/suspensionreasonreference")
    public ResponseEntity<?> getReferenceCodes() {
        try {
            // Use repository directly
            List<String> referenceCodes = repository.findDistinctSuspensionTypes();
            return new ResponseEntity<>(referenceCodes, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}