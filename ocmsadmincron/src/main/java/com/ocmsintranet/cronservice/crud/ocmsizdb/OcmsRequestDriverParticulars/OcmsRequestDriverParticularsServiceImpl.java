package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsRequestDriverParticulars entities
 */
@Service
public class OcmsRequestDriverParticularsServiceImpl extends BaseImplement<OcmsRequestDriverParticulars, OcmsRequestDriverParticularsId, OcmsRequestDriverParticularsRepository> 
        implements OcmsRequestDriverParticularsService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsRequestDriverParticularsServiceImpl(OcmsRequestDriverParticularsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}