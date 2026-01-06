package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsWebTransactionAudit;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for WebTransactionAudit entity
 */
@Repository
public interface OcmsWebTransactionAuditRepository extends BaseRepository<OcmsWebTransactionAudit, String> {
    // The base repository provides all necessary methods
    // Add custom query methods here if needed
}
