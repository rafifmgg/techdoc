package com.ocmsintranet.apiservice.workflows.furnish.dashboard;

import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListRequest;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListResponse;

/**
 * Service interface for officer dashboard operations.
 * Based on OCMS 41 User Stories 41.9-41.14.
 */
public interface FurnishDashboardService {

    /**
     * Get list of furnish applications with optional filters (OCMS41.9-41.12)
     *
     * @param request Search and filter criteria
     * @return List of furnish applications with pagination
     */
    FurnishApplicationListResponse listFurnishApplications(FurnishApplicationListRequest request);

    /**
     * Get detailed view of a single furnish application (OCMS41.13-41.14)
     *
     * @param txnNo Transaction number
     * @return Detailed furnish application information
     */
    FurnishApplicationDetailResponse getApplicationDetail(String txnNo);
}
