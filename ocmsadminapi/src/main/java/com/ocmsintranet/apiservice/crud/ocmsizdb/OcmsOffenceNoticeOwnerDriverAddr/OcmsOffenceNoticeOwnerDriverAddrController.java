package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetail;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.dto.OcmsOffenceNoticeOwnerDriverAddrDto;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for OcmsOffenceNoticeOwnerDriverAddr entity
 * POST-only endpoints, following the style of OcmsOffenceNoticeDetailController:
 * - POST /offencenoticeaddresslist
 * - POST /offencenoticeaddresspatch
 * - POST /offencenoticeaddresscreate
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsOffenceNoticeOwnerDriverAddrController {

    private final OcmsOffenceNoticeOwnerDriverAddrService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public OcmsOffenceNoticeOwnerDriverAddrController(OcmsOffenceNoticeOwnerDriverAddrService service) {
        this.service = service;
        log.info("offencenoticeaddress controller initialized");
    }

    /**
     * POST endpoint to create a new address (moved to explicit path).
     */
    @PostMapping("/offencenoticeaddress")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<OcmsOffenceNoticeOwnerDriverAddr> entities = objectMapper.convertValue(payload, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OcmsOffenceNoticeOwnerDriverAddr.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                OcmsOffenceNoticeOwnerDriverAddr entity = objectMapper.convertValue(payload, OcmsOffenceNoticeOwnerDriverAddr.class);
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
            responseData.setMessage("Error creating parameter: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint for listing offence notice addresses.
     * Supports filters: noticeNo (required for meaningful results), ownerDriverIndicator, typeOfAddress
     * Supports pagination: $skip, $limit (<= 0 means no limit)
     */
    @PostMapping("/offencenoticeaddresslist")
    public ResponseEntity<FindAllResponse<OcmsOffenceNoticeOwnerDriverAddr>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<OcmsOffenceNoticeOwnerDriverAddr> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for partial update (patch) using composite ID in the path.
     * Mirrors ParameterController's pattern: converts payload to entity, sets ID fields from path, then calls service.patch.
     */
    @PostMapping("/offencenoticeaddresspatch/{noticeNo}/{ownerDriverIndicator}/{typeOfAddress}")
    public ResponseEntity<?> patchPostWithPathVariables(
            @PathVariable String noticeNo,
            @PathVariable String ownerDriverIndicator,
            @PathVariable String typeOfAddress,
            @RequestBody Object payload) {
        try {
            // Convert payload to partial entity
            OcmsOffenceNoticeOwnerDriverAddr partial = objectMapper.convertValue(payload, OcmsOffenceNoticeOwnerDriverAddr.class);

            // Ensure composite key fields match the path variables
            partial.setNoticeNo(noticeNo);
            partial.setOwnerDriverIndicator(ownerDriverIndicator);
            partial.setTypeOfAddress(typeOfAddress);

            // Build composite ID and patch
            OcmsOffenceNoticeOwnerDriverAddrId id = new OcmsOffenceNoticeOwnerDriverAddrId(noticeNo, ownerDriverIndicator, typeOfAddress);
            service.patch(id, partial);

            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating address: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }


}