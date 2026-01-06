package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OcmsDriverNoticeRepository entity
 */
@Repository
public interface OcmsDriverNoticeRepository extends BaseRepository<OcmsDriverNotice, OcmsDriverNoticeId> {
    /**
     * Find all DN records for a given notice number
     * @param noticeNo Notice number
     * @return List of DN records
     */
    List<OcmsDriverNotice> findByNoticeNo(String noticeNo);
}