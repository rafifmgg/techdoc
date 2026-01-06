package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction;

import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface OcmsReducedOffenceAmountRepository
        extends BaseRepository<OcmsReducedOffenceAmount, OcmsReducedOffenceAmountId> {

    @NonNull
    Optional<OcmsReducedOffenceAmount> findById(@NonNull OcmsReducedOffenceAmountId id);

    /**
     * Find the maximum sr_no for a given notice number
     */
    @org.springframework.data.jpa.repository.Query("SELECT MAX(r.srNo) FROM OcmsReducedOffenceAmount r WHERE r.noticeNo = ?1")
    Integer findMaxSrNoByNoticeNo(String noticeNo);

    /**
     * Find the reduced offence amount with the highest sr_no for a given notice number
     *
     * @param noticeNo Notice number to search for
     * @return Optional containing the reduced offence amount with max sr_no
     */
    Optional<OcmsReducedOffenceAmount> findTopByNoticeNoOrderBySrNoDesc(String noticeNo);
}
