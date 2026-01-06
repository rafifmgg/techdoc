package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHoliday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsHoliday entity
 * This controller exposes four specific endpoints:
 * 1. POST /holiday - for creating holiday records
 * 2. POST /holidaylist - for listing holiday records
 * 3. POST /holidaypatch/{holidayDate} - for updating holiday records
 * 4. POST /holidayexists - for checking if a record exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsHolidayController {
    
    private final OcmsHolidayService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsHolidayController(OcmsHolidayService service) {
        this.service = service;
        log.info("Holiday controller initialized");
    }
    
    /**
     * POST endpoint for creating a holiday record
     */
    @PostMapping("/holiday")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OcmsHoliday> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OcmsHoliday.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OcmsHoliday entity = objectMapper.convertValue(payload, OcmsHoliday.class);
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
            responseData.setMessage("Error creating holiday record: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing holiday records
     */
    @PostMapping("/holidaylist")
    public ResponseEntity<FindAllResponse<OcmsHoliday>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsHoliday> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a holiday record by ID
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/holidaypatch/{holidayDate}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime holidayDate,
            @RequestBody Object payload) {
        try {
            // Convert payload to OcmsHoliday entity
            OcmsHoliday record = objectMapper.convertValue(payload, OcmsHoliday.class);
            
            // Set the ID field from path variable
            record.setHolidayDate(holidayDate);
            
            // Call the patch method from service
            service.patch(holidayDate, record);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating holiday record: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if a holiday record exists by holidayDate
     * @param request Map containing holidayDate
     * @return ResponseEntity with boolean indicating if record exists
     */
    @PostMapping("/holidayexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, Object> request) {
        try {
            // The holidayDate needs to be converted from the request format to LocalDateTime
            LocalDateTime holidayDate = null;
            Object holidayDateObj = request.get("holidayDate");
            if (holidayDateObj instanceof String) {
                // Parse the date string to LocalDateTime
                holidayDate = LocalDateTime.parse((String) holidayDateObj);
            }
            
            if (holidayDate == null) {
                throw new IllegalArgumentException("holidayDate is required");
            }
            
            // Check if record exists by trying to get it
            Optional<OcmsHoliday> record = service.getById(holidayDate);
            boolean exists = record.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking holiday record existence: " + e.getMessage());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
