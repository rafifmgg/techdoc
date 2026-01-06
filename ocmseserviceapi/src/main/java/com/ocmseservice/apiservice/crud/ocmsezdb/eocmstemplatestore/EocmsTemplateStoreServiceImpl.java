package com.ocmseservice.apiservice.crud.ocmsezdb.eocmstemplatestore;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsTemplateStoreServiceImpl extends BaseImplement<EocmsTemplateStore, String, EocmsTemplateStoreRepository> implements EocmsTemplateStoreService {
    public EocmsTemplateStoreServiceImpl(EocmsTemplateStoreRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
