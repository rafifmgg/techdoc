package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMhaPass;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMhaPassServiceImpl extends BaseImplement<OcmsDhMhaPass, OcmsDhMhaPassId, OcmsDhMhaPassRepository> 
        implements OcmsDhMhaPassService {

    public OcmsDhMhaPassServiceImpl(OcmsDhMhaPassRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
