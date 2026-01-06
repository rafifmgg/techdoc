package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsCustody;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhSpsCustodyServiceImpl extends BaseImplement<OcmsDhSpsCustody, OcmsDhSpsCustodyId, OcmsDhSpsCustodyRepository> 
        implements OcmsDhSpsCustodyService {

    public OcmsDhSpsCustodyServiceImpl(OcmsDhSpsCustodyRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}