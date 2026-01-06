package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.attachment;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment.OcmsOffenceAttachment;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment.OcmsOffenceAttachmentService;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling PLUS API offence attachment endpoint
 *
 * Note: This controller directly calls the CRUD service (OcmsOffenceAttachmentService)
 * without an intermediate workflow service layer for simplicity.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class OffenceAttachmentControllerPlus {

    private final OcmsOffenceAttachmentService ocmsOffenceAttachmentService;

    /**
     * Endpoint for retrieving offence attachments
     *
     * @param requestBody Request body containing noticeNo and optional filters
     * @return Response with offence attachment data
     */
    @PostMapping("/plus-offence-attachment")
    public ResponseEntity<?> getPlusOffenceAttachment(@RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Processing PLUS offence attachment request with body: {}", requestBody);

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

            // Convert request body to the format expected by service using the utility class
            Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);

            // Call CRUD service to get offence attachments
            FindAllResponse<OcmsOffenceAttachment> response = ocmsOffenceAttachmentService.getAll(normalizedParams);

            log.info("Successfully processed PLUS offence attachment request. Total records: {}", response.getTotal());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing PLUS offence attachment request: {}", e.getMessage(), e);
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
