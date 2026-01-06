package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraCompanyDetail;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraCompanyDetailServiceImpl extends BaseImplement<OcmsDhAcraCompanyDetail, OcmsDhAcraCompanyDetailId, OcmsDhAcraCompanyDetailRepository> 
        implements OcmsDhAcraCompanyDetailService {

    public OcmsDhAcraCompanyDetailServiceImpl(OcmsDhAcraCompanyDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
