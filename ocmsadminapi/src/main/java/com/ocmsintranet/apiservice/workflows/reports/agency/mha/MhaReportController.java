package com.ocmsintranet.apiservice.workflows.reports.agency.mha;

import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@RestController
@RequestMapping("/${api.version}/mha-report")
@Slf4j
public class MhaReportController {

    @Autowired
    private final MhaReportService notificationReportService;

    @Autowired
    private ObjectMapper objectMapper;

    public MhaReportController(MhaReportService notificationReportService) {
        this.notificationReportService = notificationReportService;
    }

    @PostMapping("/search")
    public ResponseEntity<MhaReportResponseDto> getMhaReport(
            @RequestBody Map<String, Object> rawRequest) {
        try {
            // Parse the standard fields
            MhaReportRequestDto request = objectMapper.convertValue(rawRequest, MhaReportRequestDto.class);

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

            log.info("Received notification report request: {}", request);
            return notificationReportService.getMhaReport(request);
        } catch (Exception e) {
            log.error("Error parsing request", e);
            return ResponseEntity.badRequest().build();
        }
    }
}