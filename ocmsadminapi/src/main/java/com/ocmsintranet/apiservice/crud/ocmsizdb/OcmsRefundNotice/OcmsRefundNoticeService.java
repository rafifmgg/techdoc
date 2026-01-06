package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRefundNotice;

import com.ocmsintranet.apiservice.crud.BaseService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;

import java.math.BigDecimal;

/**
 * Service interface for OcmsRefundNotice entities.
 * Primary key is refund_notice_id (format: RE{receipt_no}).
 */
public interface OcmsRefundNoticeService extends BaseService<OcmsRefundNotice, String> {

    /**
     * Create a refund notice if it doesn't already exist for the given receipt number.
     *
     * @param receiptNo The receipt number (used to generate refund_notice_id as RE{receipt_no})
     * @param noticeNo The notice number
     * @param refundAmount The amount to be refunded
     * @param amountPaid The amount paid
     * @param von The valid offence notice (for suspension details)
     * @return true if created, false if already exists
     */
    boolean createIfNotExists(String receiptNo, String noticeNo, BigDecimal refundAmount,
                              BigDecimal amountPaid, OcmsValidOffenceNotice von);
}
