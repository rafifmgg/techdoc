package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import com.ocmsintranet.apiservice.crud.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/${api.version}/ocmssuspensionreason")
public class OcmsSuspensionReasonController extends BaseController<OcmsSuspensionReason, OcmsSuspensionReasonId, OcmsSuspensionReasonService> {
    
    public OcmsSuspensionReasonController(OcmsSuspensionReasonService service) {
        super(service);
        log.info("OcmsSuspensionReason controller initialized");
    }
}
