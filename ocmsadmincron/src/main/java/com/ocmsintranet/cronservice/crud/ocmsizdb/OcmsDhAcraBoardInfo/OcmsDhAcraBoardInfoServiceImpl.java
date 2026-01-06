package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraBoardInfo;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhAcraBoardInfoServiceImpl extends BaseImplement<OcmsDhAcraBoardInfo, OcmsDhAcraBoardInfoId, OcmsDhAcraBoardInfoRepository> 
        implements OcmsDhAcraBoardInfoService {

    public OcmsDhAcraBoardInfoServiceImpl(OcmsDhAcraBoardInfoRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
