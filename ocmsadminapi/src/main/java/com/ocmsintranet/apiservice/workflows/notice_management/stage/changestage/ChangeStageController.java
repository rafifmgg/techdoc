package com.ocmsintranet.apiservice.workflows.notice_management.stage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.PlusChangeStageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling PLUS API change stage endpoints
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ChangeStageController {

    private final ChangeStageService changeStageService;

    /**
     * PLUS API endpoint for manual stage change (batch processing)
     * Based on OCMS × PLUS Interface Spec
     *
     * Endpoint: POST /v1/plus-apply-change-stage
     *
     * Request body:
     * {
     *   "noticeNo": ["500500172G", "500500173G"],
     *   "lastStageName": "NPA",
     *   "nextStageName": "ROV",
     *   "lastStageDate": "2025-09-25T06:58:42",
     *   "newStageDate": "2025-09-30T06:58:42",
     *   "offenceType": "O",
     *   "source": "004"
     * }
     *
     * Success Response (HTTP 200):
     * {
     *   "appCode": "OCMS-2000",
     *   "message": "Stage change applied successfully"
     * }
     *
     * Error Response (HTTP 400):
     * {
     *   "data": {
     *     "appCode": "OCMS-4001",
     *     "message": "noticeNo is required and cannot be empty"
     *   }
     * }
     *
     * @param request PLUS change stage request
     * @return Response with success or error
     */
    @PostMapping("/plus-apply-change-stage")
    public ResponseEntity<?> plusApplyChangeStage(@RequestBody PlusChangeStageRequest request) {
        log.info("Received request on /plus-apply-change-stage: noticeNo count={}, lastStage={}, nextStage={}",
                request != null && request.getNoticeNo() != null ? request.getNoticeNo().size() : 0,
                request != null ? request.getLastStageName() : null,
                request != null ? request.getNextStageName() : null);
        return changeStageService.processPlusApplyChangeStage(request);
    }
}
