package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OcmsNroTempServiceImpl extends BaseImplement<OcmsNroTemp, Long, OcmsNroTempRepository>
        implements OcmsNroTempService {

    public OcmsNroTempServiceImpl(OcmsNroTempRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsNroTemp> findUnprocessedByQueryReason(String queryReason) {
        return repository.findByQueryReasonAndProcessed(queryReason, false);
    }

    @Override
    public List<OcmsNroTemp> findByQueryReason(String queryReason) {
        return repository.findByQueryReason(queryReason);
    }
}
