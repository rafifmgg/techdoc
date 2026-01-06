package com.ocmsintranet.apiservice.testing.furnish_rejection.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.rejection.FurnishRejectionService;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper for calling actual furnish rejection endpoints
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EndpointHelper {

    private final FurnishRejectionService furnishRejectionService;

    /**
     * Call the furnish rejection endpoint
     *
     * @param request Furnish rejection request
     * @return Rejection result
     */
    public FurnishRejectionResult callRejectionEndpoint(FurnishRejectionRequest request) {
        log.info("Calling furnish rejection endpoint for txnNo: {}", request.getTxnNo());
        return furnishRejectionService.handleRejection(request);
    }
}
