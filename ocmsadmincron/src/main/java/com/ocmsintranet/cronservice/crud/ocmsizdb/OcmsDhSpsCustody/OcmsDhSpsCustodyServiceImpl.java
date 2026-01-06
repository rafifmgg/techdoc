package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhSpsCustody;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhSpsCustodyServiceImpl extends BaseImplement<OcmsDhSpsCustody, OcmsDhSpsCustodyId, OcmsDhSpsCustodyRepository> 
        implements OcmsDhSpsCustodyService {

    public OcmsDhSpsCustodyServiceImpl(OcmsDhSpsCustodyRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}