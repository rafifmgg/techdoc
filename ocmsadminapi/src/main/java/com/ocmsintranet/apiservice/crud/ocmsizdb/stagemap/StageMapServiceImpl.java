package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class StageMapServiceImpl
    extends BaseImplement<StageMap, StageMapId, StageMapRepository>
    implements StageMapService {

    public StageMapServiceImpl(StageMapRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
