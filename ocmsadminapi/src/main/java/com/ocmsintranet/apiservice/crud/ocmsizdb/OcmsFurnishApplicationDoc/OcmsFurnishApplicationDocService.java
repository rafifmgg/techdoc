package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.apiservice.crud.BaseService;

import java.util.List;

public interface OcmsFurnishApplicationDocService extends BaseService<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId> {

    /**
     * Find all documents by transaction number
     */
    List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo);
}
