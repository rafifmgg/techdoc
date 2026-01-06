package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsdrivernotice;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsDriverNoticeServiceImpl extends BaseImplement<EocmsDriverNotice, EocmsDriverNoticeId, EocmsDriverNoticeRepository> implements EocmsDriverNoticeService {
    public EocmsDriverNoticeServiceImpl(EocmsDriverNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
