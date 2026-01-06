package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusion;

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
 * Controller for EnotificationExclusion entity
 * This controller exposes three specific endpoints:
 * 1. POST /enotificationexclusion - for creating exclusion entries
 * 2. POST /enotificationexclusionlist - for listing exclusion entries
 * 3. POST /enotificationexclusionpatch/{id} - for updating exclusion entries
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class EnotificationExclusionController {
    
    private final EnotificationExclusionService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public EnotificationExclusionController(EnotificationExclusionService service) {
        this.service = service;
        log.info("enotification exclusion controller initialized");
    }
    
    /**
     * POST endpoint for creating an exclusion entry
     */
    @PostMapping("/enotificationexclusion")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<EnotificationExclusion> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EnotificationExclusion.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                EnotificationExclusion entity = objectMapper.convertValue(payload, EnotificationExclusion.class);
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
            responseData.setMessage("Error creating exclusion entry: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing exclusion entries
     */
    @PostMapping("/enotificationexclusionlist")
    public ResponseEntity<FindAllResponse<EnotificationExclusion>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<EnotificationExclusion> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating an exclusion entry by ID
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/enotificationexclusionpatch/{id}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String id,
            @RequestBody Object payload) {
        try {
            // Convert payload to entity
            EnotificationExclusion entity = objectMapper.convertValue(payload, EnotificationExclusion.class);
                        
            // Call the patch method from service
            service.patch(id, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating exclusion entry: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if an exclusion entry exists by ID
     * @param request Map containing idNo
     * @return ResponseEntity with boolean indicating if entry exists
     */
    @PostMapping("/enotificationexclusionexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String idNo = request.get("idNo");
            
            if (idNo == null) {
                throw new IllegalArgumentException("idNo is required");
            }
                        
            // Check if entity exists by trying to get it
            Optional<EnotificationExclusion> entity = service.getById(idNo);
            boolean exists = entity.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking exclusion entry existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for deleting an exclusion entry by ID
     */
    @PostMapping("/enotificationexclusiondelete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            // Call the delete method from service
            boolean deleted = service.delete(id);
            
            if (deleted) {
                // Return success response
                CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.DELETE_SUCCESS);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                // Return not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                responseData.setMessage("Exclusion entry not found with ID: " + id);
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error deleting exclusion entry: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
