package com.ocmsintranet.apiservice.crud.ocmsizdb.parameter;

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
 * Controller for Parameter entity
 * This controller only exposes three specific endpoints:
 * 1. POST /parameter - for creating parameters
 * 2. POST /parameterlist - for listing parameters
 * 3. POST /parameterpatch/{id1}/{id2} - for updating parameters
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class ParameterController {
    
    private final ParameterService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public ParameterController(ParameterService service) {
        this.service = service;
        log.info("parameter controller initialized");
    }
    
    /**
     * POST endpoint for creating a parameter
     */
    @PostMapping("/parameter")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<Parameter> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Parameter.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                Parameter entity = objectMapper.convertValue(payload, Parameter.class);
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
            responseData.setMessage("Error creating parameter: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing parameters
     */
    @PostMapping("/parameterlist")
    public ResponseEntity<FindAllResponse<Parameter>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<Parameter> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a parameter by composite key
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/parameterpatch/{id1}/{id2}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @RequestBody Object payload) {
        try {
            // Convert payload to Parameter entity
            Parameter parameter = objectMapper.convertValue(payload, Parameter.class);
            
            // Set the composite key fields from path variables
            parameter.setParameterId(id1);
            parameter.setCode(id2);
            
            // Create a ParameterId object for the composite key
            ParameterId parameterId = new ParameterId(id1, id2);
            
            // Call the patch method from service
            service.patch(parameterId, parameter);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating parameter: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if a parameter exists by ID and code
     * @param request Map containing parameterId and code
     * @return ResponseEntity with boolean indicating if parameter exists
     */
    @PostMapping("/parameterexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String id1 = request.get("parameterId");
            String id2 = request.get("code");
            
            if (id1 == null || id2 == null) {
                throw new IllegalArgumentException("parameterId and code are required");
            }
            
            // Create a ParameterId object for the composite key
            ParameterId parameterId = new ParameterId(id1, id2);
            
            // Check if parameter exists by trying to get it
            Optional<Parameter> parameter = service.getById(parameterId);
            boolean exists = parameter.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking parameter existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
