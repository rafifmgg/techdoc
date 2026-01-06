package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusionhistory;

import com.ocmsintranet.apiservice.crud.BaseController;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for EnotificationExclusionHistory entity
 */
@RestController
@RequestMapping("/${api.version}/enotificationexclusionhistory")
public class EnotificationExclusionHistoryController extends BaseController<EnotificationExclusionHistory, Integer, EnotificationExclusionHistoryService> {

    public EnotificationExclusionHistoryController(EnotificationExclusionHistoryService service) {
        super(service);
    }
}
