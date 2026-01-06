package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeDetail;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsOffenceNoticeDetail entities
 */
@Service
public class OcmsOffenceNoticeDetailServiceImpl extends BaseImplement<OcmsOffenceNoticeDetail, String, OcmsOffenceNoticeDetailRepository> 
        implements OcmsOffenceNoticeDetailService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsOffenceNoticeDetailServiceImpl(OcmsOffenceNoticeDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}