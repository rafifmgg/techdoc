package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsEmailNotificationRecords entities
 */
@Service
public class OcmsEmailNotificationRecordsServiceImpl extends BaseImplement<OcmsEmailNotificationRecords, OcmsEmailNotificationRecordsId, OcmsEmailNotificationRecordsRepository> 
        implements OcmsEmailNotificationRecordsService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsEmailNotificationRecordsServiceImpl(OcmsEmailNotificationRecordsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}