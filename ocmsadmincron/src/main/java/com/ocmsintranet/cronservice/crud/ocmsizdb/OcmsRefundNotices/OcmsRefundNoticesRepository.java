package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OcmsRefundNotices entity
 * Primary key: refund_notice_id
 * refund_notice_id format: RE{YYYYMMDD}{SSS}{NNN} (16 characters)
 */
@Repository
public interface OcmsRefundNoticesRepository extends BaseRepository<OcmsRefundNotices, String> {

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
     * Get the next value from the refund notice sequence
     * Uses SQL Server sequence: ocmsizmgr.seq_ocms_refund_notice
     *
     * @return The next sequence value (1-999, cycles)
     */
    @Query(value = "SELECT NEXT VALUE FOR ocmsizmgr.seq_ocms_refund_notice", nativeQuery = true)
    Integer getNextSequenceValue();
}
