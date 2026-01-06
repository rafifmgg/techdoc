package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRefundNotice;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service implementation for OcmsRefundNotice entities.
 * Primary key is refund_notice_id (format: RE{receipt_no}).
 */
@Service
@Slf4j
public class OcmsRefundNoticeServiceImpl
        extends BaseImplement<OcmsRefundNotice, String, OcmsRefundNoticeRepository>
        implements OcmsRefundNoticeService {

    private final OcmsRefundNoticeRepository refundNoticeRepository;

    public OcmsRefundNoticeServiceImpl(OcmsRefundNoticeRepository repository,
                                        DatabaseRetryService retryService) {
        super(repository, retryService);
        this.refundNoticeRepository = repository;
    }

    @Override
    public boolean createIfNotExists(String receiptNo, String noticeNo, BigDecimal refundAmount,
                                      BigDecimal amountPaid, OcmsValidOffenceNotice von) {
        // Check if refund notice already exists for this receipt
        Optional<OcmsRefundNotice> existing = refundNoticeRepository.findByReceiptNo(receiptNo);
        if (existing.isPresent()) {
            log.debug("Refund notice already exists for receipt_no: {}", receiptNo);
            return false;
        }

        // Generate refund_notice_id as RE{receipt_no}
        String refundNoticeId = "RE" + receiptNo;

        OcmsRefundNotice refundNotice = new OcmsRefundNotice();
        refundNotice.setRefundNoticeId(refundNoticeId);
        refundNotice.setReceiptNo(receiptNo);
        refundNotice.setNoticeNo(noticeNo);
        refundNotice.setRefundAmount(refundAmount);
        refundNotice.setAmountPaid(amountPaid);

        // Populate fields from VON if available
        if (von != null) {
            refundNotice.setPpCode(von.getPpCode());
            refundNotice.setAmountPayable(von.getAmountPayable());
            refundNotice.setSuspensionType(von.getSuspensionType());
            refundNotice.setEprReasonOfSuspension(von.getEprReasonOfSuspension());
            refundNotice.setCrsReasonOfSuspension(von.getCrsReasonOfSuspension());
        }

        save(refundNotice);
        log.info("Created refund notice: refund_notice_id={}, receipt_no={}, notice_no={}, refund_amount={}",
                refundNoticeId, receiptNo, noticeNo, refundAmount);
        return true;
    }
}
