package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.apiservice.crud.BaseService;

import java.util.List;

public interface OcmsFurnishApplicationService extends BaseService<OcmsFurnishApplication, String> {

    /**
     * Find all furnish applications by notice number
     */
    List<OcmsFurnishApplication> findByNoticeNo(String noticeNo);

    /**
     * Find all furnish applications by status
     */
    List<OcmsFurnishApplication> findByStatus(String status);

    /**
     * Find all furnish applications by status (for pending approval workflow)
     */
    List<OcmsFurnishApplication> findByStatusIn(List<String> statuses);
}
