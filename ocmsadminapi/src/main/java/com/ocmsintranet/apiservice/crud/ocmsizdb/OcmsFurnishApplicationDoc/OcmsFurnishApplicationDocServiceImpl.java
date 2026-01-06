package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OcmsFurnishApplicationDocServiceImpl extends BaseImplement<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId, OcmsFurnishApplicationDocRepository>
        implements OcmsFurnishApplicationDocService {

    public OcmsFurnishApplicationDocServiceImpl(OcmsFurnishApplicationDocRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo) {
        return repository.findByTxnNo(txnNo);
    }
}
