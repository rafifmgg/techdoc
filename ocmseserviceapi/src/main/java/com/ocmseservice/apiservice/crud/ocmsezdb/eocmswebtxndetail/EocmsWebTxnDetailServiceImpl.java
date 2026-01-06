package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsWebTxnDetailServiceImpl extends BaseImplement<EocmsWebTxnDetail, EocmsWebTxnDetailId, EocmsWebTxnDetailRepository> implements EocmsWebTxnDetailService {
    public EocmsWebTxnDetailServiceImpl(EocmsWebTxnDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
