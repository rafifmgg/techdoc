package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface SuspendedNoticeRepository extends BaseRepository<SuspendedNotice, SuspendedNoticeId>{
    @NonNull
    Optional<SuspendedNotice> findById(@NonNull SuspendedNoticeId id);

    /**
     * Get the maximum srNo for a given notice number
     * Used to generate the next sequential srNo when creating new suspended notice records
     */
    @Query("SELECT COALESCE(MAX(s.srNo), 0) FROM SuspendedNotice s WHERE s.noticeNo = :noticeNo")
    Integer findMaxSrNoByNoticeNo(@Param("noticeNo") String noticeNo);

    List<SuspendedNotice> findByNoticeNo(String noticeNo);

    /**
     * Find the suspended notice with the highest sr_no for a given notice number
     *
     * @param noticeNo Notice number to search for
     * @return Optional containing the suspended notice with max sr_no
     */
    Optional<SuspendedNotice> findTopByNoticeNoOrderBySrNoDesc(String noticeNo);

    /**
     * Find all suspended notices for a given notice number and suspension type.
     * Used in OCMS 41 to check for active PS (Permanent Suspension).
     *
     * @param noticeNo Notice number to search for
     * @param suspensionType Suspension type (e.g., "PS", "TS")
     * @return List of suspended notices matching the criteria
     */
    List<SuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType);

    /**
     * Find all suspended notices for a given notice number, suspension type, and reason.
     * Used in testing to verify specific suspension records.
     *
     * @param noticeNo Notice number to search for
     * @param suspensionType Suspension type (e.g., "PS", "TS")
     * @param reasonOfSuspension Reason of suspension (e.g., "PDP", "PUR")
     * @return List of suspended notices matching the criteria
     */
    List<SuspendedNotice> findByNoticeNoAndSuspensionTypeAndReasonOfSuspension(String noticeNo, String suspensionType, String reasonOfSuspension);
}
