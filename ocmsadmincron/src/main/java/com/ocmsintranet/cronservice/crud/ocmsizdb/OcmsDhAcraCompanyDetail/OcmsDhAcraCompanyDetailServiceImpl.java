package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraCompanyDetail;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraCompanyDetailServiceImpl extends BaseImplement<OcmsDhAcraCompanyDetail, OcmsDhAcraCompanyDetailId, OcmsDhAcraCompanyDetailRepository> 
        implements OcmsDhAcraCompanyDetailService {

    public OcmsDhAcraCompanyDetailServiceImpl(OcmsDhAcraCompanyDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
