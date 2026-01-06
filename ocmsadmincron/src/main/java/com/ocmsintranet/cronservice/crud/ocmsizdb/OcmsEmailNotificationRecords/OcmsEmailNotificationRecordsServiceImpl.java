package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
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