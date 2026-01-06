package com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for Icarpark entities
 */
@Service
public class IcarparkServiceImpl extends BaseImplement<Icarpark, Integer, IcarparkRepository> 
        implements IcarparkService {
    
    /**
     * Constructor with required dependencies
     */
    public IcarparkServiceImpl(IcarparkRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
