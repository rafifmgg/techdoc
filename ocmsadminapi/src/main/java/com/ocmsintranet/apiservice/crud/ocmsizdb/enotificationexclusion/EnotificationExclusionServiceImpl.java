package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusion;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class EnotificationExclusionServiceImpl
    extends BaseImplement<EnotificationExclusion, String, EnotificationExclusionRepository>
    implements EnotificationExclusionService {

    public EnotificationExclusionServiceImpl(EnotificationExclusionRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
