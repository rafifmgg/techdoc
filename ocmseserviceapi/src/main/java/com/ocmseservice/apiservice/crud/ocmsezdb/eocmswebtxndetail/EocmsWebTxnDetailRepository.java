package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EocmsWebTxnDetailRepository extends JpaRepository<EocmsWebTxnDetail, EocmsWebTxnDetailId>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsWebTxnDetail> {
    

    @Query(value = "SELECT e.* FROM eocms_web_txn_detail e " +
            "WHERE e.offence_notice_no = :offenceNoticeNo " +
            "AND CAST(e.transaction_date_and_time AS DATE) = CAST(GETDATE() AS DATE) " +
            "AND e.error_remarks IS NULL", nativeQuery = true)
    List<EocmsWebTxnDetail> findByOffenceNoticeNoAndTransactionDateToday(@Param("offenceNoticeNo") String offenceNoticeNo);

    @Query("SELECT e FROM EocmsWebTxnDetail e WHERE e.receiptNo = :receiptNo")
    Optional<EocmsWebTxnDetail> findById(@Param("receiptNo") String receiptNo);

    @Query("SELECT e FROM EocmsWebTxnDetail e WHERE e.offenceNoticeNo = :offenceNoticeNo AND e.status='S'")
    Optional<EocmsWebTxnDetail> findByNotice(@Param("offenceNoticeNo") String offenceNoticeNo);


}
