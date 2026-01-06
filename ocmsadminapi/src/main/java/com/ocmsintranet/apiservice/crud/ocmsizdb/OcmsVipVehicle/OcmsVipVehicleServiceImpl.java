package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVipVehicle;

import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

/**
 * Service implementation for OcmsVipVehicle entities
 */
@Service
public class OcmsVipVehicleServiceImpl extends BaseImplement<OcmsVipVehicle, String, OcmsVipVehicleRepository> 
        implements OcmsVipVehicleService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsVipVehicleServiceImpl(OcmsVipVehicleRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
