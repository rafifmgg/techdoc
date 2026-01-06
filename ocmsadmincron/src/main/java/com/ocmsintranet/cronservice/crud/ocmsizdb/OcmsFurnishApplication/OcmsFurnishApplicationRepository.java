package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsFurnishApplicationRepository extends BaseRepository<OcmsFurnishApplication, String> {

    /**
     * Find all furnish applications by notice number
     */
    List<OcmsFurnishApplication> findByNoticeNo(String noticeNo);

    /**
     * Find all furnish applications by status
     */
    List<OcmsFurnishApplication> findByStatus(String status);
}
