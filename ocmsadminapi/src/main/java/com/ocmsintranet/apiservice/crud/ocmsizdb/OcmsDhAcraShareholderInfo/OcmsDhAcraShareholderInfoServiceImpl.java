package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraShareholderInfo;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraShareholderInfoServiceImpl extends BaseImplement<OcmsDhAcraShareholderInfo, OcmsDhAcraShareholderInfoId, OcmsDhAcraShareholderInfoRepository> 
        implements OcmsDhAcraShareholderInfoService {

    public OcmsDhAcraShareholderInfoServiceImpl(OcmsDhAcraShareholderInfoRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
