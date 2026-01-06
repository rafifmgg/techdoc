package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMsfComcareFund;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMsfComcareFundServiceImpl extends BaseImplement<OcmsDhMsfComcareFund, OcmsDhMsfComcareFundId, OcmsDhMsfComcareFundRepository> 
        implements OcmsDhMsfComcareFundService {

    public OcmsDhMsfComcareFundServiceImpl(OcmsDhMsfComcareFundRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
