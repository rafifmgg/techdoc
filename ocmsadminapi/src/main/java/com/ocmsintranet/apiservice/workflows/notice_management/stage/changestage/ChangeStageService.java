package com.ocmsintranet.apiservice.workflows.notice_management.stage;

import org.springframework.http.ResponseEntity;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.PlusChangeStageRequest;

/**
 * Service interface for handling PLUS API change stage requests
 */
public interface ChangeStageService {

    /**
     * Process request for PLUS apply change stage endpoint
     *
     * @param request PLUS change stage request containing notice numbers and stage information
     * @return Response with success or error details
     */
    ResponseEntity<?> processPlusApplyChangeStage(PlusChangeStageRequest request);
}
