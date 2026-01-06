package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsUplVehNo;

import com.ocmsintranet.apiservice.crud.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.version}/uplvehno")
public class OcmsUplVehNoController extends BaseController<OcmsUplVehNo, String, OcmsUplVehNoService> {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsUplVehNoController(OcmsUplVehNoService service) {
        super(service);
    }
    
    // You can add custom endpoints or override base endpoints if needed
}
