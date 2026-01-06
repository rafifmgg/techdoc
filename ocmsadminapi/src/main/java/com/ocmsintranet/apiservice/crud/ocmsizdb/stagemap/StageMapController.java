package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for StageMap entity
 * This controller exposes specific endpoints for stage map operations:
 * 1. POST /stagemaplist - for listing stage maps
 *
 * Note: The PLUS API endpoint (/plus-get-stage-map) has been moved to
 * com.ocmsintranet.apiservice.workflows.plus.stagemap.StageMapControllerPlus
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class StageMapController {

    private final StageMapService service;

    @Autowired
    private ObjectMapper objectMapper;

    public StageMapController(StageMapService service) {
        this.service = service;
        log.info("stage map controller initialized");
    }

    /**
     * POST endpoint for listing stage maps
     */
    @PostMapping("/stagemaplist")
    public ResponseEntity<FindAllResponse<StageMap>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

        FindAllResponse<StageMap> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
