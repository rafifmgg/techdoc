package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsUserMessageServiceImpl extends BaseImplement<EocmsUserMessage, String, EocmsUserMessageRepository> implements EocmsUserMessageService {
    public EocmsUserMessageServiceImpl(EocmsUserMessageRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
