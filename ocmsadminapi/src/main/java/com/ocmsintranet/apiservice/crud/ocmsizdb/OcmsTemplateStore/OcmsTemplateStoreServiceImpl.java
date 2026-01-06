package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsTemplateStore;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsTemplateStore entities
 */
@Service
public class OcmsTemplateStoreServiceImpl extends BaseImplement<OcmsTemplateStore, String, OcmsTemplateStoreRepository>
        implements OcmsTemplateStoreService {

    /**
     * Constructor with required dependencies
     */
    public OcmsTemplateStoreServiceImpl(OcmsTemplateStoreRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    // You can add custom methods or override base methods if needed
}
