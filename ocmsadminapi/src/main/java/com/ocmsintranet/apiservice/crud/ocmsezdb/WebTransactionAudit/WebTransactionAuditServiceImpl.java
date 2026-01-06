package com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for WebTransactionAudit entities
 */
@Service
public class WebTransactionAuditServiceImpl extends BaseImplement<WebTransactionAudit, String, WebTransactionAuditRepository> 
        implements WebTransactionAuditService {
    
    /**
     * Constructor with required dependencies
     */
    public WebTransactionAuditServiceImpl(WebTransactionAuditRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
