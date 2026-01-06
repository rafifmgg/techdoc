package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsOffenceNoticeDetail entity
 * This controller exposes specific endpoints:
 * 1. POST /offencenoticedetaillist - for listing offence notice details
 * 2. POST /offencenoticedetailpatch - for updating offence notice details
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsOffenceNoticeDetailController {
    
    private final OcmsOffenceNoticeDetailService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsOffenceNoticeDetailController(OcmsOffenceNoticeDetailService service) {
        this.service = service;
        log.info("offencenoticedetail controller initialized");
    }
    
    /**
     * POST endpoint for listing offence notice details
     */
    @PostMapping("/offencenoticedetaillist")
    public ResponseEntity<FindAllResponse<OcmsOffenceNoticeDetail>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<OcmsOffenceNoticeDetail> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for updating an offence notice detail
     * Replaces the PATCH /offencenoticedetail/{noticeNo} endpoint
     */
    @PostMapping("/offencenoticedetailpatch")
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
            OcmsOffenceNoticeDetail entity = objectMapper.convertValue(entityData, OcmsOffenceNoticeDetail.class);
            
            // Set the ID
            entity.setNoticeNo(noticeNo);
            
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
            responseData.setMessage("Error updating notice detail: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
