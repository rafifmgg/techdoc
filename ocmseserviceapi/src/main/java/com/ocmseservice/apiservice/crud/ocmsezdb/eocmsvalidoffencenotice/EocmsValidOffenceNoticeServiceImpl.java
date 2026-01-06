package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsValidOffenceNoticeServiceImpl extends BaseImplement<EocmsValidOffenceNotice, String, EocmsValidOffenceNoticeRepository> implements EocmsValidOffenceNoticeService {
    
    public EocmsValidOffenceNoticeServiceImpl(EocmsValidOffenceNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
