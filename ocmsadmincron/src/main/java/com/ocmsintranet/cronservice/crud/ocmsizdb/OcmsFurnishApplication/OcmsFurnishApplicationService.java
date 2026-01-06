package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.cronservice.crud.BaseService;

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
}
