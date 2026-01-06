package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsNroTemp entities
 */
@Service
public class OcmsNroTempServiceImpl extends BaseImplement<OcmsNroTemp, String, OcmsNroTempRepository> 
        implements OcmsNroTempService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsNroTempServiceImpl(OcmsNroTempRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}