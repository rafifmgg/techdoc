package com.ocmsintranet.apiservice.workflows.reports.agency.ps;

import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * OCMS 18 - PS Report Controller
 *
 * REST controller for PS Report endpoints
 * Section 6: Permanent Suspension Report
 *
 * Endpoints:
 * - POST /ps-report/by-system - PS by System report (Section 6.3.1)
 * - POST /ps-report/by-officer - PS by Officer report (Section 6.3.2)
 */
@RestController
@RequestMapping("/${api.version}/ps-report")
@Slf4j
public class PsReportController {

    @Autowired
    private final PsReportService psReportService;

    @Autowired
    private ObjectMapper objectMapper;

    public PsReportController(PsReportService psReportService) {
        this.psReportService = psReportService;
    }

    /**
     * PS by System Report Endpoint
     * Section 6.3.1: List of PS applied by various sub-systems (OCMS, PLUS, Backend)
     *
     * @param rawRequest Request body with filters and pagination
     * @return PS report data
     */
    @PostMapping("/by-system")
    public ResponseEntity<PsReportResponseDto> getPsBySystemReport(
            @RequestBody Map<String, Object> rawRequest) {
        try {
            // Parse the standard fields
            PsReportRequestDto request = objectMapper.convertValue(rawRequest, PsReportRequestDto.class);

            // Extract sort field from $sort[fieldName] format
            for (Map.Entry<String, Object> entry : rawRequest.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("$sort[") && key.endsWith("]")) {
                    // Extract field name from $sort[fieldName]
                    String fieldName = key.substring(6, key.length() - 1);
                    request.setSortField(fieldName);

                    // Get sort direction
                    if (entry.getValue() != null) {
                        try {
                            request.setSortDirection(Integer.parseInt(entry.getValue().toString()));
                        } catch (NumberFormatException e) {
                            request.setSortDirection(1); // Default to ASC
                        }
                    }
                    break; // Only support single field
                }
            }

            log.info("Received PS by System report request: {}", request);
            return psReportService.getPsBySystemReport(request);
        } catch (Exception e) {
            log.error("Error parsing PS by System report request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PS by Officer Report Endpoint
     * Section 6.3.2: List of PS applied by OICs
     *
     * @param rawRequest Request body with filters and pagination
     * @return PS report data
     */
    @PostMapping("/by-officer")
    public ResponseEntity<PsReportResponseDto> getPsByOfficerReport(
            @RequestBody Map<String, Object> rawRequest) {
        try {
            // Parse the standard fields
            PsReportRequestDto request = objectMapper.convertValue(rawRequest, PsReportRequestDto.class);

            // Extract sort field from $sort[fieldName] format
            for (Map.Entry<String, Object> entry : rawRequest.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("$sort[") && key.endsWith("]")) {
                    // Extract field name from $sort[fieldName]
                    String fieldName = key.substring(6, key.length() - 1);
                    request.setSortField(fieldName);

                    // Get sort direction
                    if (entry.getValue() != null) {
                        try {
                            request.setSortDirection(Integer.parseInt(entry.getValue().toString()));
                        } catch (NumberFormatException e) {
                            request.setSortDirection(1); // Default to ASC
                        }
                    }
                    break; // Only support single field
                }
            }

            log.info("Received PS by Officer report request: {}", request);
            return psReportService.getPsByOfficerReport(request);
        } catch (Exception e) {
            log.error("Error parsing PS by Officer report request", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
