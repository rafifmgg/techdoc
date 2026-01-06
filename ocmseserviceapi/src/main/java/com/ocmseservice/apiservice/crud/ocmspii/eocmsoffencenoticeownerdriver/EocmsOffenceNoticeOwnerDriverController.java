package com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver;

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
@RequestMapping("/v1/eocmsoffencenoticeownerdriver")
public class EocmsOffenceNoticeOwnerDriverController extends BaseController<EocmsOffenceNoticeOwnerDriver, EocmsOffenceNoticeOwnerDriverId, EocmsOffenceNoticeOwnerDriverService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsOffenceNoticeOwnerDriverController(EocmsOffenceNoticeOwnerDriverService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing offence notice owner driver records
     * Replaces the GET endpoint
     */
    @PostMapping("/offencenoticeownerdriverlist")
    public ResponseEntity<FindAllResponse<EocmsOffenceNoticeOwnerDriver>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
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
        
        FindAllResponse<EocmsOffenceNoticeOwnerDriver> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating an offence notice owner driver record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/offencenoticeownerdriverpatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the composite ID components from the payload
            String noticeNo = (String) payload.get("noticeNo");
            String ownerDriverIndicator = (String) payload.get("ownerDriverIndicator");
            
            if (noticeNo == null || ownerDriverIndicator == null) {
                throw new IllegalArgumentException("noticeNo and ownerDriverIndicator are required");
            }
            
            // Create the composite ID
            EocmsOffenceNoticeOwnerDriverId id = new EocmsOffenceNoticeOwnerDriverId(noticeNo, ownerDriverIndicator);
            
            // Remove ID fields from the payload to avoid conflicts
            Map<String, Object> entityData = new HashMap<>(payload);
            
            // Convert payload to entity
            EocmsOffenceNoticeOwnerDriver entity = objectMapper.convertValue(entityData, EocmsOffenceNoticeOwnerDriver.class);
            
            // Set the ID fields
            entity.setNoticeNo(noticeNo);
            entity.setOwnerDriverIndicator(ownerDriverIndicator);
            
            // Call the patch method from service
            service.patch(id, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating offence notice owner driver: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
