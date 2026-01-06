package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

public interface OcmsFurnishApplicationDocService extends BaseService<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId> {

    /**
     * Find all documents for a transaction
     */
    List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo);
}
