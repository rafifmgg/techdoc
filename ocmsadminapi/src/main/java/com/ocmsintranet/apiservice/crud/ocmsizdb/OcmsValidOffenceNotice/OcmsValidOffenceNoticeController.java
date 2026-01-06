package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.NoticeQueryRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.OffenceNoticeWithOwnerDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.normalizer.QueryParamNormalizer;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.http.converter.json.MappingJacksonValue;

import com.ocmsintranet.apiservice.utilities.ParameterUtils;

/**
 * Controller for OcmsValidOffenceNotice entity
 * This controller exposes specific endpoints:
 * 1. POST /validoffencenoticelist - for listing valid offence notices
 * 2. POST /validoffencenoticepatch - for updating valid offence notices
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsValidOffenceNoticeController {
    
    private final OcmsValidOffenceNoticeService service;
    private final QueryParamNormalizer normalizer;

    @Autowired
    private ObjectMapper objectMapper;
    
    public OcmsValidOffenceNoticeController(OcmsValidOffenceNoticeService service, QueryParamNormalizer normalizer) {
        this.service = service;
        this.normalizer = normalizer;
        log.info("validoffencenotice controller initialized");
    }
    
    /**
     * POST endpoint for listing valid offence notices
     * Replaces the GET /validoffencenotice endpoint
     */
    @PostMapping("/validoffencenoticelist")
    public ResponseEntity<MappingJacksonValue> list(
        @RequestBody(required = false) NoticeQueryRequest req) {

        var normalized = normalizer.toNormalizedParamMap(req);
        log.info("Normalized: {}", normalized);

        FindAllResponse<OffenceNoticeWithOwnerDto> resp = service.getAllWithOwnerInfo(normalized);

        String[] fieldArr = normalized.get("$field");
        Set<String> include = null;
        if (fieldArr != null && fieldArr.length > 0 && fieldArr[0] != null && !fieldArr[0].isBlank()) {
            include = Arrays.stream(fieldArr[0].split(",", -1))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        }

        MappingJacksonValue wrapper = new MappingJacksonValue(resp);

        var filter = (include == null || include.isEmpty())
                ? SimpleBeanPropertyFilter.serializeAll()
                : SimpleBeanPropertyFilter.filterOutAllExcept(include);

        FilterProvider filters = new SimpleFilterProvider()
                .addFilter("OffenceNoticeWithOwner.fields", filter);

        wrapper.setFilters(filters);

        return new ResponseEntity<>(wrapper, HttpStatus.OK);
    }

    
    /**
     * POST endpoint for updating a valid offence notice
     * Replaces the PATCH /validoffencenotice/{noticeNo} endpoint
     */
    @PostMapping("/validoffencenoticepatch")
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
            OcmsValidOffenceNotice entity = objectMapper.convertValue(entityData, OcmsValidOffenceNotice.class);
            
            // Set the ID
            entity.setNoticeNo(noticeNo);

            // Call the enhanced patch method from service that handles both entity update and file processing
            service.patchWithFiles(noticeNo, entity, payload);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error updating notice: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}