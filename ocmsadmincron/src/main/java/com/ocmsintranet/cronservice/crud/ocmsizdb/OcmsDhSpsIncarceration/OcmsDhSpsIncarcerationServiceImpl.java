package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhSpsIncarceration;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhSpsIncarcerationServiceImpl extends BaseImplement<OcmsDhSpsIncarceration, String, OcmsDhSpsIncarcerationRepository> 
        implements OcmsDhSpsIncarcerationService {

    public OcmsDhSpsIncarcerationServiceImpl(OcmsDhSpsIncarcerationRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
