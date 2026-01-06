package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for OcmsRefundNotices entities
 * Primary key: refund_notice_id
 * refund_notice_id format: RE{YYYYMMDD}{SSS}{NNN} (16 characters)
 */
@Service
public class OcmsRefundNoticesServiceImpl
        extends BaseImplement<OcmsRefundNotices, String, OcmsRefundNoticesRepository>
        implements OcmsRefundNoticesService {

    /**
     * Constructor with required dependencies
     */
    public OcmsRefundNoticesServiceImpl(OcmsRefundNoticesRepository repository,
                                         DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsRefundNotices> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }

    @Override
    public boolean existsByRefundNoticeId(String refundNoticeId) {
        return repository.existsByRefundNoticeId(refundNoticeId);
    }

    @Override
    public long countByNoticeNo(String noticeNo) {
        return repository.countByNoticeNo(noticeNo);
    }

    @Override
    public Integer getNextRunningNumber() {
        return repository.getNextSequenceValue();
    }
}
