package com.ocmsintranet.apiservice.crud.ocmsizdb.parameter;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class ParameterServiceImpl
    extends BaseImplement<Parameter, ParameterId, ParameterRepository>
    implements ParameterService {

    public ParameterServiceImpl(ParameterRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
        //TODO Auto-generated constructor stub
    }}

