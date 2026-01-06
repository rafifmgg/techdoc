package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsSmsNotificationRecordsRepository extends BaseRepository<OcmsSmsNotificationRecords, String> {
    // No custom methods - using only methods from BaseRepository
}