package com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for EwebTxnDetail entities
 */
public interface EWebTxnDetailService extends BaseService<EwebTxnDetail, EwebTxnDetailId> {

    /**
     * Find unsynchronized transactions created today
     *
     * @return List of unsynchronized EwebTxnDetail entities
     */
    List<EwebTxnDetail> findUnsynchronizedTodayTransactions();

    /**
     * Update the synchronization status of a transaction
     *
     * @param receiptNo The receipt number of the transaction
     * @param offenceNoticeNo The offence notice number of the transaction
     * @param isSync The new synchronization status ("N" or "Y")
     * @return The updated EwebTxnDetail entity
     */
    EwebTxnDetail updateSyncStatus(String receiptNo, String offenceNoticeNo, String isSync);

    /**
     * Batch update synchronization status for multiple transactions
     *
     * @param transactions List of transactions to mark as synchronized
     * @param isSync The new synchronization status (typically "Y")
     */
    void batchUpdateSyncStatus(List<EwebTxnDetail> transactions, String isSync);
}
