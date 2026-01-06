package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecodehistory;

import com.ocmsintranet.apiservice.crud.BaseController;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.version}/offencerulecodehistory")
public class OffenceRuleCodeHistoryController extends BaseController<OffenceRuleCodeHistory, Integer, OffenceRuleCodeHistoryService> {

    public OffenceRuleCodeHistoryController(OffenceRuleCodeHistoryService service) {
        super(service);
        //TODO Auto-generated constructor stub
    }

}