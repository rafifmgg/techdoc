package com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.cronservice.crud.BaseService;
import java.util.Optional;

/**
 * Service interface for EocmsOffenceNoticeOwnerDriver entity.
 * Handles encrypted PII owner/driver data operations.
 */
public interface EocmsOffenceNoticeOwnerDriverService extends BaseService<EocmsOffenceNoticeOwnerDriver, String> {
    /**
     * Find ONOD record by composite key (notice_no + owner_driver_indicator)
     */
    Optional<EocmsOffenceNoticeOwnerDriver> findByNoticeNoAndOwnerDriverIndicator(String noticeNo, String ownerDriverIndicator);
}
