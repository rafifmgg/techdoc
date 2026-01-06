package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsEmailNotificationRecordsRepository extends BaseRepository<OcmsEmailNotificationRecords, OcmsEmailNotificationRecordsId> {

    /**
     * Find email notification records by notice number and email address
     * @param noticeNo Notice number
     * @param emailAddr Email address
     * @return List of email notification records
     */
    List<OcmsEmailNotificationRecords> findByNoticeNoAndEmailAddr(String noticeNo, String emailAddr);
}