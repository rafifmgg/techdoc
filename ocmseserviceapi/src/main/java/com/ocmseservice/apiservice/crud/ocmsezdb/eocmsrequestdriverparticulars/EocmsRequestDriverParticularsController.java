package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/eocmsrequestdriverparticulars")
public class EocmsRequestDriverParticularsController extends BaseController<EocmsRequestDriverParticulars, EocmsRequestDriverParticularsId, EocmsRequestDriverParticularsService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsRequestDriverParticularsController(EocmsRequestDriverParticularsService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing request driver particulars records
     * Replaces the GET endpoint
     */
    @PostMapping("/requestdriverparticularslist")
    public ResponseEntity<FindAllResponse<EocmsRequestDriverParticulars>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
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
        
        FindAllResponse<EocmsRequestDriverParticulars> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a request driver particulars record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/requestdriverparticularspatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the composite ID components from the payload
            String dateOfProcessingStr = (String) payload.get("dateOfProcessing");
            String noticeNo = (String) payload.get("noticeNo");
            
            if (dateOfProcessingStr == null || noticeNo == null) {
                throw new IllegalArgumentException("dateOfProcessing and noticeNo are required");
            }
            
            // Parse the date string to LocalDateTime
            LocalDateTime dateOfProcessing = LocalDateTime.parse(dateOfProcessingStr, DateTimeFormatter.ISO_DATE_TIME);
            
            // Create the composite ID
            EocmsRequestDriverParticularsId id = new EocmsRequestDriverParticularsId(dateOfProcessing, noticeNo);
            
            // Remove ID fields from the payload to avoid conflicts
            Map<String, Object> entityData = new HashMap<>(payload);
            
            // Convert payload to entity
            EocmsRequestDriverParticulars entity = objectMapper.convertValue(entityData, EocmsRequestDriverParticulars.class);
            
            // Set the ID fields
            entity.setDateOfProcessing(dateOfProcessing);
            entity.setNoticeNo(noticeNo);
            
            // Call the patch method from service
            service.patch(id, entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating request driver particulars: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
