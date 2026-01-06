package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsEmailNotificationRecordsRepository extends BaseRepository<OcmsEmailNotificationRecords, OcmsEmailNotificationRecordsId> {
    // No custom methods - using only methods from BaseRepository
}