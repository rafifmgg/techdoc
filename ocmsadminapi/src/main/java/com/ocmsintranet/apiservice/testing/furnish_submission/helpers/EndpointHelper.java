package com.ocmsintranet.apiservice.testing.furnish_submission.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.submission.FurnishSubmissionService;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper for calling actual furnish endpoints
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EndpointHelper {

    private final FurnishSubmissionService furnishSubmissionService;

    /**
     * Call the furnish submission endpoint
     *
     * @param request Furnish submission request
     * @return Submission result
     */
    public FurnishSubmissionResult callSubmitEndpoint(FurnishSubmissionRequest request) {
        log.info("Calling furnish submission endpoint for notice: {}", request.getNoticeNo());
        return furnishSubmissionService.handleFurnishSubmission(request);
    }
}
