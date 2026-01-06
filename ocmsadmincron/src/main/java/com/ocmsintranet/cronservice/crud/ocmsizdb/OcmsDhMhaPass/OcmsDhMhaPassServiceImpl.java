package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMhaPass;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMhaPassServiceImpl extends BaseImplement<OcmsDhMhaPass, OcmsDhMhaPassId, OcmsDhMhaPassRepository> 
        implements OcmsDhMhaPassService {

    public OcmsDhMhaPassServiceImpl(OcmsDhMhaPassRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
