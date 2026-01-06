package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusionhistory;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class EnotificationExclusionHistoryServiceImpl
    extends BaseImplement<EnotificationExclusionHistory, Integer, EnotificationExclusionHistoryRepository>
    implements EnotificationExclusionHistoryService {

    public EnotificationExclusionHistoryServiceImpl(EnotificationExclusionHistoryRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
