package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsFurnishApplicationDocRepository extends BaseRepository<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId> {

    /**
     * Find all documents by transaction number
     */
    List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo);
}
