package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRefundNotice;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for OcmsRefundNotice entity.
 * Primary key is refund_notice_id (format: RE{receipt_no}).
 */
@Repository
public interface OcmsRefundNoticeRepository extends BaseRepository<OcmsRefundNotice, String> {

    /**
     * Find refund notice by receipt number.
     *
     * @param receiptNo The receipt number
     * @return Optional containing the refund notice if found
     */
    Optional<OcmsRefundNotice> findByReceiptNo(String receiptNo);
}
