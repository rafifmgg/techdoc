package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsUplVehNo;

import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

/**
 * Service implementation for OcmsUplVehNo entities
 */
@Service
public class OcmsUplVehNoServiceImpl extends BaseImplement<OcmsUplVehNo, String, OcmsUplVehNoRepository> 
        implements OcmsUplVehNoService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsUplVehNoServiceImpl(OcmsUplVehNoRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
