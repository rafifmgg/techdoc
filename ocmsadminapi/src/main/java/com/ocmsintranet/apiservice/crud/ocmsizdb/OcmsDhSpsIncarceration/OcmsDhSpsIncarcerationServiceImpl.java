package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsIncarceration;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhSpsIncarcerationServiceImpl extends BaseImplement<OcmsDhSpsIncarceration, String, OcmsDhSpsIncarcerationRepository> 
        implements OcmsDhSpsIncarcerationService {

    public OcmsDhSpsIncarcerationServiceImpl(OcmsDhSpsIncarcerationRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
