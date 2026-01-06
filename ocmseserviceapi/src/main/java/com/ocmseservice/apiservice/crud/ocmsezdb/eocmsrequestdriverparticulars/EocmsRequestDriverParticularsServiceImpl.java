package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsRequestDriverParticularsServiceImpl extends BaseImplement<EocmsRequestDriverParticulars, EocmsRequestDriverParticularsId, EocmsRequestDriverParticularsRepository> implements EocmsRequestDriverParticularsService {
    public EocmsRequestDriverParticularsServiceImpl(EocmsRequestDriverParticularsRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
