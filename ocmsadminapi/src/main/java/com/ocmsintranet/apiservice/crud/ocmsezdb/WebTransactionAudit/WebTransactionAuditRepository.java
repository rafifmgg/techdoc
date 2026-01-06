package com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for WebTransactionAudit entity
 */
@Repository
public interface WebTransactionAuditRepository extends BaseRepository<WebTransactionAudit, String> {
    // The base repository provides all necessary methods
    // Add custom query methods here if needed
}
