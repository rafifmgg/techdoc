package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsRequestDriverParticularsServiceImpl extends BaseImplement<OcmsRequestDriverParticulars, OcmsRequestDriverParticularsId, OcmsRequestDriverParticularsRepository>
        implements OcmsRequestDriverParticularsService {

    public OcmsRequestDriverParticularsServiceImpl(OcmsRequestDriverParticularsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}