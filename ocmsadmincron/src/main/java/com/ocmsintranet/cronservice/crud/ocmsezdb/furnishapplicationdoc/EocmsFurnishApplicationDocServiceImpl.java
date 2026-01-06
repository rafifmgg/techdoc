package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for EocmsFurnishApplicationDoc entities (Internet DB)
 */
@Service
public class EocmsFurnishApplicationDocServiceImpl
        extends BaseImplement<EocmsFurnishApplicationDoc, EocmsFurnishApplicationDocId, EocmsFurnishApplicationDocRepository>
        implements EocmsFurnishApplicationDocService {

    /**
     * Constructor with required dependencies
     */
    public EocmsFurnishApplicationDocServiceImpl(EocmsFurnishApplicationDocRepository repository,
                                                   DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<EocmsFurnishApplicationDoc> findByIsSync(String isSync) {
        return repository.findByIsSync(isSync);
    }

    @Override
    public List<EocmsFurnishApplicationDoc> findByTxnNo(String txnNo) {
        return repository.findByTxnNo(txnNo);
    }
}
