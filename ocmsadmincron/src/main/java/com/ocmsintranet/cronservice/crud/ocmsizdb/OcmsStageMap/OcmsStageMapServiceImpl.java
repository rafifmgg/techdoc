package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsStageMap;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsStageMapServiceImpl
    extends BaseImplement<OcmsStageMap, String, OcmsStageMapRepository>
    implements OcmsStageMapService {

    public OcmsStageMapServiceImpl(OcmsStageMapRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
