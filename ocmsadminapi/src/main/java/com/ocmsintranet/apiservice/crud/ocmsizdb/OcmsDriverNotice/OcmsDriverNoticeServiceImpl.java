package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsDriverNoticeServiceImpl extends BaseImplement<OcmsDriverNotice, OcmsDriverNoticeId, OcmsDriverNoticeRepository>
        implements OcmsDriverNoticeService {

    public OcmsDriverNoticeServiceImpl(OcmsDriverNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}