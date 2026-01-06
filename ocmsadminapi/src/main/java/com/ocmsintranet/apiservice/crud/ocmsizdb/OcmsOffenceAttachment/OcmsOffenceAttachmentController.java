package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for OcmsOffenceAttachment entity
 * This controller exposes three specific endpoints:
 * 1. POST /offenceattachment - for creating attachments
 * 2. POST /offenceattachmentlist - for listing attachments
 * 3. POST /plus-offence-attachment - for listing attachments (PLUS Interface)
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsOffenceAttachmentController {
    
    private final OcmsOffenceAttachmentService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsOffenceAttachmentController(OcmsOffenceAttachmentService service) {
        this.service = service;
        log.info("OcmsOffenceAttachment controller initialized");
    }
    
    /**
     * POST endpoint for creating an attachment
     */
    @PostMapping("/offenceattachment")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OcmsOffenceAttachment> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OcmsOffenceAttachment.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OcmsOffenceAttachment entity = objectMapper.convertValue(payload, OcmsOffenceAttachment.class);
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
            responseData.setMessage("Error creating attachment: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing attachments
     */
    @PostMapping("/offenceattachmentlist")
    public ResponseEntity<FindAllResponse<OcmsOffenceAttachment>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsOffenceAttachment> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for listing offence attachments (PLUS Interface)
     * API Spec: Get attachments by noticeNo
     *
     * Request Body Example:
     * {
     *   "noticeNo": "541000009T"
     * }
     *
     * Success Response (200):
     * {
     *   "total": 2,
     *   "limit": 10,
     *   "skip": 0,
     *   "data": [
     *     {
     *       "attachmentId": 123,
     *       "noticeNo": "541000009T",
     *       "fileName": "evidence.jpg",
     *       "mime": "image/jpeg",
     *       "size": 245678
     *     }
     *   ]
     * }
     *
     * Error Response:
     * {
     *   "data": {
     *     "appCode": "OCMS-4000",
     *     "message": "noticeNo is required"
     *   }
     * }
     */
}
