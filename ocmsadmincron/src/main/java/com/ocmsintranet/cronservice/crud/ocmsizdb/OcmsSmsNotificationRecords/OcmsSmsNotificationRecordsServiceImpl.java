package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsSmsNotificationRecords entities
 */
@Service
public class OcmsSmsNotificationRecordsServiceImpl extends BaseImplement<OcmsSmsNotificationRecords, String, OcmsSmsNotificationRecordsRepository> 
        implements OcmsSmsNotificationRecordsService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsSmsNotificationRecordsServiceImpl(OcmsSmsNotificationRecordsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}