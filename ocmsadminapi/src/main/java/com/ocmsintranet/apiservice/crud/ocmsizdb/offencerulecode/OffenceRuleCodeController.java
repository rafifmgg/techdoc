package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OffenceRuleCode entity
 * This controller only exposes four specific endpoints:
 * 1. POST /offencerulecode - for creating computer rule codes
 * 2. POST /offencerulecodelist - for listing computer rule codes
 * 3. POST /offencerulecodepatch/{computerRuleCode}/{effectiveStartDate}/{vehicleCategory} - for updating computer rule codes
 * 4. GET /offencerulecodeexists/{computerRuleCode}/{effectiveStartDate}/{vehicleCategory} - for checking if a computer rule code exists
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OffenceRuleCodeController {
    
    private final OffenceRuleCodeService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OffenceRuleCodeController(OffenceRuleCodeService service) {
        this.service = service;
        log.info("offence rule code controller initialized");
    }
    
    /**
     * POST endpoint for creating an offence rule code
     */
    @PostMapping("/offencerulecode")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OffenceRuleCode> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OffenceRuleCode.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OffenceRuleCode entity = objectMapper.convertValue(payload, OffenceRuleCode.class);
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
            responseData.setMessage("Error creating offence rule code: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing offence rule codes
     */
    @PostMapping("/offencerulecodelist")
    public ResponseEntity<FindAllResponse<OffenceRuleCode>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OffenceRuleCode> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating an offence rule code by composite key
     * This is a custom endpoint that uses the patch method from service
     */
    @PostMapping("/offencerulecodepatch/{computerRuleCode}/{effectiveStartDate}/{vehicleCategory}/{compositionAmount}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable Integer computerRuleCode,
            @PathVariable LocalDateTime effectiveStartDate,
            @PathVariable String vehicleCategory,
            @PathVariable BigDecimal compositionAmount,
            @RequestBody Object payload) {
        try {
            // Convert payload to OffenceRuleCode entity
            OffenceRuleCode offenceRuleCode = objectMapper.convertValue(payload, OffenceRuleCode.class);
            
            log.info("offenceRuleCode: {}", payload.toString());
            log.info("computerRuleCode: {}", computerRuleCode);
            log.info("effectiveStartDate: {}", effectiveStartDate);
            log.info("vehicleCategory: {}", vehicleCategory);
            log.info("compositionAmount: {}", offenceRuleCode.getCompositionAmount());
            
            // Set the composite key fields from path variables
            offenceRuleCode.setComputerRuleCode(computerRuleCode);
            offenceRuleCode.setEffectiveStartDate(effectiveStartDate);
            offenceRuleCode.setVehicleCategory(vehicleCategory);
            offenceRuleCode.setCompositionAmount(compositionAmount);
            
            // Create a OffenceRuleCodeId object for the composite key
            OffenceRuleCodeId id = new OffenceRuleCodeId(computerRuleCode, effectiveStartDate, vehicleCategory, compositionAmount);
            
            // Call the patch method from service
            service.patch(id, offenceRuleCode);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating offence rule code: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint to check if an offence rule code exists by composite key
     * @param payload The request containing computerRuleCode, effectiveStartDate, and vehicleCategory
     * @return ResponseEntity with exists flag
     */
    @PostMapping("/offencerulecodeexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, Object> payload) {
        try {
            // Log the received payload for debugging
            log.debug("Received payload for exists check: {}", payload);
            
            // Extract fields from payload
            Integer computerRuleCode = null;
            if (payload.get("computerRuleCode") instanceof Integer) {
                computerRuleCode = (Integer) payload.get("computerRuleCode");
            } else if (payload.get("computerRuleCode") instanceof String) {
                computerRuleCode = Integer.parseInt((String) payload.get("computerRuleCode"));
            } else if (payload.get("computerRuleCode") != null) {
                // Handle other potential types
                computerRuleCode = Integer.parseInt(payload.get("computerRuleCode").toString());
            }
            
            LocalDateTime effectiveStartDate = null;
            try {
                if (payload.get("effectiveStartDate") instanceof String) {
                    String dateStr = (String) payload.get("effectiveStartDate");
                    log.debug("Parsing date string: {}", dateStr);
                    
                    // Try with DateTimeFormatter for more robust parsing
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    effectiveStartDate = LocalDateTime.parse(dateStr, formatter);
                }
            } catch (DateTimeParseException e) {
                log.error("Date parsing error: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid date format: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            
            String vehicleCategory = (String) payload.get("vehicleCategory");

            BigDecimal compositionAmount = null;
            if (payload.get("compositionAmount") instanceof String) {
                compositionAmount = new BigDecimal((String) payload.get("compositionAmount"));
            } else if (payload.get("compositionAmount") instanceof Number) {
                compositionAmount = BigDecimal.valueOf(((Number) payload.get("compositionAmount")).doubleValue());
            } else if (payload.get("compositionAmount") != null) {
                // Handle other potential types
                compositionAmount = new BigDecimal(payload.get("compositionAmount").toString());
            }
            
            // Validate all required fields
            if (computerRuleCode == null) {
                return new ResponseEntity<>(Map.of("error", "Missing or invalid computerRuleCode"), HttpStatus.BAD_REQUEST);
            }
            if (effectiveStartDate == null) {
                return new ResponseEntity<>(Map.of("error", "Missing or invalid effectiveStartDate"), HttpStatus.BAD_REQUEST);
            }
            if (vehicleCategory == null) {
                return new ResponseEntity<>(Map.of("error", "Missing vehicleCategory"), HttpStatus.BAD_REQUEST);
            }
            if (compositionAmount == null) {
                return new ResponseEntity<>(Map.of("error", "Missing compositionAmount"), HttpStatus.BAD_REQUEST);
            }
            
            // Create a OffenceRuleCodeId object for the composite key
            OffenceRuleCodeId id = new OffenceRuleCodeId(computerRuleCode, effectiveStartDate, vehicleCategory, compositionAmount);
            log.debug("Created composite key: {}, {}, {}", 
                    id.getComputerRuleCode(), id.getEffectiveStartDate(), id.getVehicleCategory());
            
            // Check if offence rule code exists by trying to get it
            Optional<OffenceRuleCode> offenceRuleCode = service.getById(id);
            boolean exists = offenceRuleCode.isPresent();
            
            // Create response map
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NumberFormatException e) {
            // Handle number parsing errors specifically
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid computerRuleCode format: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Log the full stack trace for debugging
            e.printStackTrace();
            
            // Return detailed error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking offence rule code existence: " + e.getMessage());
            errorResponse.put("errorType", e.getClass().getName());
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}