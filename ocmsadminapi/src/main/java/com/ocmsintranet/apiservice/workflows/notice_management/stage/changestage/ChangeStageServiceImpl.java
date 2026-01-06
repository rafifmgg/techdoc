package com.ocmsintranet.apiservice.workflows.notice_management.stage;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.ChangeOfProcessingService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.PlusChangeStageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ChangeStageService for handling PLUS API change stage requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeStageServiceImpl implements ChangeStageService {

    private final ChangeOfProcessingService changeProcessingStageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<?> processPlusApplyChangeStage(PlusChangeStageRequest request) {
        try {
            // Validate request body
            if (request == null) {
                log.warn("Null request body in PLUS change stage request");
                return createOcmsErrorResponse("OCMS-4001", "Request body is required");
            }

            // Validate noticeNo list
            if (request.getNoticeNo() == null || request.getNoticeNo().isEmpty()) {
                log.warn("Empty or null noticeNo in PLUS request");
                return createOcmsErrorResponse("OCMS-4001", "noticeNo is required and cannot be empty");
            }

            // Validate required fields
            if (request.getLastStageName() == null || request.getLastStageName().trim().isEmpty()) {
                return createOcmsErrorResponse("OCMS-4001", "lastStageName is required");
            }
            if (request.getNextStageName() == null || request.getNextStageName().trim().isEmpty()) {
                return createOcmsErrorResponse("OCMS-4001", "nextStageName is required");
            }
            if (request.getOffenceType() == null || request.getOffenceType().trim().isEmpty()) {
                return createOcmsErrorResponse("OCMS-4001", "offenceType is required");
            }
            if (request.getSource() == null || request.getSource().trim().isEmpty()) {
                return createOcmsErrorResponse("OCMS-4001", "source is required");
            }

            log.info("Processing PLUS change stage request with {} notices, lastStage: {}, nextStage: {}, offenceType: {}",
                    request.getNoticeNo().size(), request.getLastStageName(), request.getNextStageName(), request.getOffenceType());

            // Process the request
            changeProcessingStageService.processPlusChangeStage(request);

            log.info("Completed PLUS change stage request successfully");

            // Return success response
            return createOcmsSuccessResponse("OCMS-2000", "Stage change applied successfully");

        } catch (ChangeOfProcessingService.PlusChangeStageException e) {
            // Business validation errors
            log.warn("Business validation failed: {}", e.getMessage());
            return createOcmsErrorResponse("OCMS-4002", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing PLUS change stage request", e);
            return createOcmsErrorResponse("OCMS-5000", "Something went wrong on our end. Please try again later.");
        }
    }

    /**
     * Create OCMS success response
     */
    private ResponseEntity<?> createOcmsSuccessResponse(String appCode, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("appCode", appCode);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    /**
     * Create OCMS error response
     */
    private ResponseEntity<?> createOcmsErrorResponse(String appCode, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("appCode", appCode);
        data.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);

        return ResponseEntity.badRequest().body(response);
    }
}
