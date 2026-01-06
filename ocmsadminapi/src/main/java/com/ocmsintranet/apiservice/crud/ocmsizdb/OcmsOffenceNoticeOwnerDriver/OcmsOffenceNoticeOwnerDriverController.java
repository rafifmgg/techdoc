package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;

/**
 * Controller for OcmsOffenceNoticeOwnerDriver entity
 * This controller exposes specific endpoints:
 * 1. POST /offencenoticeownerdriverlist - for listing offence notice owner drivers
 * 2. POST /offencenoticeownerdriverpatch - for updating offence notice owner drivers
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsOffenceNoticeOwnerDriverController {
    
    private final OcmsOffenceNoticeOwnerDriverService service;

    @Autowired
    private ObjectMapper objectMapper;

    public OcmsOffenceNoticeOwnerDriverController(OcmsOffenceNoticeOwnerDriverService service) {
        this.service = service;
        log.info("offencenoticeownerdriver controller initialized");
    }
    
/**
     * POST endpoint for creating a offencenoticeownerdriver
     */
    @PostMapping("/offencenoticeownerdriver")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OcmsOffenceNoticeOwnerDriver> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OcmsOffenceNoticeOwnerDriver.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OcmsOffenceNoticeOwnerDriver entity = objectMapper.convertValue(payload, OcmsOffenceNoticeOwnerDriver.class);
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
            responseData.setMessage("Error creating offencenoticeownerdriver: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint for listing offence notice owner drivers
     * Replaces the GET /offencenoticeownerdriver endpoint
     */
    @PostMapping("/offencenoticeownerdriverlist")
    public ResponseEntity<FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto> response = service.getAllWithAddresses(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for updating a offencenoticeownerdriver by composite key
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/offencenoticeownerdriverpatch/{id1}/{id2}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @RequestBody Object payload) {
        try {
            // Convert payload to Parameter entity
            OcmsOffenceNoticeOwnerDriver offencenoticeownerdriver = objectMapper.convertValue(payload, OcmsOffenceNoticeOwnerDriver.class);
            
            // Set the composite key fields from path variables
            offencenoticeownerdriver.setNoticeNo(id1);
            offencenoticeownerdriver.setOwnerDriverIndicator(id2);
            
            // Create a ParameterId object for the composite key
            OcmsOffenceNoticeOwnerDriverId offencenoticeownerdriverId = new OcmsOffenceNoticeOwnerDriverId(id1, id2);
            
            // Call the patch method from service
            service.patch(offencenoticeownerdriverId, offencenoticeownerdriver);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating offencenoticeownerdriver: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

}
