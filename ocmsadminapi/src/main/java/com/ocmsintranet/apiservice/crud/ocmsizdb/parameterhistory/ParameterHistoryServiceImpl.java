package com.ocmsintranet.apiservice.crud.ocmsizdb.parameterhistory;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class ParameterHistoryServiceImpl
    extends BaseImplement<ParameterHistory, Integer, ParameterHistoryRepository>
    implements ParameterHistoryService {

    public ParameterHistoryServiceImpl(ParameterHistoryRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
        //TODO Auto-generated constructor stub
    }}
