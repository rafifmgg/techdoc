package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EocmsOffenceNoticeOwnerDriver entity.
 * Handles database operations for encrypted PII owner/driver data.
 */
@Repository
public interface EocmsOffenceNoticeOwnerDriverRepository extends BaseRepository<EocmsOffenceNoticeOwnerDriver, String> {

    /**
     * Find all records for a specific notice
     */
    List<EocmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo);

    /**
     * Find by notice number and owner/driver indicator
     */
    Optional<EocmsOffenceNoticeOwnerDriver> findByNoticeNoAndOwnerDriverIndicator(
        String noticeNo, String ownerDriverIndicator);
}