package com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsBatchJob entities
 */
@Service
public class OcmsBatchJobServiceImpl extends BaseImplement<OcmsBatchJob, Integer, OcmsBatchJobRepository> 
        implements OcmsBatchJobService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsBatchJobServiceImpl(OcmsBatchJobRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
