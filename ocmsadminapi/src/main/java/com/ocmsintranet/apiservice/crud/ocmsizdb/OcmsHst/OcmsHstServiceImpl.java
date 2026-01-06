package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsHstServiceImpl extends BaseImplement<OcmsHst, String, OcmsHstRepository>
        implements OcmsHstService {

    public OcmsHstServiceImpl(OcmsHstRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
