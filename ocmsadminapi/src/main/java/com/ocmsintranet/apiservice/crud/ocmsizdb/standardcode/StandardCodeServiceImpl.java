package com.ocmsintranet.apiservice.crud.ocmsizdb.standardcode;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class StandardCodeServiceImpl
    extends BaseImplement<StandardCode, StandardCodeId, StandardCodeRepository>
    implements StandardCodeService {

    public StandardCodeServiceImpl(StandardCodeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
