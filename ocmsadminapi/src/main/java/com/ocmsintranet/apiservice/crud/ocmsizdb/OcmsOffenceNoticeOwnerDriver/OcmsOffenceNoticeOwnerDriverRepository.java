package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsOffenceNoticeOwnerDriverRepository extends BaseRepository<OcmsOffenceNoticeOwnerDriver, OcmsOffenceNoticeOwnerDriverId> {

    /**
     * Find all ONOD records by notice number
     * @param noticeNo Notice number
     * @return List of ONOD records
     */
    List<OcmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo);

    /**
     * Find all ONOD records by ID number
     * @param idNo ID number
     * @return List of ONOD records
     */
    List<OcmsOffenceNoticeOwnerDriver> findByIdNo(String idNo);
}
