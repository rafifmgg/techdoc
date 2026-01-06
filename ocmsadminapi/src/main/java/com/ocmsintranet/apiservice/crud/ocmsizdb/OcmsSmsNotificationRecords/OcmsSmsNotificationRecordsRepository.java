package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsSmsNotificationRecordsRepository extends BaseRepository<OcmsSmsNotificationRecords, OcmsSmsNotificationRecordsId> {

    /**
     * Find SMS notification records by notice number and mobile number
     * @param noticeNo Notice number
     * @param mobileNo Mobile number
     * @return List of SMS notification records
     */
    List<OcmsSmsNotificationRecords> findByNoticeNoAndMobileNo(String noticeNo, String mobileNo);
}