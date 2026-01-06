package com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsComcryptOperationServiceImpl extends BaseImplement<OcmsComcryptOperation, String, OcmsComcryptOperationRepository> 
        implements OcmsComcryptOperationService {

    public OcmsComcryptOperationServiceImpl(OcmsComcryptOperationRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
