package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMsfComcareFund;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDhMsfComcareFundServiceImpl extends BaseImplement<OcmsDhMsfComcareFund, String, OcmsDhMsfComcareFundRepository> 
        implements OcmsDhMsfComcareFundService {

    public OcmsDhMsfComcareFundServiceImpl(OcmsDhMsfComcareFundRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
