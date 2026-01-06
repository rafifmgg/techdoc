package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplication;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

public interface EocmsFurnishApplicationService extends BaseService<EocmsFurnishApplication, String> {

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
