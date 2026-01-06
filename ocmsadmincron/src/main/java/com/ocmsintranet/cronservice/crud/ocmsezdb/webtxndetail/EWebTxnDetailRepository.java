package com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EwebTxnDetail entity
 */
@Repository
public interface EWebTxnDetailRepository extends BaseRepository<EwebTxnDetail, EwebTxnDetailId> {
    
    /**
     * Find unsynchronized transactions created today
     * 
     * @return List of unsynchronized EwebTxnDetail entities
     */
    @Query("SELECT w FROM EwebTxnDetail w WHERE (w.isSync = 'N' OR w.isSync IS NULL) AND w.status = 'S'")
    List<EwebTxnDetail> findUnsynchronizedTodayTransactions();
}
