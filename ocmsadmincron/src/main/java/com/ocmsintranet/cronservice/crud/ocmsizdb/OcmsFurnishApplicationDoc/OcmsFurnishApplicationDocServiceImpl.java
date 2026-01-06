package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for OcmsFurnishApplicationDoc entities (Intranet DB)
 */
@Service
public class OcmsFurnishApplicationDocServiceImpl
        extends BaseImplement<OcmsFurnishApplicationDoc, OcmsFurnishApplicationDocId, OcmsFurnishApplicationDocRepository>
        implements OcmsFurnishApplicationDocService {

    /**
     * Constructor with required dependencies
     */
    public OcmsFurnishApplicationDocServiceImpl(OcmsFurnishApplicationDocRepository repository,
                                                  DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsFurnishApplicationDoc> findByTxnNo(String txnNo) {
        return repository.findByTxnNo(txnNo);
    }
}
