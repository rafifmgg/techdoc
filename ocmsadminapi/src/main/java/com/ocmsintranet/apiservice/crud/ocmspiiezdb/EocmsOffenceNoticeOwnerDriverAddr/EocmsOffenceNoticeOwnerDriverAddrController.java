package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * REMOVE THIS CONTROLLER LATER IF NOT USED BY FE
 * Controller for EocmsOffenceNoticeOwnerDriverAddr entity (PII)
 * This controller exposes specific endpoints for handling encrypted PII address data:
 * 1. POST /piioffencenoticeaddress - for creating address entries
 * 2. POST /piioffencenoticeaddresslist - for listing address entries
 * 3. POST /piioffencenoticeaddresspatch - for updating address entries (composite key in body)
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class EocmsOffenceNoticeOwnerDriverAddrController {

    private final EocmsOffenceNoticeOwnerDriverAddrService service;

    @Autowired
    private ObjectMapper objectMapper;

    public EocmsOffenceNoticeOwnerDriverAddrController(EocmsOffenceNoticeOwnerDriverAddrService service) {
        this.service = service;
        log.info("PII offence notice address controller initialized");
    }

    /**
     * POST endpoint for creating an address entry with PII encryption
     */
    @PostMapping("/piioffencenoticeaddress")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<EocmsOffenceNoticeOwnerDriverAddr> entities = objectMapper.convertValue(payload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EocmsOffenceNoticeOwnerDriverAddr.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                EocmsOffenceNoticeOwnerDriverAddr entity = objectMapper.convertValue(payload, EocmsOffenceNoticeOwnerDriverAddr.class);
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
            responseData.setMessage("Error creating PII address entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint for listing address entries with PII decryption
     */
    @PostMapping("/piioffencenoticeaddresslist")
    public ResponseEntity<FindAllResponse<EocmsOffenceNoticeOwnerDriverAddr>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<EocmsOffenceNoticeOwnerDriverAddr> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for updating an address entry (composite key provided in body)
     */
    @PostMapping("/piioffencenoticeaddresspatch")
    public ResponseEntity<?> updatePost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract composite key components from payload
            String noticeNo = (String) payload.get("noticeNo");
            String ownerDriverIndicator = (String) payload.get("ownerDriverIndicator");
            String typeOfAddress = (String) payload.get("typeOfAddress");

            if (noticeNo == null || ownerDriverIndicator == null || typeOfAddress == null) {
                throw new IllegalArgumentException("noticeNo, ownerDriverIndicator, and typeOfAddress are required");
            }

            // Create composite key
            EocmsOffenceNoticeOwnerDriverAddrId id = new EocmsOffenceNoticeOwnerDriverAddrId(
                noticeNo, ownerDriverIndicator, typeOfAddress);

            // Convert payload to entity
            EocmsOffenceNoticeOwnerDriverAddr entity = objectMapper.convertValue(payload, EocmsOffenceNoticeOwnerDriverAddr.class);

            // Call the patch method from service
            service.patch(id, entity);

            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating PII address entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint to get addresses for a specific notice
     */
    @PostMapping("/piioffencenoticeaddressbynotice")
    public ResponseEntity<?> getByNoticeNo(@RequestBody Map<String, String> request) {
        try {
            String noticeNo = request.get("noticeNo");

            if (noticeNo == null) {
                throw new IllegalArgumentException("noticeNo is required");
            }

            List<EocmsOffenceNoticeOwnerDriverAddr> addresses = service.findByNoticeNo(noticeNo);

            if (!addresses.isEmpty()) {
                return new ResponseEntity<>(addresses, HttpStatus.OK);
            } else {
                // Return empty list response
                return new ResponseEntity<>(addresses, HttpStatus.OK);
            }
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error retrieving PII address entries: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint to get a specific address entry
     */
    @PostMapping("/piioffencenoticeaddressget")
    public ResponseEntity<?> getById(@RequestBody Map<String, String> request) {
        try {
            String noticeNo = request.get("noticeNo");
            String ownerDriverIndicator = request.get("ownerDriverIndicator");
            String typeOfAddress = request.get("typeOfAddress");

            if (noticeNo == null || ownerDriverIndicator == null || typeOfAddress == null) {
                throw new IllegalArgumentException("noticeNo, ownerDriverIndicator, and typeOfAddress are required");
            }

            // Create composite key
            EocmsOffenceNoticeOwnerDriverAddrId id = new EocmsOffenceNoticeOwnerDriverAddrId(
                noticeNo, ownerDriverIndicator, typeOfAddress);

            Optional<EocmsOffenceNoticeOwnerDriverAddr> entity = service.getById(id);

            if (entity.isPresent()) {
                return new ResponseEntity<>(entity.get(), HttpStatus.OK);
            } else {
                // Return not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                responseData.setMessage("PII address entry not found");
                errorResponse.setData(responseData);

                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error retrieving PII address entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}