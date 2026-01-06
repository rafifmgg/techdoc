package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OffenceRuleCode entities
 */
@Service
public class OffenceRuleCodeServiceImpl extends BaseImplement<OffenceRuleCode, OffenceRuleCodeId, OffenceRuleCodeRepository> 
        implements OffenceRuleCodeService {
    
    /**
     * Constructor with required dependencies
     */
    public OffenceRuleCodeServiceImpl(OffenceRuleCodeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}