package com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import org.springframework.stereotype.Service;

@Service
public class SuspensionReasonServiceImpl
    extends BaseImplement<SuspensionReason, SuspensionReasonId, SuspensionReasonRepository>
    implements SuspensionReasonService {

    public SuspensionReasonServiceImpl(SuspensionReasonRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
