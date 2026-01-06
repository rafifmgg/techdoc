package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplication;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for EocmsFurnishApplication entities (Internet DB)
 */
@Service
public class EocmsFurnishApplicationServiceImpl
        extends BaseImplement<EocmsFurnishApplication, String, EocmsFurnishApplicationRepository>
        implements EocmsFurnishApplicationService {

    /**
     * Constructor with required dependencies
     */
    public EocmsFurnishApplicationServiceImpl(EocmsFurnishApplicationRepository repository,
                                               DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<EocmsFurnishApplication> findByIsSync(String isSync) {
        return repository.findByIsSync(isSync);
    }

    @Override
    public List<EocmsFurnishApplication> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }
}
