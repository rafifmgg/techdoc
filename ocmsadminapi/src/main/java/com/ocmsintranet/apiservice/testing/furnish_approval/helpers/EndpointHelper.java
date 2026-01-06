package com.ocmsintranet.apiservice.testing.furnish_approval.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.approval.FurnishApprovalService;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalRequest;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper for calling actual furnish approval endpoints
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EndpointHelper {

    private final FurnishApprovalService furnishApprovalService;

    /**
     * Call the furnish approval endpoint
     *
     * @param request Furnish approval request
     * @return Approval result
     */
    public FurnishApprovalResult callApprovalEndpoint(FurnishApprovalRequest request) {
        log.info("Calling furnish approval endpoint for txnNo: {}", request.getTxnNo());
        return furnishApprovalService.handleApproval(request);
    }
}
