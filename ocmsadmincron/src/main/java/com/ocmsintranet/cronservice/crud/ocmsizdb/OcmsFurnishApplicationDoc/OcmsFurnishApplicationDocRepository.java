package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsFurnishApplicationDocRepository extends BaseRepository<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId> {

    /**
     * Find all documents for a transaction
     */
    List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo);
}
