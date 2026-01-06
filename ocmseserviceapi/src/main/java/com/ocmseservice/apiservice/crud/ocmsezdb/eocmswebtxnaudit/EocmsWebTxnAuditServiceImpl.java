package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsWebTxnAuditServiceImpl extends BaseImplement<EocmsWebTxnAudit, String, EocmsWebTxnAuditRepository> implements EocmsWebTxnAuditService {
    public EocmsWebTxnAuditServiceImpl(EocmsWebTxnAuditRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
