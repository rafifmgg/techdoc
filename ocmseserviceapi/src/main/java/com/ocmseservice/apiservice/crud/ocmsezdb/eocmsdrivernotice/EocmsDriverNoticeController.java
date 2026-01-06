package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsdrivernotice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import com.ocmseservice.apiservice.crud.beans.SingleResponse;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/eocmsdrivernotice")
public class EocmsDriverNoticeController extends BaseController<EocmsDriverNotice, EocmsDriverNoticeId, EocmsDriverNoticeService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsDriverNoticeController(EocmsDriverNoticeService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing driver notice records
     * Replaces the GET endpoint
     * 
     * @param requestBody The request parameters
     * @return ResponseEntity containing either SingleResponse or FindAllResponse depending on query parameters and result size
     */
    @PostMapping("/drivernoticelist")
    public ResponseEntity<?> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Create normalized parameters map
        Map<String, String[]> normalizedParams = normalizeRequestBody(requestBody);
        
        // Get response from service
        FindAllResponse<EocmsDriverNotice> response = service.getAll(normalizedParams);
        
        // Check for ID parameters
        boolean hasIdParams = checkForIdParameters(normalizedParams);
        
        // If querying by ID and result size is 1, return SingleResponse
        if (hasIdParams && response.getData() != null && response.getData().size() == 1) {
            return createSingleResponse(response.getData().get(0));
        }
        
        // Otherwise return FindAllResponse
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to normalize request body
     */
    private Map<String, String[]> normalizeRequestBody(Map<String, Object> requestBody) {
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
        
        return normalizedParams;
    }
    
    /**
     * Helper method to check if request contains ID parameters
     */
    private boolean checkForIdParameters(Map<String, String[]> params) {
        return params.containsKey("dateOfProcessing") || params.containsKey("noticeNo");
    }
    
    /**
     * Helper method to create a SingleResponse
     */
    private ResponseEntity<?> createSingleResponse(EocmsDriverNotice item) {
       List<EocmsDriverNotice> itemList = List.of(item);
         SingleResponse<List<EocmsDriverNotice>> response = SingleResponse.createPaginatedResponse(itemList, 0, 0, 0);
        return ResponseEntity.ok(response);
    }
    
    
    /**
     * POST endpoint for updating a driver notice record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/drivernoticepatch")
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
            EocmsDriverNoticeId id = new EocmsDriverNoticeId(dateOfProcessing, noticeNo);
            
            // Remove ID fields from the payload to avoid conflicts
            Map<String, Object> entityData = new HashMap<>(payload);
            
            // Convert payload to entity
            EocmsDriverNotice entity = objectMapper.convertValue(entityData, EocmsDriverNotice.class);
            
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
                "Error updating driver notice: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
