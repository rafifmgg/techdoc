package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionAudit;

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
 * Controller for OcmsWebTransactionAudit entity
 * This controller exposes four specific endpoints:
 * 1. POST /webtransactionaudit - for creating web transaction audit records
 * 2. POST /webtransactionauditlist - for listing web transaction audit records
 * 3. POST /webtransactionauditpatch/{webTxnId} - for updating web transaction audit records
 * 4. POST /webtransactionauditexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsWebTransactionAuditController {
    
    private final OcmsWebTransactionAuditService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsWebTransactionAuditController(OcmsWebTransactionAuditService service) {
        this.service = service;
        log.info("Web transaction audit controller initialized");
    }
    
    /**
     * POST endpoint for creating a web transaction audit record
     */
    @PostMapping("/webtransactionaudit")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OcmsWebTransactionAudit> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OcmsWebTransactionAudit.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OcmsWebTransactionAudit entity = objectMapper.convertValue(payload, OcmsWebTransactionAudit.class);
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
            responseData.setMessage("Error creating web transaction audit record: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing web transaction audit records
     */
    @PostMapping("/webtransactionauditlist")
    public ResponseEntity<FindAllResponse<OcmsWebTransactionAudit>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsWebTransactionAudit> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a web transaction audit record by ID
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/webtransactionauditpatch/{webTxnId}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String webTxnId,
            @RequestBody Object payload) {
        try {
            // Convert payload to OcmsWebTransactionAudit entity
            OcmsWebTransactionAudit record = objectMapper.convertValue(payload, OcmsWebTransactionAudit.class);
            
            // Set the ID field from path variable
            record.setWebTxnId(webTxnId);
            
            // Call the patch method from service
            service.patch(webTxnId, record);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating web transaction audit record: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if a web transaction audit record exists by webTxnId
     * @param request Map containing webTxnId
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/webtransactionauditexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String webTxnId = request.get("webTxnId");
            
            if (webTxnId == null) {
                throw new IllegalArgumentException("webTxnId is required");
            }
            
            // Check if record exists by trying to get it
            Optional<OcmsWebTransactionAudit> record = service.getById(webTxnId);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking web transaction audit record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
