package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraShareholderInfo;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraShareholderInfoServiceImpl extends BaseImplement<OcmsDhAcraShareholderInfo, OcmsDhAcraShareholderInfoId, OcmsDhAcraShareholderInfoRepository> 
        implements OcmsDhAcraShareholderInfoService {

    public OcmsDhAcraShareholderInfoServiceImpl(OcmsDhAcraShareholderInfoRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
