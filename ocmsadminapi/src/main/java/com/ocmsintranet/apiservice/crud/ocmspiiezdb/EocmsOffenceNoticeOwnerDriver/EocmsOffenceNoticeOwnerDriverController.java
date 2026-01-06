package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

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
 * Controller for EocmsOffenceNoticeOwnerDriver entity (PII)
 * This controller exposes specific endpoints for handling encrypted PII data:
 * 1. POST /piioffencenoticeownerdriver - for creating owner/driver entries
 * 2. POST /piioffencenoticeownerdriverlist - for listing owner/driver entries
 * 3. POST /piioffencenoticeownerdriverpatch/{noticeNo} - for updating owner/driver entries
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class EocmsOffenceNoticeOwnerDriverController {

    private final EocmsOffenceNoticeOwnerDriverService service;

    @Autowired
    private ObjectMapper objectMapper;

    public EocmsOffenceNoticeOwnerDriverController(EocmsOffenceNoticeOwnerDriverService service) {
        this.service = service;
        log.info("PII offence notice owner driver controller initialized");
    }

    /**
     * POST endpoint for creating an owner/driver entry with PII encryption
     */
    @PostMapping("/piioffencenoticeownerdriver")
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            if (payload instanceof List<?>) {
                // Handle array of entity objects
                List<EocmsOffenceNoticeOwnerDriver> entities = objectMapper.convertValue(payload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EocmsOffenceNoticeOwnerDriver.class));
                service.saveAll(entities);
            } else {
                // Handle single entity object
                EocmsOffenceNoticeOwnerDriver entity = objectMapper.convertValue(payload, EocmsOffenceNoticeOwnerDriver.class);
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
            responseData.setMessage("Error creating PII owner/driver entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint for listing owner/driver entries with PII decryption
     */
    @PostMapping("/piioffencenoticeownerdriverlist")
    public ResponseEntity<FindAllResponse<EocmsOffenceNoticeOwnerDriver>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<EocmsOffenceNoticeOwnerDriver> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint for updating an owner/driver entry by notice number
     */
    @PostMapping("/piioffencenoticeownerdriverpatch/{noticeNo}")
    public ResponseEntity<?> updatePostWithPathVariables(
            @PathVariable String noticeNo,
            @RequestBody Object payload) {
        try {
            // Convert payload to entity
            EocmsOffenceNoticeOwnerDriver entity = objectMapper.convertValue(payload, EocmsOffenceNoticeOwnerDriver.class);

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
            responseData.setMessage("Error updating PII owner/driver entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint to get a specific owner/driver entry by notice number
     */
    @PostMapping("/piioffencenoticeownerdriver/{noticeNo}")
    public ResponseEntity<?> getByNoticeNo(@PathVariable String noticeNo) {
        try {
            Optional<EocmsOffenceNoticeOwnerDriver> entity = service.getById(noticeNo);

            if (entity.isPresent()) {
                return new ResponseEntity<>(entity.get(), HttpStatus.OK);
            } else {
                // Return not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                responseData.setMessage("PII owner/driver entry not found with notice number: " + noticeNo);
                errorResponse.setData(responseData);

                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error retrieving PII owner/driver entry: " + e.getMessage());
            errorResponse.setData(responseData);

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}