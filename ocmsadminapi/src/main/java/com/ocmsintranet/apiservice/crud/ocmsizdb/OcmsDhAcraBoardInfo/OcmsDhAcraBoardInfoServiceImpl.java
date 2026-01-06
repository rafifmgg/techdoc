package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraBoardInfo;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraBoardInfoServiceImpl extends BaseImplement<OcmsDhAcraBoardInfo, OcmsDhAcraBoardInfoId, OcmsDhAcraBoardInfoRepository> 
        implements OcmsDhAcraBoardInfoService {

    public OcmsDhAcraBoardInfoServiceImpl(OcmsDhAcraBoardInfoRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
