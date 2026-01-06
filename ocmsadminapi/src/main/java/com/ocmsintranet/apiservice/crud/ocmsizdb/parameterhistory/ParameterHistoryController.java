package com.ocmsintranet.apiservice.crud.ocmsizdb.parameterhistory;

import com.ocmsintranet.apiservice.crud.BaseController;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.version}/parameterhistory")
public class ParameterHistoryController extends BaseController<ParameterHistory, Integer, ParameterHistoryService> {

    public ParameterHistoryController(ParameterHistoryService service) {
        super(service);
        //TODO Auto-generated constructor stub
    }}