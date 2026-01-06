package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionAudit;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for WebTransactionAudit entity
 */
@Repository
public interface OcmsWebTransactionAuditRepository extends BaseRepository<OcmsWebTransactionAudit, String> {
    /**
     * Find transactions where txnDetail JSON contains a specific string
     * Used for finding transactions by notice number or receipt number
     *
     * @param searchString String to search for in txnDetail
     * @return List of matching transactions
     */
    List<OcmsWebTransactionAudit> findByTxnDetailContaining(String searchString);
}
