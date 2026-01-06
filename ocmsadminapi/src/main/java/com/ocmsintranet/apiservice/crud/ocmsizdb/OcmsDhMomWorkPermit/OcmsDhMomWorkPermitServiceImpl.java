package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMomWorkPermit;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMomWorkPermitServiceImpl extends BaseImplement<OcmsDhMomWorkPermit, OcmsDhMomWorkPermitId, OcmsDhMomWorkPermitRepository> 
        implements OcmsDhMomWorkPermitService {

    public OcmsDhMomWorkPermitServiceImpl(OcmsDhMomWorkPermitRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
