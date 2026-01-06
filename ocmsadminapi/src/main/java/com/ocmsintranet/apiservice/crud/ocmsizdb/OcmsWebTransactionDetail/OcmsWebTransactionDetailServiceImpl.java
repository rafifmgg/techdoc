package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsWebTransactionDetail entities
 */
@Service
public class OcmsWebTransactionDetailServiceImpl extends BaseImplement<OcmsWebTransactionDetail, OcmsWebTransactionDetailId, OcmsWebTransactionDetailRepository>
        implements OcmsWebTransactionDetailService {

    /**
     * Constructor with required dependencies
     */
    public OcmsWebTransactionDetailServiceImpl(OcmsWebTransactionDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    // You can add custom methods or override base methods if needed
}
