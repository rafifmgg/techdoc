package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import com.ocmseservice.apiservice.crud.beans.SingleResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/eocmswebtxndetail")
public class EocmsWebTxnDetailController extends BaseController<EocmsWebTxnDetail, EocmsWebTxnDetailId, EocmsWebTxnDetailService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsWebTxnDetailController(EocmsWebTxnDetailService service) {
        super(service);
    }
    
    /**
     * POST endpoint for listing web transaction detail records
     * Replaces the GET endpoint
     * 
     * @param requestBody The request parameters
     * @return ResponseEntity containing either SingleResponse or FindAllResponse depending on query parameters and result size
     */
    @PostMapping("/webtxndetaillist")
    public ResponseEntity<?> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Create normalized parameters map
        Map<String, String[]> normalizedParams = normalizeRequestBody(requestBody);

        // Get response from service
        FindAllResponse<EocmsWebTxnDetail> response = service.getAll(normalizedParams);

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
    private ResponseEntity<?> createSingleResponse(EocmsWebTxnDetail item) {
        List<EocmsWebTxnDetail> itemList = List.of(item);
         SingleResponse<List<EocmsWebTxnDetail>> response = SingleResponse.createPaginatedResponse(itemList, 0, 0, 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webtxndetailpatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the composite ID components from the payload
            String receiptNo = (String) payload.get("receiptNo");
            String offenceNoticeNo = (String) payload.get("offenceNoticeNo");

            if (receiptNo == null || offenceNoticeNo == null) {
                throw new IllegalArgumentException("dateOfProcessing and noticeNo are required");
            }

            // Create the composite ID
            EocmsWebTxnDetailId id = new EocmsWebTxnDetailId(receiptNo, offenceNoticeNo);

            // Remove ID fields from the payload to avoid conflicts
            Map<String, Object> entityData = new HashMap<>(payload);

            // Convert payload to entity
            EocmsWebTxnDetail entity = objectMapper.convertValue(entityData, EocmsWebTxnDetail.class);

            // Set the ID fields
            entity.setReceiptNo(receiptNo);
            entity.setOffenceNoticeNo(offenceNoticeNo);

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
