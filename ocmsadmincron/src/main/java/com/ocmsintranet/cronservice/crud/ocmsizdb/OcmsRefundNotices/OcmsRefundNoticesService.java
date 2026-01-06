package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

/**
 * Service interface for OcmsRefundNotices entities
 * Primary key: refund_notice_id
 * refund_notice_id format: RE{YYYYMMDD}{SSS}{NNN} (16 characters)
 */
public interface OcmsRefundNoticesService extends BaseService<OcmsRefundNotices, String> {

    /**
     * Find all refund notices for a given notice_no
     *
     * @param noticeNo The notice number
     * @return List of refund notices for the notice
     */
    List<OcmsRefundNotices> findByNoticeNo(String noticeNo);

    /**
     * Check if a refund notice exists by refund_notice_id
     *
     * @param refundNoticeId The refund notice ID
     * @return true if exists, false otherwise
     */
    boolean existsByRefundNoticeId(String refundNoticeId);

    /**
     * Count the number of refund notices for a given notice_no
     *
     * @param noticeNo The notice number
     * @return Count of existing refund notices for this notice_no
     */
    long countByNoticeNo(String noticeNo);

    /**
     * Get the next running number from the sequence
     * Uses SQL Server sequence: ocmsizmgr.seq_ocms_refund_notice
     *
     * @return The next sequence value (1-999, cycles)
     */
    Integer getNextRunningNumber();
}
