package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecodehistory;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class OffenceRuleCodeHistoryServiceImpl extends BaseImplement<OffenceRuleCodeHistory, Integer, OffenceRuleCodeHistoryRepository>
    implements OffenceRuleCodeHistoryService {

    public OffenceRuleCodeHistoryServiceImpl(OffenceRuleCodeHistoryRepository repository,
            DatabaseRetryService retryService) {
        super(repository, retryService);
        //TODO Auto-generated constructor stub
    }

    }
