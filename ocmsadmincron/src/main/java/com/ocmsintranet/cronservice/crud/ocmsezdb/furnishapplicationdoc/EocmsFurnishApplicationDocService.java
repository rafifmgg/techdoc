package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

public interface EocmsFurnishApplicationDocService extends BaseService<EocmsFurnishApplicationDoc, EocmsFurnishApplicationDocId> {

    /**
     * Find all documents by sync status
     * Used by cron job to find unsynced records (is_sync = 'N')
     */
    List<EocmsFurnishApplicationDoc> findByIsSync(String isSync);

    /**
     * Find all documents for a transaction
     */
    List<EocmsFurnishApplicationDoc> findByTxnNo(String txnNo);
}
