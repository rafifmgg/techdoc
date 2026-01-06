package com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver;

import com.ocmseservice.apiservice.crud.BaseImplement;
import com.ocmseservice.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class EocmsOffenceNoticeOwnerDriverServiceImpl extends BaseImplement<EocmsOffenceNoticeOwnerDriver, EocmsOffenceNoticeOwnerDriverId, EocmsOffenceNoticeOwnerDriverRepository> implements EocmsOffenceNoticeOwnerDriverService {
    public EocmsOffenceNoticeOwnerDriverServiceImpl(EocmsOffenceNoticeOwnerDriverRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
