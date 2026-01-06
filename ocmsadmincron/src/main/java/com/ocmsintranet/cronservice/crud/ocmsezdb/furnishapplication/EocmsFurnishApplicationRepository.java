package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplication;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EocmsFurnishApplicationRepository extends BaseRepository<EocmsFurnishApplication, String> {

    /**
     * Find all furnish applications by sync status
     * Used by cron job to find unsynced records (is_sync = 'N')
     */
    List<EocmsFurnishApplication> findByIsSync(String isSync);

    /**
     * Find furnish application by notice number
     */
    List<EocmsFurnishApplication> findByNoticeNo(String noticeNo);
}
