package com.ocmsintranet.cronservice.crud.ocmsizdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for WebTxnDetail entities
 */
@Service
public class WebTxnDetailServiceImpl extends BaseImplement<WebTxnDetail, WebTxnDetailId, WebTxnDetailRepository>
        implements WebTxnDetailService {
    
    /**
     * Constructor with required dependencies
     */
    public WebTxnDetailServiceImpl(WebTxnDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
