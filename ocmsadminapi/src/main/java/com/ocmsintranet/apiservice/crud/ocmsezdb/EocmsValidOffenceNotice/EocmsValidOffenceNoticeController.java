package com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for EocmsValidOffenceNotice entity (Internet System)
 * This controller exposes endpoints for internet-based offence notice operations
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class EocmsValidOffenceNoticeController {
    
    private final EocmsValidOffenceNoticeService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public EocmsValidOffenceNoticeController(EocmsValidOffenceNoticeService service) {
        this.service = service;
        log.info("EocmsValidOffenceNotice controller initialized");
    }
    
    /**
     * POST endpoint for creating an internet offence notice
     */
    @PostMapping("/internetoffencenotice")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<EocmsValidOffenceNotice> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EocmsValidOffenceNotice.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                EocmsValidOffenceNotice entity = objectMapper.convertValue(payload, EocmsValidOffenceNotice.class);
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
            responseData.setMessage("Error creating internet offence notice: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing internet offence notices
     */
    @PostMapping("/internetoffencenoticelist")
    public ResponseEntity<FindAllResponse<EocmsValidOffenceNotice>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<EocmsValidOffenceNotice> response = service.getAllWithFilters(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating an internet offence notice
     */
    @PostMapping("/internetoffencenoticepatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the ID from the payload
            String noticeNo = (String) payload.get("noticeNo");
            if (noticeNo == null) {
                throw new IllegalArgumentException("noticeNo is required");
            }
            
            // Remove the ID from the payload to avoid conflicts
            Map<String, Object> entityData = new HashMap<>(payload);
            entityData.remove("noticeNo");
            
            // Convert payload to entity
            EocmsValidOffenceNotice entity = objectMapper.convertValue(entityData, EocmsValidOffenceNotice.class);
            
            // Call the patch method from service
            service.patch(noticeNo, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating internet offence notice: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * GET endpoint to find notices by vehicle number
     */
    @GetMapping("/internetoffencenotice/vehicle/{vehicleNo}")
    public ResponseEntity<List<EocmsValidOffenceNotice>> findByVehicleNo(@PathVariable String vehicleNo) {
        try {
            List<EocmsValidOffenceNotice> notices = service.findByVehicleNo(vehicleNo);
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            log.error("Error finding notices by vehicle number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET endpoint to find notices by payment status
     */
    @GetMapping("/internetoffencenotice/payment-status/{paymentStatus}")
    public ResponseEntity<List<EocmsValidOffenceNotice>> findByPaymentStatus(@PathVariable String paymentStatus) {
        try {
            List<EocmsValidOffenceNotice> notices = service.findByPaymentStatus(paymentStatus);
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            log.error("Error finding notices by payment status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET endpoint to find notices by processing stage
     */
    @GetMapping("/internetoffencenotice/processing-stage/{processingStage}")
    public ResponseEntity<List<EocmsValidOffenceNotice>> findByProcessingStage(@PathVariable String processingStage) {
        try {
            List<EocmsValidOffenceNotice> notices = service.findByLastProcessingStage(processingStage);
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            log.error("Error finding notices by processing stage: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET endpoint to find notices within date range
     */
    @GetMapping("/internetoffencenotice/date-range")
    public ResponseEntity<List<EocmsValidOffenceNotice>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<EocmsValidOffenceNotice> notices = service.findByDateRange(startDate, endDate);
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            log.error("Error finding notices by date range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET endpoint to find unpaid notices
     */
    @GetMapping("/internetoffencenotice/unpaid")
    public ResponseEntity<List<EocmsValidOffenceNotice>> findUnpaidNotices() {
        try {
            List<EocmsValidOffenceNotice> notices = service.findUnpaidNotices();
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            log.error("Error finding unpaid notices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}