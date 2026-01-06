package com.ocmsintranet.apiservice.crud.ocmsizdb.standardcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for StandardCode entity
 * This controller only exposes five specific endpoints:
 * 1. POST /standardcode - for creating standard codes
 * 2. POST /standardcodelist - for listing standard codes
 * 3. POST /standardcodepatch/{id1}/{id2} - for updating standard codes
 * 4. POST /standardcodeexists - for checking if a standard code exists
 * 5. POST /standardcodereference - for getting reference codes for dropdown
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class StandardCodeController {
    
    private final StandardCodeService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public StandardCodeController(StandardCodeService service) {
        this.service = service;
        log.info("standard code controller initialized");
    }
    
    /**
     * POST endpoint for creating a standard code
     */
    @PostMapping("/standardcode")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<StandardCode> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StandardCode.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                StandardCode entity = objectMapper.convertValue(payload, StandardCode.class);
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
            responseData.setMessage("Error creating standard code: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing standard codes
     */
    @PostMapping("/standardcodelist")
    public ResponseEntity<FindAllResponse<StandardCode>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<StandardCode> response = service.getAll(normalizedParams);        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a standard code by composite key
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/standardcodepatch/{id1}/{id2}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @RequestBody Object payload) {
        try {
            // Convert payload to StandardCode entity
            StandardCode standardCode = objectMapper.convertValue(payload, StandardCode.class);
            
            // Set the composite key fields from path variables
            standardCode.setReferenceCode(id1);
            standardCode.setCode(id2);
            
            // Create a StandardCodeId object for the composite key
            StandardCodeId standardCodeId = new StandardCodeId(id1, id2);
            
            // Call the patch method from service
            service.patch(standardCodeId, standardCode);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating standard code: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if a standard code exists by reference code and code
     */
    @PostMapping("/standardcodeexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> payload) {
        try {
            String referenceCode = payload.get("referenceCode");
            String code = payload.get("code");
            
            if (referenceCode == null || code == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "referenceCode and code are required");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            
            // Create a StandardCodeId object for the composite key
            StandardCodeId standardCodeId = new StandardCodeId(referenceCode, code);
            
            // Check if standard code exists by trying to get it
            Optional<StandardCode> standardCode = service.getById(standardCodeId);
            boolean exists = standardCode.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking standard code existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to get reference codes for dropdown
     */
    @PostMapping("/standardcodereference")
    public ResponseEntity<?> getReferenceCodes() {
        try {
            // Get all standard codes from the repository
            Map<String, String[]> params = new HashMap<>();
            params.put("$limit", new String[]{"9999"});
            params.put("codeStatus", new String[]{"A"});
            FindAllResponse<StandardCode> findResponse = service.getAll(params);
            List<StandardCode> allStandardCodes = findResponse.getData();
            
            // Extract distinct reference codes
            List<String> distinctReferenceCodes = allStandardCodes.stream()
                    .map(StandardCode::getReferenceCode)
                    .distinct()
                    .collect(Collectors.toList());
            
            // Convert to the format needed for dropdown
            List<Map<String, Object>> result = new ArrayList<>();
            for (String referenceCode : distinctReferenceCodes) {
                Map<String, Object> item = new HashMap<>();
                item.put("referenceCode", referenceCode);
                item.put("description", referenceCode.replace("_", " "));
                result.add(item);
            }
            
            // Create response map
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("data", result);
            
            return new ResponseEntity<>(responseMap, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error getting reference codes: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}