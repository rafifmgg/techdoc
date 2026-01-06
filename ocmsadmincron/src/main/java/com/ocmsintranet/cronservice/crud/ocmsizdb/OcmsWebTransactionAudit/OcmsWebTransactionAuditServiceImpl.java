package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsWebTransactionAudit;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for WebTransactionAudit entities
 */
@Service
public class OcmsWebTransactionAuditServiceImpl extends BaseImplement<OcmsWebTransactionAudit, String, OcmsWebTransactionAuditRepository> 
        implements OcmsWebTransactionAuditService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsWebTransactionAuditServiceImpl(OcmsWebTransactionAuditRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
