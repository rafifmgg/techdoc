package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVehicleWeightFactor;

import com.ocmsintranet.apiservice.crud.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.version}/vehicleweightfactor")
public class OcmsVehicleWeightFactorController extends BaseController<OcmsVehicleWeightFactor, String, OcmsVehicleWeightFactorService> {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsVehicleWeightFactorController(OcmsVehicleWeightFactorService service) {
        super(service);
    }
    
    // You can add custom endpoints or override base endpoints if needed
}
