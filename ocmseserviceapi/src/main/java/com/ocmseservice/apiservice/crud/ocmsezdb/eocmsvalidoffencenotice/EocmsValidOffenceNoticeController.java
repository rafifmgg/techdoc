package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice;

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
@RequestMapping("eocmsvalidoffencenotice/v1")
public class EocmsValidOffenceNoticeController extends BaseController<EocmsValidOffenceNotice, String, EocmsValidOffenceNoticeService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsValidOffenceNoticeController(EocmsValidOffenceNoticeService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing valid offence notice records
     * Replaces the GET endpoint
     */
    @PostMapping("/validoffencenoticelist")
    public ResponseEntity<FindAllResponse<EocmsValidOffenceNotice>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
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
        
        FindAllResponse<EocmsValidOffenceNotice> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a valid offence notice record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/validoffencenoticepatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the ID from the payload
            String noticeNo = (String) payload.get("noticeNo");
            
            if (noticeNo == null) {
                throw new IllegalArgumentException("noticeNo is required");
            }
            
            // Convert payload to entity
            EocmsValidOffenceNotice entity = objectMapper.convertValue(payload, EocmsValidOffenceNotice.class);
            
            // Call the patch method from service
            service.patch(noticeNo, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating valid offence notice: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    // The parkingFines endpoint has been moved to ParkingFinesController in the workflows.searchnoticetopay package
}
