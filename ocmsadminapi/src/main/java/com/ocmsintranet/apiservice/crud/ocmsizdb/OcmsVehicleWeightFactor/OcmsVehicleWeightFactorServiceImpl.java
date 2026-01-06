package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVehicleWeightFactor;

import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

/**
 * Service implementation for OcmsVehicleWeightFactor entities
 */
@Service
public class OcmsVehicleWeightFactorServiceImpl extends BaseImplement<OcmsVehicleWeightFactor, String, OcmsVehicleWeightFactorRepository> 
        implements OcmsVehicleWeightFactorService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsVehicleWeightFactorServiceImpl(OcmsVehicleWeightFactorRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
