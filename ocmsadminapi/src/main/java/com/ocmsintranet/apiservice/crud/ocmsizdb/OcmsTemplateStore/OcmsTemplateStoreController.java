package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsTemplateStore;

import com.ocmsintranet.apiservice.crud.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for OcmsTemplateStore entity
 * Provides CRUD operations for template metadata
 */
@RestController
@RequestMapping("/${api.version}/templatestore")
public class OcmsTemplateStoreController extends BaseController<OcmsTemplateStore, String, OcmsTemplateStoreService> {

    /**
     * Constructor with required dependencies
     */
    public OcmsTemplateStoreController(OcmsTemplateStoreService service) {
        super(service);
    }

    // You can add custom endpoints or override base endpoints if needed
}
