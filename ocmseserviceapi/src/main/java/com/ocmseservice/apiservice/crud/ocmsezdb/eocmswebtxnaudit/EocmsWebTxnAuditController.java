package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/eocmswebtxnaudit")
public class EocmsWebTxnAuditController extends BaseController<EocmsWebTxnAudit, String, EocmsWebTxnAuditService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsWebTxnAuditController(EocmsWebTxnAuditService service) {
        super(service);
    }
    
    /**
     * Override the default create method to handle time conversion
     */
    @Override
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            // Extract the ID from the payload
            String webTxnId = (String) ((Map<String, Object>) payload).get("webTxnId");
            
            if (webTxnId == null) {
                throw new IllegalArgumentException("webTxnId is required");
            }
            
            // Create a new entity and set its properties manually
            EocmsWebTxnAudit entity = new EocmsWebTxnAudit();
            entity.setWebTxnId(webTxnId);
            
            // Set other properties from the payload
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            
            if (payloadMap.containsKey("msgError")) {
                entity.setMsgError((String) payloadMap.get("msgError"));
            }
            
            if (payloadMap.containsKey("recordCounter")) {
                Object recordCounter = payloadMap.get("recordCounter");
                if (recordCounter instanceof Number) {
                    entity.setRecordCounter(((Number) recordCounter).intValue());
                } else if (recordCounter instanceof String) {
                    entity.setRecordCounter(Integer.parseInt((String) recordCounter));
                }
            }
            
            if (payloadMap.containsKey("sendDate")) {
                Object sendDate = payloadMap.get("sendDate");
                if (sendDate instanceof String) {
                    entity.setSendDate(LocalDateTime.parse((String) sendDate, DateTimeFormatter.ISO_DATE_TIME));
                }
            }
            
            if (payloadMap.containsKey("sendTime")) {
                Object sendTime = payloadMap.get("sendTime");
                if (sendTime instanceof String) {
                    entity.setSendTime(LocalTime.parse((String) sendTime));
                }
            }
            
            if (payloadMap.containsKey("sender")) {
                entity.setSender((String) payloadMap.get("sender"));
            }
            
            if (payloadMap.containsKey("statusNum")) {
                entity.setStatusNum((String) payloadMap.get("statusNum"));
            }
            
            if (payloadMap.containsKey("targetReceiver")) {
                entity.setTargetReceiver((String) payloadMap.get("targetReceiver"));
            }
            
            if (payloadMap.containsKey("txnDetail")) {
                entity.setTxnDetail((String) payloadMap.get("txnDetail"));
            }
            
            if (payloadMap.containsKey("creUserId")) {
                entity.setCreUserId((String) payloadMap.get("creUserId"));
            }
            
            if (payloadMap.containsKey("updUserId")) {
                entity.setUpdUserId((String) payloadMap.get("updUserId"));
            }
            
            // Call the save method from service
            service.save(entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error creating web transaction audit: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing web transaction audit records
     * Replaces the GET endpoint
     */
    @PostMapping("/webtxnauditlist")
    public ResponseEntity<FindAllResponse<EocmsWebTxnAudit>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service
        Map<String, String[]> normalizedParams = new HashMap<>();
        
        if (requestBody != null) {
            requestBody.forEach((key, value) -> {
                if (value instanceof String) {
                    normalizedParams.put(key, new String[] { (String) value });
                } else if (value instanceof Number) {
                    normalizedParams.put(key, new String[] { value.toString() });
                } else if (value instanceof Boolean) {
                    normalizedParams.put(key, new String[] { value.toString() });
                } else if (value instanceof String[]) {
                    normalizedParams.put(key, (String[]) value);
                }
            });
        }
        
        FindAllResponse<EocmsWebTxnAudit> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a web transaction audit record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/webtxnauditpatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the ID from the payload
            String webTxnId = (String) payload.get("webTxnId");
            
            if (webTxnId == null) {
                throw new IllegalArgumentException("webTxnId is required");
            }
            
            // Get existing entity
            Optional<EocmsWebTxnAudit> existingEntityOpt = service.getById(webTxnId);
            if (!existingEntityOpt.isPresent()) {
                throw new IllegalArgumentException("Web transaction audit with ID " + webTxnId + " not found");
            }
            
            EocmsWebTxnAudit existingEntity = existingEntityOpt.get();
            
            // Update properties from the payload
            if (payload.containsKey("msgError")) {
                existingEntity.setMsgError((String) payload.get("msgError"));
            }
            
            if (payload.containsKey("recordCounter")) {
                Object recordCounter = payload.get("recordCounter");
                if (recordCounter instanceof Number) {
                    existingEntity.setRecordCounter(((Number) recordCounter).intValue());
                } else if (recordCounter instanceof String) {
                    existingEntity.setRecordCounter(Integer.parseInt((String) recordCounter));
                }
            }
            
            if (payload.containsKey("sendDate")) {
                Object sendDate = payload.get("sendDate");
                if (sendDate instanceof String) {
                    existingEntity.setSendDate(LocalDateTime.parse((String) sendDate, DateTimeFormatter.ISO_DATE_TIME));
                }
            }
            
            if (payload.containsKey("sendTime")) {
                Object sendTime = payload.get("sendTime");
                if (sendTime instanceof String) {
                    existingEntity.setSendTime(LocalTime.parse((String) sendTime));
                }
            }
            
            if (payload.containsKey("sender")) {
                existingEntity.setSender((String) payload.get("sender"));
            }
            
            if (payload.containsKey("statusNum")) {
                existingEntity.setStatusNum((String) payload.get("statusNum"));
            }
            
            if (payload.containsKey("targetReceiver")) {
                existingEntity.setTargetReceiver((String) payload.get("targetReceiver"));
            }
            
            if (payload.containsKey("txnDetail")) {
                existingEntity.setTxnDetail((String) payload.get("txnDetail"));
            }
            
            if (payload.containsKey("updUserId")) {
                existingEntity.setUpdUserId((String) payload.get("updUserId"));
            }
            
            // Call the patch method from service
            service.patch(webTxnId, existingEntity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating web transaction audit: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
