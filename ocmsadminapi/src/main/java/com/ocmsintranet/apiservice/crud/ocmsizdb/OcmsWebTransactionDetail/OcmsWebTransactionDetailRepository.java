package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OcmsWebTransactionDetail entity
 */
@Repository
public interface OcmsWebTransactionDetailRepository extends BaseRepository<OcmsWebTransactionDetail, OcmsWebTransactionDetailId> {

    /**
     * Find all transactions for a specific offence notice number
     * Ordered by payment date descending (most recent first)
     */
    @Query("SELECT w FROM OcmsWebTransactionDetail w WHERE w.offenceNoticeNo = :offenceNoticeNo ORDER BY w.paymentDateAndTime DESC")
    List<OcmsWebTransactionDetail> findByOffenceNoticeNo(@Param("offenceNoticeNo") String offenceNoticeNo);

    /**
     * Find all transactions with a specific receipt number
     */
    @Query("SELECT w FROM OcmsWebTransactionDetail w WHERE w.receiptNo = :receiptNo")
    List<OcmsWebTransactionDetail> findByReceiptNo(@Param("receiptNo") String receiptNo);
}
