package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/eocmsusermessage")
public class EocmsUserMessageController extends BaseController<EocmsUserMessage, String, EocmsUserMessageService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsUserMessageController(EocmsUserMessageService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing user message records
     * Replaces the GET endpoint
     */
    @PostMapping("/usermessagelist")
    public ResponseEntity<FindAllResponse<EocmsUserMessage>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
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
        
        FindAllResponse<EocmsUserMessage> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a user message record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/usermessagepatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the ID from the payload
            String errorCode = (String) payload.get("errorCode");
            
            if (errorCode == null) {
                throw new IllegalArgumentException("errorCode is required");
            }
            
            // Convert payload to entity
            EocmsUserMessage entity = objectMapper.convertValue(payload, EocmsUserMessage.class);
            
            // Call the patch method from service
            service.patch(errorCode, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating user message: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
