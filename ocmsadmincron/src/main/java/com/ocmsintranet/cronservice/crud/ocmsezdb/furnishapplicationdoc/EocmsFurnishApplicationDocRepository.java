package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EocmsFurnishApplicationDocRepository extends BaseRepository<EocmsFurnishApplicationDoc, EocmsFurnishApplicationDocId> {

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
