package com.ocmsintranet.cronservice.crud.ocmsizdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for WebTxnDetail entity
 */
@Repository
public interface WebTxnDetailRepository extends BaseRepository<WebTxnDetail, WebTxnDetailId> {
    // The base repository provides all necessary methods
    // Add custom query methods here if needed
}
