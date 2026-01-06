package com.ocmsintranet.apiservice.workflows.notice_management.retrieval;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetail;
import com.ocmsintranet.apiservice.workflows.notice_management.retrieval.dto.OcmsOffenceNoticeDetailPlusDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetailService;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling PLUS API notice detail endpoint
 *
 * Note: This controller directly calls the CRUD OcmsOffenceNoticeDetailService
 * and performs DTO transformation in the controller layer
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class NoticeDetailControllerPlus {

    private final OcmsOffenceNoticeDetailService service;

    /**
     * Endpoint for retrieving offence notice details
     *
     * @param requestBody Request body containing noticeNo and optional filters
     * @return Response with offence notice detail data
     */
    @PostMapping("/plus-offence-notice-detail")
    public ResponseEntity<?> getPlusOffenceNoticeDetail(@RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Received request on /plus-offence-notice-detail: {}", requestBody);

        try {
            // Validate request body
            if (requestBody == null || requestBody.isEmpty()) {
                log.warn("Request body is empty");
                return createErrorResponse("OCMS-4000", "Request body is required");
            }

            // Validate noticeNo is present
            if (!requestBody.containsKey("noticeNo") || requestBody.get("noticeNo") == null) {
                log.warn("noticeNo is missing in request body");
                return createErrorResponse("OCMS-4000", "noticeNo is required");
            }

            // Validate noticeNo is not empty string
            String noticeNo = requestBody.get("noticeNo").toString().trim();
            if (noticeNo.isEmpty()) {
                log.warn("noticeNo is empty string");
                return createErrorResponse("OCMS-4000", "noticeNo is required");
            }

            // Convert request body to the format expected by service
            Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

            // Call service to get offence notice details
            FindAllResponse<OcmsOffenceNoticeDetail> internalResponse = service.getAll(normalizedParams);

            // Transform to PLUS DTO format
            FindAllResponse<OcmsOffenceNoticeDetailPlusDto> plusResponse = new FindAllResponse<>();
            plusResponse.setTotal(internalResponse.getTotal());
            plusResponse.setLimit(internalResponse.getLimit());
            plusResponse.setSkip(internalResponse.getSkip());

            // Map entities to DTOs
            plusResponse.setData(
                internalResponse.getData().stream()
                    .map(OcmsOffenceNoticeDetailPlusDto::new)
                    .collect(Collectors.toList())
            );

            log.info("Successfully processed PLUS offence notice detail request. Total records: {}", plusResponse.getTotal());
            return new ResponseEntity<>(plusResponse, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing PLUS offence notice detail request: {}", e.getMessage(), e);
            return createErrorResponse("OCMS-5000", "Something went wrong on our end. Please try again later.");
        }
    }

    /**
     * Create error response in PLUS API format
     */
    private ResponseEntity<?> createErrorResponse(String appCode, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("appCode", appCode);
        errorData.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", errorData);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
