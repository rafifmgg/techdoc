package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsSmsNotificationRecords entities
 */
@Service
public class OcmsSmsNotificationRecordsServiceImpl extends BaseImplement<OcmsSmsNotificationRecords, OcmsSmsNotificationRecordsId, OcmsSmsNotificationRecordsRepository> 
        implements OcmsSmsNotificationRecordsService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsSmsNotificationRecordsServiceImpl(OcmsSmsNotificationRecordsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}