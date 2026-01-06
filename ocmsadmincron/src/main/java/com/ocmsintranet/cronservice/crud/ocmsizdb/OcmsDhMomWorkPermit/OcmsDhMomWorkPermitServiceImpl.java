package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMomWorkPermit;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMomWorkPermitServiceImpl extends BaseImplement<OcmsDhMomWorkPermit, OcmsDhMomWorkPermitId, OcmsDhMomWorkPermitRepository> 
        implements OcmsDhMomWorkPermitService {

    public OcmsDhMomWorkPermitServiceImpl(OcmsDhMomWorkPermitRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
