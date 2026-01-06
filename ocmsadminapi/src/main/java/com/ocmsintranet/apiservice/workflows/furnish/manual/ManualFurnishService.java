package com.ocmsintranet.apiservice.workflows.furnish.manual;

import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.BulkFurnishRequest;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.BulkFurnishResult;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.ManualFurnishRequest;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.ManualFurnishResult;

/**
 * Service interface for officer manual furnish operations.
 * Based on OCMS 41 User Stories 41.46-41.51.
 */
public interface ManualFurnishService {

    /**
     * Manually furnish single notice (OCMS41.46-41.48)
     *
     * @param request Manual furnish request
     * @return Result indicating success or error
     */
    ManualFurnishResult handleManualFurnish(ManualFurnishRequest request);

    /**
     * Bulk furnish multiple notices (OCMS41.50-41.51)
     *
     * @param request Bulk furnish request
     * @return Result with success/failure counts
     */
    BulkFurnishResult handleBulkFurnish(BulkFurnishRequest request);
}
