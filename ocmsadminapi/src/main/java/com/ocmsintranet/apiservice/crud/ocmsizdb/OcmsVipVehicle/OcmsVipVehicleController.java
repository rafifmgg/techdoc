package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVipVehicle;

import com.ocmsintranet.apiservice.crud.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.version}/vipvehicle")
public class OcmsVipVehicleController extends BaseController<OcmsVipVehicle, String, OcmsVipVehicleService> {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsVipVehicleController(OcmsVipVehicleService ocmsVipVehicleService) {
        super(ocmsVipVehicleService);
    }
    
    // You can add custom endpoints or override base endpoints if needed
}
