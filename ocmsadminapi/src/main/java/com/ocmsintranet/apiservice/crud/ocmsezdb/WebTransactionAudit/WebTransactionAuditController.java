package com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit;

import com.ocmsintranet.apiservice.crud.BaseController;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for WebTransactionAudit entities
 */
@RestController
@RequestMapping("/${api.version}/web-transaction-audits")
public class WebTransactionAuditController extends BaseController<WebTransactionAudit, String, WebTransactionAuditService> {

    /**
     * Constructor with required dependencies
     */
    public WebTransactionAuditController(WebTransactionAuditService webTransactionAuditService) {
        super(webTransactionAuditService);
    }
    
    // You can add custom endpoints or override base endpoints if needed
}
