package com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Icarpark entity
 * This controller exposes specific endpoints:
 * 1. POST /carparklist - for listing carparks
 * 2. POST /basiccarparklist - for getting basic carpark information
 * 3. GET /basiccarpark - for getting basic carpark information
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class IcarparkController {
    
    private final IcarparkService service;
    
    public IcarparkController(IcarparkService service) {
        this.service = service;
        log.info("carpark controller initialized");
    }

    /**
     * POST endpoint for listing carparks
     * Replaces the GET /icarpark endpoint
     */
    @PostMapping("/carpark")
    public ResponseEntity<FindAllResponse<Icarpark>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service using the utility class
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        
        FindAllResponse<Icarpark> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }    
    
    /**
     * Get all carparks with only carParkId and carParkName fields
     * @return List of carparks with limited fields
     */
    @GetMapping("/carparkbasic")
    public ResponseEntity<List<Map<String, Object>>> getBasicCarparks() {
        // Get all carparks
        List<Icarpark> carparks = service.getAll(new HashMap<>()).getData();
        
        // Transform to only include carParkId and carParkName
        List<Map<String, Object>> result = carparks.stream()
            .map(carpark -> {
                Map<String, Object> map = new HashMap<>();
                map.put("carParkId", carpark.getCarParkId());
                map.put("carParkName", carpark.getCarParkName());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * POST endpoint for getting basic carpark information
     * Replaces the GET /icarpark/basic endpoint
     */
    @PostMapping("/carparkbasiclist")
    public ResponseEntity<List<Map<String, Object>>> getBasicCarparksPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Get all carparks
        List<Icarpark> carparks = service.getAll(new HashMap<>()).getData();
        
        // Transform to only include carParkId and carParkName
        List<Map<String, Object>> result = carparks.stream()
            .map(carpark -> {
                Map<String, Object> map = new HashMap<>();
                map.put("carParkId", carpark.getCarParkId());
                map.put("carParkName", carpark.getCarParkName());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
